package com.kolodkingal.enlightened.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * An invisible seat (1.0.23): a tea stool spawns one for whoever sits on it; it goes when he stands up or the stool is
 * broken. Standing up puts him on top of the stool.
 */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock()
                instanceof com.kolodkingal.enlightened.block.TeaStoolBlock))) {
            ejectPassengers();
            discard();
        }
    }

    /** Whether someone sits on the stool at {@code pos}. */
    public static boolean taken(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return Vec3.atBottomCenterOf(blockPosition()).add(0, 0.6, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
