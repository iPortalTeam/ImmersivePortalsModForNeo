package qouteall.imm_ptl.core.compat.mixin.iris;

import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.SodiumPrograms;
import net.irisshaders.iris.pipeline.programs.SodiumShader;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL21;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.q_misc_util.Helper;

import java.util.List;
import java.util.function.Supplier;

@Mixin(value = SodiumShader.class, remap = false)
public class MixinIrisSodiumChunkShaderInterface {
    private int uIPClippingEquation;

    private void ip_init(int shaderId) {
        uIPClippingEquation = GL20C.glGetUniformLocation(shaderId, "imm_ptl_ClippingEquation");
        if (uIPClippingEquation < 0) {
            Helper.err("uniform imm_ptl_ClippingEquation not found in transformed iris shader");
            uIPClippingEquation = -1;
        }
    }

    @Inject(
        method = "<init>",
        at = @At("RETURN"),
        require = 0
    )
    private void onInit(
            IrisRenderingPipeline pipeline, SodiumPrograms.Pass pass, ShaderBindingContext context, int handle, BlendModeOverride blendModeOverride, List bufferBlendOverrides, CustomUniforms customUniforms, Supplier flipState, float alphaTest, boolean containsTessellation, CallbackInfo ci
    ) {
        ip_init(handle);
    }

    @Inject(
        method = "setupState",
        at = @At("RETURN")
    )
    private void onSetup(CallbackInfo ci) {
        if (uIPClippingEquation != -1) {
            if (FrontClipping.isClippingEnabled) {
                double[] equation = FrontClipping.getActiveClipPlaneEquationAfterModelView();
                GL21.glUniform4f(
                    uIPClippingEquation,
                    (float) equation[0],
                    (float) equation[1],
                    (float) equation[2],
                    (float) equation[3]
                );
            }
            else {
                GL21.glUniform4f(
                    uIPClippingEquation,
                    0, 0, 0, 1
                );
            }
        }
    }
}
