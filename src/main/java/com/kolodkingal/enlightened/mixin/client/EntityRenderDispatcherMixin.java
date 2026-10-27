package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.fx.BlueBurning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A creature set alight by Azula's fire burns with blue flames: the game's flame overlay is drawn with the soul fire sprites instead. Client only. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    private static final Material ENLIGHTENED$SOUL_0 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/soul_fire_0"));
    private static final Material ENLIGHTENED$SOUL_1 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/soul_fire_1"));

    @Inject(method = "renderFlame", at = @At("HEAD"), cancellable = true)
    private void enlightened$blueFlame(PoseStack poseStack, MultiBufferSource buffer, Entity entity, Quaternionf quaternion, CallbackInfo ci) {
        if (!BlueBurning.isBlue(entity)) {
            return;
        }
        ci.cancel();
        TextureAtlasSprite sprite0 = ENLIGHTENED$SOUL_0.sprite();
        TextureAtlasSprite sprite1 = ENLIGHTENED$SOUL_1.sprite();
        poseStack.pushPose();
        float width = entity.getBbWidth() * 1.4F;
        poseStack.scale(width, width, width);
        float half = 0.5F;
        float left = entity.getBbHeight() / width;
        float yOff = 0.0F;
        poseStack.mulPose(quaternion);
        poseStack.translate(0.0F, 0.0F, 0.3F - (float) ((int) left) * 0.02F);
        float z = 0.0F;
        int i = 0;
        VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());
        for (PoseStack.Pose pose = poseStack.last(); left > 0.0F; i++) {
            TextureAtlasSprite sprite = i % 2 == 0 ? sprite0 : sprite1;
            float u0 = sprite.getU0();
            float v0 = sprite.getV0();
            float u1 = sprite.getU1();
            float v1 = sprite.getV1();
            if (i / 2 % 2 == 0) {
                float swap = u1;
                u1 = u0;
                u0 = swap;
            }
            enlightened$vertex(pose, vc, -half, 0.0F - yOff, z, u1, v1);
            enlightened$vertex(pose, vc, half, 0.0F - yOff, z, u0, v1);
            enlightened$vertex(pose, vc, half, 1.4F - yOff, z, u0, v0);
            enlightened$vertex(pose, vc, -half, 1.4F - yOff, z, u1, v0);
            left -= 0.45F;
            yOff -= 0.45F;
            half *= 0.9F;
            z -= 0.03F;
        }
        poseStack.popPose();
    }

    private static void enlightened$vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v) {
        vc.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setUv1(0, 10).setLight(240).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
