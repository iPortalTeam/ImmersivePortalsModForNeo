package qouteall.imm_ptl.core.mixin.common.container_gui;

import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Container.class)
public interface MixinContainer { // TODO @Nick1st PRIO Get this mixin to work
//    @Inject(
//        method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;I)Z",
//        at = @At("RETURN"),
//        cancellable = true
//    )
//    private static void onStillValidBlockEntity(
//        BlockEntity blockEntity, Player player, int distance, CallbackInfoReturnable<Boolean> cir
//    ) {
//        if (!cir.getReturnValue()) {
//            PortalUtils.PortalAwareRaytraceResult result = PortalUtils.portalAwareRayTrace(
//                player.level(),
//                player.getEyePosition(),
//                player.getViewVector(1),
//                32,
//                player,
//                ClipContext.Block.COLLIDER
//            );
//            if (result != null &&
//                result.hitResult().getBlockPos().equals(blockEntity.getBlockPos())
//            ) {
//                cir.setReturnValue(true);
//            }
//        }
//    }
}
