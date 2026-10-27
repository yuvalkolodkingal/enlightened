package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
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
 * The Avatar State as the show draws it on the body: the eyes glow solid white, and an air nomad's arrow tattoos
 * (head, arms, hands, legs) glow blue-white with them. Drawn as emissive overlays on the player's own model, pulsing
 * softly; Zaheer, who has no tattoos, glows at the eyes only.
 */
public class AvatarGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation EYES = Enlightened.id("textures/entity/avatar_eyes.png");
    private static final ResourceLocation ARROWS = Enlightened.id("textures/entity/avatar_arrows.png");

    public AvatarGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        BendingData data = Benders.get(player);
        if (!data.element().bends() || !data.avatar(player.level().getGameTime())) {
            return;
        }
        float pulse = 0.85F + 0.15F * Mth.sin(ageInTicks * 0.35F);
        int white = FastColor.ARGB32.colorFromFloat(1.0F, pulse, pulse, 1.0F);
        VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(EYES));
        getParentModel().renderToBuffer(poseStack, eyes, 0xF000F0, OverlayTexture.NO_OVERLAY, white);
        boolean arrows = data.element() == Element.AIR && !"zaheer".equals(data.variant()) || data.element() == Element.AVATAR && "aang".equals(data.variant());
        if (arrows) {
            int blue = FastColor.ARGB32.colorFromFloat(1.0F, 0.75F * pulse, 0.9F * pulse, 1.0F);
            VertexConsumer tattoos = buffer.getBuffer(RenderType.eyes(ARROWS));
            getParentModel().renderToBuffer(poseStack, tattoos, 0xF000F0, OverlayTexture.NO_OVERLAY, blue);
        }
    }
}
