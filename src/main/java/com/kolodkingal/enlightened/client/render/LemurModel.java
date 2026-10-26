package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.FlyingLemurEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The flying lemur: a small body, a round head with big ears that twitch and flatten in the wind, two wide wings that
 * beat in a figure of eight in flight, spread flat in a glide and fold on the ground, a long striped tail that
 * swings with a lag, and four little legs that scamper on the ground and tuck up in the air or in a perch.
 *
 * <p>Nothing here switches: the ground, flight, glide and perch poses are blended by the eased weights the entity
 * keeps ({@link FlyingLemurEntity#flyWeight} and friends), so a landing or a take-off is one continuous motion.
 * Texture 64x32 ({@code textures/entity/lemur.png}, drawn by tools/skins.py).
 */
public class LemurModel extends HierarchicalModel<FlyingLemurEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public LemurModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.tail = body.getChild("tail");
        this.frontLeftLeg = body.getChild("front_left_leg");
        this.frontRightLeg = body.getChild("front_right_leg");
        this.backLeftLeg = body.getChild("back_left_leg");
        this.backRightLeg = body.getChild("back_right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 4.0F, 8.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 13).addBox(-2.0F, -3.0F, -4.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(0.0F, -1.0F, -3.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(16, 13).addBox(0.0F, -3.0F, 0.0F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(1.0F, -2.5F, -1.5F, 0.0F, 0.0F, 0.35F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(24, 13).addBox(-3.0F, -3.0F, 0.0F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, -2.5F, -1.5F, 0.0F, 0.0F, -0.35F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 22).addBox(0.0F, 0.0F, -3.0F, 8.0F, 1.0F, 6.0F), PartPose.offset(3.0F, -1.5F, 0.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(28, 22).addBox(-8.0F, 0.0F, -3.0F, 8.0F, 1.0F, 6.0F), PartPose.offset(-3.0F, -1.5F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(34, 13).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 4.0F, 0.4F, 0.0F, 0.0F));
        body.addOrReplaceChild("front_left_leg", CubeListBuilder.create()
                .texOffs(48, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(2.0F, 2.0F, -2.5F));
        body.addOrReplaceChild("front_right_leg", CubeListBuilder.create()
                .texOffs(52, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(-2.0F, 2.0F, -2.5F));
        body.addOrReplaceChild("back_left_leg", CubeListBuilder.create()
                .texOffs(56, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(2.0F, 2.0F, 2.5F));
        body.addOrReplaceChild("back_right_leg", CubeListBuilder.create()
                .texOffs(60, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(-2.0F, 2.0F, 2.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FlyingLemurEntity lemur, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = ageInTicks - lemur.tickCount;
        float flap = Mth.lerp(partial, lemur.oFlap, lemur.flap);
        float fly = Mth.lerp(partial, lemur.oFlyWeight, lemur.flyWeight);
        float sit = Mth.lerp(partial, lemur.oSitWeight, lemur.sitWeight);
        float glide = Mth.lerp(partial, lemur.oGlideWeight, lemur.glideWeight);
        float tilt = Mth.lerp(partial, lemur.oHeadTilt, lemur.headTilt);
        float flapSpeed = lemur.flapSpeed;
        float ground = Math.max(0.0F, 1.0F - fly - sit);
        float rad = Mth.DEG_TO_RAD;

        // ---- the head: looks where it looks, dips in flight, tilts now and then on the ground, bobs with each step ----
        head.yRot = netHeadYaw * rad;
        head.xRot = headPitch * rad - 0.3F * fly + 0.15F * sit;
        head.zRot = tilt * (ground + sit);
        head.y = -1.0F + 0.4F * Mth.sin(limbSwing * 1.2F) * limbSwingAmount * ground;
        // The ears twitch, and flatten back in the wind of a fast flight.
        float twitch = lemur.earTwitch * Mth.sin(ageInTicks * 1.2F) * 0.3F;
        float flatten = 0.5F * fly * flapSpeed;
        leftEar.zRot = 0.35F + twitch + 0.1F * sit;
        rightEar.zRot = -0.35F - twitch - 0.1F * sit;
        leftEar.xRot = flatten;
        rightEar.xRot = flatten;

        // ---- the body: level on the ground, nosed down in flight, curled in a perch; a breath while it rests ----
        float breath = 0.03F * Mth.sin(ageInTicks * 0.15F);
        body.xRot = 0.15F * fly - 0.12F * sit + 0.05F * glide;
        body.y = 19.0F + 2.0F * sit - 1.0F * fly + 0.5F * Mth.sin(ageInTicks * 0.5F) * fly * (1.0F - glide) + breath * (ground + sit) * 4.0F;
        body.zRot = 0.0F;

        // ---- the wings: a figure of eight beat in flight, spread flat in a glide, folded on the ground and in a perch ----
        float beat = Mth.cos(flap * 0.6F) * 0.9F * flapSpeed * (1.0F - 0.8F * glide);
        float sweep = Mth.sin(flap * 0.6F) * 0.25F * flapSpeed * (1.0F - 0.8F * glide);
        float flyZ = -0.15F - beat;
        float foldedZ = 1.15F + 0.05F * Mth.sin(ageInTicks * 0.1F);
        float sitZ = 1.25F;
        leftWing.zRot = flyZ * fly + foldedZ * ground + sitZ * sit;
        rightWing.zRot = -(flyZ * fly + foldedZ * ground + sitZ * sit);
        leftWing.xRot = sweep * fly;
        rightWing.xRot = sweep * fly;
        leftWing.yRot = -0.15F * glide;
        rightWing.yRot = 0.15F * glide;

        // ---- the tail: trails and sways in flight, curls round in a perch, swings with a lag on the ground ----
        float lag = Mth.sin(ageInTicks * 0.3F - 0.8F);
        tail.xRot = (0.15F + 0.15F * lag) * fly + 1.0F * sit + (0.5F + 0.2F * Mth.sin(ageInTicks * 0.15F)) * ground;
        tail.yRot = 0.4F * Mth.sin(ageInTicks * 0.1F) * sit + 0.2F * Mth.cos(ageInTicks * 0.12F) * ground + 0.1F * lag * fly;

        // ---- the legs: a scamper on the ground with a lift of the knee, tucked up in the air and in a perch ----
        float walk = Mth.cos(limbSwing * 1.2F) * 0.9F * limbSwingAmount;
        float lift = Math.max(0.0F, Mth.sin(limbSwing * 1.2F)) * 0.4F * limbSwingAmount;
        float lift2 = Math.max(0.0F, -Mth.sin(limbSwing * 1.2F)) * 0.4F * limbSwingAmount;
        float tuck = 1.3F * fly + 1.4F * sit;
        frontLeftLeg.xRot = (walk - lift) * ground + tuck;
        frontRightLeg.xRot = (-walk - lift2) * ground + tuck;
        backLeftLeg.xRot = (-walk - lift2) * ground - tuck;
        backRightLeg.xRot = (walk - lift) * ground - tuck;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (young) {
            poseStack.pushPose();
            poseStack.scale(0.6F, 0.6F, 0.6F);
            poseStack.translate(0.0F, 1.0F, 0.0F);
            root.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        } else {
            root.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
    }
}
