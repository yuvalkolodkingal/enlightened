package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The lion turtle's head: its model four times over, its chin in the sea. */
public class LionTurtleRenderer extends MobRenderer<LionTurtleEntity, LionTurtleModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enlightened.id("lion_turtle"), "main");
    private static final ResourceLocation TEXTURE = Enlightened.id("textures/entity/lion_turtle.png");
    public static final float SCALE = 4.0F;

    public LionTurtleRenderer(EntityRendererProvider.Context context) {
        super(context, new LionTurtleModel(context.bakeLayer(LAYER)), 0.0F);
    }

    @Override
    protected void scale(LionTurtleEntity turtle, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(LionTurtleEntity entity) {
        return TEXTURE;
    }
}
