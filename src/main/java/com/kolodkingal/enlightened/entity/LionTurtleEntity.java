package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.world.LionTurtle;
import com.kolodkingal.enlightened.world.LionTurtles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The head of a lion turtle, rising from the sea at the front of its island ({@link LionTurtle}, kept and moved by
 * {@link LionTurtles}): a great lion's face with a crown of spined fins, a beard of them under its jaw, flippers either
 * side. It cannot be harmed and never leaves; its size is its island's. Spoken to by a Hero of the Village, it reaches
 * out its claws and gives energybending.
 */
public class LionTurtleEntity extends PathfinderMob {
    /** How long the giving lasts: the claws reach out, touch, and draw back. */
    public static final int GIFT_TICKS = 120;
    private static final EntityDataAccessor<Integer> SCALE = SynchedEntityData.defineId(LionTurtleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GIFT_TIME = SynchedEntityData.defineId(LionTurtleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GIFT_TO = SynchedEntityData.defineId(LionTurtleEntity.class, EntityDataSerializers.INT);
    private int island = -1;
    private Direction heading = Direction.NORTH;

    public LionTurtleEntity(EntityType<? extends LionTurtleEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SCALE, 1);
        builder.define(GIFT_TIME, Integer.MIN_VALUE / 2);
        builder.define(GIFT_TO, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 96.0F, 1.0F));
    }

    /** Puts the head at the front of its island, facing its way, as big as the island is. */
    public void setIsland(LionTurtles.Island i) {
        this.island = i.id;
        this.heading = i.heading;
        setScale(i.scale);
        BlockPos head = LionTurtle.headPos(i.shape(), i.center, i.heading);
        float yaw = heading.toYRot();
        moveTo(head.getX() + 0.5, head.getY(), head.getZ() + 0.5, yaw, 0.0F);
        setYHeadRot(yaw);
        yBodyRot = yaw;
    }

    public int scale() {
        return entityData.get(SCALE);
    }

    public void setScale(int scale) {
        entityData.set(SCALE, Math.max(1, scale));
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // Reaching back over the rim of the shell, so one standing at the front of the island may speak to it.
        int s = scale();
        return EntityDimensions.scalable(6.4F * s, 5.0F * s).withEyeHeight(4.2F * s);
    }

    public Direction heading() {
        return heading;
    }

    // ---- the gift ----

    /** Begins the giving: the claws reach out to {@code p}. */
    public void beginGift(Player p) {
        entityData.set(GIFT_TIME, (int) level().getGameTime());
        entityData.set(GIFT_TO, p.getId());
    }

    /** How far through the giving it is, 0 to 1, or -1 when it is not giving. */
    public float gift(float partial) {
        float f = ((int) level().getGameTime() - entityData.get(GIFT_TIME) + partial) / GIFT_TICKS;
        return f >= 0.0F && f <= 1.0F ? f : -1.0F;
    }

    /** Who it is giving to, if anyone. */
    public Entity giftTarget() {
        return gift(0.0F) < 0.0F ? null : level().getEntity(entityData.get(GIFT_TO));
    }

    /** Where its claws reach out from: the tip of its right flipper, raised high beside its face. */
    public Vec3 clawRoot(Entity toward) {
        int s = scale();
        double yaw = Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return position().add(forward.scale(0.6 * s)).add(right.scale(4.0 * s)).add(0, 5.8 * s, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        noPhysics = true;
        yBodyRot = heading.toYRot();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server && gift(0.0F) < 0.0F) {
            LionTurtle.talk(server, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.getEntity() instanceof Player p && p.isCreative()) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0 * 512.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Island", island);
        tag.putString("Heading", heading.getSerializedName());
        tag.putInt("Scale", scale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        island = tag.contains("Island") ? tag.getInt("Island") : -1;
        Direction d = Direction.byName(tag.getString("Heading"));
        heading = d != null && d.getAxis().isHorizontal() ? d : Direction.NORTH;
        setScale(tag.contains("Scale") ? tag.getInt("Scale") : 1);
    }
}
