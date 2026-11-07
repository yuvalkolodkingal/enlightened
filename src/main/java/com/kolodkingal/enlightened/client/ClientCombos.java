package com.kolodkingal.enlightened.client;

import java.util.List;
import java.util.Set;

/** The combos the local player has found, as the server last said (for the scroll's Combos tab). */
public final class ClientCombos {
    private static Set<String> found = Set.of();

    public static void set(List<String> ids) {
        found = Set.copyOf(ids);
    }

    public static boolean found(String id) {
        return found.contains(id);
    }

    public static int count() {
        return found.size();
    }

    private ClientCombos() {
    }
}
