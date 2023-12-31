package qouteall.q_misc_util.de.nick1st.neo.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.*;
import net.neoforged.neoforge.network.event.EventNetworkChannel;
import net.neoforged.neoforge.network.simple.MessageFunctions;
import net.neoforged.neoforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface NeoPacket {

    Map<ResourceLocation, SimpleChannel> channels = new HashMap<>();
    Map<ResourceLocation, EventNetworkChannel> complexChannels =new HashMap<>();

    static void register(ResourceLocation identity, Consumer<NetworkEvent> listener) {
        final EventNetworkChannel channel = NetworkRegistry.newEventChannel(
                identity,
                () -> "1",
                p -> true,
                p -> true
        );

        channel.addListener(listener);

        complexChannels.put(identity, channel);
    }

    static <T extends NeoPacket> void register(Class<T> packetClass, PacketType<T> type, BiConsumer<T, FriendlyByteBuf> write,
                                  Function<FriendlyByteBuf, T> read, BiConsumer<T, Supplier<NetworkEvent.Context>> handler, PlayNetworkDirection direction) {
        final SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                type.identifier,
                () -> "1",
                p -> true,
                p -> true
        );

        channel.registerMessage(1, packetClass, write::accept, read::apply, (message, ctx) -> {
            handler.accept(message, () -> ctx);
            ctx.setPacketHandled(true);
        }, Optional.ofNullable(direction));

        channels.put(type.identifier, channel);
    }

    static <T extends NeoPacket> void register(Class<T> packetClass, PacketType<T> type, BiConsumer<T, FriendlyByteBuf> write,
                                               Function<FriendlyByteBuf, T> read, BiConsumer<T, Supplier<NetworkEvent.Context>> handler, LoginNetworkDirection direction) {
        final SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                type.identifier,
                () -> "1",
                p -> true,
                p -> true
        );

        channel.registerMessage(1, packetClass, write::accept, read::apply, (message, ctx) -> {
            handler.accept(message, () -> ctx);
            ctx.setPacketHandled(true);
        }, Optional.ofNullable(direction));

        channels.put(type.identifier, channel);
    }

    PacketType<?> getType();

    void write(FriendlyByteBuf buf);
}
