package com.kolodkingal.enlightened.compat;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.11: from 1.21.2 a renderer draws from a render state, not from the entity. The mod's poses, layers and
 * effects read the entity (its bending, its gestures), so every render state carries the entity it was made from
 * (NeoForge's render data), and {@link #entity} gives it back.
 *
 * <p>From 1.21.9 a renderer no longer draws: it submits to a {@code SubmitNodeCollector}, which draws later in the
 * frame. The mod's renderers and layers written against 1.21.1 draw straight into the level's buffers ({@link #buffers}),
 * which the level flushes after the entities (vanilla's own parts still go through the collector).
 */
public final class RenderBridge {
    private static final ContextKey<Entity> ENTITY = new ContextKey<>(Enlightened.id("entity"));

    /**
     * The render state of the player the poses are being applied to now (set by PlayerModelMixin): what was the
     * model's own arm poses, crouch and swing in 1.21.1.
     */
    public static AvatarRenderState pose = new AvatarRenderState();

    private RenderBridge() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) EntityRenderer.class, (entity, state) -> ((EntityRenderState) state).setRenderData(ENTITY, (Entity) entity));
    }

    /** The entity a render state was made from (null for one made by hand). */
    @Nullable
    public static Entity entity(EntityRenderState state) {
        return state.getRenderData(ENTITY);
    }

    /** The level's buffers, which the code written against 1.21.1 draws into while the entities are submitted. */
    public static MultiBufferSource.BufferSource buffers() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }
}
