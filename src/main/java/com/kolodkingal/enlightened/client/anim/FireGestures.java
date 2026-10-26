package com.kolodkingal.enlightened.client.anim;

import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.B;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.E;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.I;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.L;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.O;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.S;
import static com.kolodkingal.enlightened.client.anim.Gestures.Target.LOOK;
import static com.kolodkingal.enlightened.client.anim.Gestures.define;

/**
 * The keys of the firebenders' gestures. Firebending is sharp and aggressive (Northern Shaolin): straight punches and
 * kicks that snap from the hips, the breath driving every strike, the body always pressing forward.
 */
final class FireGestures {
    static void register() {
        registerMore();
        // A fire ball: the fist chambers at the hip, then punches straight along the look with the hips snapping into it.
        define(Gesture.FIRE_BALL)
                .key(0, S).m(StancePoses.FIRE_BALL_CHARGE_M).o(StancePoses.FIRE_BALL_CHARGE_O).twist(-16).lean(2)
                .key(1, I).m(36, -40, 18).o(-40, 16, -12).twist(-20).lean(0)
                .key(3, O).mAim(LOOK, 0, -4, 0).o(20, 10, -26).twist(22).lean(12).head(2, 0, 0)
                .key(10, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);

        // A big fire ball: both fists at the hips, then both drive together, the body behind them.
        define(Gesture.FIRE_BALL_BIG)
                .key(0, S).m(30, -30, 14).o(30, 30, -14).lean(-4).drop(0).legs(0, 0)
                .key(2, I).m(44, -36, 18).o(44, 36, -18).lean(-8).drop(0.06F).legs(10, -10)
                .key(5, O).mAim(LOOK, -2, -14, 0).oAim(LOOK, -2, 14, 0).lean(16).drop(0.02F).legs(-20, 10).head(4, 0, 0)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).drop(0).legs(0, 0).head(0, 0, 0);

        // The fire fist begins: a snap into the guard, fists up.
        define(Gesture.FIRE_FIST_START)
                .key(0, S).m(-20, -10, 10).o(-20, 10, -10).lean(0)
                .key(3, O).mAim(LOOK, 10, -14, 0).oAim(LOOK, 10, 14, 0).lean(8).head(4, 0, 0)
                .key(6, S).mAim(LOOK, 6, -12, 0).oAim(LOOK, 6, 12, 0).lean(6).head(2, 0, 0);

        // The whip begins: the arm swings back and the flame uncoils forward.
        define(Gesture.FIRE_WHIP_START)
                .key(0, S).m(40, 20, 20).o(-20, 0, -14).twist(16).lean(-4)
                .key(3, O).m(-120, -20, 14).o(30, 10, -20).twist(-14).lean(8)
                .key(8, S).m(-100, -24, 14).o(34, 10, -12).twist(-10).lean(6);

        // The fire ring: a spinning back kick, the flame leaving the heel; a full turn on the planted foot.
        define(Gesture.FIRE_RING)
                .key(0, S).m(-30, -20, 20).o(-30, 20, -20).yaw(0).legs(0, 0).lean(0).drop(0)
                .key(3, I).m(-60, -30, 30).o(-60, 30, -30).yaw(60).legs(-20, 0).lean(4).drop(0.06F)
                .key(8, L).m(-80, 0, 80).o(-80, 0, -80).yaw(240).legs(-96, 10).lean(14).drop(-0.02F).roll(10)
                .key(12, O).m(-50, 0, 60).o(-50, 0, -60).yaw(360).legs(-30, 4).lean(6).drop(0.08F).roll(0)
                .key(18, E).m(StancePoses.REST_M).o(StancePoses.REST_O).yaw(360).legs(0, 0).lean(0).drop(0);

        // Healing with fire: the palms meet, warm, and pass slowly over the hurt.
        define(Gesture.FIRE_HEAL)
                .key(0, S).m(-70, -40, 0).o(-70, 40, 0).head(0, 0, 0)
                .key(6, E).m(-76, -46, 0).o(-76, 46, 0).head(10, 0, 0).mTremble(1.2F, 1.5F).oTremble(1.2F, 1.5F)
                .key(14, E).m(-88, -16, 12).o(-88, 16, -12).head(12, 0, 0)
                .key(24, S).m(StancePoses.REST_M).o(StancePoses.REST_O).head(0, 0, 0);

        // Lightning: the arms come round from the last circle and the two fingers thrust along the look.
        define(Gesture.FIRE_LIGHTNING)
                .key(0, S).m(-130, -50, 10).o(-40, 30, -30).twist(-18).lean(-6)
                .key(2, I).m(-150, -60, 0).o(-20, 40, -40).twist(-24).lean(-10)
                .key(4, O).mAim(LOOK, 0, -4, 0).o(40, 20, -30).twist(26).lean(14).head(4, 0, 0).mTremble(1.5F, 2.4F)
                .key(12, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);

        // Lightning with both hands: both arms circle wide and meet, the fingers of both thrust together.
        define(Gesture.FIRE_LIGHTNING_BOTH)
                .key(0, S).m(-120, 0, 90).o(-120, 0, -90).lean(-8).drop(0)
                .key(3, I).m(-160, -30, 30).o(-160, 30, -30).lean(-12).drop(0.04F).head(-10, 0, 0)
                .key(6, O).mAim(LOOK, 0, -10, 0).oAim(LOOK, 0, 10, 0).lean(16).drop(0).head(4, 0, 0)
                .mTremble(2, 2.4F).oTremble(2, 2.4F)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0);

        // Redirection readied: the main arm comes out with two fingers, the off hand tucked at the belly.
        define(Gesture.FIRE_REDIRECT_ON)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).twist(0)
                .key(5, O).m(-110, -40, 10).o(60, 20, 20).twist(14).head(0, -8, 0)
                .key(12, E).m(StancePoses.REDIRECT_M).o(StancePoses.REDIRECT_O).twist(10).head(0, -6, 0);

        define(Gesture.FIRE_REDIRECT_OFF)
                .key(0, S).m(StancePoses.REDIRECT_M).o(StancePoses.REDIRECT_O).twist(10).head(0, -6, 0)
                .key(4, O).m(-30, -20, 10).o(10, 10, 10).twist(-4).head(0, 0, 0)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0);

        // Lightning caught: it enters the leading fingers, is drawn across the belly, and leaves the other hand.
        define(Gesture.FIRE_REDIRECTED)
                .key(0, S).m(StancePoses.REDIRECT_M).o(StancePoses.REDIRECT_O).twist(10).lean(0).head(0, -6, 0)
                .key(2, O).m(-40, -50, 0).o(50, 30, 20).twist(30).lean(-6).head(-4, 0, 0).mTremble(4, 2.6F)
                .key(6, E).m(50, 20, 20).oAim(LOOK, 0, 10, 0).twist(-30).lean(12).head(4, 0, 0).oTremble(3, 2.6F)
                .key(16, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);
    }

    static void registerMore() {
        // The fire jet: a crouch, then thrown forward on the flames, the arms swept back.
        define(Gesture.FIRE_JET)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).drop(0).lean(0).legs(0, 0)
                .key(3, I).m(30, -10, 14).o(30, 10, -14).drop(0.20F).lean(14).legs(-40, -40).head(6, 0, 0)
                .key(5, O).m(50, 0, 22).o(50, 0, -22).drop(0).lean(10).legs(10, 10).head(-8, 0, 0)
                .key(10, S).m(50, 0, 22).o(50, 0, -22).drop(0).lean(6).legs(10, 10).head(-8, 0, 0);

        // The fire shield begins: the arms sweep out, palms down, as the fire rises round him.
        define(Gesture.FIRE_SHIELD_START)
                .key(0, S).m(-30, -30, 0).o(-30, 30, 0)
                .key(4, O).m(-60, 0, 70).o(-60, 0, -70).head(-4, 0, 0)
                .key(8, S).m(-60, 0, 70).o(-60, 0, -70).head(0, 0, 0);

        // A wall of fire: both arms rise from the hips to the chest and press out as the fire climbs.
        define(Gesture.FIRE_WALL)
                .key(0, S).m(30, -20, 12).o(30, 20, -12).lean(0).drop(0).legs(0, 0)
                .key(4, I).m(40, -24, 16).o(40, 24, -16).lean(-6).drop(0.08F).legs(-20, -20)
                .key(8, B).m(-110, -22, 4).o(-110, 22, -4).lean(10).drop(0).legs(-14, 8).head(-6, 0, 0)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).legs(0, 0).head(0, 0, 0);

        // Blaze: the arm sweeps low across the ground from one side to the other, the fire following it.
        define(Gesture.FIRE_BLAZE)
                .key(0, S).m(-20, 50, 10).o(-20, 0, -14).twist(16).lean(4)
                .key(3, I).m(10, 60, 20).o(-30, 0, -20).twist(22).lean(10).head(6, 0, 0)
                .key(7, O).m(-10, -60, 30).o(-10, 0, -16).twist(-24).lean(12).head(4, -6, 0)
                .key(12, E).m(StancePoses.REST_M).o(StancePoses.REST_O).twist(0).lean(0).head(0, 0, 0);

        // Heat control on: the hands open at the chest and the heat shimmers out of them.
        define(Gesture.FIRE_HEAT_ON)
                .key(0, S).m(-40, -30, 0).o(-40, 30, 0)
                .key(6, E).m(-70, -10, 30).o(-70, 10, -30).head(-4, 0, 0)
                .key(12, S).m(StancePoses.REST_M).o(StancePoses.REST_O).head(0, 0, 0);

        define(Gesture.FIRE_HEAT_OFF)
                .key(0, S).m(-40, -10, 20).o(-40, 10, -20)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O);

        // Illumination: the hand comes up, palm open, and a flame is lit on it.
        define(Gesture.FIRE_LIGHT_ON)
                .key(0, S).m(-10, 0, 6)
                .key(5, O).m(-100, -20, 10).head(0, -6, 0)
                .key(10, S).m(-70, -10, 10).head(0, 0, 0);

        define(Gesture.FIRE_LIGHT_OFF)
                .key(0, S).m(-70, -10, 10)
                .key(3, O).m(-90, -30, 0)
                .key(8, E).m(StancePoses.REST_M);

        // Combustion: two fingers to the third eye, then the head thrust forward as the blast leaves it.
        define(Gesture.FIRE_COMBUSTION)
                .key(0, S).m(-170, -34, -10).o(30, 10, -20).head(6, 0, 0).lean(0)
                .key(3, I).m(-176, -36, -12).o(40, 14, -24).head(12, 0, 0).lean(-6)
                .key(5, O).m(-80, -30, 20).o(-30, 10, -30).head(-16, 0, 0).lean(14)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).head(0, 0, 0).lean(0);
    }

    private FireGestures() {
    }
}
