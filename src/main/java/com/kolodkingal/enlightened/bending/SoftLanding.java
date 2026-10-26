package com.kolodkingal.enlightened.bending;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Players who may fall without harm for a while: after a catapult, a jet, a step, a leap. */
public final class SoftLanding {
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    public static void grant(ServerPlayer player, int ticks) {
        long until = player.level().getGameTime() + ticks;
        UNTIL.merge(player.getUUID(), until, Math::max);
    }

    public static boolean active(ServerPlayer player) {
        Long until = UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() > until) {
            UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void forget(ServerPlayer player) {
        UNTIL.remove(player.getUUID());
    }

    public static void clear() {
        UNTIL.clear();
    }

    private SoftLanding() {
    }
}
