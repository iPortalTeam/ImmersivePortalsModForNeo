package qouteall.imm_ptl.peripheral.wand;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import static qouteall.imm_ptl.peripheral.wand.PortalWandItem.COMPONENT_TYPE;
import static qouteall.imm_ptl.peripheral.wand.PortalWandItem.showSettings;

public final class PortalWandItemClient {
    private PortalWandItemClient() {}

    public static void onClientLeftClick(LocalPlayer player, ItemStack itemStack) {
        if (player.isShiftKeyDown()) {
            showSettings(player);
        }
        else {
            PortalWandItem.Mode mode = itemStack.getOrDefault(COMPONENT_TYPE, PortalWandItem.Mode.FALLBACK);
            switch (mode) {
                case CREATE_PORTAL -> {
                    ClientPortalWandPortalCreation.onLeftClick();
                }
                case DRAG_PORTAL -> {
                    ClientPortalWandPortalDrag.onLeftClick();
                }
                case COPY_PORTAL -> {
                    ClientPortalWandPortalCopy.onLeftClick();
                }
            }
        }
    }
}
