package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.DarkSpiritEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A dark spirit: a purple-black hunched body and head with burning yellow eyes (an emissive layer); the crawler on four
 * long jointless legs that splay like a spider's, the flyer with two long arms hanging and a tail, swaying.
 */
public class DarkSpiritRenderer extends MobRenderer<DarkSpiritEntity, DarkSpiritRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enlightened.id("dark_spirit"), "main");
    private static final ResourceLocation TEXTURE = Enlightened.id("textures/entity/dark_spirit.png");
    private static final RenderType EYES = RenderType.eyes(Enlightened.id("textures/entity/dark_spirit_eyes.png"));

    public DarkSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(LAYER)), 0.6F);
        //#if >=1.21.4
        //$$ addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<>(this) {
        //$$     @Override
        //$$     public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
        //$$                        net.minecraft.client.renderer.entity.state.PlayerRenderState state,
        //$$                        float yRot, float xRot) {
        //$$         getParentModel().renderToBuffer(pose, buffers.getBuffer(EYES), 0xF00000, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        //$$     }
        //$$ });
        //#else
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
        //#endif
    }

    @Override
    public ResourceLocation getTextureLocation(DarkSpiritEntity entity) {
        return TEXTURE;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, -5, 8, 6, 10),
                PartPose.offset(0, 10, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-3.5F, -3, -6, 7, 6, 6), PartPose.offset(0, -1, -5));
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? -3.5F : 3.5F;
            float z = i < 2 ? -3.5F : 3.5F;
            body.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(36, 0).addBox(-1, 0, -1, 2, 16, 2), PartPose.offset(x, 2, z));
        }
        body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(44, 0).addBox(-1, 0, -1, 2, 18, 2), PartPose.offset(4.5F, -1, -3));
        body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-1, 0, -1, 2, 18, 2), PartPose.offset(-4.5F, -1, -3));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(26, 16).addBox(-1, -1, 0, 2, 2, 14), PartPose.offset(0, 0, 5));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static final class Model extends HierarchicalModel<DarkSpiritEntity> {
        private final ModelPart root;
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart[] legs = new ModelPart[4];
        private final ModelPart armL;
        private final ModelPart armR;
        private final ModelPart tail;

        Model(ModelPart root) {
            //#if >=1.21.4
            //$$ super(root);
            //#endif
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            for (int i = 0; i < 4; i++) {
                legs[i] = body.getChild("leg" + i);
            }
            armL = body.getChild("arm_l");
            armR = body.getChild("arm_r");
            tail = body.getChild("tail");
        }

        //#if <1.21.4
        @Override
        public ModelPart root() {
            return root;
        }
        //#endif

        @Override
        public void setupAnim(DarkSpiritEntity s, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
            boolean flyer = s.shape() == DarkSpiritEntity.Shape.FLYER;
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
            head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F;
            body.y = flyer ? 6 + Mth.sin(age * 0.15F) : 10;
            for (int i = 0; i < 4; i++) {
                legs[i].visible = !flyer;
                float side = i % 2 == 0 ? 1 : -1;
                float phase = (i == 0 || i == 3) ? 0 : Mth.PI;
                legs[i].zRot = side * 0.9F;
                legs[i].xRot = (i < 2 ? -0.5F : 0.5F) + Mth.cos(limbSwing * 0.9F + phase) * 0.6F * limbSwingAmount;
            }
            armL.visible = armR.visible = tail.visible = flyer;
            armL.xRot = 0.3F + Mth.sin(age * 0.12F) * 0.25F;
            armR.xRot = 0.3F + Mth.sin(age * 0.12F + 1.5F) * 0.25F;
            armL.zRot = -0.25F;
            armR.zRot = 0.25F;
            tail.yRot = Mth.sin(age * 0.1F) * 0.4F;
            tail.xRot = -0.3F + Mth.cos(age * 0.08F) * 0.2F;
        }
    }
}
