package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.client.EnlightenedClientConfig;
import com.kolodkingal.enlightened.client.anim.Gestures.ArmKey;
import com.kolodkingal.enlightened.client.anim.Gestures.Mode;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The body language of the benders, laid over the vanilla animation every frame. Poses are not assigned, they are
 * blended: each layer has a weight that eases in and out, so nothing snaps. In order (each blends into what the one
 * before left):
 * <ol>
 *     <li>capture what vanilla made, take the swing of an attack out of the arms (it is put back at the end), and see
 *     which arms vanilla holds in a pose of their own (a bow, a shield, an eaten item): they are left alone;</li>
 *     <li>locomotion: the flight, the glider, the scooter, and the walk and sprint of each nation ({@link Locomotion});</li>
 *     <li>the ambient stances, then the aimed and holding stances ({@link Stances});</li>
 *     <li>the playing gestures ({@link Gestures}), oldest first;</li>
 *     <li>the head, which keeps looking where the camera looks on a leaning body;</li>
 *     <li>the swing put back, and the overlay parts copied from what they cover.</li>
 * </ol>
 * The state (weights, lean, gestures) is made by {@link Animator}; the whole body's lean is on the pose stack
 * ({@link BodyLean}). Every state vanilla owns (swimming, elytra, riding, sleeping) is left as vanilla draws it.
 */
public final class BenderPoses {
    private static final float RAD = Mth.DEG_TO_RAD;

    private static final PoseContext CTX = new PoseContext();
    private static final float[] V = new float[3];
    private static final float[] TREMBLE = new float[2];
    private static final float[] HEAD = new float[3];
    private static final float[] LEG = new float[1];

    /** The fields vanilla never resets for the player model, which this layer writes: the head's and the body's roll. */
    public static void reset(PlayerModel<?> model) {
        model.head.zRot = 0.0F;
        model.body.zRot = 0.0F;
    }

    public static void apply(PlayerModel<?> model, LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
        // The first-person hand is drawn with an all-zero animation call: leave it alone.
        if (!(entity instanceof Player player) || ageInTicks == 0.0F) {
            return;
        }
        AnimState st = Animator.peek(player);
        if (st != null) {
            st.posed = false;
        }
        float pt = ageInTicks - player.tickCount;
        if (st == null || pt >= 1.0F || st.wVanilla > 0.0F || Math.abs(st.time - ageInTicks) > 1.5 || !EnlightenedClientConfig.animations()) {
            return;
        }
        st.posed = true;
        PoseContext c = CTX;
        c.begin(model, player, st, ageInTicks, pt);

        float vanillaTwist = takeOutSwing(c);
        yields(c);
        uprightFromCrouch(c);

        Locomotion.apply(c);

        Stances.ambient(c);
        Stances.aimed(c);

        applyGestures(c);

        head(c);

        putBackSwing(c, vanillaTwist);
        Locomotion.sync(model);
    }

    // ---- capture ----

    private static HumanoidArm attackArm(Player player) {
        return player.swingingArm == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    /** Vanilla added the swing of an attack to the arms (and twisted the body); take it out again, exactly. */
    private static float takeOutSwing(PoseContext c) {
        PlayerModel<?> m = c.model;
        float f = m.attackTime;
        if (f <= 0.0F) {
            return 0.0F;
        }
        float twist = m.body.yRot;
        float g = 1.0F - f;
        g = 1.0F - g * g * g * g;
        float f1 = Mth.sin(g * Mth.PI);
        float f2 = Mth.sin(f * Mth.PI) * -(c.lookPitch - 0.7F) * 0.75F;
        ModelPart attack = attackArm(c.player) == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
        attack.xRot += f1 * 1.2F + f2;
        attack.yRot -= twist * 2.0F;
        attack.zRot -= Mth.sin(f * Mth.PI) * -0.4F;
        m.rightArm.yRot -= twist;
        m.leftArm.yRot -= twist;
        m.leftArm.xRot -= twist;
        m.body.yRot = 0.0F;
        m.rightArm.x = -5.0F;
        m.rightArm.z = 0.0F;
        m.leftArm.x = 5.0F;
        m.leftArm.z = 0.0F;
        return twist;
    }

    /** Arms that hold a pose of their own (a bow, a shield, a spyglass) or an item in use are left to vanilla. */
    private static void yields(PoseContext c) {
        PlayerModel<?> m = c.model;
        Player p = c.player;
        boolean right = !(m.rightArmPose == ArmPose.EMPTY || m.rightArmPose == ArmPose.ITEM);
        boolean left = !(m.leftArmPose == ArmPose.EMPTY || m.leftArmPose == ArmPose.ITEM);
        if (m.rightArmPose == ArmPose.BOW_AND_ARROW || m.rightArmPose == ArmPose.CROSSBOW_CHARGE || m.rightArmPose == ArmPose.CROSSBOW_HOLD
                || m.leftArmPose == ArmPose.BOW_AND_ARROW || m.leftArmPose == ArmPose.CROSSBOW_CHARGE || m.leftArmPose == ArmPose.CROSSBOW_HOLD) {
            right = true;
            left = true;
        }
        if (p.isUsingItem()) {
            HumanoidArm using = p.getUsedItemHand() == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
            if (using == HumanoidArm.RIGHT) {
                right = true;
            } else {
                left = true;
            }
        }
        boolean mRight = c.mSide == HumanoidArm.RIGHT;
        c.yieldM = mRight ? right : left;
        c.yieldO = mRight ? left : right;
    }

    /** In the air vanilla's sneak (the key that descends) must not crouch the body. */
    private static void uprightFromCrouch(PoseContext c) {
        PlayerModel<?> m = c.model;
        float w = Math.max(c.st.wAir, c.st.wGlide);
        if (!m.crouching || w <= 0.0F) {
            return;
        }
        float keep = 1.0F - w;
        m.body.xRot *= keep;
        m.rightArm.xRot -= 0.4F * w;
        m.leftArm.xRot -= 0.4F * w;
        m.rightLeg.z *= keep;
        m.leftLeg.z *= keep;
        m.rightLeg.y = Mth.lerp(w, m.rightLeg.y, 12.0F);
        m.leftLeg.y = Mth.lerp(w, m.leftLeg.y, 12.0F);
        m.head.y *= keep;
        m.body.y *= keep;
        m.rightArm.y = Mth.lerp(w, m.rightArm.y, 2.0F);
        m.leftArm.y = Mth.lerp(w, m.leftArm.y, 2.0F);
    }

    // ---- gestures ----

    private static final Gestures.ArmSolver SOLVER = (key, main, out) -> solve(CTX, key, main, out);

    private static void solve(PoseContext c, ArmKey key, boolean main, float[] out) {
        switch (key.mode) {
            case AIM -> c.aimAngles(main, key.target, key.x, key.y, key.z, out);
            case WORLD -> {
                out[0] = c.worldX(key.x) * RAD;
                out[1] = key.y * c.side * RAD;
                out[2] = key.z * c.side * RAD;
            }
            default -> {
                out[0] = key.x * RAD;
                out[1] = key.y * c.side * RAD;
                out[2] = key.z * c.side * RAD;
            }
        }
    }

    /** Plays the gestures, the oldest first, so the newest wins where they overlap. */
    private static void applyGestures(PoseContext c) {
        AnimState.Slot[] slots = c.st.slots;
        int done = 0;
        for (int pass = 0; pass < slots.length; pass++) {
            int next = -1;
            for (int i = 0; i < slots.length; i++) {
                AnimState.Slot slot = slots[i];
                if ((done & (1 << i)) == 0 && slot.active && slot.weight > 0.0F && (next < 0 || slot.start < slots[next].start)) {
                    next = i;
                }
            }
            if (next < 0) {
                break;
            }
            done |= 1 << next;
            applyGesture(c, slots[next]);
        }
    }

    private static void applyGesture(PoseContext c, AnimState.Slot slot) {
        Gestures.Def def = Gestures.get(slot.id);
        float t = slot.t;
        float w = slot.weight;
        boolean force = slot.id.force;
        c.forceM = force;
        c.forceO = force;
        for (int arm = 0; arm < 2; arm++) {
            boolean main = arm == 0;
            Mode mode = def.sampleArm(main, t, SOLVER, V, TREMBLE);
            if (mode == null) {
                continue;
            }
            if (mode == Mode.ADD) {
                c.armAddRad(main, V[0], V[1], V[2], w);
            } else {
                c.armRad(main, V[0], V[1], V[2], w);
                if (force) {
                    c.occupy(main, w);
                }
            }
            if (TREMBLE[0] > 0.0F) {
                c.tremble(main, TREMBLE[0], TREMBLE[1], w);
            }
        }
        if (def.right.sample(t, LEG)) {
            c.legX(true, LEG[0], w);
        }
        if (def.left.sample(t, LEG)) {
            c.legX(false, LEG[0], w);
        }
        if (def.head.sample(t, HEAD)) {
            c.head(HEAD[0], HEAD[1], HEAD[2], w);
        }
        float twist = def.twist.scalar(t, 0.0F);
        if (twist != 0.0F) {
            c.twist(twist, w);
        }
        c.forceM = false;
        c.forceO = false;
    }

    // ---- head ----

    /** The head keeps looking where the camera looks even when the body leans. What the stances and gestures added stays. */
    private static void head(PoseContext c) {
        ModelPart head = c.model.head;
        float tilt = Math.abs(c.pitch) + Math.abs(c.roll);
        float addX = head.xRot - c.lookPitch;
        float addY = head.yRot - c.lookYaw;
        float addZ = head.zRot;
        float x = c.lookPitch;
        float y = c.lookYaw;
        if (tilt > 0.5F || Math.abs(c.yaw) > 0.5F) {
            float a = AnimMath.ss(0.5F, 3.0F, tilt + Math.abs(c.yaw));
            float[] aim = AimMath.head(AimMath.lookDirection(c.lookPitch, c.lookYaw), c.pitch, c.roll, c.yaw * c.side);
            float ax = Mth.clamp(aim[0], -70.0F * RAD, 60.0F * RAD);
            float ay = Mth.clamp(aim[1], -75.0F * RAD, 75.0F * RAD);
            x = Mth.lerp(a, x, ax);
            y = Mth.lerp(a, y, ay);
        }
        float counter = Mth.clamp(0.4F * c.roll, -12.0F, 12.0F) * RAD;
        head.xRot = x + addX;
        head.yRot = y + addY;
        head.zRot = addZ + counter;
    }

    // ---- the swing and the shoulders ----

    private static void putBackSwing(PoseContext c, float vanillaTwist) {
        PlayerModel<?> m = c.model;
        float f = m.attackTime;
        float twist = c.twist * RAD;
        if (f > 0.0F) {
            float g = 1.0F - f;
            g = 1.0F - g * g * g * g;
            float f1 = Mth.sin(g * Mth.PI);
            float f2 = Mth.sin(f * Mth.PI) * -(c.lookPitch - 0.7F) * 0.75F;
            HumanoidArm attack = attackArm(c.player);
            boolean attackIsM = attack == c.mSide;
            float occupied = attackIsM ? c.occupiedM : c.occupiedO;
            float keep = 1.0F - 0.75F * Mth.clamp(occupied, 0.0F, 1.0F);
            ModelPart arm = attack == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
            arm.xRot -= (f1 * 1.2F + f2) * keep;
            arm.yRot += vanillaTwist * 2.0F;
            arm.zRot += Mth.sin(f * Mth.PI) * -0.4F * keep;
            m.rightArm.yRot += vanillaTwist;
            m.leftArm.yRot += vanillaTwist;
            m.leftArm.xRot += vanillaTwist;
            twist += vanillaTwist;
        }
        if (twist != 0.0F) {
            m.body.yRot = twist;
            m.rightArm.z = Mth.sin(twist) * 5.0F;
            m.rightArm.x = -Mth.cos(twist) * 5.0F;
            m.leftArm.z = -Mth.sin(twist) * 5.0F;
            m.leftArm.x = Mth.cos(twist) * 5.0F;
        }
    }

    private BenderPoses() {
    }
}
