package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.client.EnlightenedClientConfig;
import com.kolodkingal.enlightened.client.anim.Gestures.ArmKey;
import com.kolodkingal.enlightened.client.anim.Gestures.Mode;
import com.kolodkingal.enlightened.mixin.client.ItemInHandRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import static com.kolodkingal.enlightened.client.anim.AnimMath.ss;

/**
 * The hands of the local bender in first person. Vanilla draws only the empty main arm and what the hands hold; a
 * gesture or a stance adds a movement to them here (fists up for the fire fist, the palm under a lifted rock, both
 * hands thrust for an air blast, the clawed hands of bloodbending). The movement is a translation in view space
 * (blocks: +x right, +y up, +z away from the camera) and a rotation (degrees) about the hand's own pivot, summed
 * over every active stance and gesture times its weight, in a right hand's signs; the left hand mirrors
 * {@code dx}, {@code ry} and {@code rz}.
 *
 * <p>A gesture's first-person row is read from its own third-person arm keys: how far the arm is raised becomes how
 * far the hand comes up and forward on the screen, so every gesture of the tables moves the hands without a row of
 * its own. The stances have rows here. The empty off hand, which vanilla never draws, is drawn when a pose really
 * uses it, sliding up from below the screen as vanilla's equip animation does.
 */
@EventBusSubscriber(modid = Enlightened.MODID, value = Dist.CLIENT)
public final class FirstPersonArms {
    private static final float PIVOT_X = 0.40F;
    private static final float PIVOT_Y = -0.45F;
    private static final float PIVOT_Z = -0.72F;
    private static final float MAX_OFFSET = 0.8F;
    private static final float RAD = Mth.DEG_TO_RAD;

    /** What is added to one hand, in a right hand's signs, and how strongly a pose really uses the hand. */
    public static final class Offset {
        public float dx;
        public float dy;
        public float dz;
        public float rx;
        public float ry;
        public float rz;
        float used;

        void clear() {
            dx = dy = dz = rx = ry = rz = used = 0.0F;
        }

        void add(float w, float dx, float dy, float dz, float rx, float ry, float rz) {
            this.dx += w * dx;
            this.dy += w * dy;
            this.dz += w * dz;
            this.rx += w * rx;
            this.ry += w * ry;
            this.rz += w * rz;
            this.used = Math.max(this.used, w);
        }

        void ambient(float w, float dx, float dy, float dz) {
            this.dx += w * dx;
            this.dy += w * dy;
            this.dz += w * dz;
        }

        float length() {
            return Mth.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    private static final Offset RIGHT = new Offset();
    private static final Offset LEFT = new Offset();
    private static final float[] V = new float[3];
    private static final float[] TREMBLE = new float[2];
    private static double computedFor = Double.NaN;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !EnlightenedClientConfig.animations() || !EnlightenedClientConfig.firstPersonHands() || player.isScoping()) {
            return;
        }
        InteractionHand hand = event.getHand();
        ItemStack stack = event.getItemStack();
        if ((player.isUsingItem() && player.getUsedItemHand() == hand) || stack.getItem() instanceof MapItem) {
            return;
        }
        float pt = event.getPartialTick();
        AnimState s = Animator.frame(player, Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot), pt);
        if (s == null || s.wVanilla > 0.0F) {
            return;
        }
        if (computedFor != s.time) {
            computedFor = s.time;
            collect(s, player);
        }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        Offset o = arm == HumanoidArm.RIGHT ? RIGHT : LEFT;
        boolean emptyOffHand = hand == InteractionHand.OFF_HAND && stack.isEmpty();
        boolean drawEmpty = emptyOffHand && o.used > 0.01F && !player.isInvisible();
        if (o.length() < 1.0E-4F && Math.abs(o.rx) + Math.abs(o.ry) + Math.abs(o.rz) < 1.0E-3F && !drawEmpty) {
            return;
        }
        if (emptyOffHand && !drawEmpty) {
            return;
        }
        float swing = event.getSwingProgress();
        if (forced(s)) {
            swing = 0.0F;
        }
        event.setCanceled(true);
        PoseStack ps = event.getPoseStack();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float length = o.length();
        float k = length > MAX_OFFSET ? MAX_OFFSET / length : 1.0F;
        ps.pushPose();
        ps.translate(o.dx * side * k, o.dy * k, -o.dz * k);
        ps.translate(PIVOT_X * side, PIVOT_Y, PIVOT_Z);
        ps.mulPose(Axis.ZP.rotationDegrees(o.rz * side));
        ps.mulPose(Axis.YP.rotationDegrees(o.ry * side));
        ps.mulPose(Axis.XP.rotationDegrees(o.rx));
        ps.translate(-PIVOT_X * side, -PIVOT_Y, -PIVOT_Z);
        ItemInHandRendererAccessor renderer = (ItemInHandRendererAccessor) mc.gameRenderer.itemInHandRenderer;
        if (drawEmpty) {
            renderer.enlightened$renderPlayerArm(ps, event.getMultiBufferSource(), event.getPackedLight(), 1.0F - Math.min(1.0F, o.used), 0.0F, arm);
        } else {
            renderer.enlightened$renderArmWithItem(player, pt, event.getInterpolatedPitch(), hand, swing, stack, event.getEquipProgress(), ps,
                    event.getMultiBufferSource(), event.getPackedLight());
        }
        ps.popPose();
        s.fpAge = s.time;
    }

    /** Whether a forcing gesture (a punch of the element) plays strongly: the vanilla swing then stays out of its way. */
    private static boolean forced(AnimState s) {
        for (AnimState.Slot slot : s.slots) {
            if (slot.active && slot.id.force && slot.weight > 0.5F) {
                return true;
            }
        }
        return false;
    }

    /** The movement of the hand of this arm now, in the signs of a right hand, for the effects that leave it. */
    public static void hand(LocalPlayer player, HumanoidArm arm, float[] out) {
        java.util.Arrays.fill(out, 0.0F);
        AnimState s = Animator.peek(player);
        if (s == null || s.wVanilla > 0.0F || Math.abs(s.time - s.fpAge) > 2.0 || !EnlightenedClientConfig.animations()) {
            return;
        }
        Offset o = arm == HumanoidArm.RIGHT ? RIGHT : LEFT;
        float length = o.length();
        float k = length > MAX_OFFSET ? MAX_OFFSET / length : 1.0F;
        out[0] = o.dx * k;
        out[1] = o.dy * k;
        out[2] = o.dz * k;
    }

    // ====================================================================================================================
    // The rows
    // ====================================================================================================================

    private static void collect(AnimState s, LocalPlayer player) {
        RIGHT.clear();
        LEFT.clear();
        float age = (float) s.time;
        boolean calm = EnlightenedClientConfig.reducedMotion();
        HumanoidArm mArm = player.getMainArm();
        Offset m = mArm == HumanoidArm.RIGHT ? RIGHT : LEFT;
        Offset o = mArm == HumanoidArm.RIGHT ? LEFT : RIGHT;
        float w;

        // ---- stances ----
        if ((w = s.stance(Stance.FIRE_FIST)) > 0.001F) {
            float cycle = 0.55F * age;
            float punchM = Math.max(0.0F, Mth.sin(cycle));
            float punchO = Math.max(0.0F, -Mth.sin(cycle));
            m.add(w, -0.12F, 0.10F + 0.04F * punchM, 0.16F + 0.30F * punchM * punchM, -22 - 10 * punchM, 4, 0);
            o.add(w, -0.12F, 0.10F + 0.04F * punchO, 0.16F + 0.30F * punchO * punchO, -22 - 10 * punchO, 4, 0);
        }
        if ((w = s.stance(Stance.FIRE_BREATH)) > 0.001F) {
            both(w, 0.10F, -0.06F, -0.10F, 10, -10, 0);
        }
        if ((w = s.stance(Stance.FIRE_WHIP)) > 0.001F) {
            float lash = Mth.sin(0.9F * age);
            m.add(w, -0.16F + 0.06F * lash, 0.26F, 0.22F, -40 + 8 * lash, 6, 10 * Mth.sin(1.8F * age));
            tremble(m, null, w, 0.006F, 1.9F, age, calm);
        }
        if ((w = s.stance(Stance.FIRE_BALL_CHARGE)) > 0.001F) {
            float gather = ss(0.0F, 20.0F, age - s.activeSinceAge());
            m.add(w, 0.08F, -0.08F - 0.04F * gather, -0.14F, 12, -6, 0);
            o.add(w, -0.10F, 0.16F, 0.30F, -36, 6, 0);
            tremble(m, null, w, 0.004F + 0.010F * gather, 2.2F, age, calm);
        }
        if ((w = s.stance(Stance.LIGHTNING_CHARGE)) > 0.001F) {
            float held = age - s.activeSinceAge();
            float a = 0.5F * held;
            float build = ss(0.0F, 30.0F, held);
            float cx = 0.14F * (1.0F - 0.6F * build);
            m.add(w, -0.14F + cx * Mth.cos(a), 0.14F + cx * Mth.sin(a), 0.18F + 0.22F * build, -36 - 10 * build, 6, 0);
            o.add(w, -0.14F - cx * Mth.cos(a), 0.14F - cx * Mth.sin(a), 0.18F + 0.06F * build, -36, 6, 0);
            tremble(m, o, w, 0.006F + 0.010F * build, 2.4F, age, calm);
        }
        if ((w = s.stance(Stance.REDIRECT)) > 0.001F) {
            m.add(w, -0.10F, 0.14F, 0.30F, -30, 8, 0);
            o.add(w, 0.06F, -0.10F, -0.06F, 10, 0, 0);
        }
        if ((w = s.stance(Stance.ROCK_LIFT)) > 0.001F) {
            float bob = 0.02F * Mth.sin(0.12F * age);
            m.add(w, -0.18F, 0.22F + bob, 0.30F, -44, 8, -20);
            tremble(m, null, w, 0.005F, 1.5F, age, calm);
        }
        if ((w = s.stance(Stance.BLOODBENDING)) > 0.001F) {
            both(w, -0.10F, 0.26F, 0.34F, -50, 10, 0);
            tremble(m, o, w, 0.012F, 2.1F, age, calm);
        }
        if ((w = s.stance(Stance.SUFFOCATE)) > 0.001F) {
            m.add(w, -0.12F, 0.18F, 0.36F, -34, 6, 0);
            tremble(m, null, w, 0.006F, 2.0F, age, calm);
        }
        if ((w = s.stance(Stance.AIR_SHIELD)) > 0.001F) {
            float circle = 0.03F * Mth.sin(0.12F * age);
            both(w, 0.08F, 0.02F + circle, 0.06F, -8, -14, 0);
        }
        if ((w = s.stance(Stance.WATER_ARMS)) > 0.001F) {
            both(w, -0.06F, 0.16F, 0.10F, -30, 4, 0);
        }
        if ((w = s.stance(Stance.OCTOPUS)) > 0.001F) {
            float wave = 0.03F * Mth.sin(0.22F * age);
            both(w, 0.12F, 0.04F + wave, 0.02F, -10, -20, 0);
        }
        if ((w = s.stance(Stance.SPOUT)) > 0.001F) {
            both(w, 0.10F, -0.04F + 0.02F * Mth.sin(0.2F * age), 0.0F, 0, -12, 0);
        }
        if ((w = s.stance(Stance.SCOOTER)) > 0.001F) {
            m.add(w, 0.04F, -0.14F, 0.0F, 14, 0, 0);
        }
        if ((w = s.stance(Stance.GLIDE)) > 0.001F) {
            both(w, -0.06F, 0.40F, 0.10F, -70, 4, 0);
        }

        // ---- gestures, from their own arm keys ----
        for (AnimState.Slot slot : s.slots) {
            if (slot.active && slot.weight > 0.001F) {
                gesture(slot, m, o);
            }
        }

        // ---- ambient motion: the wind of flight, the breathing of a hover ----
        if (s.wAir > 0.001F && !calm) {
            float push = -0.10F * ss(1.0F, 2.0F, s.s) * s.wAir;
            RIGHT.ambient(1.0F, 0.0F, push, push);
            LEFT.ambient(1.0F, 0.0F, push, push);
            float breathe = 0.012F * Mth.sin(0.07F * age) * s.wAir * (1.0F - ss(0.2F, 0.8F, s.sH));
            RIGHT.ambient(1.0F, 0.0F, breathe, 0.0F);
            LEFT.ambient(1.0F, 0.0F, breathe, 0.0F);
        }
        if (s.wGlide > 0.001F && !calm) {
            float wind = 0.01F * Mth.sin(0.3F * age) * s.wGlide;
            RIGHT.ambient(1.0F, 0.0F, wind, 0.0F);
            LEFT.ambient(1.0F, 0.0F, -wind, 0.0F);
        }
    }

    /** The first-person solver: an aimed arm points level forward, a world arm by its elevation. */
    private static final Gestures.ArmSolver SOLVER = (key, main, out) -> {
        switch (key.mode) {
            case AIM -> {
                out[0] = (-90.0F + key.x) * RAD;
                out[1] = key.y * RAD;
                out[2] = key.z * RAD;
            }
            case WORLD -> {
                out[0] = -key.x * RAD;
                out[1] = key.y * RAD;
                out[2] = key.z * RAD;
            }
            default -> {
                out[0] = key.x * RAD;
                out[1] = key.y * RAD;
                out[2] = key.z * RAD;
            }
        }
    };

    /**
     * One gesture's hands, read from its third-person arm keys: the arm's elevation (0 hanging, 90 level, 180 up)
     * becomes how far the hand rises and comes forward on the screen; its swing in and out becomes a slide across.
     */
    private static void gesture(AnimState.Slot slot, Offset m, Offset o) {
        Gestures.Def def = Gestures.get(slot.id);
        float w = slot.weight;
        for (int arm = 0; arm < 2; arm++) {
            boolean main = arm == 0;
            Mode mode = def.sampleArm(main, slot.t, SOLVER, V, TREMBLE);
            if (mode == null || mode == Mode.ADD) {
                continue;
            }
            float x = V[0] / RAD;
            float y = V[1] / RAD;
            float z = V[2] / RAD;
            // Relative to the rest pose, a hand at the side: an arm hanging by the body moves nothing.
            float phi = Mth.clamp(-x, -60.0F, 200.0F);
            float forward = Mth.sin(Mth.clamp(phi, 0.0F, 180.0F) * RAD);
            float up = (1.0F - Mth.cos(Mth.clamp(phi, 0.0F, 180.0F) * RAD)) * 0.5F;
            float back = Math.max(0.0F, -phi) / 60.0F;
            float inward = -y / 90.0F;
            float outward = z / 90.0F;
            float dx = -0.16F * inward + 0.10F * outward;
            float dy = 0.34F * up - 0.04F * forward - 0.10F * back;
            float dz = 0.36F * forward - 0.08F * back - 0.06F * Math.max(0.0F, up - 0.8F) * 5.0F;
            float rx = -42.0F * forward - 24.0F * up + 12.0F * back;
            float ry = 18.0F * inward - 10.0F * outward;
            float rz = -12.0F * outward;
            (main ? m : o).add(w, dx, dy, dz, rx, ry, rz);
            if (TREMBLE[0] > 0.0F && !EnlightenedClientConfig.reducedMotion()) {
                tremble(main ? m : o, null, w, 0.0025F * TREMBLE[0], TREMBLE[1], (float) Animator.peek(Minecraft.getInstance().player).time, false);
            }
        }
    }

    private static void both(float w, float dx, float dy, float dz, float rx, float ry, float rz) {
        RIGHT.add(w, dx, dy, dz, rx, ry, rz);
        LEFT.add(w, dx, dy, dz, rx, ry, rz);
    }

    private static void tremble(Offset m, Offset o, float w, float amp, float speed, float age, boolean calm) {
        if (calm) {
            return;
        }
        m.ambient(w, amp * Mth.sin(speed * age), amp * Mth.cos(speed * age + 0.4F), 0.0F);
        if (o != null) {
            o.ambient(w, amp * Mth.sin(speed * age + 1.3F), amp * Mth.cos(speed * age + 1.7F), 0.0F);
        }
    }

    private FirstPersonArms() {
    }
}
