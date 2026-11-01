package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.SunWarriorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A Sun Warrior on the player model, in the cream tunic, red sash and golden apron of his people (the chief in his
 * tall golden headdress); a dancer dances with his arms raised and swaying, the flames over his hands.
 */
public class SunWarriorRenderer extends HumanoidMobRenderer<SunWarriorEntity, SunWarriorRenderer.Dancer> {
    private static final ResourceLocation[] FACES = new ResourceLocation[SunWarriorEntity.FACES];
    private static final ResourceLocation CHIEF = Enlightened.id("textures/entity/sun_warrior/chief.png");

    static {
        for (int i = 0; i < FACES.length; i++) {
            FACES[i] = Enlightened.id("textures/entity/sun_warrior/warrior_" + i + ".png");
        }
    }

    public SunWarriorRenderer(EntityRendererProvider.Context context) {
        super(context, new Dancer(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SunWarriorEntity entity) {
        return entity.role() == SunWarriorEntity.Role.CHIEF ? CHIEF : FACES[entity.face()];
    }

    public static final class Dancer extends PlayerModel<SunWarriorEntity> {
        Dancer(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(SunWarriorEntity w, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(w, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (w.dancing()) {
                float sway = Mth.sin(ageInTicks * 0.25F);
                rightArm.xRot = (float) Math.PI + 0.3F + sway * 0.35F;
                leftArm.xRot = (float) Math.PI + 0.3F - sway * 0.35F;
                rightArm.zRot = 0.35F + sway * 0.2F;
                leftArm.zRot = -0.35F + sway * 0.2F;
                body.zRot = sway * 0.08F;
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
                jacket.copyFrom(body);
            }
        }
    }
}
