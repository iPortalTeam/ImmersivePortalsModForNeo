package qouteall.q_misc_util;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import org.slf4j.Logger;
import qouteall.q_misc_util.de.nick1st.neo.networking.NeoPacket;
import qouteall.q_misc_util.de.nick1st.neo.networking.PacketType;
import qouteall.q_misc_util.dimension.DimensionEvents;
import qouteall.q_misc_util.dimension.DimensionIdRecord;
import qouteall.q_misc_util.dimension.DimensionTypeSync;
import qouteall.q_misc_util.mixin.client.IEClientPacketListener_Misc;

import java.util.Set;
import java.util.function.Supplier;

public class MiscNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final ResourceLocation id_stcRemote =
        new ResourceLocation("imm_ptl", "remote_stc");
    public static final ResourceLocation id_ctsRemote =
        new ResourceLocation("imm_ptl", "remote_cts");
    
    public record DimSyncPacket(
        CompoundTag idMapTag,
        CompoundTag typeMapTag
    ) implements NeoPacket {
        public static final PacketType<DimSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl", "dim_sync"),
            DimSyncPacket::read
        );
        
        public static DimSyncPacket createFromServer(MinecraftServer server) {
            CompoundTag idMapTag = DimensionIdRecord.recordToTag(
                DimensionIdRecord.serverRecord,
                dim -> server.getLevel(dim) != null
            );
            
            CompoundTag typeMapTag = DimensionTypeSync.createTagFromServerWorldInfo(server);
            
            return new DimSyncPacket(idMapTag, typeMapTag);
        }
        
        public static Packet<ClientCommonPacketListener> createPacket(MinecraftServer server) {
            return (Packet<ClientCommonPacketListener>) // TODO @Nick1st check
                    NeoPacket.channels.get(TYPE.identifier).toVanillaPacket(DimSyncPacket.createFromServer(server),
                            PlayNetworkDirection.PLAY_TO_CLIENT);

        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeNbt(idMapTag);
            buf.writeNbt(typeMapTag);
        }
        
        public static DimSyncPacket read(FriendlyByteBuf buf) {
            CompoundTag idMapTag = buf.readNbt();
            CompoundTag typeMapTag = buf.readNbt();
            return new DimSyncPacket(idMapTag, typeMapTag);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        public void handleOnNetworkingThread(Supplier<NetworkEvent.Context> ctx) {
            DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMapTag);
            
            DimensionTypeSync.acceptTypeMapData(typeMapTag);
            
            LOGGER.info("Received Dimension Id Sync\n{}", DimensionIdRecord.clientRecord);
            
            // it's used for command completion
            Set<ResourceKey<Level>> dimIdSet = DimensionIdRecord.clientRecord.getDimIdSet();
            ((IEClientPacketListener_Misc) ctx.get().getNetworkManager().getPacketListener()).ip_setLevels(dimIdSet);
            
//            MiscHelper.executeOnRenderThread(() -> {
                NeoForge.EVENT_BUS.post(new DimensionEvents.ClientDimensionUpdateEvent(dimIdSet));
//            });
        }
    }
    
    //@OnlyIn(Dist.CLIENT)
    public static void initClient() {
//        ClientPlayNetworking.registerGlobalReceiver(
//            DimSyncPacket.TYPE,
//            (packet, player, responseSender) -> {
//                packet.handleOnNetworkingThread(player.connection);
//            }
//        );
        
//        ClientPlayNetworking.registerGlobalReceiver(
//            DimSyncPacket.TYPE.getId(),
//            (client, handler, buf, responseSender) -> {
//                // must be handled early
//                // should not be handled in client main thread, otherwise it may be late
//                DimSyncPacket dimSyncPacket = DimSyncPacket.TYPE.read(buf);
//                dimSyncPacket.handleOnNetworkingThread(handler);
//            }
//        );
    }
    
    public static void init() {
        NeoPacket.register(DimSyncPacket.class, DimSyncPacket.TYPE, DimSyncPacket::write,
                DimSyncPacket::read, DimSyncPacket::handleOnNetworkingThread, PlayNetworkDirection.PLAY_TO_CLIENT);
    }
}
