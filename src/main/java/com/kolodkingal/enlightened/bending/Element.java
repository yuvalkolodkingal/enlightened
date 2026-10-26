package com.kolodkingal.enlightened.bending;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The bending art a player has. A player bends exactly one element at a time (an operator command can change it).
 * Each element has a colour (the main colour of its energy and its HUD title) and a glow (the pale heart of it).
 */
public enum Element implements StringRepresentable {
    NONE(0xFFFFFF, 0xFFFFFF),
    /** The Earth Kingdom's green and the brown of rock. */
    EARTH(0x6B8E23, 0xC9A96E),
    /** The Air Nomads' saffron and the pale blue-white of a gust. */
    AIR(0xE8C36A, 0xDDF3FF),
    /** The Water Tribes' blue, with the white of ice at its heart. */
    WATER(0x2E86DE, 0xBFE8FF),
    /** The Fire Nation's red, with gold at its heart. */
    FIRE(0xE2531A, 0xFFD65A);

    public static final Codec<Element> CODEC = StringRepresentable.fromEnum(Element::values);

    public final int color;
    public final int glow;

    Element(int color, int glow) {
        this.color = color;
        this.glow = glow;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Whether the player bends anything. */
    public boolean bends() {
        return this != NONE;
    }

    public Component displayName() {
        return Component.translatable("element.enlightened." + getSerializedName());
    }

    /** The title shown when a player awakens as a bender of this element ("Earthbender"). */
    public Component awakeningTitle() {
        return Component.translatable("element.enlightened." + getSerializedName() + ".title");
    }

    public static Element byName(String name) {
        for (Element e : values()) {
            if (e.getSerializedName().equals(name)) {
                return e;
            }
        }
        return NONE;
    }
}
