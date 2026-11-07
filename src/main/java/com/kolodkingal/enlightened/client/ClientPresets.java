package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.BendingData;

import java.util.ArrayList;
import java.util.List;

/** The local player's presets of the keys, as the server last listed them (for the scroll's Presets tab). */
public final class ClientPresets {
    public record Preset(String name, Ability[] slots) {
    }

    private static List<Preset> presets = List.of();
    /** Bumped on every listing, so the scroll knows to lay itself out again. */
    private static int version;

    public static void set(List<String> names, List<String> slots) {
        List<Preset> out = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String[] ids = (i < slots.size() ? slots.get(i) : "").split(",", -1);
            Ability[] row = new Ability[BendingData.SLOTS];
            for (int s = 0; s < row.length && s < ids.length; s++) {
                row[s] = ids[s].isEmpty() ? null : Ability.byId(ids[s]);
            }
            out.add(new Preset(names.get(i), row));
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        presets = List.copyOf(out);
        version++;
    }

    public static List<Preset> all() {
        return presets;
    }

    public static int version() {
        return version;
    }

    public static void clear() {
        presets = List.of();
        version++;
    }

    private ClientPresets() {
    }
}
