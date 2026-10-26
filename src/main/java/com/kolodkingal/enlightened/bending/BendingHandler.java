package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Routes an ability key press or release from a client to the ability on that slot of the bender's loadout. */
public final class BendingHandler {

    /**
     * A key from the client, with the client's own Sneak at the moment of the press: the server's crouch lags behind a
     * quick Sneak + key, so the client says which variant it pressed.
     *
     * @param key   1-5
     * @param down  true when the key was pressed, false when released (hold abilities need both)
     * @param sneak whether Sneak was held when the key was pressed
     */
    public static void onKey(ServerPlayer player, int key, boolean down, boolean sneak) {
        if (key < 1 || key > BendingData.KEYS) {
            return;
        }
        BendingData data = Benders.get(player);
        if (!data.element().bends()) {
            return;
        }
        // Releases always go through, so a hold ability can't get stuck on (e.g. when the player died holding the key).
        if (down && !player.isAlive()) {
            return;
        }
        if (down && !mayBend(player, data)) {
            return;
        }
        Ability ability = data.slot(BendingData.slotIndex(key, sneak));
        if (ability == null || !data.has(ability)) {
            if (down) {
                player.displayClientMessage(Component.translatable("message.enlightened.empty_slot").withStyle(ChatFormatting.GRAY), true);
            }
            // The release of a key whose slot changed meanwhile: let every hold ability go.
            if (!down) {
                releaseHolds(player, data);
            }
            return;
        }
        AbilityLogic logic = Abilities.of(ability);
        if (logic != null) {
            long before = data.cooldownUntil(ability);
            logic.key(new Cast(player, data, ability), down);
            // A cast that went off (its cooldown began, or a hold took hold) may complete a combo.
            if (down && (data.cooldownUntil(ability) > before || ability.kind == Ability.Kind.HOLD && data.active(ability)
                    || ability.kind == Ability.Kind.TOGGLE && data.on(ability))) {
                Combos.cast(player, data, ability);
            }
        }
        if (!down) {
            // The slot's ability got the release; any other hold still active on another key stays its own.
        }
        Benders.syncIfDirty(player);
    }

    /** Whether the bender's bending answers at all right now (a bloodbender may have taken it, or a trance holds him). */
    public static boolean mayBend(ServerPlayer player, BendingData data) {
        if (data.suppressed(player.level().getGameTime())) {
            player.displayClientMessage(Component.translatable("message.enlightened.suppressed").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (player.hasEffect(ModEffects.BLOODBENT)) {
            return false;
        }
        if (player.isSpectator() && !data.active(Ability.SPIRIT_PROJECTION)) {
            return false;
        }
        return true;
    }

    /** Releases every hold ability (as if its key came up). */
    public static void releaseHolds(ServerPlayer player, BendingData data) {
        for (Ability a : Ability.values()) {
            if (a.kind == Ability.Kind.HOLD && data.active(a)) {
                AbilityLogic logic = Abilities.of(a);
                if (logic != null) {
                    logic.key(new Cast(player, data, a), false);
                }
            }
        }
    }

    private BendingHandler() {
    }
}
