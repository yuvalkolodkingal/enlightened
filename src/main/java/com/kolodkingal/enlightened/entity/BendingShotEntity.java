package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.Raycasts;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Something a bender shot or holds in the air: a rock or a cloud of sand (drawn as the block it came from), a metal
 * blade, an ice knife, a snowball, a lava shuriken, an air blade or a fire ball. Held, it floats in front of its
 * bender where he looks (several at once orbit him); launched, it flies, hurts the first creature it hits with the
 * kind's touch (fire burns, ice freezes, sand blinds, snow slows) and shatters on a block.
 */
public class BendingShotEntity extends Entity {
    public enum Kind {
        ROCK, SAND, METAL_BLADE, ICE_KNIFE, SNOWBALL, LAVA, AIR_BLADE, FIRE_BALL;

        public boolean block() {
            return this == ROCK || this == SAND;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(BendingShotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockState> DATA_BLOCK = SynchedEntityData.defineId(BendingShotEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Boolean> DATA_HELD = SynchedEntityData.defineId(BendingShotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(BendingShotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BendingShotEntity.class, EntityDataSerializers.FLOAT);

    private static final int MAX_LIFE = 20 * 30;

    @Nullable
    private UUID owner;
    private float damage = 4.0F;
    /** The burn, freeze or slow the kind leaves, in seconds. */
    private int touch = 3;
    private int life;
    /** While held: this shot's place in its bender's orbit (0 = straight ahead). */
    private int orbit;
    private boolean launched;
    private boolean dropsItem;

    public BendingShotEntity(EntityType<? extends BendingShotEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public BendingShotEntity(Level level, Kind kind, Vec3 at, ServerPlayer owner, int color) {
        this(ModEntities.SHOT.get(), level);
        setKind(kind);
        setPos(at.x, at.y, at.z);
        xo = at.x;
        yo = at.y;
        zo = at.z;
        this.owner = owner.getUUID();
        entityData.set(DATA_COLOR, color);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, Kind.ROCK.ordinal());
        builder.define(DATA_BLOCK, Blocks.STONE.defaultBlockState());
        builder.define(DATA_HELD, false);
        builder.define(DATA_COLOR, 0xFFFFFF);
        builder.define(DATA_SIZE, 1.0F);
    }

    // ---- what it is ----

    public Kind kind() {
        int k = entityData.get(DATA_KIND);
        return k >= 0 && k < Kind.values().length ? Kind.values()[k] : Kind.ROCK;
    }

    public void setKind(Kind kind) {
        entityData.set(DATA_KIND, kind.ordinal());
    }

    public BlockState blockState() {
        return entityData.get(DATA_BLOCK);
    }

    public void setBlockState(BlockState state) {
        entityData.set(DATA_BLOCK, state);
    }

    public boolean held() {
        return entityData.get(DATA_HELD);
    }

    public int color() {
        return entityData.get(DATA_COLOR);
    }

    public float size() {
        return entityData.get(DATA_SIZE);
    }

    public void setSize(float size) {
        entityData.set(DATA_SIZE, size);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setTouch(int seconds) {
        this.touch = seconds;
    }

    /** Whether a rock set down or shattered drops the block's item (a block torn out of the ground does). */
    public void setDropsItem(boolean drops) {
        this.dropsItem = drops;
    }

    public void hold(int orbit) {
        this.orbit = orbit;
        this.launched = false;
        entityData.set(DATA_HELD, true);
    }

    public int orbit() {
        return orbit;
    }

    /** Launches the shot along {@code direction} at {@code speed} blocks a tick. */
    public void launch(Vec3 direction, double speed) {
        setDeltaMovement(direction.normalize().scale(speed));
        launched = true;
        entityData.set(DATA_HELD, false);
        life = 0;
    }

    @Nullable
    private Player ownerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    /** The trail each shot leaves (client only): heat off a fire ball, grit off a rock, frost off ice, embers off lava. */
    private void clientTrail() {
        Vec3 v = getDeltaMovement();
        boolean moving = v.lengthSqr() > 0.01;
        switch (kind()) {
            case FIRE_BALL -> {
                if (random.nextInt(2) == 0) {
                    level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), -v.x * 0.1, 0.02 - v.y * 0.1, -v.z * 0.1);
                }
                if (random.nextInt(4) == 0) {
                    level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 0, 0.01, 0);
                }
            }
            case ROCK, SAND -> {
                if (moving && random.nextInt(2) == 0 && blockState().getRenderShape() == RenderShape.MODEL) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState()), getX(), getY() + 0.3, getZ(),
                            -v.x * 0.2, 0.02, -v.z * 0.2);
                } else if (held() && random.nextInt(6) == 0) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState()), getX(), getY() + 0.1, getZ(), 0, -0.02, 0);
                }
            }
            case ICE_KNIFE, SNOWBALL -> {
                if (moving && random.nextInt(3) == 0) {
                    level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.3, getZ(), 0, 0, 0);
                }
            }
            case LAVA -> {
                if (random.nextInt(2) == 0) {
                    level().addParticle(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 0, 0, 0);
                }
            }
            case METAL_BLADE -> {
                if (moving && random.nextInt(3) == 0) {
                    level().addParticle(ParticleTypes.CRIT, getX(), getY() + 0.3, getZ(), 0, 0, 0);
                }
            }
            case AIR_BLADE -> {
            }
        }
    }

    // ---- life ----

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTrail();
            return;
        }
        life++;
        if (life > MAX_LIFE) {
            shatter(null);
            return;
        }
        if (held()) {
            tickHeld();
            return;
        }
        if (!launched) {
            // Set down and forgotten: it is nothing.
            shatter(null);
            return;
        }
        tickFlying();
    }

    /** Floats in front of its bender where he looks, in an orbit with the others he holds. */
    private void tickHeld() {
        Player p = ownerPlayer();
        if (p == null || !p.isAlive() || p.isSpectator()) {
            drop();
            return;
        }
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        double angle = (tickCount * 0.08) + orbit * (Math.PI * 2 / 3);
        Vec3 side = orbit == 0 ? Vec3.ZERO : right.scale(Math.cos(angle) * 1.2).add(0, Math.sin(angle) * 0.5, 0);
        Vec3 target = p.getEyePosition().add(look.scale(2.5)).add(side).add(0, -0.4, 0);
        Vec3 to = target.subtract(position());
        setDeltaMovement(to.scale(0.5));
        move(MoverType.SELF, getDeltaMovement());
        if (tickCount % 6 == 0 && level() instanceof ServerLevel level) {
            level.sendParticles(new com.kolodkingal.enlightened.particle.EnergyParticleOptions(color(), 0.8F), getX(), getY() + 0.3, getZ(), 1, 0.2, 0.2, 0.2, 0.0);
        }
    }

    /** Flies, falls a little (rocks and snow more than blades and fire), and hits what it meets. */
    private void tickFlying() {
        Vec3 from = position();
        Vec3 motion = getDeltaMovement();
        Vec3 to = from.add(motion);
        Player p = ownerPlayer();
        EntityHitResult entityHit = Raycasts.entityBetween(level(), this, from, to, 0.25,
                e -> e.isAlive() && e != p && !e.isSpectator() && !(e instanceof BendingShotEntity) && e.isPickable());
        if (entityHit != null) {
            hit(entityHit.getEntity());
            return;
        }
        BlockHitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            setPos(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
            shatter(blockHit.getBlockPos());
            return;
        }
        setPos(to.x, to.y, to.z);
        double gravity = switch (kind()) {
            case ROCK, SAND -> 0.03;
            case SNOWBALL -> 0.03;
            case LAVA, ICE_KNIFE, METAL_BLADE -> 0.01;
            case AIR_BLADE, FIRE_BALL -> 0.0;
        };
        setDeltaMovement(motion.x * 0.995, motion.y - gravity, motion.z * 0.995);
        if (level() instanceof ServerLevel level && tickCount % 2 == 0) {
            trail(level);
        }
    }

    private void trail(ServerLevel level) {
        switch (kind()) {
            case ROCK, SAND -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState()), getX(), getY() + 0.3, getZ(), 2, 0.1, 0.1, 0.1, 0.02);
            case METAL_BLADE -> level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.3, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            case ICE_KNIFE, SNOWBALL -> level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.3, getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            case LAVA -> level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            case AIR_BLADE -> level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
            case FIRE_BALL -> level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 3, 0.2 * size(), 0.2 * size(), 0.2 * size(), 0.01);
        }
    }

    private ResourceKey<DamageType> damageType() {
        return switch (kind()) {
            case ROCK, SAND, METAL_BLADE -> ModDamageTypes.EARTH;
            case ICE_KNIFE, SNOWBALL -> ModDamageTypes.ICE;
            case LAVA -> ModDamageTypes.LAVA;
            case AIR_BLADE -> ModDamageTypes.AIR;
            case FIRE_BALL -> ModDamageTypes.FIRE;
        };
    }

    private void hit(Entity target) {
        Player p = ownerPlayer();
        if (target instanceof LivingEntity living && p instanceof ServerPlayer owner && !AreaTargets.mayHurt(owner, living)) {
            shatter(null);
            return;
        }
        boolean hurt = target.hurt(ModDamageTypes.source(level(), damageType(), this, p), damage);
        if (hurt && target instanceof LivingEntity living) {
            switch (kind()) {
                case FIRE_BALL, LAVA -> {
                    if (p instanceof ServerPlayer owner) {
                        com.kolodkingal.enlightened.fire.FireBending.ignite(owner, living, touch);
                    } else {
                        living.igniteForSeconds(touch);
                    }
                }
                case ICE_KNIFE -> living.addEffect(new MobEffectInstance(ModEffects.FROZEN, touch * 20, 0));
                case SNOWBALL -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, touch * 20, 1));
                case SAND -> living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, touch * 20, 0));
                case AIR_BLADE, METAL_BLADE, ROCK -> {
                }
            }
            if (kind() == Kind.ROCK || kind() == Kind.SAND) {
                Vec3 push = getDeltaMovement().normalize().scale(0.6);
                living.push(push.x, 0.2, push.z);
            }
        }
        if (kind() == Kind.FIRE_BALL && size() >= 2.0F && level() instanceof ServerLevel level && p instanceof ServerPlayer owner) {
            // A big fire ball bursts: everything round the one it hit burns a little too.
            for (LivingEntity e : com.kolodkingal.enlightened.bending.Shots.area(owner, position(), size())) {
                if (e != target) {
                    e.hurt(ModDamageTypes.source(level, ModDamageTypes.FIRE, this, p), damage * 0.5F);
                    com.kolodkingal.enlightened.fire.FireBending.ignite(owner, e, touch);
                }
            }
        }
        shatter(null);
    }

    /** Breaks apart where it is: particles, a sound, and (for a rock torn out of the ground) the block's item. */
    public void shatter(@Nullable BlockPos against) {
        if (level() instanceof ServerLevel level) {
            switch (kind()) {
                case ROCK, SAND -> {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState()), getX(), getY() + 0.3, getZ(), 20, 0.3, 0.3, 0.3, 0.1);
                    level.playSound(null, getX(), getY(), getZ(), ModSounds.ROCK_HIT.get(), SoundSource.PLAYERS, 1.0F, kind() == Kind.SAND ? 1.4F : 1.0F);
                    if (dropsItem && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) {
                        spawnAtLocation(new net.minecraft.world.item.ItemStack(blockState().getBlock().asItem()));
                    }
                }
                case METAL_BLADE -> {
                    level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.3, getZ(), 12, 0.2, 0.2, 0.2, 0.1);
                    level.playSound(null, getX(), getY(), getZ(), ModSounds.METAL_CLANG.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
                }
                case ICE_KNIFE, SNOWBALL -> {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.3, getZ(), 16, 0.3, 0.3, 0.3, 0.05);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 1.5F);
                }
                case LAVA -> {
                    level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.0);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.0F);
                    if (against != null && level.getBlockState(against.above()).isAir() && level.random.nextInt(3) == 0
                            && ownerPlayer() instanceof ServerPlayer owner
                            && com.kolodkingal.enlightened.bending.BlockRights.mayPlace(owner, level, against.above())) {
                        level.setBlockAndUpdate(against.above(), Blocks.FIRE.defaultBlockState());
                    }
                }
                case AIR_BLADE -> level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                case FIRE_BALL -> {
                    level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), (int) (15 * size()), 0.3 * size(), 0.3 * size(), 0.3 * size(), 0.1);
                    level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 4, 0.2, 0.2, 0.2, 0.0);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
                    if (against != null && size() >= 1.5F && ownerPlayer() instanceof ServerPlayer owner) {
                        BlockPos above = against.above();
                        if (level.getBlockState(above).isAir() && com.kolodkingal.enlightened.bending.BlockRights.mayPlace(owner, level, above)) {
                            level.setBlockAndUpdate(above, com.kolodkingal.enlightened.fire.FireBending.fireBlock(com.kolodkingal.enlightened.bending.Benders.get(owner)));
                        }
                    }
                }
            }
        }
        discard();
    }

    /** Let go gently: a block comes down as its item, everything else vanishes. */
    public void drop() {
        if (kind().block() && dropsItem && level() instanceof ServerLevel level
                && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(blockState().getBlock().asItem()));
        }
        discard();
    }

    // ---- saving ----

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("kind")) {
            setKind(Kind.values()[Math.floorMod(tag.getInt("kind"), Kind.values().length)]);
        }
        if (tag.contains("block")) {
            setBlockState(NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("block")));
        }
        if (tag.hasUUID("owner")) {
            owner = tag.getUUID("owner");
        }
        damage = tag.getFloat("damage");
        touch = tag.getInt("touch");
        dropsItem = tag.getBoolean("drops");
        launched = tag.getBoolean("launched");
        entityData.set(DATA_COLOR, tag.getInt("color"));
        setSize(tag.contains("size") ? tag.getFloat("size") : 1.0F);
        // A held shot is never saved as held: its bender is gone when the chunk loads again.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("kind", kind().ordinal());
        tag.put("block", NbtUtils.writeBlockState(blockState()));
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putFloat("damage", damage);
        tag.putInt("touch", touch);
        tag.putBoolean("drops", dropsItem);
        tag.putBoolean("launched", launched);
        tag.putInt("color", color());
        tag.putFloat("size", size());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
