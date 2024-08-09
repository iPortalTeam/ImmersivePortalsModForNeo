package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ThrowableProjectile.class)
public class MixinThrowableProjectile {
//    @WrapOperation(
//        method = "tick",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
//        )
//    )
//    private BlockState wrapGetBlockState(
//        Level level, BlockPos blockPos, Operation<BlockState> original,
//        @Share("immptl_shouldCancelHit") LocalBooleanRef shouldCancelHit
//    ) {
//        BlockState blockState = original.call(level, blockPos);
//
//        if (blockState.getBlock() == PortalPlaceholderBlock.instance) {
//            shouldCancelHit.set(true);
//        }
//        else {
//            shouldCancelHit.set(false);
//        }
//
//        return blockState;
//    }
    
    // TODO check projectile go through nether portal
//    @WrapWithCondition(
//        method = "tick",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"
//        )
//    )
//    private boolean wrapOnHit(
//        ThrowableProjectile throwableProjectile, HitResult hitResult,
//        @Share("immptl_shouldCancelHit") LocalBooleanRef shouldCancelHit
//    ) {
//        return !shouldCancelHit.get();
//    }
}
