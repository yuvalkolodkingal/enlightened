package com.kolodkingal.enlightened.client.render;

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
import org.joml.Matrix4f;

/**
 * The Anomaly as the show draws it: a square tilted on its corner, hung in the air, made of frames one inside the
 * next: broken red frames outside, a burning yellow ring, a dark heart. It turns slowly and sways; red lightning
 * crawls out of it on every side, flickering. All of it shines (drawn unlit).
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
        pose.pushPose();
        pose.translate(0.0, entity.getBbHeight() / 2.0, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(age * 1.5F));
        pose.mulPose(Axis.XP.rotationDegrees(12.0F * Mth.sin(age * 0.04F)));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F + 6.0F * Mth.sin(age * 0.03F)));
        pose.scale(1.4F, 1.4F, 1.4F);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        Matrix4f m = pose.last().pose();
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.25F);
        // The frames, each a little in front of the last: {outer half, inner half, depth, r, g, b, alpha, broken}.
        float[][] frames = {
                {1.75F, 1.55F, -0.06F, 1.0F, 0.12F, 0.08F, 0.85F, 1},
                {1.45F, 1.30F, -0.03F, 1.0F, 0.25F, 0.08F, 0.9F, 1},
                {1.20F, 1.08F, 0.0F, 0.75F, 0.05F, 0.05F, 0.7F, 0},
                {0.98F, 0.62F, 0.03F, 1.0F, 0.88F, 0.30F, 1.0F, 0},
                {0.56F, 0.44F, 0.05F, 1.0F, 0.55F, 0.12F, 0.9F, 0},
        };
        for (float[] f : frames) {
            frame(vc, m, f[0], f[1], f[2], f[3], Math.min(1.0F, f[4] + hurt * 0.5F), f[5], f[6] * pulse, f[7] > 0, age);
        }
        // The dark heart, and a red glow behind it all.
        square(vc, m, 0.44F, 0.04F, 0.16F, 0.02F, 0.02F, 0.95F);
        square(vc, m, 2.3F, -0.1F, 0.9F, 0.06F, 0.04F, 0.22F * pulse);
        pose.popPose();
        // Red lightning crawling out of it.
        pose.pushPose();
        pose.translate(0.0, entity.getBbHeight() / 2.0, 0.0);
        tendrils(buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE)), pose.last().pose(), entity.getId(), age);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    /** A square frame from half-width {@code inner} to {@code outer}, at depth z, both faces; broken frames lose pieces that move. */
    private static void frame(VertexConsumer vc, Matrix4f m, float outer, float inner, float z, float r, float g, float b, float a, boolean broken, float age) {
        int pieces = broken ? 6 : 1;
        for (int side = 0; side < 4; side++) {
            for (int k = 0; k < pieces; k++) {
                if (broken && ((k + side + (int) (age / 6)) % 4 == 0)) {
                    continue;
                }
                float t0 = (float) k / pieces;
                float t1 = (float) (k + 1) / pieces - (broken ? 0.04F : 0.0F);
                // Along the side from -outer to outer (outer edge) and -inner to inner (inner edge).
                float o0 = -outer + 2 * outer * t0, o1 = -outer + 2 * outer * t1;
                float i0 = -inner + 2 * inner * t0, i1 = -inner + 2 * inner * t1;
                float[][] q = {{o0, outer}, {o1, outer}, {i1, inner}, {i0, inner}};
                float[][] pts = new float[4][2];
                for (int n = 0; n < 4; n++) {
                    float u = q[n][0], v = q[n][1];
                    switch (side) {
                        case 0 -> pts[n] = new float[]{u, v};
                        case 1 -> pts[n] = new float[]{v, -u};
                        case 2 -> pts[n] = new float[]{-u, -v};
                        default -> pts[n] = new float[]{-v, u};
                    }
                }
                quad(vc, m, pts, z, r, g, b, a);
            }
        }
    }

    private static void square(VertexConsumer vc, Matrix4f m, float h, float z, float r, float g, float b, float a) {
        quad(vc, m, new float[][]{{-h, -h}, {h, -h}, {h, h}, {-h, h}}, z, r, g, b, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float[][] p, float z, float r, float g, float b, float a) {
        for (int n = 0; n < 4; n++) {
            vertex(vc, m, p[n][0], p[n][1], z, r, g, b, a);
        }
        for (int n = 3; n >= 0; n--) {
            vertex(vc, m, p[n][0], p[n][1], z, r, g, b, a);
        }
    }

    /** Jagged red bolts out of the centre, new every few ticks. */
    private static void tendrils(VertexConsumer vc, Matrix4f m, int id, float age) {
        RandomSource rnd = RandomSource.create(id * 31L + (long) (age / 3));
        for (int i = 0; i < 6; i++) {
            double yaw = rnd.nextDouble() * Math.PI * 2;
            double pitch = (rnd.nextDouble() - 0.5) * 1.6;
            double len = 4.0 + rnd.nextDouble() * 5.0;
            float x = 0, y = 0, z = 0;
            int segs = 8;
            for (int k = 0; k < segs; k++) {
                double step = len / segs;
                float nx = (float) (x + Math.cos(yaw) * Math.cos(pitch) * step + (rnd.nextDouble() - 0.5) * 0.7);
                float ny = (float) (y + Math.sin(pitch) * step + (rnd.nextDouble() - 0.5) * 0.7);
                float nz = (float) (z + Math.sin(yaw) * Math.cos(pitch) * step + (rnd.nextDouble() - 0.5) * 0.7);
                float w = 0.09F * (1.0F - k / (float) segs) + 0.02F;
                float a = 0.85F * (1.0F - k / (float) segs * 0.6F);
                ribbon(vc, m, x, y, z, nx, ny, nz, w, a);
                x = nx;
                y = ny;
                z = nz;
            }
        }
    }

    /** A segment drawn as two crossed strips, so it reads from any side. */
    private static void ribbon(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float w, float a) {
        float[][] offs = {{0, w, 0}, {w, 0, w}};
        for (float[] o : offs) {
            float[][] p = {{x0 - o[0], y0 - o[1], z0 - o[2]}, {x0 + o[0], y0 + o[1], z0 + o[2]}, {x1 + o[0], y1 + o[1], z1 + o[2]}, {x1 - o[0], y1 - o[1], z1 - o[2]}};
            for (int n = 0; n < 4; n++) {
                vertex(vc, m, p[n][0], p[n][1], p[n][2], 1.0F, 0.12F, 0.08F, a);
            }
            for (int n = 3; n >= 0; n--) {
                vertex(vc, m, p[n][0], p[n][1], p[n][2], 1.0F, 0.12F, 0.08F, a);
            }
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(AnomalyEntity entity) {
        return WHITE;
    }
}
