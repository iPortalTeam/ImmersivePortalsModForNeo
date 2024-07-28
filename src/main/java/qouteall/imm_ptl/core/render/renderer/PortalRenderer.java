package qouteall.imm_ptl.core.render.renderer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.IPModInfoChecking;
import qouteall.imm_ptl.core.compat.iris_compatibility.ExperimentalIrisPortalRenderer;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisCompatibilityPortalRenderer;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisPortalRenderer;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.render.*;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public abstract class PortalRenderer {

    public static class PortalRenderingPredicateEvent extends Event {
        public final Portal portal;

        boolean canRender = true;

        public PortalRenderingPredicateEvent(Portal portal) {
            this.portal = portal;
        }

        public void setCanRender(boolean canRender) {
            this.canRender = this.canRender && canRender;
        }

        public boolean canRender() {
            return canRender;
        }
    }

    public static record PortalGroupToRender(
        PortalGroup group,
        List<Portal> portals
    ) implements PortalRenderable {
        @Override
        public PortalLike getPortalLike() {
            return group;
        }
    }
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public abstract void onBeforeTranslucentRendering(Matrix4f modelView);
    
    public abstract void onAfterTranslucentRendering(Matrix4f modelView);
    
    // will be called when rendering portal
    public abstract void onHandRenderingEnded();
    
    // will be called when rendering portal
    public void onBeforeHandRendering(Matrix4f modelView) {}
    
    // this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    // this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    // this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    // return true to skip framebuffer clear
    // this will also be called in outer world rendering
    public abstract boolean replaceFrameBufferClearing();
    
    protected List<PortalRenderable> getPortalsToRender(Matrix4f modelView) {
        Supplier<Frustum> frustumSupplier = Helper.cached(() -> {
            Frustum frustum = new Frustum(
                modelView,
                RenderSystem.getProjectionMatrix()
            );
            
            Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
            frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
            
            return frustum;
        });
        
        ObjectArrayList<PortalRenderable> renderables = new ObjectArrayList<>();
        
        ClientLevel world = client.level;
        assert world != null;
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
        for (Portal globalPortal : globalPortals) {
            if (!shouldSkipRenderingPortal(globalPortal, frustumSupplier)) {
                renderables.add(globalPortal);
            }
        }
        
        Object2ObjectOpenHashMap<PortalGroup, PortalGroupToRender> groupToRenderable =
            new Object2ObjectOpenHashMap<>();
        
        world.entitiesForRendering().forEach(e -> {
            if (e instanceof Portal portal) {
                if (!shouldSkipRenderingPortal(portal, frustumSupplier)) {
                    PortalLike renderingDelegate = portal.getRenderingDelegate();
                    
                    if (renderingDelegate instanceof PortalGroup portalGroup) {
                        // a portal group
                        if (groupToRenderable.containsKey(portalGroup)) {
                            groupToRenderable.get(portalGroup).portals.add(portal);
                        }
                        else {
                            PortalGroupToRender renderable = new PortalGroupToRender(
                                portalGroup,
                                Lists.newArrayList(portal)
                            );
                            groupToRenderable.put(portalGroup, renderable);
                            renderables.add(renderable);
                        }
                    }
                    else {
                        // a normal portal
                        renderables.add(portal);
                    }
                }
            }
        });
        
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        renderables.sort(Comparator.comparingDouble(
            e -> e.getPortalLike().getDistanceToNearestPointInPortal(cameraPos)
        ));
        return renderables;
    }
    
    private static boolean shouldSkipRenderingPortal(Portal portal, Supplier<Frustum> frustumSupplier) {
        if (!portal.isPortalValid()) {
            return true;
        }
        
        // if max portal layer is 0, the invisible portals will be force rendered
        if (!portal.isVisible() && IPGlobal.maxPortalLayer != 0) {
            return true;
        }
        
        if (RenderStates.getRenderedPortalNum() >= IPGlobal.portalRenderLimit) {
            return true;
        }
        
        Vec3 cameraPos = TransformationManager.getIsometricAdjustedCameraPos();
        
        if (!portal.isRoughlyVisibleTo(cameraPos)) {
            return true;
        }
        
        if (PortalRendering.isRendering()) {
            PortalLike outerPortal = PortalRendering.getRenderingPortal();
            
            if (outerPortal.cannotRenderInMe(portal)) {
                return true;
            }
        }
        
        double distance = portal.getDistanceToNearestPointInPortal(cameraPos);
        if (distance > getRenderRange()) {
            return true;
        }
        
        if (IPCGlobal.earlyFrustumCullingPortal) {
            // frustum culling does not work when portal is very close
            if (distance > 0.1) {
                Frustum frustum = frustumSupplier.get();
                if (!frustum.isVisible(portal.getThinBoundingBox())) {
                    return true;
                }
            }
        }
        
        if (PortalRendering.isInvalidRecursionRendering(portal)) {
            return true;
        }

        boolean predicateTest = NeoForge.EVENT_BUS.post(new PortalRenderingPredicateEvent(portal)).canRender();
        if (!predicateTest) {
            return true;
        }
        
        return false;
    }
    
    public static double getRenderRange() {
        double range = client.options.getEffectiveRenderDistance() * 16;
        if (RenderStates.isLaggy || IPGlobal.reducedPortalRendering) {
            range = 16;
        }
        if (PortalRendering.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalRendering.getPortalLayer());
        }
        if (PortalRendering.getPortalLayer() >= 1) {
            double outerPortalScale = PortalRendering.getRenderingPortal().getScale();
            if (outerPortalScale > 2) {
                range *= outerPortalScale;
                range = Math.min(range, 32 * 16);
            }
        }
        return range;
    }
    
    protected final void renderPortalContent(
        PortalRenderable portalRenderable
    ) {
        PortalLike portalLike = portalRenderable.getPortalLike();
        
        if (PortalRendering.getPortalLayer() > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        
        ClientLevel newWorld = ClientWorldLoader.getWorld(portalLike.getDestDim());
        
        PortalRendering.onBeginPortalWorldRendering();
        
        int renderDistance = getPortalRenderDistance(portalLike);
        
        invokeWorldRendering(
            new WorldRenderInfo.Builder()
                .setWorld(newWorld)
                .setCameraPos(PortalRendering.getRenderingCameraPos())
                .setCameraTransformation(portalLike.getAdditionalCameraTransformation())
                .setOverwriteCameraTransformation(false)
                .setDescription(portalLike.getDiscriminator())
                .setRenderDistance(renderDistance)
                .setDoRenderHand(false)
                .setEnableViewBobbing(true)
                .setDoRenderSky(!portalLike.isFuseView())
                .build()
        );
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager._enableDepthTest();
        
        MyRenderHelper.restoreViewPort();
        
        
    }
    
    private static int getPortalRenderDistance(PortalLike portal) {
        int mcRenderDistance = client.options.getEffectiveRenderDistance();
        
        if (portal.getScale() > 2) {
            double radiusBlocks = portal.getDestAreaRadiusEstimation() * 1.4;
            
            radiusBlocks = Math.min(radiusBlocks, 32 * 16);
            
            return Math.max((int) (radiusBlocks / 16), mcRenderDistance);
        }
        if (IPGlobal.reducedPortalRendering) {
            return mcRenderDistance / 3;
        }
        return mcRenderDistance;
    }
    
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            Runnable::run
        );
    }
    
    @Nullable
    public static Matrix4f getPortalTransformation(Portal portal) {
        Matrix4f rot = getPortalRotationMatrix(portal);
        
        Matrix4f mirror = portal instanceof Mirror ?
            TransformationManager.getMirrorTransformation(portal.getNormal()) : null;
        
        Matrix4f scale = getPortalScaleMatrix(portal);
        
        return combineNullable(rot, combineNullable(mirror, scale));
    }
    
    @Nullable
    public static Matrix4f getPortalRotationMatrix(Portal portal) {
        if (portal.getRotation() == null) {
            return null;
        }
        
        Quaternionf rot = portal.getRotation().toMcQuaternion();
        rot.conjugate();
        return rot.get(new Matrix4f());
    }
    
    @Nullable
    public static Matrix4f combineNullable(@Nullable Matrix4f a, @Nullable Matrix4f b) {
        return Helper.combineNullable(a, b, (m1, m2) -> {
            m1.mul(m2);
            return m1;
        });
    }
    
    @Nullable
    public static Matrix4f getPortalScaleMatrix(Portal portal) {
        // if it's not a fuseView portal
        // whether to apply scale transformation to camera does not change triangle position
        // to avoid abrupt fog change, do not apply for non-fuse-view portal
        // for fuse-view portal, the depth value should be correct so the scale should be applied
        if (shouldApplyScaleToModelView(portal)) {
            float v = (float) (1.0 / portal.getScale());
            return new Matrix4f().scale(v, v, v);
        }
        return null;
    }
    
    public static boolean shouldApplyScaleToModelView(PortalLike portal) {
        return portal.hasScaling() && portal.isFuseView();
    }
    
    public void onBeginIrisTranslucentRendering(Matrix4f modelView) {}
    
    private static boolean fabulousWarned = false;
    
    public static void switchToCorrectRenderer() {
        if (PortalRendering.isRendering()) {
            //do not switch when rendering
            return;
        }
        
        if (Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS) {
            if (!fabulousWarned) {
                fabulousWarned = true;
                CHelper.printChat(Component.translatable("imm_ptl.fabulous_warning"));
            }
        }
        
        IPModInfoChecking.checkShaderpack();
        
        if (IrisInterface.invoker.isIrisPresent()) {
            if (IrisInterface.invoker.isShaders()) {
                if (IPCGlobal.experimentalIrisPortalRenderer) {
                    switchRenderer(ExperimentalIrisPortalRenderer.instance);
                    return;
                }
                
                switch (IPGlobal.renderMode) {
                    case normal -> switchRenderer(IrisPortalRenderer.instance);
                    case compatibility -> switchRenderer(IrisCompatibilityPortalRenderer.instance);
                    case debug -> switchRenderer(IrisCompatibilityPortalRenderer.debugModeInstance);
                    case none -> switchRenderer(IPCGlobal.rendererDummy);
                }
                return;
            }
        }
        
        switch (IPGlobal.renderMode) {
            case normal -> switchRenderer(IPCGlobal.rendererUsingStencil);
            case compatibility -> switchRenderer(IPCGlobal.rendererUsingFrameBuffer);
            case debug -> switchRenderer(IPCGlobal.rendererDebug);
            case none -> switchRenderer(IPCGlobal.rendererDummy);
        }
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (IPCGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            IPCGlobal.renderer = renderer;
            
            if (IrisInterface.invoker.isShaders()) {
                IrisInterface.invoker.reloadPipelines();
            }
        }
    }
}
