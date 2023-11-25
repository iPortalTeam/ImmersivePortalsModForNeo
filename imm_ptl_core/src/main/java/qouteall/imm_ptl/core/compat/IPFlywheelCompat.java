package qouteall.imm_ptl.core.compat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.fabricmc.loader.api.FabricLoader;
import qouteall.q_misc_util.Helper;

@OnlyIn(Dist.CLIENT)
public class IPFlywheelCompat {
    
    public static boolean isFlywheelPresent = false;
    
    public static void init(){
        if (FabricLoader.getInstance().isModLoaded("flywheel")) {
            Helper.log("Flywheel is present");
        }
        
    }
    
}
