package com.kolodkingal.enlightened.effect;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Struck by the Anomaly's red lightning: dread. The legs go heavy; on the client the sight flashes between red and
 * black, the world shakes, a heart pounds (DreadOverlay, ClientEvents).
 */
public class DreadEffect extends MobEffect {
    /** How long one strike's dread lasts, in ticks. */
    public static final int DURATION = 100;

    public DreadEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0000);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Enlightened.id("dread"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
