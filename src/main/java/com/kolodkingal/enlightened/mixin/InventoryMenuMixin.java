package com.kolodkingal.enlightened.mixin;

import com.kolodkingal.enlightened.item.BackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player's inventory gets one more slot, the back slot, after all of its own (so no vanilla index moves). */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void enlightened$backSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        ((AbstractContainerMenuInvoker) this).enlightened$addSlot(new BackSlot(owner));
    }
}
