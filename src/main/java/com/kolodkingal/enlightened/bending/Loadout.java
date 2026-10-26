package com.kolodkingal.enlightened.bending;

import java.util.List;

/**
 * Which abilities a bender takes into battle: ten slots, two per key (R, G, V, B, N, each plain and with Sneak).
 * There are more abilities than keys, so the bender chooses: in the Bending Scroll (the K key, or the scroll item),
 * or with {@code /enlightened loadout}. A newly learned ability takes the first free slot; the default loadout of an
 * element puts its most useful abilities on the keys in a sensible order.
 */
public final class Loadout {

    /** The default order of an element's abilities on the slots (only the learned ones are placed). */
    private static List<Ability> order(Element element) {
        return switch (element) {
            case EARTH -> List.of(Ability.ROCK_THROW, Ability.ROCK_LIFT, Ability.EARTH_ARMOR, Ability.METAL_ARMOR,
                    Ability.EARTH_PILLAR, Ability.SEISMIC_SENSE, Ability.EARTH_WALL, Ability.SAND_WALL,
                    Ability.QUICKSAND, Ability.LAVA_SHURIKEN, Ability.LAVA_PATH, Ability.METAL_BLADES,
                    Ability.METAL_WALL, Ability.SAND_LIFT, Ability.METAL_PULL, Ability.WRECK);
            case AIR -> List.of(Ability.AIR_BLAST, Ability.AIR_BLADES, Ability.AIR_SHIELD, Ability.SUFFOCATE,
                    Ability.AIR_JUMP, Ability.FLIGHT, Ability.AIR_SCOOTER, Ability.SPIRIT_PROJECTION,
                    Ability.TORNADO);
            case WATER -> List.of(Ability.WATER_WHIP, Ability.ICE_KNIVES, Ability.WATER_HEALING, Ability.FREEZE,
                    Ability.OCTOPUS_FORM, Ability.WATER_ARMS, Ability.WATER_SPOUT, Ability.ICE_WALL,
                    Ability.BLOODBENDING, Ability.BLOOD_SUPPRESS, Ability.SNOW_THROW);
            case FIRE -> List.of(Ability.FIRE_BALL, Ability.FIRE_FIST, Ability.FIRE_WHIP, Ability.FIRE_RING,
                    Ability.LIGHTNING, Ability.LIGHTNING_REDIRECT, Ability.FIRE_HEALING);
            case NONE -> List.of();
        };
    }

    /** Lays the learned abilities out in the element's default order. */
    public static void reset(BendingData data) {
        for (int s = 0; s < BendingData.SLOTS; s++) {
            data.setSlot(s, null);
        }
        int slot = 0;
        for (Ability a : order(data.element())) {
            if (data.has(a) && a.bindable() && slot < BendingData.SLOTS) {
                data.setSlot(slot++, a);
            }
        }
    }

    /** Puts a newly learned ability on its default slot if that is free, else on the first free slot (if any). */
    public static void placeFree(BendingData data, Ability a) {
        List<Ability> order = order(data.element());
        int wanted = order.indexOf(a);
        if (wanted >= 0 && wanted < BendingData.SLOTS && data.slot(wanted) == null) {
            data.setSlot(wanted, a);
            return;
        }
        for (int s = 0; s < BendingData.SLOTS; s++) {
            if (data.slot(s) == null) {
                data.setSlot(s, a);
                return;
            }
        }
    }

    /**
     * Puts an ability on a slot (null clears it). An ability sits on one slot at a time: it leaves its old slot. Returns
     * false if the bender has not learned the ability or it cannot sit on a key.
     */
    public static boolean assign(BendingData data, int slot, Ability a) {
        if (slot < 0 || slot >= BendingData.SLOTS) {
            return false;
        }
        if (a == null) {
            data.setSlot(slot, null);
            return true;
        }
        if (!data.has(a) || !a.bindable()) {
            return false;
        }
        int old = data.slotOf(a);
        if (old >= 0) {
            data.setSlot(old, data.slot(slot));
        }
        data.setSlot(slot, a);
        return true;
    }

    private Loadout() {
    }
}
