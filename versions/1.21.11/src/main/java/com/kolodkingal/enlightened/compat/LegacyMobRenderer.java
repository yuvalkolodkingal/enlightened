package com.kolodkingal.enlightened.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** 1.21.11: a {@code MobRenderer} of the mod written against 1.21.1 (see {@link LegacyLiving}). */
public abstract class LegacyMobRenderer<T extends Mob, M extends EntityModel<? super AvatarRenderState>> extends net.minecraft.client.renderer.entity.MobRenderer<T, AvatarRenderState, M>
        implements LegacyLiving.Hooks<T> {
    private AvatarRenderState current;
    private SubmitNodeCollector collector;
    private CameraRenderState camera;
    private double distance;

    protected LegacyMobRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public AvatarRenderState legacyCurrent() {
        return current;
    }

    @Override
    public void legacySetCurrent(AvatarRenderState state, SubmitNodeCollector collector, CameraRenderState camera) {
        current = state;
        this.collector = collector;
        this.camera = camera;
    }

    @Override
    public SubmitNodeCollector legacyCollector() {
        return collector;
    }

    @Override
    public CameraRenderState legacyCamera() {
        return camera;
    }

    @Override
    public final void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        LegacyLiving.submit(this, state, pose, collector, camera);
    }

    @Override
    public void vanillaSubmit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
    }

    @Override
    public void legacyRender(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** 1.21.1's render: this default submits the model as vanilla does. */
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        LegacyLiving.vanilla(this, pose);
    }

    @Override
    public final Identifier getTextureLocation(AvatarRenderState state) {
        return LegacyLiving.texture(this, state);
    }

    @Override
    public abstract Identifier getTextureLocation(T entity);

    @Override
    @SuppressWarnings("unchecked")
    protected final void scale(AvatarRenderState state, PoseStack pose) {
        super.scale(state, pose);
        Entity entity = RenderBridge.entity(state);
        if (entity instanceof LivingEntity) {
            scale((T) entity, pose, state.partialTick);
        }
    }

    /** 1.21.1's scale. */
    protected void scale(T entity, PoseStack pose, float partialTick) {
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final void setupRotations(AvatarRenderState state, PoseStack pose, float bodyRot, float scale) {
        Entity entity = RenderBridge.entity(state);
        if (!(entity instanceof LivingEntity)) {
            super.setupRotations(state, pose, bodyRot, scale);
            return;
        }
        AvatarRenderState before = current;
        current = state;
        try {
            setupRotations((T) entity, pose, state.ageInTicks, bodyRot, state.partialTick, scale);
        } finally {
            current = before;
        }
    }

    /** 1.21.1's setupRotations: this default turns the body as vanilla does. */
    protected void setupRotations(T entity, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(current, pose, yBodyRot, scale);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final boolean isShaking(AvatarRenderState state) {
        Entity entity = RenderBridge.entity(state);
        return entity instanceof LivingEntity ? isShaking((T) entity) : super.isShaking(state);
    }

    /** 1.21.1's isShaking. */
    protected boolean isShaking(T entity) {
        return entity.isFullyFrozen();
    }

    @Override
    protected final boolean shouldShowName(T entity, double distanceSq) {
        distance = distanceSq;
        return shouldShowName(entity);
    }

    /** 1.21.1's shouldShowName. */
    protected boolean shouldShowName(T entity) {
        return super.shouldShowName(entity, distance);
    }
}
