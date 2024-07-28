package qouteall.imm_ptl.core.render;

import com.mojang.logging.LogUtils;
import de.nick1st.imm_ptl.events.ClientCleanupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// Note: this is sometimes effective but not always effective
// (maybe because of lighting or uploading delay?)
@OnlyIn(Dist.CLIENT)
public class ForceMainThreadRebuild {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static int forceMainThreadRebuildForFrames = 0;
    private static boolean currentFrameForceMainThreadRebuild = false;
    
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientCleanupEvent.class, e -> ForceMainThreadRebuild.reset());
    }
    
    private static void reset() {
        forceMainThreadRebuildForFrames = 0;
    }
    
    public static void onPreRender() {
        currentFrameForceMainThreadRebuild = forceMainThreadRebuildForFrames > 0;
        if (currentFrameForceMainThreadRebuild) {
            forceMainThreadRebuildForFrames--;
        }
        if (currentFrameForceMainThreadRebuild) {
            LOGGER.info("Forcing main thread rebuild in current frame");
        }
    }
    
    /**
     * Force the nearby chunks to rebuild
     */
    public static void forceMainThreadRebuildFor(int frameCount) {
        forceMainThreadRebuildForFrames = Math.max(forceMainThreadRebuildForFrames, frameCount);
    }
    
    public static boolean isCurrentFrameForceMainThreadRebuild() {
        return currentFrameForceMainThreadRebuild;
    }
}
