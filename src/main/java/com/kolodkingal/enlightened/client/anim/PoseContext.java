package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.client.EnlightenedClientConfig;
import com.kolodkingal.enlightened.client.anim.Gestures.Target;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * What one {@code setupAnim} call is working on, and the helpers every layer of the pose uses to blend into the model:
 * arms and legs by raw angle, by world elevation or aimed at something, the head, a tremble, the shoulders' twist.
 *
 * <p>Pose rows are written for a right-handed player: {@code M} is the main arm with the right arm's signs, {@code O}
 * the off arm with the left arm's. A left-handed player's rows are mirrored with one rule: y and z are multiplied by
 * {@link #side}. One instance is reused for every call (the render thread draws one player at a time).
 */
final class PoseContext {
    private static final float RAD = Mth.DEG_TO_RAD;

    PlayerModel<?> model;
    Player player;
    AnimState st;
    float age;
    float pt;
    float bodyYaw;
    /** +1 when M is a right arm, -1 when it is a left arm. */
    float side;
    ModelPart mArm;
    ModelPart oArm;
    HumanoidArm mSide;
    /** The vanilla head angles (radians) before any change: where the camera looks relative to the body. */
    float lookPitch;
    float lookYaw;
    /** The lean actually drawn (degrees) and the extra turn of the body. */
    float pitch;
    float roll;
    float yaw;
    boolean yieldM;
    boolean yieldO;
    float occupiedM;
    float occupiedO;
    float twist;
    boolean forceM;
    boolean forceO;
    private final float[] tmp3 = new float[3];

    void begin(PlayerModel<?> model, Player player, AnimState st, float age, float pt) {
        this.model = model;
        this.player = player;
        this.st = st;
        this.age = age;
        this.pt = pt;
        this.bodyYaw = st.bodyYaw;
        mSide = player.getMainArm();
        mArm = mSide == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        oArm = mSide == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        side = mSide == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        lookPitch = model.head.xRot;
        lookYaw = model.head.yRot;
        pitch = st.renderPitch;
        roll = st.renderRoll;
        yaw = st.yawAdd;
        occupiedM = 0.0F;
        occupiedO = 0.0F;
        twist = 0.0F;
        forceM = false;
        forceO = false;
    }

    private boolean held(boolean main) {
        return main ? yieldM && !forceM : yieldO && !forceO;
    }

    // ---- arms ----

    /** Blends the main (M) or off (O) arm toward raw angles (degrees) by weight {@code w}. */
    void arm(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || held(main)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += (x * RAD - arm.xRot) * w;
        arm.yRot += (y * side * RAD - arm.yRot) * w;
        arm.zRot += (z * side * RAD - arm.zRot) * w;
    }

    void arm(boolean main, float[] pose, float w) {
        arm(main, pose[0], pose[1], pose[2], w);
    }

    /** Blends an arm toward absolute angles in radians (already mirrored). */
    void armRad(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || held(main)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += (x - arm.xRot) * w;
        arm.yRot += (y - arm.yRot) * w;
        arm.zRot += (z - arm.zRot) * w;
    }

    /** Adds angles in radians (already mirrored) to an arm, times the weight. */
    void armAddRad(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || held(main)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += x * w;
        arm.yRot += y * w;
        arm.zRot += z * w;
    }

    /** Adds raw angles (degrees) to an arm, times the weight. */
    void armAdd(boolean main, float x, float y, float z, float w) {
        if (w <= 0.0F || held(main)) {
            return;
        }
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += x * RAD * w;
        arm.yRot += y * side * RAD * w;
        arm.zRot += z * side * RAD * w;
    }

    /** The arm held by its world elevation: 0 hangs, 90 points level forward, 180 straight up; y and z are raw. */
    void armWorld(boolean main, float phi, float y, float z, float w) {
        arm(main, worldX(phi), y, z, w);
    }

    float worldX(float phi) {
        return -(phi + pitch);
    }

    /** Aims an arm at a target (or along the look) plus an offset in degrees, by weight {@code w}. */
    void aim(boolean main, Target target, float dx, float dy, float dz, float w) {
        if (w <= 0.0F || held(main)) {
            return;
        }
        aimAngles(main, target, dx, dy, dz, tmp3);
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += (tmp3[0] - arm.xRot) * w;
        arm.yRot += (tmp3[1] - arm.yRot) * w;
        arm.zRot += (tmp3[2] - arm.zRot) * w;
    }

    /** The absolute angles (radians) of an arm aimed at a target with an offset (degrees), clamped to what the arm reaches. */
    void aimAngles(boolean main, Target target, float dx, float dy, float dz, float[] out) {
        Vector3f dir = direction(main, target);
        float[] a = AimMath.arm(dir, pitch, roll, yaw * side);
        HumanoidArm which = main ? mSide : mSide.getOpposite();
        boolean right = which == HumanoidArm.RIGHT;
        float x = a[0] + dx * RAD;
        float y = a[1] + dy * side * RAD;
        float z = dz * side * RAD;
        x = Mth.clamp(x, -190.0F * RAD, 60.0F * RAD);
        y = right ? Mth.clamp(y, -60.0F * RAD, 100.0F * RAD) : Mth.clamp(y, -100.0F * RAD, 60.0F * RAD);
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    /** The unit direction in the yaw-aligned world frame (x right, y up, z forward) from an arm's shoulder to a target. */
    Vector3f direction(boolean main, Target target) {
        Vec3 point = target == Target.LOOK ? null : targetPoint();
        if (point == null) {
            return AimMath.lookDirection(lookPitch, lookYaw);
        }
        Vector3f shoulder = shoulder(main);
        Vector3f d = toLocal(point).sub(shoulder);
        if (d.lengthSquared() < 1.0E-6F) {
            return AimMath.lookDirection(lookPitch, lookYaw);
        }
        return d.normalize();
    }

    /** The world point of the thing held, or null when there is none (the arm then follows the look). */
    Vec3 targetPoint() {
        Entity held = st.heldEntity;
        if (held == null || held.isRemoved()) {
            return null;
        }
        return held.getPosition(pt).add(0.0, held.getBbHeight() * 0.5, 0.0);
    }

    /** A world position as an offset from the player's feet in the yaw-aligned frame (x right, y up, z forward). */
    Vector3f toLocal(Vec3 world) {
        Vec3 feet = player.getPosition(pt);
        double dx = world.x - feet.x;
        double dy = world.y - feet.y;
        double dz = world.z - feet.z;
        float yawRad = bodyYaw * RAD;
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);
        return new Vector3f((float) (-dx * cos - dz * sin), (float) dy, (float) (-dx * sin + dz * cos));
    }

    Vector3f shoulder(boolean main) {
        HumanoidArm which = main ? mSide : mSide.getOpposite();
        return modelPoint(which == HumanoidArm.RIGHT ? -5.0F : 5.0F, 2.0F, 0.0F);
    }

    /** A point of the model (pixels) in the yaw-aligned frame relative to the feet, as the renderer draws this frame. */
    Vector3f modelPoint(float mx, float my, float mz) {
        float scale = 0.9375F;
        float x = mx / 16.0F * scale;
        float y = (my / 16.0F - 1.501F) * scale;
        float z = mz / 16.0F * scale;
        Vector3f v = new Vector3f(-x, -y, z);
        float pivotY = st.pivot;
        v.y -= pivotY;
        v.rotateX(-pitch * RAD).rotateZ(-roll * RAD);
        v.y += pivotY;
        v.rotateY(-yaw * side * RAD);
        v.add(st.jx, st.bob - st.drop.x - st.addDrop, st.jz);
        return new Vector3f(v.x, v.y, -v.z);
    }

    /** Blends the physical right or left arm toward raw angles (degrees, that arm's own signs) by weight {@code w}. */
    void physArm(boolean right, float x, float y, float z, float w) {
        boolean main = right == (mSide == HumanoidArm.RIGHT);
        if (w <= 0.0F || held(main)) {
            return;
        }
        ModelPart arm = right ? model.rightArm : model.leftArm;
        arm.xRot += (x * RAD - arm.xRot) * w;
        arm.yRot += (y * RAD - arm.yRot) * w;
        arm.zRot += (z * RAD - arm.zRot) * w;
    }

    // ---- legs ----

    /** Blends a leg toward raw angles (degrees; x and z): the right leg has outward z positive. */
    void leg(boolean right, float x, float z, float w) {
        if (w <= 0.0F) {
            return;
        }
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        leg.xRot += (x * RAD - leg.xRot) * w;
        leg.zRot += (z * RAD - leg.zRot) * w;
    }

    void legX(boolean right, float x, float w) {
        if (w <= 0.0F) {
            return;
        }
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        leg.xRot += (x * RAD - leg.xRot) * w;
    }

    void legAdd(boolean right, float x, float z, float w) {
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        leg.xRot += x * RAD * w;
        leg.zRot += z * RAD * w;
    }

    // ---- head, tremble, twist ----

    /** Adds degrees to the head's angles (y and z mirrored for a left-handed player), times the weight. */
    void head(float x, float y, float z, float w) {
        ModelPart head = model.head;
        head.xRot += x * RAD * w;
        head.yRot += y * side * RAD * w;
        head.zRot += z * side * RAD * w;
    }

    /** A tremble: {@code amp} degrees of {@code sin(speed * age + phase)} on the arm's x and a cosine on its z. */
    void tremble(boolean main, float amp, float speed, float w) {
        if (amp <= 0.0F || w <= 0.0F || EnlightenedClientConfig.reducedMotion()) {
            return;
        }
        float phase = main ? 0.0F : 1.3F;
        ModelPart arm = main ? mArm : oArm;
        arm.xRot += amp * RAD * Mth.sin(speed * age + phase) * w;
        arm.zRot += amp * RAD * Mth.cos(speed * age + phase + 0.4F) * w;
    }

    /** Marks an arm as held by an aimed pose with this weight: the swing of an attack is muted by it. */
    void occupy(boolean main, float w) {
        if (main) {
            occupiedM = Math.max(occupiedM, w);
        } else {
            occupiedO = Math.max(occupiedO, w);
        }
    }

    /** Asks for the shoulders to twist by this many degrees (the weight is applied by the caller). */
    void twist(float degrees, float w) {
        twist += degrees * side * w;
    }
}
