package com.kolodkingal.enlightened.air;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.entity.MeditatingBodyEntity;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spirit Projection: the airbender sits down in meditation and his spirit leaves the body (he becomes a spectator for
 * a while, free to look through walls and into caves). The body stays behind as a {@link MeditatingBodyEntity}; if
 * anything hurts it, the spirit is pulled back at once and the blow lands. The spirit may go only so far from the
 * body, and only for so long (both grow with the level); then it returns by itself.
 */
public final class Projection {
    private record State(ResourceKey<Level> dimension, UUID body, GameType before, long until, double range, Vec3 at) {
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static boolean isProjecting(ServerPlayer player) {
        return STATES.containsKey(player.getUUID());
    }

    /** Whether this body is the one its owner's spirit left (a body whose owner is back, or gone, is nothing). */
    public static boolean isProjecting(MinecraftServer server, UUID owner, MeditatingBodyEntity body) {
        State state = STATES.get(owner);
        return state != null && state.body.equals(body.getUUID()) && server.getPlayerList().getPlayer(owner) != null;
    }

    static void start(Cast c) {
        ServerPlayer p = c.player;
        if (isProjecting(p) || p.isSpectator() || !p.onGround() || p.isPassenger()) {
            c.refuse("message.enlightened.air.projection_cannot");
            return;
        }
        ServerLevel level = c.level();
        MeditatingBodyEntity body = new MeditatingBodyEntity(level, p);
        level.addFreshEntity(body);
        int seconds = (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.PROJECTION_SECONDS.get() : 20) * c.by(1, 2, 3);
        double range = (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.PROJECTION_RANGE.get() : 32) * c.by(1, 2, 4);
        STATES.put(p.getUUID(), new State(level.dimension(), body.getUUID(), p.gameMode.getGameModeForPlayer(),
                c.now() + seconds * 20L, range, p.position()));
        p.setGameMode(GameType.SPECTATOR);
        c.setActive(true);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.PROJECTION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        c.say("message.enlightened.air.projection_start", seconds);
    }

    /** The spirit comes back to the body: the body vanishes, the player stands where it sat. */
    public static void end(ServerPlayer p, String reasonKey) {
        State state = STATES.remove(p.getUUID());
        BendingData data = Benders.get(p);
        data.setActive(Ability.SPIRIT_PROJECTION, false);
        if (state == null) {
            return;
        }
        ServerLevel level = p.server.getLevel(state.dimension);
        Vec3 at = state.at;
        if (level != null) {
            Entity body = level.getEntity(state.body);
            if (body != null) {
                at = body.position();
                body.discard();
            }
            if (p.level() != level) {
                p.teleportTo(level, at.x, at.y, at.z, p.getYRot(), p.getXRot());
            } else {
                p.teleportTo(at.x, at.y, at.z);
            }
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 20, 0.4, 0.6, 0.4, 0.05);
            level.playSound(null, at.x, at.y, at.z, ModSounds.PROJECTION.get(), SoundSource.PLAYERS, 0.8F, 0.7F);
        }
        p.setGameMode(state.before == GameType.SPECTATOR ? GameType.SURVIVAL : state.before);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        data.startCooldown(Ability.SPIRIT_PROJECTION, p.level().getGameTime(), 400);
        if (reasonKey != null) {
            p.displayClientMessage(Component.translatable(reasonKey), true);
        }
        Benders.syncIfDirty(p);
    }

    /** The body was hurt: the spirit is pulled back and the blow lands on the player. */
    public static void onBodyHurt(MinecraftServer server, UUID owner, DamageSource source, float amount) {
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p == null || !isProjecting(p)) {
            return;
        }
        end(p, "message.enlightened.air.projection_attacked");
        p.hurt(source, amount);
    }

    /** A player who logs out mid-projection: his body goes, and he comes back in his body when he returns. */
    public static void onLogout(ServerPlayer p) {
        if (isProjecting(p)) {
            end(p, null);
        }
    }

    static void tick(Cast c) {
        ServerPlayer p = c.player;
        State state = STATES.get(p.getUUID());
        if (state == null) {
            if (c.active()) {
                c.setActive(false);
            }
            return;
        }
        if (c.now() >= state.until) {
            end(p, "message.enlightened.air.projection_over");
            return;
        }
        ServerLevel level = p.server.getLevel(state.dimension);
        Entity body = level == null ? null : level.getEntity(state.body);
        if (body == null || !body.isAlive() || p.level() != level) {
            end(p, "message.enlightened.air.projection_over");
            return;
        }
        if (p.position().distanceTo(body.position()) > state.range) {
            // Too far from the body: the tether pulls the spirit back toward it.
            Vec3 back = body.position().subtract(p.position()).normalize().scale(1.5);
            p.teleportTo(p.getX() + back.x, p.getY() + back.y, p.getZ() + back.z);
            p.displayClientMessage(Component.translatable("message.enlightened.air.projection_far"), true);
        }
        if (p.tickCount % 20 == 0) {
            int left = (int) ((state.until - c.now()) / 20);
            p.displayClientMessage(Component.translatable("message.enlightened.air.projection_left", left), true);
        }
    }

    public static void clear() {
        STATES.clear();
    }

    /** The server stopped: every spirit is put back before the players are saved. */
    public static void returnAll(MinecraftServer server) {
        for (UUID owner : STATES.keySet().toArray(new UUID[0])) {
            ServerPlayer p = server.getPlayerList().getPlayer(owner);
            if (p != null) {
                end(p, null);
            }
        }
        STATES.clear();
    }

    /** The ability's logic: a press starts the projection; a press while out of the body ends it. */
    public static final class Logic implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            if (isProjecting(c.player)) {
                Projection.end(c.player, "message.enlightened.air.projection_back");
                return;
            }
            if (!c.ready()) {
                return;
            }
            start(c);
        }

        @Override
        public void tick(Cast c) {
            Projection.tick(c);
        }

        @Override
        public void end(Cast c) {
            if (isProjecting(c.player)) {
                Projection.end(c.player, null);
            }
        }

        @Override
        public void forget(ServerPlayer player) {
            STATES.remove(player.getUUID());
        }

        @Override
        public void clear() {
            STATES.clear();
        }
    }

    @Nullable
    public static Vec3 bodyPosition(ServerPlayer p) {
        State state = STATES.get(p.getUUID());
        return state == null ? null : state.at;
    }

    private Projection() {
    }
}
