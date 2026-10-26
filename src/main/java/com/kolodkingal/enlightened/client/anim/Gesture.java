package com.kolodkingal.enlightened.client.anim;

/**
 * Every one-shot gesture of the benders: a short timeline of poses played once by an edge in the synced data (a cast,
 * a hold that begins or ends, a toggle, a landing), blended over the poses below it. The timing of each is here; its
 * keys are in the element tables ({@link EarthGestures}, {@link AirGestures}, {@link WaterGestures},
 * {@link FireGestures}); a gesture without keys does nothing.
 *
 * <p>A gesture's weight rises with an ease-out cubic over {@link #attack} ticks, holds, and falls with an ease-in-out
 * sine over the last {@link #release} ticks of {@link #dur}. At most one gesture of each {@link Group} plays at a time:
 * starting another in the same group fades the first out over 3 ticks. A {@link #force} gesture replaces the swing of
 * an attack instead of being muted by it.
 */
public enum Gesture {
    /** A new look: a hand passes over the face. */
    LOOK_CHANGE(Group.CAST, 12, 2, 5, false),

    // ===== earth =====
    /** The rock throw: a stomp lifts the stone, then a punch drives it. */
    EARTH_ROCK_THROW(Group.CAST, 14, 1, 5, true),
    /** The open hand rises and the rock with it. */
    EARTH_ROCK_GRAB(Group.HOLD, 10, 2, 5, false),
    /** The held rock is flung. */
    EARTH_ROCK_FLING(Group.HOLD, 10, 1, 5, true),
    /** A pillar under the feet: both arms drag up from the ground, the body rides up. */
    EARTH_RISE(Group.CAST, 14, 2, 6, true),
    /** A wall: a stomp, then both palms drive up. */
    EARTH_WALL(Group.CAST, 14, 2, 6, true),
    /** Seismic sense: a foot plants, the hands open low, the head tilts to listen. */
    EARTH_SEISMIC(Group.CAST, 14, 2, 6, false),
    /** A sand wall: both arms sweep low across. */
    EARTH_SAND_WAVE(Group.CAST, 12, 1, 5, true),
    /** The sand lift: one arm rises, the body with it. */
    EARTH_SAND_LIFT(Group.CAST, 14, 2, 6, true),
    /** Quicksand: both palms push down with a twist. */
    EARTH_QUICKSAND(Group.CAST, 12, 1, 5, true),
    /** The lava path: a sweep forward low, then a push. */
    EARTH_LAVA_PATH(Group.CAST, 16, 2, 6, true),
    /** A lava shuriken: a sidearm flick. */
    EARTH_LAVA_FLICK(Group.CAST, 10, 1, 4, true),
    /** Metal blades: both arms whip forward from the hips. */
    EARTH_METAL_BLADES(Group.CAST, 10, 1, 4, true),
    /** The metal pull: arms flung wide and clawed in to the chest. */
    EARTH_METAL_PULL(Group.CAST, 18, 2, 6, true),
    /** Wrecking: arms crossed and torn apart. */
    EARTH_WRECK(Group.CAST, 14, 1, 6, true),
    /** The armour climbs: arms folded in, a stomp, the chest out. */
    EARTH_ARMOR_ON(Group.CAST, 16, 2, 6, false),
    /** The armour shaken off. */
    EARTH_ARMOR_OFF(Group.CAST, 10, 1, 5, false),
    /** An earthbender lands heavily. */
    EARTH_LAND(Group.LAND, 14, 1, 6, false),

    // ===== air =====
    /** The air blast: both palms thrust with a step. */
    AIR_BLAST(Group.CAST, 10, 1, 4, true),
    /** The shield rises: arms sweep a circle out. */
    AIR_SHIELD_ON(Group.CAST, 14, 2, 5, false),
    AIR_SHIELD_OFF(Group.CAST, 8, 1, 4, false),
    /** The air jump: a crouch and the arms swing up. */
    AIR_JUMP(Group.CAST, 12, 1, 5, false),
    /** Air blades: a horizontal slice of the main arm. */
    AIR_BLADES(Group.CAST, 10, 1, 4, true),
    /** Spirit projection: he sits into meditation. */
    AIR_PROJECTION(Group.CAST, 24, 4, 8, false),
    /** The scooter: a spin and a hop onto it. */
    AIR_SCOOTER_ON(Group.CAST, 12, 1, 4, false),
    AIR_SCOOTER_OFF(Group.CAST, 8, 1, 4, false),
    /** The tornado: a full spin with the arms out, ending in a push. */
    AIR_TORNADO(Group.CAST, 22, 2, 6, true),
    /** Flight: the arms thrown up, then settle to the hover. */
    AIR_FLIGHT_ON(Group.TAKEOFF, 16, 2, 6, true),
    AIR_FLIGHT_OFF(Group.CAST, 10, 1, 5, false),
    /** The fist closes on the air in someone's lungs. */
    AIR_SUFFOCATE(Group.HOLD, 12, 2, 5, false),
    /** An airbender lands on a cushion of air. */
    AIR_LAND(Group.LAND, 12, 1, 5, false),

    // ===== water =====
    /** The whip: water is drawn from the side and lashed forward. */
    WATER_WHIP(Group.CAST, 12, 1, 5, true),
    /** Healing: the hands glide low, glowing. */
    WATER_HEAL(Group.CAST, 24, 3, 8, false),
    /** The octopus: arms sweep out and up. */
    WATER_OCTOPUS_ON(Group.CAST, 16, 2, 6, false),
    WATER_OCTOPUS_OFF(Group.CAST, 8, 1, 4, false),
    /** The spout: the arms lift from below and the body rises. */
    WATER_SPOUT_ON(Group.CAST, 14, 2, 5, false),
    WATER_SPOUT_OFF(Group.CAST, 8, 1, 4, false),
    /** Water arms: the forearms raised. */
    WATER_ARMS_ON(Group.CAST, 12, 2, 5, false),
    WATER_ARMS_OFF(Group.CAST, 8, 1, 4, false),
    /** Ice knives: both arms whip forward. */
    WATER_ICE_KNIVES(Group.CAST, 10, 1, 4, true),
    /** Freeze: both palms push down with a twist. */
    WATER_FREEZE(Group.CAST, 12, 1, 5, true),
    /** A snowball thrown overhead. */
    WATER_SNOW(Group.CAST, 10, 1, 4, true),
    /** An ice wall rises. */
    WATER_ICE_WALL(Group.CAST, 14, 2, 6, true),
    /** Bloodbending takes hold: clawed hands rise, the body hunches. */
    WATER_BLOOD_GRAB(Group.HOLD, 14, 2, 6, false),
    /** The grip let go: a fling down. */
    WATER_BLOOD_RELEASE(Group.HOLD, 10, 1, 5, false),
    /** The bending taken: a thumb to the forehead. */
    WATER_SUPPRESS(Group.CAST, 20, 2, 7, true),

    // ===== fire =====
    /** A fire ball: a punch. */
    FIRE_BALL(Group.CAST, 10, 1, 4, true),
    /** A big one: both fists together. */
    FIRE_BALL_BIG(Group.CAST, 14, 1, 5, true),
    /** The fire fist begins: a snap into the stance. */
    FIRE_FIST_START(Group.CAST, 6, 1, 3, true),
    /** The whip begins. */
    FIRE_WHIP_START(Group.CAST, 8, 1, 3, true),
    /** The fire ring: a spinning sweep of the leg. */
    FIRE_RING(Group.CAST, 18, 2, 6, true),
    /** Healing with fire: palms together, then over. */
    FIRE_HEAL(Group.CAST, 24, 3, 8, false),
    /** Lightning: the two fingers thrust. */
    FIRE_LIGHTNING(Group.CAST, 12, 1, 5, true),
    /** Lightning with both hands together. */
    FIRE_LIGHTNING_BOTH(Group.CAST, 14, 1, 5, true),
    /** Redirection readied: the arm out, two fingers. */
    FIRE_REDIRECT_ON(Group.CAST, 12, 2, 5, false),
    FIRE_REDIRECT_OFF(Group.CAST, 8, 1, 4, false),
    /** Lightning caught: in one hand, through the belly, out the other. */
    FIRE_REDIRECTED(Group.CAST, 16, 1, 6, true),

    // ===== the arts of 1.2 =====
    /** The shockwave: a leap and both fists driven into the ground. */
    EARTH_SHOCKWAVE(Group.CAST, 16, 1, 6, true),
    /** The catapult: a crouch, then sprung from the earth. */
    EARTH_CATAPULT(Group.CAST, 12, 1, 5, true),
    /** The earth grab: both hands claw up and close. */
    EARTH_GRAB(Group.CAST, 12, 1, 5, true),
    /** The tunnel begins: both palms set against the rock. */
    EARTH_TUNNEL_START(Group.CAST, 6, 1, 3, true),
    /** Extraction: a fist closes and tears the metal out. */
    EARTH_EXTRACT(Group.CAST, 12, 1, 5, true),
    /** Air suction: the palms draw in. */
    AIR_SUCTION(Group.CAST, 10, 1, 4, true),
    /** The air burst: the arms thrown wide. */
    AIR_BURST(Group.CAST, 12, 1, 5, true),
    AIR_SPOUT_ON(Group.CAST, 14, 2, 5, false),
    AIR_SPOUT_OFF(Group.CAST, 8, 1, 4, false),
    /** The water shield begins: the arms sweep up to hold the dome. */
    WATER_SHIELD_START(Group.CAST, 8, 1, 3, true),
    WATER_BUBBLE_ON(Group.CAST, 12, 2, 5, false),
    WATER_BUBBLE_OFF(Group.CAST, 8, 1, 4, false),
    /** Phase change: a slow press down, the hands spreading. */
    WATER_PHASE(Group.CAST, 14, 2, 6, true),
    /** Frost breathed: the hands cup at the chest, the head comes forward. */
    WATER_FROST_START(Group.CAST, 8, 1, 3, true),
    /** The plant tether: a sweep to the plant and a pull. */
    WATER_TETHER(Group.CAST, 14, 1, 6, true),
    /** The fire jet: a crouch, then thrown forward on the flames. */
    FIRE_JET(Group.CAST, 10, 1, 4, true),
    /** The fire shield begins: the arms sweep out, palms down. */
    FIRE_SHIELD_START(Group.CAST, 8, 1, 3, true),
    /** A wall of fire: both arms rise from low and press out. */
    FIRE_WALL(Group.CAST, 14, 2, 6, true),
    /** Blaze: the arm sweeps low across the ground. */
    FIRE_BLAZE(Group.CAST, 12, 1, 5, true),
    FIRE_HEAT_ON(Group.CAST, 12, 2, 5, false),
    FIRE_HEAT_OFF(Group.CAST, 8, 1, 4, false),
    FIRE_LIGHT_ON(Group.CAST, 10, 2, 5, false),
    FIRE_LIGHT_OFF(Group.CAST, 8, 1, 4, false),
    /** Combustion: a hand to the third eye, then the head thrust forward. */
    FIRE_COMBUSTION(Group.CAST, 14, 1, 5, true),
    /** The element step: a lean into the dash. */
    STEP(Group.CAST, 8, 1, 4, false),
    /** A combo: a flourish bigger than any one cast. */
    COMBO(Group.CAST, 18, 2, 6, true),
    /** A chakra opens: the hands rise from the lap to the heart. */
    CHAKRA_OPEN(Group.CAST, 30, 4, 10, false),
    /** The Avatar State: the body rises, the arms spread, the head thrown back. */
    AVATAR_ENTER(Group.TAKEOFF, 24, 2, 8, true),
    AVATAR_LEAVE(Group.CAST, 16, 2, 8, false);

    /** At most one gesture of a group plays at a time. */
    public enum Group {
        CAST, HOLD, TAKEOFF, LAND
    }

    public final Group group;
    /** Ticks: the whole length, the ease-in and the ease-out. */
    public final float dur;
    public final float attack;
    public final float release;
    /** Whether it replaces the vanilla swing of an attack rather than being muted by it. */
    public final boolean force;

    Gesture(Group group, float dur, float attack, float release, boolean force) {
        this.group = group;
        this.dur = dur;
        this.attack = attack;
        this.release = release;
        this.force = force;
    }

    public static final Gesture[] VALUES = values();
}
