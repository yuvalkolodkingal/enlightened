package com.kolodkingal.enlightened.client.fx;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.List;

/**
 * The geometry every element effect is built from, drawn with the game's own element textures (the animated water,
 * fire, soul fire and lava of the blocks, the ice, the snow) or as additive light: ribbons that face the camera,
 * tubes along a path, spheres, cones, rings, discs, billboards and jagged bolts. Every position is relative to the
 * camera (the world position minus the camera's), which is what the level renderer's pose stack expects.
 */
public final class FxGeometry {
    public static final int FULL = 0xF000F0;
    public static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("textures/block/water_still.png");
    public static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("textures/block/water_flow.png");
    public static final ResourceLocation FIRE = ResourceLocation.withDefaultNamespace("textures/block/fire_0.png");
    public static final ResourceLocation SOUL_FIRE = ResourceLocation.withDefaultNamespace("textures/block/soul_fire_0.png");
    public static final ResourceLocation LAVA = ResourceLocation.withDefaultNamespace("textures/block/lava_still.png");
    public static final ResourceLocation ICE = ResourceLocation.withDefaultNamespace("textures/block/ice.png");
    public static final ResourceLocation PACKED_ICE = ResourceLocation.withDefaultNamespace("textures/block/packed_ice.png");
    public static final ResourceLocation SNOW = ResourceLocation.withDefaultNamespace("textures/block/snow.png");
    /** A soft radial glow of our own (the game's particle glow is an 8x8 sprite that draws as a square). */
    public static final ResourceLocation GLOW = com.kolodkingal.enlightened.Enlightened.id("textures/fx/glow.png");
    /** Frames of the animated textures (their height over their width). */
    public static final int WATER_FRAMES = 32;
    public static final int FIRE_FRAMES = 32;
    public static final int LAVA_FRAMES = 20;

    private static final RenderType WATER_TYPE = RenderType.entityTranslucentCull(WATER);
    private static final RenderType FLOW_TYPE = RenderType.entityTranslucentCull(WATER_FLOW);
    private static final RenderType FIRE_TYPE = RenderType.entityTranslucentEmissive(FIRE);
    private static final RenderType SOUL_FIRE_TYPE = RenderType.entityTranslucentEmissive(SOUL_FIRE);
    private static final RenderType LAVA_TYPE = RenderType.entityTranslucentEmissive(LAVA);
    private static final RenderType ICE_TYPE = RenderType.entityTranslucentCull(ICE);
    private static final RenderType SNOW_TYPE = RenderType.entityTranslucentCull(SNOW);
    private static final RenderType GLOW_TYPE = RenderType.entityTranslucentEmissive(GLOW);

    public static RenderType water() {
        return WATER_TYPE;
    }

    public static RenderType flow() {
        return FLOW_TYPE;
    }

    public static RenderType fire(boolean blue) {
        return blue ? SOUL_FIRE_TYPE : FIRE_TYPE;
    }

    public static RenderType lava() {
        return LAVA_TYPE;
    }

    public static RenderType ice() {
        return ICE_TYPE;
    }

    public static RenderType snow() {
        return SNOW_TYPE;
    }

    public static RenderType glowTexture() {
        return GLOW_TYPE;
    }

    /** Additive light, untextured: the heart of a flame, a bolt, the shine of air. */
    private static MultiBufferSource.BufferSource source;

    /**
     * A buffer source of our own with a fixed buffer per element type, so several types can be built at once (a
     * stream of fire and its glow, water and its highlights). The game's shared source ends a batch the moment another
     * type is asked for, which would throw on the next vertex of the first.
     */
    public static MultiBufferSource.BufferSource source() {
        if (source == null) {
            SequencedMap<RenderType, ByteBufferBuilder> fixed = new LinkedHashMap<>();
            for (RenderType type : List.of(WATER_TYPE, FLOW_TYPE, ICE_TYPE, SNOW_TYPE, LAVA_TYPE, FIRE_TYPE, SOUL_FIRE_TYPE, GLOW_TYPE, RenderType.lightning())) {
                fixed.put(type, new ByteBufferBuilder(type.bufferSize()));
            }
            source = MultiBufferSource.immediateWithBuffers(fixed, new ByteBufferBuilder(786432));
        }
        return source;
    }

    public static RenderType light() {
        return RenderType.lightning();
    }

    /** The two texture rows (v0, v1) of frame {@code age * speed} of an animated texture with this many frames. */
    public static float[] frame(float age, int frames, float speed) {
        int f = ((int) Math.floor(age * speed)) % frames;
        if (f < 0) {
            f += frames;
        }
        return new float[]{(float) f / frames, (float) (f + 1) / frames};
    }

    // ---- vertices ----

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float r, float g, float b, float a,
                               float u, float v, int light, boolean textured) {
        if (textured) {
            vc.addVertex(pose.pose(), x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        } else {
            vc.addVertex(pose.pose(), x, y, z).setColor(r, g, b, a);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float[] uv, float[] rgba1, float[] rgba2,
                             int light, boolean textured) {
        // a-b along the first edge (alpha 1), c-d along the second (alpha 2); drawn both ways, so it is seen from either side.
        vertex(pose, vc, (float) a.x, (float) a.y, (float) a.z, rgba1[0], rgba1[1], rgba1[2], rgba1[3], uv[0], uv[1], light, textured);
        vertex(pose, vc, (float) b.x, (float) b.y, (float) b.z, rgba1[0], rgba1[1], rgba1[2], rgba1[3], uv[2], uv[1], light, textured);
        vertex(pose, vc, (float) c.x, (float) c.y, (float) c.z, rgba2[0], rgba2[1], rgba2[2], rgba2[3], uv[2], uv[3], light, textured);
        vertex(pose, vc, (float) d.x, (float) d.y, (float) d.z, rgba2[0], rgba2[1], rgba2[2], rgba2[3], uv[0], uv[3], light, textured);
        vertex(pose, vc, (float) d.x, (float) d.y, (float) d.z, rgba2[0], rgba2[1], rgba2[2], rgba2[3], uv[0], uv[3], light, textured);
        vertex(pose, vc, (float) c.x, (float) c.y, (float) c.z, rgba2[0], rgba2[1], rgba2[2], rgba2[3], uv[2], uv[3], light, textured);
        vertex(pose, vc, (float) b.x, (float) b.y, (float) b.z, rgba1[0], rgba1[1], rgba1[2], rgba1[3], uv[2], uv[1], light, textured);
        vertex(pose, vc, (float) a.x, (float) a.y, (float) a.z, rgba1[0], rgba1[1], rgba1[2], rgba1[3], uv[0], uv[1], light, textured);
    }

    private static float[] rgba(float r, float g, float b, float a) {
        return new float[]{r, g, b, a};
    }

    // ---- shapes ----

    /**
     * A strip along a path that always faces the camera (the positions are camera-relative, so the camera is the
     * origin): width from {@code w0} to {@code w1}, alpha from {@code a0} to {@code a1}, the texture rows {@code v0..v1}
     * repeated {@code repeat} times along it and scrolled by {@code scroll}.
     */
    public static void ribbon(PoseStack.Pose pose, VertexConsumer vc, List<Vec3> pts, float w0, float w1, float v0, float v1, float repeat,
                              float scroll, float r, float g, float b, float a0, float a1, int light, boolean textured) {
        int n = pts.size();
        if (n < 2) {
            return;
        }
        Vec3[] left = new Vec3[n];
        Vec3[] right = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Vec3 p = pts.get(i);
            Vec3 dir = pts.get(Math.min(n - 1, i + 1)).subtract(pts.get(Math.max(0, i - 1)));
            Vec3 toCam = p.scale(-1.0);
            Vec3 side = dir.cross(toCam);
            if (side.lengthSqr() < 1.0E-8) {
                side = new Vec3(0, 1, 0);
            }
            side = side.normalize();
            float t = (float) i / (n - 1);
            float w = Mth.lerp(t, w0, w1) * 0.5F;
            left[i] = p.add(side.scale(w));
            right[i] = p.subtract(side.scale(w));
        }
        for (int i = 0; i + 1 < n; i++) {
            float t0 = (float) i / (n - 1);
            float t1 = (float) (i + 1) / (n - 1);
            float u0 = frac(t0 * repeat - scroll);
            float u1 = u0 + (t1 - t0) * repeat;
            float[] uv = {u0, v0, u1, v1};
            // Across the strip the texture rows run; along it the columns (u). The entity formats want u in 0..1: it wraps.
            float[] c0 = rgba(r, g, b, Mth.lerp(t0, a0, a1));
            float[] c1 = rgba(r, g, b, Mth.lerp(t1, a0, a1));
            quad(pose, vc, left[i], right[i], right[i + 1], left[i + 1], new float[]{uv[0], uv[1], uv[2], uv[3]}, c0, c1, light, textured);
        }
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }

    /** A tube of {@code sides} faces along a path, radius {@code r0} at its start and {@code r1} at its end. */
    public static void tube(PoseStack.Pose pose, VertexConsumer vc, List<Vec3> pts, float r0, float r1, int sides, float v0, float v1,
                            float repeat, float scroll, float r, float g, float b, float a0, float a1, int light, boolean textured) {
        int n = pts.size();
        if (n < 2) {
            return;
        }
        Vec3[][] rings = new Vec3[n][sides];
        Vec3 normal = null;
        for (int i = 0; i < n; i++) {
            Vec3 p = pts.get(i);
            Vec3 t = pts.get(Math.min(n - 1, i + 1)).subtract(pts.get(Math.max(0, i - 1)));
            if (t.lengthSqr() < 1.0E-8) {
                t = new Vec3(0, 1, 0);
            }
            t = t.normalize();
            if (normal == null) {
                Vec3 any = Math.abs(t.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                normal = any.cross(t).normalize();
            } else {
                normal = normal.subtract(t.scale(normal.dot(t)));
                if (normal.lengthSqr() < 1.0E-8) {
                    normal = (Math.abs(t.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(t);
                }
                normal = normal.normalize();
            }
            Vec3 binormal = t.cross(normal);
            float radius = Mth.lerp((float) i / (n - 1), r0, r1);
            for (int k = 0; k < sides; k++) {
                double ang = k * Math.PI * 2 / sides;
                rings[i][k] = p.add(normal.scale(Math.cos(ang) * radius)).add(binormal.scale(Math.sin(ang) * radius));
            }
        }
        for (int i = 0; i + 1 < n; i++) {
            float t0 = (float) i / (n - 1);
            float t1 = (float) (i + 1) / (n - 1);
            float[] c0 = rgba(r, g, b, Mth.lerp(t0, a0, a1));
            float[] c1 = rgba(r, g, b, Mth.lerp(t1, a0, a1));
            float vv0 = v0 + (v1 - v0) * frac(t0 * repeat - scroll);
            float vv1 = vv0 + (v1 - v0) * (t1 - t0) * repeat;
            for (int k = 0; k < sides; k++) {
                int k2 = (k + 1) % sides;
                float u0 = (float) k / sides;
                float u1 = (float) (k + 1) / sides;
                quad(pose, vc, rings[i][k], rings[i][k2], rings[i + 1][k2], rings[i + 1][k],
                        new float[]{u0, vv0, u1, vv1}, c0, c1, light, textured);
            }
        }
    }

    /** A sphere, turned by {@code rot}; alpha {@code a} with a per-band ripple of {@code bands} (0 for none). */
    public static void sphere(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, float radius, int lat, int lon, float v0, float v1,
                              float r, float g, float b, float a, float bands, float phase, int light, boolean textured, Quaternionf rot) {
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat;
            double t1 = Math.PI * (i + 1) / lat;
            float band = bands > 0 ? (float) (0.5 + 0.5 * Math.sin(i * bands + phase)) : 1.0F;
            float alpha = bands > 0 ? a * (0.4F + 0.6F * band) : a;
            float[] col = rgba(r, g, b, alpha);
            float vv0 = v0 + (v1 - v0) * (float) i / lat;
            float vv1 = v0 + (v1 - v0) * (float) (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon;
                double p1 = Math.PI * 2 * (j + 1) / lon;
                Vec3 q00 = spherePoint(c, radius, t0, p0, rot);
                Vec3 q10 = spherePoint(c, radius, t1, p0, rot);
                Vec3 q11 = spherePoint(c, radius, t1, p1, rot);
                Vec3 q01 = spherePoint(c, radius, t0, p1, rot);
                quad(pose, vc, q00, q01, q11, q10, new float[]{(float) j / lon, vv0, (float) (j + 1) / lon, vv1}, col, col, light, textured);
            }
        }
    }

    private static Vec3 spherePoint(Vec3 c, float r, double theta, double phi, Quaternionf rot) {
        Vector3f v = new Vector3f((float) (r * Math.sin(theta) * Math.cos(phi)), (float) (r * Math.cos(theta)), (float) (r * Math.sin(theta) * Math.sin(phi)));
        if (rot != null) {
            rot.transform(v);
        }
        return c.add(v.x, v.y, v.z);
    }

    /** A cone (or a funnel) from {@code from} along {@code dir}: radius {@code r0} at the start, {@code r1} at the end. */
    /** The spine of a cone from {@code from} along {@code dir}, wobbling sideways by {@code wobble} toward the tip. */
    public static List<Vec3> conePoints(Vec3 from, Vec3 dir, float length, int segs, float wobble, float phase) {
        List<Vec3> pts = new ArrayList<>();
        Vec3 d = dir.normalize();
        Vec3 any = Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 n = any.cross(d).normalize();
        Vec3 bn = d.cross(n);
        for (int i = 0; i <= segs; i++) {
            float t = (float) i / segs;
            Vec3 p = from.add(d.scale(t * length));
            if (wobble > 0) {
                p = p.add(n.scale(Math.sin(phase + t * 6.0) * wobble * t)).add(bn.scale(Math.cos(phase * 1.3 + t * 5.0) * wobble * t));
            }
            pts.add(p);
        }
        return pts;
    }

    public static void cone(PoseStack.Pose pose, VertexConsumer vc, Vec3 from, Vec3 dir, float length, float r0, float r1, int sides, int segs,
                            float v0, float v1, float repeat, float scroll, float r, float g, float b, float a0, float a1, int light, boolean textured,
                            float wobble, float phase) {
        tube(pose, vc, conePoints(from, dir, length, segs, wobble, phase), r0, r1, sides, v0, v1, repeat, scroll, r, g, b, a0, a1, light, textured);
    }

    /**
     * Camera-facing puffs along a path, the size from {@code s0} to {@code s1} and the alpha from {@code a0} to
     * {@code a1}: the volume a tube or a ribbon lacks when it is seen along its length (the third-person camera
     * looks along every stream its player sends out, and sees a thin tube end-on as a ring).
     */
    public static void puffs(PoseStack.Pose pose, VertexConsumer vc, List<Vec3> pts, float s0, float s1, float v0, float v1,
                             float r, float g, float b, float a0, float a1, int light, boolean textured, float roll) {
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0.0F : (float) i / (n - 1);
            billboard(pose, vc, pts.get(i), Mth.lerp(t, s0, s1), v0, v1, r, g, b, Mth.lerp(t, a0, a1), light, textured, roll + i * 0.9F);
        }
    }

    /** {@link #puffs} along the spine of a cone, each puff as wide as the cone is there (times {@code scale}). */
    public static void conePuffs(PoseStack.Pose pose, VertexConsumer vc, Vec3 from, Vec3 dir, float length, float r0, float r1, int segs,
                                 float v0, float v1, float r, float g, float b, float a0, float a1, int light, boolean textured,
                                 float wobble, float phase, float scale, float roll) {
        puffs(pose, vc, conePoints(from, dir, length, segs, wobble, phase), r0 * 2.0F * scale, r1 * 2.0F * scale, v0, v1, r, g, b, a0, a1, light, textured, roll);
    }

    /** A vertical band round {@code c}: a wall of the element of {@code height}, with a ripple along it. */
    public static void ringBand(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, float radius, float height, int segs, float v0, float v1,
                                float r, float g, float b, float a, int light, boolean textured, float phase, float ripple) {
        float[] col = rgba(r, g, b, a);
        float[] colTop = rgba(r, g, b, a * 0.2F);
        for (int j = 0; j < segs; j++) {
            double p0 = Math.PI * 2 * j / segs;
            double p1 = Math.PI * 2 * (j + 1) / segs;
            float h0 = height * (1.0F + ripple * (float) Math.sin(p0 * 5 + phase));
            float h1 = height * (1.0F + ripple * (float) Math.sin(p1 * 5 + phase));
            Vec3 b0 = c.add(Math.cos(p0) * radius, 0, Math.sin(p0) * radius);
            Vec3 b1 = c.add(Math.cos(p1) * radius, 0, Math.sin(p1) * radius);
            quad(pose, vc, b0, b1, b1.add(0, h1, 0), b0.add(0, h0, 0), new float[]{(float) j / segs, v1, (float) (j + 1) / segs, v0}, col, colTop, light, textured);
        }
    }

    /** A flat ring (or a disc, with {@code rIn} 0) at {@code c} facing up, turned by {@code phase}. */
    public static void disc(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, float rIn, float rOut, int segs, float v0, float v1,
                            float r, float g, float b, float aIn, float aOut, int light, boolean textured, float phase) {
        float[] ci = rgba(r, g, b, aIn);
        float[] co = rgba(r, g, b, aOut);
        for (int j = 0; j < segs; j++) {
            double p0 = Math.PI * 2 * j / segs + phase;
            double p1 = Math.PI * 2 * (j + 1) / segs + phase;
            Vec3 i0 = c.add(Math.cos(p0) * rIn, 0, Math.sin(p0) * rIn);
            Vec3 i1 = c.add(Math.cos(p1) * rIn, 0, Math.sin(p1) * rIn);
            Vec3 o0 = c.add(Math.cos(p0) * rOut, 0, Math.sin(p0) * rOut);
            Vec3 o1 = c.add(Math.cos(p1) * rOut, 0, Math.sin(p1) * rOut);
            quad(pose, vc, i0, i1, o1, o0, new float[]{(float) j / segs, v0, (float) (j + 1) / segs, v1}, ci, co, light, textured);
        }
    }

    /** A square facing the camera at {@code c}, {@code size} across, turned by {@code roll}. */
    public static void billboard(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, float size, float v0, float v1,
                                 float r, float g, float b, float a, int light, boolean textured, float roll) {
        Vec3 toCam = c.scale(-1.0);
        if (toCam.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 n = toCam.normalize();
        Vec3 any = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = any.cross(n).normalize();
        Vec3 up = n.cross(right);
        double cs = Math.cos(roll);
        double sn = Math.sin(roll);
        Vec3 rr = right.scale(cs).add(up.scale(sn)).scale(size * 0.5);
        Vec3 uu = up.scale(cs).subtract(right.scale(sn)).scale(size * 0.5);
        float[] col = rgba(r, g, b, a);
        quad(pose, vc, c.subtract(rr).subtract(uu), c.add(rr).subtract(uu), c.add(rr).add(uu), c.subtract(rr).add(uu),
                new float[]{0, v0, 1, v1}, col, col, light, textured);
    }

    /**
     * A jagged bolt from {@code from} to {@code to}, with {@code branches} shorter forks, as camera-facing ribbons of
     * additive light: the same shape for the same {@code seed}, so it holds still between frames and jumps when the
     * seed changes.
     */
    public static void bolt(PoseStack.Pose pose, VertexConsumer vc, Vec3 from, Vec3 to, long seed, float amp, float width,
                            float r, float g, float b, float a, int branches) {
        RandomSource random = RandomSource.create(seed);
        List<Vec3> pts = jagged(from, to, amp, random);
        ribbon(pose, vc, pts, width, width, 0, 1, 1, 0, r, g, b, a, a, FULL, false);
        ribbon(pose, vc, pts, width * 0.35F, width * 0.35F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, a, a, FULL, false);
        for (int i = 0; i < branches; i++) {
            int at = 1 + random.nextInt(Math.max(1, pts.size() - 2));
            Vec3 start = pts.get(at);
            Vec3 d = to.subtract(from);
            Vec3 off = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(d.length() * 0.3);
            Vec3 end = start.add(d.scale(0.25)).add(off);
            List<Vec3> br = jagged(start, end, amp * 0.6F, random);
            ribbon(pose, vc, br, width * 0.6F, width * 0.2F, 0, 1, 1, 0, r, g, b, a * 0.8F, a * 0.3F, FULL, false);
        }
    }

    private static List<Vec3> jagged(Vec3 from, Vec3 to, float amp, RandomSource random) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int n = Math.max(3, (int) (len * 2.5) + 2);
        Vec3 dir = len < 1.0E-6 ? new Vec3(0, 1, 0) : d.normalize();
        Vec3 any = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 nrm = any.cross(dir).normalize();
        Vec3 bn = dir.cross(nrm);
        List<Vec3> pts = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            double env = Math.sin(t * Math.PI);
            Vec3 p = from.add(d.scale(t));
            if (i > 0 && i < n) {
                p = p.add(nrm.scale((random.nextDouble() - 0.5) * 2 * amp * env)).add(bn.scale((random.nextDouble() - 0.5) * 2 * amp * env));
            }
            pts.add(p);
        }
        return pts;
    }

    /** A soft glowing blob: layered billboards of the glow texture. */
    public static void glowBlob(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, float size, float r, float g, float b, float a, float roll) {
        billboard(pose, vc, c, size, 0, 1, r, g, b, a, FULL, true, roll);
        billboard(pose, vc, c, size * 0.55F, 0, 1, 1.0F, 1.0F, 1.0F, a * 0.8F, FULL, true, -roll);
    }

    private FxGeometry() {
    }
}
