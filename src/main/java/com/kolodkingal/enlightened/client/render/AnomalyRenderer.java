package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.AnomalyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** The Anomaly, as pure energy ({@link AnomalyArt}), hung at the middle of its body. */
public class AnomalyRenderer extends EntityRenderer<AnomalyEntity> {
    private static final ResourceLocation WHITE = com.kolodkingal.enlightened.Enlightened.id("textures/entity/anomaly.png");

    public AnomalyRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(AnomalyEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.translate(0.0, entity.getBbHeight() / 2.0, 0.0);
        Vec3 toCam = entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2.0, 0));
        AnomalyArt.draw(pose, buffer, entityRenderDispatcher.cameraOrientation(), toCam, entity.tickCount + partialTick, entity.getId(), 1.5F,
                entity.hurtTime > 0 ? 1.0F : 0.0F, 8, 9.0);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(AnomalyEntity entity) {
        return WHITE;
    }
}
