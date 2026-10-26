package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.MeditatingBodyEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Draws the body an airbender left in meditation: his own skin (the look he wears, if he is in view; else the skin
 * of his account), sitting still with his arms at rest.
 */
public class MeditatingBodyRenderer extends LivingEntityRenderer<MeditatingBodyEntity, PlayerModel<MeditatingBodyEntity>> {
    public MeditatingBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public void render(MeditatingBodyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        PlayerModel<MeditatingBodyEntity> model = getModel();
        model.hat.visible = true;
        model.jacket.visible = true;
        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;
        poseStack.pushPose();
        // Sitting: the body is drawn lower, the legs folded by the riding pose.
        poseStack.translate(0.0, -0.5, 0.0);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    protected boolean isShaking(MeditatingBodyEntity entity) {
        return false;
    }

    @Override
    protected void setupRotations(MeditatingBodyEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        getModel().riding = true;
    }

    @Override
    public ResourceLocation getTextureLocation(MeditatingBodyEntity entity) {
        UUID owner = entity.ownerUuid();
        if (owner != null && Minecraft.getInstance().level != null) {
            Player player = Minecraft.getInstance().level.getPlayerByUUID(owner);
            if (player instanceof AbstractClientPlayer client) {
                return client.getSkin().texture();
            }
            return Minecraft.getInstance().getSkinManager().getInsecureSkin(new GameProfile(owner, entity.ownerName())).texture();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    protected boolean shouldShowName(MeditatingBodyEntity entity) {
        return true;
    }
}
