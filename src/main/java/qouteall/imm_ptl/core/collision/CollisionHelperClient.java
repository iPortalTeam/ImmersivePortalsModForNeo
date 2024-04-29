package qouteall.imm_ptl.core.collision;

import net.minecraft.client.multiplayer.ClientLevel;
import qouteall.imm_ptl.core.ClientWorldLoader;

import static qouteall.imm_ptl.core.collision.CollisionHelper.updateCollidingPortalForWorld;

public final class CollisionHelperClient {
    private CollisionHelperClient() {}

    static void updateClientCollidingStatus() {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientLevel world : ClientWorldLoader.getClientWorlds()) {
                updateCollidingPortalForWorld(world, 0);
            }
        }
    }
}
