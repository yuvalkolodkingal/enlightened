package com.kolodkingal.enlightened.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether a living entity holds its jump key: the sky bison's rider holds it to climb. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean enlightened$isJumping();
}
