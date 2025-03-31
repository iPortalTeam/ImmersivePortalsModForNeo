package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Fog {
    // TODO @Nick1st 21.3
//    @ModifyVariable(
//        method = "setShaderFogStart", at = @At("HEAD"), argsOnly = true
//    )
//    private static float onSetShaderFogStart(float f) {
//        return MyRenderHelper.transformFogDistance(f);
//    }
//
//    @ModifyVariable(
//        method = "setShaderFogEnd", at = @At("HEAD"), argsOnly = true
//    )
//    private static float onSetShaderFogEnd(float f) {
//        return MyRenderHelper.transformFogDistance(f);
//    }
}
