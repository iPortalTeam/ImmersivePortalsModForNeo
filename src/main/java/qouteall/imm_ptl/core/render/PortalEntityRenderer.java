package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.portal.Portal;

@OnlyIn(Dist.CLIENT)
public class PortalEntityRenderer extends EntityRenderer<Portal, EntityRenderState> {
    
    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void render(@NotNull EntityRenderState renderState, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight
    ) {
       // TODO @Nick1st 21.3
//        IPCGlobal.renderer.renderPortalInEntityRenderer(portal);
//
//        if (OverlayRendering.shouldRenderOverlay(portal)) {
//            OverlayRendering.onRenderPortalEntity(portal, matrixStack, bufferSource);
//        }
//
//        if (IPGlobal.debugRenderPortalShapeMesh && !PortalRendering.isRendering()) {
//            VertexConsumer lineVertexConsumer = bufferSource.getBuffer(RenderType.lines());
//            WireRenderingHelper.renderPortalShapeMeshDebug(
//                matrixStack, lineVertexConsumer, portal
//            );
//        }
//
//        super.render(portal, yaw, partialTick, matrixStack, bufferSource, light);
        super.render(renderState, poseStack, bufferSource, packedLight);
    }
    
}
