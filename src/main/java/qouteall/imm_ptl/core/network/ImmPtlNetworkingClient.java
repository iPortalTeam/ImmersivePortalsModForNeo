package qouteall.imm_ptl.core.network;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.NetworkEvent;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.api.DimensionAPI;

import java.util.Objects;
import java.util.function.Supplier;

import static qouteall.imm_ptl.core.network.ImmPtlNetworking.LOGGER;

public abstract class ImmPtlNetworkingClient {
    public static void handleSyncPacket(Supplier<NetworkEvent.Context> ctx, ImmPtlNetworking.PortalSyncPacket syncPacket) {
        ResourceKey<Level> dimension = DimensionAPI.getClientDimKeyFromIntId(syncPacket.dimensionId());
        ClientLevel world = ClientWorldLoader.getWorld(dimension);

        Entity existing = world.getEntity(syncPacket.intId());

        if (existing instanceof Portal existingPortal) {
            // update existing portal (handles default animation)
            if (!Objects.equals(existingPortal.getUUID(), syncPacket.uuid())) {
                LOGGER.error("UUID mismatch when syncing portal {} {}", existingPortal, syncPacket.uuid());
                return;
            }

            if (existingPortal.getType() != syncPacket.type()) {
                LOGGER.error("Entity type mismatch when syncing portal {} {}", existingPortal, syncPacket.type());
                return;
            }

            existingPortal.acceptDataSync(new Vec3(syncPacket.x(), syncPacket.y(), syncPacket.z()), syncPacket.extraData());
        }
        else {
            // spawn new portal
            Entity entity = syncPacket.type().create(world);
            Validate.notNull(entity, "Entity type is null");

            if (!(entity instanceof Portal portal)) {
                LOGGER.error("Spawned entity is not a portal. {} {}", entity, syncPacket.type());
                return;
            }

            entity.setId(syncPacket.intId());
            entity.setUUID(syncPacket.uuid());
            entity.syncPacketPositionCodec(syncPacket.x(), syncPacket.y(), syncPacket.z());
            entity.moveTo(syncPacket.x(), syncPacket.y(), syncPacket.z());

            portal.readPortalDataFromNbt(syncPacket.extraData());

            world.addEntity(entity);

            ClientWorldLoader.getWorld(portal.dimensionTo);
            Portal.clientPortalSpawnSignal.emit(portal);

            if (IPGlobal.clientPortalLoadDebug) {
                LOGGER.info("Portal loaded to client {}", portal);
            }
        }
    }
}
