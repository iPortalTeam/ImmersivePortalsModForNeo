package networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class PacketType<T> {
    public final ResourceLocation identifier;
    final Function<FriendlyByteBuf, T> read;

    private PacketType(ResourceLocation identifier, Function<FriendlyByteBuf, T> read) {
        this.identifier = identifier;
        this.read = read;
    }

    public static <T> PacketType<T> create(ResourceLocation identifier, Function<FriendlyByteBuf, T> read) {
        return new PacketType<>(identifier, read);
    }
}
