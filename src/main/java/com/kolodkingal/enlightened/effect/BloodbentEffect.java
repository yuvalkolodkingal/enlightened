package com.kolodkingal.enlightened.effect;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Held by a bloodbender: the body is not its own. It cannot walk or jump ({@code CommonEvents} stops the jump), and a
 * bender under it cannot bend. The bloodbender's own code moves it where he looks.
 */
public class BloodbentEffect extends MobEffect {
    public BloodbentEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Enlightened.id("bloodbent"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
