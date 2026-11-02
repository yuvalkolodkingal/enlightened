package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.quest.AvatarQuest;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A dark spirit, a spirit turned by Vaatu's darkness: purple-black, its eyes burning yellow. It comes in two shapes, a
 * long-legged <b>crawler</b> that runs on the ground, and a <b>flyer</b> with long arms that swoops from the air. Those
 * that come for the new Avatar ({@link #hunter}) are counted when beaten ({@link AvatarQuest#spiritBeaten}).
 */
public class DarkSpiritEntity extends Monster {
    public enum Shape { CRAWLER, FLYER }

    private static final EntityDataAccessor<Integer> SHAPE = SynchedEntityData.defineId(DarkSpiritEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID hunter;

    public DarkSpiritEntity(EntityType<? extends DarkSpiritEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHAPE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public Shape shape() {
        return Shape.values()[Math.floorMod(entityData.get(SHAPE), 2)];
    }

    public void setShape(Shape shape) {
        entityData.set(SHAPE, shape.ordinal());
    }

    @Nullable
    public UUID hunter() {
        return hunter;
    }

    /** Sent after the new Avatar. */
    public void hunt(ServerPlayer p) {
        hunter = p.getUUID();
        setTarget(p);
        setPersistenceRequired();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        setShape(random.nextInt(3) == 0 ? Shape.FLYER : Shape.CRAWLER);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void tick() {
        super.tick();
        boolean flyer = shape() == Shape.FLYER;
        setNoGravity(flyer);
        if (level().isClientSide) {
            if (tickCount % 4 == 0) {
                level().addParticle(ParticleTypes.SQUID_INK, getX(), getY() + getBbHeight() * 0.5, getZ(), 0, -0.01, 0);
            }
            return;
        }
        if (flyer && getTarget() != null && getTarget().isAlive()) {
            // It swoops at its prey through the air.
            Vec3 to = getTarget().getEyePosition().subtract(position().add(0, getBbHeight() * 0.5, 0));
            Vec3 v = getDeltaMovement().add(to.normalize().scale(0.06)).scale(0.92);
            setDeltaMovement(v);
            if (to.lengthSqr() < 2.5 && tickCount % 20 == 0) {
                doHurtTarget(getTarget());
            }
        } else if (flyer) {
            setDeltaMovement(getDeltaMovement().multiply(0.9, 0.9, 0.9).add(0, -0.005, 0));
        }
        if (hunter != null && level() instanceof ServerLevel server) {
            ServerPlayer p = (ServerPlayer) server.getPlayerByUUID(hunter);
            if (p == null || p.level() != level() || p.distanceToSqr(this) > 80 * 80) {
                discard();
            } else if (getTarget() == null) {
                setTarget(p);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (hunter != null && level() instanceof ServerLevel server && server.getPlayerByUUID(hunter) instanceof ServerPlayer p
                && (source.getEntity() == p || p.distanceToSqr(this) < 32 * 32)) {
            server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 30, 0.4, 0.4, 0.4, 0.05);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            AvatarQuest.spiritBeaten(p);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return shape() != Shape.FLYER && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shape", entityData.get(SHAPE));
        if (hunter != null) {
            tag.putUUID("Hunter", hunter);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SHAPE, tag.getInt("Shape"));
        hunter = tag.hasUUID("Hunter") ? tag.getUUID("Hunter") : null;
    }
}
