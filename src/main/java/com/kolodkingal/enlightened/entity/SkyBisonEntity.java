package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The sky bison of the Air Nomads, like Appa: a huge, gentle, six-legged beast with a flat tail that swims through
 * the air. Calves live at the Southern Air Temple. Fed enough wheat a bison comes to trust whoever feeds it and is
 * tamed; a calf grows slowly into a full bison (wheat speeds it up). A grown, tamed bison takes a saddle, and then
 * up to six riders: its owner sits on its head and steers, the rest sit in the saddle behind.
 *
 * <p>Flying: the owner looks where to go and holds forward; Jump climbs ("Yip yip!"), and with no key held the bison
 * hangs in the air, drifting slowly down; it lands where it touches the ground and walks there. Riders who step off
 * high in the air fall slowly. A bison never takes fall damage.
 */
public class SkyBisonEntity extends TamableAnimal implements Saddleable {
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(SkyBisonEntity.class, EntityDataSerializers.BOOLEAN);
    /** Wheat a bison must eat from one hand before it trusts it. */
    public static final int WHEAT_TO_TAME = 16;
    /** How long a calf takes to grow (ticks; vanilla's 20 minutes, then half again: a bison grows slowly). */
    public static final int CALF_AGE = -36000;
    public static final int MAX_RIDERS = 6;
    private static final double FLY_SPEED = 0.6;
    private static final double CLIMB = 0.35;

    private int wheatEaten;

    public SkyBisonEntity(EntityType<? extends SkyBisonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 80.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.SAFE_FALL_DISTANCE, 1000.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SADDLED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(Items.WHEAT, Items.HAY_BLOCK), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1, 12.0F, 4.0F));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ---- taming, growing, the saddle ----

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.HAY_BLOCK);
    }

    public int wheatEaten() {
        return wheatEaten;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isFood(stack) && !isTame()) {
            if (!level().isClientSide) {
                // A hay bale is nine wheat.
                int worth = stack.is(Items.HAY_BLOCK) ? 9 : 1;
                usePlayerItem(player, hand, stack);
                wheatEaten += worth;
                heal(2.0F);
                if (isBaby()) {
                    ageUp(getSpeedUpSecondsWhenFeeding(-getAge()), true);
                }
                playSound(SoundEvents.COW_AMBIENT, 1.0F, 0.6F);
                gameEvent(GameEvent.EAT);
                if (wheatEaten >= WHEAT_TO_TAME) {
                    tame(player);
                    navigation.stop();
                    level().broadcastEntityEvent(this, (byte) 7);
                    player.displayClientMessage(Component.translatable("message.enlightened.bison.tamed").withStyle(ChatFormatting.AQUA), true);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                    player.displayClientMessage(Component.translatable("message.enlightened.bison.eats", wheatEaten, WHEAT_TO_TAME), true);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isOwnedBy(player) && !isBaby() && !isSaddled() && stack.is(Items.SADDLE)) {
            if (!level().isClientSide) {
                equipSaddle(stack.split(1), SoundSource.NEUTRAL);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isFood(stack)) {
            // Fed when tame: it heals, a calf grows, two fed bison may have a calf.
            return super.mobInteract(player, hand);
        }
        if (isTame() && isSaddled() && !isBaby() && canAddPassenger(player) && !player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                player.setYRot(getYRot());
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isBaby() && stack.is(Items.SADDLE)) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.enlightened.bison.too_young").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isSaddleable() {
        return isAlive() && !isBaby() && isTame();
    }

    @Override
    public void equipSaddle(ItemStack stack, @Nullable SoundSource source) {
        entityData.set(SADDLED, true);
        if (source != null) {
            level().playSound(null, this, SoundEvents.HORSE_SADDLE, source, 1.0F, 0.7F);
        }
    }

    @Override
    public boolean isSaddled() {
        return entityData.get(SADDLED);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (isSaddled()) {
            spawnAtLocation(Items.SADDLE);
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        SkyBisonEntity calf = ModEntities.SKY_BISON.get().create(level);
        if (calf != null) {
            calf.setAge(CALF_AGE);
        }
        return calf;
    }

    @Override
    public void setBaby(boolean baby) {
        setAge(baby ? CALF_AGE : 0);
    }

    @Override
    public float getAgeScale() {
        return isBaby() ? 0.4F : 1.0F;
    }

    // ---- riding ----

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < MAX_RIDERS && isSaddled() && !isBaby();
    }

    /** The owner, wherever he sits, steers. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity e : getPassengers()) {
            if (e instanceof Player p && isOwnedBy(p)) {
                return p;
            }
        }
        return null;
    }

    /** The owner on the head, the others two by two in the saddle behind, the last on the tail's root. */
    private static final double[][] SEATS = {{0.0, 1.45}, {-0.55, 0.35}, {0.55, 0.35}, {-0.55, -0.45}, {0.55, -0.45}, {0.0, -1.2}};

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        List<Entity> riders = getPassengers();
        LivingEntity driver = getControllingPassenger();
        int seat;
        if (entity == driver) {
            seat = 0;
        } else {
            seat = 1;
            for (Entity e : riders) {
                if (e == entity) {
                    break;
                }
                if (e != driver) {
                    seat++;
                }
            }
            if (driver == null) {
                seat--;
            }
        }
        double[] s = SEATS[Mth.clamp(seat, 0, SEATS.length - 1)];
        double y = dimensions.height() * (seat == 0 ? 0.86 : 0.92);
        return new Vec3(s[0] * scale, y, s[1] * scale).yRot(-yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        // Off a bison high in the air: the riders float down rather than fall.
        if (!onGround() && passenger instanceof LivingEntity living && !level().isClientSide) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, true));
        }
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        setRot(player.getYRot(), player.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward < 0.0F) {
            forward *= 0.35F;
        }
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.2F;
    }

    /** Whether it is up in the air (not standing on the ground, not swimming). */
    public boolean airborne() {
        return !onGround() && !isInWater();
    }

    @Override
    public void travel(Vec3 input) {
        LivingEntity driver = getControllingPassenger();
        if (!(driver instanceof Player rider) || !isControlledByLocalInstance()) {
            super.travel(input);
            return;
        }
        boolean climb = ((com.kolodkingal.enlightened.mixin.LivingEntityAccessor) rider).enlightened$isJumping();
        Vec3 v = getDeltaMovement();
        if (onGround() && !climb) {
            // On the ground it walks; gravity is off while ridden, so it is put back by hand.
            super.travel(input);
            Vec3 after = getDeltaMovement();
            setDeltaMovement(after.x, Math.min(after.y, 0.0) - 0.08, after.z);
            return;
        }
        // In the air: where the rider looks, forward; Jump climbs; nothing held, it hangs and drifts down.
        Vec3 look = rider.getLookAngle();
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        Vec3 wish = look.scale(input.z * FLY_SPEED).add(left.scale(input.x * FLY_SPEED));
        if (climb) {
            wish = wish.add(0, CLIMB, 0);
        } else if (input.z == 0 && input.x == 0) {
            wish = new Vec3(0, -0.04, 0);
        }
        // Heavy, as a bison is: it eases into a turn and out of a dive.
        Vec3 next = v.add(wish.subtract(v).scale(0.08));
        move(MoverType.SELF, next);
        setDeltaMovement(next);
        calculateEntityAnimation(true);
    }

    @Override
    public void tick() {
        super.tick();
        // Ridden, it carries its own weight in the air (and the server does not take it for a cheat).
        setNoGravity(getControllingPassenger() != null);
        if (!level().isClientSide && getControllingPassenger() == null && airborne()) {
            // Left in the air, it sinks gently to the ground.
            Vec3 v = getDeltaMovement();
            if (v.y < -0.12) {
                setDeltaMovement(v.x, -0.12, v.z);
            }
        }
        if (level().isClientSide && airborne() && tickCount % 4 == 0) {
            // The air it pushes with its tail.
            level().addParticle(ParticleTypes.CLOUD, getX() - Math.sin(-yBodyRot * Mth.DEG_TO_RAD) * 2.2, getY() + 0.8, getZ() - Math.cos(-yBodyRot * Mth.DEG_TO_RAD) * 2.2, 0, -0.02, 0);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ---- the rest ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Saddled", isSaddled());
        tag.putInt("WheatEaten", wheatEaten);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SADDLED, tag.getBoolean("Saddled"));
        wheatEaten = tag.getInt("WheatEaten");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COW_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return isBaby() ? 0.9F : 0.45F;
    }

    @Override
    protected float getSoundVolume() {
        return 1.4F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
