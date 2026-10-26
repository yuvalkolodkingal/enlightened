package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.SuitSkins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swaps in the skin of a bender's look (Toph, Zuko, Katara...) wherever the player's skin is drawn. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void enlightened$suit(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin own = cir.getReturnValue();
        PlayerSkin skin = SuitSkins.skinFor((AbstractClientPlayer) (Object) this, own);
        if (skin != own) {
            cir.setReturnValue(skin);
        }
    }
}
