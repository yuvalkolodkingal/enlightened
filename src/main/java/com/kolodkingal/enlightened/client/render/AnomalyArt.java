package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.client.fx.FxGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The Anomaly, two ways. To most eyes, the neon square of the show; to one struck by its red lightning and in its
 * dread, pure energy, as it truly is: nothing solid, only light. A square on its corner facing the
 * eye, in a red bloom: broken red outlines, a thick yellow frame of flickering dashes, rings of dim red energy, a heart
 * burning white and yellow; motes of energy drawn in toward the heart and thrown out from it; red lightning crawling
 * out on every side. The whole of it breathes and shudders. Drawn for the Anomaly itself and, small, for each of the
 * red cubes it sleeps in.
 */
public final class AnomalyArt {
    private static final net.minecraft.resources.ResourceLocation WHITE = com.kolodkingal.enlightened.Enlightened.id("textures/entity/anomaly.png");
    private static final net.minecraft.resources.ResourceLocation VOID = com.kolodkingal.enlightened.Enlightened.id("textures/entity/anomaly_void.png");

    /** The frames: {half-size of the centre line, half-thickness, r, g, b, pieces per side (0 whole), gap chance}. */
    private static final float[][] FRAMES = {
            {1.92F, 0.02F, 1.0F, 0.25F, 0.12F, 5, 0.45F},
            {1.70F, 0.035F, 1.0F, 0.12F, 0.06F, 9, 0.2F},
            {1.40F, 0.11F, 1.0F, 0.86F, 0.22F, 14, 0.3F},
            {1.12F, 0.04F, 1.0F, 0.10F, 0.06F, 0, 0.0F},
            {0.80F, 0.025F, 0.9F, 0.08F, 0.08F, 0, 0.0F},
            {0.50F, 0.035F, 1.0F, 0.35F, 0.08F, 0, 0.0F},
    };

    /**
     * Draws it at the pose's origin. {@code face} turns a flat thing to the camera, {@code toCam} runs from here to the
     * camera; {@code scale} is its size (1.5 for the Anomaly), {@code bolts} how many tendrils of lightning and
     * {@code reach} how far they run.
     */
    public static void draw(PoseStack pose, MultiBufferSource buffer, Quaternionf face, Vec3 toCam, float age, long seed, float scale,
                            float hurt, int bolts, double reach) {
        // Only one in its dread sees it as it truly is, pure energy in its darkness; to everyone else it is the neon
        // square of the show.
        var viewer = net.minecraft.client.Minecraft.getInstance().player;
        if (viewer != null && viewer.hasEffect(com.kolodkingal.enlightened.registry.ModEffects.DREAD)) {
            drawEnergy(pose, buffer, face, toCam, age, seed, scale, hurt, bolts, reach);
        } else {
            drawNeon(pose, buffer, face, toCam, age, seed, scale, hurt, bolts, reach);
        }
    }

    /**
     * The neon square of the show: each frame its colour laid down solid under a glow and a pale core, dark maroon rings,
     * a heart burning yellow and white, in a red bloom; its red lightning a solid red line with a glow round it.
     */
    private static void drawNeon(PoseStack pose, MultiBufferSource buffer, Quaternionf face, Vec3 toCam, float age, long seed, float scale,
                                 float hurt, int bolts, double reach) {
        float pulse = 0.88F + 0.12F * Mth.sin(age * 0.22F);
        float flicker = Mth.sin(age * 1.7F + seed) * Mth.sin(age * 0.53F) > 0.93F ? 0.6F : 1.0F;
        float bright = pulse * flicker;
        float k = scale / 1.5F;
        VertexConsumer bloom = buffer.getBuffer(FxGeometry.glowTexture());
        blob(pose, bloom, toCam, 9.0F * k, 1.0F, 0.06F, 0.03F, 0.6F * bright, age * 0.01F);
        blob(pose, bloom, toCam, 5.0F * k, 1.0F, 0.12F, 0.05F, 0.5F * bright, -age * 0.015F);
        blob(pose, bloom, toCam, 1.9F * k * pulse, 1.0F, 0.85F, 0.35F, 0.85F * flicker, age * 0.03F);
        Matrix4f out = new Matrix4f(pose.last().pose());
        pose.pushPose();
        pose.mulPose(face);
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F + 4.0F * Mth.sin(age * 0.05F)));
        float s = scale * (1.0F + 0.03F * Mth.sin(age * 0.3F));
        pose.scale(s, s, s);
        Matrix4f m = new Matrix4f(pose.last().pose());
        pose.popPose();
        int step = (int) (age / 4);
        VertexConsumer solid = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        tendrils(solid, NEON_SOLID, out, seed, age, flicker, bolts, reach);
        band(solid, true, m, 1.42F, 0.0F, -0.04F, 0.55F, 0.03F, 0.03F, 0.85F);
        band(solid, true, m, 0.98F, 0.62F, -0.035F, 0.22F, 0.02F, 0.04F, 0.95F);
        band(solid, true, m, 0.62F, 0.0F, -0.035F, 0.35F, 0.03F, 0.05F, 0.95F);
        for (int i = 0; i < FRAMES.length; i++) {
            float[] f = FRAMES[i];
            float g = Math.min(1.0F, f[3] + hurt * 0.3F);
            frame(solid, true, m, f[0] + f[1], f[0] - f[1], 0.0F, f[2] * 0.9F, g * 0.85F, f[4] * 0.85F, 0.95F, (int) f[5], f[6], seed * 131L + i * 17L + step);
        }
        band(solid, true, m, 0.27F, 0.0F, 0.02F, 1.0F, 0.92F, 0.55F, 1.0F);
        VertexConsumer glow = buffer.getBuffer(RenderType.lightning());
        tendrils(glow, NEON_LIGHT, out, seed, age, flicker, bolts, reach);
        for (int i = 0; i < FRAMES.length; i++) {
            float[] f = FRAMES[i];
            float c = f[0], w = f[1], r = f[2], g = Math.min(1.0F, f[3] + hurt * 0.3F), b = f[4];
            int pieces = (int) f[5];
            long fs = seed * 131L + i * 17L + step;
            frame(glow, m, c + w + 0.22F, c - w - 0.22F, 0.01F, r, g, b, 0.10F * bright, pieces, f[6], fs);
            frame(glow, m, c + w + 0.09F, c - w - 0.09F, 0.01F, r, g, b, 0.28F * bright, pieces, f[6], fs);
            frame(glow, m, c + w * 0.45F, c - w * 0.45F, 0.015F, Mth.lerp(0.55F, r, 1.0F), Mth.lerp(0.55F, g, 1.0F), Mth.lerp(0.55F, b, 1.0F),
                    0.7F * bright, pieces, f[6], fs);
        }
        band(glow, m, 0.18F, 0.0F, 0.03F, 1.0F, 1.0F, 0.9F, 0.9F * bright);
    }

    /** Pure energy in its darkness: what one in its dread sees. */
    private static void drawEnergy(PoseStack pose, MultiBufferSource buffer, Quaternionf face, Vec3 toCam, float age, long seed, float scale,
                                   float hurt, int bolts, double reach) {
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.22F) + 0.05F * Mth.sin(age * 1.3F);
        float flicker = Mth.sin(age * 1.7F + seed) * Mth.sin(age * 0.53F) > 0.93F ? 0.55F : 1.0F;
        float bright = pulse * flicker;
        float k = scale / 1.5F;

        // The darkness it swallows the light with, so its energy burns even against a bright sky.
        VertexConsumer dark = buffer.getBuffer(RenderType.entityTranslucent(VOID));
        voidQuad(dark, pose.last(), toCam, 10.0F * k, 0.95F + 0.05F * Mth.sin(age * 0.11F));
        Matrix4f out = new Matrix4f(pose.last().pose());
        // The lightning's dark smoke, under its light.
        VertexConsumer smoke = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        tendrils(smoke, true, out, seed, age, flicker, bolts, reach);
        // The bloom it hangs in, and the burn of its heart.
        VertexConsumer bloom = buffer.getBuffer(FxGeometry.glowTexture());
        blob(pose, bloom, toCam, 9.0F * k, 1.0F, 0.05F, 0.03F, 0.65F * bright, age * 0.01F);
        blob(pose, bloom, toCam, 5.0F * k, 1.0F, 0.12F, 0.05F, 0.55F * bright, -age * 0.015F);
        blob(pose, bloom, toCam, 2.2F * k * pulse, 1.0F, 0.75F, 0.3F, 0.9F * flicker, age * 0.03F);
        blob(pose, bloom, toCam, 1.0F * k * pulse, 1.0F, 1.0F, 0.85F, 1.0F * flicker, -age * 0.05F);

        pose.pushPose();
        pose.mulPose(face);
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F + 4.0F * Mth.sin(age * 0.05F)));
        // It shudders now and then, as if it were not quite there.
        float shudder = Mth.sin(age * 0.37F + seed) > 0.96F ? 0.06F : 0.0F;
        pose.translate(shudder * Mth.sin(age * 9.0F), shudder * Mth.cos(age * 7.0F), 0.0F);
        float s = scale * (1.0F + 0.04F * Mth.sin(age * 0.3F));
        pose.scale(s, s, s);
        Matrix4f m = new Matrix4f(pose.last().pose());
        pose.popPose();

        VertexConsumer glow = buffer.getBuffer(RenderType.lightning());
        tendrils(glow, false, out, seed, age, flicker, bolts, reach);
        // Rings of dim red energy round the heart (light, not stone: they shimmer).
        float shimmer = 0.8F + 0.2F * Mth.sin(age * 0.9F);
        band(glow, m, 1.42F, 1.14F, -0.02F, 0.8F, 0.04F, 0.03F, 0.16F * bright * shimmer);
        band(glow, m, 0.98F, 0.62F, -0.02F, 0.6F, 0.02F, 0.06F, 0.22F * bright * shimmer);
        band(glow, m, 0.62F, 0.30F, -0.02F, 0.9F, 0.15F, 0.05F, 0.18F * bright * shimmer);
        int step = (int) (age / 4);
        for (int i = 0; i < FRAMES.length; i++) {
            float[] f = FRAMES[i];
            float c = f[0], w = f[1], r = f[2], g = Math.min(1.0F, f[3] + hurt * 0.3F), b = f[4];
            int pieces = (int) f[5];
            long fs = seed * 131L + i * 17L + step;
            // Each frame breathes on its own beat.
            float beat = 0.8F + 0.2F * Mth.sin(age * 0.4F + i * 1.3F);
            frame(glow, m, c + w + 0.26F, c - w - 0.26F, 0.0F, r, g, b, 0.10F * bright * beat, pieces, f[6], fs);
            frame(glow, m, c + w + 0.10F, c - w - 0.10F, 0.005F, r, g, b, 0.30F * bright * beat, pieces, f[6], fs);
            frame(glow, m, c + w, c - w, 0.01F, r, g, b, 0.75F * bright * beat, pieces, f[6], fs);
            frame(glow, m, c + w * 0.4F, c - w * 0.4F, 0.015F, Mth.lerp(0.65F, r, 1.0F), Mth.lerp(0.65F, g, 1.0F), Mth.lerp(0.65F, b, 1.0F),
                    0.85F * bright * beat, pieces, f[6], fs);
        }
        // The heart: white-hot, its yellow edge.
        band(glow, m, 0.30F, 0.0F, 0.02F, 1.0F, 0.75F, 0.25F, 0.8F * bright);
        band(glow, m, 0.20F, 0.0F, 0.025F, 1.0F, 1.0F, 0.85F, 1.0F * bright);
        motes(glow, m, seed, age, bright);
    }

    /** Motes of energy: drawn in from the frames toward the heart, and flung out from it. */
    private static void motes(VertexConsumer vc, Matrix4f m, long seed, float age, float bright) {
        RandomSource rnd = RandomSource.create(seed * 7919L);
        for (int i = 0; i < 28; i++) {
            float speed = 0.01F + rnd.nextFloat() * 0.02F;
            float phase = Mth.frac(rnd.nextFloat() + age * speed);
            boolean in = i % 2 == 0;
            float rad = in ? 2.0F * (1.0F - phase) : 0.3F + 2.2F * phase;
            double a = rnd.nextDouble() * Math.PI * 2 + (in ? phase * 1.5 : -phase);
            float x = (float) Math.cos(a) * rad, y = (float) Math.sin(a) * rad;
            float sz = 0.025F + 0.03F * rnd.nextFloat();
            float alpha = Mth.sin(phase * Mth.PI) * bright;
            float g = in ? 0.8F : 0.25F;
            quad(vc, m, 0, new float[][]{{x - sz, y - sz}, {x + sz, y - sz}, {x + sz, y + sz}, {x - sz, y + sz}}, 0.03F, 1.0F, g, 0.3F, alpha);
        }
    }

    /** A square of darkness facing the camera, black in the middle and fading out (a shade of red in it). */
    private static void voidQuad(VertexConsumer vc, PoseStack.Pose pose, Vec3 toCam, float size, float a) {
        if (toCam.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 n = toCam.normalize();
        Vec3 any = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = any.cross(n).normalize().scale(size * 0.5);
        Vec3 up = n.cross(right.normalize()).scale(size * 0.5);
        Vec3 back = n.scale(-0.5);
        Vec3[] p = {back.subtract(right).subtract(up), back.add(right).subtract(up), back.add(right).add(up), back.subtract(right).add(up)};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        // Both windings, so it shows whichever way the faces are culled.
        for (int pass = 0; pass < 2; pass++) {
            for (int j = 0; j < 4; j++) {
                int i = pass == 0 ? j : 3 - j;
                vc.addVertex(pose, (float) p[i].x, (float) p[i].y, (float) p[i].z).setColor(0.07F, 0.0F, 0.0F, a).setUv(uv[i][0], uv[i][1])
                        .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FxGeometry.FULL).setNormal(pose, 0.0F, 1.0F, 0.0F);
            }
        }
    }

    /** A soft glow facing the camera, its heart whiter. */
    private static void blob(PoseStack pose, VertexConsumer vc, Vec3 toCam, float size, float r, float g, float b, float a, float roll) {
        FxGeometry.billboardTowards(pose.last(), vc, Vec3.ZERO, toCam, size, 0, 1, r, g, b, a, FxGeometry.FULL, true, roll);
        FxGeometry.billboardTowards(pose.last(), vc, Vec3.ZERO, toCam, size * 0.55F, 0, 1, Mth.lerp(0.5F, r, 1.0F), Mth.lerp(0.5F, g, 1.0F),
                Mth.lerp(0.5F, b, 1.0F), a * 0.7F, FxGeometry.FULL, true, -roll);
    }

    /** A square frame from half-width {@code inner} to {@code outer}; with pieces, broken into dashes, some missing. */
    private static void frame(VertexConsumer vc, Matrix4f m, float outer, float inner, float z, float r, float g, float b, float a, int pieces,
                              float gaps, long seed) {
        frame(vc, false, m, outer, inner, z, r, g, b, a, pieces, gaps, seed);
    }

    private static void frame(VertexConsumer vc, boolean tex, Matrix4f m, float outer, float inner, float z, float r, float g, float b, float a,
                              int pieces, float gaps, long seed) {
        RandomSource rnd = RandomSource.create(seed);
        int n = Math.max(1, pieces);
        for (int side = 0; side < 4; side++) {
            for (int k = 0; k < n; k++) {
                if (pieces > 0 && rnd.nextFloat() < gaps) {
                    continue;
                }
                float t0 = (float) k / n;
                float t1 = (float) (k + 1) / n - (pieces > 0 ? 0.25F / n : 0.0F);
                float o0 = -outer + 2 * outer * t0, o1 = -outer + 2 * outer * t1;
                float i0 = pieces > 0 ? o0 : -inner + 2 * inner * t0;
                float i1 = pieces > 0 ? o1 : -inner + 2 * inner * t1;
                quad(vc, tex, m, side, new float[][]{{o0, outer}, {o1, outer}, {i1, inner}, {i0, inner}}, z, r, g, b, a);
            }
        }
    }

    /** A filled square of half-width {@code outer}, or a band from {@code inner} out to it. */
    private static void band(VertexConsumer vc, Matrix4f m, float outer, float inner, float z, float r, float g, float b, float a) {
        band(vc, false, m, outer, inner, z, r, g, b, a);
    }

    private static void band(VertexConsumer vc, boolean tex, Matrix4f m, float outer, float inner, float z, float r, float g, float b, float a) {
        if (inner <= 0.0F) {
            quad(vc, tex, m, 0, new float[][]{{-outer, -outer}, {outer, -outer}, {outer, outer}, {-outer, outer}}, z, r, g, b, a);
            return;
        }
        for (int side = 0; side < 4; side++) {
            quad(vc, tex, m, side, new float[][]{{-outer, outer}, {outer, outer}, {inner, inner}, {-inner, inner}}, z, r, g, b, a);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int side, float[][] q, float z, float r, float g, float b, float a) {
        quad(vc, false, m, side, q, z, r, g, b, a);
    }

    /** A quad, both faces; solid ({@code tex}: the full entity vertex format) or light (position and colour). */
    private static void quad(VertexConsumer vc, boolean tex, Matrix4f m, int side, float[][] q, float z, float r, float g, float b, float a) {
        float[][] pts = new float[4][];
        for (int i = 0; i < 4; i++) {
            float u = q[i][0], v = q[i][1];
            pts[i] = switch (side) {
                case 0 -> new float[]{u, v};
                case 1 -> new float[]{v, -u};
                case 2 -> new float[]{-u, -v};
                default -> new float[]{-v, u};
            };
        }
        for (int i = 0; i < 4; i++) {
            point(vc, tex, m, pts[i][0], pts[i][1], z, r, g, b, a);
        }
        for (int i = 3; i >= 0; i--) {
            point(vc, tex, m, pts[i][0], pts[i][1], z, r, g, b, a);
        }
    }

    /**
     * Jagged red bolts out of the centre, new every few ticks (the same bolts each pass): drawn as smoke, a dark band
     * under each; drawn as light, a red glow, a red body, a pale core.
     */
    private static void tendrils(VertexConsumer vc, boolean smoke, Matrix4f m, long seed, float age, float flicker, int bolts, double reach) {
        tendrils(vc, smoke ? SMOKE : LIGHT, m, seed, age, flicker, bolts, reach);
    }

    /** How a pass draws the bolts: dark smoke, energy's light, the neon look's solid red line, or its light. */
    private static final int SMOKE = 0, LIGHT = 1, NEON_SOLID = 2, NEON_LIGHT = 3;

    private static void tendrils(VertexConsumer vc, int mode, Matrix4f m, long seed, float age, float flicker, int bolts, double reach) {
        RandomSource rnd = RandomSource.create(seed * 31L + (long) (age / 3));
        double scale = reach / 9.0;
        for (int i = 0; i < bolts; i++) {
            double yaw = rnd.nextDouble() * Math.PI * 2;
            double pitch = (rnd.nextDouble() - 0.5) * 1.8;
            double len = reach * (0.65 + rnd.nextDouble() * 0.7);
            double bend = (rnd.nextDouble() - 0.5) * 0.25;
            float x = 0, y = 0, z = 0;
            int segs = 10;
            for (int k = 0; k < segs; k++) {
                double step = len / segs;
                yaw += bend;
                float nx = (float) (x + Math.cos(yaw) * Math.cos(pitch) * step + (rnd.nextDouble() - 0.5) * 0.6 * scale);
                float ny = (float) (y + Math.sin(pitch) * step + (rnd.nextDouble() - 0.5) * 0.6 * scale);
                float nz = (float) (z + Math.sin(yaw) * Math.cos(pitch) * step + (rnd.nextDouble() - 0.5) * 0.6 * scale);
                float fade = (1.0F - k / (float) segs * 0.5F) * flicker;
                float w = (float) ((0.08F * (1.0F - k / (float) segs) + 0.025F) * Math.max(0.3, scale));
                if (mode == NEON_SOLID) {
                    ribbon(vc, true, m, x, y, z, nx, ny, nz, w, 0.85F, 0.05F, 0.04F, 0.9F * fade);
                } else if (mode == NEON_LIGHT) {
                    ribbon(vc, m, x, y, z, nx, ny, nz, w * 3.5F, 1.0F, 0.05F, 0.03F, 0.2F * fade);
                    ribbon(vc, m, x, y, z, nx, ny, nz, w * 0.45F, 1.0F, 0.4F, 0.35F, 0.7F * fade);
                } else if (mode == SMOKE) {
                    smokeRibbon(vc, m, x, y, z, nx, ny, nz, w * 3.0F, 0.6F * fade);
                } else {
                    ribbon(vc, m, x, y, z, nx, ny, nz, w * 2.2F, 1.0F, 0.03F, 0.02F, 0.25F * fade);
                    ribbon(vc, m, x, y, z, nx, ny, nz, w, 1.0F, 0.06F, 0.04F, 0.8F * fade);
                    ribbon(vc, m, x, y, z, nx, ny, nz, w * 0.35F, 1.0F, 0.45F, 0.35F, 0.8F * fade);
                }
                x = nx;
                y = ny;
                z = nz;
            }
        }
    }

    /** A segment of dark smoke (the full entity vertex format), two crossed strips. */
    private static void smokeRibbon(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float w, float a) {
        float[][] offs = {{0, w, 0}, {w, 0, w}};
        for (float[] o : offs) {
            float[][] p = {{x0 - o[0], y0 - o[1], z0 - o[2]}, {x0 + o[0], y0 + o[1], z0 + o[2]}, {x1 + o[0], y1 + o[1], z1 + o[2]}, {x1 - o[0], y1 - o[1], z1 - o[2]}};
            for (int pass = 0; pass < 2; pass++) {
                for (int j = 0; j < 4; j++) {
                    float[] q = p[pass == 0 ? j : 3 - j];
                    vc.addVertex(m, q[0], q[1], q[2]).setColor(0.08F, 0.0F, 0.0F, a).setUv(0.5F, 0.5F)
                            .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FxGeometry.FULL).setNormal(0.0F, 1.0F, 0.0F);
                }
            }
        }
    }

    private static void point(VertexConsumer vc, boolean tex, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        if (tex) {
            vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(0.5F, 0.5F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(FxGeometry.FULL).setNormal(0.0F, 1.0F, 0.0F);
        } else {
            vc.addVertex(m, x, y, z).setColor(r, g, b, a);
        }
    }

    /** A segment drawn as two crossed strips, so it reads from any side. */
    private static void ribbon(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float w,
                               float r, float g, float b, float a) {
        ribbon(vc, false, m, x0, y0, z0, x1, y1, z1, w, r, g, b, a);
    }

    private static void ribbon(VertexConsumer vc, boolean tex, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float w,
                               float r, float g, float b, float a) {
        float[][] offs = {{0, w, 0}, {w, 0, w}};
        for (float[] o : offs) {
            float[][] p = {{x0 - o[0], y0 - o[1], z0 - o[2]}, {x0 + o[0], y0 + o[1], z0 + o[2]}, {x1 + o[0], y1 + o[1], z1 + o[2]}, {x1 - o[0], y1 - o[1], z1 - o[2]}};
            for (int n = 0; n < 4; n++) {
                point(vc, tex, m, p[n][0], p[n][1], p[n][2], r, g, b, a);
            }
            for (int n = 3; n >= 0; n--) {
                point(vc, tex, m, p[n][0], p[n][1], p[n][2], r, g, b, a);
            }
        }
    }

    private AnomalyArt() {
    }
}
