package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.ChunkTaskDispatcher;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkTaskDispatcher.class)
public interface IEChunkTaskDispatcher {
    @Accessor("dispatcher")
    PriorityConsecutiveExecutor ip_getMailBox();
}
