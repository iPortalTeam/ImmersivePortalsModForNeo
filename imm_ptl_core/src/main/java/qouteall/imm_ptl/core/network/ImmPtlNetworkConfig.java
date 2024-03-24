package qouteall.imm_ptl.core.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.LoginNetworkDirection;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.mixin.common.other_sync.IEServerConfigurationPacketListenerImpl;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.de.nick1st.neo.networking.NeoPacket;
import qouteall.q_misc_util.de.nick1st.neo.networking.PacketType;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class ImmPtlNetworkConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public record ModVersion(
        int major, int minor, int patch
    ) {
        // for dev env
        public static final ModVersion OTHER = new ModVersion(0, 0, 0);
        
        public static ModVersion read(FriendlyByteBuf buf) {
            int major = buf.readVarInt();
            int minor = buf.readVarInt();
            int patch = buf.readVarInt();
            return new ModVersion(major, minor, patch);
        }
        
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(major);
            buf.writeVarInt(minor);
            buf.writeVarInt(patch);
        }
        
        @Override
        public String toString() {
            return "%d.%d.%d".formatted(major, minor, patch);
        }
        
        public boolean isNormalVersion() {
            return !OTHER.equals(this);
        }
        
        public boolean isCompatibleWith(ModVersion another) {
            return major == another.major && minor == another.minor;
        }
    }
    
    public static ModVersion immPtlVersion;
    
    @SuppressWarnings("UnstableApiUsage")
    public record ImmPtlConfigurationTask(
    ) implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE =
            new ConfigurationTask.Type("imm_ptl_core:config");
        
        @Override
        public void start(Consumer<Packet<?>> consumer) {
            consumer.accept(
                    NeoPacket.channels.get(S2CConfigStartPacket.TYPE).toVanillaPacket(new S2CConfigStartPacket(
                            immPtlVersion
                    ), PlayNetworkDirection.PLAY_TO_CLIENT)
//                ServerConfigurationNetworking.createS2CPacket(new S2CConfigStartPacket(
//                    immPtlVersion
//                ))
            );
        }
        
        @Override
        public @NotNull Type type() {
            return TYPE;
        }
    }
    
    public record S2CConfigStartPacket(
        ModVersion versionFromServer
    ) implements NeoPacket {
        public static final PacketType<S2CConfigStartPacket> TYPE =
            PacketType.create(
                new ResourceLocation("imm_ptl_core:config_packet"),
                S2CConfigStartPacket::read
            );
        
        public static S2CConfigStartPacket read(FriendlyByteBuf buf) {
            ModVersion info = ModVersion.read(buf);
            return new S2CConfigStartPacket(info);
        }
        
        public void write(FriendlyByteBuf buf) {
            versionFromServer.write(buf);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        // handled on client side
//        @OnlyIn(Dist.CLIENT)
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            LOGGER.info(
                "Client received ImmPtl config packet. Server mod version: {}", versionFromServer
            );
            
            serverVersion = versionFromServer;
            NeoPacket.channels.get(C2SConfigCompletePacket.TYPE.identifier).sendToServer(new C2SConfigCompletePacket(
                immPtlVersion, IPConfig.getConfig().clientTolerantVersionMismatchWithServer
            ));
        }
    }
    
    public record C2SConfigCompletePacket(
        ModVersion versionFromClient,
        boolean clientTolerantVersionMismatch
    ) implements NeoPacket {
        public static final PacketType<C2SConfigCompletePacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl_core:configure_complete"),
            C2SConfigCompletePacket::read
        );
        
        public static C2SConfigCompletePacket read(FriendlyByteBuf buf) {
            ModVersion info = ModVersion.read(buf);
            boolean clientTolerantVersionMismatch = buf.readBoolean();
            return new C2SConfigCompletePacket(info, clientTolerantVersionMismatch);
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            versionFromClient.write(buf);
            buf.writeBoolean(clientTolerantVersionMismatch);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        // handled on server side
        public void handle(
            Supplier<NetworkEvent.Context> ctx
        ) {
            GameProfile gameProfile = ((IEServerConfigurationPacketListenerImpl)
                    ctx.get().getNetworkManager().getPacketListener()).ip_getGameProfile();
            
            LOGGER.info(
                "Server received ImmPtl config packet. Mod version: {} Player: {} {}",
                versionFromClient, gameProfile.getName(), gameProfile.getId()
            );
            
            if (versionFromClient.isNormalVersion() && immPtlVersion.isNormalVersion()) {
                if ((versionFromClient.major != immPtlVersion.major ||
                    versionFromClient.minor != immPtlVersion.minor) &&
                    !IPConfig.getConfig().serverTolerantVersionMismatchWithClient &&
                    !clientTolerantVersionMismatch
                ) {
                    ctx.get().getNetworkManager().disconnect(Component.translatable(
                        "imm_ptl.mod_major_minor_version_mismatch",
                        immPtlVersion.toString(),
                        versionFromClient.toString()
                    ));
                    LOGGER.info(
                        """
                            Disconnecting client because of ImmPtl version difference (only patch version difference is tolerated).
                            Game Profile: {}
                            Client ImmPtl version: {}
                            Server ImmPtl version: {}""",
                        gameProfile, versionFromClient, immPtlVersion
                    );
                }
            }
            
//            networkHandler.completeTask(ImmPtlConfigurationTask.TYPE); // TODO @Nick1st figure out what this line is
        }
    }
    
    public static void init() {
        immPtlVersion = O_O.getImmPtlVersion();
        
        LOGGER.info("Immersive Portals Core version {}", immPtlVersion);

        NeoForge.EVENT_BUS.addListener(NetworkEvent.ServerCustomPayloadLoginEvent.class, event -> {
//            if (ServerConfigurationNetworking.canSend(event.getSource().getNetworkManager(), S2CConfigStartPacket.TYPE)) {
////                event.getSource().getNetworkManager().addTask(new ImmPtlConfigurationTask()); // TODO @Nick1st figure out what this line is
//            }
//            else {
                if (event.getSource().getSender().getServer().isDedicatedServer()) {
                    if (IPConfig.getConfig().serverRejectClientWithoutImmPtl) {
                        // cannot use translation key here
                        // because the translation does not exist on client without the mod
                        event.getSource().getNetworkManager().disconnect(Component.literal(
                            """
                                The server detected that client does not install Immersive Portals mod.
                                A server with Immersive Portals mod only works with the clients that have it.
                                
                                (Note: The networking sync may be interfered by Essential mod, Bad Packets mod or other mods. When you are using these mods, the detection may malfunction. In this case, you can disable networking check in the server side by changing `serverRejectClientWithoutImmPtl` to `false` in the server's config file `config/immersive_portals.json` and restart.)
                                """
                        ));
                    }
                    else {
                        GameProfile gameProfile =
                            ((IEServerConfigurationPacketListenerImpl) event.getSource().getNetworkManager()).ip_getGameProfile();
                        
                        LOGGER.warn(
                            "Fabric API's sendable channel sync detected that client does not install ImmPtl. {} {}",
                            gameProfile.getName(), gameProfile.getId()
                        );
                    }
                }
                else {
                    LOGGER.error("ImmPtl configuration channel is non-sendable from Fabric API in integrated server. Fabric API sendable channel sync is interfered.");
                }
//            }
        });

        NeoPacket.register(C2SConfigCompletePacket.class, C2SConfigCompletePacket.TYPE, C2SConfigCompletePacket::write,
                C2SConfigCompletePacket::read, C2SConfigCompletePacket::handle, LoginNetworkDirection.LOGIN_TO_SERVER);

//        ServerConfigurationNetworking.registerGlobalReceiver(
//            C2SConfigCompletePacket.TYPE,
//            C2SConfigCompletePacket::handle
//        );

        NeoPacket.register(S2CConfigStartPacket.class, S2CConfigStartPacket.TYPE, S2CConfigStartPacket::write,
                S2CConfigStartPacket::read, S2CConfigStartPacket::handle, LoginNetworkDirection.LOGIN_TO_CLIENT);
    }
    
    //@OnlyIn(Dist.CLIENT)
    public static void initClient() {
        // ClientConfigurationNetworking.ConfigurationPacketHandler does not provide
        // ClientConfigurationPacketListenerImpl argument
//        ClientConfigurationNetworking.registerGlobalReceiver(
//            S2CConfigStartPacket.TYPE,
//            S2CConfigStartPacket::handle
//        );
        
//        ClientLoginConnectionEvents.INIT.register(
//            (handler, client) -> {
//                LOGGER.info("Client login init");
//                // if the config packet is not received,
//                // serverProtocolInfo will always be nul
//                // it will become not null when receiving ImmPtl config packet
//                serverVersion = null;
//            }
//        );

        // TODO @Nick1st check
        NeoForge.EVENT_BUS.addListener(NetworkEvent.ClientCustomPayloadLoginEvent.class, event -> {
            LOGGER.info("Client login init");
            // if the config packet is not received,
            // serverProtocolInfo will always be nul
            // it will become not null when receiving ImmPtl config packet
            serverVersion = null;
        });

        // TODO @Nick1st check
        NeoForge.EVENT_BUS.addListener(NetworkEvent.ServerCustomPayloadLoginEvent.class, event -> {
            onClientJoin();
        });

//        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
//            onClientJoin();
//        });
    }
    
    private static void onClientJoin() {
        if (serverVersion == null) {
            warnServerMissingImmPtl();
        }
        else {
            if (serverVersion.isNormalVersion() &&
                immPtlVersion.isNormalVersion() &&
                !serverVersion.equals(immPtlVersion)
            ) {
                if (IPConfig.getConfig().shouldDisplayWarning("mod_version_mismatch")) {
                    MutableComponent text =
                        Component.translatable(
                            "imm_ptl.mod_patch_version_mismatch",
                            Component.literal(serverVersion.toString())
                                .withStyle(ChatFormatting.GOLD),
                            Component.literal(immPtlVersion.toString())
                                .withStyle(ChatFormatting.GOLD)
                        ).append(
                            IPMcHelper.getDisableWarningText("mod_version_mismatch")
                        );
                    CHelper.printChat(text);
                }
            }
        }
    }
    
    // used on client
    private static @Nullable ImmPtlNetworkConfig.ModVersion serverVersion = null;
    
    // should be called from client
    public static boolean doesServerHaveImmPtl() {
        return serverVersion != null;
    }
    
    private static void warnServerMissingImmPtl() {
        Minecraft.getInstance().execute(() -> {
            CHelper.printChat(Component.translatable("imm_ptl.server_missing_immptl"));
        });
    }
}
