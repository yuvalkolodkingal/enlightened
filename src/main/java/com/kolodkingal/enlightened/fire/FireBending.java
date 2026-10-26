package com.kolodkingal.enlightened.fire;

import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.Variants;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.water.WaterBending;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Firebending: a firebender's skin that fire hardly burns, the fire ball (a quick tap throws a small one, a held key
 * a big one), the fire fist (a stream from both hands; a master breathes fire), the fire whip, the fire ring, fire
 * healing; and lightning: the bolt (two-handed for a master) and its redirection, which a novice is as likely to be
 * burned by as to turn.
 */
public final class FireBending {
    /** When each firebender began to hold the fire ball's key. */
    private static final Map<UUID, Long> BALL_PRESSED = new HashMap<>();
    /** When each firebender's lightning will strike (he gathers it for a moment). */
    private static final Map<UUID, Long> CHARGING = new HashMap<>();

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.FIRE_SKIN, new AbilityLogic() {
            @Override
            public void key(Cast c, boolean down) {
            }

            @Override
            public void tick(Cast c) {
                if (c.level >= 3) {
                    c.player.clearFire();
                }
            }
        });
        logic.put(Ability.FIRE_BALL, new Ball());
        logic.put(Ability.FIRE_FIST, new Fist());
        logic.put(Ability.FIRE_WHIP, new Whip());
        logic.put(Ability.FIRE_RING, new Ring());
        logic.put(Ability.FIRE_HEALING, new Healing());
        logic.put(Ability.LIGHTNING, new Lightning());
        logic.put(Ability.LIGHTNING_REDIRECT, new Redirect());
        FireMore.register(logic);
    }

    /** The fire particle of this bender's look (Azula's fire is blue). */
    public static ParticleOptions flame(BendingData data) {
        return Variants.blueFire(data) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
    }

    /** How much of the fire damage a firebender's skin takes away (0.5, 0.75, 1). */
    public static float fireSkin(BendingData data) {
        if (!data.has(Ability.FIRE_SKIN)) {
            return 0F;
        }
        int level = data.level(Ability.FIRE_SKIN);
        return level >= 3 ? 1.0F : level == 2 ? 0.75F : 0.5F;
    }

    // ---- Fire Ball: tap for a small one, hold for the big one ----

    static final class Ball implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            ServerPlayer p = c.player;
            if (down) {
                if (!c.ready()) {
                    return;
                }
                BALL_PRESSED.put(p.getUUID(), c.now());
                c.setActive(true);
                return;
            }
            Long pressed = BALL_PRESSED.remove(p.getUUID());
            c.setActive(false);
            if (pressed == null || !c.ready()) {
                return;
            }
            boolean big = c.now() - pressed >= 8;
            float size = big ? c.byF(1.0, 1.5, 2.0) : 0.6F;
            float base = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_BALL_DAMAGE.get() : 4.0);
            float damage = big ? base * c.byF(1.0, 1.5, 2.0) : 3.0F;
            Shots.Aim aim = Shots.aim(p, 32);
            Vec3 from = Shots.hand(p);
            BendingShotEntity shot = new BendingShotEntity(c.level(), BendingShotEntity.Kind.FIRE_BALL, from, p, c.color());
            shot.setSize(size);
            shot.setDamage(damage);
            shot.setTouch(big ? c.by(4, 5, 6) : 2);
            shot.launch(aim.end().subtract(from), 1.4);
            c.level().addFreshEntity(shot);
            c.level().sendParticles(flame(c.data), from.x, from.y, from.z, 8, 0.2, 0.2, 0.2, 0.02);
            c.sound(ModSounds.FIRE_BLAST.get(), 1.0F, big ? 0.9F : 1.3F);
            c.exhaust(big ? 0.2F : 0.1F);
            c.cooldownSeconds(big ? 1.5 : 0.6);
        }

        @Override
        public void tick(Cast c) {
            Long pressed = BALL_PRESSED.get(c.player.getUUID());
            if (pressed != null && c.now() - pressed >= 8 && c.player.tickCount % 2 == 0) {
                Vec3 hand = Shots.hand(c.player);
                c.level().sendParticles(flame(c.data), hand.x, hand.y, hand.z, 2, 0.15, 0.15, 0.15, 0.0);
            }
        }

        @Override
        public void end(Cast c) {
            BALL_PRESSED.remove(c.player.getUUID());
            c.setActive(false);
        }

        @Override
        public void forget(ServerPlayer player) {
            BALL_PRESSED.remove(player.getUUID());
        }

        @Override
        public void clear() {
            BALL_PRESSED.clear();
        }
    }

    // ---- Fire Fist: a stream of flame from both hands; a master breathes it while he sneaks ----

    static final class Fist implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            c.setActive(down && c.fed() && !c.player.isSpectator());
            if (down) {
                c.sound(ModSounds.FIRE_STREAM.get(), 0.8F, 0.8F);
            }
        }

        @Override
        public void tick(Cast c) {
            if (!c.active()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!c.fed() || p.isSpectator()) {
                c.setActive(false);
                return;
            }
            boolean mouth = c.level >= 3 && p.isShiftKeyDown();
            double range = c.by(6, 8, 10) + (mouth ? 3 : 0);
            double dps = (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_STREAM_DPS.get() : 4.0) * c.by(1.0, 1.3, 1.6) * (mouth ? 1.5 : 1.0);
            if (p.tickCount % 4 == 0) {
                for (LivingEntity e : Shots.cone(p, range, mouth ? 25 : 20)) {
                    Shots.hurt(p, e, (float) (dps * 4 / 20), ModDamageTypes.FLAME);
                    e.igniteForSeconds(3);
                }
                c.exhaust(0.1F);
            }
            ServerLevel level = c.level();
            Vec3 look = p.getLookAngle();
            ParticleOptions flame = flame(c.data);
            if (mouth) {
                Vec3 from = p.getEyePosition().add(look.scale(0.4));
                for (int i = 0; i < 2; i++) {
                    Vec3 v = look.scale(0.8 + p.getRandom().nextDouble() * 0.4).add(p.getRandom().nextGaussian() * 0.1, p.getRandom().nextGaussian() * 0.1, p.getRandom().nextGaussian() * 0.1);
                    level.sendParticles(flame, from.x, from.y, from.z, 0, v.x, v.y, v.z, 0.7);
                }
            } else {
                for (int side = 0; side < 2; side++) {
                    Vec3 from = Shots.hand(p, side == 1);
                    for (int i = 0; i < 1; i++) {
                        Vec3 v = look.scale(0.7 + p.getRandom().nextDouble() * 0.3).add(p.getRandom().nextGaussian() * 0.08, p.getRandom().nextGaussian() * 0.08, p.getRandom().nextGaussian() * 0.08);
                        level.sendParticles(flame, from.x, from.y, from.z, 0, v.x, v.y, v.z, 0.6);
                    }
                }
            }
            if (p.tickCount % 8 == 0) {
                c.sound(SoundEvents.FIRE_AMBIENT, 0.8F, mouth ? 0.9F : 1.3F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
        }
    }

    // ---- Fire Whip ----

    static final class Whip implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            c.setActive(down && c.fed() && !c.player.isSpectator());
            if (down) {
                c.sound(ModSounds.FIRE_WHIP.get(), 1.0F, 1.0F);
            }
        }

        @Override
        public void tick(Cast c) {
            if (!c.active()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!c.fed() || p.isSpectator()) {
                c.setActive(false);
                return;
            }
            double length = c.by(6, 9, 12);
            Shots.Aim aim = Shots.aim(p, length);
            Vec3 from = Shots.hand(p);
            // The whip curls: a sine wave along the line, so it looks like a lash and not a beam.
            double len = from.distanceTo(aim.end());
            int count = Math.max(2, (int) (len * 0.6));
            Vec3 look = p.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            for (int i = 0; i <= count; i++) {
                double t = i / (double) count;
                double wave = Math.sin(t * Math.PI * 2 + p.tickCount * 0.6) * 0.5 * t;
                Vec3 at = from.lerp(aim.end(), t).add(right.scale(wave));
                c.level().sendParticles(flame(c.data), at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if (p.tickCount % 5 == 0) {
                if (aim.target() != null) {
                    float damage = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_WHIP_DAMAGE.get() : 3.0) + c.level * 0.5F;
                    Shots.hurt(p, aim.target(), damage, ModDamageTypes.FLAME);
                    aim.target().igniteForSeconds(4);
                    c.soundAt(aim.end(), ModSounds.FIRE_WHIP.get(), 0.7F, 1.4F);
                }
                c.exhaust(0.08F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
        }
    }

    // ---- Fire Ring ----

    static final class Ring implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            double radius = c.by(3, 4, 5);
            float damage = (float) ((EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_RING_DAMAGE.get() : 4.0) * c.by(1.0, 1.5, 2.0));
            for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                Shots.hurt(p, e, damage, ModDamageTypes.FIRE);
                e.igniteForSeconds(c.by(3, 4, 5));
                Shots.push(e, p.position(), 0.6, 0.3);
            }
            ParticleOptions flame = flame(c.data);
            Shots.ring(c.level(), p.position().add(0, 0.3, 0), radius, flame);
            Fx.at(c.level(), Fx.FIRE_RING, p.position(), Variants.blueFire(c.data) ? 1 : 0, (float) radius);
            c.sound(ModSounds.FIRE_RING.get(), 1.2F, 1.0F);
            c.exhaust(0.3F);
            c.cooldownSeconds(8);
        }
    }

    // ---- Fire Healing ----

    static final class Healing implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            LivingEntity target = WaterBending.healTargetFor(p);
            float base = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_HEAL.get() : 3.0);
            target.heal(base * c.byF(1.0, 1.4, 1.8));
            target.removeEffect(MobEffects.POISON);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true, true));
            c.level().sendParticles(flame(c.data), target.getX(), target.getY() + 1, target.getZ(), 15, 0.4, 0.5, 0.4, 0.01);
            c.level().sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
            Fx.around(c.level(), Fx.FIRE_HEAL, target, Variants.blueFire(c.data) ? 1 : 0, 1.0F);
            c.sound(ModSounds.HEAL.get(), 1.0F, 0.8F);
            c.exhaust(0.2F);
            c.cooldownSeconds(10);
        }
    }

    // ---- Lightning ----

    static int lightningCooldown(Cast c) {
        int base = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LIGHTNING_COOLDOWN.get() : 600;
        return (int) (base * c.by(1.0, 0.85, 0.67));
    }

    /** A bolt of light at {@code at} that burns nothing by itself. */
    public static void visualBolt(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    /** Lightning strikes a creature from {@code caster}: the damage, the stun, the sound and the light. */
    public static void strike(ServerPlayer caster, LivingEntity target, float damage) {
        ServerLevel level = caster.serverLevel();
        visualBolt(level, target.position());
        Fx.send(level, Fx.LIGHTNING, Shots.hand(caster), target.position().add(0, target.getBbHeight() * 0.5, 0), 0, 1.0F);
        target.hurt(ModDamageTypes.source(level, ModDamageTypes.LIGHTNING, caster, caster), damage);
        int stun = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LIGHTNING_STUN_TICKS.get() : 40;
        if (target.isAlive() && stun > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.STUNNED, stun, 0, false, true, true));
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.LIGHTNING_STRIKE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.0F);
    }

    static final class Lightning implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready() || CHARGING.containsKey(c.player.getUUID())) {
                return;
            }
            if (!c.fed()) {
                c.refuse("message.enlightened.hungry");
                return;
            }
            CHARGING.put(c.player.getUUID(), c.now() + 20);
            c.setActive(true);
            c.sound(ModSounds.LIGHTNING_CHARGE.get(), 1.0F, 1.0F);
        }

        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            Long at = CHARGING.get(p.getUUID());
            if (at == null) {
                if (c.active()) {
                    c.setActive(false);
                }
                return;
            }
            Vec3 hand = Shots.hand(p);
            c.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 3, 0.2, 0.2, 0.2, 0.05);
            if (c.level >= 3) {
                Vec3 left = Shots.hand(p, true);
                c.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, left.x, left.y, left.z, 3, 0.2, 0.2, 0.2, 0.05);
            }
            if (c.now() < at) {
                return;
            }
            CHARGING.remove(p.getUUID());
            c.setActive(false);
            Shots.Aim aim = Shots.aim(p, 24);
            float damage = (float) (c.level >= 3
                    ? (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LIGHTNING_TWO_HANDS_DAMAGE.get() : 12.0)
                    : (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LIGHTNING_DAMAGE.get() : 9.0));
            if (aim.target() != null && AreaTargets.mayHurt(p, aim.target())) {
                strike(p, aim.target(), damage);
            } else {
                visualBolt(c.level(), aim.end());
                Fx.send(c.level(), Fx.LIGHTNING, hand, aim.end(), 0, 1.0F);
                for (LivingEntity e : Shots.area(p, aim.end(), 2.0)) {
                    e.hurt(ModDamageTypes.source(c.level(), ModDamageTypes.LIGHTNING, p, p), damage * 0.6F);
                }
                c.soundAt(aim.end(), ModSounds.LIGHTNING_STRIKE.get(), 1.2F, 1.1F);
            }
            c.exhaust(0.6F);
            c.cooldown(lightningCooldown(c));
        }

        @Override
        public void end(Cast c) {
            CHARGING.remove(c.player.getUUID());
            c.setActive(false);
        }

        @Override
        public void forget(ServerPlayer player) {
            CHARGING.remove(player.getUUID());
        }

        @Override
        public void clear() {
            CHARGING.clear();
        }
    }

    // ---- Lightning Redirection ----

    static final class Redirect implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            c.sound(ModSounds.LIGHTNING_REDIRECT.get(), 0.8F, c.on() ? 1.0F : 0.7F);
            c.say(c.on() ? "message.enlightened.fire.redirect_on" : "message.enlightened.fire.redirect_off");
        }

        @Override
        public void tick(Cast c) {
            if (c.on() && c.player.tickCount % 10 == 0) {
                Vec3 hand = Shots.hand(c.player);
                c.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    /**
     * Lightning is about to hit a firebender in the redirection stance: a master turns all of it back (at what he looks
     * at, else at whoever cast it); an adept catches it half the time, a novice one time in three, and is burned by it
     * otherwise. Returns true if the bolt was caught (the damage is cancelled).
     */
    public static boolean redirect(ServerPlayer victim, DamageSource source, float amount) {
        BendingData data = Benders.get(victim);
        if (!data.has(Ability.LIGHTNING_REDIRECT) || !data.on(Ability.LIGHTNING_REDIRECT)) {
            return false;
        }
        int level = data.level(Ability.LIGHTNING_REDIRECT);
        double chance = level >= 3 ? 1.0 : level == 2 ? 0.5 : 0.34;
        ServerLevel world = victim.serverLevel();
        if (victim.getRandom().nextDouble() > chance) {
            victim.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.enlightened.fire.redirect_failed"), true);
            return false;
        }
        Fx.send(victim.serverLevel(), Fx.REDIRECT, Shots.hand(victim, true), Shots.hand(victim).add(victim.getLookAngle().scale(6.0)), 0, 1.0F, victim.getId());
        world.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.LIGHTNING_REDIRECT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 1, victim.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
        victim.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.enlightened.fire.redirected"), true);
        if (level >= 3) {
            Shots.Aim aim = Shots.aim(victim, 24);
            LivingEntity target = aim.target();
            if (target == null && source.getEntity() instanceof LivingEntity caster && caster != victim) {
                target = caster;
            }
            if (target != null && AreaTargets.mayHurt(victim, target)) {
                strike(victim, target, amount);
            } else {
                visualBolt(world, aim.end());
                Shots.line(world, Shots.hand(victim), aim.end(), ParticleTypes.ELECTRIC_SPARK, 3.0);
            }
        }
        return true;
    }

    private FireBending() {
    }
}
