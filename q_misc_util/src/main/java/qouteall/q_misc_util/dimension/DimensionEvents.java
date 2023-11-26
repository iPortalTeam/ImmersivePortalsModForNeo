package qouteall.q_misc_util.dimension;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.bus.api.Event;
import qouteall.q_misc_util.api.DimensionAPI;

import java.util.Set;

public abstract class DimensionEvents {

    /**
     * Will be triggered when the server dynamically add or remove a dimension.
     * Does not trigger during server initialization.
     */
    public static class ServerDimensionDynamicUpdateEvent extends Event {
        public final MinecraftServer server;
        public final Set<ResourceKey<Level>> dimensions;

        public ServerDimensionDynamicUpdateEvent(MinecraftServer server, Set<ResourceKey<Level>> dimensions) {
            this.server = server;
            this.dimensions = dimensions;
        }
    }

    /**
     * Will be triggered when the client receives dimension data synchronization
     */
    public static class ClientDimensionUpdateEvent extends Event {
        public final Set<ResourceKey<Level>> dimensions;

        public ClientDimensionUpdateEvent(Set<ResourceKey<Level>> dimensions) {
            this.dimensions = dimensions;
        }
    }

    /**
     * This event is fired when loading custom dimensions when the server is starting.
     * Inside this event, you can:
     * - use {@link MinecraftServer#registryAccess()} and {@link RegistryAccess#registryOrThrow(ResourceKey)} to access registries (including dimension type registry)
     * - use {@link MinecraftServer#getWorldData()} {@link WorldData#worldGenOptions()} to access world information like seed.
     * - use {@link DimensionAPI#addDimension(MinecraftServer, ResourceLocation, LevelStem)}.
     */
    public static class ServerDimensionsLoadEvent extends Event {
        public final MinecraftServer server;

        public ServerDimensionsLoadEvent(MinecraftServer server) {
            this.server = server;
        }
    }

    public static class BeforeRemovingDimensionEvent extends Event {
        public final ResourceKey<Level> dimension;

        public BeforeRemovingDimensionEvent(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }
}
