package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
import static org.lwjgl.opengl.GL11.GL_INCR;
import static org.lwjgl.opengl.GL11.GL_KEEP;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glStencilFunc;
import static org.lwjgl.opengl.GL11.glStencilOp;

public class IrisPortalRenderer extends PortalRenderer {
    public static final IrisPortalRenderer instance = new IrisPortalRenderer();
    
    
    private SecondaryFrameBuffer[] deferredFbs = new SecondaryFrameBuffer[0];
    
    private boolean portalRenderingNeeded = false;
    private boolean nextFramePortalRenderingNeeded = false;
    
    IrisPortalRenderer() {
        NeoForge.EVENT_BUS.addListener(IPGlobal.PreGameRenderEvent.class, preGameRenderEvent -> {
            updateNeedsPortalRendering();
        });
    }
    
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    @Override
    public void prepareRendering() {
        Validate.isTrue(!PortalRendering.isRendering());
    
        // As I tested, in Nvidia videocard, glCopyImageSubData can convert depth32 into depth24stencil8.
        // but in AMD videocard it cannot. AMD videocard only supports converting depth32 into depth32stencil8.
        IPCGlobal.useSeparatedStencilFormat = !IPMcHelper.isNvidiaVideocard();

        // Size the array off the static configured max, not PortalRendering.getMaxPortalLayer()
        // (which drops to 1 whenever RenderStates.isLaggy is true). Sizing off the dynamic value
        // caused a destroy-and-recreate of every deferred framebuffer each time isLaggy toggled,
        // which is itself expensive enough to cause more lag -- a self-sustaining stutter cycle.
        // The lag-adaptive behavior still works: it just limits how many of these pre-allocated
        // slots actually get rendered into, rather than deallocating and reallocating them.
        if (deferredFbs.length != IPGlobal.maxPortalLayer + 1) {
            for (SecondaryFrameBuffer fb : deferredFbs) {
                fb.fb.destroyBuffers();
            }

            deferredFbs = new SecondaryFrameBuffer[IPGlobal.maxPortalLayer + 1];
            for (int i = 0; i < deferredFbs.length; i++) {
                deferredFbs[i] = new SecondaryFrameBuffer();
            }

            // The vendor heuristic above doesn't reliably match what the main render target's
            // depth texture actually ends up as once Iris has touched it (a GPU-vendor-detection
            // timing issue relative to when the main target's buffers were allocated). Query its
            // real current internal format and let that override the heuristic, so the newly
            // (re)created deferred FBs below match reality instead of a guess. Only needs doing
            // when the FBs are (re)created, not every frame, since the format is baked in once.
            RenderTarget mainTargetForFormatCheck = client.getMainRenderTarget();
            int mainDepthTexForFormatCheck = mainTargetForFormatCheck.getDepthTextureId();
            if (mainDepthTexForFormatCheck > 0) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainDepthTexForFormatCheck);
                int mainFormatForFormatCheck = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                if (mainFormatForFormatCheck == GL30.GL_DEPTH32F_STENCIL8) {
                    IPCGlobal.useSeparatedStencilFormat = true;
                } else if (mainFormatForFormatCheck == GL30.GL_DEPTH24_STENCIL8) {
                    IPCGlobal.useSeparatedStencilFormat = false;
                }
            }
        }

        CHelper.checkGlError();

        for (SecondaryFrameBuffer deferredFb : deferredFbs) {
            deferredFb.prepare();
            IPPortingLibCompat.setIsStencilEnabled(deferredFb.fb, true);
            
            deferredFb.fb.bindWrite(true);
            GlStateManager._clearColor(1, 0, 1, 0);
            GlStateManager._clearDepth(1);
            GlStateManager._clearStencil(0);
            GL11.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
            
            deferredFb.fb.checkStatus();
            
            CHelper.checkGlError();
            
            deferredFb.fb.unbindWrite();
        }
    
        IPPortingLibCompat.setIsStencilEnabled(client.getMainRenderTarget(), false);
        
        // Iris now use vanilla framebuffer's depth
        client.getMainRenderTarget().bindWrite(false);
    }
    
    private void updateNeedsPortalRendering() {
        portalRenderingNeeded = nextFramePortalRenderingNeeded;
        nextFramePortalRenderingNeeded = false;
    }
    
    @Override
    public void onBeforeHandRendering(Matrix4f modelView) {
        doMainRenderings(modelView);
    }
    
    private void doMainRenderings(Matrix4f modelView) {
        CHelper.checkGlError();
        
        RenderTarget mcFrameBuffer = client.getMainRenderTarget();
        int portalLayer = PortalRendering.getPortalLayer();
        
        if (portalRenderingNeeded) {
            CHelper.doCheckGlError();
            
            // copy depth from mc fb to deferred fb
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mcFrameBuffer.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredFbs[portalLayer].fb.frameBufferId);
            GL30.glBlitFramebuffer(
                0, 0, mcFrameBuffer.viewWidth, mcFrameBuffer.viewHeight,
                0, 0, mcFrameBuffer.viewWidth, mcFrameBuffer.viewHeight,
                GL_DEPTH_BUFFER_BIT, GL_NEAREST
            );
            
            int errorCode = GL11.glGetError();
            if (errorCode != GL_NO_ERROR) {
                int mainDepthTex = mcFrameBuffer.getDepthTextureId();
                int deferredDepthTex = deferredFbs[portalLayer].fb.getDepthTextureId();
                int mainInternalFormat = 0;
                int deferredInternalFormat = 0;
                if (mainDepthTex > 0) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainDepthTex);
                    mainInternalFormat = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
                }
                if (deferredDepthTex > 0) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, deferredDepthTex);
                    deferredInternalFormat = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
                }
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                qouteall.q_misc_util.Helper.LOGGER.error(
                    "[ImmPtl] Depth blit from main framebuffer ({}x{}, depthTex={}, internalFormat=0x{}) " +
                        "to deferred framebuffer ({}x{}, depthTex={}, internalFormat=0x{}) failed with GL error 0x{}",
                    mcFrameBuffer.viewWidth, mcFrameBuffer.viewHeight,
                    mainDepthTex, Integer.toHexString(mainInternalFormat),
                    deferredFbs[portalLayer].fb.viewWidth, deferredFbs[portalLayer].fb.viewHeight,
                    deferredDepthTex, Integer.toHexString(deferredInternalFormat),
                    Integer.toHexString(errorCode)
                );
                IPGlobal.renderMode = IPGlobal.RenderMode.compatibility;
                CHelper.printChat("[Immersive Portals]" +
                    "Switched to compatibility portal rendering mode." +
                    " Portal-in-portal wont' be rendered");
            }
            
            initStencilForLayer(portalLayer);
            
            deferredFbs[portalLayer].fb.bindWrite(true);
            
            glEnable(GL_STENCIL_TEST);
            glStencilFunc(GL_EQUAL, portalLayer, 0xFF);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
            
            // draw from mc fb into deferred fb within stencil
            MyRenderHelper.drawScreenFrameBuffer(mcFrameBuffer, false, true);
            
            glDisable(GL_STENCIL_TEST);
            
            deferredFbs[portalLayer].fb.unbindWrite();
            
            mcFrameBuffer.bindWrite(false);
        }
        
        renderPortals(modelView);
        
        if (portalLayer == 0) {
            finish();
        }
        
        mcFrameBuffer.bindWrite(true);
    }
    
    @Override
    public void onHandRenderingEnded() {
    
    }
    
    private void initStencilForLayer(int portalLayer) {
        if (portalLayer == 0) {
            deferredFbs[portalLayer].fb.bindWrite(true);
            GlStateManager._clearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        }
        else {
            CHelper.checkGlError();
            
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, deferredFbs[portalLayer - 1].fb.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredFbs[portalLayer].fb.frameBufferId);
            
            GL30.glBlitFramebuffer(
                0, 0, deferredFbs[0].fb.viewWidth, deferredFbs[0].fb.viewHeight,
                0, 0, deferredFbs[0].fb.viewWidth, deferredFbs[0].fb.viewHeight,
                GL_STENCIL_BUFFER_BIT, GL_NEAREST
            );
            
            CHelper.checkGlError();
        }
    }
    
    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {
    
    }
    
    
    @Override
    public void finishRendering() {
    
    }
    
    private void finish() {
        GlStateManager._colorMask(true, true, true, true);
        
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        if (!portalRenderingNeeded) {
            return;
        }
        
        RenderTarget mainFrameBuffer = client.getMainRenderTarget();
        mainFrameBuffer.bindWrite(true);
        
        deferredFbs[0].fb.blitToScreen(mainFrameBuffer.viewWidth, mainFrameBuffer.viewHeight);
        
        CHelper.checkGlError();
    }
    
    protected void doRenderPortal(Portal portal, Matrix4f modelView) {
        nextFramePortalRenderingNeeded = true;
        
        if (!portalRenderingNeeded) {
            return;
        }
        
        //reset projection matrix
//        client.gameRenderer.loadProjectionMatrix(RenderStates.basicProjectionMatrix);
        
        //write to deferred buffer
        if (!tryRenderViewAreaInDeferredBufferAndIncreaseStencil(portal, modelView)) {
            return;
        }
        
        PortalRendering.pushPortalLayer(portal);
        
        // this is important
        client.getMainRenderTarget().bindWrite(true);
        
        renderPortalContent(portal);
        
        int innerLayer = PortalRendering.getPortalLayer();
        
        PortalRendering.popPortalLayer();
        
        int outerLayer = PortalRendering.getPortalLayer();
        
        if (innerLayer > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        
        deferredFbs[outerLayer].fb.bindWrite(true);
        
        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_EQUAL, innerLayer, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        MyRenderHelper.drawScreenFrameBuffer(
            deferredFbs[innerLayer].fb,
            true,
            false
        );
        
        glDisable(GL_STENCIL_TEST);
        
        deferredFbs[outerLayer].fb.unbindWrite();
    }
    
    private boolean tryRenderViewAreaInDeferredBufferAndIncreaseStencil(
        Portal portal, Matrix4f modelView
    ) {
        
        int portalLayer = PortalRendering.getPortalLayer();
        
        initStencilForLayer(portalLayer);
        
        deferredFbs[portalLayer].fb.bindWrite(true);
        
        GL11.glEnable(GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_EQUAL, portalLayer, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
        
        GlStateManager._enableDepthTest();
        
        boolean result = PortalRenderInfo.renderAndDecideVisibility(portal, () -> {
            ViewAreaRenderer.renderPortalArea(
                portal, Vec3.ZERO,
                modelView,
                RenderSystem.getProjectionMatrix(),
                true, true, true, true
            );
        });
        
        GL11.glDisable(GL_STENCIL_TEST);
        
        return result;
    }
    
    @Override
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            Runnable::run
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = getPortalsToRender(modelView);

        for (Portal portal : portalsToRender) {
            doRenderPortal(portal, modelView);
        }
    }
}
