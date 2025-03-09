package qouteall.imm_ptl.core.mixin.client.accessor;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CoreShaders.class)
public class CoreShadersAccessor {
    @Invoker("register")
    public static ShaderProgram register(String string, VertexFormat vertexFormat, ShaderDefines shaderDefines) {
        throw new NotImplementedException();
    }
    
}
