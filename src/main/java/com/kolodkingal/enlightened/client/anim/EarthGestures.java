package com.kolodkingal.enlightened.client.anim;

import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.B;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.E;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.I;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.O;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.S;
import static com.kolodkingal.enlightened.client.anim.Gestures.Target.HELD;
import static com.kolodkingal.enlightened.client.anim.Gestures.Target.LOOK;
import static com.kolodkingal.enlightened.client.anim.Gestures.define;

/**
 * The keys of the earthbenders' gestures. Earthbending is rooted and heavy (Hung Gar): low stances, stomps that
 * sink the body, fists and open palms that drive straight through, nothing that floats.
 */
final class EarthGestures {
    static void register() {
        // The rock throw: the right foot stamps and the body sinks as the stone comes up, the fist cocks at the hip,
        // then drives along the look with the shoulders turning into it.
        define(Gesture.EARTH_ROCK_THROW)
                .key(0, S).m(0, 0, 6).o(0, 0, -6).legs(0, 0).drop(0).lean(0).twist(0)
                .key(2, O).m(36, -20, 12).o(-70, 10, -30).legs(-52, 10).drop(0.14F).lean(6).twist(-18).head(-4, 0, 0)
                .key(5, I).m(36, -20, 12).o(-70, 10, -30).legs(-10, 4).drop(0.22F).lean(8).twist(-18)
                .key(7, O).mAim(LOOK, 0, 0, 0).o(10, 0, -24).legs(-18, 8).drop(0.10F).lean(14).twist(24).head(2, 0, 0)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).legs(0, 0).drop(0).lean(0).twist(0).head(0, 0, 0);

        // The open hand rises from the hip, palm up, and the rock rises with it; ends on the holding stance's aim.
        define(Gesture.EARTH_ROCK_GRAB)
                .key(0, S).m(10, -10, 14).o(0, 0, -6).lean(0).legs(0, 0)
                .key(3, I).m(30, -14, 10).o(-20, 0, -20).lean(6).legs(-14, -14).drop(0.08F)
                .key(10, O).mAim(HELD, -8, 0, 0).o(-26, 0, -26).lean(-2).legs(0, 0).drop(0);

        // The held rock is flung: the palm closes to a fist and punches through at it.
        define(Gesture.EARTH_ROCK_FLING)
                .key(0, S).mAim(HELD, -8, 0, 0).o(-26, 0, -26).twist(0).lean(0)
                .key(1, O).mAim(HELD, 22, -14, 0).o(-20, 0, -30).twist(-12).lean(-4)
                .key(4, O).mAim(HELD, 0, 0, 0).o(16, 0, -20).twist(20).lean(12).head(2, 0, 0)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);

        // A pillar: a deep squat with both palms at the ground, then both arms haul straight up as the body rides the rise.
        define(Gesture.EARTH_RISE)
                .key(0, S).m(0, 0, 8).o(0, 0, -8).drop(0).legs(0, 0).head(0, 0, 0)
                .key(3, O).m(40, -24, 14).o(40, 24, -14).drop(0.36F).legs(-40, -40).head(8, 0, 0)
                .key(8, B).m(-178, -10, 6).o(-178, 10, -6).drop(-0.06F).legs(0, 0).head(-14, 0, 0)
                .key(14, E).m(-20, 0, 24).o(-20, 0, -24).drop(0).head(0, 0, 0);

        // A wall: the foot stamps, both palms drive up from the hips to the chest and press forward.
        define(Gesture.EARTH_WALL)
                .key(0, S).m(0, 0, 8).o(0, 0, -8).legs(0, 0).drop(0).lean(0)
                .key(2, O).m(30, -20, 12).o(30, 20, -12).legs(-50, 8).drop(0.18F).lean(8)
                .key(4, I).m(30, -20, 12).o(30, 20, -12).legs(-8, 4).drop(0.24F).lean(8)
                .key(8, B).m(-120, -22, 4).o(-120, 22, -4).legs(-14, 8).drop(0.02F).lean(-6).head(-8, 0, 0)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).legs(0, 0).drop(0).lean(0).head(0, 0, 0);

        // Seismic sense: one foot lifts and plants, the hands open low at the sides, the head tilts as if listening.
        define(Gesture.EARTH_SEISMIC)
                .key(0, S).m(0, 0, 8).o(0, 0, -8).legs(0, 0).drop(0).head(0, 0, 0)
                .key(3, O).m(-14, 0, 24).o(-14, 0, -24).legs(-46, 6).drop(0.08F).head(4, 0, 0)
                .key(6, I).m(-14, 0, 24).o(-14, 0, -24).legs(-4, 2).drop(0.20F).head(6, 0, 6)
                .key(14, E).m(StancePoses.SEISMIC_M).o(StancePoses.SEISMIC_O).legs(0, 0).drop(0.06F).head(2, 0, 4);

        // A sand wall: both arms sweep low across the front, left to right, the sand rising in their wake.
        define(Gesture.EARTH_SAND_WAVE)
                .key(0, S).m(-30, 40, 10).o(-30, 40, -40).twist(16).lean(4)
                .key(5, E).m(-50, -50, 30).o(-50, -60, -10).twist(-18).lean(8)
                .key(12, S).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0);

        // The sand lift: the main arm rises palm up and the body lifts with it, the off arm balancing.
        define(Gesture.EARTH_SAND_LIFT)
                .key(0, S).m(10, 0, 10).o(0, 0, -6).drop(0).legs(0, 0)
                .key(3, I).m(30, -10, 12).o(-30, 0, -30).drop(0.16F).legs(-20, -20)
                .key(8, B).m(-172, -8, 10).o(-40, 0, -50).drop(-0.08F).legs(-6, -6).head(-10, 0, 0)
                .key(14, E).m(-30, 0, 20).o(-30, 0, -40).drop(0).legs(0, 0).head(0, 0, 0);

        // Quicksand: both palms rise to the chest, then push down and twist, as the ground softens.
        define(Gesture.EARTH_QUICKSAND)
                .key(0, S).m(-70, -24, 8).o(-70, 24, -8).lean(0).drop(0).legs(0, 0)
                .key(5, O).m(34, -28, 10).o(34, 28, -10).lean(16).drop(0.22F).legs(-30, -30).head(10, 0, 0)
                .key(8, S).m(30, -34, 14).o(30, 20, -4).lean(14).drop(0.20F).twist(-14)
                .key(12, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).drop(0).legs(0, 0).twist(0).head(0, 0, 0);

        // The lava path: a low sweep forward of both arms, then a push as the rock melts ahead.
        define(Gesture.EARTH_LAVA_PATH)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).lean(0).drop(0).legs(0, 0)
                .key(4, O).m(40, -30, 20).o(40, 30, -20).lean(18).drop(0.20F).legs(-34, 12).head(8, 0, 0)
                .key(9, E).m(-96, -18, 6).o(-96, 18, -6).lean(10).drop(0.06F).legs(-20, 8).head(-4, 0, 0)
                .key(16, S).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).drop(0).legs(0, 0).head(0, 0, 0);

        // A lava shuriken: the main arm cocks across the body and flicks out sidearm along the look.
        define(Gesture.EARTH_LAVA_FLICK)
                .key(0, S).m(-70, 50, 0).o(-10, 0, -10).twist(14)
                .key(2, I).m(-80, 60, -10).o(-10, 0, -14).twist(18)
                .key(4, O).mAim(LOOK, -6, -10, 0).o(10, 0, -20).twist(-16).lean(6)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0);

        // Metal blades: both hands at the hips, then both whip forward with a snap of the shoulders.
        define(Gesture.EARTH_METAL_BLADES)
                .key(0, S).m(40, -10, 14).o(40, 10, -14).lean(-4)
                .key(2, I).m(50, -14, 16).o(50, 14, -16).lean(-6)
                .key(4, O).mAim(LOOK, -4, -8, 0).oAim(LOOK, -4, 8, 0).lean(10).head(2, 0, 0)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0);

        // The metal pull: arms flung wide and up, then both fists drawn in to the chest as the metal comes.
        define(Gesture.EARTH_METAL_PULL)
                .key(0, S).m(-8, 0, 10).o(-8, 0, -10).lean(0).drop(0)
                .key(5, O).m(-30, 0, 120).o(-30, 0, -120).lean(-2).head(-8, 0, 0)
                .key(11, E).m(-76, -40, 0).o(-76, 40, 0).lean(-8).drop(0.08F).head(4, 0, 0)
                .key(14, S).m(-76, -40, 0).o(-76, 40, 0).lean(-8).drop(0.08F)
                .key(18, S).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).drop(0).head(0, 0, 0);

        // Wrecking: the arms cross at the wrists in front, grip, and tear apart with the whole back.
        define(Gesture.EARTH_WRECK)
                .key(0, S).m(-90, -40, 0).o(-90, 40, 0).lean(4).drop(0)
                .key(3, I).m(-96, -50, 0).o(-96, 50, 0).lean(8).drop(0.06F).head(6, 0, 0)
                .key(6, O).m(-70, 20, 60).o(-70, -20, -60).lean(-10).drop(0).head(-8, 0, 0).legs(-10, -10)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0).legs(0, 0);

        // The armour climbs: the foot stamps, the arms fold in, then the chest opens as the stone sets.
        define(Gesture.EARTH_ARMOR_ON)
                .key(0, S).m(0, 0, 8).o(0, 0, -8).legs(0, 0).drop(0).head(0, 0, 0)
                .key(3, O).m(-70, -40, 0).o(-70, 40, 0).legs(-44, 6).drop(0.10F).head(10, 0, 0)
                .key(6, I).m(-76, -44, 0).o(-76, 44, 0).legs(-4, 2).drop(0.22F).head(12, 0, 0)
                .key(11, B).m(-20, 0, 40).o(-20, 0, -40).legs(0, 0).drop(0).head(-6, 0, 0)
                .key(16, S).m(StancePoses.ARMORED_M).o(StancePoses.ARMORED_O).head(0, 0, 0);

        // The armour shaken off: a shrug and a shake of each arm.
        define(Gesture.EARTH_ARMOR_OFF)
                .key(0, S).m(StancePoses.ARMORED_M).o(StancePoses.ARMORED_O).drop(0)
                .key(3, O).m(-30, 0, 40).o(-30, 0, -40).drop(-0.03F).head(-4, 0, 0)
                .key(6, S).m(10, 0, 18).o(10, 0, -18).drop(0.02F)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0).head(0, 0, 0);
    }

    private EarthGestures() {
    }
}
