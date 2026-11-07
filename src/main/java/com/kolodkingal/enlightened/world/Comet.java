package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.network.CometPayload;
import com.kolodkingal.enlightened.network.ModNetwork;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Sozin's Comet (1.0.27): while Ozai fights, the comet burns across the sky of his world, the sky turns red, and every
 * firebender's fire and lightning strike twice as hard. Ozai keeps it alive ({@link #keep}) every second of his fight;
 * when he stops, it passes within two seconds.
 */
public final class Comet {
    private static final Map<ResourceKey<Level>, Long> UNTIL = new HashMap<>();
    private static final Map<ResourceKey<Level>, Boolean> SENT = new HashMap<>();
    /** How much harder fire strikes under the comet. */
    public static final float POWER = 2.0F;

    /** Keeps the comet in this world's sky for the next two seconds. */
    public static void keep(ServerLevel level) {
        UNTIL.put(level.dimension(), level.getGameTime() + 40);
    }

    public static boolean active(Level level) {
        Long until = UNTIL.get(level.dimension());
        return until != null && until >= level.getGameTime();
    }

    /** Every tick: tells each world's players when the comet comes and when it passes. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            boolean now = active(level);
            if (SENT.getOrDefault(level.dimension(), false) != now) {
                SENT.put(level.dimension(), now);
                for (ServerPlayer p : level.players()) {
                    ModNetwork.sendTo(p, new CometPayload(now));
                }
            }
        }
    }

    /** A player arrives in a world: he sees its sky as it is. */
    public static void join(ServerPlayer p) {
        ModNetwork.sendTo(p, new CometPayload(active(p.level())));
    }

    public static void clear() {
        UNTIL.clear();
        SENT.clear();
    }

    private Comet() {
    }
}
