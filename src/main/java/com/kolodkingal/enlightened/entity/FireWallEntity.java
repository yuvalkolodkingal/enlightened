package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A wall of fire a firebender raised: a line of flame {@code width} wide and three high, facing the way he looked,
 * that burns and throws back whoever walks into it and eats the arrows shot through it, for its lifetime. The client
 * draws it as a curtain of fire.
 */
public class FireWallEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(FireWallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(FireWallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_BLUE = SynchedEntityData.defineId(FireWallEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID owner;
    private int life;
    private int lifetime = 120;
    private float damage = 2.0F;

    public FireWallEntity(EntityType<? extends FireWallEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public FireWallEntity(Level level, ServerPlayer owner, Vec3 at, float yaw, float width, int lifetime, float damage, boolean blue) {
        this(ModEntities.FIRE_WALL.get(), level);
        this.owner = owner.getUUID();
        this.lifetime = lifetime;
        this.damage = damage;
        setPos(at.x, at.y, at.z);
        entityData.set(DATA_WIDTH, width);
        entityData.set(DATA_YAW, yaw);
        entityData.set(DATA_BLUE, blue);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, 5.0F);
        builder.define(DATA_YAW, 0.0F);
        builder.define(DATA_BLUE, false);
    }

    public float width() {
        return entityData.get(DATA_WIDTH);
    }

    /** The yaw (degrees) the wall faces: it runs across that direction. */
    public float yaw() {
        return entityData.get(DATA_YAW);
    }

    public boolean blue() {
        return entityData.get(DATA_BLUE);
    }

    public int lifetime() {
        return lifetime;
    }

    public int life() {
        return life;
    }

    /** The unit vector along the wall (across the way it faces). */
    public Vec3 along() {
        double yaw = Math.toRadians(yaw());
        return new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        if (level().isClientSide) {
            return;
        }
        if (life > lifetime) {
            discard();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Player owner = this.owner == null ? null : level.getPlayerByUUID(this.owner);
        float half = width() * 0.5F;
        Vec3 along = along();
        AABB box = new AABB(getX() - half - 0.5, getY(), getZ() - half - 0.5, getX() + half + 0.5, getY() + 3.0, getZ() + half + 0.5);
        for (Entity e : level.getEntities(this, box, e -> e.isAlive() && e != owner && !(e instanceof FireWallEntity) && !(e instanceof BendingShotEntity))) {
            // Inside the wall's thickness: the distance across the wall's line.
            Vec3 rel = e.position().subtract(position());
            double across = rel.x * -along.z + rel.z * along.x;
            double alongDist = rel.x * along.x + rel.z * along.z;
            if (Math.abs(across) > 0.8 || Math.abs(alongDist) > half + 0.3) {
                continue;
            }
            if (e instanceof Projectile) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, e.getX(), e.getY(), e.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
                e.discard();
                continue;
            }
            if (e instanceof LivingEntity living) {
                if (owner instanceof ServerPlayer p && !AreaTargets.mayHurt(p, living)) {
                    continue;
                }
                if (tickCount % 10 == 0) {
                    living.hurt(ModDamageTypes.source(level, ModDamageTypes.FLAME, this, owner), damage);
                    living.igniteForSeconds(4);
                }
                Shots.push(living, position().add(along.scale(alongDist)), 0.4, 0.2);
            }
        }
        if (tickCount % 2 == 0) {
            double t = (random.nextDouble() - 0.5) * width();
            level.sendParticles(blue() ? net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME : net.minecraft.core.particles.ParticleTypes.FLAME,
                    getX() + along.x * t, getY() + random.nextDouble() * 2.5, getZ() + along.z * t, 1, 0.1, 0.1, 0.1, 0.01);
        }
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
    public boolean hurt(DamageSource source, float amount) {
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

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        life = tag.getInt("Life");
        lifetime = tag.getInt("Lifetime");
        damage = tag.getFloat("Damage");
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", life);
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("Damage", damage);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }
}
