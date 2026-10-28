package com.kolodkingal.enlightened.client.fx;

import com.kolodkingal.enlightened.air.AirBending;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Variants;
import com.kolodkingal.enlightened.client.EnlightenedClientConfig;
import com.kolodkingal.enlightened.client.anim.AnimState;
import com.kolodkingal.enlightened.client.anim.Animator;
import com.kolodkingal.enlightened.client.anim.HandTracker;
import com.kolodkingal.enlightened.client.anim.Stance;
import com.kolodkingal.enlightened.item.GliderItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enlightened.client.anim.AnimMath.ss;
import static com.kolodkingal.enlightened.client.fx.FxGeometry.FULL;

/**
 * Draws the element effects of bending in the world, after the particles: the one-shot effects the server sent
 * ({@link WorldFx}: a lash of water, a wave of flame, a bolt of lightning, a gust, a pulse through the ground) and
 * the sustained ones every client makes from the synced data (the water arms and the octopus's tentacles made of
 * water, the spout's column, the fire fist's streams and the fire whip made of fire, the lightning gathering between
 * the hands, the strands of bloodbending, the air ball of the scooter, the glider's wings, the wind of flight, the
 * air shield, the seismic sense's pulses). Everything is real geometry with the game's element textures, so water is
 * water and fire is fire.
 */
public final class WorldFxRenderer {
    /** The show's water: a translucent mid-light blue with a near-white rim; ice pale blue-white; healing silvery-blue. */
    private static final float[] WATER_TINT = {0.44F, 0.70F, 0.92F};
    private static final float[] WATER_GLOW = {0.90F, 0.97F, 1.0F};
    private static final float[] HEAL_GLOW = {0.80F, 0.92F, 1.0F};
    private static final float[] AIR = {0.82F, 0.92F, 1.0F};
    /** The show's fire: a yellow core in an orange body; Azula's a white core in cyan; lightning blue-white. */
    private static final float[] FIRE_CORE = {1.0F, 0.88F, 0.45F};
    private static final float[] BLUE_CORE = {0.88F, 0.96F, 1.0F};
    private static final float[] BOLT = {0.72F, 0.86F, 1.0F};
    private static final float[] BOLT_CORE = {0.96F, 0.98F, 1.0F};
    /** The seven chakras' colours, as the visions of "The Guru": red, orange, yellow, green, blue, indigo, violet. */
    private static final float[][] CHAKRA_RGB = {{0.95F, 0.25F, 0.2F}, {1.0F, 0.6F, 0.15F}, {1.0F, 0.9F, 0.25F}, {0.3F, 0.9F, 0.4F}, {0.3F, 0.55F, 1.0F}, {0.35F, 0.25F, 0.85F}, {0.75F, 0.4F, 1.0F}};
    private static final float[] BLOOD = {0.65F, 0.02F, 0.06F};
    private static final float[] EARTH = {0.76F, 0.66F, 0.46F};
    private static final float[] METAL = {0.86F, 0.88F, 0.95F};
    private static final float[] SAND = {0.88F, 0.80F, 0.56F};

    /** The roll of each rider's air ball (it rolls with the ride), by player id. */
    private static final Map<Integer, Quaternionf> ROLL = new HashMap<>();
    private static final Map<Integer, Double> ROLL_TIME = new HashMap<>();

    private static Vec3 cam;
    private static ClientLevel level;
    private static PoseStack.Pose pose;
    private static MultiBufferSource.BufferSource buffer;
    private static float partial;
    private static long now;
    private static float age;

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        level = mc.level;
        cam = event.getCamera().getPosition();
        partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        now = level.getGameTime();
        age = now + partial;
        buffer = FxGeometry.source();
        pose = event.getPoseStack().last();
        for (WorldFx.Effect e : WorldFx.all()) {
            timed(e);
        }
        for (AbstractClientPlayer player : level.players()) {
            BendingData data = Benders.get(player);
            if (data.element().bends() && !player.isInvisible() && player.position().distanceToSqr(cam) < 96.0 * 96.0) {
                sustained(player, data);
            }
        }
        buffer.endBatch();
    }

    static void clear() {
        WHIP_TRAIL.clear();
        ROLL.clear();
        ROLL_TIME.clear();
    }

    // ---- helpers ----

    private static Vec3 rel(Vec3 world) {
        return world.subtract(cam);
    }

    private static int lightAt(Vec3 world) {
        return LevelRenderer.getLightColor(level, BlockPos.containing(world));
    }

    private static VertexConsumer vc(net.minecraft.client.renderer.RenderType type) {
        VertexConsumer base = buffer.getBuffer(type);
        return FxGeometry.rainbow(type) ? FxGeometry.rainbow(base, cam.x, cam.y, cam.z, age) : base;
    }

    /** The heart of fire of a colour (the rainbow's turns through the hues). */
    private static float[] fireCore(int colour) {
        if (colour == FxGeometry.FIRE_RAINBOW) {
            float[] c = FxGeometry.hue(age * 0.02F);
            return new float[]{0.6F + 0.4F * c[0], 0.6F + 0.4F * c[1], 0.6F + 0.4F * c[2]};
        }
        return colour == FxGeometry.FIRE_BLUE ? BLUE_CORE : FIRE_CORE;
    }

    private static float[] waterFrame() {
        return FxGeometry.frame(age, FxGeometry.WATER_FRAMES, 0.5F);
    }

    private static float[] fireFrame(float speed) {
        return FxGeometry.frame(age, FxGeometry.FIRE_FRAMES, speed);
    }

    private static Vec3 look(AbstractClientPlayer p) {
        return p.getViewVector(partial);
    }

    /**
     * The look turned {@code radians} to the side (positive to the right): a stream from a hand goes out a little
     * away from the middle, so from the bender's own camera behind him (which looks along his look) it shows beside
     * his body instead of collapsing behind it.
     */
    private static Vec3 fan(Vec3 look, double radians) {
        Vec3 side = right(look);
        return look.scale(Math.cos(radians)).add(side.scale(Math.sin(radians))).normalize();
    }

    private static Vec3 right(Vec3 look) {
        Vec3 r = look.cross(new Vec3(0, 1, 0));
        return r.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : r.normalize();
    }

    private static Vec3 hand(AbstractClientPlayer p, HumanoidArm arm) {
        return HandTracker.handOrGuess(p, arm, partial);
    }

    private static float stance(AnimState st, BendingData data, Stance s, boolean on) {
        return st != null ? st.stance(s) : (on ? 1.0F : 0.0F);
    }

    private static float easeOut(float f) {
        float v = 1.0F - Mth.clamp(f, 0.0F, 1.0F);
        return 1.0F - v * v * v;
    }

    private static float env(float f) {
        return Mth.sin(Mth.clamp(f, 0.0F, 1.0F) * Mth.PI);
    }

    /**
     * The lion turtle's gift, as it gave Wan his bending: two great claws, tan banded with black, reach out from its
     * raised flipper and touch the receiver's forehead and chest; light blooms there, broken rings of it circling the
     * two points and long rays crossing through them; then the claws draw back and the light fades.
     */
    private static void lionGift(WorldFx.Effect e, float f, float a, Vec3 from, Vec3 to, Entity target) {
        Entity turtle = level.getEntity((int) e.data());
        int scale = turtle instanceof com.kolodkingal.enlightened.entity.LionTurtleEntity t ? t.scale() : 1;
        Vec3 root = turtle instanceof com.kolodkingal.enlightened.entity.LionTurtleEntity t && target != null ? rel(t.clawRoot(target)) : to;
        Vec3[] touch;
        if (target != null) {
            Vec3[] w = com.kolodkingal.enlightened.world.LionTurtle.touchPoints(target, partial);
            touch = new Vec3[]{rel(w[0]), rel(w[1])};
        } else {
            touch = new Vec3[]{from.add(0, 1.6, 0), from.add(0, 1.05, 0)};
        }
        float reach = f < 0.2F ? smooth(f / 0.2F) : f > 0.85F ? smooth((1.0F - f) / 0.15F) : 1.0F;
        float glow = f < 0.18F ? 0.0F : f < 0.3F ? smooth((f - 0.18F) / 0.12F) : f > 0.82F ? smooth(Math.max(0.0F, 1.0F - (f - 0.82F) / 0.12F)) : 1.0F;
        // The claws.
        VertexConsumer horn = vc(FxGeometry.snow());
        for (int c = 0; c < 2; c++) {
            Vec3 tip = touch[c];
            Vec3 start = root.add(0, c == 0 ? 0.35 * scale : -0.35 * scale, 0);
            double dist = tip.distanceTo(start);
            Vec3 mid = start.add(tip).scale(0.5).add(0, dist * 0.22, 0);
            List<Vec3> pts = new ArrayList<>();
            int n = 24;
            for (int k = 0; k <= n; k++) {
                float t = reach * k / n;
                pts.add(bezier(start, mid, tip, t));
            }
            float r0 = (float) Math.min(0.32 * scale + 0.2, 0.05 * dist + 0.25);
            float r1 = 0.04F + (1.0F - reach) * r0 * 0.4F;
            FxGeometry.tube(pose, horn, pts, r0, r1, 10, 0, 1, 1, 0, 0.86F, 0.78F, 0.52F, 1.0F, 1.0F, FULL, true);
            // Black bands across it, thinning toward the point.
            for (int b = 1; b < 9; b++) {
                float t0 = reach * (b / 9.0F);
                float t1 = reach * ((b + 0.28F) / 9.0F);
                float rr = Mth.lerp(b / 9.0F, r0, r1) * 1.06F;
                List<Vec3> band = List.of(bezier(start, mid, tip, t0), bezier(start, mid, tip, t1));
                FxGeometry.tube(pose, horn, band, rr, rr * 0.97F, 10, 0, 1, 1, 0, 0.12F, 0.1F, 0.08F, 1.0F, 1.0F, FULL, true);
            }
        }
        if (glow <= 0.0F) {
            return;
        }
        VertexConsumer light = vc(FxGeometry.light());
        float pulse = 1.0F + 0.08F * Mth.sin(a * 0.4F);
        for (int c = 0; c < 2; c++) {
            Vec3 p = touch[c];
            float big = c == 0 ? 0.75F : 1.5F;
            FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), p, (c == 0 ? 1.4F : 2.0F) * pulse * glow, 1.0F, 0.98F, 0.85F, 0.95F * glow, a * 0.1F);
            // Broken rings round the point, turning.
            for (int ring = 0; ring < 2; ring++) {
                float radius = big * (1.0F + ring * 0.4F) * pulse * (0.6F + 0.4F * glow);
                float spin = a * (ring == 0 ? 0.05F : -0.04F) + c;
                arc(p, radius, spin, spin + 5.2F, 0.07F + 0.03F * ring, 1.0F, 0.97F, 0.82F, 0.85F * glow, light);
            }
        }
        // Rays crossing through the chest and the forehead, long and thin.
        float[][] rays = {{0.62F, 9.0F}, {2.5F, 9.0F}, {1.35F, 6.0F}, {-0.2F, 5.0F}};
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < rays.length; i++) {
                if (c == 0 && i > 1) {
                    break;
                }
                float ang = rays[i][0] + (c == 0 ? 0.3F : 0.0F);
                float len = rays[i][1] * (c == 0 ? 0.5F : 1.0F) * (0.4F + 0.6F * glow);
                ray(touch[c], ang, len, 0.12F, 1.0F, 0.98F, 0.88F, 0.9F * glow, light);
            }
        }
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, float t) {
        float u = 1.0F - t;
        return a.scale(u * u).add(b.scale(2 * u * t)).add(c.scale(t * t));
    }

    /** The two axes of the plane facing the camera through {@code p} (the camera is the origin). */
    private static Vec3[] facing(Vec3 p) {
        Vec3 n = p.scale(-1.0).normalize();
        Vec3 up = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = n.cross(up).normalize();
        Vec3 v = u.cross(n).normalize();
        return new Vec3[]{u, v};
    }

    /** An arc of light facing the camera round {@code p}, from angle {@code a0} to {@code a1}. */
    private static void arc(Vec3 p, float radius, float a0, float a1, float width, float r, float g, float b, float alpha, VertexConsumer vc) {
        Vec3[] ax = facing(p);
        List<Vec3> pts = new ArrayList<>();
        int n = 36;
        for (int k = 0; k <= n; k++) {
            double t = a0 + (a1 - a0) * k / n;
            pts.add(p.add(ax[0].scale(Math.cos(t) * radius)).add(ax[1].scale(Math.sin(t) * radius)));
        }
        FxGeometry.ribbon(pose, vc, pts, width, width, 0, 1, 1, 0, r, g, b, alpha * 0.4F, alpha, FULL, false);
    }

    /** A ray of light through {@code p} at {@code angle} in the plane facing the camera, {@code len} each way. */
    private static void ray(Vec3 p, float angle, float len, float width, float r, float g, float b, float alpha, VertexConsumer vc) {
        Vec3[] ax = facing(p);
        Vec3 d = ax[0].scale(Math.cos(angle)).add(ax[1].scale(Math.sin(angle)));
        for (int side = -1; side <= 1; side += 2) {
            List<Vec3> pts = List.of(p, p.add(d.scale(side * len * 0.5)), p.add(d.scale(side * len)));
            FxGeometry.ribbon(pose, vc, pts, width, 0.0F, 0, 1, 1, 0, r, g, b, alpha, 0.0F, FULL, false);
        }
    }

    // ====================================================================================================================
    // The sustained effects of the stances
    // ====================================================================================================================

    private static void sustained(AbstractClientPlayer p, BendingData data) {
        AnimState st = Animator.peek(p);
        boolean calm = EnlightenedClientConfig.reducedMotion();
        float w;
        if ((w = stance(st, data, Stance.SCOOTER, data.has(Ability.AIR_SCOOTER) && data.on(Ability.AIR_SCOOTER))) > 0.01F) {
            airBall(p, w);
        }
        if ((w = stance(st, data, Stance.GLIDE, data.gliderOpen())) > 0.01F) {
            glider(p, w);
        }
        if (st != null && st.wAir > 0.2F && st.sH > 0.5F && !calm) {
            wind(p, st);
        }
        if (AirBending.shieldUp(p, data)) {
            airShield(p, data);
        }
        if ((w = stance(st, data, Stance.WATER_ARMS, data.has(Ability.WATER_ARMS) && data.on(Ability.WATER_ARMS))) > 0.01F) {
            waterArms(p, data, w);
        }
        if ((w = stance(st, data, Stance.OCTOPUS, data.has(Ability.OCTOPUS_FORM) && data.on(Ability.OCTOPUS_FORM))) > 0.01F) {
            octopus(p, w);
            if (com.kolodkingal.enlightened.water.WaterBending.octopusBody(data)) {
                octopusBody(p, w);
            }
        }
        if (data.has(Ability.WATER_SPOUT) && data.on(Ability.WATER_SPOUT)) {
            spout(p, data, 1.0F);
        }
        if (data.has(Ability.WATER_VORTEX) && data.active(Ability.WATER_VORTEX)) {
            spout(p, data, 1.5F);
        }
        if ((w = stance(st, data, Stance.BLOODBENDING, data.active(Ability.BLOODBENDING))) > 0.01F) {
            bloodStrands(p, data, w);
        }
        if ((w = stance(st, data, Stance.SUFFOCATE, data.active(Ability.SUFFOCATE))) > 0.01F) {
            suffocation(p, data, w);
        }
        float fist = stance(st, data, Stance.FIRE_FIST, data.active(Ability.FIRE_FIST));
        float breath = stance(st, data, Stance.FIRE_BREATH, false);
        if (fist > 0.01F || breath > 0.01F) {
            fireStreams(p, data, fist, breath);
        }
        if ((w = stance(st, data, Stance.FIRE_WHIP, data.active(Ability.FIRE_WHIP))) > 0.01F) {
            fireWhip(p, data, w);
        }
        if ((w = stance(st, data, Stance.LIGHTNING_CHARGE, data.active(Ability.LIGHTNING))) > 0.01F) {
            lightningCharge(p, data, st, w);
        }
        if ((w = stance(st, data, Stance.REDIRECT, data.has(Ability.LIGHTNING_REDIRECT) && data.on(Ability.LIGHTNING_REDIRECT))) > 0.3F) {
            redirectCrackle(p, w);
        }
        if (data.has(Ability.SEISMIC_SENSE) && data.on(Ability.SEISMIC_SENSE) && p.onGround()) {
            seismicPulse(p);
        }
        if ((w = stance(st, data, Stance.WATER_SHIELD, data.active(Ability.WATER_SHIELD))) > 0.01F) {
            waterDome(p, data, w);
        }
        if ((w = stance(st, data, Stance.FIRE_SHIELD, data.active(Ability.FIRE_SHIELD))) > 0.01F) {
            fireDome(p, data, w);
        }
        if ((w = stance(st, data, Stance.FIRE_JET, data.active(Ability.FIRE_JET))) > 0.01F) {
            jetFlames(p, data, w);
        }
        if ((w = stance(st, data, Stance.FROST_BREATH, data.active(Ability.FROST_BREATH))) > 0.01F) {
            frostBreath(p, data, w);
        }
        if (data.has(Ability.AIR_SPOUT) && data.on(Ability.AIR_SPOUT)) {
            airSpout(p);
        }
        if ((w = stance(st, data, Stance.EARTH_TUNNEL, data.active(Ability.EARTH_TUNNEL))) > 0.01F) {
            tunnelDust(p, w);
        }
        if (data.has(Ability.WATER_BUBBLE) && data.on(Ability.WATER_BUBBLE) && p.isUnderWater()) {
            bubble(p, data);
        }
        if (data.has(Ability.HEAT_CONTROL) && data.on(Ability.HEAT_CONTROL) && !calm) {
            heatShimmer(p);
        }
        if (data.has(Ability.PALM_FORGE) && data.active(Ability.PALM_FORGE)) {
            handFlame(p, data);
        }
        if (data.has(Ability.ILLUMINATION) && data.on(Ability.ILLUMINATION)) {
            handFlame(p, data);
        }
        if ((w = stance(st, data, Stance.COMBUSTION, data.active(Ability.COMBUSTION))) > 0.01F) {
            thirdEye(p, w);
        }
        if ((w = stance(st, data, Stance.MEDITATE, data.meditating())) > 0.01F) {
            meditation(p, data, w);
        }
        if ((w = stance(st, data, Stance.AVATAR, data.avatar(now))) > 0.01F) {
            avatarAura(p, data, w);
        }
        if (data.has(Ability.ELEMENTAL_SPHERE) && data.on(Ability.ELEMENTAL_SPHERE) && data.avatar(now)) {
            elementalSphere(p, w);
        }
    }

    /** The water shield: a dome of water over the bender, rolling. */
    private static void waterDome(AbstractClientPlayer p, BendingData data, float w) {
        float radius = (1.9F + 0.4F * (data.level(Ability.WATER_SHIELD) - 1)) * (0.6F + 0.4F * w);
        Vec3 c = rel(p.getPosition(partial).add(0, 1.0, 0));
        float[] fr = waterFrame();
        int lit = lightAt(p.position());
        Quaternionf rot = new Quaternionf().rotationY(age * 0.05F);
        FxGeometry.sphere(pose, vc(FxGeometry.water()), c, radius, 10, 20, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.55F * w, 1.5F, age * 0.2F, lit, true, rot);
        FxGeometry.sphere(pose, vc(FxGeometry.light()), c, radius * 1.01F, 8, 16, 0, 1, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.10F * w, 2.0F, -age * 0.3F, FULL, false,
                new Quaternionf().rotationY(-age * 0.08F));
    }

    /** The fire shield: a dome of fire round the bender, burning. */
    private static void fireDome(AbstractClientPlayer p, BendingData data, float w) {
        int blue = Variants.fireColour(data);
        float radius = (1.9F + 0.4F * (data.level(Ability.FIRE_SHIELD) - 1)) * (0.6F + 0.4F * w);
        Vec3 c = rel(p.getPosition(partial).add(0, 1.0, 0));
        float[] fr = fireFrame(1.0F);
        float[] core = fireCore(blue);
        FxGeometry.sphere(pose, vc(FxGeometry.fire(blue)), c, radius, 10, 20, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.75F * w, 1.3F, age * 0.5F, FULL, true,
                new Quaternionf().rotationY(age * 0.07F));
        FxGeometry.sphere(pose, vc(FxGeometry.light()), c, radius * 0.96F, 8, 16, 0, 1, core[0], core[1], core[2], 0.12F * w, 2.0F, -age * 0.4F, FULL, false,
                new Quaternionf().rotationX(0.5F).mul(new Quaternionf().rotationY(-age * 0.1F)));
        // The show's shield is a spinning pinwheel: streaks of the core colour wind round the sphere with the spin.
        VertexConsumer light = vc(FxGeometry.light());
        for (int i = 0; i < 5; i++) {
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= 16; k++) {
                float t = k / 16.0F;
                double lat = (t - 0.5) * Math.PI * 0.9;
                double lon = age * 0.35 + i * Math.PI * 0.4 + t * 2.4;
                pts.add(c.add(Math.cos(lat) * Math.cos(lon) * radius * 1.02, Math.sin(lat) * radius * 1.02, Math.cos(lat) * Math.sin(lon) * radius * 1.02));
            }
            FxGeometry.ribbon(pose, light, pts, 0.05F, 0.22F, 0, 1, 1, 0, core[0], core[1], core[2], 0.15F * w, 0.6F * w, FULL, false);
        }
    }

    /** The fire jet: flames roaring from the feet against the flight. */
    private static void jetFlames(AbstractClientPlayer p, BendingData data, float w) {
        int blue = Variants.fireColour(data);
        Vec3 v = new Vec3(p.getX() - p.xOld, p.getY() - p.yOld, p.getZ() - p.zOld);
        Vec3 back = v.lengthSqr() < 1.0E-4 ? look(p).scale(-1.0) : v.normalize().scale(-1.0);
        float[] fr = fireFrame(1.0F);
        float[] core = fireCore(blue);
        for (HumanoidArm side : HumanoidArm.values()) {
            Vec3 foot = HandTracker.foot(p, side, partial);
            if (foot == null) {
                foot = p.getPosition(partial).add(side == HumanoidArm.RIGHT ? 0.15 : -0.15, 0.1, 0);
            }
            Vec3 f = rel(foot);
            FxGeometry.cone(pose, vc(FxGeometry.fire(blue)), f, back, 2.6F * w, 0.16F, 0.5F, 8, 6, fr[0], fr[1], 2.0F, age * 0.3F, 1.0F, 1.0F, 1.0F, 0.95F, 0.0F, FULL, true, 0.2F, age * 0.6F + side.ordinal());
            FxGeometry.cone(pose, vc(FxGeometry.light()), f, back, 1.8F * w, 0.08F, 0.25F, 6, 4, 0, 1, 1, 0, core[0], core[1], core[2], 0.7F, 0.0F, FULL, false, 0.1F, age * 0.6F);
        }
    }

    /** Frost breath: a cone of icy mist from the mouth. */
    private static void frostBreath(AbstractClientPlayer p, BendingData data, float w) {
        Vec3 look = look(p);
        Vec3 mouth = rel(p.getEyePosition(partial).add(look.scale(0.3)).add(0, -0.12, 0));
        float len = (5.0F + 2.0F * (data.level(Ability.FROST_BREATH) - 1)) * w;
        VertexConsumer light = vc(FxGeometry.light());
        FxGeometry.cone(pose, light, mouth, look, len, 0.08F, 1.0F, 10, 8, 0, 1, 1, 0, 0.85F, 0.95F, 1.0F, 0.45F, 0.0F, FULL, false, 0.25F, age * 0.4F);
        FxGeometry.cone(pose, light, mouth, look, len * 0.7F, 0.04F, 0.5F, 8, 6, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.35F, 0.0F, FULL, false, 0.15F, age * 0.4F + 1);
        FxGeometry.conePuffs(pose, vc(FxGeometry.snow()), mouth, look, len, 0.08F, 1.0F, 10, 0, 1, 0.85F, 0.95F, 1.0F, 0.55F, 0.0F, FULL, true, 0.25F, age * 0.4F, 1.2F, age * 0.2F);
    }

    /** The air spout: a column of spinning wind from the ground to the feet. */
    private static void airSpout(AbstractClientPlayer p) {
        Vec3 feet = p.getPosition(partial);
        double base = com.kolodkingal.enlightened.bending.Spouts.base(level, p, 20, false);
        if (Double.isNaN(base) || feet.y - base < 0.3) {
            return;
        }
        double bottom = base;
        VertexConsumer light = vc(FxGeometry.light());
        List<Vec3> pts = new ArrayList<>();
        int n = Math.max(4, (int) ((feet.y - bottom) * 2));
        for (int k = 0; k <= n; k++) {
            float t = (float) k / n;
            double a = age * 0.3 + t * 6;
            pts.add(rel(new Vec3(feet.x + Math.cos(a) * 0.15 * (1 - t), bottom + (feet.y - bottom) * t, feet.z + Math.sin(a) * 0.15 * (1 - t))));
        }
        // The twisting column, wide at the ground and narrowing to the feet, a paler core inside it.
        FxGeometry.tube(pose, light, pts, 1.1F, 0.3F, 12, 0, 1, 1, 0, AIR[0], AIR[1], AIR[2], 0.20F, 0.28F, FULL, false);
        FxGeometry.tube(pose, light, pts, 0.45F, 0.15F, 8, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.18F, 0.25F, FULL, false);
        for (int i = 0; i < 3; i++) {
            List<Vec3> rope = new ArrayList<>();
            for (int k = 0; k <= n * 2; k++) {
                float t = (float) k / (n * 2);
                double y = bottom + (feet.y - bottom) * t;
                double a = i * 2.09 - age * 0.45 + t * 10;
                double r = 1.05 - 0.75 * t;
                rope.add(rel(new Vec3(feet.x + Math.cos(a) * r, y, feet.z + Math.sin(a) * r)));
            }
            FxGeometry.ribbon(pose, light, rope, 0.12F, 0.04F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.35F, 0.5F, FULL, false);
        }
        spoutWhirl(feet, bottom, light, false, 0, 1, 1.0F, 1.0F, 1.0F, 0.55F, FULL, 1.0F);
        spoutWhirl(feet, bottom, light, false, 0, 1, AIR[0], AIR[1], AIR[2], 0.25F, FULL, 2.2F);
        // The dust and cloud it stirs at the ground.
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(now / 3);
        VertexConsumer snow = vc(FxGeometry.snow());
        for (int i = 0; i < 8; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2 + age * 0.1;
            double r = 0.8 + rnd.nextDouble() * 1.8;
            FxGeometry.billboard(pose, snow, rel(new Vec3(feet.x + Math.cos(ang) * r, bottom + 0.2 + rnd.nextDouble() * 0.6, feet.z + Math.sin(ang) * r)),
                    0.4F + rnd.nextFloat() * 0.5F, 0, 1, 0.95F, 0.97F, 1.0F, 0.35F, FULL, true, rnd.nextFloat() * 6);
        }
        FxGeometry.disc(pose, light, rel(new Vec3(feet.x, bottom + 0.05, feet.z)), 0.6F, 2.4F, 24, 0, 1, AIR[0], AIR[1], AIR[2], 0.25F, 0.0F, FULL, false, age * 0.4F);
    }

    /**
     * What every spout of the show carries, after the image the player sent: a wide whirl spiralling out over the
     * ground at its foot, broad streamers sweeping round the column on its way up, and tilted comet rings circling the
     * rider at his shins, his waist and his head, each turning its own way. Drawn in the element's colour and
     * texture; {@code width} scales every band (a thin bright pass over a broad soft one gives the arcs a rim).
     */
    private static void spoutWhirl(Vec3 feet, double bottom, VertexConsumer vc, boolean textured, float v0, float v1,
                                   float r, float g, float b, float a, int lit, float width) {
        float height = (float) (feet.y - bottom);
        float spin = age * 0.22F;
        // The whirl at the foot: three arms spiralling outward over the surface.
        for (int i = 0; i < 3; i++) {
            List<Vec3> arm = new ArrayList<>();
            for (int k = 0; k <= 16; k++) {
                float t = k / 16.0F;
                double ang = i * 2.094 + spin * 1.3 + t * 3.6;
                double rad = 0.5 + t * 2.4;
                arm.add(rel(new Vec3(feet.x + Math.cos(ang) * rad, bottom + 0.1 + t * 0.3, feet.z + Math.sin(ang) * rad)));
            }
            FxGeometry.ribbon(pose, vc, arm, 0.22F * width, 0.05F * width, v0, v1, 1, spin * 0.1F, r, g, b, a, 0.0F, lit, textured);
        }
        // Streamers sweeping wide round the column on its way up, every other one the other way.
        int bands = Math.max(1, Math.round(height / 2.5F));
        for (int j = 0; j < bands; j++) {
            double y = bottom + height * (j + 0.6) / (bands + 0.6);
            double radius = 1.3 + 0.6 * ((j + 1) % 2) + 0.15 * Math.sin(spin + j);
            arc(vc, feet.x, y, feet.z, radius, 0.35, (j % 2 == 0 ? spin * 1.5 : -spin * 1.2) + j * 1.7, j % 2 == 0, 4.0, 0.16F * width, textured, v0, v1, r, g, b, a * 0.9F, lit);
        }
        // The rings round the rider: tilted comets at the shins, the waist and the head.
        double[] hs = {0.15, 0.95, 1.8};
        double[] rs = {0.95, 1.25, 0.85};
        for (int j = 0; j < 3; j++) {
            boolean ccw = j != 1;
            double start = (ccw ? spin * (2.2 - j * 0.3) : -spin * 2.0) + j * 2.1;
            arc(vc, feet.x, feet.y + hs[j], feet.z, rs[j], 0.28, start, ccw, 4.4, 0.13F * width, textured, v0, v1, r, g, b, a, lit);
        }
    }

    /**
     * A comet arc round a vertical axis: the head at angle {@code start}, the tail trailing {@code span} radians
     * behind it and fading out; tilted, so it rises {@code tilt} on one side and dips on the other.
     */
    private static void arc(VertexConsumer vc, double cx, double cy, double cz, double radius, double tilt, double start, boolean ccw, double span,
                            float width, boolean textured, float v0, float v1, float r, float g, float b, float a, int lit) {
        List<Vec3> pts = new ArrayList<>();
        int segs = 20;
        for (int k = 0; k <= segs; k++) {
            double t = (double) k / segs;
            double ang = start + (ccw ? -t : t) * span;
            double y = cy + Math.sin(ang - start * 0.3) * tilt;
            pts.add(rel(new Vec3(cx + Math.cos(ang) * radius, y, cz + Math.sin(ang) * radius)));
        }
        FxGeometry.ribbon(pose, vc, pts, width, width * 0.15F, v0, v1, 1, 0, r, g, b, a, 0.0F, lit, textured);
    }

    /** The tunnel: dust streaming back past the digger. */
    private static void tunnelDust(AbstractClientPlayer p, float w) {
        Vec3 look = look(p);
        Vec3 from = rel(p.getEyePosition(partial).add(look.scale(1.5)).add(0, -0.3, 0));
        VertexConsumer light = vc(FxGeometry.light());
        FxGeometry.cone(pose, light, from, look.scale(-1.0), 2.5F * w, 0.6F, 0.1F, 8, 5, 0, 1, 1, 0, EARTH[0], EARTH[1], EARTH[2], 0.35F, 0.0F, FULL, false, 0.3F, age * 0.7F);
    }

    /** The water bubble: a pocket of air, its skin shimmering, under the water. */
    private static void bubble(AbstractClientPlayer p, BendingData data) {
        float radius = 2.0F + (data.level(Ability.WATER_BUBBLE) - 1) * 1.0F + 0.3F;
        Vec3 c = rel(p.getPosition(partial).add(0, 0.9, 0));
        VertexConsumer light = vc(FxGeometry.light());
        FxGeometry.sphere(pose, light, c, radius, 8, 16, 0, 1, 1.0F, 1.0F, 1.0F, 0.12F, 1.7F, age * 0.1F, FULL, false, new Quaternionf().rotationY(age * 0.02F));
    }

    /** Heat control: a shimmer of heat rising off the bender. */
    private static void heatShimmer(AbstractClientPlayer p) {
        Vec3 c = rel(p.getPosition(partial).add(0, 1.0, 0));
        VertexConsumer light = vc(FxGeometry.light());
        FxGeometry.sphere(pose, light, c, 1.1F + 0.05F * Mth.sin(age * 0.5F), 6, 12, 0, 1, 1.0F, 0.6F, 0.25F, 0.05F, 2.5F, age * 0.9F, FULL, false,
                new Quaternionf().rotationY(age * 0.3F));
    }

    /** Illumination: a flame burning on the open hand. */
    private static void handFlame(AbstractClientPlayer p, BendingData data) {
        int blue = Variants.fireColour(data);
        float[] core = fireCore(blue);
        Vec3 h = rel(hand(p, p.getMainArm()).add(0, 0.1, 0));
        float[] fr = fireFrame(1.0F);
        float flicker = 0.85F + 0.15F * Mth.sin(age * 1.3F);
        FxGeometry.cone(pose, vc(FxGeometry.fire(blue)), h, new Vec3(0, 1, 0), 0.6F * flicker, 0.16F, 0.02F, 8, 4, fr[0], fr[1], 1.0F, age * 0.2F, 1.0F, 1.0F, 1.0F, 0.95F, 0.3F, FULL, true, 0.08F, age * 0.5F);
        FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), h.add(0, 0.2, 0), 1.2F, core[0], core[1], core[2], 0.5F, age * 0.2F);
    }

    /** Combustion gathering: light at the third eye. */
    private static void thirdEye(AbstractClientPlayer p, float w) {
        Vec3 eye = rel(p.getEyePosition(partial).add(look(p).scale(0.25)).add(0, 0.15, 0));
        float pulse = 0.6F + 0.4F * Mth.sin(age * 1.1F);
        FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), eye, (0.4F + 0.4F * pulse) * w, 1.0F, 0.6F, 0.2F, 0.9F * w, age * 0.5F);
        FxGeometry.bolt(pose, vc(FxGeometry.light()), eye, eye.add(look(p).scale(0.6)), now / 2, 0.06F, 0.03F, 1.0F, 0.7F, 0.3F, 0.6F * w, 0);
    }

    /** Meditation: a faint ring of light at the lap and motes rising. */
    private static void meditation(AbstractClientPlayer p, BendingData data, float w) {
        Vec3 c = rel(p.getPosition(partial).add(0, 0.05, 0));
        int color = Benders.get(p).element().color;
        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.08F);
        FxGeometry.disc(pose, vc(FxGeometry.light()), c, 0.7F + 0.1F * pulse, 1.1F + 0.15F * pulse, 24, 0, 1, r, g, b, 0.25F * w, 0.0F, FULL, false, age * 0.05F);
        // The open chakras: a column of small rings up the spine, one per chakra.
        for (int i = 0; i < data.chakras(); i++) {
            Vec3 at = c.add(0, 0.25 + i * 0.22, 0);
            FxGeometry.disc(pose, vc(FxGeometry.light()), at, 0.12F, 0.22F, 12, 0, 1, 1.0F, 0.95F, 0.8F, 0.35F * w, 0.1F * w, FULL, false, age * (0.1F + i * 0.02F));
        }
    }

    /** The Avatar State: the eyes and the tattoos blaze, light streams off the body, the air whirls round it. */
    private static void avatarAura(AbstractClientPlayer p, BendingData data, float w) {
        Vec3 c = rel(p.getPosition(partial).add(0, 1.0, 0));
        VertexConsumer light = vc(FxGeometry.light());
        // Seen from inside (his own eyes), the spheres of light and the glow of his eyes would fill the whole screen:
        // he sees only the streams of light climbing round him.
        boolean ownView = p == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        if (!ownView) {
            Quaternionf rot = new Quaternionf().rotationY(age * 0.12F).mul(new Quaternionf().rotationX(0.3F * Mth.sin(age * 0.07F)));
            FxGeometry.sphere(pose, light, c, 1.6F, 10, 20, 0, 1, 0.75F, 0.9F, 1.0F, 0.08F * w, 1.5F, age * 0.3F, FULL, false, rot);
            FxGeometry.sphere(pose, light, c, 1.2F, 8, 16, 0, 1, 1.0F, 1.0F, 1.0F, 0.05F * w, 2.2F, -age * 0.4F, FULL, false, new Quaternionf().rotationZ(1.0F).mul(new Quaternionf().rotationY(-age * 0.2F)));
            Vec3 eyes = rel(p.getEyePosition(partial).add(look(p).scale(0.15)));
            FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), eyes, 0.6F * w, 0.85F, 0.95F, 1.0F, 0.8F * w, age * 0.3F);
            Vec3 forehead = HandTracker.forehead(p, partial);
            if (forehead != null) {
                FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), rel(forehead), 0.5F * w, 0.85F, 0.95F, 1.0F, 0.8F * w, -age * 0.3F);
            }
        }
        // The light at his hands (in his own eyes the hands are drawn apart, and a glow at their place in the world would hang before them).
        for (HumanoidArm arm : ownView ? new HumanoidArm[0] : HumanoidArm.values()) {
            FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), rel(hand(p, arm)), 0.5F * w, 0.85F, 0.95F, 1.0F, 0.8F * w, age * 0.4F);
        }
        // Streams of light climbing round the body (in his own eyes, only as high as his chest: above it they cross his sight).
        for (int i = 0; i < 5; i++) {
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= (ownView ? 6 : 12); k++) {
                float t = k / 12.0F;
                double a = i * 1.26 + age * 0.15 + t * 4;
                double r = 0.8 + 0.5 * Mth.sin(t * Mth.PI);
                pts.add(c.add(Math.cos(a) * r, -1.0 + 2.6 * t, Math.sin(a) * r));
            }
            FxGeometry.ribbon(pose, light, pts, 0.10F, 0.02F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.0F, (ownView ? 0.35F : 0.5F) * w, FULL, false);
        }
    }

    /** The air scooter: a ball of spinning air under the rider, rolling with the ride. */
    /**
     * The elemental sphere of Sozin's Comet: round the Avatar, from the inside out, a ring of dark stones, a band of
     * water, a grey band of air and a broad ring of fire, each tilted its own way and spinning like a gyroscope.
     */
    private static void elementalSphere(AbstractClientPlayer p, float w) {
        Vec3 c = rel(p.getPosition(partial).add(0, 1.0, 0));
        int lit = lightAt(p.getPosition(partial));
        float[] wf = waterFrame();
        float[] ff = fireFrame(1.0F);
        // Earth: six stones on the innermost ring.
        Quaternionf earthTilt = new Quaternionf().rotationX(0.5F).mul(new Quaternionf().rotationY(age * 0.12F));
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3 + age * 0.25;
            org.joml.Vector3f v = new org.joml.Vector3f((float) Math.cos(ang) * 1.5F, 0.0F, (float) Math.sin(ang) * 1.5F);
            earthTilt.transform(v);
            FxGeometry.sphere(pose, vc(FxGeometry.snow()), c.add(v.x, v.y, v.z), 0.22F, 4, 6, 0, 1, 0.42F, 0.36F, 0.28F, 0.98F * w, 0, 0, lit, true, new Quaternionf().rotationY(age * 0.5F + i));
        }
        // Water: a band on the second ring.
        List<Vec3> water = new ArrayList<>();
        Quaternionf waterTilt = new Quaternionf().rotationZ(-0.6F).mul(new Quaternionf().rotationY(-age * 0.1F));
        for (int k = 0; k <= 32; k++) {
            double ang = k * Math.PI * 2 / 32 - age * 0.3;
            org.joml.Vector3f v = new org.joml.Vector3f((float) Math.cos(ang) * 2.0F, 0.0F, (float) Math.sin(ang) * 2.0F);
            waterTilt.transform(v);
            water.add(c.add(v.x, v.y, v.z));
        }
        FxGeometry.tube(pose, vc(FxGeometry.flow()), water, 0.18F, 0.18F, 8, wf[0], wf[1], 6.0F, age * 0.1F, WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.9F * w, 0.9F * w, lit, true);
        // Air: a grey band on the third.
        List<Vec3> air = new ArrayList<>();
        Quaternionf airTilt = new Quaternionf().rotationX(-0.9F).mul(new Quaternionf().rotationY(age * 0.08F));
        for (int k = 0; k <= 32; k++) {
            double ang = k * Math.PI * 2 / 32 + age * 0.35;
            org.joml.Vector3f v = new org.joml.Vector3f((float) Math.cos(ang) * 2.5F, 0.0F, (float) Math.sin(ang) * 2.5F);
            airTilt.transform(v);
            air.add(c.add(v.x, v.y, v.z));
        }
        FxGeometry.ribbon(pose, vc(FxGeometry.light()), air, 0.35F, 0.35F, 0, 1, 1, 0, 0.85F, 0.9F, 0.95F, 0.35F * w, 0.35F * w, FULL, false);
        // Fire: the broad outer ring.
        List<Vec3> fire = new ArrayList<>();
        Quaternionf fireTilt = new Quaternionf().rotationZ(0.35F).mul(new Quaternionf().rotationY(-age * 0.06F));
        for (int k = 0; k <= 40; k++) {
            double ang = k * Math.PI * 2 / 40 - age * 0.4;
            org.joml.Vector3f v = new org.joml.Vector3f((float) Math.cos(ang) * 3.1F, 0.0F, (float) Math.sin(ang) * 3.1F);
            fireTilt.transform(v);
            fire.add(c.add(v.x, v.y, v.z));
        }
        FxGeometry.tube(pose, vc(FxGeometry.fire(false)), fire, 0.3F, 0.3F, 8, ff[0], ff[1], 10.0F, age * 0.2F, 1.0F, 1.0F, 1.0F, 0.95F * w, 0.95F * w, FULL, true);
        FxGeometry.puffs(pose, vc(FxGeometry.fire(false)), fire, 0.7F, 0.7F, ff[0], ff[1], 1.0F, 1.0F, 1.0F, 0.8F * w, 0.8F * w, FULL, true, age * 0.3F);
    }

    private static void airBall(AbstractClientPlayer p, float w) {
        Vec3 feet = p.getPosition(partial);
        // The ball stands on the ground under him and he sits on top of it (Stances.drop lifts the body by its height).
        float radius = 0.6F;
        Vec3 center = rel(feet.add(0, radius, 0));
        // The ball rolls: its turn is the ride's distance over its radius, about the axis across the motion.
        Quaternionf rot = ROLL.computeIfAbsent(p.getId(), k -> new Quaternionf());
        double last = ROLL_TIME.getOrDefault(p.getId(), (double) age);
        float dt = (float) Mth.clamp(age - last, 0.0, 2.0);
        ROLL_TIME.put(p.getId(), (double) age);
        Vec3 v = new Vec3(p.getX() - p.xOld, 0, p.getZ() - p.zOld);
        double speed = v.length();
        if (speed > 1.0E-4 && dt > 0) {
            Vec3 axis = new Vec3(0, 1, 0).cross(v.normalize());
            rot.premul(new Quaternionf().fromAxisAngleRad((float) axis.x, (float) axis.y, (float) axis.z, (float) (speed * dt / radius)));
        }
        // An idle spin too, so it never looks like a still thing.
        Quaternionf spin = new Quaternionf(rot).mul(new Quaternionf().rotationY(age * 0.08F));
        VertexConsumer light = vc(FxGeometry.light());
        // The show's ball: a translucent cyan sphere wrapped in spinning white bands.
        FxGeometry.sphere(pose, light, center, radius, 10, 20, 0, 1, 0.45F, 0.88F, 0.95F, 0.42F * w, 1.6F, age * 0.2F, FULL, false, spin);
        Quaternionf spin2 = new Quaternionf(rot).mul(new Quaternionf().rotationX(1.1F)).mul(new Quaternionf().rotationY(-age * 0.13F));
        FxGeometry.sphere(pose, light, center, radius * 0.8F, 8, 16, 0, 1, 0.75F, 0.97F, 1.0F, 0.3F * w, 2.1F, -age * 0.3F, FULL, false, spin2);
        for (int i = 0; i < 5; i++) {
            List<Vec3> band = new ArrayList<>();
            for (int k = 0; k <= 14; k++) {
                float t = k / 14.0F;
                double lat = (i / 5.0 - 0.45 + 0.15 * Math.sin(t * 3 + i)) * Math.PI * 0.8;
                double lon = -age * 0.45 + i * 1.26 + t * 2.6;
                band.add(center.add(Math.cos(lat) * Math.cos(lon) * radius * 1.02, Math.sin(lat) * radius * 1.02, Math.cos(lat) * Math.sin(lon) * radius * 1.02));
            }
            FxGeometry.ribbon(pose, light, band, 0.03F, 0.11F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.15F * w, 0.7F * w, FULL, false);
        }
        // Streaks of wind round the ball.
        for (int i = 0; i < 3; i++) {
            List<Vec3> pts = new ArrayList<>();
            float phase = age * 0.25F + i * 2.1F;
            for (int k = 0; k <= 10; k++) {
                float t = k / 10.0F;
                double a = phase + t * Math.PI * 1.4;
                double y = Math.sin(t * Math.PI + i) * radius * 0.9;
                double r = Math.sqrt(Math.max(0.0, radius * radius * 1.15 - y * y));
                pts.add(center.add(Math.cos(a) * r, y, Math.sin(a) * r));
            }
            FxGeometry.ribbon(pose, light, pts, 0.10F, 0.02F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.0F, 0.45F * w, FULL, false);
        }
        // The dust it kicks up: a thin ring at the ground.
        FxGeometry.disc(pose, light, center.add(0, -radius + 0.03, 0), radius * 0.6F, radius * 1.5F, 20, 0, 1, 0.85F, 0.8F, 0.7F, 0.16F * w, 0.0F, FULL, false, age * 0.3F);
    }

    /** The glider's wings, open on the staff above the airbender's hands, coloured as the staff was dyed. */
    /**
     * The glider staff of the show: a wooden staff over the hands from which two fans of orange fabric open on
     * radial ribs, sweeping out to the sides and forward like a kite, with a small tail fin behind; the bender hangs
     * under it. Drawn whenever the staff is open, fluttering a little in the wind.
     */
    private static void glider(AbstractClientPlayer p, float w) {
        ItemStack stack = p.getMainHandItem().getItem() instanceof GliderItem ? p.getMainHandItem() : p.getOffhandItem();
        int color = stack.getItem() instanceof GliderItem ? GliderItem.color(stack) : GliderItem.DEFAULT_COLOR;
        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        Vec3 look = look(p);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = right(flat);
        Vec3 hr = hand(p, HumanoidArm.RIGHT);
        Vec3 hl = hand(p, HumanoidArm.LEFT);
        Vec3 grip = rel(hr.add(hl).scale(0.5).add(0, 0.12, 0));
        int light = lightAt(hr);
        float open = FxGeometry.easeOut(w);
        float flutter = 0.05F * Mth.sin(age * 0.7F);
        VertexConsumer snow = vc(FxGeometry.snow());
        // The staff across the hands, and the hinge above it.
        List<Vec3> staff = List.of(grip.subtract(side.scale(1.0)), grip.add(side.scale(1.0)));
        FxGeometry.tube(pose, snow, staff, 0.04F, 0.04F, 6, 0, 1, 1, 0, 0.42F, 0.28F, 0.14F, 1.0F, 1.0F, light, true);
        Vec3 hinge = grip.add(0, 0.05, 0);
        // Two fans: ribs from the hinge out to an arc from behind the staff round to the front, fabric between them.
        int ribs = 7;
        double a0 = Math.toRadians(-55);
        double a1 = Math.toRadians(80);
        float span = 1.9F * open;
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Vec3 prev = null;
            for (int i = 0; i <= ribs; i++) {
                double t = (double) i / ribs;
                double ang = a0 + (a1 - a0) * t;
                // Each rib leans out to its side and swings from back to front; the tips sag and flutter.
                double out = Math.cos(ang) * span;
                double fwd = Math.sin(ang) * span * 0.75;
                double sag = -0.08 * open * t * (1 - t) * 4 + flutter * t;
                Vec3 tip = hinge.add(side.scale(sgn * out)).add(flat.scale(fwd)).add(0, sag, 0);
                FxGeometry.tube(pose, snow, List.of(hinge, tip), 0.025F, 0.012F, 4, 0, 1, 1, 0, 0.35F, 0.22F, 0.1F, 1.0F, 1.0F, light, true);
                if (prev != null) {
                    quadColored(snow, hinge, prev, tip, hinge.add(0, 0.001, 0), r, g, b, 0.95F, light);
                    quadColored(snow, hinge.add(0, 0.001, 0), tip, prev, hinge, r * 0.85F, g * 0.85F, b * 0.85F, 0.95F, light);
                }
                prev = tip;
            }
        }
        // The tail fin behind the hinge.
        Vec3 tailBase = grip.add(flat.scale(-0.3));
        Vec3 tailEnd = grip.add(flat.scale(-0.3 - 0.8 * open)).add(0, -0.05, 0);
        FxGeometry.tube(pose, snow, List.of(tailBase, tailEnd), 0.03F, 0.015F, 4, 0, 1, 1, 0, 0.35F, 0.22F, 0.1F, 1.0F, 1.0F, light, true);
        quadColored(snow, tailBase, tailEnd, tailEnd.add(0, 0.28 * open, 0), tailBase.add(0, 0.12 * open, 0), r, g, b, 0.95F, light);
        quadColored(snow, tailBase.add(0, 0.12 * open, 0), tailEnd.add(0, 0.28 * open, 0), tailEnd, tailBase, r * 0.85F, g * 0.85F, b * 0.85F, 0.95F, light);
    }

    private static void quadColored(VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float al, int light) {
        FxGeometry.flatQuad(pose, vc, a, b, c, d, r, g, bl, al, light);
    }

    /** The wind of a fast flight: streaks trailing the body. */
    private static void wind(AbstractClientPlayer p, AnimState st) {
        Vec3 v = new Vec3(p.getX() - p.xOld, p.getY() - p.yOld, p.getZ() - p.zOld);
        if (v.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 back = v.normalize().scale(-1.0);
        Vec3 center = rel(p.getPosition(partial).add(0, 1.0, 0));
        float strength = ss(0.5F, 1.8F, st.sH) * st.wAir;
        VertexConsumer light = vc(FxGeometry.light());
        for (int i = 0; i < 6; i++) {
            float seedA = i * 2.4F;
            Vec3 off = new Vec3(Math.cos(seedA) * 0.55, Math.sin(seedA * 1.7) * 0.5, Math.sin(seedA) * 0.55);
            float phase = (age * 0.5F + i * 0.9F) % 3.0F;
            Vec3 start = center.add(off).add(back.scale(phase * 0.6));
            List<Vec3> pts = List.of(start, start.add(back.scale(1.5 + 2.5 * strength)));
            FxGeometry.ribbon(pose, light, pts, 0.10F, 0.02F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.22F * strength, 0.0F, FULL, false);
        }
    }

    /** The air shield: a pale sphere of circling bands, breathing. */
    private static void airShield(AbstractClientPlayer p, BendingData data) {
        float radius = (float) AirBending.shieldRadius(data) * (1.0F + 0.035F * Mth.sin(age * 0.12F));
        Vec3 c = rel(p.getPosition(partial).add(0, 1.0, 0));
        Quaternionf rot = new Quaternionf().rotationY(age * 0.02F).mul(new Quaternionf().rotationX(0.08F * Mth.sin(age * 0.05F)));
        VertexConsumer light = vc(FxGeometry.light());
        FxGeometry.sphere(pose, light, c, radius, 10, 20, 0, 1, AIR[0], AIR[1], AIR[2], 0.14F, 1.3F, age * 0.15F, FULL, false, rot);
        Quaternionf rot2 = new Quaternionf().rotationZ(0.6F).mul(new Quaternionf().rotationY(-age * 0.035F));
        FxGeometry.sphere(pose, light, c, radius * 0.97F, 8, 16, 0, 1, 1.0F, 1.0F, 1.0F, 0.07F, 2.0F, -age * 0.2F, FULL, false, rot2);
        // The show's cocoon of air: concentric white spiral lines winding round the body with the spin.
        for (int i = 0; i < 6; i++) {
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= 14; k++) {
                float t = k / 14.0F;
                double lat = (i / 6.0 - 0.5 + t * 0.25) * Math.PI * 0.9;
                double lon = age * 0.3 + i * 1.3 + t * 3.0;
                pts.add(c.add(Math.cos(lat) * Math.cos(lon) * radius * 1.03, Math.sin(lat) * radius * 1.03, Math.cos(lat) * Math.sin(lon) * radius * 1.03));
            }
            FxGeometry.ribbon(pose, light, pts, 0.03F, 0.12F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.1F, 0.5F, FULL, false);
        }
    }

    /** Water arms: tendrils of water along the forearms, rolling. */
    private static void waterArms(AbstractClientPlayer p, BendingData data, float w) {
        Vec3 look = look(p);
        Vec3 side = right(look);
        float len = 1.0F + data.level(Ability.WATER_ARMS) * 0.7F;
        float[] fr = waterFrame();
        VertexConsumer water = vc(FxGeometry.water());
        VertexConsumer light = vc(FxGeometry.light());
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 h = hand(p, arm);
            Vec3 dir = fan(look, arm == HumanoidArm.RIGHT ? 0.2 : -0.2);
            int lit = lightAt(h);
            float phase = age * 0.3F + (arm == HumanoidArm.RIGHT ? 0 : 1.6F);
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= 8; k++) {
                float t = k / 8.0F;
                Vec3 at = h.add(dir.scale(t * len * w)).add(side.scale(Math.sin(phase + t * 4) * 0.12 * t))
                        .add(0, Math.cos(phase * 1.2 + t * 3) * 0.10 * t, 0);
                pts.add(rel(at));
            }
            FxGeometry.tube(pose, water, pts, 0.15F, 0.06F, 8, fr[0], fr[1], 2.0F, age * 0.05F, WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.85F * w, 0.6F * w, lit, true);
            FxGeometry.puffs(pose, water, pts, 0.34F, 0.14F, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.7F * w, 0.4F * w, lit, true, age * 0.1F);
            FxGeometry.ribbon(pose, light, pts, 0.36F, 0.16F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.18F * w, 0.08F * w, FULL, false);
            FxGeometry.ribbon(pose, light, pts, 0.06F, 0.02F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.35F * w, 0.1F * w, FULL, false);
        }
    }

    /** The octopus form: eight tentacles of water from the waist, curling and swaying. */
    private static void octopus(AbstractClientPlayer p, float w) {
        Vec3 base = p.getPosition(partial).add(0, 1.05, 0);
        float[] fr = waterFrame();
        int lit = lightAt(base);
        VertexConsumer water = vc(FxGeometry.water());
        VertexConsumer light = vc(FxGeometry.light());
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + age * 0.015;
            float curl = 0.5F + 0.5F * Mth.sin(age * 0.09F + i * 1.3F);
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= 8; k++) {
                float t = k / 8.0F;
                double r = 0.35 + t * 2.3 * w;
                double y = -0.35 * t + t * t * 1.6 * curl + Math.sin(t * 4 + age * 0.2 + i) * 0.12 * t;
                double sway = Math.sin(age * 0.17 + i + t * 2) * 0.25 * t;
                pts.add(rel(base.add(Math.cos(a + sway) * r, y, Math.sin(a + sway) * r)));
            }
            FxGeometry.tube(pose, water, pts, 0.17F, 0.04F, 8, fr[0], fr[1], 3.0F, age * 0.06F + i * 0.2F, WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.9F * w, 0.6F * w, lit, true);
            FxGeometry.ribbon(pose, light, pts, 0.40F, 0.12F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.16F * w, 0.06F * w, FULL, false);
            FxGeometry.ribbon(pose, light, pts, 0.05F, 0.01F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.3F * w, 0.05F * w, FULL, false);
        }
        // A collar of water round the waist that the tentacles grow from.
        FxGeometry.ringBand(pose, water, rel(base.add(0, -0.25, 0)), 0.5F, 0.4F, 16, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.6F * w, lit, true, age * 0.2F, 0.2F);
    }

    /**
     * The mastered octopus form wraps the whole body in water, as Katara's does in the show: a great bulb of flowing
     * water over the bender from the ground to above the head, widest at the chest, the bender seen through it, the
     * arms reaching out from its foot.
     */
    private static void octopusBody(AbstractClientPlayer p, float w) {
        Vec3 feet = p.getPosition(partial);
        float tall = p.getBbHeight() + 0.7F;
        float[] fr = waterFrame();
        int lit = lightAt(feet.add(0, 1, 0));
        VertexConsumer flow = vc(FxGeometry.flow());
        VertexConsumer water = vc(FxGeometry.water());
        VertexConsumer light = vc(FxGeometry.light());
        int n = 14;
        List<Vec3> spine = new ArrayList<>();
        float[] radii = new float[n + 1];
        for (int k = 0; k <= n; k++) {
            float t = (float) k / n;
            double sway = Math.sin(age * 0.12 + t * 3) * 0.05;
            spine.add(rel(feet.add(sway, -0.1 + tall * t, -sway)));
            radii[k] = shellRadius(t, k);
        }
        for (int k = 0; k < n; k++) {
            FxGeometry.tube(pose, flow, spine.subList(k, k + 2), radii[k], radii[k + 1], 18, fr[0], fr[1], 0.5F, -age * 0.05F + (float) k / n,
                    WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.42F * w, 0.42F * w, lit, true);
        }
        // A bright skin where the light catches the water, and currents running up the bulb.
        FxGeometry.ribbon(pose, light, spine.subList(0, n), 2.2F, 1.0F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.08F * w, 0.12F * w, FULL, false);
        for (int i = 0; i < 3; i++) {
            float t = (age * 0.015F + i / 3.0F) % 0.9F;
            float r = shellRadius(t, 0) + 0.03F;
            FxGeometry.ringBand(pose, light, rel(feet.add(0, -0.1 + tall * t, 0)), r, 0.06F, 20, 0, 1, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.3F * w, FULL, false, age * 0.3F, 0.3F);
        }
        // The pool it stands in.
        FxGeometry.disc(pose, water, rel(feet.add(0, 0.03, 0)), 0.0F, 1.5F, 24, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.6F * w, 0.0F, lit, true, age * 0.1F);
    }

    /** The radius of the octopus bulb at height {@code t} (0 the ground, 1 its crown): widest at the chest, rippling. */
    private static float shellRadius(float t, int k) {
        double e = (t - 0.42) / 0.58;
        float shape = (float) (1.15 * Math.sqrt(Math.max(0.0, 1.0 - e * e)));
        return Math.max(0.02F, shape + 0.04F * Mth.sin(age * 0.22F + k * 1.3F) * shape);
    }

    /** The water spout: a column of water from the surface to the feet, spiralling up, with a crown of spray. */
    private static void spout(AbstractClientPlayer p, BendingData data, float girth) {
        Vec3 feet = p.getPosition(partial);
        // Water or ice under him (a waterbender raises his spout off a frozen lake too).
        double base = com.kolodkingal.enlightened.bending.Spouts.base(level, p, 26, true);
        if (Double.isNaN(base)) {
            return;
        }
        double top = feet.y + 0.1;
        double bottom = base;
        if (top - bottom < 0.3) {
            return;
        }
        float[] fr = waterFrame();
        int lit = lightAt(feet);
        VertexConsumer flow = vc(FxGeometry.flow());
        VertexConsumer water = vc(FxGeometry.water());
        VertexConsumer light = vc(FxGeometry.light());
        List<Vec3> pts = new ArrayList<>();
        int n = Math.max(4, (int) ((top - bottom) * 2));
        for (int k = 0; k <= n; k++) {
            float t = (float) k / n;
            double y = bottom + (top - bottom) * t;
            double a = age * 0.25 + t * 5;
            pts.add(rel(new Vec3(feet.x + Math.cos(a) * 0.15 * (1 - t), y, feet.z + Math.sin(a) * 0.15 * (1 - t))));
        }
        float height = (float) (top - bottom);
        // The twisting body of water, wide at the surface and narrowing to the feet.
        FxGeometry.tube(pose, flow, pts, 1.05F * girth, 0.35F * girth, 14, fr[0], fr[1], height * 0.5F, -age * 0.08F, WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.9F, 0.8F, lit, true);
        FxGeometry.ribbon(pose, light, pts, 1.6F * girth, 0.5F * girth, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.12F, 0.18F, FULL, false);
        // Ropes of foam winding up the column.
        for (int i = 0; i < 3; i++) {
            List<Vec3> rope = new ArrayList<>();
            for (int k = 0; k <= n * 2; k++) {
                float t = (float) k / (n * 2);
                double y = bottom + (top - bottom) * t;
                double a = i * 2.1 - age * 0.3 + t * 9;
                double r = (0.95 - 0.6 * t) * girth;
                rope.add(rel(new Vec3(feet.x + Math.cos(a) * r, y, feet.z + Math.sin(a) * r)));
            }
            FxGeometry.ribbon(pose, light, rope, 0.10F, 0.05F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.35F, 0.45F, FULL, false);
        }
        // The whirl, the streamers and the rings round the rider: water with a bright foaming rim.
        spoutWhirl(feet, bottom, water, true, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.85F, lit, 2.0F * girth);
        spoutWhirl(feet, bottom, light, false, 0, 1, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.45F, FULL, 0.8F * girth);
        // Chunks of foam flung off the twisting body, as in the show, and droplets round the crown.
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(now / 3);
        for (int i = 0; i < 12; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double h = bottom + rnd.nextDouble() * height;
            double r = 0.9 + rnd.nextDouble() * 0.9;
            FxGeometry.billboard(pose, vc(FxGeometry.snow()), rel(new Vec3(feet.x + Math.cos(ang) * r, h, feet.z + Math.sin(ang) * r)), 0.14F + rnd.nextFloat() * 0.22F, 0, 1, 0.95F, 0.98F, 1.0F, 0.85F, lit, true, rnd.nextFloat() * 6);
        }
        // The swell at the surface, and the cup of water under the feet.
        Vec3 foot = rel(new Vec3(feet.x, bottom + 0.05, feet.z));
        FxGeometry.disc(pose, water, foot, 0.5F, 2.2F * girth, 24, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.7F, 0.0F, lit, true, age * 0.1F);
        FxGeometry.ringBand(pose, water, foot, 1.3F * girth, 0.5F, 20, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.5F, lit, true, age * 0.4F, 0.5F);
        FxGeometry.disc(pose, water, rel(new Vec3(feet.x, top - 0.02, feet.z)), 0.2F, 0.8F, 16, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.8F, 0.3F, lit, true, -age * 0.2F);
    }

    /** Bloodbending: strands of red from both hands to the victim, and a faint red haze round them. */
    private static void bloodStrands(AbstractClientPlayer p, BendingData data, float w) {
        Entity held = data.heldEntityId() < 0 ? null : level.getEntity(data.heldEntityId());
        if (!(held instanceof LivingEntity victim)) {
            return;
        }
        Vec3 center = victim.getPosition(partial).add(0, victim.getBbHeight() * 0.5, 0);
        // (No lightning: bloodbending is a grip on the blood, drawn only as a dark red throb round the one held.)
        VertexConsumer light = vc(FxGeometry.light());
        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.5F);
        FxGeometry.sphere(pose, light, rel(center), victim.getBbWidth() * 0.9F, 6, 12, 0, 1, BLOOD[0], BLOOD[1], BLOOD[2], (0.03F + 0.03F * pulse) * w, 1.5F, age * 0.3F, FULL, false,
                new Quaternionf().rotationY(age * 0.05F));
    }

    /** Suffocation: the air round the victim's head wrung tight, spinning. */
    private static void suffocation(AbstractClientPlayer p, BendingData data, float w) {
        Entity held = data.heldEntityId() < 0 ? null : level.getEntity(data.heldEntityId());
        if (!(held instanceof LivingEntity victim)) {
            return;
        }
        // After Zaheer's suffocation of the Earth Queen: a clear bubble of air round the whole head, a bright rim where
        // the eye meets its edge, streaks of air whirling inside it as the breath is drawn out, a glint on the glass,
        // and a thin stream from the mouth to the airbender's hand.
        float radius = Math.max(0.42F, victim.getBbWidth() * 0.85F);
        Vec3 headWorld = victim.getEyePosition(partial).add(0, -0.05, 0);
        Vec3 head = rel(headWorld);
        VertexConsumer light = vc(FxGeometry.light());
        Quaternionf rot = new Quaternionf().rotationY(age * 0.3F);
        FxGeometry.sphere(pose, light, head, radius, 8, 16, 0, 1, 0.85F, 0.95F, 1.0F, 0.07F * w, 0.0F, 0.0F, FULL, false, rot);
        FxGeometry.sphere(pose, light, head, radius * 0.97F, 8, 16, 0, 1, AIR[0], AIR[1], AIR[2], 0.10F * w, 3.0F, age * 0.6F, FULL, false, rot);
        // The rim: a ring facing the camera.
        Vec3 toCam = head.scale(-1.0).normalize();
        Vec3 u = toCam.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1.0E-6) {
            u = new Vec3(1, 0, 0);
        }
        u = u.normalize();
        Vec3 v = u.cross(toCam).normalize();
        List<Vec3> rim = new ArrayList<>();
        for (int k = 0; k <= 32; k++) {
            double a = Math.PI * 2 * k / 32;
            rim.add(head.add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius)));
        }
        FxGeometry.ribbon(pose, light, rim, 0.07F, 0.07F, 0, 1, 1, 0, 0.9F, 0.97F, 1.0F, 0.55F * w, 0.55F * w, FULL, false);
        FxGeometry.ribbon(pose, light, rim, 0.22F, 0.22F, 0, 1, 1, 0, AIR[0], AIR[1], AIR[2], 0.15F * w, 0.15F * w, FULL, false);
        // Streaks of air whirling inside, each on its own tilt.
        for (int i = 0; i < 5; i++) {
            Quaternionf tilt = new Quaternionf().rotationY(i * 1.26F + age * (0.35F + i * 0.05F)).mul(new Quaternionf().rotationX(0.5F + i * 0.45F));
            List<Vec3> streak = new ArrayList<>();
            for (int k = 0; k <= 14; k++) {
                double a = k / 14.0 * 3.4;
                org.joml.Vector3f pt = new org.joml.Vector3f((float) (Math.cos(a) * radius * 0.78), 0, (float) (Math.sin(a) * radius * 0.78));
                tilt.transform(pt);
                streak.add(head.add(pt.x, pt.y, pt.z));
            }
            FxGeometry.ribbon(pose, light, streak, 0.06F, 0.01F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.6F * w, 0.0F, FULL, false);
        }
        // The glint, up and to one side.
        FxGeometry.billboard(pose, light, head.add(u.scale(-radius * 0.45)).add(v.scale(radius * 0.5)), radius * 0.35F, 0, 1, 1.0F, 1.0F, 1.0F, 0.5F * w, FULL, false, 0);
        // The breath drawn out to the bender's hand.
        Vec3 mouth = headWorld.add(victim.getLookAngle().scale(radius * 0.6)).add(0, -0.12, 0);
        Vec3 h = hand(p, HumanoidArm.RIGHT);
        List<Vec3> breath = new ArrayList<>();
        for (int k = 0; k <= 16; k++) {
            float t = k / 16.0F;
            Vec3 at = mouth.lerp(h, t);
            double wob = Math.sin(t * 9 - age * 0.6) * 0.06 * (1 - t);
            breath.add(rel(at.add(0, wob, wob)));
        }
        FxGeometry.ribbon(pose, light, breath, 0.10F, 0.03F, 0, 1, 1, 0, AIR[0], AIR[1], AIR[2], 0.35F * w, 0.1F * w, FULL, false);
    }

    /** The fire fist: streams of fire from the fists (or the mouth, for a master's breath). */
    private static void fireStreams(AbstractClientPlayer p, BendingData data, float fist, float breath) {
        int blue = Variants.fireColour(data);
        float[] core = fireCore(blue);
        Vec3 look = look(p);
        int lvl = Math.max(1, data.level(Ability.FIRE_FIST));
        float len = 6.0F + lvl * 1.5F;
        float[] fr = fireFrame(0.8F);
        VertexConsumer fire = vc(FxGeometry.fire(blue));
        VertexConsumer light = vc(FxGeometry.light());
        if (fist > 0.01F) {
            for (HumanoidArm arm : HumanoidArm.values()) {
                Vec3 h = rel(hand(p, arm));
                Vec3 dir = fan(look, arm == HumanoidArm.RIGHT ? 0.12 : -0.12);
                float phase = age * 0.45F + (arm == HumanoidArm.RIGHT ? 0 : 2.0F);
                float flicker = 0.85F + 0.15F * Mth.sin(age * 1.7F + arm.ordinal());
                FxGeometry.cone(pose, fire, h, dir, len * fist, 0.13F, 0.75F, 10, 9, fr[0], fr[1], len / 1.6F, age * 0.18F, 1.0F, 1.0F, 1.0F, 0.95F * flicker, 0.0F, FULL, true, 0.28F, phase);
                // Puffs of flame along the stream give it body from every angle, the bender's own camera included.
                // Puffs of flame along the stream give it body from every angle; the far tongues roll upward and break up.
                List<Vec3> spine = FxGeometry.conePoints(h, dir, len * fist, 12, 0.28F, phase);
                for (int k = 0; k < spine.size(); k++) {
                    float t = (float) k / (spine.size() - 1);
                    spine.set(k, spine.get(k).add(0, t * t * 0.9 + Math.sin(age * 0.5F + k) * 0.08 * t, 0));
                }
                FxGeometry.puffs(pose, fire, spine, 0.34F, 1.9F, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.9F * flicker, 0.0F, FULL, true, age * 0.15F + arm.ordinal());
                FxGeometry.cone(pose, light, h, dir, len * fist * 0.8F, 0.06F, 0.3F, 8, 6, 0, 1, 1, 0, core[0], core[1], core[2], 0.65F, 0.0F, FULL, false, 0.15F, phase + 1);
                FxGeometry.conePuffs(pose, vc(FxGeometry.glowTexture()), h, dir, len * fist * 0.9F, 0.2F, 0.6F, 6, 0, 1, core[0], core[1], core[2], 0.5F, 0.0F, FULL, true, 0.15F, phase + 1, 1.0F, age * 0.1F);
                FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), h, 0.5F, core[0], core[1], core[2], 0.6F * fist, age * 0.2F);
            }
        }
        if (breath > 0.01F) {
            Vec3 mouth = rel(p.getEyePosition(partial).add(look.scale(0.25)).add(0, -0.12, 0));
            float flicker = 0.85F + 0.15F * Mth.sin(age * 1.9F);
            FxGeometry.cone(pose, fire, mouth, look, (len + 3.0F) * breath, 0.12F, 1.0F, 12, 10, fr[0], fr[1], len / 1.4F, age * 0.2F, 1.0F, 1.0F, 1.0F, 0.95F * flicker, 0.0F, FULL, true, 0.3F, age * 0.5F);
            FxGeometry.conePuffs(pose, fire, mouth, look, (len + 3.0F) * breath, 0.12F, 1.0F, 14, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.9F * flicker, 0.0F, FULL, true, 0.3F, age * 0.5F, 1.3F, age * 0.2F);
            FxGeometry.cone(pose, light, mouth, look, (len + 2.0F) * breath, 0.06F, 0.4F, 8, 6, 0, 1, 1, 0, core[0], core[1], core[2], 0.7F, 0.0F, FULL, false, 0.15F, age * 0.5F + 1);
        }
    }

    /** The fire whip: a lash of fire from the main hand, snaking along the look. */
    /** Where each whip hand was over the last ticks, so the rope trails the arm's swing. */
    private static final Map<Integer, java.util.ArrayDeque<Vec3>> WHIP_TRAIL = new HashMap<>();

    /**
     * The fire whip as Zuko lashes it: a thick rope of orange fire with a yellow-white heart, anchored at the hand and
     * trailing its swing in long loops and S-curves, its tip cracking out along the look; tongues peel off the rope.
     */
    private static void fireWhip(AbstractClientPlayer p, BendingData data, float w) {
        int blue = Variants.fireColour(data);
        float[] core = fireCore(blue);
        Vec3 look = look(p);
        Vec3 side = right(look);
        Vec3 up = side.cross(look);
        int lvl = Math.max(1, data.level(Ability.FIRE_WHIP));
        float len = (7.0F + 3.0F * (lvl - 1)) * w;
        Vec3 h = hand(p, p.getMainArm());
        java.util.ArrayDeque<Vec3> trail = WHIP_TRAIL.computeIfAbsent(p.getId(), k -> new java.util.ArrayDeque<>());
        if (trail.isEmpty() || trail.peekFirst().distanceToSqr(h) > 1.0E-4) {
            trail.addFirst(h);
        }
        while (trail.size() > 14) {
            trail.removeLast();
        }
        // The rope: the first part follows where the hand has been (the lag of a real whip), the rest loops out along the look.
        List<Vec3> pts = new ArrayList<>();
        int lagPts = Math.min(trail.size(), 5);
        int i = 0;
        for (Vec3 past : trail) {
            if (i >= lagPts) {
                break;
            }
            Vec3 at = past.add(look.scale(i * 0.35));
            pts.add(rel(at));
            i++;
        }
        Vec3 base = pts.isEmpty() ? rel(h) : pts.get(pts.size() - 1);
        for (int k = 1; k <= 16; k++) {
            float t = k / 16.0F;
            double loop = Math.sin(t * Math.PI * 2.5 + age * 0.55) * 1.1 * Math.sqrt(t);
            double rise = Math.cos(t * Math.PI * 1.7 + age * 0.45) * 0.55 * t + 0.25 * Math.sin(t * Math.PI);
            Vec3 at = base.add(look.scale(t * len)).add(side.scale(loop)).add(up.scale(rise));
            pts.add(at);
        }
        float[] fr = fireFrame(0.9F);
        VertexConsumer fire = vc(FxGeometry.fire(blue));
        FxGeometry.tube(pose, fire, pts, 0.26F, 0.07F, 8, fr[0], fr[1], len / 1.2F, age * 0.25F, 1.0F, 1.0F, 1.0F, 0.95F, 0.6F, FULL, true);
        FxGeometry.tube(pose, vc(FxGeometry.light()), pts, 0.11F, 0.02F, 6, 0, 1, 1, 0, core[0], core[1], core[2], 0.8F, 0.3F, FULL, false);
        FxGeometry.puffs(pose, fire, pts, 0.6F, 0.25F, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.8F, 0.35F, FULL, true, age * 0.3F);
        FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), pts.get(pts.size() - 1), 0.6F, core[0], core[1], core[2], 0.6F * w, age * 0.3F);
        FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), rel(h), 0.7F, core[0], core[1], core[2], 0.5F * w, -age * 0.2F);
    }

    private static void lightningCharge(AbstractClientPlayer p, BendingData data, AnimState st, float w) {
        Vec3 hr = rel(hand(p, HumanoidArm.RIGHT));
        Vec3 hl = rel(hand(p, HumanoidArm.LEFT));
        float build = st != null ? ss(0.0F, 30.0F, age - st.activeSinceAge()) : 0.5F;
        VertexConsumer light = vc(FxGeometry.light());
        long seed = now / 2;
        int arcs = 1 + (int) (build * 2);
        for (int i = 0; i < arcs; i++) {
            FxGeometry.bolt(pose, light, hr, hl, seed * 13 + i * 101, 0.12F + 0.1F * build, 0.035F, BOLT[0], BOLT[1], BOLT[2], (0.6F + 0.4F * build) * w, 1);
        }
        for (HumanoidArm arm : HumanoidArm.values()) {
            Vec3 h = arm == HumanoidArm.RIGHT ? hr : hl;
            for (int i = 0; i < 2; i++) {
                net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(seed * 7 + arm.ordinal() * 31 + i);
                Vec3 end = h.add((rnd.nextDouble() - 0.5) * 0.7, (rnd.nextDouble() - 0.5) * 0.7, (rnd.nextDouble() - 0.5) * 0.7);
                FxGeometry.bolt(pose, light, h, end, seed + i * 17 + arm.ordinal() * 53, 0.08F, 0.025F, BOLT[0], BOLT[1], BOLT[2], 0.6F * w, 0);
            }
            FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), h, 0.35F + 0.3F * build, BOLT[0], BOLT[1], BOLT[2], 0.7F * w, age * 0.4F);
        }
    }

    /** Redirection readied: a crackle at the leading fingers now and then. */
    private static void redirectCrackle(AbstractClientPlayer p, float w) {
        if ((now / 4) % 3 != 0) {
            return;
        }
        Vec3 h = rel(hand(p, p.getMainArm()));
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(now / 4);
        Vec3 end = h.add((rnd.nextDouble() - 0.5) * 0.5, (rnd.nextDouble() - 0.5) * 0.5, (rnd.nextDouble() - 0.5) * 0.5);
        FxGeometry.bolt(pose, vc(FxGeometry.light()), h, end, now / 4, 0.06F, 0.02F, BOLT[0], BOLT[1], BOLT[2], 0.5F * w, 0);
    }

    /** The seismic sense: a pulse spreads over the ground from the feet every second. */
    private static void seismicPulse(AbstractClientPlayer p) {
        float phase = ((now % 20) + partial) / 20.0F;
        float radius = 0.4F + 6.0F * easeOut(phase);
        float alpha = (1.0F - phase) * 0.35F;
        Vec3 feet = rel(p.getPosition(partial).add(0, 0.04, 0));
        FxGeometry.disc(pose, vc(FxGeometry.light()), feet, Math.max(0.0F, radius - 0.35F), radius, 32, 0, 1, EARTH[0], EARTH[1], EARTH[2], alpha, alpha * 0.3F, FULL, false, 0);
    }

    // ====================================================================================================================
    // The one-shot effects
    // ====================================================================================================================

    private static void timed(WorldFx.Effect e) {
        float a = e.age(now, partial);
        float f = Mth.clamp(a / e.kind().life, 0.0F, 1.0F);
        Vec3 from = rel(e.from());
        Vec3 to = rel(e.to());
        Entity entity = e.entity() < 0 ? null : level.getEntity(e.entity());
        Vec3 at = entity != null ? rel(entity.getPosition(partial).add(0, entity.getBbHeight() * 0.5, 0)) : from;
        int lit = lightAt(e.from());
        switch (e.kind()) {
            case WATER_WHIP -> waterWhip(from, to, a, f, lit);
            case WATER_SPLASH -> {
                float[] fr = waterFrame();
                float r = 0.2F + e.data() * 1.1F * easeOut(f);
                FxGeometry.ringBand(pose, vc(FxGeometry.water()), from, r, 0.7F * (1.0F - f), 16, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.75F * (1.0F - f), lit, true, a * 0.4F, 0.5F);
                FxGeometry.disc(pose, vc(FxGeometry.water()), from, 0, r, 16, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.6F * (1.0F - f), 0.0F, lit, true, a * 0.2F);
            }
            case WATER_HEAL, FIRE_HEAL -> healing(e, at, entity, a, f, lit);
            case FREEZE -> freeze(from, e.data(), a, f, lit);
            case ICE_RISE -> {
                float[] fr = {0, 1};
                int n = Math.max(2, (int) from.distanceTo(to) + 1);
                for (int i = 0; i <= n; i++) {
                    Vec3 c = from.lerp(to, (double) i / n);
                    float h = 1.6F * easeOut(f) * (0.7F + 0.3F * Mth.sin(i * 1.7F));
                    FxGeometry.ringBand(pose, vc(FxGeometry.ice()), c, 0.18F, h, 5, fr[0], fr[1], 0.85F, 0.95F, 1.0F, 0.85F * (1.0F - f * f), lit, true, i, 0.0F);
                }
            }
            case FIRE_RING -> fireRing(from, e, a, f);
            case FIRE_BURST -> {
                int blue = e.color();
                float[] fr = fireFrame(1.0F);
                float r = e.data() * 0.6F * (0.3F + 0.7F * easeOut(f));
                FxGeometry.sphere(pose, vc(FxGeometry.fire(blue)), from, r, 6, 12, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - f), 0, 0, FULL, true, new Quaternionf().rotationY(a * 0.3F));
                float[] core = fireCore(blue);
                FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), from, r * 2.2F, core[0], core[1], core[2], 0.8F * (1.0F - f), a * 0.2F);
            }
            case LIGHTNING -> lightningBolt(from, to, e, a, f);
            case REDIRECT -> {
                if (entity == null) {
                    break;
                }
                Vec3 belly = rel(entity.getPosition(partial).add(0, entity.getBbHeight() * 0.45, 0));
                long seed = e.born() * 31 + now / 2;
                float flick = 0.7F + 0.3F * Mth.sin(a * 2.3F);
                if (f < 0.6F) {
                    FxGeometry.bolt(pose, vc(FxGeometry.light()), from, belly, seed, 0.15F, 0.09F, BOLT[0], BOLT[1], BOLT[2], flick * (1.0F - f / 0.6F), 2);
                }
                if (f > 0.3F) {
                    FxGeometry.bolt(pose, vc(FxGeometry.light()), belly, to, seed + 7, 0.2F, 0.1F, BOLT[0], BOLT[1], BOLT[2], flick * (1.0F - f), 3);
                }
                FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), belly, 1.0F, BOLT[0], BOLT[1], BOLT[2], 0.6F * (1.0F - f), a);
            }
            case AIR_BLAST -> airBlast(from, to, e, a, f);
            case AIR_BURST -> {
                float r = e.data() * easeOut(f);
                FxGeometry.sphere(pose, vc(FxGeometry.light()), from, r, 8, 16, 0, 1, 1.0F, 1.0F, 1.0F, 0.45F * (1.0F - f), 1.5F, a * 0.5F, FULL, false, new Quaternionf().rotationY(a * 0.2F));
            }
            case SEISMIC -> {
                float r = e.data() * easeOut(f);
                FxGeometry.disc(pose, vc(FxGeometry.light()), from.add(0, 0.04, 0), Math.max(0.0F, r - 0.4F), r, 32, 0, 1, EARTH[0], EARTH[1], EARTH[2], 0.4F * (1.0F - f), 0.1F * (1.0F - f), FULL, false, 0);
            }
            case METAL_PULL -> metalPull(from, e, a, f);
            case QUICKSAND -> {
                Vec3 ground = entity != null ? rel(entity.getPosition(partial).add(0, 0.06, 0)) : from.add(0, 0.06, 0);
                float[] fr = {0, 1};
                float en = env(f * 1.1F);
                FxGeometry.disc(pose, vc(FxGeometry.snow()), ground, 0.0F, 1.4F, 20, fr[0], fr[1], SAND[0], SAND[1], SAND[2], 0.85F * en, 0.5F * en, lit, true, a * 0.25F);
                FxGeometry.disc(pose, vc(FxGeometry.snow()), ground.add(0, 0.02, 0), 0.4F, 1.1F, 16, fr[0], fr[1], SAND[0] * 0.8F, SAND[1] * 0.8F, SAND[2] * 0.8F, 0.7F * en, 0.3F * en, lit, true, -a * 0.4F);
                FxGeometry.ringBand(pose, vc(FxGeometry.snow()), ground, 1.3F, 0.25F, 20, fr[0], fr[1], SAND[0], SAND[1], SAND[2], 0.6F * en, lit, true, a * 0.3F, 0.6F);
            }
            case SUPPRESS -> {
                if (entity instanceof LivingEntity victim) {
                    Vec3 forehead = rel(victim.getEyePosition(partial).add(victim.getViewVector(partial).scale(0.25)).add(0, 0.12, 0));
                    float pulse = 0.5F + 0.5F * Mth.sin(a * 0.8F);
                    float en = env(f);
                    FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), forehead, 0.5F + 0.3F * pulse, 1.0F, 0.95F, 0.9F, 0.9F * en, a * 0.3F);
                    FxGeometry.sphere(pose, vc(FxGeometry.light()), rel(victim.getPosition(partial).add(0, victim.getBbHeight() * 0.5, 0)), victim.getBbWidth() * 0.9F, 6, 12, 0, 1,
                            BLOOD[0], BLOOD[1], BLOOD[2], 0.12F * en, 1.5F, a * 0.4F, FULL, false, new Quaternionf().rotationY(a * 0.1F));
                }
            }
            case LAVA_BURST -> {
                float[] fr = FxGeometry.frame(age, FxGeometry.LAVA_FRAMES, 0.5F);
                float r = e.data() * (0.3F + 0.7F * easeOut(f));
                FxGeometry.sphere(pose, vc(FxGeometry.lava()), from, r, 6, 12, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - f), 0, 0, FULL, true, new Quaternionf().rotationY(a * 0.4F));
                FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), from, r * 2.0F, 1.0F, 0.55F, 0.15F, 0.7F * (1.0F - f), a * 0.3F);
            }
            case STEP -> {
                Vec3 d = to.subtract(from);
                float[] rgb = elementRgb(e.color());
                Vec3 head = from.add(d.scale(Math.min(1.0F, f * 2.0F)));
                List<Vec3> pts = List.of(head.subtract(d.scale(0.5)), head);
                FxGeometry.ribbon(pose, vc(FxGeometry.light()), pts, 0.6F, 0.2F, 0, 1, 1, 0, rgb[0], rgb[1], rgb[2], 0.0F, 0.5F * (1.0F - f), FULL, false);
                FxGeometry.sphere(pose, vc(FxGeometry.light()), from, 0.8F * easeOut(f), 6, 12, 0, 1, rgb[0], rgb[1], rgb[2], 0.3F * (1.0F - f), 1.5F, a, FULL, false, null);
            }
            case VINE -> {
                Vec3 end = entity != null ? at : to;
                float reach = Math.min(1.0F, a / 4.0F);
                List<Vec3> pts = new ArrayList<>();
                Vec3 d = end.subtract(from);
                Vec3 side = right(d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : d.normalize());
                for (int k = 0; k <= 12; k++) {
                    float t = k / 12.0F * reach;
                    pts.add(from.add(d.scale(t)).add(side.scale(Math.sin(t * 8 + a * 0.5) * 0.25 * t)).add(0, Math.sin(t * Math.PI) * 0.4, 0));
                }
                FxGeometry.ribbon(pose, vc(FxGeometry.snow()), pts, 0.22F, 0.12F, 0, 1, 4, 0, 0.25F, 0.55F, 0.22F, 0.95F * (1.0F - f * f), 0.9F * (1.0F - f * f), lit, true);
                FxGeometry.tube(pose, vc(FxGeometry.snow()), pts, 0.06F, 0.04F, 5, 0, 1, 4, 0, 0.18F, 0.42F, 0.16F, 0.95F * (1.0F - f * f), 0.9F * (1.0F - f * f), lit, true);
            }
            case BLAZE_ARC -> {
                int blue = e.color();
                Vec3 dir = to.subtract(from);
                dir = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
                float reach = e.data() * easeOut(f * 1.2F);
                float[] fr = fireFrame(1.0F);
                float[] core = fireCore(blue);
                int segs = 14;
                VertexConsumer fire = vc(FxGeometry.fire(blue));
                for (int i = 0; i < segs; i++) {
                    double a0 = Math.toRadians(-40 + 80.0 * i / segs);
                    double a1 = Math.toRadians(-40 + 80.0 * (i + 1) / segs);
                    Vec3 p0 = from.add(dir.yRot((float) a0).scale(reach));
                    Vec3 p1 = from.add(dir.yRot((float) a1).scale(reach));
                    float h = 1.2F * (1.0F - f * 0.7F) * (0.8F + 0.2F * Mth.sin(a * 0.6F + i));
                    FxGeometry.tube(pose, fire, List.of(p0.add(0, 0.1, 0), p0.add(0, 0.1 + h, 0)), 0.5F, 0.1F, 6, fr[0], fr[1], 1, age * 0.2F, 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - f), 0.0F, FULL, true);
                    if (i == segs - 1) {
                        FxGeometry.tube(pose, fire, List.of(p1.add(0, 0.1, 0), p1.add(0, 0.1 + h, 0)), 0.5F, 0.1F, 6, fr[0], fr[1], 1, age * 0.2F, 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - f), 0.0F, FULL, true);
                    }
                }
                FxGeometry.disc(pose, vc(FxGeometry.light()), from.add(0, 0.08, 0), Math.max(0.0F, reach - 1.0F), reach, 24, 0, 1, core[0], core[1], core[2], 0.3F * (1.0F - f), 0.0F, FULL, false, 0);
            }
            case COMBUSTION -> {
                long seed = e.born() * 17 + now / 2;
                float beam = Mth.clamp(a / 3.0F, 0.0F, 1.0F);
                Vec3 head = from.lerp(to, beam);
                // The show's ray is a thin, nearly transparent shimmer with a faint orange-white heart, sparking like a firecracker.
                FxGeometry.bolt(pose, vc(FxGeometry.light()), from, head, seed, 0.05F, 0.06F * (1.0F - f * 0.5F), 1.0F, 0.85F, 0.6F, 0.7F * (1.0F - f), 0);
                FxGeometry.ribbon(pose, vc(FxGeometry.light()), List.of(from, head), 0.22F, 0.22F, 0, 1, 1, 0, 1.0F, 0.8F, 0.6F, 0.12F * (1.0F - f), 0.12F * (1.0F - f), FULL, false);
                net.minecraft.util.RandomSource sparks = net.minecraft.util.RandomSource.create(seed);
                int n = (int) (from.distanceTo(head) * 2);
                for (int k = 0; k < n; k++) {
                    Vec3 spark = from.lerp(head, sparks.nextDouble()).add((sparks.nextDouble() - 0.5) * 0.4, (sparks.nextDouble() - 0.5) * 0.4, (sparks.nextDouble() - 0.5) * 0.4);
                    FxGeometry.billboard(pose, vc(FxGeometry.glowTexture()), spark, 0.12F + sparks.nextFloat() * 0.15F, 0, 1, 1.0F, 0.95F, 0.7F, 0.9F * (1.0F - f), FULL, true, sparks.nextFloat() * 6);
                }
                if (beam >= 1.0F) {
                    float bf = Mth.clamp((a - 3.0F) / 9.0F, 0.0F, 1.0F);
                    float r = e.data() * 1.6F * easeOut(bf);
                    float[] fr = fireFrame(1.0F);
                    FxGeometry.sphere(pose, vc(FxGeometry.fire(false)), to, r, 8, 14, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - bf), 0, 0, FULL, true, new Quaternionf().rotationY(a * 0.4F));
                    FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), to, r * 2.5F, 1.0F, 0.75F, 0.35F, 0.9F * (1.0F - bf), a);
                    // A white flash at the heart of the blast as it goes off.
                    FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), to, r * 3.5F * (1.0F - bf), 1.0F, 1.0F, 1.0F, Math.max(0.0F, 1.0F - bf * 4.0F), -a);
                    FxGeometry.disc(pose, vc(FxGeometry.light()), to, r * 1.5F, r * 2.5F, 24, 0, 1, 1.0F, 0.8F, 0.5F, 0.4F * (1.0F - bf), 0.0F, FULL, false, a);
                }
            }
            case SHOCKWAVE -> {
                float r = e.data() * easeOut(f);
                FxGeometry.ringBand(pose, vc(FxGeometry.snow()), from.add(0, 0.05, 0), r, 1.0F * (1.0F - f * 0.8F), 32, 0, 1, EARTH[0] * 0.7F, EARTH[1] * 0.65F, EARTH[2] * 0.55F, 0.8F * (1.0F - f), lit, true, a * 0.3F, 0.35F);
                FxGeometry.disc(pose, vc(FxGeometry.light()), from.add(0, 0.06, 0), Math.max(0.0F, r - 0.8F), r, 32, 0, 1, EARTH[0], EARTH[1], EARTH[2], 0.35F * (1.0F - f), 0.0F, FULL, false, 0);
            }
            case EARTH_GRAB -> {
                Vec3 feet = entity != null ? rel(entity.getPosition(partial)) : from;
                float h = 1.0F * easeOut(f * 1.5F) * (1.0F - Math.max(0.0F, f - 0.7F) / 0.3F);
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3 + a * 0.05;
                    Vec3 c = feet.add(Math.cos(ang) * 0.7, 0.02, Math.sin(ang) * 0.7);
                    FxGeometry.ringBand(pose, vc(FxGeometry.snow()), c, 0.18F, h * (0.8F + 0.2F * Mth.sin(i * 2.1F)), 5, 0, 1, EARTH[0] * 0.7F, EARTH[1] * 0.65F, EARTH[2] * 0.55F, 0.9F, lit, true, i, 0.0F);
                }
            }
            case ICE_WAVE -> {
                Vec3 dir = to.subtract(from);
                dir = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
                float reach = e.data() * easeOut(f * 1.2F);
                Vec3 side = right(dir);
                int segs = 12;
                VertexConsumer ice = vc(FxGeometry.ice());
                for (int i = 0; i <= segs; i++) {
                    float t = (float) i / segs;
                    Vec3 c = from.add(dir.scale(reach)).add(side.scale((t - 0.5F) * reach * 1.2F)).add(0, 0.05, 0);
                    float h = (1.4F + 0.6F * Mth.sin(i * 1.3F)) * (1.0F - f * 0.6F);
                    FxGeometry.ringBand(pose, ice, c, 0.2F, h, 5, 0, 1, 0.85F, 0.95F, 1.0F, 0.9F * (1.0F - f * f), lit, true, i, 0.0F);
                }
                FxGeometry.ringBand(pose, vc(FxGeometry.light()), from.add(dir.scale(reach)), reach * 0.6F, 0.4F * (1.0F - f), 20, 0, 1, 0.8F, 0.95F, 1.0F, 0.35F * (1.0F - f), FULL, false, a, 0.3F);
            }
            case WATER_VORTEX -> waterVortex(from, to, e, a, f, lit);
            case ENERGYBEND -> {
                // The lion turtle's gift: the Avatar glows blue-white, the touched one orange-red, and columns of light rise from both.
                VertexConsumer light = vc(FxGeometry.light());
                float en = env(f);
                Vec3 touched = entity != null ? rel(entity.getPosition(partial)) : to;
                for (int side = 0; side < 2; side++) {
                    Vec3 spot = side == 0 ? from : touched;
                    float[] rgb = side == 0 ? new float[]{0.6F, 0.85F, 1.0F} : new float[]{1.0F, 0.55F, 0.2F};
                    FxGeometry.ringBand(pose, light, spot.add(0, 0.05, 0), 0.7F, 14.0F * en, 20, 0, 1, rgb[0], rgb[1], rgb[2], 0.45F * en, FULL, false, a * 0.2F, 0.1F);
                    FxGeometry.ringBand(pose, light, spot.add(0, 0.05, 0), 0.35F, 18.0F * en, 12, 0, 1, 1.0F, 1.0F, 1.0F, 0.35F * en, FULL, false, -a * 0.3F, 0.0F);
                    FxGeometry.sphere(pose, light, spot.add(0, 1.0, 0), 1.2F, 8, 16, 0, 1, rgb[0], rgb[1], rgb[2], 0.25F * en, 1.5F, a * 0.4F, FULL, false, new Quaternionf().rotationY(a * 0.2F));
                    FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), spot.add(0, 1.6, 0), 2.5F, rgb[0], rgb[1], rgb[2], 0.7F * en, a * 0.3F);
                }
            }
            case SPIRITBEND -> {
                // Spiritbending as Korra does it: a spiral of golden light winding up and round the creature, softening to white.
                VertexConsumer light = vc(FxGeometry.light());
                float en = env(f);
                float h = (float) (to.y - from.y);
                Vec3 c = entity != null ? rel(entity.getPosition(partial)) : from;
                for (int i = 0; i < 2; i++) {
                    List<Vec3> pts = new ArrayList<>();
                    for (int k = 0; k <= 24; k++) {
                        float t = k / 24.0F;
                        double ang = t * Math.PI * 4 + a * 0.3 + i * Math.PI;
                        double r = e.data() * 0.9 + 0.5 + 0.2 * Math.sin(a * 0.5 + k);
                        pts.add(c.add(Math.cos(ang) * r, h * t, Math.sin(ang) * r));
                    }
                    FxGeometry.ribbon(pose, light, pts, 0.22F, 0.08F, 0, 1, 1, 0, 1.0F, 0.85F, 0.35F, 0.6F * en, 0.2F * en, FULL, false);
                    FxGeometry.puffs(pose, vc(FxGeometry.glowTexture()), pts, 0.5F, 0.3F, 0, 1, 1.0F, 0.9F, 0.5F, 0.45F * en, 0.2F * en, FULL, true, a * 0.2F);
                }
                FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), c.add(0, h * 0.5, 0), 1.6F + h * 0.5F, 1.0F, 0.92F, 0.6F, 0.4F * en, a);
            }
            case OCTOPUS_LASH -> {
                // An arm of the octopus form lashing out: a thick tendril of water that snaps to its mark and back.
                float reach = env(f);
                List<Vec3> pts = new ArrayList<>();
                Vec3 d = to.subtract(from);
                Vec3 side = right(d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : d.normalize());
                for (int k = 0; k <= 10; k++) {
                    float t = k / 10.0F * reach;
                    pts.add(from.add(d.scale(t)).add(side.scale(Math.sin(t * Math.PI * 2 + a * 0.8) * 0.3 * t)).add(0, Math.sin(t * Math.PI) * 0.4, 0));
                }
                float[] wfr = waterFrame();
                FxGeometry.tube(pose, vc(FxGeometry.water()), pts, 0.2F, 0.06F, 8, wfr[0], wfr[1], 2.0F, a * 0.1F, WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.9F, 0.7F, lit, true);
                FxGeometry.ribbon(pose, vc(FxGeometry.light()), pts, 0.3F, 0.1F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.2F, 0.1F, FULL, false);
                FxGeometry.puffs(pose, vc(FxGeometry.water()), pts, 0.35F, 0.15F, wfr[0], wfr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.6F, 0.5F, lit, true, a * 0.3F);
            }
            case FIRE_SPIN -> {
                int blue = e.color();
                float r = e.data() * easeOut(f);
                float[] fr = fireFrame(1.0F);
                float[] core = fireCore(blue);
                VertexConsumer fire = vc(FxGeometry.fire(blue));
                for (int i = 0; i < 4; i++) {
                    List<Vec3> pts = new ArrayList<>();
                    for (int k = 0; k <= 16; k++) {
                        float t = k / 16.0F;
                        double ang = i * Math.PI / 2 + a * 0.8 + t * 4.0;
                        double rr = r * t;
                        pts.add(from.add(Math.cos(ang) * rr, 0.6 + Math.sin(t * 7 + a) * 0.3, Math.sin(ang) * rr));
                    }
                    FxGeometry.ribbon(pose, fire, pts, 0.9F, 0.3F, fr[0], fr[1], 3.0F, a * 0.2F, 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - f), 0.5F * (1.0F - f), FULL, true);
                    FxGeometry.ribbon(pose, vc(FxGeometry.light()), pts, 0.25F, 0.05F, 0, 1, 1, 0, core[0], core[1], core[2], 0.6F * (1.0F - f), 0.1F * (1.0F - f), FULL, false);
                }
            }
            case EXTRACT -> {
                float[] fr = {0, 1};
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4 + a * 0.3;
                    double rr = 0.6 * (1.0F - f);
                    Vec3 c = from.add(Math.cos(ang) * rr, Math.sin(a * 0.4 + i) * 0.2, Math.sin(ang) * rr);
                    FxGeometry.billboard(pose, vc(FxGeometry.glowTexture()), c, 0.25F, fr[0], fr[1], METAL[0], METAL[1], METAL[2], 0.9F * (1.0F - f), FULL, true, a * 0.5F + i);
                }
            }
            case AVATAR_RISE -> {
                // The burst as he rises: the one who rises sees it go out from him, not a white wall in his eyes.
                boolean near = from.lengthSqr() < 9.0;
                float r = e.data() * easeOut(f);
                // From inside, the sphere and a tall band would be a wall of white round him: he sees a low ring go out.
                if (!near) {
                    FxGeometry.sphere(pose, vc(FxGeometry.light()), from.add(0, 1.0, 0), r, 10, 20, 0, 1, 0.8F, 0.92F, 1.0F, 0.4F * (1.0F - f), 1.5F, a * 0.5F, FULL, false, new Quaternionf().rotationY(a * 0.3F));
                }
                FxGeometry.ringBand(pose, vc(FxGeometry.light()), from.add(0, 0.05, 0), r * 1.3F, (near ? 0.5F : 2.0F) * (1.0F - f), 32, 0, 1, 1.0F, 1.0F, 1.0F, (near ? 0.3F : 0.5F) * (1.0F - f), FULL, false, a, 0.3F);
                if (!near) {
                    FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), from.add(0, 1.6, 0), 3.0F * (1.0F - f * 0.5F), 0.85F, 0.95F, 1.0F, 0.9F * (1.0F - f), a);
                }
            }
            case CHAKRA -> {
                float[] rgb = CHAKRA_RGB[Mth.clamp((int) e.data() - 1, 0, 6)];
                float y = (float) (to.y - from.y) * easeOut(f);
                Vec3 ring = from.add(0, y, 0);
                FxGeometry.disc(pose, vc(FxGeometry.light()), ring, 0.5F, 0.9F + 0.3F * Mth.sin(a * 0.3F), 24, 0, 1, rgb[0], rgb[1], rgb[2], 0.6F * (1.0F - f), 0.0F, FULL, false, a * 0.2F);
                FxGeometry.ringBand(pose, vc(FxGeometry.light()), from.add(0, 0.05, 0), 1.2F, y, 24, 0, 1, 1.0F, 0.95F, 0.85F, 0.25F * (1.0F - f), FULL, false, a * 0.3F, 0.2F);
                FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), ring, 1.2F, 1.0F, 0.95F, 0.85F, 0.7F * (1.0F - f), a);
            }
            case LION_GIFT -> lionGift(e, f, a, from, to, entity);
            case EARTH_RISE -> {
                float r = e.data() * easeOut(f);
                FxGeometry.ringBand(pose, vc(FxGeometry.snow()), from.add(0, 0.05, 0), r, 0.9F * (1.0F - f), 20, 0, 1, EARTH[0] * 0.7F, EARTH[1] * 0.65F, EARTH[2] * 0.6F, 0.6F * (1.0F - f), lit, true, a * 0.3F, 0.4F);
            }
        }
    }

    /**
     * The water vortex: a column of water twisting up from the ground, as the one Aang rode up a hull: a thick body
     * of flowing water wound with white froth, chunks of foam flung off it, and spray at the foot and the crown.
     */
    private static void waterVortex(Vec3 from, Vec3 to, WorldFx.Effect e, float a, float f, int lit) {
        float grow = easeOut(Mth.clamp(a / 10.0F, 0.0F, 1.0F)) * (f > 0.85F ? (1.0F - f) / 0.15F : 1.0F);
        if (grow <= 0.01F) {
            return;
        }
        float radius = e.data() * 0.55F * grow;
        float height = (float) (to.y - from.y) * grow;
        float[] fr = waterFrame();
        VertexConsumer flow = vc(FxGeometry.flow());
        VertexConsumer water = vc(FxGeometry.water());
        VertexConsumer light = vc(FxGeometry.light());
        int n = Math.max(6, (int) (height * 2));
        List<Vec3> spine = new ArrayList<>();
        for (int k = 0; k <= n; k++) {
            float t = (float) k / n;
            double ang = a * 0.3 + t * 4;
            spine.add(from.add(Math.cos(ang) * 0.15 * t, height * t, Math.sin(ang) * 0.15 * t));
        }
        FxGeometry.tube(pose, flow, spine, radius * 0.6F, radius, 14, fr[0], fr[1], height * 0.6F, -a * 0.12F, WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.9F, 0.75F, lit, true);
        // White froth winding up the body, and foam flung off it.
        for (int i = 0; i < 4; i++) {
            List<Vec3> rope = new ArrayList<>();
            for (int k = 0; k <= n * 2; k++) {
                float t = (float) k / (n * 2);
                double ang = i * 1.57 - a * 0.45 + t * 10;
                double r = radius * (0.65 + 0.4 * t);
                rope.add(from.add(Math.cos(ang) * r, height * t, Math.sin(ang) * r));
            }
            FxGeometry.ribbon(pose, light, rope, 0.14F, 0.08F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.55F, 0.7F, FULL, false);
            FxGeometry.puffs(pose, vc(FxGeometry.snow()), rope.subList(0, rope.size() / 2 + 1), 0.3F, 0.5F, 0, 1, 0.95F, 0.98F, 1.0F, 0.0F, 0.75F, lit, true, a * 0.2F + i);
        }
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(e.born() + (long) (a / 3));
        for (int i = 0; i < 8; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double h = rnd.nextDouble() * height;
            double r = radius * (1.1 + rnd.nextDouble() * 0.6);
            FxGeometry.billboard(pose, vc(FxGeometry.snow()), from.add(Math.cos(ang) * r, h, Math.sin(ang) * r), 0.15F + rnd.nextFloat() * 0.2F, 0, 1, 0.95F, 0.98F, 1.0F, 0.8F, lit, true, rnd.nextFloat() * 6);
        }
        FxGeometry.disc(pose, water, from.add(0, 0.05, 0), radius * 0.5F, radius * 1.9F, 24, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.75F, 0.0F, lit, true, a * 0.15F);
        FxGeometry.ringBand(pose, light, from.add(0, 0.1, 0), radius * 1.4F, 0.5F, 24, 0, 1, 1.0F, 1.0F, 1.0F, 0.35F, FULL, false, a * 0.5F, 0.6F);
        FxGeometry.ringBand(pose, water, from.add(0, height - 0.3, 0), radius * 1.15F, 0.7F, 20, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.7F, lit, true, -a * 0.4F, 0.5F);
    }

    private static float[] elementRgb(int color) {
        return new float[]{((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F};
    }

    private static void waterWhip(Vec3 from, Vec3 to, float a, float f, int lit) {
        Vec3 d = to.subtract(from);
        Vec3 look = d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : d.normalize();
        Vec3 side = right(look);
        Vec3 up = side.cross(look);
        float reach = Math.min(1.0F, a / 4.0F);
        List<Vec3> pts = new ArrayList<>();
        for (int k = 0; k <= 14; k++) {
            float t = k / 14.0F * reach;
            Vec3 at = from.add(d.scale(t)).add(side.scale(Math.sin(t * Math.PI * 3 + a * 0.9) * 0.5 * t * (1.0F - f)))
                    .add(up.scale(Math.cos(t * 4 + a * 0.7) * 0.25 * t * (1.0F - f)));
            pts.add(at);
        }
        float[] fr = waterFrame();
        float alpha = 0.9F * (1.0F - f * f);
        FxGeometry.ribbon(pose, vc(FxGeometry.water()), pts, 0.36F, 0.16F, fr[0], fr[1], (float) d.length() / 1.2F, a * 0.2F, WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], alpha, alpha * 0.8F, lit, true);
        FxGeometry.puffs(pose, vc(FxGeometry.water()), pts, 0.5F, 0.28F, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], alpha * 0.8F, alpha * 0.7F, lit, true, a * 0.2F);
        // The show draws a thin near-white rim along every stream: a wider, faint band of light behind the water.
        FxGeometry.ribbon(pose, vc(FxGeometry.light()), pts, 0.42F, 0.2F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.22F * (1.0F - f), 0.12F * (1.0F - f), FULL, false);
        FxGeometry.ribbon(pose, vc(FxGeometry.light()), pts, 0.08F, 0.03F, 0, 1, 1, 0, WATER_GLOW[0], WATER_GLOW[1], WATER_GLOW[2], 0.45F * (1.0F - f), 0.2F * (1.0F - f), FULL, false);
        if (reach >= 1.0F) {
            float sf = Mth.clamp((a - 4.0F) / 6.0F, 0.0F, 1.0F);
            FxGeometry.ringBand(pose, vc(FxGeometry.water()), to, 0.2F + 0.8F * easeOut(sf), 0.5F * (1.0F - sf), 12, fr[0], fr[1], WATER_TINT[0], WATER_TINT[1], WATER_TINT[2], 0.7F * (1.0F - sf), lit, true, a * 0.5F, 0.5F);
        }
    }

    private static void healing(WorldFx.Effect e, Vec3 at, Entity entity, float a, float f, int lit) {
        boolean fire = e.kind() == com.kolodkingal.enlightened.fx.Fx.FIRE_HEAL;
        int blue = fire ? e.color() : 0;
        float en = env(f);
        float height = entity != null ? entity.getBbHeight() : 1.8F;
        float[] fr = fire ? fireFrame(0.7F) : waterFrame();
        VertexConsumer body = fire ? vc(FxGeometry.fire(blue)) : vc(FxGeometry.water());
        float[] tint = fire ? new float[]{1, 1, 1} : WATER_TINT;
        float[] core = fire ? fireCore(blue) : HEAL_GLOW;
        for (int s = 0; s < 2; s++) {
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= 18; k++) {
                float t = k / 18.0F;
                double ang = t * Math.PI * 2.2 + a * 0.25 + s * Math.PI;
                double r = 0.45 + height * 0.25;
                double y = -height * 0.45 + height * 0.9 * t + Math.sin(t * 6 + a * 0.3) * 0.08;
                pts.add(at.add(Math.cos(ang) * r, y, Math.sin(ang) * r));
            }
            FxGeometry.ribbon(pose, body, pts, 0.22F, 0.10F, fr[0], fr[1], 2.5F, a * 0.1F, tint[0], tint[1], tint[2], 0.85F * en, 0.5F * en, fire ? FULL : lit, true);
            FxGeometry.ribbon(pose, vc(FxGeometry.light()), pts, 0.07F, 0.02F, 0, 1, 1, 0, core[0], core[1], core[2], 0.4F * en, 0.1F * en, FULL, false);
        }
        FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), at, 0.9F + 0.2F * Mth.sin(a * 0.6F), core[0], core[1], core[2], 0.35F * en, a * 0.2F);
        if (!fire) {
            // The show's healing water glows silvery-blue, with faint rays and ripple rings spreading from it.
            VertexConsumer light = vc(FxGeometry.light());
            for (int i = 0; i < 7; i++) {
                double ang = i * Math.PI * 2 / 7 + a * 0.08;
                double len = 0.9 + 0.3 * Math.sin(a * 0.5 + i);
                Vec3 dir = new Vec3(Math.cos(ang) * 0.8, Math.sin(ang * 1.7) * 0.5, Math.sin(ang) * 0.8).normalize();
                FxGeometry.ribbon(pose, light, List.of(at, at.add(dir.scale(len))), 0.08F, 0.0F, 0, 1, 1, 0, core[0], core[1], core[2], 0.35F * en, 0.0F, FULL, false);
            }
            float ripple = (a * 0.06F) % 1.0F;
            FxGeometry.ringBand(pose, light, at.add(0, -height * 0.45, 0), 0.4F + ripple * 1.3F, 0.08F, 24, 0, 1, core[0], core[1], core[2], 0.3F * en * (1.0F - ripple), FULL, false, 0, 0);
        }
    }

    private static void freeze(Vec3 from, float radius, float a, float f, int lit) {
        float r = radius * easeOut(f);
        float[] fr = {0, 1};
        VertexConsumer ice = vc(FxGeometry.ice());
        FxGeometry.disc(pose, ice, from.add(0, 0.05, 0), 0, r, 24, fr[0], fr[1], 0.85F, 0.95F, 1.0F, 0.85F * (1.0F - f * 0.6F), 0.3F * (1.0F - f), lit, true, 0);
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create((long) (from.x * 31 + from.z * 17));
        int crystals = 6 + (int) (radius * 2);
        for (int i = 0; i < crystals; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double d = rnd.nextDouble() * radius * 0.85;
            float h = (0.5F + rnd.nextFloat() * 0.9F) * easeOut(Mth.clamp(f * 1.5F - i * 0.03F, 0, 1));
            Vec3 c = from.add(Math.cos(ang) * d, 0.02, Math.sin(ang) * d);
            FxGeometry.ringBand(pose, ice, c, 0.12F + rnd.nextFloat() * 0.1F, h, 5, fr[0], fr[1], 0.85F, 0.95F, 1.0F, 0.9F * (1.0F - f * f), lit, true, i, 0.0F);
        }
        FxGeometry.ringBand(pose, vc(FxGeometry.light()), from, r, 0.3F * (1.0F - f), 24, 0, 1, 0.8F, 0.95F, 1.0F, 0.35F * (1.0F - f), FULL, false, a, 0.3F);
        // The show: a pale flash runs over the water as it goes rigid, and little four-point sparkles crackle at the edge.
        if (f < 0.3F) {
            FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), from.add(0, 0.4, 0), radius * 2.0F, 1.0F, 1.0F, 1.0F, 0.8F * (1.0F - f / 0.3F), a);
        }
        for (int i = 0; i < 8; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            Vec3 at = from.add(Math.cos(ang) * r, 0.1 + rnd.nextDouble() * 0.8, Math.sin(ang) * r);
            FxGeometry.billboard(pose, vc(FxGeometry.glowTexture()), at, 0.18F, 0, 1, 1.0F, 1.0F, 1.0F, 0.9F * (1.0F - f) * (0.5F + 0.5F * Mth.sin(a * 2 + i)), FULL, true, i);
        }
    }

    private static void fireRing(Vec3 from, WorldFx.Effect e, float a, float f) {
        int blue = e.color();
        float radius = 0.5F + (e.data() - 0.5F) * easeOut(f * 1.3F);
        float[] fr = fireFrame(1.0F);
        float[] core = fireCore(blue);
        float height = 1.4F * (1.0F - f * 0.6F);
        Vec3 c = from.add(0, 0.05, 0);
        FxGeometry.ringBand(pose, vc(FxGeometry.fire(blue)), c, radius, height, 36, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.95F * (1.0F - f), FULL, true, a * 0.6F, 0.3F);
        FxGeometry.ringBand(pose, vc(FxGeometry.fire(blue)), c, radius * 0.8F, height * 0.7F, 28, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.7F * (1.0F - f), FULL, true, -a * 0.5F, 0.4F);
        FxGeometry.disc(pose, vc(FxGeometry.light()), c, Math.max(0.0F, radius - 0.8F), radius, 36, 0, 1, core[0], core[1], core[2], 0.35F * (1.0F - f), 0.0F, FULL, false, 0);
    }

    /**
     * Lightning, after Thor's beam in Enhanced ({@link ThorBolt}): the bolt from the firebender's hand to where it
     * strikes, writhing as it crackles, then gone. If the caster is in sight it leaves his hand where his hand is now.
     */
    private static void lightningBolt(Vec3 from, Vec3 to, WorldFx.Effect e, float a, float f) {
        float alpha = f < 0.7F ? 1.0F : (1.0F - f) / 0.3F;
        ThorBolt.draw(pose, vc(FxGeometry.light()), from, to, e.born() * 104729L, a, alpha, 1.6F, FULL);
        if (f < 0.25F) {
            FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), to, 2.0F * (1.0F - f * 4.0F), ThorBolt.BLUE[0], ThorBolt.BLUE[1], ThorBolt.BLUE[2], 0.6F * (1.0F - f * 4.0F), a);
        }
    }

    private static void airBlast(Vec3 from, Vec3 to, WorldFx.Effect e, float a, float f) {
        Vec3 d = to.subtract(from);
        if (d.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 dir = d.normalize();
        Vec3 head = from.add(d.scale(easeOut(f)));
        float alpha = 0.35F * (1.0F - f);
        VertexConsumer light = vc(FxGeometry.light());
        FxGeometry.cone(pose, light, head, dir.scale(-1.0), 2.6F, 0.9F, 0.15F, 10, 6, 0, 1, 1, 0, AIR[0], AIR[1], AIR[2], alpha, 0.0F, FULL, false, 0.2F, a * 0.8F);
        FxGeometry.conePuffs(pose, vc(FxGeometry.glowTexture()), head, dir.scale(-1.0), 2.6F, 0.9F, 0.2F, 5, 0, 1, AIR[0], AIR[1], AIR[2], alpha * 1.2F, 0.0F, FULL, true, 0.2F, a * 0.8F, 1.2F, a * 0.3F);
        Vec3 any = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 n = any.cross(dir).normalize();
        Vec3 bn = dir.cross(n);
        for (int i = 0; i < 3; i++) {
            List<Vec3> pts = new ArrayList<>();
            for (int k = 0; k <= 10; k++) {
                float t = k / 10.0F;
                double ang = a * 0.9 + i * 2.1 + t * 7;
                double r = 0.2 + 0.7 * t;
                pts.add(head.subtract(dir.scale(t * 2.4)).add(n.scale(Math.cos(ang) * r)).add(bn.scale(Math.sin(ang) * r)));
            }
            FxGeometry.ribbon(pose, light, pts, 0.12F, 0.03F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.5F * (1.0F - f), 0.0F, FULL, false);
        }
        FxGeometry.glowBlob(pose, vc(FxGeometry.glowTexture()), head, 0.8F, AIR[0], AIR[1], AIR[2], 0.3F * (1.0F - f), a * 0.5F);
    }

    private static void metalPull(Vec3 from, WorldFx.Effect e, float a, float f) {
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(e.born());
        float pull = 1.0F - f * f;
        VertexConsumer light = vc(FxGeometry.light());
        VertexConsumer glow = vc(FxGeometry.glowTexture());
        for (int i = 0; i < 24; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double r = e.data() * (0.4 + rnd.nextDouble() * 0.6) * pull;
            double y = rnd.nextDouble() * 1.6 - 0.2;
            Vec3 at = from.add(Math.cos(ang) * r, y * (0.3 + pull * 0.7), Math.sin(ang) * r);
            Vec3 tail = from.add(Math.cos(ang) * (r + 0.6 * pull), y * (0.3 + pull * 0.7), Math.sin(ang) * (r + 0.6 * pull));
            FxGeometry.ribbon(pose, light, List.of(at, tail), 0.06F, 0.01F, 0, 1, 1, 0, METAL[0], METAL[1], METAL[2], 0.8F * (1.0F - f * 0.5F), 0.0F, FULL, false);
            FxGeometry.billboard(pose, glow, at, 0.22F, 0, 1, METAL[0], METAL[1], METAL[2], 0.9F * (1.0F - f * 0.5F), FULL, true, a * 0.3F + i);
        }
    }

    private WorldFxRenderer() {
    }
}
