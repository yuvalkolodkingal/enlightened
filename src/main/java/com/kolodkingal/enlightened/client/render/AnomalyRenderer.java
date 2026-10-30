package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.client.fx.FxGeometry;
import com.kolodkingal.enlightened.entity.AnomalyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Anomaly as the show draws it, in neon: a square standing on its corner, always facing whoever looks at it, in a
 * red bloom. From the outside in: broken red outlines, a red frame, a thick yellow frame broken into flickering
 * dashes, another red frame, dark maroon rings, a red-orange frame, and at the heart a small square burning yellow
 * and white. Red lightning crawls out of it on every side. Each frame is a tube of light: its own colour laid down
 * solid (so it holds against a bright sky), a glow round it, a pale core.
 */
public class AnomalyRenderer extends EntityRenderer<AnomalyEntity> {
    private static final ResourceLocation WHITE = com.kolodkingal.enlightened.Enlightened.id("textures/entity/anomaly.png");

    public AnomalyRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(AnomalyEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        float age = entity.tickCount + partialTick;
        float hurt = entity.hurtTime > 0 ? 1.0F : 0.0F;
        float pulse = 0.88F + 0.12F * Mth.sin(age * 0.22F);
        float flicker = Mth.sin(age * 1.7F) * Mth.sin(age * 0.53F) > 0.93F ? 0.6F : 1.0F;
        float bright = pulse * flicker;
        pose.pushPose();
        pose.translate(0.0, entity.getBbHeight() / 2.0, 0.0);
        Matrix4f out = new Matrix4f(pose.last().pose());
        // The square faces the eye, on its corner, swaying a little.
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F + 4.0F * Mth.sin(age * 0.05F)));
        float s = 1.5F * (1.0F + 0.03F * Mth.sin(age * 0.3F));
        pose.scale(s, s, s);
        Matrix4f m = new Matrix4f(pose.last().pose());
        pose.popPose();

        // Each buffer is filled whole before the next is asked for (asking for another ends the last one's batch).
        // First the red bloom it hangs in, and the burn of its heart.
        VertexConsumer bloom = buffer.getBuffer(FxGeometry.glowTexture());
        Vec3 toCam = entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2.0, 0));
        blob(pose, bloom, toCam, 9.0F, 1.0F, 0.06F, 0.03F, 0.6F * bright, age * 0.01F);
        blob(pose, bloom, toCam, 5.0F, 1.0F, 0.12F, 0.05F, 0.5F * bright, -age * 0.015F);
        blob(pose, bloom, toCam, 1.9F * pulse, 1.0F, 0.85F, 0.35F, 0.85F * flicker, age * 0.03F);

        // The frames: {half-size of the centre line, half-thickness, r, g, b, pieces per side (0 whole), gap chance}.
        float[][] frames = {
                {1.92F, 0.02F, 1.0F, 0.25F, 0.12F, 5, 0.45F},
                {1.70F, 0.035F, 1.0F, 0.12F, 0.06F, 9, 0.2F},
                {1.40F, 0.11F, 1.0F, 0.86F, 0.22F, 14, 0.3F},
                {1.12F, 0.04F, 1.0F, 0.10F, 0.06F, 0, 0.0F},
                {0.80F, 0.025F, 0.75F, 0.06F, 0.06F, 0, 0.0F},
                {0.50F, 0.035F, 1.0F, 0.35F, 0.08F, 0, 0.0F},
        };
        int step = (int) (age / 4);

        // Then everything solid: the lightning's red lines, the deep red body, the dark maroon rings round the heart,
        // each frame's own colour, and the heart itself.
        VertexConsumer solid = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        tendrils(solid, true, out, entity.getId(), age, flicker);
        fill(solid, true, m, 1.42F, 0.0F, -0.04F, 0.55F, 0.03F, 0.03F, 0.85F);
        fill(solid, true, m, 0.98F, 0.62F, -0.035F, 0.22F, 0.02F, 0.04F, 0.95F);
        fill(solid, true, m, 0.62F, 0.0F, -0.035F, 0.35F, 0.03F, 0.05F, 0.95F);
        for (int i = 0; i < frames.length; i++) {
            float[] f = frames[i];
            float g = Math.min(1.0F, f[3] + hurt * 0.3F);
            frame(solid, true, m, f[0] + f[1], f[0] - f[1], 0.0F, f[2] * 0.9F, g * 0.85F, f[4] * 0.85F, 0.95F, (int) f[5], f[6],
                    entity.getId() * 131L + i * 17L + step);
        }
        fill(solid, true, m, 0.27F, 0.0F, 0.02F, 1.0F, 0.92F, 0.55F, 1.0F);

        // Last the light: the lightning's glow and pale cores, each frame's glow and core, the white of the heart.
        VertexConsumer glow = buffer.getBuffer(RenderType.lightning());
        tendrils(glow, false, out, entity.getId(), age, flicker);
        for (int i = 0; i < frames.length; i++) {
            float[] f = frames[i];
            float c = f[0], w = f[1], r = f[2], g = Math.min(1.0F, f[3] + hurt * 0.3F), b = f[4];
            int pieces = (int) f[5];
            long seed = entity.getId() * 131L + i * 17L + step;
            frame(glow, false, m, c + w + 0.22F, c - w - 0.22F, 0.01F, r, g, b, 0.10F * bright, pieces, f[6], seed);
            frame(glow, false, m, c + w + 0.09F, c - w - 0.09F, 0.01F, r, g, b, 0.28F * bright, pieces, f[6], seed);
            frame(glow, false, m, c + w * 0.45F, c - w * 0.45F, 0.015F, Mth.lerp(0.55F, r, 1.0F), Mth.lerp(0.55F, g, 1.0F), Mth.lerp(0.55F, b, 1.0F),
                    0.7F * bright, pieces, f[6], seed);
        }
        fill(glow, false, m, 0.18F, 0.0F, 0.03F, 1.0F, 1.0F, 0.9F, 0.9F * bright);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    /**
     * A square frame from half-width {@code inner} to {@code outer}, both faces, solid ({@code tex}) or glowing. With
     * pieces, each side is broken into that many dashes, some of them missing (by {@code gaps}, changing every few
     * ticks), as the Anomaly's frames glitch.
     */
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
    private static void fill(VertexConsumer vc, boolean tex, Matrix4f m, float outer, float inner, float z, float r, float g, float b, float a) {
        if (inner <= 0.0F) {
            quad(vc, tex, m, 0, new float[][]{{-outer, -outer}, {outer, -outer}, {outer, outer}, {-outer, outer}}, z, r, g, b, a);
            return;
        }
        for (int side = 0; side < 4; side++) {
            quad(vc, tex, m, side, new float[][]{{-outer, outer}, {outer, outer}, {inner, inner}, {-inner, inner}}, z, r, g, b, a);
        }
    }

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
     * Jagged red bolts out of the centre, new every few ticks (the same bolts for both passes): drawn solid, a red line;
     * drawn as light, a red glow round it and a lighter red core.
     */
    private static void tendrils(VertexConsumer vc, boolean tex, Matrix4f m, int id, float age, float flicker) {
        RandomSource rnd = RandomSource.create(id * 31L + (long) (age / 3));
        for (int i = 0; i < 8; i++) {
            double yaw = rnd.nextDouble() * Math.PI * 2;
            double pitch = (rnd.nextDouble() - 0.5) * 1.8;
            double len = 6.0 + rnd.nextDouble() * 6.0;
            double bend = (rnd.nextDouble() - 0.5) * 0.25;
            float x = 0, y = 0, z = 0;
            int segs = 10;
            for (int k = 0; k < segs; k++) {
                double step = len / segs;
                yaw += bend;
                float nx = (float) (x + Math.cos(yaw) * Math.cos(pitch) * step + (rnd.nextDouble() - 0.5) * 0.6);
                float ny = (float) (y + Math.sin(pitch) * step + (rnd.nextDouble() - 0.5) * 0.6);
                float nz = (float) (z + Math.sin(yaw) * Math.cos(pitch) * step + (rnd.nextDouble() - 0.5) * 0.6);
                float fade = (1.0F - k / (float) segs * 0.5F) * flicker;
                float w = 0.08F * (1.0F - k / (float) segs) + 0.025F;
                if (tex) {
                    ribbon(vc, true, m, x, y, z, nx, ny, nz, w, 0.85F, 0.05F, 0.04F, 0.9F * fade);
                } else {
                    ribbon(vc, false, m, x, y, z, nx, ny, nz, w * 3.5F, 1.0F, 0.05F, 0.03F, 0.2F * fade);
                    ribbon(vc, false, m, x, y, z, nx, ny, nz, w * 0.45F, 1.0F, 0.4F, 0.35F, 0.7F * fade);
                }
                x = nx;
                y = ny;
                z = nz;
            }
        }
    }

    /** A segment drawn as two crossed strips, so it reads from any side. */
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

    /** A soft glow facing the camera, its heart whiter. */
    private static void blob(PoseStack pose, VertexConsumer vc, Vec3 toCam, float size, float r, float g, float b, float a, float roll) {
        FxGeometry.billboardTowards(pose.last(), vc, Vec3.ZERO, toCam, size, 0, 1, r, g, b, a, FxGeometry.FULL, true, roll);
        FxGeometry.billboardTowards(pose.last(), vc, Vec3.ZERO, toCam, size * 0.55F, 0, 1, Mth.lerp(0.5F, r, 1.0F), Mth.lerp(0.5F, g, 1.0F),
                Mth.lerp(0.5F, b, 1.0F), a * 0.7F, FxGeometry.FULL, true, -roll);
    }

    /** A vertex for either buffer: the solid one takes the full entity format, the glow only position and colour. */
    private static void point(VertexConsumer vc, boolean tex, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        if (tex) {
            vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0.0F, 1.0F, 0.0F);
        } else {
            vc.addVertex(m, x, y, z).setColor(r, g, b, a);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AnomalyEntity entity) {
        return WHITE;
    }
}
