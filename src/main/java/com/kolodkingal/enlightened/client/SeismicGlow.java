package com.kolodkingal.enlightened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The creatures an earthbender's seismic sense feels: the server names them every half second and the client
 * outlines them (through walls, as the glowing effect does) for a little longer than that, so the outline never
 * flickers. Client only.
 */
public final class SeismicGlow {
    private static final Map<Integer, Long> UNTIL = new HashMap<>();

    public static void show(List<Integer> ids, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long until = mc.level.getGameTime() + ticks;
        for (int id : ids) {
            UNTIL.put(id, until);
        }
    }

    /** Whether this entity is outlined by the local player's seismic sense right now. */
    public static boolean glows(Entity entity) {
        if (UNTIL.isEmpty() || !entity.level().isClientSide) {
            return false;
        }
        Long until = UNTIL.get(entity.getId());
        if (until == null) {
            return false;
        }
        long now = entity.level().getGameTime();
        if (now > until) {
            UNTIL.remove(entity.getId());
            return false;
        }
        return true;
    }

    public static void clear() {
        UNTIL.clear();
    }

    private SeismicGlow() {
    }
}
