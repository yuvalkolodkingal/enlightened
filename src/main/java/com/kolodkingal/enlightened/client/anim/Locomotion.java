package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.client.EnlightenedClientConfig;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;

import static com.kolodkingal.enlightened.client.anim.AnimMath.ss;

/**
 * The locomotion layer of the pose: the airbender's flight (hover, cruise and sprint poses blended by speed, with the
 * hover drift, the leg flutter and the lean of a climb), the glider (hands up on the frame, legs trailing), the air
 * scooter (seated, one hand down steering), and everyone's walk and sprint with the character of their nation.
 */
final class Locomotion {

    static void apply(PoseContext c) {
        flight(c);
        glide(c);
        scooter(c);
        walk(c);
    }

    /** Tetherless flight: Zaheer's open spread, arms sweeping back as he speeds up. */
    private static void flight(PoseContext c) {
        AnimState st = c.st;
        float w = st.wAir;
        if (w <= 0.001F) {
            return;
        }
        float s = st.s;
        float age = c.age;
        float calm = EnlightenedClientConfig.reducedMotion() ? 0.3F : 1.0F;
        float cruise = ss(0.2F, 1.0F, s);
        float sprint = ss(1.0F, 2.0F, s);
        float[] hm = StancePoses.FLIGHT_HOVER_M;
        float[] cm = StancePoses.FLIGHT_CRUISE_M;
        float[] sm = StancePoses.FLIGHT_SPRINT_M;
        float mx = AnimMath.lerp(sprint, AnimMath.lerp(cruise, hm[0], cm[0]), sm[0]);
        float my = AnimMath.lerp(sprint, AnimMath.lerp(cruise, hm[1], cm[1]), sm[1]);
        float mz = AnimMath.lerp(sprint, AnimMath.lerp(cruise, hm[2], cm[2]), sm[2]);
        // Backwards: the hover with the arms and legs trailing forward.
        float back = ss(0.1F, 0.6F, -s);
        float armBack = -22.0F * back;
        float legBack = -10.0F * back;
        // Hovering: the arms drift on the air, the legs sway.
        float hover = (1.0F - ss(0.2F, 0.8F, st.sH)) * calm;
        float drift = 5.0F * hover;
        float mDriftX = drift * Mth.sin(0.07F * age);
        float oDriftX = drift * Mth.sin(0.07F * age + 1.7F);
        float mDriftZ = drift * Mth.sin(0.09F * age);
        float oDriftZ = drift * Mth.sin(0.09F * age + 1.7F);
        float sway = 4.0F * hover * Mth.sin(0.07F * age);
        // A climb or descent: arms in or out, legs up or forward.
        float vertical = ss(0.3F, 0.8F, Math.abs(st.u)) * (1.0F - ss(0.3F, 0.8F, s));
        float armIn = st.u > 0.0F ? -6.0F * vertical : 8.0F * vertical;
        float legV = st.u > 0.0F ? 4.0F * vertical : -6.0F * vertical;
        // At speed the legs trail and flutter in the wind.
        float flutter = 5.0F * ss(0.5F, 1.5F, s) * Mth.sin(0.45F * age);
        float legTrail = 12.0F * cruise + 6.0F * sprint;
        c.arm(true, mx + mDriftX + armBack, my, mz + mDriftZ + armIn, w);
        c.arm(false, mx + oDriftX + armBack, -my, -(mz + oDriftZ + armIn), w);
        c.leg(true, legTrail + sway + legBack + legV + flutter, 3.0F + 2.0F * cruise, w);
        c.leg(false, legTrail + sway + legBack + legV - flutter, -3.0F - 2.0F * cruise, w);
        c.head(-10.0F * cruise - 8.0F * sprint, 0, 0, w);
    }

    /**
     * The glider: he lies flat under it, both hands up gripping the spar over his shoulders, the head up looking ahead,
     * the legs together trailing straight, never a running stride: they only drift a little in the wind, part a touch
     * in a slow glide, lift in a dive and drop in a climb; the arms lean into each turn.
     */
    private static void glide(PoseContext c) {
        AnimState st = c.st;
        float w = st.wGlide;
        if (w <= 0.001F) {
            return;
        }
        float age = c.age;
        float calm = EnlightenedClientConfig.reducedMotion() ? 0.3F : 1.0F;
        float wind = 2.5F * calm * Mth.sin(0.3F * age);
        float bank = Mth.clamp(st.turn * 0.6F, -8.0F, 8.0F);
        // The hands hold the staff overhead whenever it is open, on the ground too; the body lies flat only in the air.
        float grip = Math.max(w, st.stance(Stance.GLIDE));
        c.arm(true, StancePoses.GLIDE_M[0] + wind, StancePoses.GLIDE_M[1] - bank, StancePoses.GLIDE_M[2], grip);
        c.arm(false, StancePoses.GLIDE_O[0] - wind, StancePoses.GLIDE_O[1] - bank, StancePoses.GLIDE_O[2], grip);
        float flutter = 2.0F * calm * Mth.sin(0.35F * age);
        float slow = 1.0F - ss(0.3F, 1.0F, st.s);
        float pitch = 6.0F * Mth.clamp(-st.u, -1.0F, 1.0F);
        // Full weight on the legs as soon as the glide starts, so the walk's swing never shows under it.
        float legs = Math.max(w, ss(0.0F, 0.3F, grip) * (st.onGround ? 0.0F : 1.0F));
        c.leg(true, 4.0F + pitch + flutter, 1.0F + 4.0F * slow, legs);
        c.leg(false, 4.0F + pitch - flutter, -1.0F - 4.0F * slow, legs);
        c.head(-58.0F, 0, 0, w);
        c.occupy(true, grip);
        c.occupy(false, grip);
    }

    /** The air scooter: seated on the ball with the knees up, the main hand low steering, the off arm out for balance. */
    private static void scooter(PoseContext c) {
        AnimState st = c.st;
        float w = st.wScooter;
        if (w <= 0.001F) {
            return;
        }
        float age = c.age;
        float calm = EnlightenedClientConfig.reducedMotion() ? 0.3F : 1.0F;
        float moving = ss(0.04F, 0.3F, st.speed);
        float bounce = 3.0F * calm * Mth.sin(0.5F * age) * moving;
        float steer = Mth.clamp(st.turn * 1.5F, -20.0F, 20.0F);
        c.arm(true, StancePoses.SCOOTER_M[0] + bounce, StancePoses.SCOOTER_M[1] + steer, StancePoses.SCOOTER_M[2], w);
        c.arm(false, StancePoses.SCOOTER_O[0] - bounce, StancePoses.SCOOTER_O[1] + steer, StancePoses.SCOOTER_O[2] - 10.0F * moving, w);
        c.leg(true, -76.0F + bounce, 9.0F, w);
        c.leg(false, -76.0F - bounce, -9.0F, w);
        c.twist(-steer * 0.4F, w);
    }

    /** Walk and sprint character: scales the vanilla swing that is already in the arms and legs. */
    private static void walk(PoseContext c) {
        AnimState st = c.st;
        PlayerModel<?> model = c.model;
        float free = (1.0F - st.wAir) * (1.0F - st.wGlide) * (1.0F - st.wScooter);
        if (free <= 0.001F) {
            return;
        }
        RunCharacter ch = RunCharacter.of(st.element);
        float k = AnimMath.lerp(st.wSprint, ch.walkArms, ch.sprintArms);
        float swing = st.limbSwing * 0.6662F;
        float amount = st.limbAmount;
        float rightWalk = Mth.cos(swing + Mth.PI) * amount;
        float leftWalk = Mth.cos(swing) * amount;
        model.rightArm.xRot += (k - 1.0F) * share(model.rightArmPose) * rightWalk * free;
        model.leftArm.xRot += (k - 1.0F) * share(model.leftArmPose) * leftWalk * free;
        float kLegs = AnimMath.lerp(st.wSprint, 1.0F, ch.sprintLegs);
        model.rightLeg.xRot += (kLegs - 1.0F) * Mth.cos(swing) * 1.4F * amount * free;
        model.leftLeg.xRot += (kLegs - 1.0F) * Mth.cos(swing + Mth.PI) * 1.4F * amount * free;
        if (st.wSprint > 0.001F) {
            c.twist(-ch.twist * Mth.cos(swing) * Math.min(1.0F, amount), st.wSprint * free);
        }
    }

    /** The overlay parts follow the parts they cover. */
    static void sync(PlayerModel<?> model) {
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    private static float share(ArmPose pose) {
        return pose == ArmPose.EMPTY ? 1.0F : pose == ArmPose.ITEM ? 0.5F : 0.0F;
    }

    private Locomotion() {
    }
}
