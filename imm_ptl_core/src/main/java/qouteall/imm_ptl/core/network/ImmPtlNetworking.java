package qouteall.imm_ptl.core.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.de.nick1st.neo.networking.NeoPacket;
import qouteall.q_misc_util.de.nick1st.neo.networking.PacketType;

import java.util.UUID;
import java.util.function.Supplier;

public class ImmPtlNetworking {
    
    static final Logger LOGGER = LogUtils.getLogger();
    
    // client to server
    public record TeleportPacket(
        int dimensionId, Vec3 posBefore, UUID portalId
    ) implements NeoPacket {
        public static final PacketType<TeleportPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl:teleport"),
            TeleportPacket::read
        );
        
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
            buf.writeDouble(posBefore.x);
            buf.writeDouble(posBefore.y);
            buf.writeDouble(posBefore.z);
            buf.writeUUID(portalId);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ResourceKey<Level> dim = DimensionAPI.getServerDimKeyFromIntId(
                ctx.get().getSender().getServer(), dimensionId
            );
            
            IPGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                ctx.get().getSender(), dim, posBefore, portalId
            );
        }
    }
    
    // server to client
    public record GlobalPortalSyncPacket(
        int dimensionId, CompoundTag data
    ) implements NeoPacket {
        public static final PacketType<GlobalPortalSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl:upd_glb_ptl"),
            GlobalPortalSyncPacket::read
        );
        
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
        public PacketType<?> getType() {
            return TYPE;
        }
        
//        @OnlyIn(Dist.CLIENT)
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ResourceKey<Level> dim = DimensionAPI.getClientDimKeyFromIntId(dimensionId);
            
            GlobalPortalStorage.receiveGlobalPortalSync(dim, data);
        }
    }
    
    /**
     * server to client
     * {@link ClientboundAddEntityPacket}
     * This packet is redirected, so there is no need to contain dimension id
     */
    public record PortalSyncPacket(
        int id,
        UUID uuid,
        EntityType<?> type,
        int dimensionId,
        double x,
        double y,
        double z,
        CompoundTag extraData
    ) implements NeoPacket {

        public static final PacketType<PortalSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl:spawn_portal"),
            PortalSyncPacket::read
        );
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(id);
            buf.writeUUID(uuid);
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, type);
            buf.writeVarInt(dimensionId);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeNbt(extraData);
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
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        /**
         * {@link ClientPacketListener#handleAddEntity(ClientboundAddEntityPacket)}
         */
//        @OnlyIn(Dist.CLIENT)
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ImmPtlNetworkingClient.handleSyncPacket(ctx, this);
//
//            ResourceKey<Level> dimension = DimensionAPI.getClientDimKeyFromIntId(dimensionId);
//            ClientLevel world = ClientWorldLoader.getWorld(dimension);
//
//            Entity existing = world.getEntity(id);
//
//            if (existing instanceof Portal existingPortal) {
//                // update existing portal (handles default animation)
//                if (!Objects.equals(existingPortal.getUUID(), uuid)) {
//                    LOGGER.error("UUID mismatch when syncing portal {} {}", existingPortal, uuid);
//                    return;
//                }
//
//                if (existingPortal.getType() != type) {
//                    LOGGER.error("Entity type mismatch when syncing portal {} {}", existingPortal, type);
//                    return;
//                }
//
//                existingPortal.acceptDataSync(new Vec3(x, y, z), extraData);
//            }
//            else {
//                // spawn new portal
//                Entity entity = type.create(world);
//                Validate.notNull(entity, "Entity type is null");
//
//                if (!(entity instanceof Portal portal)) {
//                    LOGGER.error("Spawned entity is not a portal. {} {}", entity, type);
//                    return;
//                }
//
//                entity.setId(id);
//                entity.setUUID(uuid);
//                entity.syncPacketPositionCodec(x, y, z);
//                entity.moveTo(x, y, z);
//
//                portal.readPortalDataFromNbt(extraData);
//
//                world.addEntity(entity);
//
//                ClientWorldLoader.getWorld(portal.dimensionTo);
//                Portal.clientPortalSpawnSignal.emit(portal);
//
//                if (IPGlobal.clientPortalLoadDebug) {
//                    LOGGER.info("Portal loaded to client {}", portal);
//                }
//            }
        }
    }
    
    public static void init() {
//        ServerPlayNetworking.registerGlobalReceiver(
//            TeleportPacket.TYPE,
//            (packet, player, responseSender) -> packet.handle(player)
//        );
        NeoPacket.register(TeleportPacket.class, TeleportPacket.TYPE, TeleportPacket::write,
                TeleportPacket::read, TeleportPacket::handle, PlayNetworkDirection.PLAY_TO_SERVER);

        NeoPacket.register(GlobalPortalSyncPacket.class, GlobalPortalSyncPacket.TYPE, GlobalPortalSyncPacket::write,
                GlobalPortalSyncPacket::read, GlobalPortalSyncPacket::handle, PlayNetworkDirection.PLAY_TO_CLIENT);

        NeoPacket.register(PortalSyncPacket.class, PortalSyncPacket.TYPE, PortalSyncPacket::write,
                PortalSyncPacket::read, PortalSyncPacket::handle, PlayNetworkDirection.PLAY_TO_CLIENT);
    }
    
    public static void initClient() {
//        ClientPlayNetworking.registerGlobalReceiver(
//            GlobalPortalSyncPacket.TYPE,
//            (packet, player, responseSender) -> packet.handle()
//        );
        
//        ClientPlayNetworking.registerGlobalReceiver(
//            PortalSyncPacket.TYPE,
//            (packet, player, responseSender) -> packet.handle()
//        );
    }
    
}
