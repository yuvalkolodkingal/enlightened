package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.FlyingLemurEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the flying lemur with its model: banked into its turns and nosed into its climbs and dives in flight. */
public class LemurRenderer extends MobRenderer<FlyingLemurEntity, LemurModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enlightened.id("lemur"), "main");
    private static final ResourceLocation TEXTURE = Enlightened.id("textures/entity/lemur.png");

    public LemurRenderer(EntityRendererProvider.Context context) {
        super(context, new LemurModel(context.bakeLayer(LAYER)), 0.3F);
    }

    @Override
    protected void setupRotations(FlyingLemurEntity lemur, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(lemur, poseStack, bob, yBodyRot, partialTick, scale);
        float bank = Mth.lerp(partialTick, lemur.oBank, lemur.bank);
        float pitch = Mth.lerp(partialTick, lemur.oPitchLean, lemur.pitchLean);
        if (Math.abs(bank) > 0.01F || Math.abs(pitch) > 0.01F) {
            poseStack.translate(0.0F, 0.3F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(bank));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0.0F, -0.3F, 0.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingLemurEntity entity) {
        return TEXTURE;
    }
}
