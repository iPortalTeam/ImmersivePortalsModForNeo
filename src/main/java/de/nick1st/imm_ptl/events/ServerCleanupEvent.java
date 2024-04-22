package de.nick1st.imm_ptl.events;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

public class ServerCleanupEvent extends Event {
    public final MinecraftServer server;

    public ServerCleanupEvent(MinecraftServer server) {
        this.server = server;
    }
}
