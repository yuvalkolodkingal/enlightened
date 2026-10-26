package com.kolodkingal.enlightened.client.fx;

import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.network.FxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** The one-shot effects the server sent, kept for their life and drawn by {@link WorldFxRenderer}. */
public final class WorldFx {
    /** One effect: what, where from, where to, its colour, its number, the entity it follows (or -1), and its birth. */
    public record Effect(Fx kind, Vec3 from, Vec3 to, int color, float data, int entity, long born) {
        public float age(long now, float partial) {
            return now - born + partial;
        }

        public boolean dead(long now) {
            return now - born >= kind.life;
        }
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();

    public static void add(FxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.kind() < 0 || payload.kind() >= Fx.VALUES.length) {
            return;
        }
        EFFECTS.add(new Effect(Fx.VALUES[payload.kind()], new Vec3(payload.x(), payload.y(), payload.z()),
                new Vec3(payload.tx(), payload.ty(), payload.tz()), payload.color(), payload.data(), payload.entity(), level.getGameTime()));
    }

    public static List<Effect> all() {
        return EFFECTS;
    }

    public static void tick(ClientLevel level) {
        long now = level.getGameTime();
        EFFECTS.removeIf(e -> e.dead(now));
    }

    public static void clear() {
        EFFECTS.clear();
        WorldFxRenderer.clear();
    }

    private WorldFx() {
    }
}
