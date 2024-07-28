package qouteall.imm_ptl.core.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.mixin.client.sync.MixinMinecraft_RedirectedPacket;
import qouteall.q_misc_util.dimension.DimensionIntId;
import qouteall.q_misc_util.my_util.LimitedLogger;

//@OnlyIn(Dist.CLIENT)
public class PacketRedirectionClient {

    public static final Minecraft client = Minecraft.getInstance();
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);

    /**
     * This ensures that when it calls client.execute(...)
     * the task will be executed with redirected dimension.
     * {@link MixinMinecraft_RedirectedPacket}
     * This is also used in networking threads.
     */
    public static final ThreadLocal<ResourceKey<Level>> clientTaskRedirection =
            ThreadLocal.withInitial(() -> null);

    public static boolean getIsProcessingRedirectedMessage() {
        return clientTaskRedirection.get() != null;
    }

    /**
     * For vanilla packets, in {@link PacketUtils#ensureRunningOnSameThread(Packet, PacketListener, BlockableEventLoop)}
     * it will resubmit the task,
     * and the task will be redirected in {@link MixinMinecraft_RedirectedPacket},
     * except for the bundle packet {@link net.minecraft.client.multiplayer.ClientPacketListener#handleBundlePacket(ClientboundBundlePacket)}.
     * <p>
     * For mod packets ({@link ClientboundCustomPayloadPacket}),
     * the mod will also handle the packet using {@link Minecraft#execute(Runnable)} (If not, that mod has the bug)
     */
    public static void handleRedirectedPacket(
            int dimensionIntId,
            Packet<ClientGamePacketListener> packet,
            ClientGamePacketListener handler
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            ResourceKey<Level> dimension = DimensionIntId.getClientMap()
                    .fromIntegerId(dimensionIntId);

            ResourceKey<Level> oldTaskRedirection = clientTaskRedirection.get();
            clientTaskRedirection.set(dimension);

            try {
                ClientWorldLoader.withSwitchedWorldFailSoft(
                        dimension,
                        () -> {
                            packet.handle(handler);
                        }
                );
            } finally {
                clientTaskRedirection.set(oldTaskRedirection);
            }
        } else {
            minecraft.execute(() -> {
                handleRedirectedPacket(
                        dimensionIntId, packet, handler
                );
            });
        }
    }
}
