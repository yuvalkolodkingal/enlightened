package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.TornadoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws the tornado: a translucent funnel of twisting bands, narrow at the ground and wide at the top, each band
 * turning faster the lower it is, the whole funnel swaying and fading toward its rim. The cloud particles the server
 * sends ride inside it.
 */
public class TornadoRenderer extends EntityRenderer<TornadoEntity> {
    private static final int BANDS = 14;
    private static final int SEGMENTS = 18;

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(TornadoEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = entity.tickCount + partialTick;
        float size = entity.size();
        float height = size * 2.5F;
        // It grows out of the ground over its first ticks and sways on its base.
        float grow = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
        grow = 1.0F - (1.0F - grow) * (1.0F - grow);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(3.0F * Mth.sin(t * 0.09F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(3.0F * Mth.cos(t * 0.07F)));
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        for (int b = 0; b < BANDS; b++) {
            float f0 = (float) b / BANDS;
            float f1 = (float) (b + 1) / BANDS;
            float y0 = f0 * height * grow;
            float y1 = f1 * height * grow;
            float r0 = size * (0.25F + 0.75F * f0) * grow * (1.0F + 0.06F * Mth.sin(t * 0.3F + b));
            float r1 = size * (0.25F + 0.75F * f1) * grow * (1.0F + 0.06F * Mth.sin(t * 0.3F + b + 1));
            // The band twists with time, lower bands faster; the rim of the funnel thins out.
            float spin0 = t * (0.35F - 0.2F * f0) + b * 0.6F;
            float spin1 = t * (0.35F - 0.2F * f1) + (b + 1) * 0.6F;
            float a0 = 0.22F * (1.0F - f0 * 0.7F) * grow;
            float a1 = 0.22F * (1.0F - f1 * 0.7F) * grow;
            for (int i = 0; i < SEGMENTS; i++) {
                float u0 = (float) i / SEGMENTS * Mth.TWO_PI;
                float u1 = (float) (i + 1) / SEGMENTS * Mth.TWO_PI;
                // Every other segment is a lighter streak, so the turn can be seen.
                float streak = (i % 2 == 0) ? 1.0F : 0.55F;
                float x00 = Mth.cos(u0 + spin0) * r0;
                float z00 = Mth.sin(u0 + spin0) * r0;
                float x10 = Mth.cos(u1 + spin0) * r0;
                float z10 = Mth.sin(u1 + spin0) * r0;
                float x01 = Mth.cos(u0 + spin1) * r1;
                float z01 = Mth.sin(u0 + spin1) * r1;
                float x11 = Mth.cos(u1 + spin1) * r1;
                float z11 = Mth.sin(u1 + spin1) * r1;
                vertex(consumer, matrix, x00, y0, z00, a0 * streak);
                vertex(consumer, matrix, x01, y1, z01, a1 * streak);
                vertex(consumer, matrix, x11, y1, z11, a1 * streak);
                vertex(consumer, matrix, x10, y0, z10, a0 * streak);
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float alpha) {
        consumer.addVertex(matrix, x, y, z).setColor(0.88F, 0.92F, 0.98F, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(TornadoEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
