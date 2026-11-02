package com.kolodkingal.enlightened.quest;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /avatarquest light|dark}: the answer to Vaatu's offer (what his offer's two clickable answers run), and
 * {@code /avatarquest} alone, where one stands in the quest. Anyone may use it; it does nothing until Vaatu has asked.
 */
public final class QuestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("avatarquest")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.translatable("message.enlightened.quest.stage." + AvatarQuest.stage(p).name().toLowerCase(java.util.Locale.ROOT),
                            AvatarQuest.kills(p), AvatarQuest.SPIRITS).withStyle(ChatFormatting.AQUA), false);
                    return 1;
                })
                .then(Commands.literal("light").executes(c -> answer(c.getSource(), false)))
                .then(Commands.literal("dark").executes(c -> answer(c.getSource(), true))));
    }

    private static int answer(CommandSourceStack source, boolean dark) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        String said = AvatarQuest.answer(p, dark);
        source.sendSuccess(() -> Component.translatable(said).withStyle(dark ? ChatFormatting.DARK_RED : ChatFormatting.AQUA), false);
        return 1;
    }

    private QuestCommand() {
    }
}
