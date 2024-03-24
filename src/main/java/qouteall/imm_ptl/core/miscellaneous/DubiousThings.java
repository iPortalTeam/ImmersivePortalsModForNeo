package qouteall.imm_ptl.core.miscellaneous;

import net.neoforged.neoforge.common.NeoForge;
import qouteall.imm_ptl.core.IPGlobal;

public class DubiousThings {
    public static void init() {
        NeoForge.EVENT_BUS.addListener(IPGlobal.PostClientTickEvent.class, postClientTickEvent -> DubiousThings.tick());
    }
    
    private static void tick() {
        // things removed
    }
    
}
