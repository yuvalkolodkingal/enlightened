package com.kolodkingal.enlightened.effect;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

/**
 * Sinking in quicksand (a sandbender's trick): slow, pulled down, unable to jump, and a little hurt as it sinks (the
 * amplifier is the level of the sandbender minus one: deeper and more painful).
 */
public class QuicksandEffect extends MobEffect {
    public QuicksandEffect() {
        super(MobEffectCategory.HARMFUL, 0xC2B280);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Enlightened.id("quicksand"), -0.8,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.onGround() || entity.getDeltaMovement().y > -0.02) {
            entity.setDeltaMovement(entity.getDeltaMovement().x * 0.5, -0.06 - amplifier * 0.02, entity.getDeltaMovement().z * 0.5);
            entity.hurtMarked = true;
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 5 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    entity.getX(), entity.getY() + 0.2, entity.getZ(), 6, 0.4, 0.1, 0.4, 0.05);
        }
        if (entity.tickCount % 20 == 0 && amplifier > 0) {
            entity.hurt(entity.damageSources().inWall(), amplifier);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
