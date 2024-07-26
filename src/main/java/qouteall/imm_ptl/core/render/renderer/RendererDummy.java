package qouteall.imm_ptl.core.render.renderer;

import org.joml.Matrix4f;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.PortalRenderable;

import java.util.List;

public class RendererDummy extends PortalRenderer {
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    @Override
    public void prepareRendering() {
    
    }
    
    @Override
    public void onBeforeTranslucentRendering(Matrix4f matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(Matrix4f matrixStack) {
    
    }
    
    @Override
    public void onHandRenderingEnded( ) {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    protected void doRenderPortal(
        PortalRenderable portal,
        Matrix4f matrixStack
    ) {
    
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    protected void renderPortals(Matrix4f matrixStack) {
        List<PortalRenderable> portalsToRender = getPortalsToRender(matrixStack);
    
        for (PortalRenderable portal : portalsToRender) {
            doRenderPortal(portal, matrixStack);
        }
    }
}
