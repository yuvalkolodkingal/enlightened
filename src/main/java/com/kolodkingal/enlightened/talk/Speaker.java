package com.kolodkingal.enlightened.talk;

import java.util.Locale;

/**
 * Who speaks in a talk (1.0.23): the key of the dialogue table ({@link Dialogue}), the name and colour on the talk
 * screen, and the portrait (a face cut from the speaker's 64x64 skin, or, for the great beings, an ability's symbol).
 */
public enum Speaker {
    PATHIK("entity.enlightened.guru_pathik", 0xFFC14D, null),
    BUMI("entity.enlightened.king_bumi", 0x7FBF4A, null),
    GUARD_OMASHU("entity.enlightened.city_guard.omashu", 0x8FC46A, null),
    GUARD_BA_SING_SE("entity.enlightened.city_guard.ba_sing_se", 0x8FC46A, null),
    GUARD_ELLORA("entity.enlightened.city_guard.ellora", 0xF2C75C, null),
    SUN_CHIEF("entity.enlightened.sun_warrior.chief", 0xFF9A3C, null),
    SUN_DANCER("entity.enlightened.sun_warrior.dancer", 0xFF9A3C, null),
    SUN_WARRIOR("entity.enlightened.sun_warrior.warrior", 0xFF9A3C, null),
    LION_TURTLE("entity.enlightened.lion_turtle", 0x9CD67A, "energybending"),
    RAAVA("entity.enlightened.great_spirit.raava", 0x9FE8FF, "spiritbending"),
    IROH("entity.enlightened.iroh", 0xE0A030, null),
    ZUKO("entity.enlightened.zuko", 0xD94A2A, null),
    FIRE_HERALD("entity.enlightened.fire_herald", 0xC23B22, null),
    OZAI("entity.enlightened.ozai", 0xB01E10, null);

    public final String nameKey;
    public final int color;
    /** The ability whose symbol is the portrait, or null: a face from the skin. */
    public final String symbol;

    Speaker(String nameKey, int color, String symbol) {
        this.nameKey = nameKey;
        this.color = color;
        this.symbol = symbol;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
