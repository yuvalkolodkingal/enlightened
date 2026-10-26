package com.kolodkingal.enlightened.client.screen;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Gate;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.Variants;
import com.kolodkingal.enlightened.client.BendingHud;
import com.kolodkingal.enlightened.client.KeyBindings;
import com.kolodkingal.enlightened.network.ModNetwork;
import com.kolodkingal.enlightened.network.ScrollActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bending Scroll: on the left every ability of the bender's element, with its level in stars, its cost, its gate
 * and a Learn button; on the right the ten slots of the loadout (each key plain and with Sneak). Click an ability to
 * select it, then a slot to put it there; right-click a slot to empty it. Everything is read from the synced bending
 * data, so the screen always shows what the server knows.
 */
public class ScrollScreen extends Screen {
    private static final int ROW = 11;
    private static final int LEFT = 8;
    @Nullable
    private Ability selected;
    private final List<Button> learnButtons = new ArrayList<>();
    private final List<Button> slotButtons = new ArrayList<>();
    private int listTop;
    private int slotsLeft;
    private int scroll;
    private List<Ability> abilities = List.of();

    public ScrollScreen() {
        super(Component.translatable("screen.enlightened.scroll"));
    }

    private BendingData data() {
        return Benders.get(minecraft.player);
    }

    @Override
    protected void init() {
        super.init();
        learnButtons.clear();
        slotButtons.clear();
        BendingData data = data();
        Element element = data.element();
        abilities = Ability.of(element);
        listTop = 30;
        int rows = Math.max(1, (height - listTop - 30) / ROW);
        int listWidth = Math.min(230, width / 2 + 20);
        for (int i = 0; i < Math.min(rows, abilities.size()); i++) {
            final int row = i;
            Button learn = Button.builder(Component.literal("+"), b -> {
                Ability a = rowAbility(row);
                if (a != null) {
                    ModNetwork.sendToServer(ScrollActionPayload.learn(a));
                }
            }).bounds(LEFT + listWidth - 14, listTop + i * ROW, 12, ROW - 1).build();
            learnButtons.add(addRenderableWidget(learn));
        }
        slotsLeft = LEFT + listWidth + 12;
        int slotWidth = Math.max(60, width - slotsLeft - 8);
        for (int key = 1; key <= BendingData.KEYS; key++) {
            for (int s = 0; s < 2; s++) {
                final int slot = BendingData.slotIndex(key, s == 1);
                Button button = Button.builder(Component.empty(), b -> {
                    if (selected != null) {
                        ModNetwork.sendToServer(ScrollActionPayload.assign(slot, selected));
                    }
                }).bounds(slotsLeft, listTop + (key - 1) * 2 * 14 + s * 14 + 10, slotWidth, 13).build();
                slotButtons.add(addRenderableWidget(button));
            }
        }
        int bottom = height - 24;
        addRenderableWidget(Button.builder(Component.translatable("screen.enlightened.scroll.reset_loadout"),
                b -> ModNetwork.sendToServer(ScrollActionPayload.resetLoadout())).bounds(slotsLeft, bottom, Math.min(slotWidth, 120), 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(LEFT, bottom, 80, 18).build());
    }

    @Nullable
    private Ability rowAbility(int row) {
        int index = row + scroll;
        return index >= 0 && index < abilities.size() ? abilities.get(index) : null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        BendingData data = data();
        Element element = data.element();
        // Refresh the buttons' faces before they draw.
        for (int i = 0; i < learnButtons.size(); i++) {
            Ability a = rowAbility(i);
            Button b = learnButtons.get(i);
            b.visible = a != null;
            b.active = a != null && Mastery.canLearn(data, a);
        }
        for (int slot = 0; slot < slotButtons.size(); slot++) {
            Ability a = data.slot(slot);
            Button b = slotButtons.get(slot);
            int key = slot / 2 + 1;
            boolean sneak = slot % 2 == 1;
            MutableComponent label = Component.literal((sneak ? "⇧ " : "") + "[")
                    .append(KeyBindings.ability(key).getTranslatedKeyMessage()).append("] ").withStyle(ChatFormatting.YELLOW);
            if (a != null && data.has(a)) {
                label.append(a.displayName().copy().withStyle(ChatFormatting.WHITE));
            } else {
                label.append(Component.literal("-").withStyle(ChatFormatting.DARK_GRAY));
            }
            b.setMessage(label);
            b.active = selected != null && selected.bindable();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, element.awakeningTitle().copy().append(" ").append(Variants.current(data).displayName())
                .withStyle(s -> s.withColor(element.color).withBold(true)), width / 2, 8, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.enlightened.scroll.points", data.points(), data.spent())
                .withStyle(ChatFormatting.GOLD), LEFT, 19, 0xFFFFFF, true);
        graphics.drawString(font, Component.translatable("screen.enlightened.scroll.loadout").withStyle(ChatFormatting.AQUA), slotsLeft, listTop, 0xFFFFFF, true);
        for (int i = 0; i < learnButtons.size(); i++) {
            Ability a = rowAbility(i);
            if (a == null) {
                continue;
            }
            int y = listTop + i * ROW + 1;
            int level = data.level(a);
            boolean open = Mastery.gateOpen(data, a);
            ChatFormatting color = a == selected ? ChatFormatting.YELLOW : level > 0 ? ChatFormatting.WHITE : open ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
            MutableComponent line = a.displayName().copy().withStyle(color);
            line.append(Component.literal(" " + BendingHud.stars(level)).withStyle(ChatFormatting.GOLD));
            if (level < Ability.MAX_LEVEL) {
                line.append(Component.translatable("screen.enlightened.scroll.cost", Ability.cost(level + 1)).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (!open) {
                line.append(Component.literal(" ")).append(a.gate == Gate.TETHERLESS
                        ? Component.translatable("screen.enlightened.scroll.tethered").withStyle(ChatFormatting.DARK_PURPLE)
                        : Component.translatable("screen.enlightened.scroll.gate", a.gate.displayName(), Mastery.gateCost(a.gate)).withStyle(ChatFormatting.RED));
            } else if (a.gate != Gate.NONE && level == 0) {
                line.append(Component.literal(" ")).append(a.gate.displayName().copy().withStyle(ChatFormatting.DARK_AQUA));
            }
            if (a.kind == Ability.Kind.PASSIVE) {
                line.append(Component.literal(" ")).append(Component.translatable("screen.enlightened.scroll.passive").withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.drawString(font, line, LEFT, y, 0xFFFFFF, true);
        }
        if (abilities.size() > learnButtons.size()) {
            graphics.drawString(font, Component.translatable("screen.enlightened.scroll.more").withStyle(ChatFormatting.DARK_GRAY), LEFT, listTop + learnButtons.size() * ROW + 2, 0xFFFFFF, true);
        }
        // The selected ability's description, under the slots.
        Ability shown = selected != null ? selected : hovered(mouseX, mouseY);
        if (shown != null) {
            int y = listTop + BendingData.KEYS * 28 + 14;
            List<net.minecraft.util.FormattedCharSequence> wrapped = font.split(shown.description(), width - slotsLeft - 8);
            graphics.drawString(font, shown.displayName().copy().withStyle(ChatFormatting.YELLOW), slotsLeft, y, 0xFFFFFF, true);
            for (int i = 0; i < wrapped.size() && y + 10 + i * 9 < height - 28; i++) {
                graphics.drawString(font, wrapped.get(i), slotsLeft, y + 10 + i * 9, 0xDDDDDD, false);
            }
        }
    }

    @Nullable
    private Ability hovered(int mouseX, int mouseY) {
        if (mouseX < LEFT || mouseX > slotsLeft - 12) {
            return null;
        }
        int row = (mouseY - listTop) / ROW;
        return mouseY >= listTop && row >= 0 && row < learnButtons.size() ? rowAbility(row) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            for (int slot = 0; slot < slotButtons.size(); slot++) {
                if (slotButtons.get(slot).isMouseOver(mouseX, mouseY)) {
                    ModNetwork.sendToServer(ScrollActionPayload.assign(slot, null));
                    return true;
                }
            }
        }
        if (button == 0) {
            Ability a = hovered((int) mouseX, (int) mouseY);
            if (a != null && !learnButtonAt(mouseX, mouseY)) {
                selected = a == selected ? null : a;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean learnButtonAt(double mouseX, double mouseY) {
        for (Button b : learnButtons) {
            if (b.visible && b.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, abilities.size() - learnButtons.size());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.SCROLL.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
