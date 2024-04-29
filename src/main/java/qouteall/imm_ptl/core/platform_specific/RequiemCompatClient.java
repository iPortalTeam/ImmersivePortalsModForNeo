package qouteall.imm_ptl.core.platform_specific;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.q_misc_util.Helper;

import static qouteall.imm_ptl.core.platform_specific.RequiemCompat.getPossessedEntity;
import static qouteall.imm_ptl.core.platform_specific.RequiemCompat.isRequiemPresent;

public final class RequiemCompatClient {
    private RequiemCompatClient() {}

    public static void onPlayerTeleportedClient() {
        if (!isRequiemPresent) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        Mob possessedEntity = getPossessedEntity(player);
        if (possessedEntity != null) {
            if (possessedEntity.level() != player.level()) {
                Helper.LOGGER.info("Move Requiem Possessed Entity at Client");
                ClientTeleportationManager.moveClientEntityAcrossDimension(
                        possessedEntity,
                        ((ClientLevel) player.level()),
                        player.position()
                );
            }
        }
    }
}
