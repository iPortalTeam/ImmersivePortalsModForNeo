package qouteall.imm_ptl.core.compat;

import net.neoforged.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class IPCompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        

        ModList modList = ModList.get();
        if (mixinClassName.contains("IrisSodium")) {
            boolean sodiumLoaded = modList.isLoaded("sodium");
            boolean irisLoaded = modList.isLoaded("iris");
            return sodiumLoaded && irisLoaded;
        }
        
        if (mixinClassName.contains("Iris")) {
            boolean irisLoaded = modList.isLoaded("iris");
            return irisLoaded;
        }
        
        if (mixinClassName.contains("Sodium")) {
            boolean sodiumLoaded = modList.isLoaded("sodium");
            return sodiumLoaded;
        }
        
        if (mixinClassName.contains("Flywheel")) {
            boolean flywheelLoaded = modList.isLoaded("flywheel");
            return flywheelLoaded;
        }
        
        if (mixinClassName.contains("CardinalComp")) {
            boolean cardinalCompLoaded = modList.isLoaded("cardinal-components-base");
            return cardinalCompLoaded;
        }
        
        return false;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    
    }
}
