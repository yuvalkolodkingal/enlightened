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
        logic.put(Ability.ENERGY_RESTORE, new Restore());
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

    /**
     * The Avatar State at will, for the master Avatar (Guru Pathik's lessons, all seven chakras open), rested: 12 / 18 /
     * ... it lasts two minutes (avatar.avatarStateLengthSeconds). Pressed again inside it, he leaves it.
     */
    static final class State implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            BendingData data = c.data;
            long now = c.now();
            if (data.avatar(now)) {
                if (Chakras.masters(data)) {
                    Chakras.leaveAtWill(p, data);
                } else {
                    c.refuse("message.enlightened.avatar.no_control");
                }
                return;
            }
            if (!c.ready()) {
                return;
            }
            if (!Chakras.taught(data)) {
                c.refuse("message.enlightened.avatar.guru");
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
            Chakras.enterAtWill(p, data, 1.0F);
            c.exhaust(0.5F);
            c.cooldownSeconds(5);
        }
    }

    /**
     * Energybending, the lion turtle's gift (and the Avatar's art): a touch to forehead and chest takes a bender's
     * bending, for {@code avatar.energybendingAirSeconds} (six minutes) when an airbender bends it,
     * {@code avatar.energybendingOtherSeconds} (three) for anyone else, bender or not, and
     * {@code avatar.energybendingAvatarMinutes} (two hours) for the Avatar, who must have all seven chakras open. A will
     * stronger than his own ({@link Mastery#energyStrength}) turns the energy on him: his own bending is taken for as
     * long, and the touched one is unharmed. In the Avatar State no will is stronger.
     */
    static final class Energy implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            BendingData mine = c.data;
            long now = c.now();
            if (mine.element() == com.kolodkingal.enlightened.bending.Element.AVATAR && mine.chakras() < Chakras.COUNT) {
                c.refuse("message.enlightened.energy.chakras");
                return;
            }
            if (!(aimed(p, 2.6) instanceof ServerPlayer other) || !Benders.get(other).element().bends()) {
                c.refuse("message.enlightened.avatar.energy_nobody");
                return;
            }
            BendingData theirs = Benders.get(other);
            if (theirs.suppressed(now)) {
                c.refuse("message.enlightened.energy.already", other.getDisplayName());
                return;
            }
            ServerLevel level = c.level();
            Fx.send(level, Fx.ENERGYBEND, p.position(), other.position(), c.color(), 0, other.getId());
            level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AWAKEN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.6F);
            long ticks = duration(mine);
            if (Mastery.energyStrength(other) > Mastery.energyStrength(p) && !mine.avatar(now)) {
                // The will of the touched one is the stronger: the energy turns, and it is his own bending that goes.
                mine.setSuppressedUntil(now + ticks);
                BendingTicker.endAll(p, mine);
                BendingTicker.applyPassives(p, mine);
                p.addEffect(new MobEffectInstance(ModEffects.STUNNED, 60, 0, false, true, true));
                Benders.syncIfDirty(p);
                p.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_turned", minutes(ticks)).withStyle(ChatFormatting.RED), false);
                return;
            }
            theirs.setSuppressedUntil(now + ticks);
            BendingTicker.endAll(other, theirs);
            BendingTicker.applyPassives(other, theirs);
            Benders.syncIfDirty(other);
            p.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_taken", other.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
            other.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_lost", p.getDisplayName(), minutes(ticks)).withStyle(ChatFormatting.RED), true);
            level.sendParticles(ParticleTypes.END_ROD, other.getX(), other.getY() + 1.0, other.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            c.exhaust(0.6F);
            c.cooldownSeconds(30);
        }

        /** How long the energybending of this bender takes a bending for, in ticks. */
        static long duration(BendingData mine) {
            boolean loaded = EnlightenedConfig.SPEC.isLoaded();
            return switch (mine.element()) {
                case AVATAR -> (loaded ? EnlightenedConfig.ENERGYBENDING_AVATAR_MINUTES.get() : 120) * 1200L;
                case AIR -> (loaded ? EnlightenedConfig.ENERGYBENDING_AIR_SECONDS.get() : 360) * 20L;
                default -> (loaded ? EnlightenedConfig.ENERGYBENDING_OTHER_SECONDS.get() : 180) * 20L;
            };
        }

        private static String minutes(long ticks) {
            long seconds = ticks / 20;
            return seconds % 60 == 0 ? String.valueOf(seconds / 60) : String.format(java.util.Locale.ROOT, "%.1f", seconds / 60.0);
        }
    }

    /** Energybending given back: a touch, and one whose bending was taken (by energybending or a bloodbender) has it again. */
    static final class Restore implements AbilityLogic {
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
            BendingData theirs = Benders.get(other);
            if (!theirs.suppressed(c.now())) {
                c.refuse("message.enlightened.energy.not_taken", other.getDisplayName());
                return;
            }
            theirs.setSuppressedUntil(0);
            Benders.syncIfDirty(other);
            ServerLevel level = c.level();
            Fx.send(level, Fx.ENERGYBEND, p.position(), other.position(), c.color(), 0, other.getId());
            level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AWAKEN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.2F);
            p.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_given", other.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
            other.displayClientMessage(Component.translatable("message.enlightened.avatar.energy_back", p.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
            c.exhaust(0.3F);
            c.cooldownSeconds(10);
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
                // Raised into the State by the sphere alone (the Avatar State not on his keys): he leaves it with it.
                if (AUTO.remove(p.getUUID()) && c.data.avatar(c.now()) && Chakras.masters(c.data)) {
                    Chakras.leaveAtWill(p, c.data);
                }
                BendingTicker.applyPassives(p, c.data);
                return;
            }
            if (!c.data.avatar(c.now())) {
                // Without the Avatar State on his keys, the sphere takes him into it (if he masters it and is rested).
                if (c.data.slotOf(Ability.AVATAR_STATE) >= 0 || !enter(c)) {
                    if (c.data.slotOf(Ability.AVATAR_STATE) >= 0) {
                        c.refuse("message.enlightened.avatar.sphere_needs");
                    }
                    return;
                }
                AUTO.add(p.getUUID());
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
                AUTO.remove(p.getUUID());
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
            AUTO.remove(c.player.getUUID());
        }

        /** Those the sphere raised into the Avatar State itself, who leave it when the sphere goes. */
        private static final java.util.Set<java.util.UUID> AUTO = new java.util.HashSet<>();

        /** Enters the Avatar State for the sphere, as the Avatar State art would (refusing as it does); true if he is in it. */
        private static boolean enter(Cast c) {
            BendingData data = c.data;
            long now = c.now();
            if (!Chakras.taught(data)) {
                c.refuse("message.enlightened.avatar.guru");
                return false;
            }
            if (data.chakras() < Chakras.COUNT) {
                c.refuse("message.enlightened.avatar.chakras");
                return false;
            }
            if (now < data.avatarCooldownUntil()) {
                c.refuse("message.enlightened.avatar.rest", (data.avatarCooldownUntil() - now) / 20);
                return false;
            }
            Chakras.enterAtWill(c.player, data, 1.0F);
            return data.avatar(now);
        }
    }

    private AvatarBending() {
    }
}
