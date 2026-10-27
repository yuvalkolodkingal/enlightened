package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.GuruPathikEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Guru Pathik, sitting in the lotus on the floor of his meditation room, breathing slowly, his eyes following whoever
 * comes; drawn on the player model in his own skin (an old bald hermit with a long white beard and an orange cloth).
 */
public class GuruPathikRenderer extends MobRenderer<GuruPathikEntity, MeditatingBodyModel<GuruPathikEntity>> {
    private static final ResourceLocation TEXTURE = Enlightened.id("textures/entity/guru_pathik.png");

    public GuruPathikRenderer(EntityRendererProvider.Context context) {
        super(context, new MeditatingBodyModel<>(context.bakeLayer(ModelLayers.PLAYER), true), 0.5F);
    }

    @Override
    public void render(GuruPathikEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float breath = 0.012F * (float) Math.sin((entity.tickCount + partialTick) * 0.08F);
        poseStack.translate(0.0, -0.56 + breath, 0.0);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GuruPathikEntity entity) {
        return TEXTURE;
    }
}
