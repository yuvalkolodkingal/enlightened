package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A small tornado an airbender sent ahead of him: it walks forward, lifts and spins everything it meets (creatures,
 * items, arrows), hurts a little, and dies out after its time. Its size (radius, height, reach) is the airbender's
 * level.
 */
public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID owner;
    private Vec3 heading = new Vec3(0, 0, 1);
    private int life;
    private int lifetime = 120;
    private float damage = 1.0F;
    private boolean aggressive;

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
        super(type, level);
    }

    public TornadoEntity(Level level, ServerPlayer owner, Vec3 at, Vec3 heading, float size, int lifetime, float damage, boolean aggressive) {
        this(ModEntities.TORNADO.get(), level);
        this.owner = owner.getUUID();
        this.heading = new Vec3(heading.x, 0, heading.z).normalize();
        this.lifetime = lifetime;
        this.damage = damage;
        this.aggressive = aggressive;
        setPos(at.x, at.y, at.z);
        entityData.set(DATA_SIZE, size);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 1.5F);
    }

    public float size() {
        return entityData.get(DATA_SIZE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        life++;
        if (life > lifetime) {
            discard();
            return;
        }
        float size = size();
        // It walks forward, settles on the ground, and stops at a wall.
        Vec3 step = heading.scale(0.18);
        move(MoverType.SELF, new Vec3(step.x, onGround() ? 0 : -0.3, step.z));
        if (horizontalCollision) {
            heading = heading.yRot((float) Math.PI * 0.5F);
        }
        ServerLevel level = (ServerLevel) level();
        double height = size * 2.5;
        Player owner = this.owner == null ? null : level.getPlayerByUUID(this.owner);
        AABB box = new AABB(getX() - size, getY(), getZ() - size, getX() + size, getY() + height, getZ() + size);
        for (Entity e : level.getEntities(this, box, e -> e.isAlive() && e != owner && !(e instanceof TornadoEntity) && !(e instanceof BendingShotEntity))) {
            if (e instanceof LivingEntity living && owner instanceof ServerPlayer p && !AreaTargets.mayHurt(p, living)) {
                continue;
            }
            if (e instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            Vec3 to = e.position().subtract(position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            double d = flat.length();
            Vec3 tangent = d < 0.1 ? new Vec3(1, 0, 0) : new Vec3(-flat.z, 0, flat.x).normalize();
            double lift = e.getY() - getY() < height - 1.0 ? 0.25 : 0.0;
            Vec3 pull = d < 0.1 ? Vec3.ZERO : flat.normalize().scale(-0.08);
            e.setDeltaMovement(tangent.scale(0.35).add(pull).add(0, lift, 0));
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (e instanceof LivingEntity living && tickCount % 10 == 0 && owner instanceof ServerPlayer p) {
                living.hurt(ModDamageTypes.source(level, ModDamageTypes.AIR, this, p), damage + (aggressive ? 1.0F : 0F));
            }
            if (e instanceof Projectile || e instanceof ItemEntity) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.1, 0));
            }
        }
        // The funnel: rings of cloud climbing round the axis.
        if (tickCount % 2 == 0) {
            for (int i = 0; i < 6; i++) {
                double t = (tickCount * 0.3 + i * 1.047);
                double h = random.nextDouble() * height;
                double r = size * (0.3 + 0.7 * h / height);
                level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(t + h) * r, getY() + h, getZ() + Math.sin(t + h) * r, 1, 0, 0, 0, 0.0);
            }
            if (onGround()) {
                level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, getX(), getY() + 0.1, getZ(), 1, 0, 0, 0, 0.0);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            owner = tag.getUUID("owner");
        }
        heading = new Vec3(tag.getDouble("hx"), 0, tag.getDouble("hz"));
        if (heading.lengthSqr() < 1.0E-4) {
            heading = new Vec3(0, 0, 1);
        }
        life = tag.getInt("life");
        lifetime = tag.getInt("lifetime");
        damage = tag.getFloat("damage");
        aggressive = tag.getBoolean("aggressive");
        entityData.set(DATA_SIZE, tag.contains("size") ? tag.getFloat("size") : 1.5F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putDouble("hx", heading.x);
        tag.putDouble("hz", heading.z);
        tag.putInt("life", life);
        tag.putInt("lifetime", lifetime);
        tag.putFloat("damage", damage);
        tag.putBoolean("aggressive", aggressive);
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
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
