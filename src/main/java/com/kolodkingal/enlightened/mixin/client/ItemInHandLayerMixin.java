package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.item.GliderItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The glider staff is not drawn in the hand while it is open: the open glider is drawn whole over the bender. Client only. */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void enlightened$openGlider(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poseStack,
                                        MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (stack.getItem() instanceof GliderItem && entity instanceof Player p && Benders.get(p).gliderOpen()) {
            ci.cancel();
        }
    }
}
