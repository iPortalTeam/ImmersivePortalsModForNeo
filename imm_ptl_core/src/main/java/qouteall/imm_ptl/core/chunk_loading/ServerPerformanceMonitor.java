package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;

public class ServerPerformanceMonitor {
    
    private static final int sampleNum = 20;
    
    private static PerformanceLevel level = PerformanceLevel.bad;
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(50);
    
    public static void init() {
        NeoForge.EVENT_BUS.addListener(TickEvent.ServerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                ServerPerformanceMonitor.tick(event.getServer());
            }
        });
    }
    
    private static long lastUpdateTime = 0;
    
    private static void tick(MinecraftServer server) {
        if (!IPGlobal.enableServerPerformanceAdjustment) {
            level = PerformanceLevel.good;
            return;
        }
        
        if (!server.isRunning()) {
            return;
        }
        
        long currTime = System.nanoTime();
        
        // update every 20 seconds
        if (currTime - lastUpdateTime < Helper.secondToNano(20)) {
            return;
        }
        else {
            lastUpdateTime = currTime;
        }
        
        float tickTime = server.getAverageTickTime();
        PerformanceLevel newLevel = PerformanceLevel.getServerPerformanceLevel(tickTime);
        if (newLevel != level) {
            level = newLevel;
            limitedLogger.log("Server performance level: " + newLevel);
        }
    }
    
    public static PerformanceLevel getLevel() {
        return level;
    }
    
}
