package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.quest.AvatarQuest;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One of the two great spirits: <b>Raava</b>, the spirit of light (white and pale blue, a kite of light trailing
 * ribbons), or <b>Vaatu</b>, the spirit of darkness (black and red, with long tendrils).
 *
 * <p>Raava waits in her glades, gentle: speak to her and she asks your help against Vaatu; speak to her again and she
 * enters you, and you are the Avatar ({@link AvatarQuest#merge}). Nothing harms her there. Where the world has no room
 * for an Avatar (or is on the fate path) she is not found.
 *
 * <p>In the Spirit World, set against one player ({@link #chosen}), either of them is a boss: it flies round him, strikes
 * him with bolts of its energy, lashes him when he comes near, and (Vaatu) calls dark spirits to it. Vaatu waits
 * peacefully while his offer stands.
 */
public class GreatSpiritEntity extends PathfinderMob {
    public enum Kind { RAAVA, VAATU }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(GreatSpiritEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(GreatSpiritEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float BOSS_HEALTH = 300.0F;
    /** How long Raava waits for an answer (ticks). */
    public static final int ANSWER = 1200;

    @Nullable
    private UUID chosen;
    @Nullable
    private UUID asked;
    private long askedAt;
    private int lost;
    private final ServerBossEvent boss = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    public GreatSpiritEntity(EntityType<? extends GreatSpiritEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        setPersistenceRequired();
        boss.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, BOSS_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
        builder.define(HOSTILE, false);
    }

    @Override
    protected void registerGoals() {
    }

    public Kind kind() {
        return Kind.values()[Math.floorMod(entityData.get(KIND), 2)];
    }

    public boolean hostile() {
        return entityData.get(HOSTILE);
    }

    @Nullable
    public UUID chosen() {
        return chosen;
    }

    /** Raava in her glade, waiting. */
    public static GreatSpiritEntity raavaAt(ServerLevel level, Vec3 at) {
        GreatSpiritEntity r = ModEntities.GREAT_SPIRIT.get().create(level);
        r.entityData.set(KIND, Kind.RAAVA.ordinal());
        r.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        return r;
    }

    /** Set against (or, for Vaatu waiting on his offer, before) one player in the Spirit World. */
    public void setup(Kind kind, ServerPlayer p, boolean hostile) {
        entityData.set(KIND, kind.ordinal());
        chosen = p.getUUID();
        boss.setName(Component.translatable("entity.enlightened.great_spirit." + kind.name().toLowerCase(java.util.Locale.ROOT)));
        boss.setColor(kind == Kind.RAAVA ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.RED);
        if (hostile) {
            turnHostile(p);
        }
    }

    public void turnHostile(ServerPlayer p) {
        entityData.set(HOSTILE, true);
        chosen = p.getUUID();
        setHealth(getMaxHealth());
        boss.setVisible(true);
        setTarget(p);
        playSound(kind() == Kind.VAATU ? SoundEvents.WARDEN_ROAR : SoundEvents.BEACON_ACTIVATE, 3.0F, 0.6F);
    }

    // ---- Raava in her glade ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || kind() != Kind.RAAVA || hostile() || chosen != null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer p) {
            p.sendSystemMessage(Component.translatable("raava.enlightened.says", Component.translatable(talk(p))).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** What Raava says to someone who speaks to her (doing what it says). */
    public String talk(ServerPlayer p) {
        if (!AvatarQuest.raavaFree(p.getServer())) {
            return "raava.enlightened.taken";
        }
        if (com.kolodkingal.enlightened.bending.Benders.get(p).element() == com.kolodkingal.enlightened.bending.Element.AVATAR) {
            return "raava.enlightened.already";
        }
        long now = level().getGameTime();
        if (p.getUUID().equals(asked) && now - askedAt <= ANSWER) {
            asked = null;
            return AvatarQuest.merge(p, this) ? "raava.enlightened.merge" : "raava.enlightened.taken";
        }
        asked = p.getUUID();
        askedAt = now;
        return "raava.enlightened.ask";
    }

    // ---- living ----

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            if (tickCount % 3 == 0) {
                level().addParticle(kind() == Kind.RAAVA ? ParticleTypes.END_ROD : ParticleTypes.SQUID_INK,
                        getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 3, getZ() + (random.nextDouble() - 0.5) * 2, 0, -0.02, 0);
            }
            return;
        }
        boss.setProgress(getHealth() / getMaxHealth());
        if (chosen == null) {
            // Raava in her glade: she drifts, and is not found where she may not choose.
            setDeltaMovement(0, Math.sin(tickCount * 0.05) * 0.01, 0);
            setPos(getX(), getY() + getDeltaMovement().y, getZ());
            if (tickCount % 100 == 0 && !AvatarQuest.raavaFree(level.getServer())) {
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 40, 0.8, 1.2, 0.8, 0.05);
                discard();
            }
            return;
        }
        ServerPlayer p = (ServerPlayer) level.getPlayerByUUID(chosen);
        if (p == null || !p.isAlive() || p.level() != level || p.distanceToSqr(this) > 80 * 80) {
            // The one it was set against is gone: it fades, to come again when he does.
            if (++lost > 200) {
                discard();
            }
            return;
        }
        lost = 0;
        boss.addPlayer(p);
        double side = kind() == Kind.RAAVA ? 0.0 : Math.PI;
        if (!hostile()) {
            float yaw = (float) (Math.atan2(p.getZ() - getZ(), p.getX() - getX()) * 180.0 / Math.PI) - 90.0F;
            setYRot(yaw);
            yBodyRot = yHeadRot = yaw;
            return;
        }
        // Round him, high, and strike.
        double a = side + tickCount * 0.03;
        Vec3 want = p.position().add(Math.cos(a) * 9, 4 + Math.sin(tickCount * 0.05) * 1.5, Math.sin(a) * 9);
        Vec3 v = getDeltaMovement().add(want.subtract(position()).scale(0.02)).scale(0.9);
        setDeltaMovement(v);
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        float yaw = (float) (Math.atan2(p.getZ() - getZ(), p.getX() - getX()) * 180.0 / Math.PI) - 90.0F;
        setYRot(yaw);
        yBodyRot = yHeadRot = yaw;
        if (tickCount % 40 == 0) {
            bolt(level, p, 8.0F);
        }
        if (distanceToSqr(p) < 36 && tickCount % 25 == 0) {
            p.hurt(damageSources().mobAttack(this), 10.0F);
            Vec3 away = p.position().subtract(position()).normalize();
            p.push(away.x * 1.5, 0.6, away.z * 1.5);
            p.hurtMarked = true;
        }
        if (kind() == Kind.VAATU && tickCount % 160 == 0) {
            for (int i = 0; i < 2; i++) {
                DarkSpiritEntity s = ModEntities.DARK_SPIRIT.get().create(level);
                if (s != null) {
                    s.moveTo(getX() + random.nextInt(5) - 2, p.getY(), getZ() + random.nextInt(5) - 2, 0, 0);
                    s.hunt(p);
                    level.addFreshEntity(s);
                }
            }
        }
    }

    /** A bolt of its energy at him: Raava's white, Vaatu's red. */
    void bolt(ServerLevel level, LivingEntity target, float damage) {
        Vec3 from = position().add(0, 1.5, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        if (kind() == Kind.VAATU) {
            Fx.send(level, Fx.ANOMALY_BOLT, from, to, 0xD01020, 1.0F, getId());
        } else {
            Fx.send(level, Fx.LIGHTNING, from, to, 0xEFFBFF, 1.0F, getId());
        }
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.8F, kind() == Kind.VAATU ? 0.6F : 1.4F);
        target.hurt(damageSources().indirectMagic(this, this), damage);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.getEntity() instanceof Player pl && pl.isCreative()) {
            return super.hurt(source, amount);
        }
        // Only the one it is set against can harm it, and only in the fight; Raava in her glade, nobody.
        if (!hostile() || source.getEntity() == null || !source.getEntity().getUUID().equals(chosen)
                && !(source.getEntity() instanceof net.minecraft.world.entity.OwnableEntity o && chosen != null && chosen.equals(o.getOwnerUUID()))) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && chosen != null && level.getPlayerByUUID(chosen) instanceof ServerPlayer p) {
            level.sendParticles(kind() == Kind.RAAVA ? ParticleTypes.END_ROD : ParticleTypes.SQUID_INK, getX(), getY() + 1.5, getZ(), 120, 1.5, 2.0, 1.5, 0.1);
            AvatarQuest.spiritDefeated(p, kind());
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        boss.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        boss.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", entityData.get(KIND));
        tag.putBoolean("Hostile", hostile());
        if (chosen != null) {
            tag.putUUID("Chosen", chosen);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("Kind"));
        entityData.set(HOSTILE, tag.getBoolean("Hostile"));
        chosen = tag.hasUUID("Chosen") ? tag.getUUID("Chosen") : null;
        boss.setVisible(hostile());
        boss.setName(Component.translatable("entity.enlightened.great_spirit." + kind().name().toLowerCase(java.util.Locale.ROOT)));
    }
}
