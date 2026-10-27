package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * The seven chakras and the Avatar State, after the Bedrock add-on and the guru's lessons: a bender who meditates
 * (the Meditate key, still, on the ground; faster on a Meditation Point) opens his chakras one by one, each asking
 * for points spent on his element and a stretch of meditation. Each open chakra strengthens him (a heart, quicker
 * cooldowns, stronger bending); with all seven open he may enter the <b>Avatar State</b>: a few seconds in which
 * nothing can hurt him and his bending is twice as strong, then a long rest. Meditation also earns a little
 * experience.
 */
public final class Chakras {
    public static final int COUNT = 7;
    private static final ResourceLocation HEALTH_ID = Enlightened.id("chakra_health");
    private static final ResourceLocation AVATAR_SPEED_ID = Enlightened.id("avatar_speed");
    /** Ticks the Meditate key must be held before the Avatar State is entered. */
    private static final int AVATAR_HOLD = 20;

    private static int seconds(net.neoforged.neoforge.common.ModConfigSpec.IntValue v, int fallback) {
        return EnlightenedConfig.SPEC.isLoaded() ? v.get() : fallback;
    }

    /** Ticks of meditation one chakra asks for (a third of them on a Meditation Point). */
    public static int ticksPerChakra() {
        return seconds(EnlightenedConfig.CHAKRA_MEDITATION_SECONDS, 60) * 20;
    }

    /** The points spent on the element the n-th chakra (1..7) asks for. */
    public static int pointsFor(int chakra) {
        int each = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.CHAKRA_POINTS_EACH.get() : 2;
        return chakra * each;
    }

    public static Component name(int chakra) {
        return Component.translatable("chakra.enlightened." + chakra);
    }

    // ---- what the open chakras give ----

    /** The factor on bending damage: +5 percent for the third and sixth chakra, doubled in the Avatar State. */
    public static float damageFactor(BendingData data, long now) {
        float f = 1.0F + (data.chakras() >= 3 ? 0.05F : 0.0F) + (data.chakras() >= 6 ? 0.05F : 0.0F);
        return data.avatar(now) ? f * 2.0F : f;
    }

    /** The factor on cooldowns: -5 percent for the second and fifth chakra, halved in the Avatar State. */
    public static float cooldownFactor(BendingData data, long now) {
        float f = 1.0F - (data.chakras() >= 2 ? 0.05F : 0.0F) - (data.chakras() >= 5 ? 0.05F : 0.0F);
        return data.avatar(now) ? f * 0.5F : f;
    }

    /** The extra hearts (as health) of the first and fourth chakra, and the speed of the Avatar State. */
    public static void applyBody(ServerPlayer player, BendingData data) {
        double health = (data.chakras() >= 1 ? 2.0 : 0.0) + (data.chakras() >= 4 ? 2.0 : 0.0);
        BendingTicker.setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, health, health > 0);
        boolean avatar = data.avatar(player.level().getGameTime());
        BendingTicker.setModifier(player, Attributes.MOVEMENT_SPEED, AVATAR_SPEED_ID, 0.3, avatar,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // ---- meditation ----

    /** The Meditate key went down or came up. */
    public static void meditate(ServerPlayer p, boolean down) {
        BendingData data = Benders.get(p);
        if (!data.element().bends()) {
            return;
        }
        if (down) {
            if (data.avatar(p.level().getGameTime())) {
                return;
            }
            if (!p.onGround() && !p.isInWater()) {
                p.displayClientMessage(Component.translatable("message.enlightened.meditate.ground").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            data.setMeditating(true);
            data.setMeditateSince(p.level().getGameTime());
            p.displayClientMessage(Component.translatable("message.enlightened.meditate.begin").withStyle(ChatFormatting.GRAY), true);
        } else {
            data.setMeditating(false);
        }
        Benders.syncIfDirty(p);
    }

    /** Every tick of a bender: the meditation's progress, the chakras it opens, the Avatar State's course. */
    public static void tick(ServerPlayer p, BendingData data) {
        long now = p.level().getGameTime();
        if (data.avatar(now)) {
            avatarTick(p, data, now);
        } else if (data.avatarUntil() > 0) {
            data.setAvatarUntil(0);
            BendingTicker.applyPassives(p, data);
            p.displayClientMessage(Component.translatable("message.enlightened.avatar.leave").withStyle(ChatFormatting.GRAY), true);
        }
        if (!data.meditating()) {
            return;
        }
        // Standing up, moving, fighting or falling ends it.
        boolean moving = p.getDeltaMovement().horizontalDistanceSqr() > 0.003 || (!p.onGround() && !p.isInWater()) || p.swinging;
        if (moving || !p.isAlive()) {
            data.setMeditating(false);
            return;
        }
        boolean point = onMeditationPoint(p);
        long held = now - data.meditateSince();
        // All seven open: holding on enters the Avatar State.
        if (data.chakras() >= COUNT) {
            if (held >= AVATAR_HOLD && now >= data.avatarCooldownUntil()) {
                data.setMeditating(false);
                enterAvatar(p, data, false);
            } else if (held >= AVATAR_HOLD && p.tickCount % 20 == 0) {
                p.displayClientMessage(Component.translatable("message.enlightened.avatar.rest", (data.avatarCooldownUntil() - now) / 20).withStyle(ChatFormatting.GRAY), true);
            }
        }
        int step = point ? 3 : 1;
        data.setMeditation(data.meditation() + step);
        // Experience for the patient.
        int xpSeconds = seconds(EnlightenedConfig.MEDITATION_XP_SECONDS, 15);
        if (xpSeconds > 0 && data.meditation() % (xpSeconds * 20) < step) {
            Mastery.earn(p, 1);
        }
        if (p.tickCount % 4 == 0) {
            ServerLevel level = p.serverLevel();
            double a = p.tickCount * 0.1;
            level.sendParticles(new com.kolodkingal.enlightened.particle.EnergyParticleOptions(Benders.glow(p), 0.7F),
                    p.getX() + Math.cos(a) * 0.8, p.getY() + 0.6 + 0.3 * Math.sin(a * 2), p.getZ() + Math.sin(a) * 0.8, 1, 0, 0.02, 0, 0.0);
        }
        int next = data.chakras() + 1;
        if (next <= COUNT && data.meditation() >= ticksPerChakra()) {
            if (data.spent() < pointsFor(next)) {
                if (p.tickCount % 60 == 0) {
                    p.displayClientMessage(Component.translatable("message.enlightened.chakra.points", name(next), pointsFor(next)).withStyle(ChatFormatting.GRAY), true);
                }
                return;
            }
            data.setMeditation(0);
            data.setChakras(next);
            BendingTicker.applyPassives(p, data);
            ServerLevel level = p.serverLevel();
            level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 0.8F + next * 0.1F);
            Fx.send(level, Fx.CHAKRA, p.position(), p.position().add(0, 2.0, 0), Benders.color(p), next);
            p.displayClientMessage(Component.translatable("message.enlightened.chakra.open", name(next), next, COUNT).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            if (next == COUNT) {
                p.displayClientMessage(Component.translatable("message.enlightened.chakra.all").withStyle(ChatFormatting.GOLD), false);
            }
        }
    }

    public static boolean onMeditationPoint(ServerPlayer p) {
        return p.serverLevel().getBlockState(p.blockPosition()).is(ModTags.MEDITATION_POINT)
                || p.serverLevel().getBlockState(p.blockPosition().below()).is(ModTags.MEDITATION_POINT);
    }

    // ---- the Avatar State ----

    /** Enters the Avatar State: by meditation, or by itself when near death ({@code dire}). */
    public static void enterAvatar(ServerPlayer p, BendingData data, boolean dire) {
        enterAvatar(p, data, dire, 1.0F);
    }

    /** Enters the Avatar State for the configured time times {@code scale} (the Avatar's own art lasts longer). */
    public static void enterAvatar(ServerPlayer p, BendingData data, boolean dire, float scale) {
        long now = p.level().getGameTime();
        int seconds = Math.round(seconds(EnlightenedConfig.AVATAR_STATE_SECONDS, 12) * scale);
        data.setAvatarUntil(now + seconds * 20L);
        data.setAvatarCooldownUntil(now + seconds(EnlightenedConfig.AVATAR_STATE_COOLDOWN_SECONDS, 300) * 20L);
        data.clearCooldowns();
        p.setHealth(Math.max(p.getHealth(), dire ? 6.0F : p.getHealth()));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, seconds * 20, 1, false, false, true));
        BendingTicker.applyPassives(p, data);
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.5F, 0.5F);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AIR_SHIELD_BREAK.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        Fx.at(level, Fx.AVATAR_RISE, p.position(), Benders.color(p), 4.0F);
        for (net.minecraft.world.entity.LivingEntity e : Shots.area(p, p.position(), 4.0)) {
            Shots.push(e, p.position(), 1.5, 0.6);
        }
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.enlightened.avatar.enter", p.getDisplayName())
                .withStyle(ChatFormatting.AQUA), false);
        Benders.syncIfDirty(p);
    }

    private static void avatarTick(ServerPlayer p, BendingData data, long now) {
        p.fallDistance = 0;
        if (p.tickCount % 2 == 0) {
            ServerLevel level = p.serverLevel();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.4, 0.5, 0.4, 0.01);
        }
        if (p.tickCount % 20 == 0) {
            p.setAirSupply(p.getMaxAirSupply());
            p.clearFire();
        }
    }

    /** Near death with all seven chakras open: the Avatar State takes him, if it is rested. Returns whether it did. */
    public static boolean dire(ServerPlayer p, BendingData data) {
        boolean allowed = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.AVATAR_STATE_ON_LOW_HEALTH.get();
        long now = p.level().getGameTime();
        if (!allowed || data.chakras() < COUNT || data.avatar(now) || now < data.avatarCooldownUntil()) {
            return false;
        }
        enterAvatar(p, data, true);
        return true;
    }

    private Chakras() {
    }
}
