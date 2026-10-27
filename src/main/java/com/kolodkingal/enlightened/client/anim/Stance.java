package com.kolodkingal.enlightened.client.anim;

/**
 * A sustained pose that lasts while a synced state holds (an ability is active or switched on, a glider is open):
 * it has an eased weight and its own pose rows in {@link Stances}. Ambient stances are applied first; aimed and
 * holding ones after, so they win where they overlap (and their arms count as occupied for the swing restore).
 */
public enum Stance {
    // ---- ambient ----
    /** Earth or metal armour: heavy, the arms a little out, the legs wide. */
    ARMORED(4, 5, false),
    /** Seismic sense: knees bent, palms low, the head tilted to listen. */
    SEISMIC(4, 6, false),
    /** The air shield: both arms out, circling. */
    AIR_SHIELD(2, 4, false),
    /** The air scooter: seated on the ball, one hand down. */
    SCOOTER(2, 3, false),
    /** The glider: hands up on the frame, legs trailing. */
    GLIDE(2, 4, false),
    /** Tetherless flight. */
    FLIGHT(3, 4, false),
    /** The octopus form: arms wide, tentacles swaying. */
    OCTOPUS(3, 4, false),
    /** The water spout: balancing, swaying. */
    SPOUT(3, 4, false),
    /** Water arms: forearms raised, hands open. */
    WATER_ARMS(3, 4, false),
    /** Lightning redirection readied: one arm out, two fingers. */
    REDIRECT(2, 4, false),

    // ---- aimed and holding ----
    /** A rock held in the air: the open palm under it. */
    ROCK_LIFT(2, 3, true),
    /** Bloodbending: both clawed hands at the victim, trembling. */
    BLOODBENDING(2, 4, true),
    /** Suffocation: the fist clenched at the victim. */
    SUFFOCATE(2, 4, true),
    /** The fire fist: alternating punches along the look. */
    FIRE_FIST(1.5F, 3, true),
    /** Fire breathed: the head forward, the arms bent back. */
    FIRE_BREATH(1.5F, 3, true),
    /** The fire whip: the main arm lashing. */
    FIRE_WHIP(1.5F, 3, true),
    /** A fire ball gathering: the fist cocked at the hip, the other palm forward. */
    FIRE_BALL_CHARGE(1.5F, 3, true),
    /** Lightning gathering: the arms circle, then point. */
    LIGHTNING_CHARGE(1.5F, 3, true),
    /** The earth tunnel: both palms driving into the rock ahead. */
    EARTH_TUNNEL(1.5F, 3, true),
    /** A dome of water held up over the head. */
    WATER_SHIELD(1.5F, 3, true),
    /** Frost breathed: the head forward, the hands cupped at the chest. */
    FROST_BREATH(1.5F, 3, true),
    /** The fire jet: streamlined, the arms back, the fire from the feet. */
    FIRE_JET(1.0F, 2, false),
    /** A dome of fire held up round the body. */
    FIRE_SHIELD(1.5F, 3, true),
    /** The air spout: balancing on the column of wind. */
    AIR_SPOUT(3, 4, false),
    /** Meditation: the lotus, the hands at the lap. */
    MEDITATE(4, 6, false),
    /** The Avatar State: afloat, the arms spread, the head back. */
    AVATAR(3, 6, false),
    /** Combustion gathering at the forehead: a hand to the third eye. */
    COMBUSTION(1.5F, 3, true),
    /** The palm forge: both hands cupped before the chest, the fire between them. */
    PALM_FORGE(2, 3, false);

    public final float tauIn;
    public final float tauOut;
    public final boolean aimed;

    Stance(float tauIn, float tauOut, boolean aimed) {
        this.tauIn = tauIn;
        this.tauOut = tauOut;
        this.aimed = aimed;
    }

    public static final Stance[] VALUES = values();
    public static final int COUNT = VALUES.length;
}
