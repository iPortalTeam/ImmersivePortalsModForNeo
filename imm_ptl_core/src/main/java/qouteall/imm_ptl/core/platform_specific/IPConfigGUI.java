package qouteall.imm_ptl.core.platform_specific;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.screens.Screen;

@OnlyIn(Dist.CLIENT)
public class IPConfigGUI {
    public static Screen createClothConfigScreen(Screen parent) {
        return AutoConfig.getConfigScreen(IPConfig.class, parent).get();
    }
}
