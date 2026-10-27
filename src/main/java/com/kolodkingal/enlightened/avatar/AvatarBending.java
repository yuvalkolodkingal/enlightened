package com.kolodkingal.enlightened.avatar;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Chakras;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * The arts only the Avatar has, as the shows tell them: the Avatar State entered at will once the seven chakras are
 * open (Aang after the Guru, Korra), energybending, the lion turtle's gift (Aang on Ozai, Korra giving bending back),
 * spiritbending (Unalaq's art Korra turned to good: a spiral of golden light that calms a spirit), and the elemental
 * sphere of Sozin's Comet (rings of the four elements carrying the Avatar through the air).
 */
public final class AvatarBending {
    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.AVATAR_STATE, new State());
        logic.put(Ability.ENERGYBENDING, new Energy());
        logic.put(Ability.SPIRITBENDING, new Spirit());
        logic.put(Ability.ELEMENTAL_SPHERE, new Sphere());
    }

    /** The creature the Avatar aims at: on the ray, or the nearest in a narrow cone, within {@code range}. */
    @javax.annotation.Nullable
    private static LivingEntity aimed(ServerPlayer p, double range) {
        LivingEntity target = Shots.aim(p, range).target();
        if (target != null) {
            return target;
        }
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : Shots.cone(p, range, 30)) {
            double d = e.distanceToSqr(p);
            if (d < bestD) {
                best = e;
                bestD = d;
            }
        }
        return best;
    }

    /** The Avatar State at will: all seven chakras open, and rested. 12 / 18 / 24 seconds by level. */
    static final class State implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            BendingData data = c.data;
            long now = c.now();
            if (data.avatar(now)) {
                return;
            }
            if (data.chakras() < Chakras.COUNT) {
                c.refuse("message.enlightened.avatar.chakras");
                return;
            }
            if (now < data.avatarCooldownUntil()) {
                c.refuse("message.enlightened.avatar.rest", (data.avatarCooldownUntil() - now) / 20);
                return;
            }
            Chakras.enterAvatar(p, data, false, c.byF(1.0, 1.5, 2.0));
            c.exhaust(0.5F);
            c.cooldownSeconds(5);
        }
    }

    /**
     * Energybending: a touch to forehead and chest. A bender's bending is taken for {@code avatar.energybendingMinutes};
     * one whose bending was taken (by this, or by a bloodbender) gets it back. A will stronger than the Avatar's turns
     * the energy on him: he is stunned, and the touched one is unharmed.
     */
    static final class Energy implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!(aimed(p, 2.6) instanceof ServerPlayer other) || !Benders.get(other).element().bends()) {
                c.refuse("message.enlightened.avatar.energy_nobody");
                return;
            }
            ServerLevel level = c.level();
            BendingData theirs = Benders.get(other);
            long now = c.now();
            Fx.send(level, Fx.ENERGYBEND, p.position(), other.position(), c.color(), 0, other.getId());
            level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AWAKEN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.6F);
            if (theirs.suppressed(now)) {
                theirs.setSuppressedUntil(0);
                Benders.syncIfDirty(other);
                p.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_given", other.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
                other.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_back").withStyle(ChatFormatting.AQUA), true);
            } else if (Mastery.strength(other) > Mastery.strength(p) + 2 * (c.level - 1) && !c.data.avatar(now)) {
                // The will of the touched one is the stronger: the Avatar's own energy is bent.
                p.addEffect(new MobEffectInstance(ModEffects.STUNNED, 60, 0, false, true, true));
                p.hurt(p.damageSources().magic(), 4.0F);
                c.refuse("message.enlightened.avatar.energy_turned");
            } else {
                int minutes = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.ENERGYBENDING_MINUTES.get() : 30;
                theirs.setSuppressedUntil(now + minutes * 1200L);
                BendingTicker.endAll(other, theirs);
                BendingTicker.applyPassives(other, theirs);
                Benders.syncIfDirty(other);
                p.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_taken", other.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
                other.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_lost", minutes).withStyle(ChatFormatting.RED), true);
            }
            level.sendParticles(ParticleTypes.END_ROD, other.getX(), other.getY() + 1.0, other.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            c.exhaust(0.6F);
            c.cooldownSeconds(c.by(60, 45, 30));
        }
    }

    /** Spiritbending: a spiral of golden light round the creature aimed at (within 8), calming it for 30 / 45 / 60 s. */
    static final class Spirit implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            LivingEntity target = aimed(p, 8);
            if (target == null) {
                c.refuse("message.enlightened.avatar.energy_nobody");
                return;
            }
            ServerLevel level = c.level();
            target.addEffect(new MobEffectInstance(ModEffects.PACIFIED, c.by(600, 900, 1200), 0, false, true, true));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
            target.heal(4.0F);
            target.clearFire();
            Fx.send(level, Fx.SPIRITBEND, target.position(), target.position().add(0, target.getBbHeight(), 0), c.color(), target.getBbWidth(), target.getId());
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
            c.exhaust(0.2F);
            c.cooldownSeconds(c.by(8, 6, 5));
        }
    }

    /** The elemental sphere: only in the Avatar State; rings of the four round him, and flight at a sprint. */
    static final class Sphere implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.on()) {
                c.setOn(false);
                BendingTicker.applyPassives(p, c.data);
                return;
            }
            if (!c.data.avatar(c.now())) {
                c.refuse("message.enlightened.avatar.sphere_needs");
                return;
            }
            c.setOn(true);
            BendingTicker.applyPassives(p, c.data);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            Vec3 at = p.position();
            for (LivingEntity e : Shots.area(p, at, 4.0)) {
                if (AreaTargets.mayHurt(p, e)) {
                    Shots.push(e, at, 1.2, 0.5);
                }
            }
            c.sound(ModSounds.AWAKEN.get(), 1.2F, 0.8F);
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!c.data.avatar(c.now())) {
                c.setOn(false);
                BendingTicker.applyPassives(p, c.data);
                return;
            }
            p.fallDistance = 0;
            // The rings strike whoever is in the way as he moves.
            if (p.tickCount % 10 == 0 && p.getDeltaMovement().lengthSqr() > 0.02) {
                for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), 2.6)) {
                    Shots.hurt(p, e, 3.0F + c.level, com.kolodkingal.enlightened.registry.ModDamageTypes.FLAME);
                    Shots.push(e, p.position(), 1.0, 0.4);
                }
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    private AvatarBending() {
    }
}
