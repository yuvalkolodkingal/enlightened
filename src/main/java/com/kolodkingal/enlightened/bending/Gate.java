package com.kolodkingal.enlightened.bending;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * What a sub-bending art needs before its abilities can be learned: a number of ability points already spent on the
 * element (sand, lava and metal for earth; ice and blood for water; lightning for fire: see
 * {@code EnlightenedConfig}), or, for the airbender's flight, that he let go of his earthly tethers
 * ({@link BendingData#tetherless()}).
 */
public enum Gate {
    NONE,
    SAND,
    LAVA,
    METAL,
    ICE,
    BLOOD,
    LIGHTNING,
    TETHERLESS;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("gate.enlightened." + id());
    }
}
