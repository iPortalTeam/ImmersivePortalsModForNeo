package qouteall.imm_ptl.peripheral.mixin.common.dim_stack;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ToFullChunk;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkStatusTasks.class)
public class MixinChunkStatus_CVB {
    @Inject(
            method = "generateSpawn", at = @At("HEAD")
    )
    private static void onGenerateSpawn(
            WorldGenContext worldGenContext, ChunkStatus chunkStatus,
            Executor executor, ToFullChunk toFullChunk, List<ChunkAccess> list,
            ChunkAccess chunkAccess, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        DimStackManagement.replaceBedrock(worldGenContext.level(), chunkAccess);
    }
}