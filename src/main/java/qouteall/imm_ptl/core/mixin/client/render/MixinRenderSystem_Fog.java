package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import qouteall.imm_ptl.core.render.MyRenderHelper;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Fog {

    @ModifyVariable(
            method = "setShaderFog", at = @At("HEAD"), argsOnly = true
    )
    private static FogParameters transformShaderFogParameter(FogParameters fogParameters) {
        return new FogParameters(MyRenderHelper.transformFogDistance(fogParameters.start()),
                MyRenderHelper.transformFogDistance(fogParameters.end()),
                fogParameters.shape(),
                fogParameters.red(),
                fogParameters.green(),
                fogParameters.blue(),
                fogParameters.alpha());
    }

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
