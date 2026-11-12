package com.kolodkingal.enlightened.client.dev;

import com.kolodkingal.enlightened.Enlightened;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 26.3: the screenshot tour (the 1.21.1 copy's {@code client.dev.Tour}) is not part of this copy: it drives the client
 * through APIs that 26.1 rebuilt. Asking for it only says so, once.
 */
public final class Tour {
    private static boolean told;

    private Tour() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!told && Boolean.getBoolean("enlightened.tour")) {
            told = true;
            Enlightened.LOGGER.warn("The tour is not available on Minecraft 26.3");
        }
    }
}
