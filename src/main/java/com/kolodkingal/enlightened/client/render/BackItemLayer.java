package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.item.BackSlot;
import com.kolodkingal.enlightened.item.GliderItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws what a player wears in the back slot on his back, over his armour: slung across it from the right shoulder to
 * the left hip, as a sword or a staff is carried; a block sits flat against it. The glider is drawn as its staff by
 * {@code WorldFxRenderer} instead, and nothing is drawn while it is open.
 */
public class BackItemLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BackItemLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = BackSlot.get(player);
        if (stack.isEmpty() || stack.getItem() instanceof GliderItem || player.isInvisible() || Benders.get(player).gliderOpen()) {
            return;
        }
        boolean armour = !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Behind the back (over the chestplate if he wears one), halfway down it.
        pose.translate(0.0F, 0.38F, armour ? 0.22F : 0.17F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        if (stack.getItem() instanceof BlockItem) {
            pose.scale(0.5F, 0.5F, 0.5F);
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(-45.0F));
            pose.scale(0.9F, 0.9F, 0.9F);
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(player, stack, ItemDisplayContext.FIXED, false, pose, buffer, player.level(),
                light, OverlayTexture.NO_OVERLAY, player.getId());
        pose.popPose();
    }
}
