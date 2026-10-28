package com.kolodkingal.enlightened.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightning drawn as Thor's beam is in Enhanced ({@code ThorEffects.beam}): a jagged bolt from the hand with a white-hot
 * core, an electric-blue body and a deep blue halo, writhing from one shape to the next instead of jumping, loose
 * strands crackling beside it, short forks branching off, and flares where it leaves the hand and where it strikes. It
 * starts thin, so a bolt that leaves the camera's own hand never fills the screen. Positions are camera-relative.
 */
final class ThorBolt {
    static final float[] CORE = {1.0F, 1.0F, 1.0F};
    static final float[] BLUE = {0x4D / 255.0F, 0xB8 / 255.0F, 1.0F};
    static final float[] HALO = {0x1E / 255.0F, 0x6B / 255.0F, 1.0F};
    /** How long one shape of the bolt lasts before it has flowed into the next (ticks). */
    private static final float SHAPE_TICKS = 1.5F;

    static void draw(PoseStack.Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, long seed, float time, float alpha, float scale, int light) {
        double length = a.distanceTo(b);
        if (length < 0.05 || alpha <= 0.0F) {
            return;
        }
        float flicker = 0.85F + 0.15F * Mth.sin(time * 2.9F) * Mth.cos(time * 1.3F);
        float phase = time / SHAPE_TICKS;
        double jitter = Math.max(0.12, Math.min(1.2, length * 0.035)) * 1.15;
        // A soft glow all along the line.
        FxGeometry.ribbon(pose, vc, List.of(a, b), 0.12F * scale, 0.16F * scale, 0, 1, 1, 0, HALO[0], HALO[1], HALO[2], 0.10F * flicker * alpha, 0.14F * flicker * alpha, light, false);
        List<Vec3> path = flowingPath(a, b, jitter, seed, phase);
        tapered(pose, vc, path, 0.30F * scale, HALO, 0.30F * flicker * alpha, light);
        tapered(pose, vc, path, 0.15F * scale, BLUE, 0.75F * flicker * alpha, light);
        tapered(pose, vc, path, 0.055F * scale, CORE, 0.97F * alpha, light);
        Vec3 dir = b.subtract(a).scale(1.0 / length);
        long step = Mth.floor(phase);
        float f = flow(phase);
        for (int s = 0; s < 2; s++) {
            float weight = s == 0 ? 1.0F - f : f;
            if (weight <= 0.0F) {
                continue;
            }
            RandomSource random = shape(seed + 31L, step + s);
            // Loose strands that crackle round the main bolt.
            for (int i = 0; i < 2; i++) {
                List<Vec3> strand = lightningPath(a, b, jitter * 1.6, random);
                tapered(pose, vc, strand, 0.05F * scale, BLUE, 0.55F * weight * alpha, light);
            }
            // Forks.
            for (int i = 0; i < 3 && path.size() > 3; i++) {
                Vec3 start = path.get(1 + random.nextInt(path.size() - 2));
                double reach = 0.5 + random.nextDouble() * Math.min(2.0, length * 0.15);
                Vec3 side = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
                Vec3 end = start.add(dir.scale(reach * 0.6)).add(side.scale(reach * 0.8));
                List<Vec3> fork = lightningPath(start, end, reach * 0.2, random);
                FxGeometry.ribbon(pose, vc, fork, 0.06F * scale, 0.02F * scale, 0, 1, 1, 0, BLUE[0], BLUE[1], BLUE[2], 0.6F * weight * alpha, 0.3F * weight * alpha, light, false);
                FxGeometry.ribbon(pose, vc, fork, 0.02F * scale, 0.01F * scale, 0, 1, 1, 0, CORE[0], CORE[1], CORE[2], 0.8F * weight * alpha, 0.4F * weight * alpha, light, false);
            }
        }
        // Flares where it leaves (smaller the closer it is to the camera) and where it strikes.
        float near = (float) Mth.clamp(a.length() / 2.5, 0.12, 1.0);
        flare(pose, vc, a, 0.2F * scale * flicker * near, 0.9F * alpha, light);
        flare(pose, vc, b, 0.34F * scale * flicker, alpha, light);
    }

    /** The path as a strip a quarter as wide at its start, growing to the full width three blocks along. */
    private static void tapered(PoseStack.Pose pose, VertexConsumer vc, List<Vec3> path, float width, float[] rgb, float alpha, int light) {
        double along = 0.0;
        for (int i = 1; i < path.size(); i++) {
            Vec3 p = path.get(i - 1);
            Vec3 q = path.get(i);
            double next = along + p.distanceTo(q);
            FxGeometry.ribbon(pose, vc, List.of(p, q), taper(width, along), taper(width, next), 0, 1, 1, 0, rgb[0], rgb[1], rgb[2], alpha, alpha, light, false);
            along = next;
        }
    }

    private static float taper(float width, double along) {
        return width * (float) Math.min(1.0, 0.25 + along / 4.0);
    }

    /** A glow facing the camera: rings of blue, a white-hot heart, four thin spikes through it. */
    private static void flare(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, float radius, float alpha, int light) {
        Vec3 toCam = c.scale(-1.0);
        Vec3 side = toCam.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(toCam.normalize()).normalize();
        ring(pose, vc, c, side, up, radius * 0.8F, radius * 0.45F, HALO, 0.16F * alpha, light);
        ring(pose, vc, c, side, up, radius * 0.5F, radius * 0.4F, BLUE, 0.3F * alpha, light);
        ring(pose, vc, c, side, up, radius * 0.22F, radius * 0.3F, CORE, 0.85F * alpha, light);
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 4.0;
            Vec3 spike = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).scale(radius * (i % 2 == 0 ? 1.5 : 1.0));
            FxGeometry.ribbon(pose, vc, List.of(c.subtract(spike), c.add(spike)), radius * 0.07F, radius * 0.07F, 0, 1, 1, 0, BLUE[0], BLUE[1], BLUE[2], 0.6F * alpha, 0.6F * alpha, light, false);
        }
    }

    private static void ring(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, Vec3 u, Vec3 v, float radius, float width, float[] rgb, float alpha, int light) {
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i <= 14; i++) {
            double t = Math.PI * 2 * i / 14;
            pts.add(c.add(u.scale(Math.cos(t) * radius)).add(v.scale(Math.sin(t) * radius)));
        }
        FxGeometry.ribbon(pose, vc, pts, width, width, 0, 1, 1, 0, rgb[0], rgb[1], rgb[2], alpha, alpha, light, false);
    }

    // ---- the paths (Enhanced's EnergyGeometry) ----

    private static RandomSource shape(long seed, long step) {
        return RandomSource.create(seed + step * 0x9E3779B97F4A7C15L);
    }

    private static float flow(float phase) {
        float f = phase - Mth.floor(phase);
        return f * f * (3.0F - 2.0F * f);
    }

    /** A jagged path that flows from one random shape to the next as {@code phase} grows by one. */
    static List<Vec3> flowingPath(Vec3 a, Vec3 b, double jitter, long seed, float phase) {
        long step = Mth.floor(phase);
        List<Vec3> from = lightningPath(a, b, jitter, shape(seed, step));
        List<Vec3> to = lightningPath(a, b, jitter, shape(seed, step + 1));
        float f = flow(phase);
        if (from.size() != to.size()) {
            return f < 0.5F ? from : to;
        }
        List<Vec3> path = new ArrayList<>(from.size());
        for (int i = 0; i < from.size(); i++) {
            path.add(from.get(i).lerp(to.get(i), f));
        }
        return path;
    }

    /** A jagged path by midpoint displacement: kinked at every scale, the same for the same random source. */
    static List<Vec3> lightningPath(Vec3 a, Vec3 b, double jitter, RandomSource random) {
        List<Vec3> points = new ArrayList<>();
        points.add(a);
        int depth = Mth.clamp(Mth.ceillog2(Math.max(2, (int) (a.distanceTo(b) * 3.0))), 1, 6);
        subdivide(points, a, b, jitter, depth, random);
        return points;
    }

    private static void subdivide(List<Vec3> points, Vec3 a, Vec3 b, double jitter, int depth, RandomSource random) {
        if (depth == 0) {
            points.add(b);
            return;
        }
        Vec3 dir = b.subtract(a);
        Vec3 perp1 = dir.cross(new Vec3(0, 1, 0));
        if (perp1.lengthSqr() < 1.0E-6) {
            perp1 = dir.cross(new Vec3(1, 0, 0));
        }
        perp1 = perp1.normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();
        Vec3 mid = a.add(b).scale(0.5).add(perp1.scale((random.nextDouble() - 0.5) * 2 * jitter)).add(perp2.scale((random.nextDouble() - 0.5) * 2 * jitter));
        subdivide(points, a, mid, jitter * 0.5, depth - 1, random);
        subdivide(points, mid, b, jitter * 0.5, depth - 1, random);
    }

    private ThorBolt() {
    }
}
