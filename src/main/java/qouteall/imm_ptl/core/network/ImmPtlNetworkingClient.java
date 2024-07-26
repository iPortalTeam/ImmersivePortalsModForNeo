package qouteall.imm_ptl.core.network;

import de.nick1st.imm_ptl.events.ClientPortalSpawnEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.Objects;

import static qouteall.imm_ptl.core.network.ImmPtlNetworking.LOGGER;

public abstract class ImmPtlNetworkingClient {
    public static void handleSyncPacket(ImmPtlNetworking.PortalSyncPacket syncPacket) {
//      Helper.LOGGER.info("PortalSyncPacket handle {}", RenderStates.frameIndex);
        ResourceKey<Level> dimension = PortalAPI.clientIntToDimKey(syncPacket.dimensionId());
        ClientLevel world = ClientWorldLoader.getWorld(dimension);

        Entity existing = world.getEntity(syncPacket.id());

        if (existing instanceof Portal existingPortal) {
            // update existing portal (handles default animation)
            if (!Objects.equals(existingPortal.getUUID(), syncPacket.uuid())) {
                LOGGER.error("UUID mismatch when syncing portal {} {}", existingPortal, syncPacket.uuid());
                return;
            }

            if (existingPortal.getType() != syncPacket.entityType()) {
                LOGGER.error("Entity type mismatch when syncing portal {} {}", existingPortal, syncPacket.type());
                return;
            }

            existingPortal.acceptDataSync(new Vec3(syncPacket.x(), syncPacket.y(), syncPacket.z()), syncPacket.extraData());
        }
        else {
            // spawn new portal
            Entity entity = syncPacket.entityType().create(world);
            Validate.notNull(entity, "Entity type is null");

            if (!(entity instanceof Portal portal)) {
                LOGGER.error("Spawned entity is not a portal. {} {}", entity, syncPacket.type());
                return;
            }

            entity.setId(syncPacket.id());
            entity.setUUID(syncPacket.uuid());
            entity.syncPacketPositionCodec(syncPacket.x(), syncPacket.y(), syncPacket.z());
            entity.moveTo(syncPacket.x(), syncPacket.y(), syncPacket.z());

            portal.readPortalDataFromNbt(syncPacket.extraData());

            world.addEntity(entity);

            ClientWorldLoader.getWorld(portal.dimensionTo);
            NeoForge.EVENT_BUS.post(new ClientPortalSpawnEvent(portal));

            if (IPGlobal.clientPortalLoadDebug) {
                LOGGER.info("Portal loaded to client {}", portal);
            }
        }
    }
}
