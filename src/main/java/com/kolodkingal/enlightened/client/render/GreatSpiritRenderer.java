package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.GreatSpiritEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Raava and Vaatu as the show draws them: a flat kite of a spirit, the head round and patterned, the body tapering to
 * a point, turned to face where it looks, with long ribbons (Raava) or tendrils (Vaatu) trailing and waving from it.
 * Raava glows white and pale blue; Vaatu is black edged in red. Both are drawn in their own light.
 */
public class GreatSpiritRenderer extends EntityRenderer<GreatSpiritEntity> {
    private static final ResourceLocation RAAVA = Enlightened.id("textures/entity/raava.png");
    private static final ResourceLocation VAATU = Enlightened.id("textures/entity/vaatu.png");
    /** The body's half width and height (blocks). */
    private static final float HALF = 1.6F;
    private static final float HEIGHT = 4.2F;

    public GreatSpiritRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(GreatSpiritEntity s, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean raava = s.kind() == GreatSpiritEntity.Kind.RAAVA;
        float age = s.tickCount + partial;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(raava ? RAAVA : VAATU));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, s.yBodyRotO, s.yBodyRot)));
        pose.translate(0, 0.4 + Mth.sin(age * 0.06F) * 0.15, 0);
        pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * 0.04F) * 6.0F));
        Matrix4f m = pose.last().pose();
        int full = LightTexture.FULL_BRIGHT;
        // The body: the top three quarters of the texture.
        quad(vc, m, pose, -HALF, 0, HALF, HEIGHT, 0, 0, 1, 0.75F, full, 1.0F);
        // The ribbons or tendrils, from the body's sides and foot, waving.
        int count = raava ? 4 : 8;
        for (int i = 0; i < count; i++) {
            float side = i % 2 == 0 ? -1 : 1;
            float x0 = side * (raava ? 0.4F + 0.3F * (i / 2) : 0.3F + 0.35F * (i / 2));
            float y0 = raava ? 0.6F + 0.4F * (i / 2) : 1.6F + 0.25F * (i / 2);
            ribbon(vc, m, pose, x0, y0, side, raava ? 3.6F : 4.5F, age * (raava ? 0.12F : 0.18F) + i * 1.7F, raava ? 0.22F : 0.16F, full, raava);
        }
        pose.popPose();
        super.render(s, yaw, partial, pose, buffers, light);
    }

    /** A flat quad in the spirit's plane (both sides). */
    private static void quad(VertexConsumer vc, Matrix4f m, PoseStack pose, float x0, float y0, float x1, float y1,
                             float u0, float v0, float u1, float v1, int light, float alpha) {
        int a = (int) (alpha * 255);
        PoseStack.Pose p = pose.last();
        vc.addVertex(m, x0, y0, 0).setColor(255, 255, 255, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(m, x1, y0, 0).setColor(255, 255, 255, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(m, x1, y1, 0).setColor(255, 255, 255, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(m, x0, y1, 0).setColor(255, 255, 255, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(m, x0, y1, 0).setColor(255, 255, 255, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, -1);
        vc.addVertex(m, x1, y1, 0).setColor(255, 255, 255, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, -1);
        vc.addVertex(m, x1, y0, 0).setColor(255, 255, 255, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, -1);
        vc.addVertex(m, x0, y0, 0).setColor(255, 255, 255, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, -1);
    }

    /**
     * A ribbon trailing down and outward from a point on the body, waving as a wave runs along it; drawn from the
     * strip at the bottom of the texture, thinning and fading to its tip.
     */
    private static void ribbon(VertexConsumer vc, Matrix4f m, PoseStack pose, float x0, float y0, float side, float length,
                               float phase, float width, int light, boolean raava) {
        int segs = 12;
        float px = x0;
        float py = y0;
        for (int k = 1; k <= segs; k++) {
            float t = k / (float) segs;
            float x = x0 + side * (raava ? t * 1.6F : t * 2.2F) + Mth.sin(phase + t * 5.0F) * 0.35F * t;
            float y = y0 - t * length * (raava ? 0.55F : 0.75F) + Mth.cos(phase * 0.8F + t * 4.0F) * 0.25F * t;
            float w0 = width * (1.0F - (k - 1) / (float) segs * 0.8F);
            float w1 = width * (1.0F - t * 0.8F);
            float dx = x - px;
            float dy = y - py;
            float len = Mth.sqrt(dx * dx + dy * dy) + 1.0E-4F;
            float nx = -dy / len;
            float ny = dx / len;
            float a0 = 1.0F - (k - 1) / (float) segs * 0.7F;
            float a1 = 1.0F - t * 0.7F;
            strip(vc, m, pose, px + nx * w0, py + ny * w0, px - nx * w0, py - ny * w0, x - nx * w1, y - ny * w1, x + nx * w1, y + ny * w1,
                    (k - 1) / (float) segs, t, light, a0, a1);
            px = x;
            py = y;
        }
    }

    private static void strip(VertexConsumer vc, Matrix4f m, PoseStack pose, float ax, float ay, float bx, float by, float cx, float cy,
                              float dx, float dy, float u0, float u1, int light, float a0, float a1) {
        PoseStack.Pose p = pose.last();
        int i0 = (int) (a0 * 255);
        int i1 = (int) (a1 * 255);
        float v0 = 0.78F;
        float v1 = 0.97F;
        for (int face = 0; face < 2; face++) {
            float n = face == 0 ? 1 : -1;
            float[][] q = face == 0 ? new float[][]{{ax, ay, u0, v0, i0}, {bx, by, u0, v1, i0}, {cx, cy, u1, v1, i1}, {dx, dy, u1, v0, i1}}
                    : new float[][]{{dx, dy, u1, v0, i1}, {cx, cy, u1, v1, i1}, {bx, by, u0, v1, i0}, {ax, ay, u0, v0, i0}};
            for (float[] v : q) {
                vc.addVertex(m, v[0], v[1], 0).setColor(255, 255, 255, (int) v[4]).setUv(v[2], v[3]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(p, 0, 0, n);
            }
        }
    }

    @Override
    public boolean shouldRender(GreatSpiritEntity s, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(s.getBoundingBox().inflate(4.0));
    }

    @Override
    public ResourceLocation getTextureLocation(GreatSpiritEntity s) {
        return s.kind() == GreatSpiritEntity.Kind.RAAVA ? RAAVA : VAATU;
    }
}
