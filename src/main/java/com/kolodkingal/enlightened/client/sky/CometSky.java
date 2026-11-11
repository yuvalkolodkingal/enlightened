package com.kolodkingal.enlightened.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
//#if <1.21.5
import com.mojang.blaze3d.vertex.BufferUploader;
//#endif
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/**
 * Sozin's Comet in the sky (1.0.27): while it passes, a red veil lies over the whole sky and the fog, and the comet
 * itself, a burning head with a long tail of fire, crosses it slowly. It fades in and out over two seconds.
 */
public final class CometSky {
    //#if >=1.21.5
    //$$ // (1.21.5 draws through render pipelines: the comet's is the game's position-colour one, seen through everything)
    //$$ private static final com.mojang.blaze3d.pipeline.RenderPipeline PIPELINE = com.mojang.blaze3d.pipeline.RenderPipeline
    //$$         .builder(net.minecraft.client.renderer.RenderPipelines.DEBUG_FILLED_SNIPPET)
    //$$         .withLocation(com.kolodkingal.enlightened.Enlightened.id("pipeline/comet_sky"))
    //$$         .withCull(false).withDepthWrite(false)
    //$$         .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
    //$$         .build();
    //$$ private static final net.minecraft.client.renderer.RenderType TYPE = net.minecraft.client.renderer.RenderType.create(
    //$$         "enlightened_comet_sky", 1536, PIPELINE, net.minecraft.client.renderer.RenderType.CompositeState.builder().createCompositeState(false));
    //#endif
    private static boolean active;
    private static float strength;

    public static void set(boolean on) {
        active = on;
    }

    public static boolean active() {
        return active;
    }

    /** How strongly the comet shows (0 to 1), easing in and out. */
    public static float strength() {
        return strength;
    }

    public static void tick() {
        strength = Mth.clamp(strength + (active ? 0.025F : -0.025F), 0.0F, 1.0F);
        if (Minecraft.getInstance().level == null) {
            active = false;
            strength = 0.0F;
        }
    }

    /** The fog leans to the comet's red. */
    public static void fog(ViewportEvent.ComputeFogColor event) {
        if (strength <= 0.0F) {
            return;
        }
        float s = strength * 0.75F;
        event.setRed(Mth.lerp(s, event.getRed(), 0.78F));
        event.setGreen(Mth.lerp(s, event.getGreen(), 0.16F));
        event.setBlue(Mth.lerp(s, event.getBlue(), 0.08F));
    }

    public static void render(RenderLevelStageEvent event) {
        if (strength <= 0.0F || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Matrix4f m = new Matrix4f(event.getModelViewMatrix());
        float time = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        //#if >=1.21.5
        //$$ net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        //$$ VertexConsumer b = buffers.getBuffer(TYPE);
        //#else
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        //#endif
        // The red veil: a box round the eye, deeper red overhead, lighter at the horizon.
        float r = 90.0F;
        int top = (int) (150 * strength);
        int side = (int) (110 * strength);
        quad(b, m, -r, r, -r, r, r, -r, r, r, r, -r, r, r, 0.62F, 0.06F, 0.03F, top, top);
        quad(b, m, -r, -r, -r, r, -r, -r, r, r, -r, -r, r, -r, 0.75F, 0.18F, 0.06F, side / 2, side);
        quad(b, m, -r, -r, r, r, -r, r, r, r, r, -r, r, r, 0.75F, 0.18F, 0.06F, side / 2, side);
        quad(b, m, -r, -r, -r, -r, -r, r, -r, r, r, -r, r, -r, 0.75F, 0.18F, 0.06F, side / 2, side);
        quad(b, m, r, -r, -r, r, -r, r, r, r, r, r, r, -r, 0.75F, 0.18F, 0.06F, side / 2, side);
        // The comet: its head high in the sky, crossing slowly; its tail streams away behind it.
        double a = time * 0.0006;
        Vec3 head = new Vec3(Math.cos(a) * 0.75, 0.55, Math.sin(a) * 0.75).normalize().scale(80);
        Vec3 along = new Vec3(-Math.sin(a), -0.18, Math.cos(a)).normalize();
        Vec3 up = head.normalize();
        Vec3 side1 = along.cross(up).normalize();
        float headSize = 3.2F;
        int alpha = (int) (255 * strength);
        // The tail: segments that widen and fade.
        int segments = 14;
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments;
            double t1 = (i + 1) / (double) segments;
            Vec3 p0 = head.subtract(along.scale(t0 * 55));
            Vec3 p1 = head.subtract(along.scale(t1 * 55));
            double w0 = headSize * (0.8 + t0 * 2.6);
            double w1 = headSize * (0.8 + t1 * 2.6);
            int a0 = (int) (alpha * (1 - t0) * 0.85);
            int a1 = (int) (alpha * (1 - t1) * 0.85);
            float g0 = (float) (0.75 - t0 * 0.55);
            vertex(b, m, p0.add(side1.scale(w0)), 1.0F, g0, 0.25F, a0);
            vertex(b, m, p0.subtract(side1.scale(w0)), 1.0F, g0, 0.25F, a0);
            vertex(b, m, p1.subtract(side1.scale(w1)), 1.0F, g0 - 0.04F, 0.2F, a1);
            vertex(b, m, p1.add(side1.scale(w1)), 1.0F, g0 - 0.04F, 0.2F, a1);
        }
        // The head: a bright core in a glow.
        Vec3 cross2 = side1.cross(up).normalize();
        for (int layer = 0; layer < 2; layer++) {
            double s = layer == 0 ? headSize * 2.2 : headSize;
            int al = layer == 0 ? alpha / 3 : alpha;
            float g = layer == 0 ? 0.6F : 0.95F;
            vertex(b, m, head.add(side1.scale(s)).add(cross2.scale(s)), 1.0F, g, 0.5F, al);
            vertex(b, m, head.add(side1.scale(s)).subtract(cross2.scale(s)), 1.0F, g, 0.5F, al);
            vertex(b, m, head.subtract(side1.scale(s)).subtract(cross2.scale(s)), 1.0F, g, 0.5F, al);
            vertex(b, m, head.subtract(side1.scale(s)).add(cross2.scale(s)), 1.0F, g, 0.5F, al);
        }
        //#if >=1.21.5
        //$$ buffers.endBatch(TYPE);
        //#else
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        //#endif
    }

    private static void quad(VertexConsumer b, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, int a0, int a1) {
        b.addVertex(m, x0, y0, z0).setColor(red, green, blue, a0 / 255.0F);
        b.addVertex(m, x1, y1, z1).setColor(red, green, blue, a0 / 255.0F);
        b.addVertex(m, x2, y2, z2).setColor(red, green, blue, a1 / 255.0F);
        b.addVertex(m, x3, y3, z3).setColor(red, green, blue, a1 / 255.0F);
    }

    private static void vertex(VertexConsumer b, Matrix4f m, Vec3 p, float red, float green, float blue, int alpha) {
        b.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(red, green, blue, Math.max(0, alpha) / 255.0F);
    }

    private CometSky() {
    }
}
