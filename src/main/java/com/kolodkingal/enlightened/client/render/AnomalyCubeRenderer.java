package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.block.AnomalyCubeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A red cube of the Anomaly, drawn as a small Anomaly of pure energy ({@link AnomalyArt}) in the middle of its block. */
public class AnomalyCubeRenderer implements BlockEntityRenderer<AnomalyCubeBlockEntity> {
    public AnomalyCubeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AnomalyCubeBlockEntity cube, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (cube.getLevel() == null) {
            return;
        }
        var pos = cube.getBlockPos();
        long seed = pos.asLong();
        float age = cube.getLevel().getGameTime() + partialTick + (seed & 255);
        pose.pushPose();
        pose.translate(0.5, 0.5 + 0.05 * Math.sin(age * 0.07), 0.5);
        Vec3 toCam = mc.gameRenderer.getMainCamera().getPosition().subtract(Vec3.atCenterOf(pos));
        AnomalyArt.draw(pose, buffer, mc.getEntityRenderDispatcher().cameraOrientation(), toCam, age, seed, 0.24F, 0.0F, 3, 1.4);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AnomalyCubeBlockEntity cube) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(AnomalyCubeBlockEntity cube) {
        return new AABB(cube.getBlockPos()).inflate(1.5);
    }
}
