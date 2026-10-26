package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.earth.EarthBending;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The earth armour and the metal armour: a shell of rock or iron drawn over the whole body on a model inflated past
 * the skin's outer layer, with the eyes left open (the shell textures are transparent at the eyes).
 */
public class ShellLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation EARTH = Enlightened.id("textures/entity/earth_armor.png");
    private static final ResourceLocation METAL = Enlightened.id("textures/entity/metal_armor.png");

    private final PlayerModel<AbstractClientPlayer> shell;

    public ShellLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root, boolean slim) {
        super(parent);
        this.shell = new PlayerModel<>(root, slim);
        shell.hat.visible = false;
        shell.jacket.visible = false;
        shell.leftSleeve.visible = false;
        shell.rightSleeve.visible = false;
        shell.leftPants.visible = false;
        shell.rightPants.visible = false;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        BendingData data = Benders.get(player);
        if (!EarthBending.armored(data)) {
            return;
        }
        boolean metal = data.has(Ability.METAL_ARMOR) && data.on(Ability.METAL_ARMOR);
        getParentModel().copyPropertiesTo(shell);
        shell.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(metal ? METAL : EARTH));
        shell.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
