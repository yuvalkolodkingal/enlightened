package com.kolodkingal.enlightened.bending;

import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of one ability. {@link Abilities} finds it by {@link Ability}; {@link BendingHandler} calls
 * {@link #key} when the ability's key goes down or up, {@link BendingTicker} calls {@link #tick} every tick for every
 * ability the bender has learned (so toggles, holds and passives can run), and {@link #end} when the bender loses the
 * element, dies, or logs out.
 */
public interface AbilityLogic {
    /** The ability's key went down ({@code down}) or came up. A release always arrives, so a hold can never stick. */
    void key(Cast c, boolean down);

    /** Every server tick of the bender's player (alive), for every learned ability. */
    default void tick(Cast c) {
    }

    /** Everything the ability left in the world or on the player ends (the element is lost, the player died or left). */
    default void end(Cast c) {
    }

    /** The server stopped: forget every static map and list the ability keeps. */
    default void clear() {
    }

    /** A player left the server: forget what the ability keeps about him. */
    default void forget(ServerPlayer player) {
    }
}
