package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.SeismicGlow;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Outlines the creatures the local earthbender's seismic sense feels, as the glowing effect would. Client only. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void enlightened$seismic(CallbackInfoReturnable<Boolean> cir) {
        if (SeismicGlow.glows((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
