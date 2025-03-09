package qouteall.imm_ptl.core.mixin.client.render.shader;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.CompiledShader;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;

@Mixin(value = CompiledShader.class)
public class MixinCompiledShader {
    
    @ModifyVariable(
        method = "compile",
        at = @At("HEAD"),
        argsOnly = true
    )
    private static String onCompile(
        String code,
        @Local(argsOnly = true) ResourceLocation resourceLocation,
        @Local(argsOnly = true) CompiledShader.Type type
    ) {
        return ShaderCodeTransformation.transform(type, resourceLocation.toString(), code);
    }
}
