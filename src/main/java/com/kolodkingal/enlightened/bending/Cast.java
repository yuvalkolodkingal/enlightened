package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.particle.EnergyParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Everything an ability's code needs about the moment it runs: the bender, his data, the level he has of the ability,
 * and shortcuts for cooldowns, messages, sounds and particles in his colours. One is made per key press and per tick.
 */
public final class Cast {
    public final ServerPlayer player;
    public final BendingData data;
    public final Ability ability;
    /** The level the bender has of the ability (1 to 3). */
    public final int level;

    public Cast(ServerPlayer player, BendingData data, Ability ability) {
        this.player = player;
        this.data = data;
        this.ability = ability;
        this.level = data.level(ability);
    }

    public ServerLevel level() {
        return player.serverLevel();
    }

    public long now() {
        return player.level().getGameTime();
    }

    /** Picks one of three numbers by the ability's level (novice, adept, master). */
    public double by(double novice, double adept, double master) {
        return level >= 3 ? master : level == 2 ? adept : novice;
    }

    public int by(int novice, int adept, int master) {
        return level >= 3 ? master : level == 2 ? adept : novice;
    }

    public float byF(double novice, double adept, double master) {
        return (float) by(novice, adept, master);
    }

    /** Whether the ability's cooldown has run out (if not, the HUD already shows the seconds left). */
    public boolean ready() {
        return data.isReady(ability, now());
    }

    public void cooldown(int ticks) {
        data.startCooldown(ability, now(), Math.max(0, Math.round(ticks * Chakras.cooldownFactor(data, now()))));
    }

    public void cooldownSeconds(double seconds) {
        cooldown((int) Math.round(seconds * 20));
    }

    public boolean on() {
        return data.on(ability);
    }

    public void setOn(boolean on) {
        data.setOn(ability, on);
    }

    public boolean active() {
        return data.active(ability);
    }

    public void setActive(boolean on) {
        data.setActive(ability, on);
    }

    /** Whether the bender has food to bend with (Creative always has). */
    public boolean fed() {
        return player.isCreative() || player.getFoodData().getFoodLevel() > 0;
    }

    public void exhaust(float amount) {
        if (!player.isCreative()) {
            player.causeFoodExhaustion(amount);
        }
    }

    public int color() {
        return Benders.color(player);
    }

    public int glow() {
        return Benders.glow(player);
    }

    public void say(String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    public void refuse(String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public void soundAt(Vec3 at, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A burst of energy particles in the bender's colour. */
    public void sparks(Vec3 at, int count, double spread, float scale) {
        level().sendParticles(new EnergyParticleOptions(color(), scale), at.x, at.y, at.z, count, spread, spread, spread, 0.05);
    }
}
