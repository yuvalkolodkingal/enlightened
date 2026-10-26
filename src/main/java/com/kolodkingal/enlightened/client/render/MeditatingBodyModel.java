package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.MeditatingBodyEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The body an airbender left behind, sitting in the lotus: the legs crossed, the hands resting together at the lap,
 * the back straight, breathing slowly; the head nods a touch with each breath and settles.
 */
public class MeditatingBodyModel extends PlayerModel<MeditatingBodyEntity> {
    public MeditatingBodyModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(MeditatingBodyEntity body, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(body, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
        float breath = Mth.sin(ageInTicks * 0.08F);
        float rad = Mth.DEG_TO_RAD;
        // The settle into stillness over the first second after the body is left.
        float settle = Mth.clamp(ageInTicks / 20.0F, 0.0F, 1.0F);
        settle = settle * settle * (3.0F - 2.0F * settle);

        this.body.xRot = (-2.0F + 1.5F * breath) * rad * settle;
        this.body.y = 0.0F;
        this.head.xRot = (6.0F + 2.0F * breath) * rad * settle + headPitch * rad * (1.0F - settle);
        this.head.yRot = netHeadYaw * rad * (1.0F - settle) + 2.0F * rad * Mth.sin(ageInTicks * 0.03F) * settle;
        this.head.zRot = 0.0F;

        // The lotus: the thighs out and up, the shins crossed in front.
        float legX = -92.0F * rad;
        this.rightLeg.xRot = legX * settle;
        this.leftLeg.xRot = legX * settle;
        this.rightLeg.yRot = -28.0F * rad * settle;
        this.leftLeg.yRot = 28.0F * rad * settle;
        this.rightLeg.zRot = -12.0F * rad * settle;
        this.leftLeg.zRot = 12.0F * rad * settle;

        // The hands together at the lap, the elbows out a little; they rise and fall with the breath.
        float armX = (-34.0F + 2.0F * breath) * rad;
        this.rightArm.xRot = armX * settle;
        this.leftArm.xRot = armX * settle;
        this.rightArm.yRot = -48.0F * rad * settle;
        this.leftArm.yRot = 48.0F * rad * settle;
        this.rightArm.zRot = (10.0F - 1.0F * breath) * rad * settle;
        this.leftArm.zRot = (-10.0F + 1.0F * breath) * rad * settle;

        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }
}
