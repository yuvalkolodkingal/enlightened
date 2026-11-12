package com.kolodkingal.enlightened.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 26.3: the container screen's corner and hovered slot have no public getters (compat.Gui26). */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int enlightened$leftPos();

    @Accessor("topPos")
    int enlightened$topPos();

    @Accessor("hoveredSlot")
    Slot enlightened$hoveredSlot();
}
