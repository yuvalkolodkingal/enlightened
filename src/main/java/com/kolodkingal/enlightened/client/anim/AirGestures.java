package com.kolodkingal.enlightened.client.anim;

import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.B;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.E;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.I;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.L;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.O;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.S;
import static com.kolodkingal.enlightened.client.anim.Gestures.Target.HELD;
import static com.kolodkingal.enlightened.client.anim.Gestures.Target.LOOK;
import static com.kolodkingal.enlightened.client.anim.Gestures.define;

/**
 * The keys of the airbenders' gestures. Airbending is circular and light (Ba Gua): spirals, spins and sweeps that
 * never stop, the body always moving, the weight never planted.
 */
final class AirGestures {
    static void register() {
        registerMore();
        // The air blast: a small step back as the arms spiral in across the chest, then both palms thrust along the look.
        define(Gesture.AIR_BLAST)
                .key(0, S).m(-60, -40, 10).o(-60, 40, -10).lean(-6).legs(0, 0).twist(0)
                .key(2, I).m(-80, -50, 16).o(-80, 50, -16).lean(-10).legs(14, -8).twist(-10)
                .key(4, O).mAim(LOOK, -2, -6, 0).oAim(LOOK, -2, 6, 0).lean(12).legs(-16, 8).twist(8).head(2, 0, 0)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).legs(0, 0).twist(0).head(0, 0, 0);

        // The shield rises: the arms draw a circle, up and out, and settle into the circling stance.
        define(Gesture.AIR_SHIELD_ON)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).twist(0).head(0, 0, 0)
                .key(5, E).m(-170, -20, 20).o(-170, 20, -20).twist(10).head(-10, 0, 0)
                .key(10, E).m(-90, 0, 90).o(-90, 0, -90).twist(-10)
                .key(14, S).m(StancePoses.AIR_SHIELD_M).o(StancePoses.AIR_SHIELD_O).twist(0).head(0, 0, 0);

        // The shield let go: the arms fall and cross, a breath out.
        define(Gesture.AIR_SHIELD_OFF)
                .key(0, S).m(StancePoses.AIR_SHIELD_M).o(StancePoses.AIR_SHIELD_O).head(0, 0, 0)
                .key(4, O).m(-30, -30, 0).o(-30, 30, 0).head(6, 0, 0)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O).head(0, 0, 0);

        // The air jump: a crouch, then the arms swing up and the legs tuck as the air lifts him.
        define(Gesture.AIR_JUMP)
                .key(0, S).m(20, 0, 10).o(20, 0, -10).drop(0).legs(0, 0).lean(0)
                .key(2, I).m(50, -10, 14).o(50, 10, -14).drop(0.16F).legs(-24, -24).lean(10)
                .key(5, O).m(-150, -10, 20).o(-150, 10, -20).drop(-0.04F).legs(-40, -20).lean(-6).head(-10, 0, 0)
                .key(12, E).m(-30, 0, 30).o(-30, 0, -30).drop(0).legs(0, 0).lean(0).head(0, 0, 0);

        // Air blades: the main arm chambers across the body, then slices level across with a turn of the hips.
        define(Gesture.AIR_BLADES)
                .key(0, S).m(-80, 50, 0).o(-20, 0, -20).twist(18).lean(2)
                .key(2, I).m(-90, 60, -10).o(-30, 0, -30).twist(24).lean(4)
                .key(5, O).m(-88, -60, 10).o(-10, 0, -16).twist(-26).lean(8).head(0, -6, 0)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);

        // Spirit projection: he sinks to the ground into the lotus, the hands meeting at the lap, and goes still.
        define(Gesture.AIR_PROJECTION)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).drop(0).legs(0, 0).head(0, 0, 0)
                .key(6, E).m(-40, -20, 20).o(-40, 20, -20).drop(0.30F).legs(-60, -60).head(6, 0, 0)
                .key(14, E).m(-30, -50, 0).o(-30, 50, 0).drop(0.62F).legs(-90, -90).head(10, 0, 0)
                .key(24, S).m(-30, -50, 0).o(-30, 50, 0).drop(0.62F).legs(-90, -90).head(12, 0, 0);

        // Onto the scooter: a half spin that gathers the ball, then a hop onto it into the seated stance.
        define(Gesture.AIR_SCOOTER_ON)
                .key(0, S).m(-30, 0, 20).o(-30, 0, -20).yaw(0).drop(0).legs(0, 0)
                .key(4, E).m(-70, 0, 70).o(-70, 0, -70).yaw(180).drop(0.04F).legs(-20, -20)
                .key(8, O).m(-50, 0, 50).o(-50, 0, -50).yaw(360).drop(-0.10F).legs(-70, -70)
                .key(12, S).m(StancePoses.SCOOTER_M).o(StancePoses.SCOOTER_O).yaw(360).drop(0).legs(-75, -75);

        // Off the scooter: he steps off with a light bounce.
        define(Gesture.AIR_SCOOTER_OFF)
                .key(0, S).m(StancePoses.SCOOTER_M).o(StancePoses.SCOOTER_O).drop(0).legs(-75, -75)
                .key(3, O).m(-40, 0, 40).o(-40, 0, -40).drop(-0.06F).legs(-20, -20)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0).legs(0, 0);

        // The tornado: a full turn on the ball of the foot with the arms out, the spiral gathering, then both palms push it on.
        define(Gesture.AIR_TORNADO)
                .key(0, S).m(-40, 0, 30).o(-40, 0, -30).yaw(0).lean(0).legs(0, 0)
                .key(4, I).m(-90, 0, 80).o(-90, 0, -80).yaw(100).lean(-4).legs(-10, 20)
                .key(10, L).m(-90, 0, 80).o(-90, 0, -80).yaw(300).lean(-4).legs(-10, 20).head(-6, 0, 0)
                .key(14, O).mAim(LOOK, -4, -8, 0).oAim(LOOK, -4, 8, 0).yaw(360).lean(10).legs(-16, 8).head(2, 0, 0)
                .key(22, E).m(StancePoses.REST_M).o(StancePoses.REST_O).yaw(360).lean(0).legs(0, 0).head(0, 0, 0);

        // Flight: the arms throw up and the body lifts, then they spread into the hover.
        define(Gesture.AIR_FLIGHT_ON)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).drop(0).legs(0, 0).head(0, 0, 0)
                .key(3, I).m(30, 0, 14).o(30, 0, -14).drop(0.10F).legs(-20, -20).head(6, 0, 0)
                .key(8, B).m(-176, -6, 10).o(-176, 6, -10).drop(-0.08F).legs(10, 10).head(-14, 0, 0)
                .key(16, E).m(StancePoses.FLIGHT_HOVER_M).o(StancePoses.FLIGHT_HOVER_O).drop(0).legs(0, 0).head(0, 0, 0);

        // The flight let go: the arms fold and he settles.
        define(Gesture.AIR_FLIGHT_OFF)
                .key(0, S).m(StancePoses.FLIGHT_HOVER_M).o(StancePoses.FLIGHT_HOVER_O).drop(0)
                .key(4, O).m(-20, 0, 10).o(-20, 0, -10).drop(0.06F)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0);

        // Suffocation: the open hand reaches at the victim and the fingers close, slowly, to a fist; the other arm pulls back.
        define(Gesture.AIR_SUFFOCATE)
                .key(0, S).mAim(HELD, -20, 10, 0).o(-20, 0, -14).lean(0).head(0, 0, 0)
                .key(4, O).mAim(HELD, 6, -4, 0).o(20, 10, -24).lean(6).head(6, 0, 0)
                .key(12, E).mAim(HELD, 0, 0, 0).o(30, 14, -30).lean(8).head(8, 0, -4);
    }

    static void registerMore() {
        // Air suction: the palms reach out and draw in to the chest, the body rocking back.
        define(Gesture.AIR_SUCTION)
                .key(0, S).m(-40, 0, 20).o(-40, 0, -20).lean(0)
                .key(3, O).mAim(LOOK, -2, -8, 0).oAim(LOOK, -2, 8, 0).lean(8)
                .key(7, E).m(-70, -40, 0).o(-70, 40, 0).lean(-10).head(-6, 0, 0)
                .key(10, S).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0);

        // The air burst: the arms cross at the chest and are thrown wide, the body rising on its toes.
        define(Gesture.AIR_BURST)
                .key(0, S).m(-70, -50, 0).o(-70, 50, 0).drop(0).head(0, 0, 0)
                .key(3, I).m(-80, -60, 0).o(-80, 60, 0).drop(0.06F).head(8, 0, 0)
                .key(5, O).m(-30, 0, 120).o(-30, 0, -120).drop(-0.08F).head(-10, 0, 0)
                .key(12, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0).head(0, 0, 0);

        // The air spout rises: the arms lift from the sides as the wind takes him.
        define(Gesture.AIR_SPOUT_ON)
                .key(0, S).m(10, 0, 10).o(10, 0, -10).drop(0).legs(0, 0)
                .key(5, I).m(30, 0, 14).o(30, 0, -14).drop(0.10F).legs(-16, -16)
                .key(10, B).m(-60, 0, 70).o(-60, 0, -70).drop(-0.08F).legs(-4, -4).head(-8, 0, 0)
                .key(14, S).m(20, 0, 48).o(20, 0, -48).drop(0).legs(0, 0).head(0, 0, 0);

        define(Gesture.AIR_SPOUT_OFF)
                .key(0, S).m(20, 0, 48).o(20, 0, -48).drop(0)
                .key(4, O).m(-20, 0, 30).o(-20, 0, -30).drop(0.06F)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0);
    }

    private AirGestures() {
    }
}
