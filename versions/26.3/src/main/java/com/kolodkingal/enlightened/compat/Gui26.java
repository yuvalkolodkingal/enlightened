package com.kolodkingal.enlightened.compat;

import com.kolodkingal.enlightened.mixin.client.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/** 26.3: what the GUI code written against 1.21.1 asks of the screen and the HUD, which 26.1 moved or hid. */
public final class Gui26 {
    private Gui26() {
    }

    /** 1.21.1's {@code options.hideGui = hidden}: the HUD is hidden or shown. */
    public static void hideGui(boolean hidden) {
        var hud = Minecraft.getInstance().gui.hud;
        if (hud.isHidden() != hidden) {
            hud.toggle();
        }
    }

    /** 1.21.1's mouse button poll, from the mouse handler (the left, middle and right buttons; others read as up). */
    public static boolean mouseDown(int button) {
        var mouse = Minecraft.getInstance().mouseHandler;
        return switch (button) {
            case com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT -> mouse.isLeftPressed();
            case com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE -> mouse.isMiddlePressed();
            case com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT -> mouse.isRightPressed();
            default -> false;
        };
    }

    /** 1.21.1's {@code EditBox.setFilter}: text the filter refuses is put back to the last text it took. */
    public static void filter(net.minecraft.client.gui.components.EditBox box, java.util.function.Predicate<String> filter) {
        String[] last = {box.getValue()};
        box.setResponder(t -> {
            if (filter.test(t)) {
                last[0] = t;
            } else {
                box.setValue(last[0]);
            }
        });
    }

    public static int left(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).enlightened$leftPos();
    }

    public static int top(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).enlightened$topPos();
    }

    @Nullable
    public static Slot hovered(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).enlightened$hoveredSlot();
    }
}
