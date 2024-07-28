package qouteall.imm_ptl.core.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.ServerPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorageClient;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;

import java.util.UUID;

import static qouteall.imm_ptl.core.network.ImmPtlNetworkingClient.handleSyncPacket;

public class ImmPtlNetworking {

    static final Logger LOGGER = LogUtils.getLogger();

    // client to server
    public static record TeleportPacket(
            int dimensionId, Vec3 eyePosBeforeTeleportation, UUID portalId
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TeleportPacket> TYPE = new Type<>(ResourceLocation.parse("imm_ptl:teleport"));

        public static final StreamCodec<FriendlyByteBuf, TeleportPacket> CODEC = StreamCodec.of(
                (b, p) -> p.write(b), TeleportPacket::read
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

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(dimensionId);
            buf.writeDouble(eyePosBeforeTeleportation.x);
            buf.writeDouble(eyePosBeforeTeleportation.y);
            buf.writeDouble(eyePosBeforeTeleportation.z);
            buf.writeUUID(portalId);
        }

        public void handle(ServerPayloadContext ctx) {
            ServerPlayer player = ctx.player();
            ResourceKey<Level> dim = PortalAPI.serverIntToDimKey(
                    player.server, dimensionId
            );

            ServerTeleportationManager.of(player.server).onPlayerTeleportedInClient(
                    player, dim, eyePosBeforeTeleportation, portalId
            );
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // server to client
    public static record GlobalPortalSyncPacket(
            int dimensionId, CompoundTag data
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GlobalPortalSyncPacket> TYPE = new Type<>(ResourceLocation.parse("imm_ptl:upd_glb_ptl"));

        public static final StreamCodec<FriendlyByteBuf, GlobalPortalSyncPacket> CODEC = StreamCodec.of(
                (b, p) -> p.write(b), GlobalPortalSyncPacket::read
        );

        public static GlobalPortalSyncPacket read(FriendlyByteBuf buf) {
            int dimId = buf.readVarInt();
            CompoundTag compoundTag = buf.readNbt();
            return new GlobalPortalSyncPacket(dimId, compoundTag);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(dimensionId);
            buf.writeNbt(data);
        }

        @OnlyIn(Dist.CLIENT)
        public void handle() {
            ResourceKey<Level> dim = PortalAPI.clientIntToDimKey(dimensionId);

            GlobalPortalStorageClient.receiveGlobalPortalSync(dim, data);
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * server to client
     * {@link ClientboundAddEntityPacket}
     * This packet is redirected, so there is no need to contain dimension intId
     */
    public static record PortalSyncPacket(
            int id,
            UUID uuid,
            EntityType<?> entityType,
            int dimensionId,
            double x,
            double y,
            double z,
            CompoundTag extraData
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PortalSyncPacket> TYPE = new Type<>(ResourceLocation.parse("imm_ptl:spawn_portal"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PortalSyncPacket> CODEC = StreamCodec.of(
                (b, p) -> p.write(b), PortalSyncPacket::read
        );

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(id);
            buf.writeUUID(uuid);
            ByteBufCodecs.registry(Registries.ENTITY_TYPE).encode(buf, entityType);
            buf.writeVarInt(dimensionId);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeNbt(extraData);
        }

        public static PortalSyncPacket read(RegistryFriendlyByteBuf buf) {
            int id = buf.readVarInt();
            UUID uuid = buf.readUUID();
            EntityType<?> type = ByteBufCodecs.registry(Registries.ENTITY_TYPE).decode(buf);
            int dimensionId = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            CompoundTag extraData = buf.readNbt();
            return new PortalSyncPacket(id, uuid, type, dimensionId, x, y, z, extraData);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /**
         * {@link ClientPacketListener#handleAddEntity(ClientboundAddEntityPacket)}
         */
        //@OnlyIn(Dist.CLIENT)
        public void handle() {
            handleSyncPacket(this);
        }
    }
}
