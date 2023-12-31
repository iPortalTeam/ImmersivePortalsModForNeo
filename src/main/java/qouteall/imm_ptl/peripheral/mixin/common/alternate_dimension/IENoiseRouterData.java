package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseRouterData.class)
public interface IENoiseRouterData {
    @Invoker("end")
    static NoiseRouter ip_end(HolderGetter<DensityFunction> holderGetter){
        throw new RuntimeException();
    }
    
    @Invoker("noNewCaves")
    static NoiseRouter ip_noNewCaves(HolderGetter<DensityFunction> holderGetter, HolderGetter<NormalNoise.NoiseParameters> holderGetter2, DensityFunction densityFunction){
        throw new RuntimeException();
    }
    
    @Invoker("slideEndLike")
    static DensityFunction ip_slideEndLike(DensityFunction densityFunction, int i, int j) {
        throw new RuntimeException();
    }
    
    @Invoker("getFunction")
    static DensityFunction ip_getFunction(HolderGetter<DensityFunction> holderGetter, ResourceKey<DensityFunction> resourceKey) {
        throw new RuntimeException();
    }
    
    @Accessor("BASE_3D_NOISE_END")
    static ResourceKey<DensityFunction> get_BASE_3D_NOISE_END(){throw new RuntimeException();}
    
    
    
}
