package com.kolodkingal.enlightened.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the back slot be added to the player's inventory menu. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {
    @Invoker("addSlot")
    Slot enlightened$addSlot(Slot slot);
}
