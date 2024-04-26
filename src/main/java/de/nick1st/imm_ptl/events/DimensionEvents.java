package de.nick1st.imm_ptl.events;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

import java.util.List;

public abstract class DimensionEvents {
    public static class ServerDimensionsLoadEvent extends Event {
        public MinecraftServer server;

        public ServerDimensionsLoadEvent(MinecraftServer server) {
            this.server = server;
        }
    }

    public static class BeforeRemovingDimensionEvent extends Event {
        private MinecraftServer server;
        public ServerLevel dimension;

        public MinecraftServer getServer() {
            return server;
        }

        public BeforeRemovingDimensionEvent(MinecraftServer server, ServerLevel dimension) {
            this.server = server;
            this.dimension = dimension;
        }
    }

    public static class CLIENT_DIMENSION_DYNAMIC_REMOVE_EVENT extends Event {
        public ResourceKey<Level> level;

        public CLIENT_DIMENSION_DYNAMIC_REMOVE_EVENT(ResourceKey<Level> level) {
            this.level = level;
        }
    }

    public static class CLIENT_WORLD_LOAD_EVENT extends Event {
        public ClientLevel level;

        public CLIENT_WORLD_LOAD_EVENT(ClientLevel level) {
            this.level = level;
        }
    }

    public static class CLIENT_DIMENSION_UPDATE_EVENT extends Event {
        public List<ResourceKey<Level>> dimensions;
        public CLIENT_DIMENSION_UPDATE_EVENT(List<ResourceKey<Level>> dimensions) {
            this.dimensions = dimensions;
        }
    }
}
