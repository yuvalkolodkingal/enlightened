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
import com.kolodkingal.enlightened.client.screen.ScrollUi.BigButton;
import com.kolodkingal.enlightened.network.ModNetwork;
import com.kolodkingal.enlightened.network.ScrollActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The Bending Scroll, after the Hero Creator of Enhanced: a live picture of the bender on the left, two tabs of big
 * buttons on top (Abilities and Keys), and the points bar under them.
 * <ul>
 * <li><b>Abilities</b>: chips that sort the arts (all, the basic ones, each sub-art; for the Avatar, each element), a
 * search box, a grid of cards (the art's name, its stars, what it needs) and, beside it, the chosen art in full: what it
 * does, its level, a big Learn button, and the ten keys to put it on.</li>
 * <li><b>Keys</b>: the ten keys (each plain and with Sneak) as big buttons; pick one, then the art to put there. Right
 * click empties a key.</li>
 * </ul>
 * Everything is read from the synced bending data each frame, so the screen always shows what the server knows.
 */
public class ScrollScreen extends Screen {
    private static final int TABS_Y = 16;
    private static final int BAR_Y = 38;
    private static final int TOP = 54;
    private static final int CARD_H = 24;
    private static final int GAP = 2;

    private enum Tab { ABILITIES, KEYS }

    private static Tab tab = Tab.ABILITIES;
    private static String chip = "all";
    private static String query = "";
    @Nullable
    private static Ability selected;
    private static int selectedSlot = -1;

    private int scroll;
    private boolean searching;
    private final Map<BigButton, Ability> cards = new LinkedHashMap<>();
    private final List<BigButton> keyButtons = new ArrayList<>();
    @Nullable
    private BigButton learnButton;
    private int gridLeft;
    private int gridTop;
    private int gridRight;
    private int gridBottom;
    private int columns = 1;
    private int listSize;
    private int detailX;
    private int detailW;

    public ScrollScreen() {
        super(Component.translatable("screen.enlightened.scroll"));
    }

    /** A fresh scroll on a tab, reset to its first page (the tour's pictures). */
    public static ScrollScreen tour(boolean keys) {
        tab = keys ? Tab.KEYS : Tab.ABILITIES;
        chip = "all";
        query = "";
        selected = null;
        selectedSlot = keys ? 2 : -1;
        return new ScrollScreen();
    }

    private BendingData data() {
        return Benders.get(minecraft.player);
    }

    private int previewRight() {
        return width >= 420 ? 100 : 0;
    }

    private int left() {
        return previewRight() + 6;
    }

    private int right() {
        return width - 6;
    }

    private int bottom() {
        return height - 30;
    }

    private Element element() {
        return data().element();
    }

    // ---- layout ----

    @Override
    protected void init() {
        cards.clear();
        keyButtons.clear();
        learnButton = null;
        int x0 = left();
        int w = right() - x0;
        int tabW = Math.min(110, (w - 4) / 2);
        addRenderableWidget(new BigButton(x0, TABS_Y, tabW, 18, Component.translatable("screen.enlightened.scroll.tab.abilities"), null,
                element().color, () -> switchTab(Tab.ABILITIES)).selected(tab == Tab.ABILITIES));
        addRenderableWidget(new BigButton(x0 + tabW + 4, TABS_Y, tabW, 18, Component.translatable("screen.enlightened.scroll.tab.keys"), null,
                0xFFD54F, () -> switchTab(Tab.KEYS)).selected(tab == Tab.KEYS));
        if (tab == Tab.ABILITIES) {
            initAbilities(x0, w);
        } else {
            initKeys(x0, w);
        }
        int by = height - 24;
        addRenderableWidget(new BigButton(right() - 80, by, 80, 18, Component.translatable("gui.done"), null, 0xB0B0B0, this::onClose));
        if (tab == Tab.KEYS) {
            addRenderableWidget(new BigButton(right() - 80 - 4 - 110, by, 110, 18, Component.translatable("screen.enlightened.scroll.reset_loadout"), null,
                    0xEF9A9A, () -> ModNetwork.sendToServer(ScrollActionPayload.resetLoadout())));
        }
    }

    private void switchTab(Tab to) {
        if (tab != to) {
            tab = to;
            scroll = 0;
            rebuildWidgets();
        }
    }

    private void initAbilities(int x0, int w) {
        // The chips.
        int cx = x0;
        for (Map.Entry<String, Component> c : chips().entrySet()) {
            String id = c.getKey();
            int cw = Math.min(90, font.width(c.getValue()) + 14);
            if (cx + cw > right()) {
                break;
            }
            addRenderableWidget(new BigButton(cx, TOP, cw, 16, c.getValue(), null, chipColor(id), () -> {
                if (!chip.equals(id)) {
                    chip = id;
                    scroll = 0;
                    rebuildWidgets();
                }
            }).selected(chip.equals(id)));
            cx += cw + 3;
        }
        int gridW = Math.max(120, Math.round(w * 0.52F));
        gridLeft = x0;
        gridRight = x0 + gridW;
        detailX = gridRight + 6;
        detailW = right() - detailX;
        EditBox search = addRenderableWidget(new EditBox(font, x0 + 1, TOP + 20, gridW - 2, 14, Component.translatable("screen.enlightened.scroll.search")));
        search.setHint(Component.translatable("screen.enlightened.scroll.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(32);
        search.setValue(query);
        search.setResponder(text -> {
            if (!text.equals(query)) {
                query = text;
                searching = true;
                scroll = 0;
                rebuildWidgets();
            }
        });
        if (searching) {
            setFocused(search);
            search.setFocused(true);
            search.moveCursorToEnd(false);
        }
        List<Ability> list = filtered();
        listSize = list.size();
        if (selected == null || !Ability.of(element()).contains(selected) && !data().has(selected)) {
            selected = list.isEmpty() ? null : list.get(0);
        }
        gridTop = TOP + 38;
        gridBottom = bottom();
        columns = gridW >= 200 ? 2 : 1;
        int cardW = (gridW - GAP * (columns - 1)) / columns;
        int rows = Math.max(1, (gridBottom - gridTop + GAP) / (CARD_H + GAP));
        int maxScroll = Math.max(0, (list.size() + columns - 1) / columns - rows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = scroll * columns; i < list.size() && i < (scroll + rows) * columns; i++) {
            Ability a = list.get(i);
            int k = i - scroll * columns;
            int x = gridLeft + (k % columns) * (cardW + GAP);
            int y = gridTop + (k / columns) * (CARD_H + GAP);
            BigButton card = new BigButton(x, y, cardW, CARD_H, a.displayName(), null, a.element.color, () -> {
                selected = a;
                searching = false;
            });
            card.symbol = ScrollUi.symbol(a);
            cards.put(addRenderableWidget(card), a);
        }
        // The detail panel's buttons: Learn, and the ten keys.
        if (detailW >= 90) {
            int keyW = (detailW - 8 - 4 * GAP) / BendingData.KEYS;
            int keysTop = bottom() - 4 - 2 * 16 - GAP;
            for (int s = 0; s < 2; s++) {
                for (int key = 1; key <= BendingData.KEYS; key++) {
                    int slot = BendingData.slotIndex(key, s == 1);
                    BigButton b = new BigButton(detailX + 4 + (key - 1) * (keyW + GAP), keysTop + s * (16 + GAP), keyW, 16, keyLabel(slot), null,
                            0xFFD54F, () -> {
                                if (selected != null) {
                                    ModNetwork.sendToServer(ScrollActionPayload.assign(slot, data().slot(slot) == selected ? null : selected));
                                }
                            });
                    keyButtons.add(addRenderableWidget(b));
                }
            }
            learnButton = addRenderableWidget(new BigButton(detailX + 4, keysTop - 13 - 24, detailW - 8, 22, Component.empty(), null, 0x66BB6A, () -> {
                if (selected != null) {
                    ModNetwork.sendToServer(ScrollActionPayload.learn(selected));
                }
            }));
        }
    }

    private void initKeys(int x0, int w) {
        int half = (w - 6) / 2;
        int top = TOP + 14;
        int rowH = Math.max(20, Math.min(28, (bottom() - top) / BendingData.KEYS - GAP));
        int colW = (half - GAP) / 2;
        for (int key = 1; key <= BendingData.KEYS; key++) {
            for (int s = 0; s < 2; s++) {
                int slot = BendingData.slotIndex(key, s == 1);
                BigButton b = new BigButton(x0 + s * (colW + GAP), top + (key - 1) * (rowH + GAP), colW, rowH, keyLabel(slot), null, 0xFFD54F, () -> {
                    selectedSlot = selectedSlot == slot ? -1 : slot;
                }).onRight(btn -> ModNetwork.sendToServer(ScrollActionPayload.assign(slot, null)));
                keyButtons.add(addRenderableWidget(b));
            }
        }
        // The arts that can sit on a key, to choose from.
        gridLeft = x0 + half + 6;
        gridRight = right();
        gridTop = top;
        gridBottom = bottom();
        int gridW = gridRight - gridLeft;
        columns = gridW >= 200 ? 2 : 1;
        int cardW = (gridW - GAP * (columns - 1)) / columns;
        int cardH = 20;
        List<Ability> list = new ArrayList<>();
        BendingData data = data();
        for (Ability a : Ability.values()) {
            if (a.bindable() && data.has(a)) {
                list.add(a);
            }
        }
        listSize = list.size();
        int rows = Math.max(1, (gridBottom - gridTop + GAP) / (cardH + GAP));
        int maxScroll = Math.max(0, (list.size() + columns - 1) / columns - rows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = scroll * columns; i < list.size() && i < (scroll + rows) * columns; i++) {
            Ability a = list.get(i);
            int k = i - scroll * columns;
            BigButton card = new BigButton(gridLeft + (k % columns) * (cardW + GAP), gridTop + (k / columns) * (cardH + GAP), cardW, cardH,
                    a.displayName(), null, a.element.color, () -> {
                        if (selectedSlot >= 0) {
                            ModNetwork.sendToServer(ScrollActionPayload.assign(selectedSlot, a));
                            selectedSlot = Math.min(selectedSlot + 1, BendingData.KEYS * 2 - 1);
                        }
                    });
            card.symbol = ScrollUi.symbol(a);
            cards.put(addRenderableWidget(card), a);
        }
    }

    // ---- what is listed ----

    /** The chips of the abilities tab, in order: all, the basic arts, each sub-art (the Avatar: each element). */
    private Map<String, Component> chips() {
        Map<String, Component> out = new LinkedHashMap<>();
        out.put("all", Component.translatable("screen.enlightened.scroll.chip.all"));
        Element element = element();
        List<Ability> all = Ability.of(element);
        if (element == Element.AVATAR) {
            for (Ability a : all) {
                out.putIfAbsent("element:" + a.element.getSerializedName(), a.element.displayName());
            }
        } else {
            out.put("basic", Component.translatable("screen.enlightened.scroll.chip.basic"));
            for (Ability a : all) {
                if (a.gate != Gate.NONE) {
                    out.putIfAbsent("gate:" + a.gate.id(), a.gate.displayName());
                }
            }
        }
        out.put("learned", Component.translatable("screen.enlightened.scroll.chip.learned"));
        return out;
    }

    private int chipColor(String id) {
        if (id.startsWith("element:")) {
            return Element.byName(id.substring(8)).color;
        }
        if (id.startsWith("gate:")) {
            return 0x4DD0E1;
        }
        return id.equals("learned") ? 0xFFD54F : element().color;
    }

    private List<Ability> filtered() {
        BendingData data = data();
        List<Ability> source = new ArrayList<>(Ability.of(element()));
        for (Ability a : Ability.values()) {
            // The lion turtle's gift is listed for whoever holds it.
            if (a.gift() && data.has(a) && !source.contains(a)) {
                source.add(a);
            }
        }
        Predicate<Ability> byChip = switch (chip) {
            case "basic" -> a -> a.gate == Gate.NONE;
            case "learned" -> data::has;
            case "all" -> a -> true;
            default -> chip.startsWith("gate:") ? a -> ("gate:" + a.gate.id()).equals(chip)
                    : chip.startsWith("element:") ? a -> ("element:" + a.element.getSerializedName()).equals(chip) : a -> true;
        };
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Ability> out = new ArrayList<>();
        for (Ability a : source) {
            if (!byChip.test(a)) {
                continue;
            }
            if (!q.isEmpty() && !I18n.get("ability.enlightened." + a.id()).toLowerCase(Locale.ROOT).contains(q)
                    && !I18n.get("ability.enlightened." + a.id() + ".desc").toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(a);
        }
        return out;
    }

    private Component keyLabel(int slot) {
        int key = slot / 2 + 1;
        boolean sneak = slot % 2 == 1;
        return Component.literal(sneak ? "⇧ " : "").append(KeyBindings.ability(key).getTranslatedKeyMessage());
    }

    // ---- drawing ----

    /** The card's grey line: its stars, or what it waits for. */
    private Component cardLine(BendingData data, Ability a) {
        int level = data.level(a);
        MutableComponent line = Component.literal(BendingHud.stars(level)).withStyle(ChatFormatting.GOLD);
        if (a.kind == Ability.Kind.PASSIVE) {
            line.append(Component.literal(" ")).append(Component.translatable("screen.enlightened.scroll.passive"));
        }
        if (level == 0 && !Mastery.gateOpen(data, a)) {
            line.append(Component.literal(" ")).append(Component.translatable("screen.enlightened.scroll.locked").withStyle(ChatFormatting.RED));
        } else if (level < Ability.MAX_LEVEL && !a.gift()) {
            line.append(Component.translatable("screen.enlightened.scroll.cost", Ability.cost(level + 1)));
        }
        int slot = data.slotOf(a);
        if (slot >= 0) {
            line.append(Component.literal(" [").append(keyLabel(slot)).append("]").withStyle(ChatFormatting.YELLOW));
        }
        return line;
    }

    private void refreshButtons(BendingData data) {
        for (Map.Entry<BigButton, Ability> e : cards.entrySet()) {
            BigButton b = e.getKey();
            Ability a = e.getValue();
            b.sub = cardLine(data, a);
            b.selected = tab == Tab.ABILITIES && a == selected;
            boolean dim = data.level(a) == 0 && !Mastery.gateOpen(data, a);
            b.color = dim ? 0x707070 : a.element.color;
            if (tab == Tab.KEYS) {
                b.active = selectedSlot >= 0;
                b.selected = selectedSlot >= 0 && data.slot(selectedSlot) == a;
            }
        }
        for (int i = 0; i < keyButtons.size(); i++) {
            BigButton b = keyButtons.get(i);
            int slot = tab == Tab.KEYS ? BendingData.slotIndex(i / 2 + 1, i % 2 == 1) : BendingData.slotIndex(i % BendingData.KEYS + 1, i >= BendingData.KEYS);
            Ability on = data.slot(slot);
            boolean filled = on != null && data.has(on);
            if (tab == Tab.KEYS) {
                b.sub = filled ? on.displayName() : Component.translatable("screen.enlightened.scroll.keys.empty");
                b.symbol = filled ? ScrollUi.symbol(on) : null;
                b.color = filled ? on.element.color : 0xFFD54F;
                b.selected = slot == selectedSlot;
            } else {
                b.active = selected != null && selected.bindable() && data.has(selected);
                b.selected = selected != null && on == selected;
            }
        }
        if (learnButton != null) {
            Ability a = selected;
            learnButton.visible = a != null;
            if (a != null) {
                int level = data.level(a);
                boolean can = Mastery.canLearn(data, a);
                learnButton.active = can;
                if (level >= Ability.MAX_LEVEL) {
                    learnButton.setMessage(Component.translatable("screen.enlightened.scroll.mastered"));
                    learnButton.sub = null;
                } else {
                    learnButton.setMessage(Component.translatable(level == 0 ? "screen.enlightened.scroll.learn" : "screen.enlightened.scroll.learn_level",
                            level + 1));
                    String why = Mastery.refusal(data, a);
                    learnButton.sub = can ? Component.translatable("screen.enlightened.scroll.learn.cost", Ability.cost(level + 1), data.points())
                            : Component.translatable(why, a.displayName(), Mastery.gateCost(a.gate));
                }
                learnButton.color = level >= Ability.MAX_LEVEL ? 0xFFD54F : 0x66BB6A;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        BendingData data = data();
        refreshButtons(data);
        super.render(g, mouseX, mouseY, partialTick);
        Element element = data.element();
        // The title, over the tabs.
        Component title = element.awakeningTitle().copy().append(" ").append(Variants.current(data).displayName())
                .withStyle(s -> s.withColor(element.color).withBold(true));
        ScrollUi.drawFitted(g, font, title, left(), 4, right() - left(), 0xFFFFFF);
        renderPointsBar(g, data);
        if (previewRight() > 0) {
            renderPreview(g, data, mouseX, mouseY);
        }
        if (tab == Tab.ABILITIES) {
            renderDetail(g, data);
            if (cards.isEmpty()) {
                g.drawString(font, Component.translatable("screen.enlightened.scroll.none").withStyle(ChatFormatting.GRAY), gridLeft + 4, gridTop + 6, 0xFFFFFF, true);
            }
        } else {
            g.drawString(font, Component.translatable("screen.enlightened.scroll.keys.help").withStyle(ChatFormatting.GRAY), left(), TOP, 0xFFFFFF, true);
            if (cards.isEmpty()) {
                g.drawString(font, Component.translatable("screen.enlightened.scroll.keys.nothing").withStyle(ChatFormatting.GRAY), gridLeft + 4, gridTop + 6, 0xFFFFFF, true);
            }
        }
        renderScrollBar(g);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        // A wash of the element's colour from the top, as the Hero Creator's backdrop.
        int c = element().color;
        g.fillGradient(0, 0, width, height / 2, 0x50000000 | ScrollUi.darken(c), 0x00000000);
    }

    /** The points bar: points to spend in gold, the share already spent, and the way to the next point. */
    private void renderPointsBar(GuiGraphics g, BendingData data) {
        int x0 = left();
        int right = right();
        int points = data.points();
        int spent = data.spent();
        Component text = Component.translatable("screen.enlightened.scroll.points_bar", points).withStyle(s -> s.withColor(points > 0 ? 0xFFD54F : 0xB0B0B0).withBold(true));
        int tw = Math.min(font.width(text), (right - x0) / 3);
        ScrollUi.drawFitted(g, font, text, x0, BAR_Y + 3, tw, 0xFFFFFF);
        Component spentText = Component.translatable("screen.enlightened.scroll.spent", spent).withStyle(ChatFormatting.GRAY);
        int sw = Math.min(font.width(spentText), 80);
        ScrollUi.drawFitted(g, font, spentText, right - sw, BAR_Y + 3, sw, 0xFFFFFF);
        int bx = x0 + tw + 8;
        int bw = right - sw - 8 - bx;
        if (bw > 10) {
            float f = spent + points == 0 ? 0F : spent / (float) (spent + points);
            g.fill(bx, BAR_Y + 1, bx + bw, BAR_Y + 9, 0xFF202020);
            g.fill(bx, BAR_Y + 1, bx + Math.round(bw * f), BAR_Y + 9, 0xFF000000 | data.element().color);
            g.fill(bx + Math.round(bw * f), BAR_Y + 1, bx + bw, BAR_Y + 9, points > 0 ? 0xFF6D5A1E : 0xFF202020);
            g.renderOutline(bx - 1, BAR_Y, bw + 2, 10, 0xFF000000 | ScrollUi.darken(data.element().color));
            // The way to the next point, a thin line under the bar.
            float xp = data.xp() / (float) Mastery.xpPerPoint();
            g.fill(bx, BAR_Y + 11, bx + bw, BAR_Y + 13, 0xFF202020);
            g.fill(bx, BAR_Y + 11, bx + Math.round(bw * xp), BAR_Y + 13, 0xFF7CB342);
        }
    }

    /** The live picture of the bender, who follows the mouse with his eyes; under it his element and arts. */
    private void renderPreview(GuiGraphics g, BendingData data, int mouseX, int mouseY) {
        int l = previewRight();
        int top = TABS_Y;
        int bottom = bottom();
        ScrollUi.panel(g, 4, top, l - 4, bottom - top, data.element().color);
        int figureBottom = bottom - 34;
        int scale = Math.max(20, Math.min(48, (figureBottom - top - 10) / 2 - 4));
        // The bender acts out the chosen art (the Animator moves the picture on with the real frame).
        com.kolodkingal.enlightened.client.anim.Animator.previewPartial = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, 6, top + 4, l - 2, figureBottom, scale, 0.0625F, mouseX, mouseY, minecraft.player);
        } finally {
            com.kolodkingal.enlightened.client.anim.Animator.previewPartial = -1.0F;
        }
        if (shownArt() != null) {
            Ability shown = shownArt();
            int sz = 16;
            ScrollUi.drawSymbol(g, ScrollUi.symbol(shown), l - sz - 4, top + 4, sz);
        }
        Component name = minecraft.player.getName().copy().withStyle(s -> s.withColor(data.element().color).withBold(true));
        int w = Math.min(font.width(name), l - 12);
        ScrollUi.drawFitted(g, font, name, 4 + (l - 4 - w) / 2, bottom - 30, l - 12, 0xFFFFFF);
        int learned = 0;
        int mastered = 0;
        for (Ability a : Ability.values()) {
            if (data.has(a)) {
                learned++;
                if (data.level(a) >= Ability.MAX_LEVEL) {
                    mastered++;
                }
            }
        }
        Component arts = Component.translatable("screen.enlightened.scroll.arts", learned, mastered).withStyle(ChatFormatting.GRAY);
        int aw = Math.min(font.width(arts), l - 12);
        ScrollUi.drawFitted(g, font, arts, 4 + (l - 4 - aw) / 2, bottom - 18, l - 12, 0xFFFFFF);
    }

    /** The chosen art in full: its name, stars, kind, what it needs, and what it does. */
    private void renderDetail(GuiGraphics g, BendingData data) {
        if (detailW < 90) {
            return;
        }
        int top = TOP + 20;
        int bottom = bottom();
        Ability a = selected;
        ScrollUi.panel(g, detailX, top, detailW, bottom - top, a != null ? a.element.color : element().color);
        if (a == null) {
            return;
        }
        int x = detailX + 6;
        int room = detailW - 12;
        int y = top + 6;
        // The symbol, big, beside the name; it breathes a little.
        int size = 28;
        float pulse = 1.0F + 0.04F * net.minecraft.util.Mth.sin((minecraft.player.tickCount + minecraft.getTimer().getGameTimeDeltaPartialTick(false)) * 0.15F);
        g.pose().pushPose();
        g.pose().translate(x + size / 2.0F, y + size / 2.0F, 0);
        g.pose().scale(pulse, pulse, 1);
        ScrollUi.drawSymbol(g, ScrollUi.symbol(a), -size / 2, -size / 2, size);
        g.pose().popPose();
        ScrollUi.drawBig(g, font, a.displayName().copy().withStyle(s -> s.withColor(a.element.color).withBold(true)), x + size + 5, y + 1, 1.5F, room - size - 5);
        y += 15;
        int lineX = x + size + 5;
        int lineRoom = room - size - 5;
        int level = data.level(a);
        MutableComponent line = Component.literal(BendingHud.stars(level) + " ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("screen.enlightened.scroll.level", level, Ability.MAX_LEVEL).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  "))
                .append(Component.translatable("screen.enlightened.scroll.kind." + a.kind.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.AQUA));
        ScrollUi.drawFitted(g, font, line, lineX, y, lineRoom, 0xFFFFFF);
        y = Math.max(y + 11, top + 6 + size + 3);
        if (a.gate != Gate.NONE) {
            boolean open = Mastery.gateOpen(data, a);
            Component gate = a.gate == Gate.TETHERLESS ? Component.translatable("screen.enlightened.scroll.tethered")
                    : a.gate == Gate.CHAKRAS ? Component.translatable("screen.enlightened.scroll.chakras")
                    : Component.translatable("screen.enlightened.scroll.gate", a.gate.displayName(), Mastery.gateCost(a.gate));
            ScrollUi.drawFitted(g, font, (open ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN) : Component.literal("✖ ").withStyle(ChatFormatting.RED))
                    .append(gate.copy().withStyle(open ? ChatFormatting.DARK_AQUA : ChatFormatting.RED)), x, y, room, 0xFFFFFF);
            y += 11;
        }
        y += 3;
        int textBottom = learnButton != null ? learnButton.getY() - 4 : bottom - 4;
        // The description at its size if it fits, else smaller (never cut short).
        float scale = 1.0F;
        List<FormattedCharSequence> wrapped = font.split(a.description(), room);
        while (scale > 0.5F && y + wrapped.size() * 10 * scale > textBottom) {
            scale -= 0.125F;
            wrapped = font.split(a.description(), Math.round(room / scale));
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        for (int i = 0; i < wrapped.size() && y + (i + 1) * 10 * scale <= textBottom + 2; i++) {
            g.drawString(font, wrapped.get(i), 0, i * 10, 0xDDDDDD, false);
        }
        g.pose().popPose();
        if (!keyButtons.isEmpty()) {
            Component keys = Component.translatable(a.bindable() ? "screen.enlightened.scroll.put_on_key" : "screen.enlightened.scroll.passive_key")
                    .withStyle(ChatFormatting.YELLOW);
            ScrollUi.drawFitted(g, font, keys, x - 2, keyButtons.get(0).getY() - 11, room, 0xFFFFFF);
        }
    }

    private void renderScrollBar(GuiGraphics g) {
        int rowH = (tab == Tab.KEYS ? 20 : CARD_H) + GAP;
        int rows = Math.max(1, (gridBottom - gridTop + GAP) / rowH);
        int total = (listSize + columns - 1) / columns;
        if (total <= rows) {
            return;
        }
        int x = gridRight + 2;
        int h = gridBottom - gridTop;
        int knob = Math.max(10, h * rows / total);
        int y = gridTop + (h - knob) * scroll / Math.max(1, total - rows);
        g.fill(x, gridTop, x + 2, gridBottom, 0x40FFFFFF);
        g.fill(x, y, x + 2, y + knob, 0xC0FFFFFF);
    }

    // ---- the picture acts out the art ----

    @Nullable
    private Ability previewing;
    private int previewWait;
    private int previewStep;

    /** The art the picture shows: the chosen card, or the chosen key's art. */
    @Nullable
    private Ability shownArt() {
        if (tab == Tab.ABILITIES) {
            return selected;
        }
        if (selectedSlot >= 0) {
            Ability on = data().slot(selectedSlot);
            return on != null && data().has(on) ? on : null;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        Ability a = shownArt();
        if (a != previewing) {
            previewing = a;
            previewStep = 0;
            previewWait = 4;
        }
        if (a == null || previewRight() == 0 || --previewWait > 0) {
            return;
        }
        List<com.kolodkingal.enlightened.client.anim.Gesture> steps = com.kolodkingal.enlightened.client.anim.Animator.preview(a);
        com.kolodkingal.enlightened.client.anim.Gesture g = steps.get(previewStep % steps.size());
        com.kolodkingal.enlightened.client.anim.Animator.playPreview(minecraft.player, g);
        previewStep++;
        // The next gesture of the art when this one ends; a rest before it all begins again.
        previewWait = Math.round(g.dur) + (previewStep % steps.size() == 0 ? 26 : 3);
    }

    // ---- input ----

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= gridLeft && mouseX <= gridRight + 4 && mouseY >= gridTop && mouseY <= gridBottom) {
            int before = scroll;
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
            if (scroll != before) {
                searching = false;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!(getFocused() instanceof EditBox)) {
            searching = false;
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.SCROLL.matches(keyCode, scanCode) && !(getFocused() instanceof EditBox)) {
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
