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
 * The seven chakras and the Avatar State, after the guru's lessons in the show. Guru Pathik, who meditates at the
 * Southern Air Temple, teaches a bender to open his chakras ({@code avatar.guruRequired}: none open before). Then
 * meditation (the Meditate key, still, on the ground; faster on a Meditation Point, six times as fast before the guru
 * himself, who asks no points) opens them one by one, each asking for points spent on his element and a stretch of
 * meditation. Each open chakra strengthens him a little (a heart, quicker cooldowns, stronger bending).
 *
 * <p>The <b>Avatar State</b> is the Avatar's alone: a few seconds in which nothing can hurt him and his bending is twice
 * as strong. Untaught, it takes him only when he is near death, and leaves him to a long rest. Taught by the guru with
 * all seven chakras open, he enters it at will (his art) and leaves it at will, and rests three
 * times as long as he spent in it. Meditation also earns a little experience.
 */
public final class Chakras {
    public static final int COUNT = 7;
    private static final ResourceLocation HEALTH_ID = Enlightened.id("chakra_health");
    private static final ResourceLocation AVATAR_SPEED_ID = Enlightened.id("avatar_speed");
    private static final ResourceLocation AVATAR_STEADY_ID = Enlightened.id("avatar_steady");
    private static final ResourceLocation AVATAR_ARMOR_ID = Enlightened.id("avatar_armor");

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

    /** The factor on bending damage: +5 percent for the third and sixth chakra, two and a half times in the Avatar State. */
    public static float damageFactor(BendingData data, long now) {
        float f = 1.0F + (data.chakras() >= 3 ? 0.05F : 0.0F) + (data.chakras() >= 6 ? 0.05F : 0.0F);
        return data.avatar(now) ? f * 2.5F : f;
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
        // The Avatar State: not invincible, but hard to move and hard to hurt (most of each blow is taken away in
        // CommonEvents), his armour stronger.
        BendingTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, AVATAR_STEADY_ID, 0.7, avatar);
        BendingTicker.setModifier(player, Attributes.ARMOR, AVATAR_ARMOR_ID, 8.0, avatar);
    }

    // ---- meditation ----

    /** The Meditate key went down or came up. */
    public static void meditate(ServerPlayer p, boolean down) {
        BendingData data = Benders.get(p);
        if (!data.element().bends()) {
            return;
        }
        // A toggle: a press sits him down, the next stands him up (so does moving); letting go of the key changes nothing.
        if (!down) {
            return;
        }
        if (data.meditating()) {
            data.setMeditating(false);
            p.displayClientMessage(Component.translatable("message.enlightened.meditate.end").withStyle(ChatFormatting.GRAY), true);
            Benders.syncIfDirty(p);
            return;
        }
        startMeditation(p, data, false);
    }

    /** Sits him down to meditate; on a mat ({@code onMat}) he is on the ground whatever the last packet said. */
    public static boolean startMeditation(ServerPlayer p, BendingData data, boolean onMat) {
        if (!data.element().bends() || data.avatar(p.level().getGameTime())) {
            return false;
        }
        if (!onMat && !p.onGround() && !p.isInWater()) {
            p.displayClientMessage(Component.translatable("message.enlightened.meditate.ground").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        data.setMeditating(true);
        data.setMeditateSince(p.level().getGameTime());
        SEAT.put(p.getUUID(), p.position());
        p.displayClientMessage(Component.translatable(taught(data) ? "message.enlightened.meditate.begin" : "message.enlightened.chakra.closed")
                .withStyle(ChatFormatting.GRAY), true);
        Benders.syncIfDirty(p);
        return true;
    }

    /** Every tick of a bender: the meditation's progress, the chakras it opens, the Avatar State's course. */
    public static void tick(ServerPlayer p, BendingData data) {
        if (com.kolodkingal.enlightened.world.Cosmic.in(p)) {
            com.kolodkingal.enlightened.world.Cosmic.tick(p, data);
        }
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
        // Walking off, fighting or falling ends it (a moment's grace as he settles, or is set on a mat).
        Vec3 seat = SEAT.get(p.getUUID());
        boolean moving = seat != null && seat.distanceToSqr(p.position()) > 0.25 || p.getDeltaMovement().horizontalDistanceSqr() > 0.003
                || (!p.onGround() && !p.isInWater()) || p.swinging;
        if (now - data.meditateSince() > 10 && moving || !p.isAlive()) {
            data.setMeditating(false);
            return;
        }
        boolean point = onMeditationPoint(p);
        boolean guru = Guru.near(p);
        if (!taught(data)) {
            // No guru has shown him the way: the meditation calms him, but his chakras stay closed.
            if (p.tickCount % 80 == 0) {
                p.displayClientMessage(Component.translatable("message.enlightened.chakra.closed").withStyle(ChatFormatting.GRAY), true);
            }
            int xpSeconds = seconds(EnlightenedConfig.MEDITATION_XP_SECONDS, 15);
            data.setMeditation(Math.min(data.meditation() + 1, ticksPerChakra() - 1));
            if (xpSeconds > 0 && p.tickCount % (xpSeconds * 20) == 0) {
                Mastery.earn(p, 1);
            }
            return;
        }
        int step = guru ? 6 : point ? 3 : 1;
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
            if (!guru && data.spent() < pointsFor(next)) {
                if (p.tickCount % 60 == 0) {
                    p.displayClientMessage(Component.translatable("message.enlightened.chakra.points", name(next), pointsFor(next)).withStyle(ChatFormatting.GRAY), true);
                }
                return;
            }
            data.setMeditation(0);
            if (next == COUNT) {
                // The Thought chakra: his spirit goes out to the cosmic plane, and opens it at the end of the path there.
                data.setMeditating(false);
                if (guru) {
                    Guru.lesson(p, next);
                }
                if (com.kolodkingal.enlightened.world.Cosmic.send(p)) {
                    return;
                }
            }
            open(p, data, next, guru);
        }
    }

    /** The n-th chakra opens: its bonuses, its sound and light, the word of it (and the guru's lesson, before him). */
    public static void open(ServerPlayer p, BendingData data, int next, boolean guru) {
        data.setChakras(next);
        BendingTicker.applyPassives(p, data);
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 0.8F + next * 0.1F);
        Fx.send(level, Fx.CHAKRA, p.position(), p.position().add(0, 2.0, 0), Benders.color(p), next);
        p.displayClientMessage(Component.translatable("message.enlightened.chakra.open", name(next), next, COUNT).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        if (guru && next < COUNT) {
            Guru.lesson(p, next);
        }
        if (next == COUNT) {
            p.displayClientMessage(Component.translatable(data.element() == Element.AVATAR
                    ? "message.enlightened.chakra.all_avatar" : "message.enlightened.chakra.all").withStyle(ChatFormatting.GOLD), false);
        }
        Benders.syncIfDirty(p);
    }

    /** Whether his chakras may open: Guru Pathik taught him (or the server asks for no guru). */
    public static boolean taught(BendingData data) {
        boolean required = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.GURU_REQUIRED.get();
        return data.guruTaught() || !required;
    }

    /** The master Avatar: taught, all seven chakras open. He enters and leaves the Avatar State at will. */
    public static boolean masters(BendingData data) {
        return data.element() == Element.AVATAR && data.chakras() >= COUNT && taught(data);
    }

    /**
     * The master Avatar enters the Avatar State by his own will (his art), for the configured time times {@code scale}; he
     * will rest three times as long as he stays in it.
     */
    public static void enterAtWill(ServerPlayer p, BendingData data, float scale) {
        enterAvatar(p, data, false, scale);
        long now = p.level().getGameTime();
        ENTERED.put(p.getUUID(), now);
        data.setAvatarCooldownUntil(data.avatarUntil() + (data.avatarUntil() - now) * 2);
    }

    /** He leaves the Avatar State by his own will, and rests three times as long as he spent in it (at least 10 s). */
    public static void leaveAtWill(ServerPlayer p, BendingData data) {
        long now = p.level().getGameTime();
        Long since = ENTERED.remove(p.getUUID());
        long spent = since == null ? 0 : now - since;
        data.setAvatarUntil(now);
        data.setAvatarCooldownUntil(now + Math.max(200L, spent * 3));
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 1.4F);
        Benders.syncIfDirty(p);
    }

    /** Where each meditating bender sat down: walking away from it ends the meditation. */
    private static final java.util.Map<java.util.UUID, Vec3> SEAT = new java.util.HashMap<>();

    /** When each master Avatar entered the State at will (for his rest when he leaves it). */
    private static final java.util.Map<java.util.UUID, Long> ENTERED = new java.util.HashMap<>();

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
        // The Avatar State is the Avatar's alone: no other bender ever enters it, by any road.
        if (data.element() != Element.AVATAR) {
            return;
        }
        long now = p.level().getGameTime();
        int seconds = Math.round(seconds(EnlightenedConfig.AVATAR_STATE_SECONDS, 120) * scale);
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

    /**
     * The Avatar near death: the Avatar State takes him by itself, if it is rested, taught or not (the way it took
     * Aang before the guru). Only the Avatar has it. Returns whether it did.
     */
    public static boolean dire(ServerPlayer p, BendingData data) {
        boolean allowed = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.AVATAR_STATE_ON_LOW_HEALTH.get();
        long now = p.level().getGameTime();
        if (!allowed || data.element() != Element.AVATAR || data.avatar(now) || now < data.avatarCooldownUntil()) {
            return false;
        }
        enterAvatar(p, data, true);
        return true;
    }

    private Chakras() {
    }
}
