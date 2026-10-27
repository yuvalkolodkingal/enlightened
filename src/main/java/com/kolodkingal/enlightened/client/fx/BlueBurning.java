package com.kolodkingal.enlightened.client.fx;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/** Which creatures burn with Azula's blue fire this moment (the server says so when its fire takes), by entity id. */
public final class BlueBurning {
    private static final Map<Integer, Long> UNTIL = new HashMap<>();

    public static void mark(int entityId, long until) {
        UNTIL.merge(entityId, until, Math::max);
    }

    public static boolean isBlue(Entity entity) {
        Long until = UNTIL.get(entity.getId());
        if (until == null) {
            return false;
        }
        if (until < entity.level().getGameTime() || !entity.isOnFire()) {
            UNTIL.remove(entity.getId());
            return false;
        }
        return true;
    }

    public static void clear() {
        UNTIL.clear();
    }

    private BlueBurning() {
    }
}
