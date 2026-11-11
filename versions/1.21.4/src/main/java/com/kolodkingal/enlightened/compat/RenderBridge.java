package com.kolodkingal.enlightened.compat;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.4 only: from 1.21.2 a renderer draws from a render state, not from the entity. The mod's poses, layers and
 * effects read the entity (its bending, its gestures), so every render state carries the entity it was made from
 * (NeoForge's render data), and {@link #entity} gives it back.
 */
public final class RenderBridge {
    private static final ContextKey<Entity> ENTITY = new ContextKey<>(Enlightened.id("entity"));

    /**
     * The render state of the player the poses are being applied to now (set by PlayerModelMixin): what was the
     * model's own arm poses, crouch and swing in 1.21.1.
     */
    public static net.minecraft.client.renderer.entity.state.PlayerRenderState pose = new net.minecraft.client.renderer.entity.state.PlayerRenderState();

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
}
