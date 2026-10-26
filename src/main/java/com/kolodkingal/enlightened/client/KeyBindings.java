package com.kolodkingal.enlightened.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Ability keys 1-5 (default R, G, V, B, N): each triggers the ability on that key of the bender's loadout, and with
 * Sneak the ability on the key's Sneak slot. The Change Look key (default H) cycles through the element's looks
 * (Sneak + H goes back). The Scroll key (default K) opens the Bending Scroll: the abilities, the points and the loadout.
 */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.enlightened";

    public static final KeyMapping[] ABILITIES = {
            create(1, GLFW.GLFW_KEY_R),
            create(2, GLFW.GLFW_KEY_G),
            create(3, GLFW.GLFW_KEY_V),
            create(4, GLFW.GLFW_KEY_B),
            create(5, GLFW.GLFW_KEY_N),
    };

    public static final KeyMapping CHANGE_LOOK = new KeyMapping("key.enlightened.change_look", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping SCROLL = new KeyMapping("key.enlightened.scroll", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    private static KeyMapping create(int ability, int key) {
        return new KeyMapping("key.enlightened.ability_" + ability, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    public static KeyMapping ability(int ability) {
        return ABILITIES[ability - 1];
    }

    private KeyBindings() {
    }
}
