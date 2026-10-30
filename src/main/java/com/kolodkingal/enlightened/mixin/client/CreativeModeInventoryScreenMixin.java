package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.item.BackSlot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** In the creative inventory tab the back slot sits beside the armour, not over the hotbar where an unknown slot would go. Client only. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    private static final String WRAPPER = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V";

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE", target = WRAPPER), index = 2)
    private int enlightened$backX(Slot slot, int index, int x, int y) {
        return slot instanceof BackSlot ? BackSlot.CREATIVE_X : x;
    }

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE", target = WRAPPER), index = 3)
    private int enlightened$backY(Slot slot, int index, int x, int y) {
        return slot instanceof BackSlot ? BackSlot.CREATIVE_Y : y;
    }
}
