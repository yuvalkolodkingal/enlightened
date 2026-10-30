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
    /** How the item's icon is turned on the back, so a sword's hilt sits over the right shoulder and its point by the left hip. */
    static final float ROLL = 0.0F;
    private static final net.minecraft.resources.ResourceLocation WHITE = net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png");

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
        float out = armour ? 0.19F : 0.13F;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        strap(pose, buffer, light, out + 0.005F);
        // Flat against the back (over the chestplate if he wears one), slung across it: the hilt over the right shoulder.
        pose.translate(0.0F, 0.36F, out + 0.04F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        if (stack.getItem() instanceof BlockItem) {
            pose.scale(0.5F, 0.5F, 0.5F);
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(ROLL));
            pose.scale(1.25F, 1.25F, 1.25F);
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(player, stack, ItemDisplayContext.FIXED, false, pose, buffer, player.level(),
                light, OverlayTexture.NO_OVERLAY, player.getId());
        pose.popPose();
    }

    /**
     * The leather strap that holds it: across the back from the right shoulder to the left hip and the same way across
     * the chest, two pixels wide, just over the body (or the chestplate).
     */
    private static void strap(PoseStack pose, MultiBufferSource buffer, int light, float out) {
        var vc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(WHITE));
        var m = pose.last();
        float w = 1.6F / 16.0F;
        for (float z : new float[]{out, -out}) {
            // From the top of the right shoulder (-x) to the bottom of the left side (+x); y runs down the body.
            float x0 = -4.0F / 16.0F;
            float x1 = 4.0F / 16.0F;
            float y0 = 0.0F;
            float y1 = 12.0F / 16.0F;
            float[][] q = {{x0, y0}, {x0 + w * 1.4F, y0}, {x1, y1 - w * 1.4F}, {x1, y1}};
            for (float[] v : z > 0 ? q : new float[][]{q[3], q[2], q[1], q[0]}) {
                vc.addVertex(m, v[0], v[1], z).setColor(0xFF5A3A22).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                        .setNormal(m, 0.0F, 0.0F, z > 0 ? 1.0F : -1.0F);
            }
        }
    }
}
