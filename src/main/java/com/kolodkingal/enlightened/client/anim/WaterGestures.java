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
 * The keys of the waterbenders' gestures. Waterbending flows (Tai Chi): every move is a curve that gathers and
 * releases, the weight shifting from foot to foot, the hands drawing circles.
 */
final class WaterGestures {
    static void register() {
        // The whip: the main hand draws the water from the side in an arc up and over, then lashes along the look.
        define(Gesture.WATER_WHIP)
                .key(0, S).m(-40, 30, 30).o(-30, 10, -30).twist(12).lean(-2)
                .key(4, E).m(-170, 10, 30).o(-50, 20, -30).twist(16).lean(-8).head(-8, 0, 0)
                .key(7, O).mAim(LOOK, -2, -10, 0).o(20, 10, -24).twist(-16).lean(10).head(2, 0, 0)
                .key(12, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);

        // Healing: the hands come together at the chest, open palms down, and glide in slow circles over the hurt.
        define(Gesture.WATER_HEAL)
                .key(0, S).m(-60, -30, 6).o(-60, 30, -6).head(0, 0, 0)
                .key(6, E).m(-80, -10, 16).o(-80, 10, -16).head(10, 0, 0)
                .key(12, E).m(-70, -30, 10).o(-90, 10, -20).head(12, 0, 4)
                .key(18, E).m(-90, -10, 20).o(-70, 30, -10).head(12, 0, -4)
                .key(24, S).m(StancePoses.REST_M).o(StancePoses.REST_O).head(0, 0, 0);

        // The octopus: the arms rise from the sides in a wide circle as the tentacles come up, and settle swaying.
        define(Gesture.WATER_OCTOPUS_ON)
                .key(0, S).m(0, 0, 8).o(0, 0, -8).drop(0).head(0, 0, 0)
                .key(6, E).m(-100, 0, 100).o(-100, 0, -100).drop(0.08F).head(-6, 0, 0)
                .key(11, E).m(-10, 0, 60).o(-10, 0, -60).drop(0.02F)
                .key(16, S).m(StancePoses.OCTOPUS_M).o(StancePoses.OCTOPUS_O).drop(0).head(0, 0, 0);

        define(Gesture.WATER_OCTOPUS_OFF)
                .key(0, S).m(StancePoses.OCTOPUS_M).o(StancePoses.OCTOPUS_O)
                .key(4, O).m(-40, -20, 20).o(-40, 20, -20)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O);

        // The spout: the arms lift from below the hips to the shoulders as the water rises under him.
        define(Gesture.WATER_SPOUT_ON)
                .key(0, S).m(20, -10, 10).o(20, 10, -10).drop(0).legs(0, 0)
                .key(5, I).m(40, -20, 14).o(40, 20, -14).drop(0.12F).legs(-20, -20)
                .key(10, B).m(-90, 0, 60).o(-90, 0, -60).drop(-0.10F).legs(-6, -6).head(-10, 0, 0)
                .key(14, S).m(StancePoses.SPOUT_M).o(StancePoses.SPOUT_O).drop(0).legs(0, 0).head(0, 0, 0);

        define(Gesture.WATER_SPOUT_OFF)
                .key(0, S).m(StancePoses.SPOUT_M).o(StancePoses.SPOUT_O).drop(0)
                .key(4, O).m(-20, 0, 30).o(-20, 0, -30).drop(0.08F)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0);

        // Water arms: the forearms come up as the water wraps them, a shake of each.
        define(Gesture.WATER_ARMS_ON)
                .key(0, S).m(10, 0, 10).o(10, 0, -10)
                .key(4, E).m(-100, -20, 20).o(-100, 20, -20).head(-4, 0, 0)
                .key(8, O).m(-60, 0, 0).o(-60, 0, 0)
                .key(12, S).m(StancePoses.WATER_ARMS_M).o(StancePoses.WATER_ARMS_O).head(0, 0, 0);

        define(Gesture.WATER_ARMS_OFF)
                .key(0, S).m(StancePoses.WATER_ARMS_M).o(StancePoses.WATER_ARMS_O)
                .key(3, O).m(20, 0, 20).o(20, 0, -20)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O);

        // Ice knives: both hands gather at one hip, then whip out together along the look.
        define(Gesture.WATER_ICE_KNIVES)
                .key(0, S).m(-20, 40, 0).o(30, 40, 10).twist(20).lean(-2)
                .key(2, I).m(-30, 50, -6).o(40, 50, 14).twist(26).lean(-4)
                .key(4, O).mAim(LOOK, -4, -8, 0).oAim(LOOK, -4, 8, 0).twist(-16).lean(10).head(2, 0, 0)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);

        // Freeze: both palms rise and press down, the fingers spreading, as the water sets.
        define(Gesture.WATER_FREEZE)
                .key(0, S).m(-80, -20, 10).o(-80, 20, -10).lean(0).drop(0)
                .key(4, O).m(30, -20, 20).o(30, 20, -20).lean(14).drop(0.16F).head(8, 0, 0).legs(-24, -24)
                .key(7, S).m(26, -24, 24).o(26, 24, -24).lean(12).drop(0.14F)
                .key(12, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).drop(0).head(0, 0, 0).legs(0, 0);

        // A snowball: packed between the hands, then thrown overhead.
        define(Gesture.WATER_SNOW)
                .key(0, S).m(-70, -30, 0).o(-70, 30, 0).lean(-4)
                .key(3, I).m(-170, -10, 10).o(-60, 20, -10).lean(-10).head(-6, 0, 0)
                .key(5, O).mAim(LOOK, -6, 0, 0).o(10, 0, -20).lean(10).head(2, 0, 0)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0);

        // An ice wall: a low gather, then both arms rise to the chest and press out as the wall climbs.
        define(Gesture.WATER_ICE_WALL)
                .key(0, S).m(20, -10, 10).o(20, 10, -10).drop(0).legs(0, 0)
                .key(4, I).m(40, -20, 16).o(40, 20, -16).drop(0.14F).legs(-24, -24)
                .key(9, B).m(-110, -22, 4).o(-110, 22, -4).drop(-0.02F).legs(-10, 4).head(-8, 0, 0)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0).legs(0, 0).head(0, 0, 0);

        // Bloodbending takes hold: the hands rise clawed toward the victim and the body hunches over them, trembling.
        define(Gesture.WATER_BLOOD_GRAB)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).lean(0).head(0, 0, 0).drop(0)
                .key(4, I).m(-40, -30, 20).o(-40, 30, -20).lean(6).head(8, 0, 0).drop(0.06F)
                .key(9, O).mAim(HELD, -10, -16, 0).oAim(HELD, -10, 16, 0).lean(16).head(14, 0, 4).drop(0.10F)
                .mTremble(3, 2.1F).oTremble(3, 2.3F)
                .key(14, S).mAim(HELD, -8, -12, 0).oAim(HELD, -8, 12, 0).lean(14).head(12, 0, 4).drop(0.08F)
                .mTremble(3, 2.1F).oTremble(3, 2.3F);

        // The grip let go: both hands fling down and away, the body straightening.
        define(Gesture.WATER_BLOOD_RELEASE)
                .key(0, S).mAim(HELD, -8, -12, 0).oAim(HELD, -8, 12, 0).lean(14).head(12, 0, 4).drop(0.08F)
                .key(3, O).m(30, 0, 40).o(30, 0, -40).lean(-6).head(-6, 0, 0).drop(-0.02F)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0).drop(0);

        // The bending taken: the thumb goes to the victim's forehead and holds, the other hand at their chest, then withdraws.
        define(Gesture.WATER_SUPPRESS)
                .key(0, S).m(-40, -20, 10).o(-30, 20, -10).lean(0).head(0, 0, 0)
                .key(4, O).mAim(LOOK, -14, -6, 0).oAim(LOOK, 10, 10, 0).lean(10).head(6, 0, 0)
                .key(13, S).mAim(LOOK, -14, -6, 0).oAim(LOOK, 10, 10, 0).lean(10).head(6, 0, 0).mTremble(1.5F, 1.9F)
                .key(20, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0);
    }

    private WaterGestures() {
    }
}
