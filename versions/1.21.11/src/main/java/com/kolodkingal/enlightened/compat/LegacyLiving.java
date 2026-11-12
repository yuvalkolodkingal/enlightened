package com.kolodkingal.enlightened.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.21.11: what the living renderers of the mod that were written against 1.21.1 share ({@link LegacyLivingRenderer},
 * {@link LegacyMobRenderer}, {@link LegacyHumanoidRenderer}): every one draws an {@link AvatarRenderState} (the state of
 * the player model, which some of them use, and a living entity's state for the others), and calls back the 1.21.1
 * methods with the render state's entity. The 1.21.1 code draws into the level's buffers; vanilla's part (the model,
 * the layers, the name) is submitted to the collector of the submit being answered.
 */
final class LegacyLiving {
    private LegacyLiving() {
    }

    interface Hooks<T extends LivingEntity> {
        AvatarRenderState legacyCurrent();

        void legacySetCurrent(AvatarRenderState state, SubmitNodeCollector collector, CameraRenderState camera);

        SubmitNodeCollector legacyCollector();

        CameraRenderState legacyCamera();

        void legacyRender(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light);

        void vanillaSubmit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera);

        Identifier getTextureLocation(T entity);
    }

    @SuppressWarnings("unchecked")
    static <T extends LivingEntity> void submit(Hooks<T> hooks, AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Entity entity = RenderBridge.entity(state);
        if (!(entity instanceof LivingEntity)) {
            hooks.vanillaSubmit(state, pose, collector, camera);
            return;
        }
        AvatarRenderState before = hooks.legacyCurrent();
        SubmitNodeCollector beforeCollector = hooks.legacyCollector();
        CameraRenderState beforeCamera = hooks.legacyCamera();
        hooks.legacySetCurrent(state, collector, camera);
        try {
            hooks.legacyRender((T) entity, state.bodyRot, state.partialTick, pose, RenderBridge.buffers(), state.lightCoords);
        } finally {
            hooks.legacySetCurrent(before, beforeCollector, beforeCamera);
        }
    }

    /** 1.21.1's {@code super.render(...)}: vanilla's submit of the state being drawn. */
    static <T extends LivingEntity> void vanilla(Hooks<T> hooks, PoseStack pose) {
        AvatarRenderState state = hooks.legacyCurrent();
        if (state != null && hooks.legacyCollector() != null) {
            hooks.vanillaSubmit(state, pose, hooks.legacyCollector(), hooks.legacyCamera());
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends LivingEntity> Identifier texture(Hooks<T> hooks, AvatarRenderState state) {
        Entity entity = RenderBridge.entity(state);
        return entity instanceof LivingEntity ? hooks.getTextureLocation((T) entity) : state.skin.body().texturePath();
    }
}
