package com.kolodkingal.enlightened.bending;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every bending ability of the mod. Each belongs to one element and, for sub-bending, one {@link Gate} that must be
 * open before it can be learned. An ability has up to three levels (novice, adept, master): level {@code n} costs
 * {@code n} ability points, and most numbers (size, range, damage, count) grow with the level.
 *
 * <p>{@link Kind}: a {@code PRESS} ability acts when its key is pressed; a {@code HOLD} one acts while the key is
 * held; a {@code TOGGLE} one is switched on and off by its key; a {@code PASSIVE} one is never on a key at all.
 *
 * <p>The order of the constants is saved nowhere (saves use the name), but the stream codec sends ordinals, so a
 * client and a server must have the same list.
 */
public enum Ability {
    // ---- Earth ----
    ROCK_THROW(Element.EARTH, Gate.NONE, Kind.PRESS, true),
    ROCK_LIFT(Element.EARTH, Gate.NONE, Kind.PRESS, false),
    EARTH_ARMOR(Element.EARTH, Gate.NONE, Kind.TOGGLE, false),
    EARTH_PILLAR(Element.EARTH, Gate.NONE, Kind.PRESS, false),
    EARTH_WALL(Element.EARTH, Gate.NONE, Kind.PRESS, false),
    SEISMIC_SENSE(Element.EARTH, Gate.NONE, Kind.TOGGLE, false),
    SAND_WALL(Element.EARTH, Gate.SAND, Kind.PRESS, false),
    SAND_LIFT(Element.EARTH, Gate.SAND, Kind.PRESS, false),
    QUICKSAND(Element.EARTH, Gate.SAND, Kind.PRESS, false),
    LAVA_PATH(Element.EARTH, Gate.LAVA, Kind.PRESS, false),
    LAVA_SHURIKEN(Element.EARTH, Gate.LAVA, Kind.PRESS, false),
    LAVA_SKIN(Element.EARTH, Gate.LAVA, Kind.PASSIVE, false),
    METAL_WALL(Element.EARTH, Gate.METAL, Kind.PRESS, false),
    METAL_BLADES(Element.EARTH, Gate.METAL, Kind.PRESS, false),
    METAL_ARMOR(Element.EARTH, Gate.METAL, Kind.TOGGLE, false),
    METAL_PULL(Element.EARTH, Gate.METAL, Kind.PRESS, false),
    WRECK(Element.EARTH, Gate.METAL, Kind.PRESS, false),
    SHOCKWAVE(Element.EARTH, Gate.NONE, Kind.PRESS, false),
    CATAPULT(Element.EARTH, Gate.NONE, Kind.PRESS, false),
    EARTH_GRAB(Element.EARTH, Gate.NONE, Kind.PRESS, false),
    EARTH_TUNNEL(Element.EARTH, Gate.NONE, Kind.HOLD, false),
    DENSITY_SHIFT(Element.EARTH, Gate.NONE, Kind.PASSIVE, false),
    EARTH_STEP(Element.EARTH, Gate.NONE, Kind.PASSIVE, false),
    EXTRACTION(Element.EARTH, Gate.METAL, Kind.PRESS, false),

    // ---- Air ----
    AIR_BLAST(Element.AIR, Gate.NONE, Kind.PRESS, true),
    AIR_SHIELD(Element.AIR, Gate.NONE, Kind.TOGGLE, false),
    AIR_JUMP(Element.AIR, Gate.NONE, Kind.PRESS, false),
    AIR_BLADES(Element.AIR, Gate.NONE, Kind.PRESS, false),
    SPIRIT_PROJECTION(Element.AIR, Gate.NONE, Kind.PRESS, false),
    AIR_BUBBLE(Element.AIR, Gate.NONE, Kind.PASSIVE, false),
    AIR_SCOOTER(Element.AIR, Gate.NONE, Kind.TOGGLE, false),
    TORNADO(Element.AIR, Gate.NONE, Kind.PRESS, false),
    FLIGHT(Element.AIR, Gate.TETHERLESS, Kind.TOGGLE, false),
    SUFFOCATE(Element.AIR, Gate.TETHERLESS, Kind.HOLD, false),
    AIR_SUCTION(Element.AIR, Gate.NONE, Kind.PRESS, false),
    AIR_BURST(Element.AIR, Gate.NONE, Kind.PRESS, false),
    AIR_SPOUT(Element.AIR, Gate.NONE, Kind.TOGGLE, false),
    AIR_AGILITY(Element.AIR, Gate.NONE, Kind.PASSIVE, false),
    AIR_STEP(Element.AIR, Gate.NONE, Kind.PASSIVE, false),

    // ---- Water ----
    WATER_WHIP(Element.WATER, Gate.NONE, Kind.PRESS, true),
    WATER_HEALING(Element.WATER, Gate.NONE, Kind.PRESS, false),
    OCTOPUS_FORM(Element.WATER, Gate.NONE, Kind.TOGGLE, false),
    WATER_SPOUT(Element.WATER, Gate.NONE, Kind.TOGGLE, false),
    WATER_ARMS(Element.WATER, Gate.NONE, Kind.TOGGLE, false),
    PLANT_WATER(Element.WATER, Gate.NONE, Kind.PASSIVE, false),
    ICE_KNIVES(Element.WATER, Gate.ICE, Kind.PRESS, false),
    FREEZE(Element.WATER, Gate.ICE, Kind.PRESS, false),
    SNOW_THROW(Element.WATER, Gate.ICE, Kind.PRESS, false),
    ICE_WALL(Element.WATER, Gate.ICE, Kind.PRESS, false),
    BLOODBENDING(Element.WATER, Gate.BLOOD, Kind.PRESS, false),
    BLOOD_SUPPRESS(Element.WATER, Gate.BLOOD, Kind.PRESS, false),
    WATER_SHIELD(Element.WATER, Gate.NONE, Kind.HOLD, false),
    WATER_BUBBLE(Element.WATER, Gate.NONE, Kind.TOGGLE, false),
    PHASE_CHANGE(Element.WATER, Gate.ICE, Kind.PRESS, false),
    FROST_BREATH(Element.WATER, Gate.ICE, Kind.HOLD, false),
    PLANT_TETHER(Element.WATER, Gate.NONE, Kind.PRESS, false),
    WATER_VORTEX(Element.WATER, Gate.NONE, Kind.PRESS, false),
    FAST_SWIM(Element.WATER, Gate.NONE, Kind.PASSIVE, false),
    WATER_STEP(Element.WATER, Gate.NONE, Kind.PASSIVE, false),

    // ---- Fire ----
    FIRE_SKIN(Element.FIRE, Gate.NONE, Kind.PASSIVE, true),
    FIRE_BALL(Element.FIRE, Gate.NONE, Kind.PRESS, false),
    FIRE_FIST(Element.FIRE, Gate.NONE, Kind.HOLD, false),
    FIRE_WHIP(Element.FIRE, Gate.NONE, Kind.HOLD, false),
    FIRE_RING(Element.FIRE, Gate.NONE, Kind.PRESS, false),
    FIRE_HEALING(Element.FIRE, Gate.NONE, Kind.PRESS, false),
    LIGHTNING(Element.FIRE, Gate.LIGHTNING, Kind.PRESS, false),
    LIGHTNING_REDIRECT(Element.FIRE, Gate.LIGHTNING, Kind.TOGGLE, false),
    FIRE_JET(Element.FIRE, Gate.NONE, Kind.PRESS, false),
    FIRE_SHIELD(Element.FIRE, Gate.NONE, Kind.HOLD, false),
    WALL_OF_FIRE(Element.FIRE, Gate.NONE, Kind.PRESS, false),
    BLAZE(Element.FIRE, Gate.NONE, Kind.PRESS, false),
    HEAT_CONTROL(Element.FIRE, Gate.NONE, Kind.TOGGLE, false),
    ILLUMINATION(Element.FIRE, Gate.NONE, Kind.TOGGLE, false),
    FIRE_STEP(Element.FIRE, Gate.NONE, Kind.PASSIVE, false),
    PALM_FORGE(Element.FIRE, Gate.NONE, Kind.HOLD, false),
    COMBUSTION(Element.FIRE, Gate.COMBUSTION, Kind.PRESS, false),
    // ---- the Avatar ----
    AVATAR_STATE(Element.AVATAR, Gate.NONE, Kind.PRESS, true),
    ENERGYBENDING(Element.AVATAR, Gate.NONE, Kind.PRESS, false),
    SPIRITBENDING(Element.AVATAR, Gate.NONE, Kind.PRESS, false),
    ELEMENTAL_SPHERE(Element.AVATAR, Gate.NONE, Kind.TOGGLE, false);

    /** How an ability answers its key. */
    public enum Kind { PRESS, HOLD, TOGGLE, PASSIVE }

    public static final int MAX_LEVEL = 3;

    public final Element element;
    public final Gate gate;
    public final Kind kind;
    /** Whether every bender of the element starts with this ability at level 1, for free. */
    public final boolean starter;

    Ability(Element element, Gate gate, Kind kind, boolean starter) {
        this.element = element;
        this.gate = gate;
        this.kind = kind;
        this.starter = starter;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("ability.enlightened." + id());
    }

    /** The short description shown in the scroll. */
    public Component description() {
        return Component.translatable("ability.enlightened." + id() + ".desc");
    }

    /** The points the given level costs (level 1: 1 point, level 2: 2, level 3: 3). */
    public static int cost(int level) {
        return level;
    }

    /** Whether the ability can sit on a key. */
    public boolean bindable() {
        return kind != Kind.PASSIVE;
    }

    public static Ability byId(String id) {
        for (Ability a : values()) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    public static List<Ability> of(Element element) {
        List<Ability> list = new ArrayList<>();
        for (Ability a : values()) {
            if (element.commands(a.element)) {
                list.add(a);
            }
        }
        return list;
    }

    /** The starter ability of an element (the one every bender begins with). */
    public static Ability starterOf(Element element) {
        for (Ability a : values()) {
            if (a.element == element && a.starter) {
                return a;
            }
        }
        return null;
    }
}
