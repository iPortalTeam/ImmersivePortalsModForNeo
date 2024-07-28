package qouteall.imm_ptl.core.portal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.q_misc_util.my_util.IntBox;

public class LoadingIndicatorEntity extends Entity {
    public static final EntityType<LoadingIndicatorEntity> entityType = getEntityType();

    static EntityType<LoadingIndicatorEntity> getEntityType() {
        EntityType.Builder<LoadingIndicatorEntity> builder = EntityType.Builder.of(
                        LoadingIndicatorEntity::new,
                        MobCategory.MISC)
                .fireImmune()
                .clientTrackingRange(96)
                .updateInterval(20);
        builder.dimensions = new EntityDimensions(1, 1, .85f, EntityAttachments.createDefault(1, 1), true);
        return builder.build("");
    }
    
    private static final EntityDataAccessor<Component> TEXT = SynchedEntityData.defineId(
        LoadingIndicatorEntity.class, EntityDataSerializers.COMPONENT
    );
    private static final EntityDataAccessor<BlockPos> BOX_LOW_POS = SynchedEntityData.defineId(
        LoadingIndicatorEntity.class, EntityDataSerializers.BLOCK_POS
    );
    private static final EntityDataAccessor<BlockPos> BOX_HIGH_POS = SynchedEntityData.defineId(
        LoadingIndicatorEntity.class, EntityDataSerializers.BLOCK_POS
    );
    
    public boolean isValid = false;
    
    public LoadingIndicatorEntity(EntityType type, Level world) {
        super(type, world);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (level().isClientSide()) {
            tickClient();
        }
        else {
            // remove after quitting server and restarting
            if (!isValid) {
                remove(RemovalReason.KILLED);
            }
        }
    }
    
    //@OnlyIn(Dist.CLIENT)
    private void tickClient() {
        addParticles();
        
        if (tickCount > 40) {
            LocalPlayer player = Minecraft.getInstance().player;
            
            if (player != null &&
                player.level() == level() &&
                player.position().distanceToSqr(position()) < 16 * 16
            ) {
                showMessageClient();
            }
        }
    }
    
    //@OnlyIn(Dist.CLIENT)
    private void addParticles() {
        int num = tickCount < 100 ? 50 : 20;
        
        IntBox box = getBox();
        BlockPos size = box.getSize();
        RandomSource random = level().getRandom();
        
        for (int i = 0; i < num; i++) {
            Vec3 p = new Vec3(
                random.nextDouble(), random.nextDouble(), random.nextDouble()
            ).multiply(Vec3.atLowerCornerOf(size)).add(Vec3.atLowerCornerOf(box.l));
            
            double speedMultiplier = 20;
            
            double vx = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
            double vy = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
            double vz = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
            
            level().addParticle(
                ParticleTypes.PORTAL,
                p.x, p.y, p.z,
                vx, vy, vz
            );
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
       builder.define(TEXT, Component.literal("Loading..."));
       builder.define(BOX_LOW_POS, BlockPos.ZERO);
       builder.define(BOX_HIGH_POS, BlockPos.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    
    }
    
    public void inform(Component str) {
        setText(str);
    }
    
    public void setText(Component str) {
        getEntityData().set(TEXT, str);
    }
    
    public Component getText() {
        return getEntityData().get(TEXT);
    }
    
    public void setBox(IntBox box) {
        getEntityData().set(BOX_LOW_POS, box.l);
        getEntityData().set(BOX_HIGH_POS, box.h);
    }
    
    public IntBox getBox() {
        return new IntBox(
            getEntityData().get(BOX_LOW_POS),
            getEntityData().get(BOX_HIGH_POS)
        );
    }
    
    //@OnlyIn(Dist.CLIENT)
    private void showMessageClient() {
        Gui inGameHud = Minecraft.getInstance().gui;
        inGameHud.setOverlayMessage(
            getText(), false
        );
    }
}
