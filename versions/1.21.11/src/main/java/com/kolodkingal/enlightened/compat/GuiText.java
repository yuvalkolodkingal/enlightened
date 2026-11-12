package com.kolodkingal.enlightened.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.11: from 1.21.6 the GUI does not draw text whose colour has no alpha, which 1.21.1 drew opaque. The GUI code
 * written against 1.21.1 gives colours as 0xRRGGBB in places; they are drawn opaque here, as on 1.21.1.
 */
public final class GuiText {
    private GuiText() {
    }

    private static int opaque(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    public static void drawString(GuiGraphics g, Font font, @Nullable String text, int x, int y, int color) {
        g.drawString(font, text, x, y, opaque(color));
    }

    public static void drawString(GuiGraphics g, Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, opaque(color), shadow);
    }

    public static void drawString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.drawString(font, text, x, y, opaque(color));
    }

    public static void drawString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, opaque(color), shadow);
    }

    public static void drawString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, opaque(color));
    }

    public static void drawString(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, opaque(color), shadow);
    }

    public static void drawCenteredString(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawCenteredString(font, text, x, y, opaque(color));
    }

    public static void drawCenteredString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawCenteredString(font, text, x, y, opaque(color));
    }

    public static void drawCenteredString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.drawCenteredString(font, text, x, y, opaque(color));
    }

    public static void drawWordWrap(GuiGraphics g, Font font, FormattedText text, int x, int y, int width, int color) {
        g.drawWordWrap(font, text, x, y, width, opaque(color));
    }

    public static void drawWordWrap(GuiGraphics g, Font font, FormattedText text, int x, int y, int width, int color, boolean shadow) {
        g.drawWordWrap(font, text, x, y, width, opaque(color), shadow);
    }
}
