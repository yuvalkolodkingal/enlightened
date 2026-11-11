package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.anim.BenderPoses;
import com.kolodkingal.enlightened.compat.RenderBridge;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The benders' poses, blended on top of the vanilla animation (1.21.4: from the render state's entity). */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("HEAD"))
    private void enlightened$reset(PlayerRenderState state, CallbackInfo ci) {
        RenderBridge.pose = state;
        BenderPoses.reset((PlayerModel) (Object) this);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("TAIL"))
    private void enlightened$poses(PlayerRenderState state, CallbackInfo ci) {
        RenderBridge.pose = state;
        if (RenderBridge.entity(state) instanceof LivingEntity entity) {
            BenderPoses.apply((PlayerModel) (Object) this, entity, state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks);
        }
    }
}
