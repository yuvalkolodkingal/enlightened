package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The flying lemur of the Air Nomads (Momo): a small winged creature of jungles, cherry groves and mountain meadows
 * that flits from tree to tree. Tamed with fruit (apples, berries, melon, glow berries: {@code enlightened:lemur_food},
 * one time in three), it follows its owner, sits when told (right-click), rides on his shoulder as a parrot does (it hops
 * up by itself now and then, or when he sneaks and right-clicks it; it hops down when he jumps, flies or swims) and
 * breeds on the same fruit. It never falls
 * hard, never drowns long (it lands), and chirps.
 *
 * <p>An airbender's lemur is his pet: the one whose death would sever his earthly tethers.
 */
public class FlyingLemurEntity extends net.minecraft.world.entity.animal.ShoulderRidingEntity {
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(FlyingLemurEntity.class, EntityDataSerializers.BOOLEAN);

    /** The flap of the wings, for the model (client), eased so it does not snap when it lands. */
    public float flap;
    public float oFlap;
    public float flapSpeed;
    private float flapping = 1.0F;
    public float earTwitch;
    /** Smoothed client-side blends for the model: how far it is in flight, sitting, gliding; its bank into a turn. */
    public float flyWeight;
    public float oFlyWeight;
    public float sitWeight;
    public float oSitWeight;
    public float glideWeight;
    public float oGlideWeight;
    public float bank;
    public float oBank;
    public float pitchLean;
    public float oPitchLean;
    /** An idle head tilt that comes and goes. */
    public float headTilt;
    public float oHeadTilt;
    private float headTiltTarget;

    public FlyingLemurEntity(EntityType<? extends FlyingLemurEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        setPathfindingMalus(PathType.COCOA, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new TamableAnimalPanicGoal(1.3));
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.2, Ingredient.of(ModTags.LEMUR_FOOD), false));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal(this));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new LemurWanderGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLYING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    public static boolean checkLemurSpawnRules(EntityType<FlyingLemurEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON) && Animal.isBrightEnoughToSpawn(level, pos);
    }

    public boolean isFlying() {
        return !onGround();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        calculateFlapping();
        if (!level().isClientSide) {
            entityData.set(DATA_FLYING, isFlying());
        }
    }

    private void calculateFlapping() {
        oFlap = flap;
        flapSpeed +=(onGround() || isOrderedToSit() ? -1.0F : 4.0F) * 0.3F;
        flapSpeed = Math.max(0.0F, Math.min(1.0F, flapSpeed));
        if (!onGround() && flapping < 1.0F) {
            flapping = 1.0F;
        }
        flapping *= 0.9F;
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < 0.0) {
            setDeltaMovement(v.multiply(1.0, 0.6, 1.0));
        }
        flap += flapping * 2.0F;
        earTwitch = random.nextInt(60) == 0 ? 1.0F : earTwitch * 0.8F;
        blends(v);
    }

    /** The eased blends the model draws with, so a landing, a perch and a glide never snap. */
    private void blends(Vec3 v) {
        oFlyWeight = flyWeight;
        oSitWeight = sitWeight;
        oGlideWeight = glideWeight;
        oBank = bank;
        oPitchLean = pitchLean;
        oHeadTilt = headTilt;
        boolean sitting = isOrderedToSit() || isInSittingPose();
        boolean flying = !onGround() && !sitting;
        boolean gliding = flying && v.y < -0.03 && v.horizontalDistanceSqr() > 0.01;
        flyWeight += ((flying ? 1.0F : 0.0F) - flyWeight) * 0.25F;
        sitWeight += ((sitting ? 1.0F : 0.0F) - sitWeight) * 0.18F;
        glideWeight += ((gliding ? 1.0F : 0.0F) - glideWeight) * 0.15F;
        float turn = Mth.wrapDegrees(yBodyRot - yBodyRotO);
        bank += (Mth.clamp(-turn * 2.5F, -35.0F, 35.0F) * flyWeight - bank) * 0.2F;
        float climb = (float) Mth.clamp(v.y * 90.0, -30.0, 30.0);
        pitchLean += ((-climb + 10.0F * (float) Math.min(1.0, v.horizontalDistance() * 4.0)) * flyWeight - pitchLean) * 0.2F;
        if (random.nextInt(80) == 0) {
            headTiltTarget = headTiltTarget == 0.0F ? (random.nextBoolean() ? 0.35F : -0.35F) : 0.0F;
        }
        headTilt += (headTiltTarget - headTilt) * 0.1F;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTame() && stack.is(ModTags.LEMUR_FOOD)) {
            stack.consume(1, player);
            playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.2F + (random.nextFloat() - random.nextFloat()) * 0.2F);
            if (!level().isClientSide) {
                if (random.nextInt(3) == 0 && !EventHooks.onAnimalTame(this, player)) {
                    tame(player);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isOwnedBy(player)) {
            if (stack.is(ModTags.LEMUR_FOOD) && getHealth() < getMaxHealth()) {
                stack.consume(1, player);
                heal(3.0F);
                playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.2F);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (isFood(stack) && getAge() == 0 && canFallInLove()) {
                return super.mobInteract(player, hand);
            }
            // Sneaking, with nothing in hand: up onto his shoulder.
            if (player.isShiftKeyDown() && stack.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer owner) {
                setOrderedToSit(false);
                if (!setEntityOnShoulder(owner)) {
                    owner.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.enlightened.lemur.no_shoulder"), true);
                }
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && stack.isEmpty()) {
                return InteractionResult.SUCCESS;
            }
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
                jumping = false;
                navigation.stop();
                setTarget(null);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.LEMUR_FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        FlyingLemurEntity baby = ModEntities.LEMUR.get().create(level);
        if (baby != null && isTame()) {
            baby.setOwnerUUID(getOwnerUUID());
            baby.setTame(true, true);
        }
        return baby;
    }

    @Override
    public boolean canMate(Animal other) {
        return other != this && other instanceof FlyingLemurEntity lemur && isTame() && lemur.isTame() && isInLove() && lemur.isInLove();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof Player)) {
            super.doPush(entity);
        }
    }

    @Override
    public boolean canBeLeashed() {
        return !isOrderedToSit();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.LEMUR_CHIRP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LEMUR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LEMUR_HURT.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public float getVoicePitch() {
        return isBaby() ? 1.5F : 1.1F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    /** Flits about, and to a tree when it sees one. */
    static class LemurWanderGoal extends WaterAvoidingRandomFlyingGoal {
        LemurWanderGoal(PathfinderMob mob, double speed) {
            super(mob, speed);
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            Vec3 pos = null;
            if (mob.isInWater()) {
                pos = LandRandomPos.getPos(mob, 15, 15);
            }
            if (mob.getRandom().nextFloat() >= probability) {
                pos = treePos();
            }
            return pos == null ? super.getPosition() : pos;
        }

        @Nullable
        private Vec3 treePos() {
            BlockPos at = mob.blockPosition();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
            for (BlockPos p : BlockPos.betweenClosed(at.getX() - 3, at.getY() - 6, at.getZ() - 3, at.getX() + 3, at.getY() + 6, at.getZ() + 3)) {
                if (at.equals(p)) {
                    continue;
                }
                BlockState state = mob.level().getBlockState(above.setWithOffset(p, net.minecraft.core.Direction.DOWN));
                boolean leaves = state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock || state.is(net.minecraft.tags.BlockTags.LOGS);
                if (leaves && mob.level().isEmptyBlock(p) && mob.level().isEmptyBlock(pos.setWithOffset(p, net.minecraft.core.Direction.UP))) {
                    return Vec3.atBottomCenterOf(p);
                }
            }
            return null;
        }
    }
}
