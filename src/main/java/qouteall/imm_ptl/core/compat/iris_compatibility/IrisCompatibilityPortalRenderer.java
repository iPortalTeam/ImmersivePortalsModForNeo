package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.*;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

import java.util.List;

import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;

public class IrisCompatibilityPortalRenderer extends PortalRenderer {

    public static final IrisCompatibilityPortalRenderer instance = new IrisCompatibilityPortalRenderer(false);
    public static final IrisCompatibilityPortalRenderer debugModeInstance =
            new IrisCompatibilityPortalRenderer(true);

    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();

    // TODO figure out why this field existed in old versions
    private Matrix4f passingModelView = new Matrix4f();

    public boolean isDebugMode;

    public IrisCompatibilityPortalRenderer(boolean isDebugMode) {
        this.isDebugMode = isDebugMode;
    }

    @Override
    public boolean replaceFrameBufferClearing() {
        client.getMainRenderTarget().bindWrite(false);

        return false;
    }

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            return;
        }

        passingModelView = modelView;

        GL11.glDisable(GL_STENCIL_TEST);
    }

    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {

    }

    @Override
    public void finishRendering() {
        GL11.glDisable(GL_STENCIL_TEST);
    }

    @Override
    public void prepareRendering() {
        deferredBuffer.prepare();

        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(Minecraft.ON_OSX);

        IPPortingLibCompat.setIsStencilEnabled(
                client.getMainRenderTarget(), false
        );

        // Iris now use vanilla framebuffer's depth
        client.getMainRenderTarget().bindWrite(false);
    }

    protected void doRenderPortal(PortalRenderable portal, Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            // this renderer only supports one-layer portal
            return;
        }

        if (!testShouldRenderPortal(portal, modelView)) {
            return;
        }

        client.getMainRenderTarget().bindWrite(true);

        PortalRendering.pushPortalLayer(portal.getPortalLike());

        renderPortalContent(portal);

        PortalRendering.popPortalLayer();

        CHelper.enableDepthClamp();

        if (!isDebugMode) {
            // draw portal content to the deferred buffer
            deferredBuffer.fb.bindWrite(true);
            MyRenderHelper.drawPortalAreaWithFramebuffer(
                    portal,
                    client.getMainRenderTarget(),
                    modelView,
                    RenderSystem.getProjectionMatrix()
            );
        }
        else {
            deferredBuffer.fb.bindWrite(true);
            MyRenderHelper.drawScreenFrameBuffer(
                    client.getMainRenderTarget(),
                    true, true
            );
        }

        CHelper.disableDepthClamp();

        RenderSystem.colorMask(true, true, true, true);

        client.getMainRenderTarget().bindWrite(true);
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

    private boolean testShouldRenderPortal(PortalRenderable portal, Matrix4f modelView) {

        //reset projection matrix
//        client.gameRenderer.loadProjectionMatrix(RenderStates.basicProjectionMatrix);

        deferredBuffer.fb.bindWrite(true);

        return PortalRenderInfo.renderAndDecideVisibility(portal.getPortalLike(), () -> {

            ViewAreaRenderer.renderPortalArea(
                    portal, Vec3.ZERO,
                    modelView,
                    RenderSystem.getProjectionMatrix(),
                    true, false, false, true
            );
        });
    }

    @Override
    public void onHandRenderingEnded() {

    }

    protected void renderPortals(Matrix4f modelView) {
        List<PortalRenderable> portalsToRender = getPortalsToRender(modelView);

        for (PortalRenderable portal : portalsToRender) {
            doRenderPortal(portal, modelView);
        }
    }
}
