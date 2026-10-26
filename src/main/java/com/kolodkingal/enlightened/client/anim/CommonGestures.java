package com.kolodkingal.enlightened.client.anim;

import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.B;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.E;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.I;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.O;
import static com.kolodkingal.enlightened.client.anim.AnimMath.Ease.S;
import static com.kolodkingal.enlightened.client.anim.Gestures.Target.LOOK;
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

        // The element step: a lean into the dash, the arms trailing.
        define(Gesture.STEP)
                .key(0, S).m(-10, 0, 10).o(-10, 0, -10).lean(0)
                .key(2, O).m(40, 0, 20).o(40, 0, -20).lean(18).head(-6, 0, 0)
                .key(8, E).m(StancePoses.REST_M).o(StancePoses.REST_O).lean(0).head(0, 0, 0);

        // A combo: a flourish bigger than any one cast: a full turn with the arms wide, ending in a thrust.
        define(Gesture.COMBO)
                .key(0, S).m(-60, 0, 40).o(-60, 0, -40).yaw(0).lean(0)
                .key(6, E).m(-90, 0, 90).o(-90, 0, -90).yaw(180).lean(-6).head(-8, 0, 0)
                .key(11, O).mAim(LOOK, -2, -8, 0).oAim(LOOK, -2, 8, 0).yaw(360).lean(14).head(2, 0, 0)
                .key(18, E).m(StancePoses.REST_M).o(StancePoses.REST_O).yaw(360).lean(0).head(0, 0, 0);

        // A chakra opens: from the lap the hands rise, palms up, to the heart, and open to the sky.
        define(Gesture.CHAKRA_OPEN)
                .key(0, S).m(-34, -48, 10).o(-34, 48, -10).head(6, 0, 0)
                .key(10, E).m(-80, -40, 0).o(-80, 40, 0).head(-4, 0, 0)
                .key(20, E).m(-150, -20, 20).o(-150, 20, -20).head(-14, 0, 0)
                .key(30, S).m(-34, -48, 10).o(-34, 48, -10).head(6, 0, 0);

        // The Avatar State: the body rises, the arms spread wide, the head thrown back to the light.
        define(Gesture.AVATAR_ENTER)
                .key(0, S).m(-20, 0, 10).o(-20, 0, -10).drop(0).head(0, 0, 0).lean(0)
                .key(4, I).m(-70, -40, 0).o(-70, 40, 0).drop(0.10F).head(10, 0, 0).lean(6)
                .key(10, B).m(-40, 0, 80).o(-40, 0, -80).drop(-0.5F).head(-22, 0, 0).lean(-10)
                .key(24, S).m(-40, 0, 70).o(-40, 0, -70).drop(-0.45F).head(-16, 0, 0).lean(-8);

        define(Gesture.AVATAR_LEAVE)
                .key(0, S).m(-40, 0, 70).o(-40, 0, -70).drop(-0.45F).head(-16, 0, 0)
                .key(8, E).m(-20, 0, 20).o(-20, 0, -20).drop(0.08F).head(8, 0, 0)
                .key(16, E).m(StancePoses.REST_M).o(StancePoses.REST_O).drop(0).head(0, 0, 0);
    }

    private CommonGestures() {
    }
}
