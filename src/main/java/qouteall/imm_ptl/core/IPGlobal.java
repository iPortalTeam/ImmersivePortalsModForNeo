package qouteall.imm_ptl.core;

import com.google.gson.Gson;
import me.shedaniel.autoconfig.ConfigHolder;
import net.neoforged.bus.api.Event;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.MyTaskList;

public class IPGlobal {
    
    public static ConfigHolder<IPConfig> configHolder;
    
    public static int maxNormalPortalRadius = 32;
    
    
    /**
     * It fires right after ticking client world, which is earlier than the Fabric event.
     */
    public static class PostClientTickEvent extends Event {}

    public static class PreGameRenderEvent extends Event {}
    
    // executed after ticking. will be cleared when client encounter loading screen
    public static final MyTaskList CLIENT_TASK_LIST = new MyTaskList();
    
    // won't be cleared
    public static final MyTaskList PRE_GAME_RENDER_TASK_LIST = new MyTaskList();
    public static final MyTaskList PRE_TOTAL_RENDER_TASK_LIST = new MyTaskList();

    public static final Gson gson = MiscHelper.gson;
    
    public static int maxPortalLayer = 5;
    
    public static int indirectLoadingRadiusCap = 8;
    
    public static boolean lagAttackProof = true;
    
    public static RenderMode renderMode = RenderMode.normal;
    
    public static boolean doCheckGlError = true;
    
    public static boolean renderYourselfInPortal = true;
    
    public static boolean activeLoading = true;
    
    public static int netherPortalFindingRadius = 128;
    
    public static boolean teleportationDebugEnabled = false;
    
    public static boolean correctCrossPortalEntityRendering = true;
    
    public static boolean disableTeleportation = false;
    
    public static boolean looseMovementCheck = false;
    
    public static boolean pureMirror = false;
    
    public static int portalRenderLimit = 200;
    
    public static boolean cacheGlBuffer = true;
    
    public static boolean reducedPortalRendering = false;
    
    public static boolean useSecondaryEntityVertexConsumer = true;
    
    public static boolean cullSectionsBehind = true;
    
    public static boolean offsetOcclusionQuery = true;
    
    public static boolean cloudOptimization = true;
    
    public static boolean crossPortalCollision = true;
    
    public static boolean netherPortalOverlay = false;
    
    public static boolean debugDisableFog = false;
    
    public static int scaleLimit = 30;
    
    public static boolean easeCreativePermission = true;
    public static boolean easeCommandStickPermission = true;
    
    public static boolean enableDepthClampForPortalRendering = true;
    
    public static boolean enableServerCollision = true;
    
    public static boolean enableSharedBlockMeshBuffers = true;
    
    public static boolean saveMemoryInBufferPack = true;
    
    public static boolean enableDatapackPortalGen = true;
    
    public static boolean enableCrossPortalView = true;
    
    public static boolean enableClippingMechanism = true;
    
    public static boolean enableWarning = true;
    
    public static boolean enableMirrorCreation = true;
    
    public static boolean lightVanillaNetherPortalWhenCrouching = true;
    
    public static boolean enableNetherPortalEffect = true;
    
    public static boolean tickOnlyIfChunkLoaded = true;
    
    public static boolean allowClientEntityPosInterpolation = true;
    
    public static boolean alwaysOverrideTerrainSetup = false;
    
    public static boolean viewBobbingReduce = true;
    
    public static boolean enableClientPerformanceAdjustment = true;
    public static boolean enableServerPerformanceAdjustment = true;
    
    public static boolean enableCrossPortalSound = true;
    
    public static boolean checkModInfoFromInternet = true;
    
    public static boolean enableUpdateNotification = true;
    
    public static boolean logClientPlayerCollidingPortalUpdate = false;
    
    public static boolean chunkPacketDebug = false;
    
    public static boolean entityUntrackDebug = false;
    public static boolean entityTrackDebug = false;
    public static boolean clientPortalLoadDebug = false;
    
    public static boolean debugRenderPortalShapeMesh = false;
    
    // make debug text not out-of-screen
    public static boolean moveDebugTextToTop = false;

    public static boolean boxPortalSpecialIteration = true;

    public static enum RenderMode {
        normal,
        compatibility,
        debug,
        none
    }
    
    // this should not be in core but the config is in core
    public static enum NetherPortalMode {
        normal,
        vanilla,
        adaptive,
        disabled
    }
    
    public static enum EndPortalMode {
        normal,
        toObsidianPlatform,
        scaledView,
        scaledViewRotating,
        vanilla
    }
    
    public static NetherPortalMode netherPortalMode = NetherPortalMode.adaptive;
    
    public static EndPortalMode endPortalMode = EndPortalMode.normal;
}
