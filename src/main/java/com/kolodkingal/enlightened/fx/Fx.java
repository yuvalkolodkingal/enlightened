package com.kolodkingal.enlightened.fx;

import com.kolodkingal.enlightened.network.FxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The one-shot effects of bending that are drawn as real element geometry on the clients (water made of water, fire
 * of fire, lightning of light): the server names the effect, where it starts and ends, its colour and one number
 * (a radius, a size), and every client in sight draws it for {@link #life} ticks. The sustained effects (a water
 * spout, the fire fist's streams, the air ball of the scooter) are not sent: the clients draw them from the synced
 * bending data.
 */
public enum Fx {
    /** A lash of water from a hand to a point. */
    WATER_WHIP(12),
    /** Water circling the healed creature (entity). */
    WATER_HEAL(36),
    /** Frost bursting out over the ground (radius). */
    FREEZE(24),
    /** An ice wall's frost rising along a line. */
    ICE_RISE(14),
    /** The crown of a splash where water hit (size). */
    WATER_SPLASH(10),
    /** A wave of flame rolling out from the bender (radius). */
    FIRE_RING(18),
    /** Fire circling the healed creature (entity). */
    FIRE_HEAL(36),
    /** A fire ball bursting (size). */
    FIRE_BURST(10),
    /** A bolt of lightning from a hand to its mark. */
    LIGHTNING(10),
    /** Lightning taken in at one hand and sent out of the other, through the body (entity: the redirector). */
    REDIRECT(12),
    /** A gust travelling from the hands along the look (range). */
    AIR_BLAST(9),
    /** The air shield bursting (radius). */
    AIR_BURST(12),
    /** A pulse of the seismic sense spreading over the ground (radius). */
    SEISMIC(20),
    /** Metal glints drawn in from all round (radius). */
    METAL_PULL(14),
    /** The ground swirling into quicksand under a creature (entity). */
    QUICKSAND(24),
    /** The bending taken: energy at the forehead (entity). */
    SUPPRESS(24),
    /** Lava bursting (size). */
    LAVA_BURST(10),
    /** Earth bursting up where a pillar or a wall rises (size). */
    EARTH_RISE(12),
    /** An element step: a streak of the element along the dash (colour: the element's). */
    STEP(10),
    /** A vine lashing from a plant to its mark. */
    VINE(14),
    /** An arc of flame sweeping over the ground ahead (data: the range; to: the way it faces). */
    BLAZE_ARC(16),
    /** A combustion beam from the forehead to its mark, and the blast (data: the blast's size). */
    COMBUSTION(14),
    /** A wave of earth rolling out (radius). */
    SHOCKWAVE(16),
    /** Earth closing round a creature (entity). */
    EARTH_GRAB(14),
    /** A wave of ice sweeping forward (data: range; to: the way). */
    ICE_WAVE(16),
    /** A whirl of flame spinning out from the bender (radius). */
    FIRE_SPIN(18),
    /** Metal drawn out of an ore (at). */
    EXTRACT(12),
    /** The Avatar State entered: light bursting out. */
    AVATAR_RISE(20),
    /** A chakra opening: a ring of light rising (data: the chakra's number). */
    CHAKRA(30);

    public final int life;

    Fx(int life) {
        this.life = life;
    }

    public static final Fx[] VALUES = values();

    /** Sends the effect to every player within 64 blocks of its start. */
    public static void send(ServerLevel level, Fx fx, Vec3 from, Vec3 to, int color, float data, int entity) {
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, 64.0,
                new FxPayload(fx.ordinal(), from.x, from.y, from.z, to.x, to.y, to.z, color, data, entity));
    }

    public static void send(ServerLevel level, Fx fx, Vec3 from, Vec3 to, int color, float data) {
        send(level, fx, from, to, color, data, -1);
    }

    public static void at(ServerLevel level, Fx fx, Vec3 at, int color, float data) {
        send(level, fx, at, at, color, data, -1);
    }

    public static void around(ServerLevel level, Fx fx, net.minecraft.world.entity.Entity entity, int color, float data) {
        send(level, fx, entity.position(), entity.position(), color, data, entity.getId());
    }
}
