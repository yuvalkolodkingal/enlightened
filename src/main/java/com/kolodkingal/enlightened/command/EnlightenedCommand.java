package com.kolodkingal.enlightened.command;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Loadout;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.Variant;
import com.kolodkingal.enlightened.bending.Variants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * /enlightened element get [player]
 * /enlightened element set &lt;players&gt; &lt;element&gt; [look]
 * /enlightened look &lt;players&gt; &lt;look&gt;
 * /enlightened points add &lt;players&gt; &lt;points&gt;
 * /enlightened learn &lt;players&gt; &lt;ability&gt; [level]
 * /enlightened loadout &lt;player&gt; &lt;slot 1-10&gt; &lt;ability|none&gt;
 * /enlightened tetherless &lt;players&gt; &lt;true|false&gt;
 * /enlightened reset &lt;players&gt;
 * /enlightened info [player]
 */
public final class EnlightenedCommand {
    private static final DynamicCommandExceptionType UNKNOWN_ELEMENT = new DynamicCommandExceptionType(
            name -> Component.translatable("command.enlightened.unknown_element", name));
    private static final DynamicCommandExceptionType UNKNOWN_LOOK = new DynamicCommandExceptionType(
            name -> Component.translatable("command.enlightened.unknown_look", name));
    private static final DynamicCommandExceptionType UNKNOWN_ABILITY = new DynamicCommandExceptionType(
            name -> Component.translatable("command.enlightened.unknown_ability", name));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("enlightened")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("element")
                        .then(Commands.literal("get")
                                .executes(ctx -> get(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> get(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(Element.values()).map(Element::getSerializedName), builder))
                                                .executes(ctx -> set(ctx, ""))
                                                .then(Commands.argument("look", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                Variants.of(element(StringArgumentType.getString(ctx, "element"))).stream().map(Variant::id), builder))
                                                        .executes(ctx -> set(ctx, StringArgumentType.getString(ctx, "look"))))))))
                .then(Commands.literal("look")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("look", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Variants.of(Benders.element(ctx.getSource().getPlayerOrException())).stream().map(Variant::id), builder))
                                        .executes(EnlightenedCommand::look))))
                .then(Commands.literal("points")
                        .then(Commands.literal("add")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("points", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(EnlightenedCommand::addPoints)))))
                .then(Commands.literal("learn")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Ability.values()).map(Ability::id), builder))
                                        .executes(ctx -> learn(ctx, -1))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, Ability.MAX_LEVEL))
                                                .executes(ctx -> learn(ctx, IntegerArgumentType.getInteger(ctx, "level")))))))
                .then(Commands.literal("loadout")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, BendingData.SLOTS))
                                        .then(Commands.argument("ability", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        java.util.stream.Stream.concat(java.util.stream.Stream.of("none"),
                                                                Arrays.stream(Ability.values()).map(Ability::id)), builder))
                                                .executes(EnlightenedCommand::loadout)))))
                .then(Commands.literal("tetherless")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .executes(EnlightenedCommand::tetherless))))
                .then(Commands.literal("chakras")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("count", IntegerArgumentType.integer(0, 7))
                                        .executes(EnlightenedCommand::chakras))))
                .then(Commands.literal("avatar")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.literal("enter").executes(ctx -> avatar(ctx, "enter")))
                                .then(Commands.literal("leave").executes(ctx -> avatar(ctx, "leave")))
                                .then(Commands.literal("rest").executes(ctx -> avatar(ctx, "rest")))))
                .then(Commands.literal("temple")
                        .executes(ctx -> temple(ctx, net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition())))
                        .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                .executes(ctx -> temple(ctx, net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
                .then(Commands.literal("guru")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.literal("taught").executes(ctx -> guru(ctx, true)))
                                .then(Commands.literal("untaught").executes(ctx -> guru(ctx, false)))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(EnlightenedCommand::reset)))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int chakras(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            BendingData data = Benders.get(player);
            data.setChakras(count);
            data.setMeditation(0);
            BendingTicker.applyPassives(player, data);
            Benders.syncIfDirty(player);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.chakras", count), true);
        return count;
    }

    /** Builds the Southern Air Temple with its terrace floor at {@code pos}, wherever that is (no mountain needed). */
    private static int temple(CommandContext<CommandSourceStack> ctx, net.minecraft.core.BlockPos pos) {
        net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
        com.kolodkingal.enlightened.world.AirTemplePiece piece = new com.kolodkingal.enlightened.world.AirTemplePiece(pos);
        net.minecraft.world.level.levelgen.structure.BoundingBox box = piece.getBoundingBox();
        net.minecraft.util.RandomSource random = level.getRandom();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(cx, cz);
                net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), random, chunkBox, chunk, pos);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.temple", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    /** Marks players as taught (or untaught) by Guru Pathik, as if they had spoken to him. */
    private static int guru(CommandContext<CommandSourceStack> ctx, boolean taught) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            Benders.get(player).setGuruTaught(taught);
            Benders.syncIfDirty(player);
            n++;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.translatable(taught ? "command.enlightened.guru.taught" : "command.enlightened.guru.untaught", count), true);
        return n;
    }

    /** Enters or leaves the Avatar State, or ends its rest, for players (whatever their chakras). */
    private static int avatar(CommandContext<CommandSourceStack> ctx, String what) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            BendingData data = Benders.get(player);
            // Only the Avatar has an Avatar State.
            if (data.element() != Element.AVATAR) {
                continue;
            }
            switch (what) {
                case "enter" -> {
                    data.setAvatarCooldownUntil(0);
                    com.kolodkingal.enlightened.bending.Chakras.enterAvatar(player, data, false);
                }
                case "leave" -> data.setAvatarUntil(0);
                default -> data.setAvatarCooldownUntil(0);
            }
            BendingTicker.applyPassives(player, data);
            Benders.syncIfDirty(player);
            n++;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.avatar." + what, count), true);
        return n;
    }

    private static Element element(String name) {
        return Element.byName(name);
    }

    private static int get(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        BendingData data = Benders.get(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.element.get", player.getDisplayName(),
                data.element().displayName(), Variants.current(data).displayName()), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, String look) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        String name = StringArgumentType.getString(ctx, "element");
        Element element = element(name);
        if (element == Element.NONE && !name.equals("none")) {
            throw UNKNOWN_ELEMENT.create(name);
        }
        for (ServerPlayer player : players) {
            if (element == Element.NONE) {
                Benders.setElement(player, Element.NONE);
            } else {
                Benders.awaken(player, element, look, false);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.element.set", players.size(), element.displayName()), true);
        return players.size();
    }

    private static int look(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        String id = StringArgumentType.getString(ctx, "look");
        int changed = 0;
        for (ServerPlayer player : players) {
            Element element = Benders.element(player);
            List<Variant> looks = Variants.of(element);
            Variant look = looks.stream().filter(v -> v.id().equals(id)).findFirst().orElse(null);
            if (look == null) {
                throw UNKNOWN_LOOK.create(id);
            }
            Benders.setVariant(player, look);
            changed++;
        }
        int n = changed;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.look.set", n, id), true);
        return changed;
    }

    private static int addPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        int points = IntegerArgumentType.getInteger(ctx, "points");
        for (ServerPlayer player : players) {
            Mastery.addPoints(player, points);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.points.added", points, players.size()), true);
        return players.size();
    }

    private static int learn(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        String id = StringArgumentType.getString(ctx, "ability");
        Ability ability = Ability.byId(id);
        if (ability == null) {
            throw UNKNOWN_ABILITY.create(id);
        }
        for (ServerPlayer player : players) {
            BendingData data = Benders.get(player);
            if (!data.element().commands(ability.element)) {
                continue;
            }
            int target = level < 0 ? Math.min(Ability.MAX_LEVEL, data.level(ability) + 1) : level;
            data.setLevel(ability, target);
            if (target > 0 && ability.bindable() && data.slotOf(ability) < 0) {
                Loadout.placeFree(data, ability);
            }
            if (target == 0) {
                data.setOn(ability, false);
                int slot = data.slotOf(ability);
                if (slot >= 0) {
                    data.setSlot(slot, null);
                }
            }
            if (ability.kind == Ability.Kind.PASSIVE) {
                data.setOn(ability, target > 0);
            }
            BendingTicker.applyPassives(player, data);
            Benders.syncIfDirty(player);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.learned", ability.displayName(), players.size()), true);
        return players.size();
    }

    private static int loadout(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
        String id = StringArgumentType.getString(ctx, "ability");
        Ability ability = id.equals("none") ? null : Ability.byId(id);
        if (ability == null && !id.equals("none")) {
            throw UNKNOWN_ABILITY.create(id);
        }
        BendingData data = Benders.get(player);
        boolean ok = Loadout.assign(data, slot, ability);
        Benders.syncIfDirty(player);
        if (!ok) {
            ctx.getSource().sendFailure(Component.translatable("command.enlightened.loadout.failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.loadout.set", slot + 1,
                ability == null ? Component.literal("-") : ability.displayName()), true);
        return 1;
    }

    private static int tetherless(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        boolean value = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value");
        for (ServerPlayer player : players) {
            BendingData data = Benders.get(player);
            data.setTetherless(value);
            if (!value) {
                data.setLevel(Ability.FLIGHT, 0);
                data.setLevel(Ability.SUFFOCATE, 0);
                data.setOn(Ability.FLIGHT, false);
            }
            BendingTicker.applyPassives(player, data);
            Benders.syncIfDirty(player);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.tetherless", players.size(), value), true);
        return players.size();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer player : players) {
            Mastery.reset(player);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.reset", players.size()), true);
        return players.size();
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        BendingData data = Benders.get(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.info", player.getDisplayName(),
                data.element().displayName(), data.points(), data.xp(), Mastery.xpPerPoint(), data.spent(), data.kills()), false);
        for (Ability a : Ability.of(data.element())) {
            if (data.level(a) > 0) {
                int slot = data.slotOf(a);
                ctx.getSource().sendSuccess(() -> Component.translatable("command.enlightened.info.ability", a.displayName(), data.level(a),
                        slot < 0 ? "-" : String.valueOf(slot + 1)), false);
            }
        }
        return 1;
    }

    private EnlightenedCommand() {
    }
}
