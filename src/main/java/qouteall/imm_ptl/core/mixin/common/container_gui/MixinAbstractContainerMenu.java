package qouteall.imm_ptl.core.mixin.common.container_gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;

@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu {
    @WrapOperation(
        method = "lambda$stillValid$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;canInteractWithBlock(Lnet/minecraft/core/BlockPos;D)Z"
        )
    )
    private static boolean wrapDistanceToSqr(
            Player instance, BlockPos pPos, double pDistance, Operation<Boolean> original, @Local(argsOnly = true) Level world, @Local(argsOnly = true) BlockPos blockPos
    ) {
        boolean canInteract = original.call(instance, blockPos, pDistance);
        if (canInteract) {
            return true;
        }

        return BlockManipulationServer.validateReach(instance, world, blockPos);
    }
}
