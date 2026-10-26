package com.kolodkingal.enlightened.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.phys.Vec3;

/**
 * Draws nothing: the last layer of a player, it records into {@link HandTracker} where the hands, feet and forehead
 * of a bender are in the world this frame, from the pose stack the body was just drawn with.
 */
public class HandTrackerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float FIST_Y = 8.5F;

    public HandTrackerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        AnimState state = Animator.peek(player);
        if (state == null || state.wVanilla > 0.0F || !state.posed) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        float fist = slim ? 0.5F : 1.0F;
        Vec3 handRight = at(poseStack, model.rightArm, -fist, FIST_Y, 0.0F);
        Vec3 handLeft = at(poseStack, model.leftArm, fist, FIST_Y, 0.0F);
        Vec3 footRight = at(poseStack, model.rightLeg, 0.0F, 12.0F, 0.0F);
        Vec3 footLeft = at(poseStack, model.leftLeg, 0.0F, 12.0F, 0.0F);
        Vec3 forehead = at(poseStack, model.head, 0.0F, -6.0F, -4.15F);
        HandTracker.record(player, ageInTicks, handRight, handLeft, footRight, footLeft, forehead);
    }

    private static Vec3 at(PoseStack ps, ModelPart part, float x, float y, float z) {
        ps.pushPose();
        part.translateAndRotate(ps);
        Vec3 world = HandTracker.world(ps, x / 16.0F, y / 16.0F, z / 16.0F);
        ps.popPose();
        return world;
    }
}
