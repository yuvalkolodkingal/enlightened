package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Chakras;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * A part of the Avatar State, drawn as the show draws it: the body outlined in a glow of energy, here in the colour of
 * the highest chakra he has opened (red for the first, violet for the seventh). Two shells of the player's own model,
 * a little larger than him, emissive and see-through, the outer fainter, flickering as energy does.
 */
public class ChakraAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation AURA = Enlightened.id("textures/entity/chakra_aura.png");

    public ChakraAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        BendingData data = Benders.get(player);
        long now = player.level().getGameTime();
        if (!Chakras.partial(data, now)) {
            return;
        }
        int rgb = Chakras.colour(Chakras.power(data, now));
        float r = (rgb >> 16 & 255) / 255.0F;
        float g = (rgb >> 8 & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        float flicker = 0.8F + 0.2F * Mth.sin(ageInTicks * 0.6F) * Mth.sin(ageInTicks * 0.23F + 1.0F);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(AURA));
        float[][] shells = {{1.06F, 0.55F}, {1.14F, 0.28F}};
        for (float[] shell : shells) {
            pose.pushPose();
            // Grown about the middle of the body, so the glow stands out round him on every side.
            pose.translate(0.0F, 0.75F, 0.0F);
            pose.scale(shell[0], shell[0], shell[0]);
            pose.translate(0.0F, -0.75F, 0.0F);
            int colour = FastColor.ARGB32.colorFromFloat(shell[1] * flicker, r, g, b);
            getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, colour);
            pose.popPose();
        }
    }
}
