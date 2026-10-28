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
 * The lion turtle's head, after the show: a great skull, pale grey-tan, a broad snout with a dark nose pad, heavy brows
 * over big amber eyes, a wide jaw; round the face a crown of long spined fins sweeping back, a beard of shorter ones
 * under the jaw, two long whiskers; a thick grey-green neck running back under the shell; a flipper either side that
 * paddles slowly, and, while it gives energybending, rises beside its face to reach out its claws.
 *
 * <p>Drawn in units of a tenth of the island's scale ({@link LionTurtleRenderer} scales it 1.6 × the scale): at the
 * largest a pixel is a block, the skull 48 wide. Texture 512 × 512 ({@code textures/entity/lion_turtle.png}, by
 * tools/skins.py).
 */
public class LionTurtleModel extends HierarchicalModel<LionTurtleEntity> {
    /** The fins of the crown: angle round the face (degrees, 0 = right, 90 = down), length, and how far they splay. */
    static final float[][] CROWN = {
            {-90, 50, 1.4F}, {-68, 48, 1.5F}, {-112, 48, 1.5F}, {-46, 46, 1.6F}, {-134, 46, 1.6F},
            {-24, 42, 1.7F}, {-156, 42, 1.7F}, {-2, 38, 1.8F}, {-178, 38, 1.8F}, {20, 32, 1.8F}, {160, 32, 1.8F}};
    /** The beard's fins under the jaw. */
    static final float[][] BEARD = {{90, 26, 0.6F}, {66, 22, 0.7F}, {114, 22, 0.7F}, {44, 18, 0.8F}, {136, 18, 0.8F}};
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
        // The neck slopes down and back, under the front of the shell.
        root.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 136).addBox(-20.0F, -20.0F, 0.0F, 40.0F, 40.0F, 50.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 12.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                // The skull, the snout and its nose pad, the jaw.
                .texOffs(0, 0).addBox(-24.0F, -26.0F, -20.0F, 48.0F, 40.0F, 40.0F)
                .texOffs(176, 0).addBox(-17.0F, -6.0F, -34.0F, 34.0F, 20.0F, 14.0F)
                .texOffs(272, 0).addBox(-9.0F, -9.0F, -36.0F, 18.0F, 6.0F, 8.0F)
                .texOffs(0, 80).addBox(-20.0F, 14.0F, -32.0F, 40.0F, 10.0F, 46.0F)
                // The brows, the eyes standing out under them.
                .texOffs(324, 0).addBox(-22.0F, -19.0F, -23.0F, 16.0F, 6.0F, 8.0F)
                .texOffs(324, 0).mirror().addBox(6.0F, -19.0F, -23.0F, 16.0F, 6.0F, 8.0F).mirror(false)
                .texOffs(372, 0).addBox(-20.0F, -13.0F, -22.0F, 12.0F, 8.0F, 2.0F)
                .texOffs(372, 0).mirror().addBox(8.0F, -13.0F, -22.0F, 12.0F, 8.0F, 2.0F).mirror(false)
                // A ruff of skin behind the skull, where the fins grow from.
                .texOffs(176, 80).addBox(-30.0F, -32.0F, 8.0F, 60.0F, 52.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        fins(head, "crown", CROWN, 26.0F, 24.0F, -7.0F, 6.0F);
        fins(head, "beard", BEARD, 16.0F, 6.0F, 18.0F, -14.0F);
        // The whiskers, from the sides of the snout: out, a little forward, and drooping.
        for (int side = -1; side <= 1; side += 2) {
            float dx = side * 0.8F;
            float dy = 0.55F;
            float dz = -0.25F;
            float n = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            head.addOrReplaceChild(side < 0 ? "right_whisker" : "left_whisker", CubeListBuilder.create()
                    .texOffs(330, 34).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 30.0F),
                    PartPose.offsetAndRotation(side * 16.0F, 6.0F, -30.0F, (float) Math.asin(-dy / n), (float) Math.atan2(dx / n, dz / n), 0.0F));
        }
        root.addOrReplaceChild("left_flipper", CubeListBuilder.create()
                .texOffs(0, 226).addBox(0.0F, -3.0F, -14.0F, 60.0F, 6.0F, 28.0F), PartPose.offset(20.0F, 18.0F, 8.0F));
        root.addOrReplaceChild("right_flipper", CubeListBuilder.create()
                .texOffs(0, 226).mirror().addBox(-60.0F, -3.0F, -14.0F, 60.0F, 6.0F, 28.0F), PartPose.offset(-20.0F, 18.0F, 8.0F));
        return LayerDefinition.create(mesh, 512, 512);
    }

    /**
     * Fins round the face: each a broad base and a narrow tip, rooted on an ellipse (rx by ry, centred at y cy, z cz)
     * and sweeping back and out.
     */
    private static void fins(PartDefinition head, String name, float[][] fins, float rx, float ry, float cy, float cz) {
        for (int i = 0; i < fins.length; i++) {
            float ang = fins[i][0] * Mth.DEG_TO_RAD;
            float len = fins[i][1];
            float splay = fins[i][2];
            float ox = Mth.cos(ang);
            float oy = Mth.sin(ang);
            // The way it points: back (+z), and out from the face by its splay.
            float dx = ox * splay;
            float dy = oy * splay;
            float dz = 1.0F;
            float n = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            float pitch = (float) Math.asin(-dy / n);
            float yaw = (float) Math.atan2(dx / n, dz / n);
            float base = Math.round(len * 0.55F);
            // Pointed first, then turned about its own length so its broad side lies along the face's edge.
            PartDefinition root = head.addOrReplaceChild(name + i, CubeListBuilder.create(),
                    PartPose.offsetAndRotation(ox * rx, cy + oy * ry, cz, pitch, yaw, 0.0F));
            PartDefinition fin = root.addOrReplaceChild("blade", CubeListBuilder.create()
                    .texOffs(176, 34).addBox(-5.0F, -2.5F, 0.0F, 10.0F, 5.0F, base),
                    PartPose.rotation(0.0F, 0.0F, ang + Mth.HALF_PI));
            fin.addOrReplaceChild("tip", CubeListBuilder.create()
                    .texOffs(260, 34).addBox(-3.0F, -1.5F, 0.0F, 6.0F, 3.0F, len - base),
                    PartPose.offset(0.0F, 0.0F, base - 1.0F));
        }
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(LionTurtleEntity turtle, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = ageInTicks - turtle.tickCount;
        // The head turns a little toward who looks at it; it is too great to turn far. It breathes.
        head.yRot = Mth.clamp(netHeadYaw, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.4F;
        head.xRot = Mth.clamp(headPitch, -20.0F, 20.0F) * Mth.DEG_TO_RAD * 0.3F + Mth.sin(ageInTicks * 0.02F) * 0.02F;
        // The flippers paddle slowly, as a turtle swims.
        float stroke = Mth.sin(ageInTicks * 0.03F);
        leftFlipper.zRot = 0.12F + stroke * 0.15F;
        leftFlipper.yRot = -0.35F + stroke * 0.2F;
        leftFlipper.xRot = 0.0F;
        rightFlipper.zRot = -0.12F - stroke * 0.15F;
        rightFlipper.yRot = 0.35F - stroke * 0.2F;
        rightFlipper.xRot = 0.0F;
        // Giving: one flipper rises beside the face and reaches forward, holds, and sinks back.
        float g = turtle.gift(partial);
        if (g >= 0.0F) {
            float up = g < 0.2F ? g / 0.2F : g > 0.85F ? (1.0F - g) / 0.15F : 1.0F;
            up = up * up * (3.0F - 2.0F * up);
            rightFlipper.zRot = Mth.lerp(up, rightFlipper.zRot, 1.2F);
            rightFlipper.yRot = Mth.lerp(up, rightFlipper.yRot, 0.15F);
            rightFlipper.xRot = 0.0F;
            head.xRot += up * 0.15F;
        }
    }
}
