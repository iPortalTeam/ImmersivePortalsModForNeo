package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.CompiledShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Clipping {
    @Inject(
        method = "setShader(Lnet/minecraft/client/renderer/CompiledShaderProgram;)V",
        at = @At("RETURN")
    )
    private static void onSetShader(CompiledShaderProgram shader, CallbackInfo ci) {
        iportal_onShaderSet();
    }
    
    @Unique
    private static void iportal_onShaderSet() {
        if (IPGlobal.enableClippingMechanism) {
            if (!IrisInterface.invoker.isIrisPresent()) {
                if (CrossPortalEntityRenderer.isRenderingEntityNormally ||
                    CrossPortalEntityRenderer.isRenderingEntityProjection
                ) {
                    FrontClipping.updateClippingEquationUniformForCurrentShader(true);
                }
                else if (RenderStates.isRenderingPortalWeather) {
                    FrontClipping.updateClippingEquationUniformForCurrentShader(false);
                }
                else {
                    FrontClipping.unsetClippingUniform();
                }
            }
            else {
                FrontClipping.unsetClippingUniform();
            }
        }
    }
}
