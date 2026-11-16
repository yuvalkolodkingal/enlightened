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

    /**
     * 1.21.1's number of a mouse button 26.1 numbers {@code sdl}: 26.1 moved to SDL's (left 1, middle 2, right 3), the
     * screens read 1.21.1's (left 0, right 1, middle 2). Without it a left click came in as "1", and the Bending Scroll's
     * buttons took no left click at all.
     */
    public static int legacyButton(int sdl) {
        return switch (sdl) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 1;
            default -> sdl > 3 ? sdl - 1 : sdl;
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
