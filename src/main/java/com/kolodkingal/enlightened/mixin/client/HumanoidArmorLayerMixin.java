package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.render.BrigadeLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armour a player chose to hide is not drawn; brigade armour is not drawn as armour at all, but as the brigade's
 * uniform on the skin ({@link BrigadeLayer}). Client only.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void enlightened$skip(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<?> model,
                                  float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player p && com.kolodkingal.enlightened.bending.Benders.get(p).armorHidden()
                || entity instanceof Player && BrigadeLayer.brigade(entity.getItemBySlot(slot)) != null) {
            ci.cancel();
        }
    }
}
