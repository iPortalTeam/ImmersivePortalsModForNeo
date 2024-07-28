package qouteall.imm_ptl.core.mixin.common.miscellaneous;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Mob.class)
public abstract class MixinMob {
    /*
    @Overwrite
    private static void onTickLeash(Entity e, CallbackInfo ci) {
        Mob this_ = (Mob) e;
        if (this_.getLeashHolder() != null) {
            if (this_.getLeashHolder().level() != this_.level()) {
                this_.dropLeash(true, true);
            }
        }
    }*/
}
