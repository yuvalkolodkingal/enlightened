package com.kolodkingal.enlightened.command;

import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Benders;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /armor show|hide} (anyone): whether the armour one wears is drawn on him (for everyone who sees him). Hidden,
 * it still protects him.
 */
public final class ArmorCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("armor")
                .then(Commands.literal("show").executes(c -> set(c.getSource(), false)))
                .then(Commands.literal("hide").executes(c -> set(c.getSource(), true))));
    }

    private static int set(CommandSourceStack source, boolean hidden) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        BendingData data = Benders.get(p);
        data.setArmorHidden(hidden);
        Benders.syncIfDirty(p);
        source.sendSuccess(() -> Component.translatable(hidden ? "command.enlightened.armor.hidden" : "command.enlightened.armor.shown")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private ArmorCommand() {
    }
}
