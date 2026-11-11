package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.anim.BodyLean;
import com.kolodkingal.enlightened.compat.RenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Benders lean into their flight, their glide and their run (1.21.4: from the render state's player). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("TAIL"))
    private void enlightened$lean(PlayerRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        if (RenderBridge.entity(state) instanceof AbstractClientPlayer player) {
            BodyLean.apply(player, poseStack, bodyRot, state.partialTick);
        }
    }
}
