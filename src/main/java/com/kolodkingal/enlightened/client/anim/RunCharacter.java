package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.bending.Element;
import net.minecraft.util.Mth;

/**
 * How each nation walks and sprints: the earthbender rooted and heavy, the airbender light and bouncy, the waterbender
 * flowing with a swing of the shoulders, the firebender sharp and leaning in. These scale the vanilla swing (so the
 * feet still match the ground) and add a lean, a twist of the shoulders and a bob.
 */
enum RunCharacter {
    NONE(1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F),
    EARTH(0.8F, 0.9F, 1.1F, 9.0F, 3.0F, 0.030F),
    AIR(1.15F, 1.3F, 1.0F, 6.0F, 6.0F, 0.016F),
    WATER(1.2F, 1.2F, 1.0F, 8.0F, 9.0F, 0.020F),
    FIRE(0.9F, 1.15F, 1.05F, 14.0F, 5.0F, 0.022F);

    /** How much of the vanilla arm swing shows walking and sprinting, and of the leg swing sprinting. */
    final float walkArms;
    final float sprintArms;
    final float sprintLegs;
    /** The lean (degrees) of a sprint, the twist (degrees) of the shoulders with each stride, the bob (blocks). */
    final float lean;
    final float twist;
    final float bob;

    RunCharacter(float walkArms, float sprintArms, float sprintLegs, float lean, float twist, float bob) {
        this.walkArms = walkArms;
        this.sprintArms = sprintArms;
        this.sprintLegs = sprintLegs;
        this.lean = lean;
        this.twist = twist;
        this.bob = bob;
    }

    static RunCharacter of(Element element) {
        return switch (element) {
            case EARTH -> EARTH;
            case AIR -> AIR;
            case WATER -> WATER;
            case FIRE -> FIRE;
            case AVATAR -> AIR;
            default -> NONE;
        };
    }

    /** The bob (blocks) of a sprint: highest with the legs spread. */
    static float bob(AnimState st, RunCharacter ch) {
        if (st.wSprint <= 0.0F) {
            return 0.0F;
        }
        return ch.bob * (Math.abs(Mth.cos(st.limbSwing * 0.6662F)) - 0.5F) * Math.min(1.0F, st.limbAmount) * st.wSprint;
    }
}
