package com.kolodkingal.enlightened.client.anim;

/**
 * The arm poses of the ambient stances, as {@code {x, y, z}} in degrees for a right-handed player ({@code M} the main
 * arm with the right arm's signs, {@code O} the off arm with the left's): a gesture that hands over to a stance ends on
 * these, so nothing pops.
 */
public final class StancePoses {
    public static final float[] ARMORED_M = {-4, 0, 16};
    public static final float[] ARMORED_O = {-4, 0, -16};
    public static final float[] SEISMIC_M = {-28, -8, 14};
    public static final float[] SEISMIC_O = {-28, 8, -14};
    public static final float[] AIR_SHIELD_M = {-58, 0, 62};
    public static final float[] AIR_SHIELD_O = {-58, 0, -62};
    public static final float[] SCOOTER_M = {-24, -12, 42};
    public static final float[] SCOOTER_O = {-46, 8, -64};
    public static final float[] GLIDE_M = {-225, 0, 0};
    public static final float[] GLIDE_O = {-225, 0, 0};
    public static final float[] FLIGHT_HOVER_M = {-18, 0, 58};
    public static final float[] FLIGHT_HOVER_O = {-18, 0, -58};
    public static final float[] FLIGHT_CRUISE_M = {38, 0, 24};
    public static final float[] FLIGHT_CRUISE_O = {38, 0, -24};
    public static final float[] FLIGHT_SPRINT_M = {52, 0, 10};
    public static final float[] FLIGHT_SPRINT_O = {52, 0, -10};
    public static final float[] OCTOPUS_M = {-22, 0, 80};
    public static final float[] OCTOPUS_O = {-22, 0, -80};
    public static final float[] SPOUT_M = {12, 0, 42};
    public static final float[] SPOUT_O = {12, 0, -42};
    public static final float[] WATER_ARMS_M = {-48, -6, 6};
    public static final float[] WATER_ARMS_O = {-48, 6, -6};
    public static final float[] REDIRECT_M = {-82, -28, 10};
    public static final float[] REDIRECT_O = {32, 12, 22};
    public static final float[] FIRE_BALL_CHARGE_M = {28, -34, 16};
    public static final float[] FIRE_BALL_CHARGE_O = {-34, 14, -10};
    public static final float[] FIRE_BREATH_M = {-62, -26, 12};
    public static final float[] FIRE_BREATH_O = {-62, 26, -12};
    public static final float[] REST_M = {0, 0, 4};
    public static final float[] REST_O = {0, 0, -4};

    private StancePoses() {
    }
}
