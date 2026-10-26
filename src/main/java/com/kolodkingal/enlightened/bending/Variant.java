package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One look of a bender: a character of the Avatar world whose skin the bender wears (Toph, Zuko, Katara...), or
 * "your own skin". A look changes only how the bender looks and the colour of his energy; the abilities are the same
 * in every look, with one exception: Azula's fire is blue.
 *
 * @param id      the look's id, unique within the element; "" is never used (the first look is the default)
 * @param element the element the look belongs to
 * @param skin    the skin texture, or null for the player's own skin
 * @param slim    whether the skin is drawn on the slim (Alex) model
 * @param color   the main energy colour in this look
 * @param glow    the pale heart of the energy in this look
 */
public record Variant(String id, Element element, @Nullable ResourceLocation skin, boolean slim, int color, int glow) {

    public static final String OWN = "own";

    public Component displayName() {
        return Component.translatable("look.enlightened." + element.getSerializedName() + "." + id);
    }

    public boolean ownSkin() {
        return skin == null;
    }

    public static ResourceLocation suit(String name) {
        return Enlightened.id("textures/entity/suit/" + name + ".png");
    }
}
