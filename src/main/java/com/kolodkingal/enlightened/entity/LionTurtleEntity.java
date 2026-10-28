package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.world.LionTurtle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The head of a lion turtle: a great lion's face with a mane of weed, rising from the sea at the front of its island
 * ({@link LionTurtle}). It cannot be harmed and never leaves. While someone is near it swims, a block at a time, its
 * island and all on it with it; where land, ice or another shell bars its way it turns. Spoken to by a Hero of the
 * Village, it gives energybending.
 */
public class LionTurtleEntity extends PathfinderMob {
    private BlockPos island;
    private Direction heading = Direction.NORTH;
    private int swim;

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
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32.0F, 1.0F));
    }

    /** Sets the island it carries (its centre at sea level) and the way it faces, and puts the head at its front. */
    public void setIsland(BlockPos center, Direction heading) {
        this.island = center.immutable();
        this.heading = heading;
        BlockPos head = LionTurtle.headPos(center, heading);
        float yaw = heading.toYRot();
        moveTo(head.getX() + 0.5, head.getY(), head.getZ() + 0.5, yaw, 0.0F);
        setYHeadRot(yaw);
        yBodyRot = yaw;
    }

    public BlockPos island() {
        return island;
    }

    public Direction heading() {
        return heading;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        noPhysics = true;
        yBodyRot = heading.toYRot();
        if (!(level() instanceof ServerLevel server) || island == null) {
            return;
        }
        // It swims only while someone is near to ride it: an island left alone stays where it was left.
        if (server.getNearestPlayer(getX(), getY(), getZ(), 96.0, false) == null) {
            return;
        }
        if (++swim < LionTurtle.moveTicks()) {
            return;
        }
        swim = 0;
        swimOn(server);
    }

    /** One stroke: a block ahead, or, where the way is barred, a turn toward open sea. */
    public void swimOn(ServerLevel server) {
        if (!LionTurtle.openAhead(server, island, heading)) {
            for (Direction turn : new Direction[]{heading.getClockWise(), heading.getCounterClockWise(), heading.getOpposite()}) {
                if (LionTurtle.openAhead(server, island, turn)) {
                    LionTurtle.turn(server, island, heading, turn);
                    setIsland(island, turn);
                    return;
                }
            }
            return;
        }
        LionTurtle.shift(server, LionTurtle.box(island), heading, island.getY());
        setIsland(island.relative(heading), heading);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
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
        return distance < 256.0 * 256.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (island != null) {
            tag.putInt("IslandX", island.getX());
            tag.putInt("IslandY", island.getY());
            tag.putInt("IslandZ", island.getZ());
            tag.putString("Heading", heading.getSerializedName());
        }
        tag.putInt("Swim", swim);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IslandX")) {
            island = new BlockPos(tag.getInt("IslandX"), tag.getInt("IslandY"), tag.getInt("IslandZ"));
            Direction d = Direction.byName(tag.getString("Heading"));
            heading = d != null && d.getAxis().isHorizontal() ? d : Direction.NORTH;
        }
        swim = tag.getInt("Swim");
    }
}
