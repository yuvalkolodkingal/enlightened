package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.anim.BenderPoses;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The benders' poses, blended on top of the vanilla animation. The reset at the head clears the two fields vanilla
 * never resets and the poses write (this model is shared by everyone with the same arm width).
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void enlightened$reset(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        BenderPoses.reset((PlayerModel<?>) (Object) this);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void enlightened$poses(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        BenderPoses.apply((PlayerModel<?>) (Object) this, entity, limbSwing, limbSwingAmount, ageInTicks);
    }
}
