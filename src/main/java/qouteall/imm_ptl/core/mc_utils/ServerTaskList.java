package qouteall.imm_ptl.core.mc_utils;

import de.nick1st.imm_ptl.events.ServerCleanupEvent;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.q_misc_util.my_util.MyTaskList;

public class ServerTaskList {
    public static void init() {
        NeoForge.EVENT_BUS.addListener(TickEvent.ServerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                of(event.getServer()).processTasks();
            }
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
