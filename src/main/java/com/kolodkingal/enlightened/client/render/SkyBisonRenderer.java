package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.SkyBisonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The sky bison: its model, scaled down while it is a calf. */
public class SkyBisonRenderer extends MobRenderer<SkyBisonEntity, SkyBisonModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enlightened.id("sky_bison"), "main");
    private static final ResourceLocation TEXTURE = Enlightened.id("textures/entity/sky_bison.png");

    public SkyBisonRenderer(EntityRendererProvider.Context context) {
        super(context, new SkyBisonModel(context.bakeLayer(LAYER)), 1.6F);
    }

    @Override
    protected void scale(SkyBisonEntity bison, PoseStack poseStack, float partialTick) {
        float s = bison.getAgeScale();
        poseStack.scale(s, s, s);
        shadowRadius = 1.6F * s;
    }

    @Override
    public ResourceLocation getTextureLocation(SkyBisonEntity entity) {
        return TEXTURE;
    }
}
