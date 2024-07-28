package qouteall.imm_ptl.core.mixin.common.container_gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;

@SuppressWarnings("ALL")
@Mixin(Container.class)
public interface MixinContainer {
    @Inject(
        method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;F)Z",
        at = @At("RETURN"),
        remap = false,
        cancellable = true
    )
    private static void onStillValidBlockEntity(
        BlockEntity blockEntity, Player player, float distance, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            BlockPos targetPos = blockEntity.getBlockPos();
            Level targetWorld = blockEntity.getLevel();

            if (BlockManipulationServer.validateReach(player, targetWorld, targetPos)) {
                cir.setReturnValue(true);
            }
        }
    }

}
