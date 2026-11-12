package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.compat.LegacyHumanoidRenderer;
import com.kolodkingal.enlightened.compat.LegacyLivingRenderer;
import com.kolodkingal.enlightened.compat.LegacyMobRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 1.21.11: the dispatcher gives every avatar render state to the player's renderer (by its skin's model). The mod's
 * creatures drawn on the player model (the renderers written against 1.21.1, which draw an avatar render state) are
 * given back to their own renderer, found by the state's entity type as for any other entity.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherAvatarMixin {
    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
            at = @At("HEAD"), cancellable = true)
    private void enlightened$ownRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        if (state instanceof AvatarRenderState && state.entityType != null) {
            EntityRenderer<?, ?> own = renderers.get(state.entityType);
            if (own instanceof LegacyLivingRenderer<?, ?> || own instanceof LegacyMobRenderer<?, ?> || own instanceof LegacyHumanoidRenderer<?, ?>) {
                cir.setReturnValue(own);
            }
        }
    }
}
