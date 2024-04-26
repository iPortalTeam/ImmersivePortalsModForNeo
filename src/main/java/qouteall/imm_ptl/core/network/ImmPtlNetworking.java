package qouteall.imm_ptl.core.network;

import com.mojang.logging.LogUtils;
import de.nick1st.imm_ptl.events.ClientPortalSpawnEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;

import java.util.Objects;
import java.util.UUID;

public class ImmPtlNetworking {
    
    static final Logger LOGGER = LogUtils.getLogger();
    
    // client to server
    public static record TeleportPacket(
        int dimensionId, Vec3 eyePosBeforeTeleportation, UUID portalId
    ) implements CustomPacketPayload {
        public static final ResourceLocation ID = new ResourceLocation("imm_ptl:teleport");
        
        public static TeleportPacket read(FriendlyByteBuf buf) {
            int dimId = buf.readVarInt();
            Vec3 pos = new Vec3(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
            );
            UUID portalId = buf.readUUID();
            return new TeleportPacket(dimId, pos, portalId);
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(dimensionId);
            buf.writeDouble(eyePosBeforeTeleportation.x);
            buf.writeDouble(eyePosBeforeTeleportation.y);
            buf.writeDouble(eyePosBeforeTeleportation.z);
            buf.writeUUID(portalId);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }
        
        public void handle(PlayPayloadContext playPayloadContext) {
            Player player = playPayloadContext.player().get();
            ResourceKey<Level> dim = PortalAPI.serverIntToDimKey(
                player.getServer(), dimensionId
            );
            
            ServerTeleportationManager.of(player.getServer()).onPlayerTeleportedInClient(
                    (ServerPlayer) player, dim, eyePosBeforeTeleportation, portalId
            );
        }
    }
    
    // server to client
    public static record GlobalPortalSyncPacket(
        int dimensionId, CompoundTag data
    ) implements CustomPacketPayload {
        public static final ResourceLocation ID = new ResourceLocation("imm_ptl:upd_glb_ptl");
        
        public static GlobalPortalSyncPacket read(FriendlyByteBuf buf) {
            int dimId = buf.readVarInt();
            CompoundTag compoundTag = buf.readNbt();
            return new GlobalPortalSyncPacket(dimId, compoundTag);
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(dimensionId);
            buf.writeNbt(data);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }
        
        //@OnlyIn(Dist.CLIENT)
        public void handle(PlayPayloadContext playPayloadContext) {
            ResourceKey<Level> dim = PortalAPI.clientIntToDimKey(dimensionId);
            
            GlobalPortalStorage.receiveGlobalPortalSync(dim, data);
        }
    }
    
    /**
     * server to client
     * {@link ClientboundAddEntityPacket}
     * This packet is redirected, so there is no need to contain dimension intId
     */
    public static record PortalSyncPacket(
        int intId,
        UUID uuid,
        EntityType<?> type,
        int dimensionId,
        double x,
        double y,
        double z,
        CompoundTag extraData
    ) implements CustomPacketPayload {

        public PortalSyncPacket {
            // debug
//            Helper.LOGGER.info("PortalSyncPacket create {}", MiscHelper.getServer().overworld().getGameTime());
        }
        
        public static final ResourceLocation ID = new ResourceLocation("imm_ptl:spawn_portal");
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(intId);
            buf.writeUUID(uuid);
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, type);
            buf.writeVarInt(dimensionId);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeNbt(extraData);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        public static PortalSyncPacket read(FriendlyByteBuf buf) {
            int id = buf.readVarInt();
            UUID uuid = buf.readUUID();
            EntityType<?> type = buf.readById(BuiltInRegistries.ENTITY_TYPE);
            int dimensionId = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            CompoundTag extraData = buf.readNbt();
            return new PortalSyncPacket(id, uuid, type, dimensionId, x, y, z, extraData);
        }
        
        /**
         * {@link ClientPacketListener#handleAddEntity(ClientboundAddEntityPacket)}
         */
        //@OnlyIn(Dist.CLIENT)
        public void handle(PlayPayloadContext playPayloadContext) {
//            Helper.LOGGER.info("PortalSyncPacket handle {}", RenderStates.frameIndex);

            ResourceKey<Level> dimension = PortalAPI.clientIntToDimKey(dimensionId);
            ClientLevel world = ClientWorldLoader.getWorld(dimension);

            Entity existing = world.getEntity(intId);

            if (existing instanceof Portal existingPortal) {
                // update existing portal (handles default animation)
                if (!Objects.equals(existingPortal.getUUID(), uuid)) {
                    LOGGER.error("UUID mismatch when syncing portal {} {}", existingPortal, uuid);
                    return;
                }

                if (existingPortal.getType() != type) {
                    LOGGER.error("Entity type mismatch when syncing portal {} {}", existingPortal, type);
                    return;
                }

                existingPortal.acceptDataSync(new Vec3(x, y, z), extraData);
            }
            else {
                // spawn new portal
                Entity entity = type.create(world);
                Validate.notNull(entity, "Entity type is null");

                if (!(entity instanceof Portal portal)) {
                    LOGGER.error("Spawned entity is not a portal. {} {}", entity, type);
                    return;
                }

                entity.setId(intId);
                entity.setUUID(uuid);
                entity.syncPacketPositionCodec(x, y, z);
                entity.moveTo(x, y, z);

                portal.readPortalDataFromNbt(extraData);

                world.addEntity(entity);

                ClientWorldLoader.getWorld(portal.getDestDim());
                NeoForge.EVENT_BUS.post(new ClientPortalSpawnEvent(portal));

                if (IPGlobal.clientPortalLoadDebug) {
                    LOGGER.info("Portal loaded to client {}", portal);
                }
            }
        }
    }
}
