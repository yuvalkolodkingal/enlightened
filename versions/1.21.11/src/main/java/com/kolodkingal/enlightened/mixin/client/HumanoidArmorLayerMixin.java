package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.render.BrigadeLayer;
import com.kolodkingal.enlightened.compat.RenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No armour on a player who hid it (/armor hide), and no plain armour piece where a brigade uniform is drawn instead
 * (1.21.11: the layer submits, and each piece is given the render state).
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void enlightened$hidden(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yRot, float xRot,
                                    CallbackInfo ci) {
        if (RenderBridge.entity(state) instanceof Player p && com.kolodkingal.enlightened.bending.Benders.get(p).armorHidden()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void enlightened$brigade(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot, int light,
                                     HumanoidRenderState state, CallbackInfo ci) {
        if (RenderBridge.entity(state) instanceof Player && BrigadeLayer.brigade(stack) != null) {
            ci.cancel();
        }
    }
}
