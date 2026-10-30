package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.AnomalyEntity;
import com.kolodkingal.enlightened.client.fx.FxGeometry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The Anomaly ({@link AnomalyArt}). It is drawn not with the other creatures but after them, with the fog put aside: it
 * is light itself, and shines through the darkness it brings (the Darkness effect closes the fog in, which would hide
 * anything else).
 */
public class AnomalyRenderer extends EntityRenderer<AnomalyEntity> {
    private static final ResourceLocation WHITE = com.kolodkingal.enlightened.Enlightened.id("textures/entity/anomaly.png");
    /** How far off an Anomaly is still drawn. */
    private static final double RANGE = 128.0;

    public AnomalyRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(AnomalyEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        // Drawn in the level's late stage instead (drawAll), past the fog.
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    /** Draws every Anomaly near the camera, the fog lifted while it is drawn and put back after. */
    public static void drawAll(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var list = mc.level.getEntitiesOfClass(AnomalyEntity.class, new net.minecraft.world.phys.AABB(cam, cam).inflate(RANGE), AnomalyEntity::isAlive);
        if (list.isEmpty()) {
            return;
        }
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE / 2);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        MultiBufferSource.BufferSource buffers = FxGeometry.source();
        PoseStack pose = event.getPoseStack();
        for (AnomalyEntity e : list) {
            Vec3 mid = e.getPosition(partial).add(0, e.getBbHeight() / 2.0, 0);
            pose.pushPose();
            pose.translate(mid.x - cam.x, mid.y - cam.y, mid.z - cam.z);
            AnomalyArt.draw(pose, buffers, mc.getEntityRenderDispatcher().cameraOrientation(), cam.subtract(mid), e.tickCount + partial, e.getId(),
                    1.5F, e.hurtTime > 0 ? 1.0F : 0.0F, 8, 9.0);
            pose.popPose();
            buffers.endBatch();
        }
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
    }

    @Override
    public ResourceLocation getTextureLocation(AnomalyEntity entity) {
        return WHITE;
    }
}
