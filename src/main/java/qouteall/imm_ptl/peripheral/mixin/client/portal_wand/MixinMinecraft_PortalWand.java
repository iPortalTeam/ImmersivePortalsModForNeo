package qouteall.imm_ptl.peripheral.mixin.client.portal_wand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.peripheral.wand.PortalWandItem;
import qouteall.imm_ptl.peripheral.wand.PortalWandItemClient;

@Mixin(Minecraft.class)
public class MixinMinecraft_PortalWand {
    @Shadow
    @Nullable
    public LocalPlayer player;
    
    @Inject(
        method = "startAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
        ),
        cancellable = true
    )
    private void onStartingAttack(CallbackInfoReturnable<Boolean> cir) {
        assert player != null;
        ItemStack itemStack = player.getMainHandItem();
        if (itemStack.getItem() instanceof PortalWandItem) {
            PortalWandItemClient.onClientLeftClick(player, itemStack);
            cir.setReturnValue(false);
        }
    }
}
