package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.air.AirBending;
import com.kolodkingal.enlightened.earth.EarthBending;
import com.kolodkingal.enlightened.fire.FireBending;
import com.kolodkingal.enlightened.water.WaterBending;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** The registry of every ability's server logic, filled once by each element when the mod loads. */
public final class Abilities {
    private static final Map<Ability, AbilityLogic> LOGIC = new EnumMap<>(Ability.class);

    static {
        EarthBending.register(LOGIC);
        AirBending.register(LOGIC);
        WaterBending.register(LOGIC);
        FireBending.register(LOGIC);
    }

    @Nullable
    public static AbilityLogic of(Ability ability) {
        return LOGIC.get(ability);
    }

    public static Collection<AbilityLogic> all() {
        return LOGIC.values();
    }

    /** The server stopped: every ability forgets its statics. */
    public static void clearAll() {
        for (AbilityLogic logic : LOGIC.values()) {
            logic.clear();
        }
        Combos.clear();
    }

    public static void forgetAll(ServerPlayer player) {
        for (AbilityLogic logic : LOGIC.values()) {
            logic.forget(player);
        }
        Combos.forget(player);
    }

    private Abilities() {
    }
}
