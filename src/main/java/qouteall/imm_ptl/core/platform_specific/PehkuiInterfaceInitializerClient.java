package qouteall.imm_ptl.core.platform_specific;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.portal.Portal;

import static qouteall.imm_ptl.core.platform_specific.PehkuiInterfaceInitializer.doScalingForEntity;

public class PehkuiInterfaceInitializerClient {
    static void onPlayerTeleportedClient(Portal portal) {
        if (portal.hasScaling() && portal.isTeleportChangesScale()) {
            Minecraft client = Minecraft.getInstance();

            LocalPlayer player = client.player;

            Validate.notNull(player);

            doScalingForEntity(player, portal);

            IECamera camera = (IECamera) client.gameRenderer.getMainCamera();
            camera.ip_setCameraY(
                    ((float) (camera.ip_getCameraY() * portal.getScaling())),
                    ((float) (camera.ip_getLastCameraY() * portal.getScaling()))
            );
        }
    }
}
