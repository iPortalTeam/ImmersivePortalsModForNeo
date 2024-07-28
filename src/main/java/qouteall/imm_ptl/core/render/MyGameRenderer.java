package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import de.nick1st.imm_ptl.events.ClientCleanupEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.mixin.client.render.IESectionRenderDispatcher;
import qouteall.imm_ptl.core.render.context_management.*;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.util.Stack;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class MyGameRenderer {
    public static final Minecraft client = Minecraft.getInstance();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    public static final int MAX_SECONDARY_BUFFER_NUM = 2;
    
    // portal rendering and outer world rendering uses different buffer builder storages
    private static Stack<RenderBuffers> secondaryRenderBuffers = new Stack<>();
    private static int usingRenderBuffersObjectNum = 0;
    
    // the vanilla visibility sections discovery code is multi-threaded
    // when the player teleports through a portal, on the first frame it will not work normally
    // so use IP's non-multi-threaded algorithm at the first frame
    public static int vanillaTerrainSetupOverride = 0;
    
    public static boolean enablePortalCaveCulling = true;
    
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientCleanupEvent.class, e -> secondaryRenderBuffers.clear());
    }
    
    @Nullable
    private static RenderBuffers acquireRenderBuffersObject() {
        if (usingRenderBuffersObjectNum >= MAX_SECONDARY_BUFFER_NUM) {
            return null;
        }
        usingRenderBuffersObjectNum++;
        
        if (secondaryRenderBuffers.isEmpty()) {
            return new RenderBuffers(0);
        }
        else {
            return secondaryRenderBuffers.pop();
        }
    }
    
    private static void returnRenderBuffersObject(RenderBuffers renderBuffers) {
        usingRenderBuffersObjectNum--;
        secondaryRenderBuffers.push(renderBuffers);
    }
    
    public static void renderWorldNew(
        WorldRenderInfo worldRenderInfo,
        Consumer<Runnable> invokeWrapper
    ) {
        WorldRenderInfo.pushRenderInfo(worldRenderInfo);
        
        switchAndRenderTheWorld(
            worldRenderInfo.world,
            worldRenderInfo.cameraPos,
            worldRenderInfo.cameraPos,
            invokeWrapper,
            worldRenderInfo.renderDistance,
            worldRenderInfo.doRenderHand
        );
        
        WorldRenderInfo.popRenderInfo();
    }
    
    private static void switchAndRenderTheWorld(
        ClientLevel newWorld,
        Vec3 thisTickCameraPos,
        Vec3 lastTickCameraPos,
        Consumer<Runnable> invokeWrapper,
        int renderDistance,
        boolean doRenderHand
    ) {
        if (!enablePortalCaveCulling) {
            client.smartCull = false;
        }
        
        if (!PortalRendering.shouldEnableSodiumCaveCulling()) {
            client.smartCull = false;
        }
        
        ResourceKey<Level> newDimension = newWorld.dimension();
        
        LevelRenderer worldRenderer = ClientWorldLoader.getWorldRenderer(newDimension);
        
        CHelper.checkGlError();
        
        float tickDelta = RenderStates.getPartialTick();
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) client.gameRenderer;
        DimensionRenderHelper helper =
            ClientWorldLoader.getDimensionRenderHelper(newDimension);
        Camera newCamera = new Camera();
        
        // store old state
        ClientLevel oldWorld = client.level;
        LevelRenderer oldWorldRenderer = client.levelRenderer;
        LightTexture oldLightmap = client.gameRenderer.lightTexture();
        boolean oldNoClip = client.player.noPhysics;
        boolean oldDoRenderHand = ieGameRenderer.ip_getDoRenderHand();
        ObjectArrayList<SectionRenderDispatcher.RenderSection> oldChunkInfoList =
            ((IEWorldRenderer) oldWorldRenderer).portal_getChunkInfoList();
        HitResult oldCrosshairTarget = client.hitResult;
        Camera oldCamera = client.gameRenderer.getMainCamera();
        PostChain oldTransparencyShader = ((IEWorldRenderer) worldRenderer).portal_getTransparencyShader();
        RenderBuffers oldRenderBuffers = ((IEWorldRenderer) worldRenderer).ip_getRenderBuffers();
        RenderBuffers oldClientRenderBuffers = client.renderBuffers();
        SectionBufferBuilderPack oldSectionRenderDispatcherFixedBuffers =
            ((IESectionRenderDispatcher) worldRenderer.getSectionRenderDispatcher())
                .ip_getFixedBuffers();
        Frustum oldFrustum = ((IEWorldRenderer) worldRenderer).portal_getFrustum();
        
        // the projection matrix contains view bobbing.
        // the view bobbing is related with scale
        Matrix4f oldProjectionMatrix = RenderSystem.getProjectionMatrix();
        
        ObjectArrayList<SectionRenderDispatcher.RenderSection> newChunkInfoList = VisibleSectionDiscovery.takeList();
        ((IEWorldRenderer) oldWorldRenderer).portal_setChunkInfoList(newChunkInfoList);
        
        Object irisPipeline = IrisInterface.invoker.getPipeline(worldRenderer);
        
        // switch (note: it will no longer switch the world that client player is in )
        ((IEMinecraftClient) client).ip_setWorldRenderer(worldRenderer);
        client.level = newWorld;
        ieGameRenderer.ip_setLightmapTextureManager(helper.lightmapTexture);
        
        client.getBlockEntityRenderDispatcher().level = newWorld;
        client.player.noPhysics = true;
        client.gameRenderer.setRenderHand(doRenderHand);
        
        FogRendererContext.swappingManager.pushSwapping(newDimension);
        ((IEParticleManager) client.particleEngine).ip_setWorld(newWorld);
        if (BlockManipulationClient.remotePointedDim == newDimension) {
            client.hitResult = BlockManipulationClient.remoteHitResult;
        }
        if (!PortalRendering.shouldRenderHitResult()) {
            client.hitResult = null;
        }
        ieGameRenderer.ip_setCamera(newCamera);
        
        RenderBuffers newRenderBuffers = null;
        if (IPGlobal.useSecondaryEntityVertexConsumer) {
            newRenderBuffers = acquireRenderBuffersObject();
            if (newRenderBuffers != null) {
                ((IEWorldRenderer) worldRenderer).ip_setRenderBuffers(newRenderBuffers);
                ((IEMinecraftClient) client).ip_setRenderBuffers(newRenderBuffers);
                
                /*
                  the vanilla buffer pack may be used by {@link net.minecraft.client.renderer.MultiBufferSource.BufferSource}
                  The BufferSource does not always immediately finish building.
                  Reusing that may cause "Already Building" error in Buffer Builder when doing main-thread chunk rebuilding.
                  This does not occur in vanilla because vanilla does main-thread chunk rebuilding before entity rendering. With portal rendering it could do chunk rebuilding after some entity rendering.
                 */
                ((IESectionRenderDispatcher) worldRenderer.getSectionRenderDispatcher())
                    .ip_setFixedBuffers(newRenderBuffers.fixedBufferPack());
            }
            else{
                // draw the content in the buffers,
                // to avoid messing with content in the portals
                // TODO it may draw with wrong stencil func here
                client.renderBuffers().bufferSource().endBatch();
            }
        }
        
        Object newSodiumContext = SodiumInterface.invoker.createNewContext(renderDistance);
        SodiumInterface.invoker.switchContextWithCurrentWorldRenderer(newSodiumContext);
        
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(null);
        
        IrisInterface.invoker.setPipeline(worldRenderer, null);
        
        //update lightmap
        if (!RenderStates.isDimensionRendered(newDimension)) {
            helper.lightmapTexture.updateLightTexture(0);
        }
        
        //invoke rendering
        invokeWrapper.accept(() -> {
            client.getProfiler().push("render_portal_content");
            client.gameRenderer.renderLevel(
                tickDelta,
                Util.getNanos()
            );
            client.getProfiler().pop();
        });
        
        SodiumInterface.invoker.switchContextWithCurrentWorldRenderer(newSodiumContext);
        
        //recover
        
        ((IEMinecraftClient) client).ip_setWorldRenderer(oldWorldRenderer);
        client.level = oldWorld;
        ieGameRenderer.ip_setLightmapTextureManager(oldLightmap);
        client.getBlockEntityRenderDispatcher().level = oldWorld;
        client.player.noPhysics = oldNoClip;
        client.gameRenderer.setRenderHand(oldDoRenderHand);
        
        ((IEParticleManager) client.particleEngine).ip_setWorld(oldWorld);
        client.hitResult = oldCrosshairTarget;
        ieGameRenderer.ip_setCamera(oldCamera);
        
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(oldTransparencyShader);
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).portal_setChunkInfoList(oldChunkInfoList);
        VisibleSectionDiscovery.returnList(newChunkInfoList);
        
        ((IEWorldRenderer) worldRenderer).ip_setRenderBuffers(oldRenderBuffers);
        ((IEMinecraftClient) client).ip_setRenderBuffers(oldClientRenderBuffers);
        ((IESectionRenderDispatcher) worldRenderer.getSectionRenderDispatcher())
            .ip_setFixedBuffers(oldSectionRenderDispatcherFixedBuffers);
        if (newRenderBuffers != null) {
            returnRenderBuffersObject(newRenderBuffers);
        }
        
        ((IEWorldRenderer) worldRenderer).portal_setFrustum(oldFrustum);
        
        client.gameRenderer.resetProjectionMatrix(oldProjectionMatrix);
        
        IrisInterface.invoker.setPipeline(worldRenderer, irisPipeline);
        
        client.getEntityRenderDispatcher()
            .prepare(
                client.level,
                oldCamera,
                client.crosshairPickEntity
            );
        
        CHelper.checkGlError();
        
        client.smartCull = true;
    }
    
    /**
     * {@link LevelRenderer#renderLevel}
     */
    @IPVanillaCopy
    public static void resetFogState() {
        Camera camera = client.gameRenderer.getMainCamera();
        float g = client.gameRenderer.getRenderDistance();
        
        Vec3 cameraPos = camera.getPosition();
        double x = cameraPos.x();
        double y = cameraPos.y();
        double z = cameraPos.z();
        
        boolean isFoggy = client.level.effects().isFoggyAt(Mth.floor(x), Mth.floor(y)) ||
            client.gui.getBossOverlay().shouldCreateWorldFog();
        
        FogRenderer.setupFog(
            camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(g, 32.0F), isFoggy, RenderStates.getPartialTick()
        );
        FogRenderer.levelFogColor();
    }
    
    public static void updateFogColor() {
        FogRenderer.setupColor(
            client.gameRenderer.getMainCamera(),
            RenderStates.getPartialTick(),
            client.level,
            client.options.getEffectiveRenderDistance(),
            client.gameRenderer.getDarkenWorldAmount(RenderStates.getPartialTick())
        );
    }
    
    /**
     * {@link LevelRenderer#renderLevel}
     */
    @IPVanillaCopy
    public static void resetDiffuseLighting() {
        ClientLevel world = client.level;
        assert world != null;
        if (world.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        }
        else {
            Lighting.setupLevel();
        }
    }
    
    
}
