package com.kolodkingal.enlightened.compat;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3: from 1.21.2 a renderer draws from a render state, not from the entity. The mod's poses, layers and effects
 * read the entity (its bending, its gestures), so every render state carries the entity it was made from (NeoForge's
 * render data), and {@link #entity} gives it back.
 *
 * <p>From 1.21.9 a renderer no longer draws: it submits to a {@code SubmitNodeCollector}, and 26.1 removed the
 * buffers it could still draw into. The mod's renderers, layers and effects written against 1.21.1 draw into a
 * {@link MultiBufferSource} that records them and submits each recording to the collector ({@link #buffers}).
 */
public final class RenderBridge {
    private static final ContextKey<Entity> ENTITY = new ContextKey<>(Enlightened.id("entity"));

    /**
     * The render state of the player the poses are being applied to now (set by PlayerModelMixin): what was the
     * model's own arm poses, crouch and swing in 1.21.1.
     */
    public static AvatarRenderState pose = new AvatarRenderState();

    /** The collector the world's effects are submitted to now (set while the level's custom geometry is submitted). */
    @Nullable
    private static SubmitNodeCollector current;

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

    /** A buffer source whose drawing is submitted to {@code collector}. */
    public static MultiBufferSource.BufferSource buffers(SubmitNodeCollector collector) {
        return new MultiBufferSource.BufferSource(collector);
    }

    /** The collector of the level's custom geometry while it is being submitted, or null. */
    @Nullable
    public static SubmitNodeCollector current() {
        return current;
    }

    /** Runs {@code draw} with {@code collector} as the {@link #current} one. */
    public static void with(SubmitNodeCollector collector, Runnable draw) {
        SubmitNodeCollector before = current;
        current = collector;
        try {
            draw.run();
        } finally {
            current = before;
        }
    }
}
