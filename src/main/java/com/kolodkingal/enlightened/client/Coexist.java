package com.kolodkingal.enlightened.client;

import net.neoforged.fml.ModList;

/**
 * Playing with Enhanced installed too. Both mods put their abilities on the same keys and their panel in the same
 * corner, so a player who is both a hero and a bender would fire both at once. The two mods agree, without either
 * needing the other's classes, through a few JVM properties (client side only):
 * <ul>
 *   <li>{@code kolodkingal.powers.<mod>}: "1" while the local player has that mod's powers;</li>
 *   <li>{@code kolodkingal.powers.active}: the mod the ability keys go to while he has both ("enhanced" or
 *   "enlightened"); Enhanced owns the key that switches it, and publishes its name in
 *   {@code kolodkingal.powers.switchKey};</li>
 *   <li>{@code kolodkingal.hud.bottom}: where the active mod's panel ends this frame ("R:96": right corner, y 96), so
 *   the other mod's one-line reminder goes under it.</li>
 * </ul>
 * With Enhanced absent (or an Enhanced from before this agreement) nothing changes.
 */
public final class Coexist {
    private static final String SELF = "enlightened";
    private static final String OTHER = "enhanced";
    private static final boolean OTHER_LOADED = ModList.get().isLoaded(OTHER);

    private Coexist() {
    }

    /** Says whether the local player bends (every client tick). */
    public static void publish(boolean hasPowers) {
        if (OTHER_LOADED) {
            System.setProperty("kolodkingal.powers." + SELF, hasPowers ? "1" : "0");
        }
    }

    /** Whether the local player is also one of Enhanced's heroes. */
    public static boolean otherHasPowers() {
        return OTHER_LOADED && "1".equals(System.getProperty("kolodkingal.powers." + OTHER));
    }

    /** Whether the ability keys and the panel are this mod's now (always, unless he is a hero too and switched to it). */
    public static boolean active() {
        return !otherHasPowers() || SELF.equals(System.getProperty("kolodkingal.powers.active"));
    }

    /** The name of the key that switches between the two, as Enhanced wrote it ("" if unknown). */
    public static String switchKey() {
        return System.getProperty("kolodkingal.powers.switchKey", "");
    }

    /** Says where this mod's panel ended this frame. */
    public static void hudBottom(int y, boolean right) {
        if (OTHER_LOADED) {
            System.setProperty("kolodkingal.hud.bottom", (right ? "R:" : "L:") + y);
        }
    }

    /** Where the other mod's panel ended ({@code [y, right ? 1 : 0]}), or null. */
    public static int[] otherHudBottom() {
        String value = System.getProperty("kolodkingal.hud.bottom");
        if (value == null || value.length() < 3) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(value.substring(2)), value.charAt(0) == 'R' ? 1 : 0};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
