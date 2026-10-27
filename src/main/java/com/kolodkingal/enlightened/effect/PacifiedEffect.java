package com.kolodkingal.enlightened.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Calmed by spiritbending: the creature forgets its prey, fights no one, and mends a little, while it lasts. */
public class PacifiedEffect extends MobEffect {
    public PacifiedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2D66B);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
            if (mob instanceof net.minecraft.world.entity.NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
        }
        if (entity.tickCount % 40 == 0) {
            entity.heal(1.0F);
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.7, entity.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
