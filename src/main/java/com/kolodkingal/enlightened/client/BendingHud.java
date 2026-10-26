package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.air.AirBending;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.Variant;
import com.kolodkingal.enlightened.bending.Variants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Top-left panel: the element and the look, then one line per key with the ability on it (and, after a slash, the
 * one on its Sneak), each with its level in stars and its state (a cooldown, ON or OFF, "active", the water carried),
 * then the points and the experience toward the next.
 */
public class BendingHud implements LayeredDraw.Layer {
    private static int shownWidth;
    private static long shownUntil;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || player.isDeadOrDying()) {
            return;
        }
        BendingData data = Benders.get(player);
        Element element = data.element();
        if (!element.bends()) {
            return;
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        Variant look = Variants.current(data);
        MutableComponent title = element.awakeningTitle().copy().withStyle(s -> s.withColor(look.color()).withBold(true));
        if (Variants.hasChoice(element)) {
            title.append(Component.literal(" ").withStyle(s -> s.withBold(false)))
                    .append(Component.literal("[").append(KeyBindings.CHANGE_LOOK.getTranslatedKeyMessage()).append("] ")
                            .withStyle(s -> s.withBold(false).withColor(ChatFormatting.YELLOW)))
                    .append(look.displayName().copy().withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY)));
        }

        List<Component> lines = new ArrayList<>();
        for (int key = 1; key <= BendingData.KEYS; key++) {
            MutableComponent line = Component.literal("[").append(KeyBindings.ability(key).getTranslatedKeyMessage()).append("] ")
                    .withStyle(ChatFormatting.YELLOW);
            line.append(slot(player, data, BendingData.slotIndex(key, false), now));
            line.append(Component.literal("  /  ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(Component.literal("⇧ ").withStyle(ChatFormatting.GOLD));
            line.append(slot(player, data, BendingData.slotIndex(key, true), now));
            lines.add(line);
        }
        MutableComponent points = Component.translatable("hud.enlightened.points", data.points()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal("  "))
                .append(Component.translatable("hud.enlightened.xp", data.xp(), Mastery.xpPerPoint()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  "))
                .append(Component.literal("[").append(KeyBindings.SCROLL.getTranslatedKeyMessage()).append("] ").withStyle(ChatFormatting.YELLOW))
                .append(Component.translatable("hud.enlightened.scroll").withStyle(ChatFormatting.WHITE));
        lines.add(points);
        if (element == Element.WATER) {
            lines.add(Component.translatable("hud.enlightened.water", (int) data.water()).withStyle(s -> s.withColor(Element.WATER.color)));
        }
        if (data.suppressed(now)) {
            lines.add(Component.translatable("hud.enlightened.suppressed", (data.suppressedUntil() - now) / 20).withStyle(ChatFormatting.RED));
        }
        if (element == Element.AIR && data.tetherless()) {
            lines.add(Component.translatable("hud.enlightened.tetherless").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        int width = font.width(title) + 4;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        if (width >= shownWidth || now > shownUntil || now < shownUntil - 20) {
            if (width != shownWidth) {
                shownUntil = now + 20;
            }
            shownWidth = width;
        }
        width = shownWidth;
        int x = 4;
        int y = 4;
        int height = 12 + lines.size() * 10;
        graphics.fill(x - 2, y - 2, x + width + 4, y + height, 0x70000000);
        graphics.drawString(font, title, x, y, 0xFFFFFFFF, true);
        y += 12;
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }

    private static Component slot(LocalPlayer player, BendingData data, int slot, long now) {
        Ability a = data.slot(slot);
        if (a == null || !data.has(a)) {
            return Component.literal("-").withStyle(ChatFormatting.DARK_GRAY);
        }
        MutableComponent line = a.displayName().copy().withStyle(ChatFormatting.WHITE);
        line.append(Component.literal(" " + stars(data.level(a))).withStyle(ChatFormatting.GOLD));
        Component status = status(player, data, a, now);
        if (status != null) {
            line.append(Component.literal(" ")).append(status);
        }
        return line;
    }

    public static String stars(int level) {
        return "★".repeat(Math.max(0, level)) + "☆".repeat(Math.max(0, Ability.MAX_LEVEL - level));
    }

    private static Component status(LocalPlayer player, BendingData data, Ability a, long now) {
        long left = data.cooldownUntil(a) - now;
        if (left > 0 && !data.active(a) && !(a.kind == Ability.Kind.TOGGLE && data.on(a))) {
            return Component.translatable("hud.enlightened.seconds", String.format(Locale.ROOT, "%.1f", left / 20.0)).withStyle(ChatFormatting.GRAY);
        }
        if (a == Ability.AIR_SHIELD && data.on(a)) {
            float strength = AirBending.shieldStrength(data);
            return Component.translatable("hud.enlightened.shield", (int) Math.ceil(Math.min(strength, shieldLeftGuess(player, data))), (int) strength)
                    .withStyle(ChatFormatting.GREEN);
        }
        if (a == Ability.FLIGHT && data.on(a)) {
            return onOff(data.flying());
        }
        if (a.kind == Ability.Kind.TOGGLE) {
            return onOff(data.on(a));
        }
        if (data.active(a)) {
            String key = switch (a) {
                case ROCK_LIFT -> "hud.enlightened.holding";
                case BLOODBENDING -> "hud.enlightened.holding";
                case AIR_JUMP -> "hud.enlightened.gliding";
                case LIGHTNING -> "hud.enlightened.charging";
                case SPIRIT_PROJECTION -> "hud.enlightened.projecting";
                case FIRE_BALL -> "hud.enlightened.charging";
                default -> "hud.enlightened.active";
            };
            return Component.translatable(key).withStyle(ChatFormatting.AQUA);
        }
        return null;
    }

    /** The client does not know the shield's wear; it shows the shield's full strength while it is up. */
    private static float shieldLeftGuess(LocalPlayer player, BendingData data) {
        return AirBending.shieldStrength(data);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enlightened.on" : "hud.enlightened.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }
}
