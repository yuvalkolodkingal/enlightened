package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Variants;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.item.EternalFlameItem;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.world.LionTurtle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A dragon of the old world: long, serpentine, maned and horned, flying without wings.
 *
 * <p><b>The masters</b>, Ran (red) and Shaw (blue), sleep in the two caves at the summit of the Sun Warriors' mountain.
 * When someone comes onto the platform between the caves they wake, come out and circle him, and judge: whoever
 * carries the Eternal Flame still alight (or is a Hero of the Village) they wrap in a great whirl of rainbow fire, and
 * bless: his fire burns stronger, rainbow fire is his to choose, and Ran gives him a golden dragon egg. Anyone else
 * they burn. Then they go back to sleep. Nothing harms the masters.
 *
 * <p><b>A dragon of one's own</b> hatches from the golden egg: a pup, red, blue or white, that is its hatcher's. It
 * grows in twenty minutes (meat speeds it) into a huge dragon with a great deal of health. Its owner rides it
 * (right-click; sneak and right-click to make it stay) and flies: he looks where to go and holds forward, Jump
 * climbs, and attacking (left-click) breathes fire where he looks. Unridden, it follows him and breathes fire at
 * what attacks him, what he attacks and the monsters near. A blue dragon breathes blue fire.
 */
public class DragonEntity extends TamableAnimal {
    public enum Colour {
        RED("red", Variants.FIRE_NORMAL), BLUE("blue", Variants.FIRE_BLUE), WHITE("white", Variants.FIRE_NORMAL);

        public final String id;
        /** The colour of fire it breathes. */
        public final int fire;

        Colour(String id, int fire) {
            this.id = id;
            this.fire = fire;
        }

        public static Colour of(int i) {
            Colour[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    /** What a master is doing. */
    public enum Phase { SLEEP, RISE, CIRCLE, BLESS, BURN, RETURN }

    private static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MASTER = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);

    /** A pup grows up in twenty minutes. */
    public static final int PUP_AGE = -24000;
    /** How long a breath lasts, and the rest after it (ticks). */
    public static final int BREATH = 24;
    public static final int BREATH_REST = 30;
    /** How far a grown dragon's fire reaches. */
    public static final double REACH = 14.0;
    /** A master's scale (a grown dragon's is 1). */
    public static final float MASTER_SCALE = 1.6F;
    /** How long each part of the judgment lasts (ticks). */
    public static final int RISE_TICKS = 60;
    public static final int CIRCLE_TICKS = 100;
    public static final int BLESS_TICKS = 140;
    public static final int BURN_TICKS = 90;
    /** How long before the masters judge the same one again. */
    public static final int REST = 1200;

    // ---- the master's ----
    private BlockPos cave = BlockPos.ZERO;
    private BlockPos centre = BlockPos.ZERO;
    @Nullable
    private UUID judged;
    private int phaseTicks;
    private final Map<UUID, Long> lastJudged = new HashMap<>();

    // ---- the breath ----
    private int breathing;
    private int breathRest;
    private Vec3 breathDir = Vec3.ZERO;

    /** Where the head has been lately (client only: the body follows it). */
    public final ArrayDeque<Vec3> trail = new ArrayDeque<>();

    public DragonEntity(EntityType<? extends DragonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ARMOR, 10.0).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.SAFE_FALL_DISTANCE, 1000.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOUR, 0);
        builder.define(MASTER, false);
        builder.define(PHASE, 0);
        builder.define(BREATHING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new BreathGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.2, 10.0F, 3.0F));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> isTame() && !isBaby() && e instanceof Enemy && !(e instanceof Creeper)));
    }

    public Colour colour() {
        return Colour.of(entityData.get(COLOUR));
    }

    public void setColour(Colour colour) {
        entityData.set(COLOUR, colour.ordinal());
    }

    public boolean master() {
        return entityData.get(MASTER);
    }

    public Phase phase() {
        Phase[] all = Phase.values();
        return all[Math.floorMod(entityData.get(PHASE), all.length)];
    }

    private void setPhase(Phase phase) {
        entityData.set(PHASE, phase.ordinal());
        phaseTicks = 0;
    }

    public BlockPos cave() {
        return cave;
    }

    public BlockPos centre() {
        return centre;
    }

    /** Makes this Ran or Shaw, asleep in a cave by the summit's centre. */
    public void makeMaster(Colour colour, BlockPos cave, BlockPos centre) {
        entityData.set(MASTER, true);
        setColour(colour);
        this.cave = cave.immutable();
        this.centre = centre.immutable();
        setPersistenceRequired();
        var scale = getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(MASTER_SCALE);
        }
        setNoGravity(true);
        moveTo(cave.getX() + 0.5, cave.getY(), cave.getZ() + 0.5, cave.getX() < centre.getX() ? -90.0F : 90.0F, 0.0F);
        setYHeadRot(getYRot());
        setYBodyRot(getYRot());
        setPhase(Phase.SLEEP);
        refreshDimensions();
    }

    /** A pup hatched from the golden egg: its hatcher's (if there is one), of a colour chosen by chance. */
    public static DragonEntity hatch(ServerLevel level, BlockPos at, @Nullable Player owner) {
        DragonEntity pup = ModEntities.DRAGON.get().create(level);
        if (pup == null) {
            return null;
        }
        pup.setColour(Colour.of(level.random.nextInt(3)));
        pup.setAge(PUP_AGE);
        pup.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        if (owner != null) {
            pup.tame(owner);
        }
        pup.setPersistenceRequired();
        level.addFreshEntity(pup);
        return pup;
    }

    // ---- growing, taming, riding ----

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.MUTTON) || stack.is(Items.CHICKEN)
                || stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.COD) || stack.is(Items.SALMON);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (master()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (isTame() && isOwnedBy(player)) {
            if (isFood(stack)) {
                if (!level().isClientSide) {
                    usePlayerItem(player, hand, stack);
                    heal(10.0F);
                    if (isBaby()) {
                        ageUp(getSpeedUpSecondsWhenFeeding(-getAge()), true);
                    }
                    playSound(SoundEvents.GENERIC_EAT, 1.0F, isBaby() ? 1.3F : 0.6F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (player.isSecondaryUseActive()) {
                if (!level().isClientSide) {
                    setOrderedToSit(!isOrderedToSit());
                    navigation.stop();
                    setTarget(null);
                    player.displayClientMessage(Component.translatable(isOrderedToSit() ? "message.enlightened.dragon.stays"
                            : "message.enlightened.dragon.follows"), true);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (isBaby()) {
                if (!level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.enlightened.dragon.too_young",
                            (-getAge() + 1199) / 1200).withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.CONSUME;
            }
            if (!level().isClientSide) {
                setOrderedToSit(false);
                player.setYRot(getYRot());
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isTame() && isFood(stack)) {
            // A wild dragon may come to trust whoever feeds it.
            if (!level().isClientSide) {
                usePlayerItem(player, hand, stack);
                if (random.nextInt(3) == 0) {
                    tame(player);
                    navigation.stop();
                    setTarget(null);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                                                   net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData data) {
        if (!master()) {
            setColour(Colour.of(random.nextInt(3)));
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {
        setAge(baby ? PUP_AGE : 0);
    }

    @Override
    public float getAgeScale() {
        return isBaby() ? 0.3F : 1.0F;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !master() && !isBaby() && isTame() && getPassengers().isEmpty() && passenger instanceof Player p && isOwnedBy(p);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return !master() && getFirstPassenger() instanceof Player p && isOwnedBy(p) ? p : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height() * 0.8, -0.9 * scale).yRot(-yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
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
        float forward = player.zza;
        if (forward < 0.0F) {
            forward *= 0.4F;
        }
        return new Vec3(player.xxa * 0.6F, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.3F;
    }

    public boolean airborne() {
        return !onGround() && !isInWater();
    }

    /** Blocks a tick a grown dragon flies at full speed. */
    public static final double FLY_SPEED = 1.6;

    @Override
    public void travel(Vec3 input) {
        if (master()) {
            return;
        }
        LivingEntity driver = getControllingPassenger();
        if (!(driver instanceof Player rider) || !isControlledByLocalInstance()) {
            super.travel(input);
            return;
        }
        boolean climb = ((com.kolodkingal.enlightened.mixin.LivingEntityAccessor) rider).enlightened$isJumping();
        Vec3 v = getDeltaMovement();
        if (onGround() && !climb) {
            super.travel(input);
            Vec3 after = getDeltaMovement();
            setDeltaMovement(after.x, Math.min(after.y, 0.0) - 0.08, after.z);
            return;
        }
        Vec3 look = rider.getLookAngle();
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        Vec3 wish = look.scale(input.z * FLY_SPEED).add(left.scale(input.x * FLY_SPEED));
        if (climb) {
            wish = wish.add(0, FLY_SPEED * 0.6, 0);
        } else if (input.z == 0 && input.x == 0) {
            wish = new Vec3(0, -0.03, 0);
        }
        // Lithe, as a dragon is: it answers quickly.
        Vec3 next = v.add(wish.subtract(v).scale(0.2));
        move(MoverType.SELF, next);
        setDeltaMovement(next);
        calculateEntityAnimation(true);
    }

    // ---- the breath ----

    /** Where its fire comes from. */
    public Vec3 mouth() {
        float yaw = yHeadRot * Mth.DEG_TO_RAD;
        double reach = getBbWidth() * 0.6;
        return position().add(-Mth.sin(yaw) * reach, getBbHeight() * 0.6, Mth.cos(yaw) * reach);
    }

    /** Whether it breathes fire just now (on the client too: its jaws open). */
    public boolean breathing() {
        return level().isClientSide ? entityData.get(BREATHING) : breathing > 0;
    }

    /** Starts a breath of fire along a direction (if it is not resting from the last one); whether it began. */
    public boolean breathe(Vec3 dir) {
        if (breathing > 0 || breathRest > 0 || dir.lengthSqr() < 1.0E-6) {
            return false;
        }
        breathDir = dir.normalize();
        breathing = master() ? BURN_TICKS : BREATH;
        playSound(SoundEvents.ENDER_DRAGON_SHOOT, master() ? 3.0F : 1.5F, isBaby() ? 1.6F : 0.8F);
        return true;
    }

    /** The rider asks for fire, where he looks. */
    public void riderBreathes(Player rider) {
        if (getControllingPassenger() == rider) {
            breathe(rider.getLookAngle());
        }
    }

    /** The colour of fire it breathes just now (the masters breathe rainbow fire as they bless). */
    public int fireColour() {
        return master() && phase() == Phase.BLESS ? Variants.FIRE_RAINBOW : colour().fire;
    }

    private double reach() {
        return (isBaby() ? 5.0 : REACH) * (master() ? 1.3 : 1.0);
    }

    private float breathDamage() {
        float base = master() ? 2.5F : isBaby() ? 2.0F : 7.0F;
        return colour() == Colour.BLUE ? base * 1.3F : base;
    }

    /** Whether its fire passes this creature by: its rider and owner, the owner's other beasts, the masters' people. */
    public boolean spares(Entity e) {
        if (e == this || hasPassenger(e) || e.isSpectator()) {
            return true;
        }
        if (master()) {
            return e instanceof DragonEntity d && d.master() || e instanceof SunWarriorEntity;
        }
        LivingEntity owner = getOwner();
        if (owner != null && (e == owner || e instanceof OwnableEntity o && owner.getUUID().equals(o.getOwnerUUID()))) {
            return true;
        }
        return e instanceof Player p && (p.isCreative() || !(getOwner() instanceof Player op && op.canHarmPlayer(p)));
    }

    private void breathTick(ServerLevel level) {
        if (getControllingPassenger() instanceof Player rider) {
            breathDir = rider.getLookAngle();
        } else if (getTarget() != null && !master()) {
            breathDir = getTarget().getEyePosition().subtract(mouth()).normalize();
        }
        Vec3 from = mouth();
        double reach = reach();
        if (breathing % 3 == 0) {
            Fx.send(level, Fx.DRAGON_FIRE, from, from.add(breathDir.scale(reach)), fireColour(), (float) (isBaby() ? 0.4 : master() ? 1.6 : 1.0), getId());
        }
        if (breathing % 5 == 0) {
            burnCone(level, from, breathDir, reach, 25.0, breathDamage());
        }
    }

    /** Burns the creatures in a cone of fire. */
    void burnCone(ServerLevel level, Vec3 from, Vec3 dir, double reach, double degrees, float damage) {
        double cos = Math.cos(Math.toRadians(degrees));
        AABB box = new AABB(from, from.add(dir.scale(reach))).inflate(reach * 0.45);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !spares(e))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double d = to.length();
            if (d > reach + e.getBbWidth() || (d > 1.0 && to.scale(1.0 / d).dot(dir) < cos)) {
                continue;
            }
            e.hurt(damageSources().mobAttack(this), damage);
            e.igniteForSeconds(6.0F);
            if (colour() == Colour.BLUE && fireColour() == Variants.FIRE_BLUE) {
                Fx.send(level, Fx.BLUE_BURN, e.position(), e.position(), 1, 130.0F, e.getId());
            }
        }
    }

    /** It breathes fire at its mark: comes within reach, faces it, and breathes when it may. */
    final class BreathGoal extends Goal {
        BreathGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return !master() && t != null && t.isAlive() && getControllingPassenger() == null && !isOrderedToSit()
                    && distanceToSqr(t) > 9.0 && distanceToSqr(t) < 24 * 24;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = Math.sqrt(distanceToSqr(t));
            if (d > reach() * 0.8) {
                getNavigation().moveTo(t, 1.2);
            } else {
                getNavigation().stop();
                if (hasLineOfSight(t)) {
                    breathe(t.getEyePosition().subtract(mouth()));
                }
            }
        }
    }

    // ---- the masters' judgment ----

    private void masterTick(ServerLevel level) {
        setNoGravity(true);
        noPhysics = phase() != Phase.SLEEP;
        phaseTicks++;
        ServerPlayer p = judged == null ? null : (ServerPlayer) level.getPlayerByUUID(judged);
        if (phase() != Phase.SLEEP && phase() != Phase.RETURN && (p == null || !p.isAlive() || p.distanceToSqr(Vec3.atCenterOf(centre)) > 48 * 48)) {
            setPhase(Phase.RETURN);
        }
        Vec3 c = Vec3.atBottomCenterOf(centre);
        double side = colour() == Colour.RED ? 0.0 : Math.PI;
        switch (phase()) {
            case SLEEP -> {
                setDeltaMovement(Vec3.ZERO);
                if (phaseTicks % 20 == 0) {
                    ServerPlayer comer = comer(level);
                    if (comer != null) {
                        judged = comer.getUUID();
                        setPhase(Phase.RISE);
                        playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, colour() == Colour.RED ? 0.8F : 0.95F);
                    }
                }
            }
            case RISE -> {
                flyTo(c.add(Math.cos(side) * 7, 7, Math.sin(side) * 7), 0.5);
                if (phaseTicks >= RISE_TICKS) {
                    setPhase(Phase.CIRCLE);
                }
            }
            case CIRCLE -> {
                circle(p, 9.0, 5.0, 0.06, side);
                if (phaseTicks >= CIRCLE_TICKS) {
                    boolean worthy = EternalFlameItem.carries(p) || LionTurtle.hero(p);
                    setPhase(worthy ? Phase.BLESS : Phase.BURN);
                    if (worthy) {
                        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.enlightened.dragons.worthy").withStyle(ChatFormatting.GOLD)));
                    } else {
                        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.enlightened.dragons.unworthy").withStyle(ChatFormatting.DARK_RED)));
                    }
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(worthy ? "message.enlightened.dragons.worthy.sub"
                            : "message.enlightened.dragons.unworthy.sub")));
                }
            }
            case BLESS -> {
                circle(p, 7.5, 3.0 + Mth.sin(phaseTicks * 0.05F) * 1.5, 0.1, side);
                Vec3 feet = p.position();
                if (phaseTicks % 5 == 0) {
                    // Fire round him, not at him: ahead along the circle it flies.
                    Vec3 ahead = getDeltaMovement().lengthSqr() > 1.0E-4 ? getDeltaMovement().normalize() : getLookAngle();
                    Fx.send(level, Fx.DRAGON_FIRE, mouth(), mouth().add(ahead.scale(9.0)), Variants.FIRE_RAINBOW, 0.8F, getId());
                }
                if (colour() == Colour.RED && phaseTicks % 20 == 1) {
                    Fx.send(level, Fx.DRAGON_VORTEX, feet, feet.add(0, 8, 0), Variants.FIRE_RAINBOW, 4.0F, p.getId());
                }
                if (phaseTicks >= BLESS_TICKS) {
                    if (blesser(level)) {
                        bless(p);
                    }
                    lastJudged.put(p.getUUID(), level.getGameTime());
                    setPhase(Phase.RETURN);
                }
            }
            case BURN -> {
                Vec3 at = p.position().add(Math.cos(side + phaseTicks * 0.02) * 8, 4, Math.sin(side + phaseTicks * 0.02) * 8);
                flyTo(at, 0.6);
                faceTowards(p.getEyePosition().subtract(position()));
                if (phaseTicks > 10 && breathing == 0 && breathRest == 0) {
                    breathe(p.getEyePosition().subtract(mouth()));
                }
                if (breathing > 0) {
                    breathDir = p.getEyePosition().subtract(mouth()).normalize();
                }
                if (phaseTicks >= BURN_TICKS) {
                    lastJudged.put(p.getUUID(), level.getGameTime());
                    breathing = 0;
                    setPhase(Phase.RETURN);
                }
            }
            case RETURN -> {
                Vec3 home = Vec3.atBottomCenterOf(cave);
                flyTo(home, 0.5);
                if (position().distanceTo(home) < 1.0 || phaseTicks > 400) {
                    moveTo(home.x, home.y, home.z, cave.getX() < centre.getX() ? -90.0F : 90.0F, 0.0F);
                    setYHeadRot(getYRot());
                    setYBodyRot(getYRot());
                    judged = null;
                    setPhase(Phase.SLEEP);
                }
            }
        }
    }

    /** Someone on the platform the masters have not lately judged. */
    @Nullable
    private ServerPlayer comer(ServerLevel level) {
        long now = level.getGameTime();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !p.isAlive()) {
                continue;
            }
            double dx = p.getX() - (centre.getX() + 0.5);
            double dz = p.getZ() - (centre.getZ() + 0.5);
            if (dx * dx + dz * dz > 14 * 14 || Math.abs(p.getY() - centre.getY()) > 4) {
                continue;
            }
            Long last = lastJudged.get(p.getUUID());
            if (last != null && now - last < REST) {
                continue;
            }
            return p;
        }
        return null;
    }

    /** Ran blesses (or Shaw, when Ran is not there). */
    private boolean blesser(ServerLevel level) {
        if (colour() == Colour.RED) {
            return true;
        }
        return level.getEntitiesOfClass(DragonEntity.class, getBoundingBox().inflate(64), d -> d.master() && d.colour() == Colour.RED).isEmpty();
    }

    /**
     * The blessing: fire stronger, rainbow fire to choose, and a golden egg (for one who brought the flame, each time;
     * for a hero who did not, the first time only).
     */
    public static void bless(ServerPlayer p) {
        BendingData data = Benders.get(p);
        boolean first = !data.dragonBlessed();
        data.setDragonBlessed(true);
        Benders.syncIfDirty(p);
        boolean flame = EternalFlameItem.give(p);
        CompoundTag kept = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        boolean egg = flame || !kept.getBoolean("enlightened_dragon_egg");
        if (egg) {
            kept.putBoolean("enlightened_dragon_egg", true);
            p.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
            ItemStack stack = new ItemStack(ModBlocks.GOLDEN_DRAGON_EGG_ITEM.get());
            if (!p.getInventory().add(stack)) {
                p.drop(stack, false);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        p.sendSystemMessage(Component.translatable(first ? "message.enlightened.dragons.blessed" : "message.enlightened.dragons.again")
                .withStyle(ChatFormatting.GOLD));
        if (egg) {
            p.sendSystemMessage(Component.translatable("message.enlightened.dragons.egg").withStyle(ChatFormatting.YELLOW));
        }
    }

    private void circle(ServerPlayer p, double radius, double up, double speed, double side) {
        double a = side + tickCount * speed;
        Vec3 at = p.position().add(Math.cos(a) * radius, up, Math.sin(a) * radius);
        flyTo(at, 0.9);
    }

    private void flyTo(Vec3 at, double speed) {
        Vec3 to = at.subtract(position());
        double d = to.length();
        Vec3 want = d < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(speed, d * 0.3) / d);
        Vec3 v = getDeltaMovement().add(want.subtract(getDeltaMovement()).scale(0.25));
        setDeltaMovement(v);
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (v.lengthSqr() > 0.004 && phase() != Phase.BURN) {
            faceTowards(v);
        }
    }

    private void faceTowards(Vec3 dir) {
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * Mth.RAD_TO_DEG);
        setYRot(Mth.approachDegrees(getYRot(), yaw, 12.0F));
        setXRot(Mth.clamp(pitch, -60.0F, 60.0F));
        yBodyRot = yHeadRot = getYRot();
    }

    // ---- ticking ----

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (master()) {
                masterTick(server);
            } else {
                setNoGravity(getControllingPassenger() != null);
                if (getControllingPassenger() == null && airborne()) {
                    Vec3 v = getDeltaMovement();
                    if (v.y < -0.15) {
                        setDeltaMovement(v.x, -0.15, v.z);
                    }
                }
            }
            if (entityData.get(BREATHING) != breathing > 0) {
                entityData.set(BREATHING, breathing > 0);
            }
            if (breathing > 0) {
                breathTick(server);
                if (--breathing == 0) {
                    breathRest = BREATH_REST;
                }
            } else if (breathRest > 0) {
                breathRest--;
            }
        } else {
            Vec3 head = position();
            if (trail.isEmpty() || trail.peekFirst().distanceToSqr(head) > 0.04) {
                trail.addFirst(head);
                while (trail.size() > 120) {
                    trail.removeLast();
                }
            }
            if (airborne() && tickCount % 5 == 0 && !master()) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, getX(), getY() + getBbHeight() * 0.5, getZ(), 0, 0.01, 0);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (master()) {
            // The masters are not to be harmed: only the void, or a Creative player, can.
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.getEntity() instanceof Player pl && pl.isCreative()) {
                return super.hurt(source, amount);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !master() && super.isPushable();
    }

    @Override
    public void push(double x, double y, double z) {
        if (!master()) {
            super.push(x, y, z);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !spares(target) && super.canAttack(target);
    }

    // ---- saving ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Colour", entityData.get(COLOUR));
        tag.putBoolean("Master", master());
        if (master()) {
            tag.putIntArray("Cave", new int[]{cave.getX(), cave.getY(), cave.getZ()});
            tag.putIntArray("Centre", new int[]{centre.getX(), centre.getY(), centre.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(COLOUR, tag.getInt("Colour"));
        if (tag.getBoolean("Master")) {
            int[] c = tag.getIntArray("Cave");
            int[] m = tag.getIntArray("Centre");
            if (c.length == 3 && m.length == 3) {
                makeMaster(colour(), new BlockPos(c[0], c[1], c[2]), new BlockPos(m[0], m[1], m[2]));
            } else {
                entityData.set(MASTER, true);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return master() && phase() == Phase.SLEEP ? null : SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected float getSoundVolume() {
        return isBaby() ? 0.4F : master() ? 2.0F : 0.8F;
    }

    @Override
    public float getVoicePitch() {
        return isBaby() ? 1.8F : 1.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** For tests: the master's judgment. */
    public int phaseTicks() {
        return phaseTicks;
    }

    public List<UUID> judgedLately() {
        return new ArrayList<>(lastJudged.keySet());
    }
}
