package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.compat.RenderBridge;
import com.kolodkingal.enlightened.item.GliderItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An open glider is drawn spread over the airbender, not held in his hand (1.21.4). */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void enlightened$openGlider(ArmedEntityRenderState state, ItemStackRenderState item, HumanoidArm arm, PoseStack poseStack,
                                        MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (RenderBridge.entity(state) instanceof Player p && p.getItemHeldByArm(arm).getItem() instanceof GliderItem
                && Benders.get(p).gliderOpen()) {
            ci.cancel();
        }
    }
}
