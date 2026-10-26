package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.client.screen.ScrollScreen;
import net.minecraft.client.Minecraft;

/** Opens the Bending Scroll screen (the K key, or the scroll item in the hand of a bender). Client only. */
public final class ClientScroll {
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
            mc.setScreen(new ScrollScreen());
        }
    }

    private ClientScroll() {
    }
}
