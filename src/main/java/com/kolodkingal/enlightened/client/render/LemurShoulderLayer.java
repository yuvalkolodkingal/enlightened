package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.FlyingLemurEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A flying lemur riding on the player's shoulder, as the game draws a parrot there (the parrot's own layer draws only
 * parrots): sitting, its ears twitching, its head turning with the player's.
 */
public class LemurShoulderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = Enlightened.id("textures/entity/lemur.png");
    private static final String ID = Enlightened.MODID + ":flying_lemur";
    private final LemurModel model;
    /** A lemur that never enters the world, for the model to read its pose from. */
    private FlyingLemurEntity pose;

    public LemurShoulderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, LemurModel model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        shoulder(poseStack, buffer, packedLight, player, player.getShoulderEntityLeft(), true, ageInTicks, netHeadYaw, headPitch);
        shoulder(poseStack, buffer, packedLight, player, player.getShoulderEntityRight(), false, ageInTicks, netHeadYaw, headPitch);
    }

    private void shoulder(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CompoundTag tag, boolean left,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        if (tag == null || !ID.equals(tag.getString("id"))) {
            return;
        }
        FlyingLemurEntity lemur = pose(player.level());
        if (lemur == null) {
            return;
        }
        lemur.tickCount = player.tickCount;
        lemur.sitWeight = lemur.oSitWeight = 1.0F;
        lemur.flyWeight = lemur.oFlyWeight = 0.0F;
        lemur.earTwitch = (player.tickCount + (left ? 0 : 37)) % 90 < 4 ? 1.0F : 0.0F;
        poseStack.pushPose();
        // Its feet on the shoulder (the model's feet are at its 24th pixel).
        poseStack.translate(left ? 0.36F : -0.36F, player.isCrouching() ? -1.15F : -1.33F, 0.02F);
        poseStack.scale(0.85F, 0.85F, 0.85F);
        model.setupAnim(lemur, 0.0F, 0.0F, ageInTicks, netHeadYaw * 0.5F, headPitch * 0.5F);
        model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    private FlyingLemurEntity pose(Level level) {
        if (pose == null || pose.level() != level) {
            pose = ModEntities.LEMUR.get().create(level);
        }
        return pose;
    }
}
