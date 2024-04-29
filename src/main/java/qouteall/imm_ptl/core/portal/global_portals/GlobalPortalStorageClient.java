package qouteall.imm_ptl.core.portal.global_portals;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;

import java.util.List;

import static qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage.getGlobalPortals;
import static qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage.getPortalsFromTag;

public final class GlobalPortalStorageClient {
    private GlobalPortalStorageClient() {}

    public static void onClientCleanup() {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientLevel clientWorld : ClientWorldLoader.getClientWorlds()) {
                for (Portal globalPortal : getGlobalPortals(clientWorld)) {
                    globalPortal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                }
            }
        }
    }

    public static void receiveGlobalPortalSync(ResourceKey<Level> dimension, CompoundTag compoundTag) {
        ClientLevel world = ClientWorldLoader.getWorld(dimension);

        List<Portal> oldGlobalPortals = ((IEClientWorld) world).ip_getGlobalPortals();
        if (oldGlobalPortals != null) {
            for (Portal p : oldGlobalPortals) {
                p.remove(Entity.RemovalReason.KILLED);
            }
        }

        List<Portal> newPortals = getPortalsFromTag(compoundTag, world);
        for (Portal p : newPortals) {
            p.myUnsetRemoved();
            p.isGlobalPortal = true;

            Validate.isTrue(p.isPortalValid());

            ClientWorldLoader.getWorld(p.getDestDim());
        }

        ((IEClientWorld) world).ip_setGlobalPortals(newPortals);

        Helper.log("Global Portals Updated " + dimension.location());
    }
}
