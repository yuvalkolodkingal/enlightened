package com.kolodkingal.enlightened.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The pieces the Bending Scroll is built of, after the Hero Creator of Enhanced: big buttons with a coloured frame on a
 * dark panel, a bold coloured label and a grey line under it, and text that squeezes to fit instead of being cut.
 */
public final class ScrollUi {
    private ScrollUi() {
    }

    public static int darken(int rgb) {
        return (rgb >> 1) & 0x7F7F7F;
    }

    /** A dark panel framed in a colour (the preview's, the detail's). */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, 0x70000000);
        g.renderOutline(x, y, w, h, 0xFF000000 | darken(color));
    }

    /**
     * A big button: a dark panel framed in its colour, an item on the left if it has one, the label in bold colour and an
     * optional grey line under it. Left click runs its action; right click runs its right action, if it has one.
     */
    public static class BigButton extends AbstractButton {
        public int color;
        @Nullable
        public Component sub;
        @Nullable
        public ItemStack icon;
        /** A picture (an ability's symbol, 32 px square) drawn on the left in place of an item. */
        @Nullable
        public ResourceLocation symbol;
        public boolean selected;
        private final Runnable action;
        @Nullable
        private Consumer<BigButton> rightAction;

        public BigButton(int x, int y, int width, int height, Component label, @Nullable Component sub, int color, Runnable action) {
            super(x, y, width, height, label);
            this.sub = sub;
            this.color = color;
            this.action = action;
        }

        public BigButton icon(@Nullable ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public BigButton selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public BigButton onRight(Consumer<BigButton> rightAction) {
            this.rightAction = rightAction;
            return this;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == 0 || (button == 1 && rightAction != null);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1 && rightAction != null && visible && isMouseOver(mouseX, mouseY)) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                rightAction.accept(this);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean lit = isHoveredOrFocused() && active;
            int x = getX();
            int y = getY();
            g.fill(x, y, x + width, y + height, selected ? 0xF0283A52 : lit ? 0xE0182436 : 0xC0101824);
            g.renderOutline(x, y, width, height, 0xFF000000 | (lit || selected ? color : darken(color)));
            if (lit || selected) {
                g.renderOutline(x + 1, y + 1, width - 2, height - 2, 0x80000000 | color);
            }
            int textX = x + 6;
            if (symbol != null) {
                int size = Math.min(height - 4, 20);
                drawSymbol(g, symbol, x + 3, y + (height - size) / 2, size);
                textX = x + 6 + size;
            } else if (icon != null && height >= 18) {
                g.renderFakeItem(icon, x + 4, y + (height - 16) / 2);
                textX = x + 24;
            }
            int textColor = active ? color : 0x707070;
            Component label = getMessage().copy().withStyle(s -> s.withColor(textColor).withBold(true));
            int room = x + width - 4 - textX;
            if (sub == null || height < 18) {
                drawFitted(g, font, label, textX, y + (height - 8) / 2, room, 0xFFFFFF);
            } else {
                drawFitted(g, font, label, textX, y + height / 2 - 9, room, 0xFFFFFF);
                drawFitted(g, font, sub.copy().withStyle(s -> s.withColor(active ? 0xB0B0B0 : 0x606060)), textX, y + height / 2 + 2, room, 0xFFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** An ability's symbol, at any size. */
    public static void drawSymbol(GuiGraphics g, ResourceLocation symbol, int x, int y, int size) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(symbol, x, y, size, size, 0, 0, 32, 32, 32, 32);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /** The symbol of an ability (textures/gui/ability/&lt;id&gt;.png, drawn by tools/symbols.py). */
    public static ResourceLocation symbol(com.kolodkingal.enlightened.bending.Ability a) {
        return ResourceLocation.fromNamespaceAndPath("enlightened", "textures/gui/ability/" + a.id() + ".png");
    }

    /** Text drawn at its size if it fits {@code room}, else squeezed to fit (never cut). */
    public static void drawFitted(GuiGraphics g, Font font, Component text, int x, int y, int room, int color) {
        int w = font.width(text);
        if (w <= room || room <= 0) {
            g.drawString(font, text, x, y, color, true);
            return;
        }
        float scale = (float) room / w;
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - scale) * 4, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, true);
        g.pose().popPose();
    }

    /** Text drawn larger (a heading), squeezed if it would pass {@code room}. */
    public static void drawBig(GuiGraphics g, Font font, Component text, int x, int y, float scale, int room) {
        float s = Math.min(scale, room / (float) Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, 0, 0, 0xFFFFFF, true);
        g.pose().popPose();
    }
}
