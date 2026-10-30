package com.kolodkingal.enlightened.client.cosmic;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Chakras;
import com.kolodkingal.enlightened.client.fx.FxGeometry;
import com.kolodkingal.enlightened.world.Cosmic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The paths of the cosmic plane, made of energy: a band of violet light over the unseen floor, bright at its edges,
 * with pulses of light running along it toward its end, and the platforms as discs of the same light. At the end of
 * the Anomaly's path, the seven chakras as the show draws them in "The Guru": dark circles ringed with flames of
 * their colour, red, orange, yellow, green, blue, indigo, violet, each with its lotus symbol glowing inside it; the
 * first, red, is open and lit from within.
 */
final class ChakraPathRenderer {
    private static final ResourceLocation WHITE = Enlightened.id("textures/entity/anomaly.png");
    private static final ResourceLocation[] SYMBOLS = new ResourceLocation[Chakras.COUNT];

    static {
        for (int i = 0; i < Chakras.COUNT; i++) {
            SYMBOLS[i] = Enlightened.id("textures/entity/chakra/" + (i + 1) + ".png");
        }
    }

    static void render(PoseStack.Pose pose, Vec3 cam, float age, MultiBufferSource.BufferSource buffers, boolean anomaly) {
        Matrix4f m = pose.pose();
        VertexConsumer light = buffers.getBuffer(FxGeometry.light());
        if (anomaly) {
            int ax = Cosmic.ANOMALY_X;
            path(light, m, cam, age, z -> ax, 3, Cosmic.CIRCLE_Z[6] + 1);
            disc(light, m, cam, age, ax + 0.5, 0.5, 3.4);
            disc(light, m, cam, age, ax + 0.5, Cosmic.CIRCLE_Z[6] + 4.5, 3.4);
            for (int n = 0; n < Chakras.COUNT; n++) {
                flames(light, m, cam, age, n);
            }
            buffers.endBatch(FxGeometry.light());
            for (int n = Chakras.COUNT - 1; n >= 0; n--) {
                circle(buffers, m, cam, age, n);
            }
        } else {
            path(light, m, cam, age, Cosmic::pathX, 3, 63);
            disc(light, m, cam, age, 0.5, 0.5, 3.4);
            disc(light, m, cam, age, Cosmic.END.x, Cosmic.END.z, 4.4);
            buffers.endBatch(FxGeometry.light());
        }
    }

    /** The band of light from z0 to z1 over the floor, its centre line at {@code cx(z)}. */
    private static void path(VertexConsumer vc, Matrix4f m, Vec3 cam, float age, java.util.function.IntUnaryOperator cx, int z0, int z1) {
        double y = Cosmic.Y + 1.02 - cam.y;
        for (int z = z0 - 1; z <= z1; z++) {
            double xa = cx.applyAsInt(z) + 0.5 - cam.x, xb = cx.applyAsInt(z + 1) + 0.5 - cam.x;
            double za = z + 0.5 - cam.z, zb = z + 1.5 - cam.z;
            // A pulse of light runs along the path toward its end, every few blocks.
            float run = Mth.frac((z - age * 0.25F) / 9.0F);
            float pulse = run > 0.8F ? (run - 0.8F) * 5.0F : 0.0F;
            float shimmer = 0.85F + 0.15F * Mth.sin(age * 0.2F + z * 0.7F);
            strip(vc, m, xa, xb, y, za, zb, -1.55, 1.55, 0.55F, 0.25F, 0.95F, (0.22F + 0.35F * pulse) * shimmer);
            strip(vc, m, xa, xb, y + 0.005, za, zb, -0.5, 0.5, 0.8F, 0.6F, 1.0F, (0.15F + 0.5F * pulse) * shimmer);
            for (int side = -1; side <= 1; side += 2) {
                strip(vc, m, xa, xb, y + 0.01, za, zb, side * 1.55 - 0.08, side * 1.55 + 0.08, 0.95F, 0.8F, 1.0F, 0.85F * shimmer);
                strip(vc, m, xa, xb, y + 0.008, za, zb, side * 1.55 - 0.3, side * 1.55 + 0.3, 0.7F, 0.35F, 1.0F, 0.25F * shimmer);
            }
        }
    }

    /** A flat strip across the path between two of its rows, from {@code w0} to {@code w1} across. */
    private static void strip(VertexConsumer vc, Matrix4f m, double xa, double xb, double y, double za, double zb, double w0, double w1,
                              float r, float g, float b, float a) {
        quad(vc, m, xa + w0, y, za, xa + w1, y, za, xb + w1, y, zb, xb + w0, y, zb, r, g, b, a, a);
    }

    /** A platform: a disc of the path's light, its rim bright, a slow swirl in it. */
    private static void disc(VertexConsumer vc, Matrix4f m, Vec3 cam, float age, double x, double z, double radius) {
        double y = Cosmic.Y + 1.02 - cam.y;
        int segs = 40;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            float swirl = 0.8F + 0.2F * Mth.sin((float) (a0 * 3 + age * 0.08F));
            ring(vc, m, cam, x, y, z, a0, a1, 0.0, radius - 0.2, 0.55F, 0.25F, 0.95F, 0.35F * swirl, 0.2F * swirl);
            ring(vc, m, cam, x, y + 0.01, z, a0, a1, radius - 0.2, radius, 0.95F, 0.8F, 1.0F, 0.85F, 0.85F);
            ring(vc, m, cam, x, y + 0.005, z, a0, a1, radius, radius + 0.45, 0.7F, 0.35F, 1.0F, 0.3F, 0.0F);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, Vec3 cam, double x, double y, double z, double a0, double a1, double r0, double r1,
                             float r, float g, float b, float aIn, float aOut) {
        double cx = x - cam.x, cz = z - cam.z;
        quad(vc, m, cx + Math.cos(a0) * r0, y, cz + Math.sin(a0) * r0, cx + Math.cos(a1) * r0, y, cz + Math.sin(a1) * r0,
                cx + Math.cos(a1) * r1, y, cz + Math.sin(a1) * r1, cx + Math.cos(a0) * r1, y, cz + Math.sin(a0) * r1, r, g, b, aIn, aOut);
    }

    /** The centre of the n-th circle, the first standing across the Anomaly's path, the rest rising beyond it. */
    private static Vec3 centre(int n, Vec3 cam) {
        return Cosmic.circleCentre(n).subtract(cam);
    }

    /** The flames round a circle: a bright rim, tongues of fire of its colour licking outward, a soft halo. */
    private static void flames(VertexConsumer vc, Matrix4f m, Vec3 cam, float age, int n) {
        Vec3 c = centre(n, cam);
        int rgb = Chakras.colour(n + 1);
        float r = (rgb >> 16 & 255) / 255.0F, g = (rgb >> 8 & 255) / 255.0F, b = (rgb & 255) / 255.0F;
        double rad = Cosmic.CIRCLE_R;
        int segs = 72;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            float lick = 0.5F + 0.5F * Mth.sin(age * 0.35F + i * 1.7F + n) * Mth.sin(age * 0.21F + i * 0.9F);
            double tongue = 0.25 + 0.55 * lick;
            vertical(vc, m, c, a0, a1, rad - 0.12, rad + 0.08, Mth.lerp(0.6F, r, 1.0F), Mth.lerp(0.6F, g, 1.0F), Mth.lerp(0.6F, b, 1.0F), 0.95F, 0.95F);
            vertical(vc, m, c, a0, a1, rad + 0.08, rad + 0.08 + tongue, r, g, b, 0.8F, 0.0F);
            vertical(vc, m, c, a0, a1, rad - 0.5, rad - 0.12, r, g, b, 0.0F, 0.5F);
            vertical(vc, m, c, a0, a1, rad + 0.08, rad + 1.4, r, g, b, 0.18F, 0.0F);
        }
    }

    /** A piece of a ring standing across the path (in the plane of x and y), from r0 to r1. */
    private static void vertical(VertexConsumer vc, Matrix4f m, Vec3 c, double a0, double a1, double r0, double r1, float r, float g, float b,
                                 float aIn, float aOut) {
        quad(vc, m, c.x + Math.cos(a0) * r0, c.y + Math.sin(a0) * r0, c.z, c.x + Math.cos(a1) * r0, c.y + Math.sin(a1) * r0, c.z,
                c.x + Math.cos(a1) * r1, c.y + Math.sin(a1) * r1, c.z, c.x + Math.cos(a0) * r1, c.y + Math.sin(a0) * r1, c.z, r, g, b, aIn, aOut);
    }

    /** A circle's dark face and its glowing symbol (the open first circle lit red from within). */
    private static void circle(MultiBufferSource.BufferSource buffers, Matrix4f m, Vec3 cam, float age, int n) {
        Vec3 c = centre(n, cam);
        int rgb = Chakras.colour(n + 1);
        float r = (rgb >> 16 & 255) / 255.0F, g = (rgb >> 8 & 255) / 255.0F, b = (rgb & 255) / 255.0F;
        boolean open = n == 0;
        float pulse = 0.8F + 0.2F * Mth.sin(age * 0.15F + n);
        double rad = Cosmic.CIRCLE_R - 0.1;
        VertexConsumer face = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        int segs = 48;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            float fr = open ? 0.45F * pulse : 0.02F, fg = open ? 0.03F : 0.0F, fb = open ? 0.03F : 0.03F;
            textured(face, m, c.x, c.y, c.z, c.x + Math.cos(a0) * rad, c.y + Math.sin(a0) * rad, c.z, c.x + Math.cos(a1) * rad, c.y + Math.sin(a1) * rad, c.z,
                    c.x, c.y, c.z, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, fr, fg, fb, 0.93F);
        }
        buffers.endBatch(RenderType.entityTranslucentEmissive(WHITE));
        double h = rad * 0.82;
        float a = open ? 1.0F : 0.75F * pulse;
        VertexConsumer sym = buffers.getBuffer(RenderType.entityTranslucentEmissive(SYMBOLS[n]));
        symbol(sym, m, c.add(0, 0, -0.03), h, r, g, b, a);
        buffers.endBatch(RenderType.entityTranslucentEmissive(SYMBOLS[n]));
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(SYMBOLS[n]));
        symbol(glow, m, c.add(0, 0, -0.04), h * 1.04, r, g, b, (open ? 0.9F : 0.5F) * pulse);
        buffers.endBatch(RenderType.eyes(SYMBOLS[n]));
    }

    private static void symbol(VertexConsumer vc, Matrix4f m, Vec3 c, double h, float r, float g, float b, float a) {
        textured(vc, m, c.x - h, c.y - h, c.z, c.x + h, c.y - h, c.z, c.x + h, c.y + h, c.z, c.x - h, c.y + h, c.z,
                1, 1, 0, 1, 0, 0, 1, 0, r, g, b, a);
    }

    private static void textured(VertexConsumer vc, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2,
                                 double z2, double x3, double y3, double z3, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                 float r, float g, float b, float a) {
        double[][] p = {{x0, y0, z0, u0, v0}, {x1, y1, z1, u1, v1}, {x2, y2, z2, u2, v2}, {x3, y3, z3, u3, v3}};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                double[] q = p[pass == 0 ? k : 3 - k];
                vc.addVertex(m, (float) q[0], (float) q[1], (float) q[2]).setColor(r, g, b, a).setUv((float) q[3], (float) q[4])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FxGeometry.FULL).setNormal(0.0F, 0.0F, 1.0F);
            }
        }
    }

    /** An untextured quad of light, both faces; the first two corners take {@code aIn}, the last two {@code aOut}. */
    private static void quad(VertexConsumer vc, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2,
                             double z2, double x3, double y3, double z3, float r, float g, float b, float aIn, float aOut) {
        double[][] p = {{x0, y0, z0, aIn}, {x1, y1, z1, aIn}, {x2, y2, z2, aOut}, {x3, y3, z3, aOut}};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                double[] q = p[pass == 0 ? k : 3 - k];
                vc.addVertex(m, (float) q[0], (float) q[1], (float) q[2]).setColor(r, g, b, (float) q[3]);
            }
        }
    }

    private ChakraPathRenderer() {
    }
}
