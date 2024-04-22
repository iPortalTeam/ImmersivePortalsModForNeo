package qouteall.imm_ptl.core.mc_utils;

import de.nick1st.imm_ptl.events.ServerCleanupEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.q_misc_util.my_util.MyTaskList;

public class ServerTaskList {
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            of(server).processTasks();
        });

        NeoForge.EVENT_BUS.addListener(ServerCleanupEvent.class, event -> {
            MinecraftServer server = event.server;
            of(server).forceClearTasks();
        });
    }
    
    // the tasks are executed after ticking. will be cleared when server closes
    public static MyTaskList of(MinecraftServer server) {
        return IPPerServerInfo.of(server).taskList;
    }
}
