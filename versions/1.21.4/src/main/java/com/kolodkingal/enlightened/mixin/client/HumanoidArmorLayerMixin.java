package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.render.BrigadeLayer;
import com.kolodkingal.enlightened.compat.RenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No armour on a player who hid it (/armor hide), and no plain armour piece where a brigade uniform is drawn instead
 * (1.21.4: the piece no longer knows its wearer, so the layer remembers whom it is drawing).
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Unique
    private Player enlightened$player;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void enlightened$hidden(PoseStack poseStack, MultiBufferSource buffer, int light, HumanoidRenderState state, float yRot, float xRot,
                                    CallbackInfo ci) {
        enlightened$player = RenderBridge.entity(state) instanceof Player p ? p : null;
        if (enlightened$player != null && com.kolodkingal.enlightened.bending.Benders.get(enlightened$player).armorHidden()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void enlightened$brigade(PoseStack poseStack, MultiBufferSource buffer, ItemStack stack, EquipmentSlot slot, int light,
                                     net.minecraft.client.model.HumanoidModel<?> model, CallbackInfo ci) {
        if (enlightened$player != null && BrigadeLayer.brigade(stack) != null) {
            ci.cancel();
        }
    }
}
