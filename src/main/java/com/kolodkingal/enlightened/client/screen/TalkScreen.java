package com.kolodkingal.enlightened.client.screen;

import com.kolodkingal.enlightened.network.ModNetwork;
import com.kolodkingal.enlightened.network.TalkPayload;
import com.kolodkingal.enlightened.talk.Speaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The talk screen (1.0.23, after Enhanced's Asgardian talk): a panel at the bottom of the screen, the speaker's face cut
 * from his own skin (or, for the lion turtle and Raava, a symbol) in a frame of his colour, his name, his line typed out
 * at 40 letters a second (a click, Space or Enter finishes it), and up to five answers above the panel (keys 1-5 choose
 * them); "Farewell" or Esc closes it. In a right-to-left language the face moves to the right and the text aligns right.
 */
public class TalkScreen extends Screen {
    static final float TYPE_SPEED = 2.0F;
    static final int PANEL_HEIGHT = 84;
    static final int FACE = 40;
    static final int BUTTON_HEIGHT = 20;

    private final TalkPayload.Open talk;
    private final Speaker who;
    private final String text;
    private float typed;
    private boolean answered;
    private int waiting = -1;

    TalkScreen(TalkPayload.Open talk) {
        super(Component.translatable(whoOf(talk).nameKey));
        this.talk = talk;
        this.who = whoOf(talk);
        Object[] args = new Object[talk.args().size()];
        for (int i = 0; i < args.length; i++) {
            String arg = talk.args().get(i);
            args[i] = arg.startsWith("@") ? Component.translatable(arg.substring(1)) : arg;
        }
        this.text = Component.translatable(talk.line(), args).getString();
    }

    private static Speaker whoOf(TalkPayload.Open talk) {
        Speaker[] all = Speaker.values();
        return talk.speaker() >= 0 && talk.speaker() < all.length ? all[talk.speaker()] : Speaker.BUMI;
    }

    /** The server opened a talk (or said the next line of one). */
    public static void open(TalkPayload.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TalkScreen old) {
            old.answered = true; // replaced: the talk goes on
        }
        mc.setScreen(new TalkScreen(payload));
    }

    static boolean rightToLeft() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    private int panelWidth() {
        return Math.min(400, width - 20);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return height - PANEL_HEIGHT - 12;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(320, panelWidth() - 40);
        int x = (width - buttonWidth) / 2;
        List<String> options = talk.options();
        int y = panelTop() - 6 - options.size() * (BUTTON_HEIGHT + 4);
        for (int i = 0; i < options.size(); i++) {
            addRenderableWidget(new Answer(x, y + i * (BUTTON_HEIGHT + 4), buttonWidth, i + 1, options.get(i)));
        }
        Component bye = Component.translatable("npc.enlightened.screen.farewell");
        int byeWidth = font.width(bye) + 12;
        int byeX = rightToLeft() ? panelLeft() + 6 : panelLeft() + panelWidth() - byeWidth - 6;
        addRenderableWidget(new Farewell(byeX, panelTop() + PANEL_HEIGHT - 16, byeWidth, bye));
    }

    @Override
    public void tick() {
        typed = Math.min(text.length(), typed + TYPE_SPEED);
        if (waiting >= 0 && ++waiting > 10) {
            onClose();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.level.getEntity(talk.entityId()) == null) {
            onClose();
        }
    }

    private boolean doneTyping() {
        return typed >= text.length();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!doneTyping() && mouseY >= panelTop()) {
            typed = text.length();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The world stays visible: only the panel is drawn.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int w = panelWidth();
        int color = 0xFF000000 | who.color;
        g.fillGradient(left, top, left + w, top + PANEL_HEIGHT, 0xE0141A2C, 0xE00A0E18);
        g.renderOutline(left, top, w, PANEL_HEIGHT, color);
        g.fill(left + 1, top + 1, left + w - 1, top + 2, 0x60FFFFFF & color);
        boolean rtl = rightToLeft();
        int faceX = rtl ? left + w - FACE - 10 : left + 10;
        int faceY = top + 12;
        g.fill(faceX - 2, faceY - 2, faceX + FACE + 2, faceY + FACE + 2, color);
        g.fill(faceX - 1, faceY - 1, faceX + FACE + 1, faceY + FACE + 1, 0xFF000000);
        ResourceLocation skin = who.symbol == null ? skin() : null;
        if (skin != null) {
            g.blit(skin, faceX, faceY, FACE, FACE, 8, 8, 8, 8, 64, 64);
            g.blit(skin, faceX, faceY, FACE, FACE, 40, 8, 8, 8, 64, 64);
        } else {
            ScrollUi.drawSymbol(g, com.kolodkingal.enlightened.bending.Ability.byId(who.symbol == null ? "spiritbending" : who.symbol) != null
                    ? ScrollUi.symbol(com.kolodkingal.enlightened.bending.Ability.byId(who.symbol == null ? "spiritbending" : who.symbol))
                    : ScrollUi.symbol(com.kolodkingal.enlightened.bending.Ability.SPIRITBENDING), faceX, faceY, FACE);
        }
        int textLeft = rtl ? left + 10 : faceX + FACE + 10;
        int textRight = rtl ? faceX - 10 : left + w - 10;
        int textWidth = textRight - textLeft;
        Component name = title.copy().withStyle(s -> s.withColor(who.color).withBold(true));
        g.drawString(font, name, rtl ? textRight - font.width(name) : textLeft, top + 8, 0xFFFFFF, true);
        int shown = Mth.floor(typed);
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(FormattedText.of(text.substring(0, Math.min(shown, text.length()))), textWidth));
        int y = top + 22;
        for (FormattedCharSequence l : lines) {
            if (y > top + PANEL_HEIGHT - 20) {
                break;
            }
            g.drawString(font, l, rtl ? textRight - font.width(l) : textLeft, y, 0xF2EEDD, false);
            y += font.lineHeight + 1;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!doneTyping() && mc.level != null && (mc.level.getGameTime() / 6) % 2 == 0) {
            g.drawString(font, "▸", rtl ? left + 10 : left + w - 14, top + PANEL_HEIGHT - 14, color, false);
        }
        for (var child : children()) {
            if (child instanceof Answer answer) {
                answer.visible = doneTyping();
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** The speaker's skin, from his renderer on this client (a 64x64 player-layout skin), or null. */
    @Nullable
    @SuppressWarnings("unchecked")
    private ResourceLocation skin() {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(talk.entityId());
        if (entity == null) {
            return null;
        }
        //#if >=1.21.4
        //$$ var renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        //$$ if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living) {
        //$$     return living.getTextureLocation((net.minecraft.client.renderer.entity.state.LivingEntityRenderState)
        //$$             living.createRenderState(entity, 1.0F));
        //$$ }
        //$$ return null;
        //#else
        EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) mc.getEntityRenderDispatcher().getRenderer(entity);
        return renderer.getTextureLocation(entity);
        //#endif
    }

    private void answer(String option) {
        if (answered) {
            return;
        }
        answered = true;
        ModNetwork.sendToServer(new TalkPayload.Choose(talk.entityId(), option));
        waiting = 0;
        for (var child : children()) {
            if (child instanceof AbstractButton button) {
                button.active = false;
            }
        }
    }

    @Override
    public void removed() {
        if (!answered) {
            answered = true;
            ModNetwork.sendToServer(new TalkPayload.Choose(talk.entityId(), ""));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class Answer extends AbstractButton {
        private final String option;
        private final int number;

        Answer(int x, int y, int width, int number, String option) {
            super(x, y, width, BUTTON_HEIGHT, Component.translatable("npc.enlightened.option." + option));
            this.option = option;
            this.number = number;
        }

        @Override
        public void onPress() {
            answer(option);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused() && active;
            int color = 0xFF000000 | who.color;
            g.fill(getX(), getY(), getX() + width, getY() + height, lit ? 0xE0283048 : 0xD0121828);
            g.renderOutline(getX(), getY(), width, height, lit ? color : 0xFF3A4258);
            boolean rtl = rightToLeft();
            String n = number + ".";
            Component words = getMessage();
            int textY = getY() + (height - 8) / 2;
            int ink = active ? (lit ? 0xFFFFFF : 0xE6E0CC) : 0x808080;
            int room = width - 18 - font.width(n);
            if (rtl) {
                g.drawString(font, n, getX() + width - 6 - font.width(n), textY, color, false);
                int ww = Math.min(font.width(words), room);
                ScrollUi.drawFitted(g, font, words.copy().withStyle(s -> s.withColor(ink)), getX() + width - 12 - font.width(n) - ww, textY, room, ink);
            } else {
                g.drawString(font, n, getX() + 6, textY, color, false);
                ScrollUi.drawFitted(g, font, words.copy().withStyle(s -> s.withColor(ink)), getX() + 12 + font.width(n), textY, room, ink);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class Farewell extends AbstractButton {
        Farewell(int x, int y, int width, Component message) {
            super(x, y, width, 12, message);
        }

        @Override
        public void onPress() {
            onClose();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int ink = isHoveredOrFocused() ? 0xFFFFFF : 0x9A9A9A;
            g.drawString(font, getMessage(), getX() + 6, getY() + 2, ink, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int index = keyCode - 49; // GLFW_KEY_1
        if (index >= 0 && index < talk.options().size() && doneTyping()) {
            answer(talk.options().get(index));
            return true;
        }
        if (!doneTyping() && (keyCode == 32 || keyCode == 257)) {
            typed = text.length();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
