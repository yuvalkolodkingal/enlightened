package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.jetbrains.annotations.Nullable;

/**
 * Armour with the Seven Havens brigade trim, worn as the brigade's uniform: drawn on the skin itself, a whisker over it
 * (no bulk of armour), from the brigade's own design ({@code entity/brigade/<brigade>.png}, in the skin's layout): the
 * helmet the head, the chestplate the body and arms, the leggings or boots the legs. The trim's material picks the
 * brigade: lapis water, redstone fire, gold (or quartz) air, coal earth, netherite the Avatar's.
 */
public class BrigadeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(Enlightened.id("brigade"), "wide");
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(Enlightened.id("brigade"), "slim");

    private final PlayerModel<AbstractClientPlayer> uniform;

    public BrigadeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root, boolean slim) {
        super(parent);
        uniform = new PlayerModel<>(root, slim);
    }

    /** The brigade a piece of armour is the uniform of (water, fire, air, earth, avatar), or null. */
    @Nullable
    public static String brigade(ItemStack stack) {
        ArmorTrim trim = stack.isEmpty() ? null : stack.get(DataComponents.TRIM);
        if (trim == null || !trim.pattern().is(ModItems.BRIGADE_TRIM)) {
            return null;
        }
        String m = trim.material().unwrapKey().map(k -> k.location().getPath()).orElse("");
        return switch (m) {
            case "lapis", "amethyst", "diamond" -> "water";
            case "redstone", "copper" -> "fire";
            case "gold", "quartz" -> "air";
            case "netherite" -> "avatar";
            default -> "earth";
        };
    }

    private static ResourceLocation texture(String brigade) {
        return Enlightened.id("textures/entity/brigade/" + brigade + ".png");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || com.kolodkingal.enlightened.bending.Benders.get(player).armorHidden()) {
            return;
        }
        String head = brigade(player.getItemBySlot(EquipmentSlot.HEAD));
        String chest = brigade(player.getItemBySlot(EquipmentSlot.CHEST));
        String legs = brigade(player.getItemBySlot(EquipmentSlot.LEGS));
        String feet = brigade(player.getItemBySlot(EquipmentSlot.FEET));
        if (head == null && chest == null && legs == null && feet == null) {
            return;
        }
        getParentModel().copyPropertiesTo(uniform);
        uniform.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        // Each worn piece draws its part of its own brigade's uniform.
        part(pose, buffers, light, overlay, head, Part.HEAD);
        part(pose, buffers, light, overlay, chest, Part.BODY);
        part(pose, buffers, light, overlay, legs != null ? legs : feet, Part.LEGS);
    }

    private enum Part { HEAD, BODY, LEGS }

    private void part(PoseStack pose, MultiBufferSource buffers, int light, int overlay, @Nullable String brigade, Part part) {
        if (brigade == null) {
            return;
        }
        uniform.setAllVisible(false);
        switch (part) {
            case HEAD -> {
                uniform.head.visible = true;
                uniform.hat.visible = true;
            }
            case BODY -> {
                uniform.body.visible = uniform.jacket.visible = true;
                uniform.leftArm.visible = uniform.rightArm.visible = true;
                uniform.leftSleeve.visible = uniform.rightSleeve.visible = true;
            }
            case LEGS -> {
                uniform.leftLeg.visible = uniform.rightLeg.visible = true;
                uniform.leftPants.visible = uniform.rightPants.visible = true;
            }
        }
        uniform.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(texture(brigade))), light, overlay);
    }
}
