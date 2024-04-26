package qouteall.imm_ptl.peripheral.platform_specific;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class IPFeatureControl {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static boolean isProvidedByJarInJar() {
        // TODO @Nick1st - This Jar in Jar test should be checked and reenabled
//        ModContainer modContainer = FabricLoader.getInstance()
//            .getModContainer("iportal")
//            .orElseThrow(() -> new RuntimeException("iportal mod not found"));
//
//        return modContainer.getContainingMod().isPresent();
        return true;
    }
    
    public static boolean enableVanillaBehaviorChangingByDefault() {
        return !isProvidedByJarInJar();
    }
}
