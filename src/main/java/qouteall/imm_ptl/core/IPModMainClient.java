package qouteall.imm_ptl.core;

import de.nick1st.imm_ptl.events.ClientCleanupEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import qouteall.imm_ptl.core.collision.CollisionHelper;
import qouteall.imm_ptl.core.commands.ClientDebugCommand;
import qouteall.imm_ptl.core.compat.IPFlywheelCompat;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.miscellaneous.DubiousThings;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.network.ImmPtlNetworkConfig;
import qouteall.imm_ptl.core.network.ImmPtlNetworking;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.portal.animation.ClientPortalAnimationManagement;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.ForceMainThreadRebuild;
import qouteall.imm_ptl.core.render.GuiPortalRendering;
import qouteall.imm_ptl.core.render.ImmPtlViewArea;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;
import qouteall.imm_ptl.core.render.VisibleSectionDiscovery;
import qouteall.imm_ptl.core.render.context_management.CloudContext;
import qouteall.imm_ptl.core.render.optimization.GLResourceCache;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.imm_ptl.core.render.renderer.RendererUsingFrameBuffer;
import qouteall.imm_ptl.core.render.renderer.RendererUsingStencil;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.q_misc_util.dimension.DimensionIntId;
import qouteall.q_misc_util.my_util.MyTaskList;

public class IPModMainClient {
    
    private static void showNvidiaVideoCardWarning() {
        IPGlobal.CLIENT_TASK_LIST.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (IPMcHelper.isNvidiaVideocard()) {
                    if (!SodiumInterface.invoker.isSodiumPresent()) {
                        CHelper.printChat(
                            Component.translatable("imm_ptl.nvidia_warning")
                                .withStyle(ChatFormatting.RED)
                                .append(McHelper.getLinkText("https://github.com/CaffeineMC/sodium-fabric/issues/1486"))
                        );
                    }
                }
            })
        ));
    }
    
    private static void showQuiltWarning() {
        IPGlobal.CLIENT_TASK_LIST.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (O_O.isQuilt()) {
                    if (IPConfig.getConfig().shouldDisplayWarning("quilt")) {
                        CHelper.printChat(
                            Component.translatable("imm_ptl.quilt_warning")
                                .append(IPMcHelper.getDisableWarningText("quilt"))
                        );
                    }
                }
            })
        ));
    }
    
    public static void init() {
        ClientWorldLoader.init();
        
        ClientTeleportationManager.init();
        
        Minecraft.getInstance().execute(() -> {
            ShaderCodeTransformation.init();
            
            MyRenderHelper.init();
            
            IPCGlobal.rendererUsingStencil = new RendererUsingStencil();
            IPCGlobal.rendererUsingFrameBuffer = new RendererUsingFrameBuffer();
            
            IPCGlobal.renderer = IPCGlobal.rendererUsingStencil;
        });
        
        DubiousThings.init();
        
        CrossPortalEntityRenderer.init();
        
        GLResourceCache.init();
        
        CollisionHelper.initClient();
        
        PortalRenderInfo.init();
        
        CloudContext.init();
        
        SharedBlockMeshBuffers.init();
        
        GcMonitor.initClient();

        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> {
            ClientDebugCommand.register(event.getDispatcher());
        });
        
//        showIntelVideoCardWarning();
        
        showNvidiaVideoCardWarning();
        
        showQuiltWarning();
        
        StableClientTimer.init();
        
        ClientPortalAnimationManagement.init();
        
        VisibleSectionDiscovery.init();
        
        ImmPtlViewArea.init();
        
        IPFlywheelCompat.init();
    
        GuiPortalRendering._init();
        
        ImmPtlNetworking.initClient();
        ImmPtlNetworkConfig.initClient();
        
        ForceMainThreadRebuild.init();

        NeoForge.EVENT_BUS.addListener(ClientCleanupEvent.class, e -> {
            IPGlobal.CLIENT_TASK_LIST.forceClearTasks();
        });

        DimensionIntId.initClient();
    }
    
}
