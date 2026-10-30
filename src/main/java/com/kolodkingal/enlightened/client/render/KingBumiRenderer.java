package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.KingBumiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * King Bumi on his throne: sitting back, his hands on the arms of the throne, his head following whoever comes, a
 * chuckle in his shoulders now and then; drawn on the player model in his look's skin.
 */
public class KingBumiRenderer extends MobRenderer<KingBumiEntity, KingBumiRenderer.Seated> {
    private static final ResourceLocation TEXTURE = Enlightened.id("textures/entity/king_bumi.png");

    public KingBumiRenderer(EntityRendererProvider.Context context) {
        super(context, new Seated(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public void render(KingBumiEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, -0.6, 0.0);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KingBumiEntity entity) {
        return TEXTURE;
    }

    /** The player model, seated, the forearms on the throne's arms. */
    public static final class Seated extends PlayerModel<KingBumiEntity> {
        Seated(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(KingBumiEntity king, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            riding = true;
            super.setupAnim(king, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
            float chuckle = Mth.sin(ageInTicks * 0.9F) * Math.max(0.0F, Mth.sin(ageInTicks * 0.05F) - 0.8F) * 5.0F;
            rightArm.xRot = -0.75F + chuckle * 0.1F;
            leftArm.xRot = -0.75F + chuckle * 0.1F;
            rightArm.zRot = 0.12F;
            leftArm.zRot = -0.12F;
            body.y = chuckle * 0.3F;
            head.y = chuckle * 0.3F;
            hat.copyFrom(head);
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            jacket.copyFrom(body);
        }
    }
}
