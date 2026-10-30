package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.particle.EnergyParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Anomaly of <i>Seven Havens</i>: a tilted square of burning frames hung in the air, red lightning crawling out of
 * it. It sleeps in the red cubes of Ellora and wakes when someone tries to break one. It drifts round whoever fights
 * it, strikes them with red lightning, and now and then bursts outward. Blows and bending do it only half their harm;
 * <b>energybending</b> drains its life (a quarter of it at a time) and gives some to the bender, the surest way to beat
 * it. Beaten, it sends every bender who fought it to the cosmic plane, where the first chakra opens for them.
 */
public class AnomalyEntity extends Monster implements Enemy {
    /** How far from where it woke it drifts, and how far it looks for someone to fight. */
    public static final double LEASH = 18.0;
    public static final double SIGHT = 32.0;
    /** The share of its life one touch of energybending drains. */
    public static final float DRAIN = 0.25F;

    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.enlightened.anomaly"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private BlockPos home;
    private boolean draining;

    public AnomalyEntity(EntityType<? extends AnomalyEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 400.0).add(Attributes.ARMOR, 10.0)
                .add(Attributes.FOLLOW_RANGE, SIGHT).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    /** Wakes it over the cube that was struck. */
    public void wake(BlockPos cube) {
        home = cube;
        moveTo(cube.getX() + 0.5, cube.getY() + 4.0, cube.getZ() + 0.5, 0.0F, 0.0F);
    }

    public BlockPos home() {
        return home == null ? blockPosition() : home;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setDeltaMovement(getDeltaMovement().scale(0.85));
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 h = Vec3.atCenterOf(home());
        Player target = level.getNearestPlayer(getX(), getY(), getZ(), SIGHT,
                e -> e instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && pl.isAlive() && pl.distanceToSqr(h) < (LEASH + 14) * (LEASH + 14));
        // It drifts round whoever fights it (or round where it woke), a little above them, never far from home.
        double t = tickCount * 0.03;
        Vec3 centre = target != null ? target.position() : h;
        Vec3 want = centre.add(Math.cos(t) * 6.0, 3.5 + Math.sin(t * 1.7) * 0.8, Math.sin(t) * 6.0);
        Vec3 off = want.subtract(h);
        if (off.horizontalDistance() > LEASH) {
            off = off.scale(LEASH / off.horizontalDistance());
            want = h.add(off);
        }
        Vec3 pull = want.subtract(position());
        if (pull.lengthSqr() > 0.01) {
            setDeltaMovement(getDeltaMovement().add(pull.normalize().scale(Math.min(0.06, pull.length() * 0.02))));
        }
        if (target != null) {
            getLookControl().setLookAt(target);
        }
        boolean angry = getHealth() < getMaxHealth() * 0.5F;
        int every = angry ? 24 : 40;
        if (target != null && tickCount % every == 0 && hasLineOfSight(target)) {
            bolt(level, target, angry ? 8.0F : 6.0F);
        }
        if (tickCount % (angry ? 90 : 130) == 0) {
            burst(level, angry);
        }
        if (tickCount % 3 == 0) {
            level.sendParticles(new EnergyParticleOptions(0xFF2A1A, 0.9F), getX(), getY() + getBbHeight() / 2, getZ(), 2, 1.0, 1.0, 1.0, 0.02);
        }
        bar.setProgress(getHealth() / getMaxHealth());
    }

    /** Red lightning from it to someone. */
    private void bolt(ServerLevel level, Player target, float damage) {
        Vec3 from = position().add(0, getBbHeight() / 2, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Fx.send(level, Fx.ANOMALY_BOLT, from, to, 0xFF2A1A, 1.0F);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.8F, 1.6F);
        target.hurt(damageSources().indirectMagic(this, this), damage);
    }

    /** A burst of red energy all round it: whoever is near is hurt and thrown back. */
    private void burst(ServerLevel level, boolean angry) {
        double r = angry ? 8.0 : 6.0;
        Vec3 c = position().add(0, getBbHeight() / 2, 0);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.7F, 1.4F);
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2 / 40;
            level.sendParticles(new EnergyParticleOptions(0xFF3020, 1.2F), c.x + Math.cos(a) * r * 0.6, c.y, c.z + Math.sin(a) * r * 0.6, 1,
                    Math.cos(a) * 0.3, 0.0, Math.sin(a) * 0.3, 0.2);
        }
        List<Player> near = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r), p -> !p.isSpectator() && !p.isCreative());
        for (Player p : near) {
            p.hurt(damageSources().indirectMagic(this, this), angry ? 9.0F : 6.0F);
            Vec3 push = p.position().subtract(c).normalize().scale(1.2);
            p.push(push.x, 0.5, push.z);
            p.hurtMarked = true;
        }
    }

    /**
     * Energybending: the bender's energy draws the Anomaly's out, a quarter of its life at a time, and gives him some of
     * it. Returns whether it worked.
     */
    public boolean drain(ServerPlayer bender) {
        if (!isAlive()) {
            return false;
        }
        draining = true;
        try {
            hurt(damageSources().indirectMagic(bender, bender), getMaxHealth() * DRAIN);
        } finally {
            draining = false;
        }
        bender.heal(6.0F);
        ServerLevel level = (ServerLevel) level();
        Fx.send(level, Fx.ENERGYBEND, bender.position(), position(), Benders.color(bender), 0, getId());
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.2F, 0.6F);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        // Only energybending reaches its heart; every other blow does half.
        if (!draining && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 0.5F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        bar.removeAllPlayers();
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.4F);
        level.sendParticles(new EnergyParticleOptions(0xFFE45A, 2.0F), getX(), getY() + 1, getZ(), 80, 1.5, 1.5, 1.5, 0.3);
        // Every bender who fought it goes to the cosmic plane, where its light opens the first chakra.
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(SIGHT), ServerPlayer::isAlive)) {
            p.sendSystemMessage(Component.translatable("message.enlightened.anomaly.defeated").withStyle(ChatFormatting.GOLD));
            if (Benders.get(p).element().bends()) {
                com.kolodkingal.enlightened.world.Cosmic.sendForAnomaly(p);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BlockPos h = home();
        tag.putIntArray("Home", new int[]{h.getX(), h.getY(), h.getZ()});
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int[] h = tag.getIntArray("Home");
        if (h.length == 3) {
            home = new BlockPos(h[0], h[1], h[2]);
        }
        if (hasCustomName()) {
            bar.setName(getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** Whether an Anomaly is awake near a place. */
    public static boolean awakeNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(AnomalyEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(48), Mob::isAlive).isEmpty();
    }
}
