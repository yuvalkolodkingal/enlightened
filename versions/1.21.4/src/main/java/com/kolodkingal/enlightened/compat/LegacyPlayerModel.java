package com.kolodkingal.enlightened.compat;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.21.4 only: a mob of the mod drawn on the player model, written against the 1.21.1 model: posed from the entity, its
 * {@code super.setupAnim(entity, ...)} the vanilla humanoid animation with those numbers.
 */
public class LegacyPlayerModel<T extends LivingEntity> extends PlayerModel {
    private PlayerRenderState current;

    public LegacyPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setupAnim(PlayerRenderState state) {
        Entity entity = RenderBridge.entity(state);
        if (entity instanceof LivingEntity) {
            current = state;
            setupAnim((T) entity, state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot, state.xRot);
        } else {
            super.setupAnim(state);
        }
    }

    /** The vanilla humanoid animation with these numbers (a subclass's super call). */
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerRenderState state = current != null ? current : new PlayerRenderState();
        state.walkAnimationPos = limbSwing;
        state.walkAnimationSpeed = limbSwingAmount;
        state.ageInTicks = ageInTicks;
        state.yRot = netHeadYaw;
        state.xRot = headPitch;
        super.setupAnim(state);
    }

    /** 1.21.1's {@code riding = ...}: whether the body is posed seated. */
    protected void legacyRiding(boolean riding) {
        if (current != null) {
            current.isPassenger = riding;
        }
    }
}
