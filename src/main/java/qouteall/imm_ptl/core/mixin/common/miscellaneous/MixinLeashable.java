package qouteall.imm_ptl.core.mixin.common.miscellaneous;

import net.minecraft.world.entity.Leashable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Leashable.class)
public interface MixinLeashable {
    // TODO check whether a mixin for handling cross-world leash is needed
}
