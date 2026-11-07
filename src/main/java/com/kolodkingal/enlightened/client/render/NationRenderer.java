package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.NationNpcEntity;
import com.kolodkingal.enlightened.entity.OzaiEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;

/**
 * The people of the Fire Nation on the player model (1.0.27): Ozai in his Fire Lord's robes (his look's skin; on his
 * jets his arms point down and back, as Iroh's do), Zuko, and the herald.
 */
public class NationRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private static final ResourceLocation OZAI = Enlightened.id("textures/entity/suit/ozai.png");
    private static final ResourceLocation ZUKO = Enlightened.id("textures/entity/suit/zuko.png");
    private static final ResourceLocation HERALD = Enlightened.id("textures/entity/fire_herald.png");
    private final Function<T, ResourceLocation> skin;

    private NationRenderer(EntityRendererProvider.Context context, PlayerModel<T> model, Function<T, ResourceLocation> skin) {
        super(context, model, 0.5F);
        this.skin = skin;
    }

    public static NationRenderer<OzaiEntity> ozai(EntityRendererProvider.Context context) {
        return new NationRenderer<>(context, new Jets(context.bakeLayer(ModelLayers.PLAYER)), e -> OZAI);
    }

    public static NationRenderer<NationNpcEntity> npc(EntityRendererProvider.Context context) {
        return new NationRenderer<>(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false),
                e -> e.kind() == NationNpcEntity.Kind.ZUKO ? ZUKO : HERALD);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return skin.apply(entity);
    }

    static final class Jets extends PlayerModel<OzaiEntity> {
        Jets(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(OzaiEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (entity.jets()) {
                float wobble = (float) Math.sin(ageInTicks * 0.6F) * 0.05F;
                rightArm.xRot = 0.4F + wobble;
                leftArm.xRot = 0.4F - wobble;
                rightArm.zRot = 0.35F;
                leftArm.zRot = -0.35F;
                rightLeg.xRot = 0.15F;
                leftLeg.xRot = -0.05F;
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
                rightPants.copyFrom(rightLeg);
                leftPants.copyFrom(leftLeg);
            } else if (entity.prisoner()) {
                // In chains: arms hanging, head bowed.
                head.xRot = 0.5F;
                hat.copyFrom(head);
            }
        }
    }
}
