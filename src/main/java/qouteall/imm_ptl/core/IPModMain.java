package qouteall.imm_ptl.core;

import com.mojang.logging.LogUtils;
import de.nick1st.imm_ptl.networking.Payloads;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;
import qouteall.imm_ptl.core.chunk_loading.EntitySync;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTickets;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.chunk_loading.ServerPerformanceMonitor;
import qouteall.imm_ptl.core.chunk_loading.WorldInfoSender;
import qouteall.imm_ptl.core.collision.CollisionHelper;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.debug.DebugUtil;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.network.ImmPtlNetworkConfig;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.BreakableMirror;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.animation.NormalAnimation;
import qouteall.imm_ptl.core.portal.animation.RotationAnimation;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManager;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.GlobalTrackedPortal;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.imm_ptl.core.portal.global_portals.WorldWrappingPortal;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalEntity;
import qouteall.imm_ptl.core.portal.shape.BoxPortalShape;
import qouteall.imm_ptl.core.portal.shape.RectangularPortalShape;
import qouteall.imm_ptl.core.portal.shape.SpecialFlatPortalShape;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.imm_ptl.peripheral.platform_specific.IPFeatureControl;
import qouteall.q_misc_util.Helper;

import java.io.File;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class IPModMain {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void init(IEventBus eventBus) {
        loadConfig();
        
        Helper.LOGGER.info("Immersive Portals Mod Initializing");

        eventBus.addListener(RegisterPayloadHandlersEvent.class, Payloads::register);
        ImmPtlNetworkConfig.init(eventBus);

        NeoForge.EVENT_BUS.addListener(IPGlobal.PostClientTickEvent.class, postClientTickEvent -> IPGlobal.CLIENT_TASK_LIST.processTasks());

        NeoForge.EVENT_BUS.addListener(IPGlobal.PreGameRenderEvent.class, preGameRenderEvent -> IPGlobal.PRE_GAME_RENDER_TASK_LIST.processTasks());
        
        RectangularPortalShape.init();
        SpecialFlatPortalShape.init();
        BoxPortalShape.init();
        
        ImmPtlChunkTracking.init();
        
        WorldInfoSender.init();
        
        GlobalPortalStorage.init();
        
        EntitySync.init();
        
        ServerTeleportationManager.init();
        
        CollisionHelper.init();
        
        PortalExtension.init();
        
        GcMonitor.initCommon();
        
        ServerPerformanceMonitor.init();
        
        ImmPtlChunkTickets.init();
        
        IPPortingLibCompat.init();
        
        BlockManipulationServer.init();

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> {
            PortalCommand.register(event.getDispatcher(), event.getBuildContext());
        });

        // @Nick1st moved to IPModEntry (those are registry functions)
//        SubCommandArgumentType.init();
//        TimingFunctionArgumentType.init();
//        AxisArgumentType.init();

        DebugUtil.init();
        
        ServerTaskList.init();

        CustomPortalGenManager.init(eventBus);

        // intrinsic animation driver types
        RotationAnimation.init();
        NormalAnimation.init();
//        OscillationAnimation.init();

        if (!IPFeatureControl.enableVanillaBehaviorChangingByDefault()) {
            LOGGER.info("""
                iPortal is provided by jar-in-jar.
                The default value of 'nether portal mode', 'end portal mode' and 'enable mirror creation' in config will be respectively vanilla, vanilla and false.
                (The default value is only used when config file is not present of missing fields. This does not change existing config.)
                """);
        }
    }
    
    private static void loadConfig() {
        // upgrade old config
        Path gameDir = O_O.getGameDir();
        File oldConfigFile = gameDir.resolve("config").resolve("immersive_portals_fabric.json").toFile();
        if (oldConfigFile.exists()) {
            File dest = gameDir.resolve("config").resolve("immersive_portals.json").toFile();
            boolean succeeded = oldConfigFile.renameTo(dest);
            if (succeeded) {
                Helper.log("Upgraded old config file");
            }
            else {
                Helper.err("Failed to upgrade old config file");
            }
        }
        
        Helper.log("Loading Immersive Portals config");
        IPGlobal.configHolder = AutoConfig.register(IPConfig.class, GsonConfigSerializer::new);
        IPGlobal.configHolder.registerSaveListener((configHolder, ipConfig) -> {
            ipConfig.onConfigChanged();
            return InteractionResult.SUCCESS;
        });
        IPConfig ipConfig = IPConfig.getConfig();
        ipConfig.onConfigChanged();
    }

    public static void registerBlocksForge(RegisterEvent.RegisterHelper<Block> registerHelper) {
        registerBlocks(registerHelper::register);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, PortalPlaceholderBlock> regFunc) {
        regFunc.accept(
            new ResourceLocation("immersive_portals", "nether_portal_block"),
            PortalPlaceholderBlock.instance
        );
    }

    public static void registerEntityTypesForge(RegisterEvent.RegisterHelper<EntityType<?>> registerHelper) {
        registerEntityTypes(registerHelper::register);
    }

    public static void registerEntityTypes(BiConsumer<ResourceLocation, EntityType<?>> regFunc) {

        regFunc.accept(
            new ResourceLocation("immersive_portals", "portal"),
            Portal.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "nether_portal_new"),
            NetherPortalEntity.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "end_portal"),
            EndPortalEntity.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "mirror"),
            Mirror.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "breakable_mirror"),
            BreakableMirror.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "global_tracked_portal"),
            GlobalTrackedPortal.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "border_portal"),
            WorldWrappingPortal.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "end_floor_portal"),
            VerticalConnectingPortal.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "general_breakable_portal"),
            GeneralBreakablePortal.ENTITY_TYPE
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "loading_indicator"),
            LoadingIndicatorEntity.entityType
        );
    }
}
