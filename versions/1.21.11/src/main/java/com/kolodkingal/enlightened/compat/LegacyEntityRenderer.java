package com.kolodkingal.enlightened.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.11: a renderer of the mod written against the 1.21.1 renderer, which draws from the entity: it gets the
 * render state's entity ({@link RenderBridge}) and draws into the level's buffers; its {@code super.render(...)}
 * submits what 1.21.1's drew (the name).
 */
public abstract class LegacyEntityRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {
    private EntityRenderState current;
    private SubmitNodeCollector collector;
    private CameraRenderState camera;

    protected LegacyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Entity entity = RenderBridge.entity(state);
        if (entity == null) {
            super.submit(state, pose, collector, camera);
            return;
        }
        EntityRenderState before = current;
        SubmitNodeCollector beforeCollector = this.collector;
        CameraRenderState beforeCamera = this.camera;
        current = state;
        this.collector = collector;
        this.camera = camera;
        try {
            render((T) entity, Mth.rotLerp(state.partialTick, entity.yRotO, entity.getYRot()), state.partialTick, pose, RenderBridge.buffers(), state.lightCoords);
        } finally {
            current = before;
            this.collector = beforeCollector;
            this.camera = beforeCamera;
        }
    }

    /** 1.21.1's render: draws the entity; this default submits what vanilla adds (the name tag). */
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (current != null) {
            super.submit(current, pose, collector, camera);
        }
    }

    /** The collector of the submit being answered (for what must be submitted: items). */
    protected SubmitNodeCollector collector() {
        return collector;
    }

    /** 1.21.1's texture of the entity (nothing asks for it from 1.21.2; kept for the renderers that use it themselves). */
    public Identifier getTextureLocation(T entity) {
        return null;
    }
}
