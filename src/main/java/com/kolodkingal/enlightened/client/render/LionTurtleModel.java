package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The lion turtle's head and front flippers, as in the show: a lion's face, pale and broad, with a great mane of
 * green weed round it and a beard under the chin, small ears, a wide muzzle; a thick neck running back under the
 * shell; flippers either side that paddle slowly. Drawn at a quarter size and scaled up four times by
 * {@link LionTurtleRenderer}. Texture 256x128 ({@code textures/entity/lion_turtle.png}, drawn by tools/skins.py).
 */
public class LionTurtleModel extends HierarchicalModel<LionTurtleEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;

    public LionTurtleModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftFlipper = root.getChild("left_flipper");
        this.rightFlipper = root.getChild("right_flipper");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 72).addBox(-12.0F, -4.0F, -2.0F, 24.0F, 24.0F, 30.0F), PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.0F, -16.0F, -24.0F, 32.0F, 28.0F, 24.0F)
                .texOffs(112, 0).addBox(-24.0F, -24.0F, -6.0F, 48.0F, 44.0F, 8.0F)
                .texOffs(0, 52).addBox(-10.0F, 0.0F, -30.0F, 20.0F, 10.0F, 6.0F)
                .texOffs(52, 52).addBox(-4.0F, -2.0F, -31.0F, 8.0F, 4.0F, 2.0F)
                .texOffs(72, 52).addBox(-12.0F, 10.0F, -26.0F, 24.0F, 10.0F, 10.0F)
                .texOffs(140, 52).addBox(-20.0F, -22.0F, -12.0F, 6.0F, 6.0F, 4.0F)
                .texOffs(140, 52).addBox(14.0F, -22.0F, -12.0F, 6.0F, 6.0F, 4.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("left_flipper", CubeListBuilder.create()
                .texOffs(108, 72).addBox(0.0F, -2.0F, -11.0F, 40.0F, 4.0F, 22.0F), PartPose.offset(14.0F, 18.0F, 12.0F));
        root.addOrReplaceChild("right_flipper", CubeListBuilder.create()
                .texOffs(108, 72).mirror().addBox(-40.0F, -2.0F, -11.0F, 40.0F, 4.0F, 22.0F), PartPose.offset(-14.0F, 18.0F, 12.0F));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(LionTurtleEntity turtle, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // The head turns a little toward who looks at it; it is too great to turn far.
        head.yRot = Mth.clamp(netHeadYaw, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = Mth.clamp(headPitch, -20.0F, 20.0F) * Mth.DEG_TO_RAD * 0.4F + Mth.sin(ageInTicks * 0.02F) * 0.03F;
        // The flippers paddle slowly, as a turtle swims.
        float stroke = Mth.sin(ageInTicks * 0.04F);
        leftFlipper.zRot = 0.15F + stroke * 0.18F;
        leftFlipper.yRot = -0.35F + stroke * 0.2F;
        rightFlipper.zRot = -0.15F - stroke * 0.18F;
        rightFlipper.yRot = 0.35F - stroke * 0.2F;
    }
}
