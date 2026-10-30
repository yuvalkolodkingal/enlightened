package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.CityGuardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A city guard on the player model in the uniform of his city (entity/guard/&lt;city&gt;_&lt;face&gt;.png), with his
 * city's helmet over it: the round brimmed cap of Omashu, the tiered cone of Ba Sing Se, the crested golden helm of
 * Ellora.
 */
public class CityGuardRenderer extends HumanoidMobRenderer<CityGuardEntity, PlayerModel<CityGuardEntity>> {
    public static final ModelLayerLocation HELMET = new ModelLayerLocation(Enlightened.id("city_guard"), "helmet");
    private static final ResourceLocation[][] SKINS = new ResourceLocation[CityGuardEntity.City.values().length][CityGuardEntity.VOICES];
    private static final ResourceLocation[] HELMETS = new ResourceLocation[CityGuardEntity.City.values().length];

    static {
        for (CityGuardEntity.City city : CityGuardEntity.City.values()) {
            for (int v = 0; v < CityGuardEntity.VOICES; v++) {
                SKINS[city.ordinal()][v] = Enlightened.id("textures/entity/guard/" + city.id + "_" + v + ".png");
            }
            HELMETS[city.ordinal()] = Enlightened.id("textures/entity/guard/" + city.id + "_helmet.png");
        }
    }

    public CityGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new Helmet(this, context.bakeLayer(HELMET)));
    }

    @Override
    public ResourceLocation getTextureLocation(CityGuardEntity entity) {
        return SKINS[entity.city().ordinal()][entity.voice()];
    }

    /** The three helmets, in head space (pixels, y down; the head spans -8..0). */
    public static LayerDefinition createHelmetLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("omashu", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -9.5F, -4.5F, 9, 3, 9)
                .texOffs(0, 12).addBox(-6.0F, -7.0F, -6.0F, 12, 1, 12)
                .texOffs(36, 0).addBox(-1.0F, -10.5F, -1.0F, 2, 1, 2), PartPose.ZERO);
        root.addOrReplaceChild("ba_sing_se", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-7.0F, -8.5F, -7.0F, 14, 1, 14)
                .texOffs(0, 0).addBox(-5.0F, -9.5F, -5.0F, 10, 1, 10)
                .texOffs(0, 0).addBox(-3.0F, -10.5F, -3.0F, 6, 1, 6)
                .texOffs(36, 0).addBox(-1.0F, -11.5F, -1.0F, 2, 1, 2), PartPose.ZERO);
        root.addOrReplaceChild("ellora", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -9.0F, -4.5F, 9, 4, 9)
                .texOffs(36, 0).addBox(-0.5F, -12.0F, -3.5F, 1, 3, 7), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    static final class Helmet extends RenderLayer<CityGuardEntity, PlayerModel<CityGuardEntity>> {
        private final ModelPart[] parts;

        Helmet(RenderLayerParent<CityGuardEntity, PlayerModel<CityGuardEntity>> parent, ModelPart root) {
            super(parent);
            CityGuardEntity.City[] cities = CityGuardEntity.City.values();
            parts = new ModelPart[cities.length];
            for (CityGuardEntity.City c : cities) {
                parts[c.ordinal()] = root.getChild(c.id);
            }
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CityGuardEntity guard, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (guard.isInvisible()) {
                return;
            }
            int c = guard.city().ordinal();
            pose.pushPose();
            getParentModel().getHead().translateAndRotate(pose);
            parts[c].render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HELMETS[c])), light,
                    LivingEntityRenderer.getOverlayCoords(guard, 0.0F));
            pose.popPose();
        }
    }
}
