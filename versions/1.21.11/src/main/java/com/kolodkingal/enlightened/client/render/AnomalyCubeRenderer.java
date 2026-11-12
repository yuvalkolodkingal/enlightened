package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.block.AnomalyCubeBlockEntity;
import com.kolodkingal.enlightened.compat.RenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A red cube of the Anomaly, drawn as a small Anomaly of pure energy ({@link AnomalyArt}) in the middle of its block.
 * 1.21.11: the age is taken into a render state, and the energy drawn into the level's buffers while it is submitted.
 */
public class AnomalyCubeRenderer implements BlockEntityRenderer<AnomalyCubeBlockEntity, AnomalyCubeRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        float age;
        long seed;
    }

    public AnomalyCubeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AnomalyCubeBlockEntity cube, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(cube, state, partialTick, camera, crumbling);
        state.seed = cube.getBlockPos().asLong();
        state.age = (cube.getLevel() == null ? 0 : cube.getLevel().getGameTime()) + partialTick + (state.seed & 255);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.age;
        pose.pushPose();
        pose.translate(0.5, 0.5 + 0.05 * Math.sin(age * 0.07), 0.5);
        Vec3 toCam = camera.pos.subtract(Vec3.atCenterOf(state.blockPos));
        AnomalyArt.draw(pose, RenderBridge.buffers(), camera.orientation, toCam, age, state.seed, 0.24F, 0.0F, 3, 1.4);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(AnomalyCubeBlockEntity cube) {
        return new AABB(cube.getBlockPos()).inflate(1.5);
    }
}
