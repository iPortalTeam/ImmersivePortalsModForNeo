package qouteall.imm_ptl.core.mixin.client.render.shader;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.CompiledShaderProgram;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEShader;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

import java.util.List;

@Mixin(CompiledShaderProgram.class)
public abstract class MixinShaderInstance implements IEShader {
    
    @Shadow
    @Final
    private int programId;
    
    @Unique
    private int ip_clippingEquationLoccation;
    
    @Inject(
        method = "setupUniforms",
        at = @At("RETURN")
    )
    private void onSetupUniforms(CallbackInfo ci) {
        ip_clippingEquationLoccation = Uniform.glGetUniformLocation(programId, "iportal_ClippingEquation");
    }
    
    @Override
    public int ip_getClippingEquationUniformLocation() {
        return ip_clippingEquationLoccation;
    }
}
