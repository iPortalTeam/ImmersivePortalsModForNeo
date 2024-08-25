package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import qouteall.imm_ptl.core.ducks.IEEntity;

@SuppressWarnings("resource")
@Mixin(Player.class)
public abstract class MixinPlayer_Collision {
    
    /**
     * @author qouteall
     * @reason mixin does not allow cancel in redirect
     */
    @Overwrite
    public boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
        LivingEntity this_ = (LivingEntity) (Object) this;
        
        AABB box = this_.getDimensions(pose).makeBoundingBox(this_.position());
        AABB activeCollisionBox = ((IEEntity) this_).ip_getActiveCollisionBox(box);
        if (activeCollisionBox == null) {
            return true;
        }
        return this_.level().noCollision(
            this_, activeCollisionBox.deflate(1.0E-7)
            // TODO check the issue of wrongly crouch after going through a scaling portal
            //  when head is touching ceiling because of floating point error
        );
    }
}
