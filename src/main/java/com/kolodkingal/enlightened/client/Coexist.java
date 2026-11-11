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

    private static boolean looked;
    private static net.neoforged.neoforge.attachment.AttachmentType<?> heroType;
    private static java.lang.reflect.Method powerOf;
    private static java.lang.reflect.Method hasPowers;

    /**
     * Whether a player (anyone in view, not only the local one) is one of Enhanced's heroes: read from Enhanced's
     * synced {@code enhanced:power} attachment by its id, so that this mod needs none of Enhanced's classes.
     */
    public static boolean isHero(net.minecraft.world.entity.player.Player player) {
        if (!OTHER_LOADED) {
            return false;
        }
        try {
            if (!looked) {
                looked = true;
                heroType = net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(OTHER, "power"));
            }
            if (heroType == null) {
                return false;
            }
            java.util.Optional<?> data = player.getExistingData(heroType);
            if (data.isEmpty()) {
                return false;
            }
            if (powerOf == null) {
                powerOf = data.get().getClass().getMethod("power");
                hasPowers = powerOf.getReturnType().getMethod("hasPowers");
            }
            return (Boolean) hasPowers.invoke(powerOf.invoke(data.get()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            heroType = null; // an Enhanced this does not understand: behave as if the player were no hero
            return false;
        }
    }

    /**
     * Whether this mod leaves a player's body to Enhanced's animation: a hero who is not casting any bending right
     * now. (While a bending gesture plays, the bending poses the body; the rest of the time the hero's own stance,
     * flight and run do, so the two never fight over the same limbs.)
     */
    public static boolean yieldsBody(net.minecraft.world.entity.player.Player player,
                                     com.kolodkingal.enlightened.client.anim.AnimState state) {
        if (!isHero(player)) {
            return false;
        }
        for (com.kolodkingal.enlightened.client.anim.AnimState.Slot slot : state.slots) {
            if (slot != null && slot.id != null && slot.weight > 0.01F) {
                return false;
            }
        }
        return true;
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
