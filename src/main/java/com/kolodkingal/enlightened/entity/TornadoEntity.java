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
 *
 * <p>1.1.0: another bender can cast his element into it (a team combo, {@link com.kolodkingal.enlightened.bending.TeamCombos}):
 * fire makes it a fire tornado (much shorter, much harder), water a great water tornado, earth a dust devil, ice a
 * blizzard. Its {@link Kind} is synced, so the clients draw the funnel in that element's colours.
 */
public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);

    /** What the tornado is made of: air, or the element another bender cast into it. */
    public enum Kind {
        AIR, FIRE, WATER, SAND, ICE;

        public static Kind byId(int id) {
            Kind[] all = values();
            return id >= 0 && id < all.length ? all[id] : AIR;
        }
    }

    @Nullable
    private UUID owner;
    private Vec3 heading = new Vec3(0, 0, 1);
    private int life;
    private int lifetime = 120;
    private float damage = 1.0F;
    private boolean aggressive;
    /** The bender who cast his element into it (a team combo), who shares its kills. */
    @Nullable
    private UUID partner;

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
        builder.define(DATA_KIND, 0);
    }

    public float size() {
        return entityData.get(DATA_SIZE);
    }

    public Kind kind() {
        return Kind.byId(entityData.get(DATA_KIND));
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Nullable
    public UUID partner() {
        return partner;
    }

    /** Ticks it has left to live. */
    public int left() {
        return Math.max(0, lifetime - life);
    }

    public float damage() {
        return damage;
    }

    /**
     * Another bender's element joins the tornado: it becomes that kind, its size, life and blows changed by the
     * factors given ({@code life} scales the time it has left). Only a tornado of plain air takes an element.
     */
    public boolean infuse(Kind kind, UUID partner, float sizeFactor, double lifeFactor, float damageFactor) {
        if (kind() != Kind.AIR || kind == Kind.AIR) {
            return false;
        }
        entityData.set(DATA_KIND, kind.ordinal());
        entityData.set(DATA_SIZE, size() * sizeFactor);
        this.partner = partner;
        lifetime = life + Math.max(10, (int) Math.round((lifetime - life) * lifeFactor));
        damage *= damageFactor;
        aggressive = true;
        return true;
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
            if (partner != null && partner.equals(e.getUUID())) {
                continue; // the bender who fed it is not its prey
            }
            Vec3 to = e.position().subtract(position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            double d = flat.length();
            Vec3 tangent = d < 0.1 ? new Vec3(1, 0, 0) : new Vec3(-flat.z, 0, flat.x).normalize();
            double lift = e.getY() - getY() < height - 1.0 ? 0.25 : 0.0;
            Vec3 pull = d < 0.1 ? Vec3.ZERO : flat.normalize().scale(kind() == Kind.WATER ? -0.14 : -0.08);
            e.setDeltaMovement(tangent.scale(kind() == Kind.WATER ? 0.45 : 0.35).add(pull).add(0, lift, 0));
            e.hurtMarked = true;
            e.fallDistance = 0;
            // Fire strikes twice as often as air; every other kind as air does.
            int every = kind() == Kind.FIRE ? 5 : 10;
            if (e instanceof LivingEntity living && tickCount % every == 0 && owner instanceof ServerPlayer p) {
                living.hurt(ModDamageTypes.source(level, damageType(), this, p), damage + (aggressive ? 1.0F : 0F));
                touch(p, living);
            }
            if (e instanceof Projectile || e instanceof ItemEntity) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.1, 0));
            }
        }
        // The funnel: rings of cloud (or of the element cast into it) climbing round the axis.
        if (tickCount % 2 == 0) {
            net.minecraft.core.particles.ParticleOptions grain = switch (kind()) {
                case AIR -> ParticleTypes.CLOUD;
                case FIRE -> ParticleTypes.FLAME;
                case WATER -> ParticleTypes.SPLASH;
                case SAND -> new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.SAND.defaultBlockState());
                case ICE -> ParticleTypes.SNOWFLAKE;
            };
            int count = kind() == Kind.AIR ? 6 : 12;
            for (int i = 0; i < count; i++) {
                double t = (tickCount * 0.3 + i * 1.047);
                double h = random.nextDouble() * height;
                double r = size * (0.3 + 0.7 * h / height);
                level.sendParticles(grain, getX() + Math.cos(t + h) * r, getY() + h, getZ() + Math.sin(t + h) * r, 1, 0, 0, 0, 0.0);
            }
            if (kind() == Kind.FIRE && tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.LAVA, getX(), getY() + height * 0.5, getZ(), 2, size * 0.4, height * 0.3, size * 0.4, 0.0);
            } else if (kind() == Kind.WATER && tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + height * 0.5, getZ(), 6, size * 0.6, height * 0.4, size * 0.6, 0.05);
            }
            if (kind() == Kind.FIRE && tickCount % 20 == 0) {
                level.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.FIRE_AMBIENT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
            }
            if (onGround()) {
                level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, getX(), getY() + 0.1, getZ(), 1, 0, 0, 0, 0.0);
            }
        }
    }

    private net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> damageType() {
        return switch (kind()) {
            case AIR -> ModDamageTypes.AIR;
            case FIRE -> ModDamageTypes.FIRE;
            case WATER -> ModDamageTypes.WATER;
            case SAND -> ModDamageTypes.EARTH;
            case ICE -> ModDamageTypes.ICE;
        };
    }

    /** What the element in the funnel does besides the blow: burns, drowns the flames, blinds, freezes. */
    private void touch(ServerPlayer owner, LivingEntity living) {
        switch (kind()) {
            case FIRE -> com.kolodkingal.enlightened.fire.FireBending.ignite(owner, living, 4);
            case WATER -> {
                living.clearFire();
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            case SAND -> {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0));
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            case ICE -> {
                living.setTicksFrozen(living.getTicksRequiredToFreeze() + 40);
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.kolodkingal.enlightened.registry.ModEffects.FROZEN, 30, 0));
            }
            default -> {
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
        entityData.set(DATA_KIND, tag.getInt("kind"));
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
        tag.putInt("kind", kind().ordinal());
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
