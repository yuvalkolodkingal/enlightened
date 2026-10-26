package com.kolodkingal.enlightened.client.anim;

import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.E;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.O;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.S;
import static com.kolodkingal.enlightened.client.anim.Gestures.define;

/** The keys of the gestures every bender shares: the look change and the landings. */
final class CommonGestures {
    static void register() {
        // A new look: the main hand passes over the face, top to chin, and the head turns away a touch and back.
        define(Gesture.LOOK_CHANGE)
                .key(0, S).m(-10, 0, 6).head(0, 0, 0)
                .key(3, O).m(-150, -22, -8).head(-6, 6, 0)
                .key(8, E).m(-80, -30, -4).head(4, -4, 0)
                .key(12, S).m(StancePoses.REST_M).head(0, 0, 0);

        // The airbender touches down on a cushion of air: the knees give a little, the arms float out and settle.
        define(Gesture.AIR_LAND)
                .key(0, S).m(-20, 0, 40).o(-20, 0, -40).drop(0).legs(0, 0)
                .key(3, O).m(-30, 0, 55).o(-30, 0, -55).drop(0.08F).legs(-16, -16)
                .key(12, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0).legs(0, 0);

        // The earthbender lands like a dropped boulder: a deep dip, one fist to the ground, then up.
        define(Gesture.EARTH_LAND)
                .key(0, S).m(-30, 0, 20).o(-30, 0, -20).drop(0).lean(0).legs(0, 0)
                .key(2, O).m(70, -20, 10).o(-60, 0, -50).drop(0.42F).lean(22).legs(-60, 20).head(-10, 0, 0)
                .key(7, S).m(40, -10, 8).o(-40, 0, -40).drop(0.30F).lean(16).legs(-48, 14)
                .key(14, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0).lean(0).legs(0, 0).head(0, 0, 0);
    }

    private CommonGestures() {
    }
}
