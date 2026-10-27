package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.SkyBisonEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The sky bison, after Appa: a great white body with a brown arrow down its back, a broad head with a fringe of fur,
 * two short horns and a wide dark nose, six short legs, and a flat paddle of a tail. On the ground the legs walk in
 * threes; in the air they tuck back and the tail beats slowly up and down, the body pitched where it flies. The
 * saddle (a leather pad with a wooden basket rim) shows when it is saddled. Texture 256x256
 * ({@code textures/entity/sky_bison.png}, drawn by tools/skins.py).
 */
public class SkyBisonModel extends HierarchicalModel<SkyBisonEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart saddle;
    private final ModelPart[] legs = new ModelPart[6];

    public SkyBisonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.tail = body.getChild("tail");
        this.saddle = body.getChild("saddle");
        for (int i = 0; i < 6; i++) {
            legs[i] = body.getChild("leg" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.0F, -26.0F, -28.0F, 40.0F, 26.0F, 56.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 82).addBox(-13.0F, -12.0F, -16.0F, 26.0F, 20.0F, 16.0F)
                .texOffs(84, 82).addBox(-12.0F, -15.0F, -17.0F, 24.0F, 5.0F, 6.0F)
                .texOffs(84, 93).addBox(-6.0F, 1.0F, -19.0F, 12.0F, 6.0F, 3.0F), PartPose.offset(0.0F, -10.0F, -27.0F));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(144, 82).addBox(0.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(160, 82).addBox(2.5F, -6.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(12.0F, -10.0F, -7.0F, 0.0F, 0.0F, 0.3F));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().mirror()
                .texOffs(144, 82).addBox(-4.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(160, 82).addBox(-5.5F, -6.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(-12.0F, -10.0F, -7.0F, 0.0F, 0.0F, -0.3F));
        float[][] at = {{14, -18}, {-14, -18}, {14, 0}, {-14, 0}, {14, 18}, {-14, 18}};
        for (int i = 0; i < 6; i++) {
            body.addOrReplaceChild("leg" + i, CubeListBuilder.create()
                    .texOffs(192, 0).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 12.0F, 10.0F), PartPose.offset(at[i][0], 0.0F, at[i][1]));
        }
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(114, 93).addBox(-14.0F, -2.0F, 0.0F, 28.0F, 4.0F, 22.0F), PartPose.offset(0.0F, -10.0F, 27.0F));
        body.addOrReplaceChild("saddle", CubeListBuilder.create()
                .texOffs(0, 128).addBox(-16.0F, -4.0F, -18.0F, 32.0F, 4.0F, 28.0F)
                .texOffs(120, 128).addBox(-16.0F, -9.0F, -18.0F, 32.0F, 5.0F, 2.0F)
                .texOffs(120, 128).addBox(-16.0F, -9.0F, 8.0F, 32.0F, 5.0F, 2.0F)
                .texOffs(120, 136).addBox(14.0F, -9.0F, -16.0F, 2.0F, 5.0F, 24.0F)
                .texOffs(120, 136).addBox(-16.0F, -9.0F, -16.0F, 2.0F, 5.0F, 24.0F), PartPose.offset(0.0F, -26.0F, 0.0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SkyBisonEntity bison, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float rad = Mth.DEG_TO_RAD;
        boolean flying = bison.airborne();
        float fly = flying ? 1.0F : 0.0F;
        saddle.visible = bison.isSaddled();
        // The body pitches where it flies, and rises and falls with each beat of the tail.
        float beat = Mth.sin(ageInTicks * 0.12F);
        body.xRot = fly * Mth.clamp(bison.getXRot(), -30.0F, 30.0F) * rad;
        body.y = 12.0F + fly * beat * 0.8F + (1 - fly) * 0.3F * Mth.sin(ageInTicks * 0.05F);
        head.yRot = netHeadYaw * rad * 0.5F;
        head.xRot = headPitch * rad * 0.5F - fly * 0.1F;
        // The tail: a slow paddle in the air, a lazy sway on the ground.
        tail.xRot = fly * (0.15F + 0.35F * beat) + (1 - fly) * (0.35F + 0.05F * Mth.sin(ageInTicks * 0.08F));
        tail.yRot = (1 - fly) * 0.15F * Mth.sin(ageInTicks * 0.04F);
        // The legs: in threes on the ground (each side's front and back with the other side's middle); tucked back in flight.
        float walk = Mth.cos(limbSwing * 0.5F) * 0.6F * limbSwingAmount;
        for (int i = 0; i < 6; i++) {
            int row = i / 2;
            boolean left = i % 2 == 0;
            float phase = (row == 1) == left ? walk : -walk;
            float tuck = row == 0 ? 0.9F : row == 1 ? 1.1F : 1.3F;
            legs[i].xRot = (1 - fly) * phase + fly * (tuck + 0.08F * Mth.sin(ageInTicks * 0.12F + i));
        }
    }
}
