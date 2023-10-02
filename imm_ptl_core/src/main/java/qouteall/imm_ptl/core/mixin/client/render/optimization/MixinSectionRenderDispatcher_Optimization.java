package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.util.thread.ProcessorMailbox;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;

@Mixin(SectionRenderDispatcher.class)
public abstract class MixinSectionRenderDispatcher_Optimization {
    @Shadow
    private volatile int freeBufferCount;
    
    @Mutable
    @Shadow
    @Final
    private Queue<SectionBufferBuilderPack> freeBuffers;
    
    @Shadow
    @Final
    private ProcessorMailbox<Runnable> mailbox;
    
    @Shadow
    protected abstract void runTask();
    
    @Shadow
    @Nullable
    protected abstract SectionRenderDispatcher.RenderSection.CompileTask pollTask();
    
    @Shadow
    private volatile int toBatchCount;
    
    @Shadow
    @Final
    private PriorityBlockingQueue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchHighPriority;
    
    @Shadow
    @Final
    private Queue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchLowPriority;
    
    @Shadow
    @Final
    private Executor executor;
    
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I",
            ordinal = 1
        ),
        require = 0
    )
    private int redirectMax(int a, int b) {
        if (SharedBlockMeshBuffers.isEnabled()) {
            return 0;
        }
        return Math.max(a, b);
    }
    
    // inject on constructor seems to be not working normally, so use redirect
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/thread/ProcessorMailbox;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/thread/ProcessorMailbox;"
        ),
        require = 0
    )
    private ProcessorMailbox<Runnable> redirectCreate(Executor dispatcher, String name) {
        if (SharedBlockMeshBuffers.isEnabled()) {
            Validate.isTrue(freeBufferCount == 0);
            freeBuffers = SharedBlockMeshBuffers.acquireThreadBuffers();
            freeBufferCount = freeBuffers.size();
        }
        
        return ProcessorMailbox.create(dispatcher, name);
    }
    
    /**
     * Multiple chunk render dispatchers can manipulate the buffer queue now,
     * so checking isEmpty and poll it after will not work.
     * The logic of runTask has changed as follows:
     * 1. if there is no buffer, isEmpty returns true
     * 2. if there is a buffer but no task, isEmpty still returns true
     * 3. if there is a buffer and a task, isEmpty will return false
     * 4. pollTask will not return null
     * 5. the buffer object and task object are passed by thread local
     * (Mixin seems does not allow local capture or modifying local variable in redirect)
     */
    @Redirect(
        method = "runTask",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Queue;isEmpty()Z"
        )
    )
    private boolean redirectIsEmpty(Queue<SectionBufferBuilderPack> bufferQueue) {
        if (!SharedBlockMeshBuffers.isEnabled()) {
            return bufferQueue.isEmpty();
        }
        
        SectionBufferBuilderPack buffer = bufferQueue.poll();
        
        if (buffer != null) {
            for (; ; ) {
                SectionRenderDispatcher.RenderSection.CompileTask polledTask = pollTask();
                
                if (polledTask != null) {
                    if (((IEChunkCompileTask) polledTask).getIsCancelled().get()) {
                        // if the task is cancelled, discard this task and continue
                        continue;
                    }
                    
                    SharedBlockMeshBuffers.bufferTemp.set(buffer);
                    SharedBlockMeshBuffers.taskTemp.set(polledTask);
                    
                    // will launch the task
                    return false;
                }
                else {
                    // no task to run
                    // put the buffer back to the queue
                    bufferQueue.add(buffer);
                    
                    // exit runTasks
                    return true;
                }
            }
        }
        else {
            mailbox.tell(this::runTask);
            // in vanilla, when a task finishes it will call runTask again
            // when there is no buffer, there must be other tasks running and the runTask will trigger later
            // but with shared buffers, the buffers may be taken by other dimensions.
            
            // exit runTasks
            return true;
        }
    }
    
    @Redirect(
        method = "runTask",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;pollTask()Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$CompileTask;"
        )
    )
    SectionRenderDispatcher.RenderSection.CompileTask redirectPollTask(SectionRenderDispatcher instance) {
        if (!SharedBlockMeshBuffers.isEnabled()) {
            return pollTask();
        }
        
        var task = SharedBlockMeshBuffers.taskTemp.get();
        
        Validate.notNull(task);
        
        SharedBlockMeshBuffers.taskTemp.set(null);
        
        return task;
    }
    
    @Redirect(
        method = "runTask",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Queue;poll()Ljava/lang/Object;"
        )
    )
    private <E> E redirectPollBuffer(Queue<E> queue) {
        if (!SharedBlockMeshBuffers.isEnabled()) {
            return queue.poll();
        }
        
        var object = SharedBlockMeshBuffers.bufferTemp.get();
        
        Validate.notNull(object);
        
        SharedBlockMeshBuffers.bufferTemp.set(null);
        
        return (E) object;
    }
    
    @Redirect(
        method = "dispose",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Queue;clear()V"
        )
    )
    private void redirectClear(Queue<SectionBufferBuilderPack> queue) {
        if (SharedBlockMeshBuffers.isEnabled()) {
            // the freeBuffers queue was a shared object, don't clear it
            // otherwise it will break after dynamically removing a dimension
            freeBuffers = new ConcurrentLinkedQueue<>();
        }
        else {
            freeBuffers.clear();
        }
    }
    
}