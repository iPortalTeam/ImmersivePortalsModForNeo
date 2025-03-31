package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.mc_utils.WireRenderingHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

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
    public void render(EntityRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight
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
    }
    
}
