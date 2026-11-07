package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.IrohEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Iroh on the player model, in his tea server's clothes (entity/iroh_tea.png). Flying down to a horn's call, his arms
 * point down and back, the fire jetting from his fists and feet.
 */
public class IrohRenderer extends HumanoidMobRenderer<IrohEntity, PlayerModel<IrohEntity>> {
    private static final ResourceLocation SKIN = Enlightened.id("textures/entity/iroh_tea.png");

    public IrohRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(IrohEntity entity) {
        return SKIN;
    }

    static final class Model extends PlayerModel<IrohEntity> {
        Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(IrohEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (entity.arriving() > 0) {
                float wobble = (float) Math.sin(ageInTicks * 0.6F) * 0.05F;
                rightArm.xRot = 0.35F + wobble;
                leftArm.xRot = 0.35F - wobble;
                rightArm.zRot = 0.3F;
                leftArm.zRot = -0.3F;
                rightLeg.xRot = 0.1F;
                leftLeg.xRot = -0.1F;
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
                rightPants.copyFrom(rightLeg);
                leftPants.copyFrom(leftLeg);
            }
        }
    }
}
