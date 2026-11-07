package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.NationNpcEntity;
import com.kolodkingal.enlightened.entity.OzaiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * The people of the Fire Nation on the player model, in the skins Eilon sent (1.0.28): Ozai as the Phoenix King (on his
 * jets his arms point down and back; in his cell his head is bowed), Fire Lord Zuko seated on the throne, and the
 * herald and the palace guards (slim arms).
 */
public final class NationRenderer {
    private static final ResourceLocation OZAI = Enlightened.id("textures/entity/ozai.png");
    private static final ResourceLocation ZUKO = Enlightened.id("textures/entity/zuko_fire_lord.png");
    private static final ResourceLocation GUARD = Enlightened.id("textures/entity/fire_guard.png");

    public static Ozai ozai(EntityRendererProvider.Context context) {
        return new Ozai(context);
    }

    public static Npc npc(EntityRendererProvider.Context context) {
        return new Npc(context);
    }

    public static final class Ozai extends HumanoidMobRenderer<OzaiEntity, PlayerModel<OzaiEntity>> {
        Ozai(EntityRendererProvider.Context context) {
            super(context, new Jets(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(OzaiEntity entity) {
            return OZAI;
        }
    }

    /** Zuko and the guards: Zuko wide-armed and seated on his throne, the guards slim-armed. */
    public static final class Npc extends HumanoidMobRenderer<NationNpcEntity, PlayerModel<NationNpcEntity>> {
        private final PlayerModel<NationNpcEntity> seated;
        private final PlayerModel<NationNpcEntity> slim;

        Npc(EntityRendererProvider.Context context) {
            super(context, new Seated(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
            this.seated = getModel();
            this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }

        @Override
        public void render(NationNpcEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
            boolean zuko = entity.kind() == NationNpcEntity.Kind.ZUKO;
            model = zuko ? seated : slim;
            pose.pushPose();
            if (zuko) {
                pose.translate(0.0, -0.55, 0.0);
            }
            super.render(entity, yaw, partialTick, pose, buffer, light);
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(NationNpcEntity entity) {
            return entity.kind() == NationNpcEntity.Kind.ZUKO ? ZUKO : GUARD;
        }
    }

    static final class Seated extends PlayerModel<NationNpcEntity> {
        Seated(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(NationNpcEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            riding = true;
            super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
            rightArm.xRot = -0.7F;
            leftArm.xRot = -0.7F;
            rightArm.zRot = 0.1F;
            leftArm.zRot = -0.1F;
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
        }
    }

    static final class Jets extends PlayerModel<OzaiEntity> {
        Jets(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(OzaiEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (entity.jets()) {
                float wobble = (float) Math.sin(ageInTicks * 0.6F) * 0.05F;
                rightArm.xRot = 0.4F + wobble;
                leftArm.xRot = 0.4F - wobble;
                rightArm.zRot = 0.35F;
                leftArm.zRot = -0.35F;
                rightLeg.xRot = 0.15F;
                leftLeg.xRot = -0.05F;
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
                rightPants.copyFrom(rightLeg);
                leftPants.copyFrom(leftLeg);
            } else if (entity.prisoner()) {
                head.xRot = 0.5F;
                hat.copyFrom(head);
            }
        }
    }

    private NationRenderer() {
    }
}
