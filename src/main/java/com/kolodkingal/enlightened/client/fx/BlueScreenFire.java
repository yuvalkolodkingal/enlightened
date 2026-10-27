package com.kolodkingal.enlightened.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** The flames over the screen of a player burning with blue fire: the game's own overlay, with the soul fire sprite. */
public final class BlueScreenFire {
    private static final Material SOUL = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/soul_fire_1"));

    public static void render(PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.depthFunc(519);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        TextureAtlasSprite sprite = SOUL.sprite();
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float um = (u0 + u1) / 2.0F;
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float vm = (v0 + v1) / 2.0F;
        float shrink = sprite.uvShrinkRatio();
        float ua = Mth.lerp(shrink, u0, um);
        float ub = Mth.lerp(shrink, u1, um);
        float va = Mth.lerp(shrink, v0, vm);
        float vb = Mth.lerp(shrink, v1, vm);
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.translate((float) (-(i * 2 - 1)) * 0.24F, -0.3F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (i * 2 - 1) * 10.0F));
            Matrix4f m = poseStack.last().pose();
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            b.addVertex(m, -0.5F, -0.5F, -0.5F).setUv(ub, vb).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            b.addVertex(m, 0.5F, -0.5F, -0.5F).setUv(ua, vb).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            b.addVertex(m, 0.5F, 0.5F, -0.5F).setUv(ua, va).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            b.addVertex(m, -0.5F, 0.5F, -0.5F).setUv(ub, va).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            BufferUploader.drawWithShader(b.buildOrThrow());
            poseStack.popPose();
        }
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);
    }

    private BlueScreenFire() {
    }
}
