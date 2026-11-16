package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Locale;

/**
 * (1.0.39) The official server, put on the player's Multiplayer list the first time the game reaches its title screen
 * with the mod: appended at the end, unless an entry with its address is already there, and never touching the player's
 * other servers. A flag in the client config ({@code official_server.addedOfficialServer}) makes it once only, so an entry
 * the player deletes never comes back; {@code officialServer = false} turns it off. Registered on the client only.
 */
public final class OfficialServer {
    public static final String NAME = "Enlightened Avatar SMP";
    public static final String ADDRESS = "enlightened.exaroton.me";

    private static boolean checked;

    private OfficialServer() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (checked) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TitleScreen) || !EnlightenedClientConfig.SPEC.isLoaded()) {
            return;
        }
        checked = true;
        if (!EnlightenedClientConfig.OFFICIAL_SERVER.get() || EnlightenedClientConfig.ADDED_OFFICIAL_SERVER.get()) {
            return;
        }
        try {
            ensure(new ServerList(mc));
            EnlightenedClientConfig.ADDED_OFFICIAL_SERVER.set(true);
            EnlightenedClientConfig.SPEC.save();
        } catch (RuntimeException e) {
            Enlightened.LOGGER.warn("Could not add the official server to the server list", e);
        }
    }

    /** Loads the list, appends the official server unless its address is there, and saves. True when it was added. */
    public static boolean ensure(ServerList list) {
        list.load();
        for (int i = 0; i < list.size(); i++) {
            ServerData entry = list.get(i);
            if (entry != null && entry.ip != null && entry.ip.trim().toLowerCase(Locale.ROOT).equals(ADDRESS)) {
                Enlightened.LOGGER.info("The official server is on the server list already");
                return false;
            }
        }
        list.add(new ServerData(NAME, ADDRESS, ServerData.Type.OTHER), false);
        list.save();
        Enlightened.LOGGER.info("Added the official server ({}) to the server list", ADDRESS);
        return true;
    }
}
