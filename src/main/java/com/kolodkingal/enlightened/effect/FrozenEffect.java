package com.kolodkingal.enlightened.effect;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Frozen by a waterbender: very slow, shivering (the freezing of powder snow), and unable to jump. */
public class FrozenEffect extends MobEffect {
    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0xBFE8FF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Enlightened.id("frozen"), -0.7,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.canFreeze()) {
            entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze() + 20));
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    2, 0.3, 0.4, 0.3, 0.01);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
