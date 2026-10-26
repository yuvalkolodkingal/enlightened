package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.client.anim.Gestures.Target;
import net.minecraft.util.Mth;

import static com.kolodkingal.enlightened.client.anim.AnimMath.ss;

/**
 * The pose rows of the stances ({@link Stance}): ambient ones first, then the aimed and holding ones, each blended
 * into the model by its eased weight. {@code AIM} rows point an arm in three dimensions at a target, so a fist still
 * points where the bender looks on a leaning body.
 */
final class Stances {

    /** Applies every ambient stance. */
    static void ambient(PoseContext c) {
        AnimState st = c.st;
        float age = c.age;
        float ground = 1.0F - Math.max(st.wAir, st.wGlide);
        float w;
        if ((w = st.stance(Stance.ARMORED)) > 0.001F) {
            // Heavy: the arms held a little out by the stone, a wide stance, the head up.
            c.arm(true, StancePoses.ARMORED_M, w * ground);
            c.arm(false, StancePoses.ARMORED_O, w * ground);
            c.leg(true, 0, 5, w * ground);
            c.leg(false, 0, -5, w * ground);
            c.head(2, 0, 0, w);
        }
        if ((w = st.stance(Stance.SEISMIC)) > 0.001F) {
            // Knees bent, the palms open low at the sides, the head tilted a touch to listen through the feet.
            c.arm(true, StancePoses.SEISMIC_M, w * ground);
            c.arm(false, StancePoses.SEISMIC_O, w * ground);
            c.leg(true, -12, 6, w * ground * (1.0F - st.limbAmount));
            c.leg(false, -12, -6, w * ground * (1.0F - st.limbAmount));
            c.head(3, 0, 5 + 1.5F * Mth.sin(0.05F * age), w);
        }
        if ((w = st.stance(Stance.AIR_SHIELD)) > 0.001F) {
            // The arms out at the sides, circling slowly with the wind.
            float circle = 6.0F * Mth.sin(0.12F * age);
            float circle2 = 6.0F * Mth.cos(0.12F * age);
            c.arm(true, StancePoses.AIR_SHIELD_M[0] + circle, circle2, StancePoses.AIR_SHIELD_M[2] + circle2, w);
            c.arm(false, StancePoses.AIR_SHIELD_O[0] - circle, circle2, StancePoses.AIR_SHIELD_O[2] - circle2, w);
        }
        if ((w = st.stance(Stance.OCTOPUS)) > 0.001F) {
            // The arms wide, swaying with the tentacles, the legs a little bent.
            float wave = 14.0F * Mth.sin(0.22F * age);
            float wave2 = 10.0F * Mth.sin(0.22F * age + 1.2F);
            c.arm(true, StancePoses.OCTOPUS_M[0] + wave, 0, StancePoses.OCTOPUS_M[2] + wave2, w);
            c.arm(false, StancePoses.OCTOPUS_O[0] - wave, 0, StancePoses.OCTOPUS_O[2] - wave2, w);
            c.leg(true, -8, 4, w * ground * (1.0F - st.limbAmount));
            c.leg(false, -8, -4, w * ground * (1.0F - st.limbAmount));
        }
        if ((w = st.stance(Stance.SPOUT)) > 0.001F) {
            // Balancing on the column: arms low and out, the body swaying, the legs together.
            float sway = 8.0F * Mth.sin(0.2F * age);
            c.arm(true, StancePoses.SPOUT_M[0], 0, StancePoses.SPOUT_M[2] + sway, w);
            c.arm(false, StancePoses.SPOUT_O[0], 0, StancePoses.SPOUT_O[2] + sway, w);
            c.leg(true, -6 + 3.0F * Mth.sin(0.2F * age), 2, w);
            c.leg(false, -6 - 3.0F * Mth.sin(0.2F * age), -2, w);
            c.twist(3.0F * Mth.sin(0.2F * age + 0.6F), w);
        }
        if ((w = st.stance(Stance.WATER_ARMS)) > 0.001F) {
            // The forearms raised, the water on them rolling.
            float roll = 4.0F * Mth.sin(0.3F * age);
            c.arm(true, StancePoses.WATER_ARMS_M[0] + roll, StancePoses.WATER_ARMS_M[1], StancePoses.WATER_ARMS_M[2], w);
            c.arm(false, StancePoses.WATER_ARMS_O[0] - roll, StancePoses.WATER_ARMS_O[1], StancePoses.WATER_ARMS_O[2], w);
        }
        if ((w = st.stance(Stance.REDIRECT)) > 0.001F) {
            // The lightning stance: the leading arm out with two fingers, the other at the belly, the body turned side on.
            c.arm(true, StancePoses.REDIRECT_M, w);
            c.arm(false, StancePoses.REDIRECT_O, w);
            c.twist(12, w);
            c.head(0, -8, 0, w);
            c.leg(true, -10, 4, w * ground * (1.0F - st.limbAmount));
            c.leg(false, 12, -4, w * ground * (1.0F - st.limbAmount));
        }
    }

    /** Applies the aimed and holding stances: later rows win where they overlap. */
    static void aimed(PoseContext c) {
        AnimState st = c.st;
        float age = c.age;
        float ground = 1.0F - Math.max(st.wAir, st.wGlide);
        float w;
        if ((w = st.stance(Stance.ROCK_LIFT)) > 0.001F) {
            // The open palm under the rock, a slow bob with it; the other arm braced.
            float bob = 4.0F * Mth.sin(0.12F * age);
            c.aim(true, Target.HELD, -8 + bob, 0, 0, w);
            c.tremble(true, 1.2F, 1.5F, w);
            c.arm(false, -26, 0, -26, w);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.BLOODBENDING)) > 0.001F) {
            // Both clawed hands at the victim, trembling with the strain; the body hunched over them.
            c.aim(true, Target.HELD, -8, -12, 0, w);
            c.aim(false, Target.HELD, -8, 12, 0, w);
            c.tremble(true, 3.4F, 2.1F, w);
            c.tremble(false, 3.4F, 2.3F, w);
            c.head(12, 0, 4, w);
            c.leg(true, -10, 0, w * ground);
            c.leg(false, 10, 0, w * ground);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.SUFFOCATE)) > 0.001F) {
            // The fist clenched at the victim, the knuckles tightening; the other arm drawn back.
            float grip = 2.0F * Mth.sin(0.8F * age);
            c.aim(true, Target.HELD, grip, 0, 0, w);
            c.tremble(true, 1.5F, 2.0F, w);
            c.arm(false, 30, 14, -30, w);
            c.head(8, 0, -4, w);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.FIRE_FIST)) > 0.001F) {
            // The guard, with the fists punching along the look in turn, each snapping out and drawing back.
            float cycle = 0.55F * age;
            float punchM = Math.max(0.0F, Mth.sin(cycle));
            float punchO = Math.max(0.0F, -Mth.sin(cycle));
            punchM = punchM * punchM;
            punchO = punchO * punchO;
            c.aim(true, Target.LOOK, 6 - 8 * punchM, -12 + 10 * punchM, 0, w);
            c.aim(false, Target.LOOK, 6 - 8 * punchO, 12 - 10 * punchO, 0, w);
            c.twist(10.0F * Mth.sin(cycle), w);
            c.head(2, 0, 0, w);
            c.leg(true, -14, 4, w * ground * (1.0F - st.limbAmount));
            c.leg(false, 14, -4, w * ground * (1.0F - st.limbAmount));
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.FIRE_BREATH)) > 0.001F) {
            // Fire breathed: the head thrust forward, the arms bent back at the sides, the chest heaving.
            float heave = 3.0F * Mth.sin(0.4F * age);
            c.arm(true, StancePoses.FIRE_BREATH_M[0] + heave, StancePoses.FIRE_BREATH_M[1], StancePoses.FIRE_BREATH_M[2], w);
            c.arm(false, StancePoses.FIRE_BREATH_O[0] + heave, StancePoses.FIRE_BREATH_O[1], StancePoses.FIRE_BREATH_O[2], w);
            c.head(-6 + heave, 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.FIRE_WHIP)) > 0.001F) {
            // The main arm lashing the flame along the look: a figure of eight at the wrist; the other arm back.
            float lash = 12.0F * Mth.sin(0.9F * age);
            float lash2 = 8.0F * Mth.sin(1.8F * age);
            c.aim(true, Target.LOOK, -16 + lash, lash2, 0, w);
            c.arm(false, 34, 10, -12, w);
            c.twist(-8 + 4.0F * Mth.sin(0.9F * age), w);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.FIRE_BALL_CHARGE)) > 0.001F) {
            // The fist cocked at the hip as the ball gathers, trembling; the other palm out to aim.
            float gather = ss(0.0F, 20.0F, age - st.activeSinceAge());
            c.arm(true, StancePoses.FIRE_BALL_CHARGE_M[0] + 8 * gather, StancePoses.FIRE_BALL_CHARGE_M[1], StancePoses.FIRE_BALL_CHARGE_M[2], w);
            c.tremble(true, 1.0F + 2.5F * gather, 2.2F, w);
            c.aim(false, Target.LOOK, -4, 10, 0, w);
            c.twist(-14, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = st.stance(Stance.LIGHTNING_CHARGE)) > 0.001F) {
            // The arms circle wide (the classic generation), growing taut as the charge builds, then come to point.
            float held = age - st.activeSinceAge();
            float a = 0.5F * held;
            float build = ss(0.0F, 30.0F, held);
            float mx = -90 + 55 * Mth.sin(a) * (1.0F - 0.5F * build);
            float my = -30 - 35 * Mth.cos(a) * (1.0F - 0.5F * build);
            float ox = -90 - 55 * Mth.sin(a) * (1.0F - 0.5F * build);
            float oy = 30 + 35 * Mth.cos(a) * (1.0F - 0.5F * build);
            c.arm(true, mx, my, 16, w * (1.0F - build));
            c.arm(false, ox, oy, -16, w * (1.0F - build));
            c.aim(true, Target.LOOK, -10, -6, 0, w * build);
            c.arm(false, -30, 30, -30, w * build);
            c.tremble(true, 1.0F + 3.0F * build, 2.4F, w);
            c.tremble(false, 1.0F + 2.0F * build, 2.6F, w);
            c.twist(-10 * build, w);
            c.head(-4, 0, 0, w);
            c.leg(true, -14, 6, w * ground * (1.0F - st.limbAmount));
            c.leg(false, 12, -6, w * ground * (1.0F - st.limbAmount));
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** What the stances add to the body's lean (degrees, forward positive). */
    static float lean(AnimState st) {
        return 8.0F * st.stance(Stance.BLOODBENDING) + 4.0F * st.stance(Stance.FIRE_FIST) + 6.0F * st.stance(Stance.FIRE_BREATH)
                + 6.0F * st.stance(Stance.SEISMIC) * (1.0F - st.wAir) + 4.0F * st.stance(Stance.LIGHTNING_CHARGE);
    }

    /** What the stances add to the body's dip (blocks, positive sinks). */
    static float drop(AnimState st) {
        float ground = 1.0F - Math.max(st.wAir, st.wGlide);
        return 0.26F * st.stance(Stance.SCOOTER) + 0.12F * st.stance(Stance.SEISMIC) * ground * (1.0F - st.limbAmount)
                + 0.08F * st.stance(Stance.BLOODBENDING) * ground + 0.10F * st.stance(Stance.OCTOPUS) * ground * (1.0F - st.limbAmount)
                + 0.06F * st.stance(Stance.FIRE_FIST) * ground * (1.0F - st.limbAmount) + 0.08F * st.stance(Stance.LIGHTNING_CHARGE) * ground * (1.0F - st.limbAmount);
    }

    private Stances() {
    }
}
