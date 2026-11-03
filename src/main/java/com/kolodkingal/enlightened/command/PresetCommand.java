package com.kolodkingal.enlightened.command;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Mastery;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Presets of abilities on the keys, for anyone: {@code /bendpreset save <name>} keeps which ability sits on each of
 * the ten slots and the level of each; {@code /bendpreset load <name>} learns them again up to those levels (with
 * his points, as the scroll would: what he cannot yet learn is left out) and lays them out on the keys;
 * {@code /bendpreset list}, {@code /bendpreset delete <name>}. Kept on the player, through deaths.
 */
public final class PresetCommand {
    private static final String KEY = "enlightened_presets";
    public static final int MAX = 20;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bendpreset")
                .then(Commands.literal("save").then(Commands.argument("name", StringArgumentType.word()).executes(PresetCommand::save)))
                .then(Commands.literal("load").then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(presets(c.getSource().getPlayerOrException()).getAllKeys(), b))
                        .executes(PresetCommand::load)))
                .then(Commands.literal("delete").then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(presets(c.getSource().getPlayerOrException()).getAllKeys(), b))
                        .executes(PresetCommand::delete)))
                .then(Commands.literal("list").executes(PresetCommand::list)));
    }

    static CompoundTag presets(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
    }

    static void store(Player p, CompoundTag presets) {
        CompoundTag kept = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        kept.put(KEY, presets);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
    }

    /** Saves the player's slots as a preset; false if he has too many already. */
    public static boolean save(ServerPlayer p, String name) {
        CompoundTag all = presets(p);
        if (!all.contains(name) && all.size() >= MAX) {
            return false;
        }
        BendingData data = Benders.get(p);
        ListTag slots = new ListTag();
        CompoundTag levels = new CompoundTag();
        for (int s = 0; s < BendingData.SLOTS; s++) {
            Ability a = data.slot(s);
            slots.add(StringTag.valueOf(a == null ? "" : a.id()));
            if (a != null) {
                levels.putInt(a.id(), data.level(a));
            }
        }
        CompoundTag preset = new CompoundTag();
        preset.put("Slots", slots);
        preset.put("Levels", levels);
        all.put(name, preset);
        store(p, all);
        return true;
    }

    /** Levels learned by the last {@link #load}, and the abilities it could not learn. */
    static int lastLearned;
    static final java.util.List<Ability> LAST_MISSING = new java.util.ArrayList<>();

    /**
     * Learns a preset's abilities up to their saved levels (with the player's points, where he may) and lays them out
     * on the slots; returns how many abilities were placed, or -1 if there is no such preset.
     */
    public static int load(ServerPlayer p, String name) {
        CompoundTag all = presets(p);
        if (!all.contains(name)) {
            return -1;
        }
        // 1.0.17 kept only the slots, as a list.
        boolean old = all.getTagType(name) == Tag.TAG_LIST;
        ListTag slots = old ? all.getList(name, Tag.TAG_STRING) : all.getCompound(name).getList("Slots", Tag.TAG_STRING);
        CompoundTag levels = old ? new CompoundTag() : all.getCompound(name).getCompound("Levels");
        BendingData data = Benders.get(p);
        lastLearned = 0;
        LAST_MISSING.clear();
        for (int s = 0; s < BendingData.SLOTS && s < slots.size(); s++) {
            Ability a = Ability.byId(slots.getString(s));
            if (a == null) {
                continue;
            }
            int want = Math.max(1, levels.contains(a.id()) ? levels.getInt(a.id()) : 1);
            while (data.level(a) < want && Mastery.canLearn(data, a)) {
                int before = data.level(a);
                com.kolodkingal.enlightened.bending.Mastery.learn(p, a);
                if (data.level(a) == before) {
                    break;
                }
                lastLearned++;
            }
            if (data.level(a) < 1 && !LAST_MISSING.contains(a)) {
                LAST_MISSING.add(a);
            }
        }
        int placed = 0;
        for (int s = 0; s < BendingData.SLOTS; s++) {
            Ability a = s < slots.size() ? Ability.byId(slots.getString(s)) : null;
            boolean ok = a != null && data.has(a) && a.bindable();
            data.setSlot(s, ok ? a : null);
            placed += ok ? 1 : 0;
        }
        Benders.syncIfDirty(p);
        return placed;
    }

    private static int save(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        if (!save(p, name)) {
            c.getSource().sendFailure(Component.translatable("command.enlightened.preset.full", MAX));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.enlightened.preset.saved", name).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int load(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        int n = load(p, name);
        if (n < 0) {
            c.getSource().sendFailure(Component.translatable("command.enlightened.preset.none", name));
            return 0;
        }
        int learned = lastLearned;
        c.getSource().sendSuccess(() -> Component.translatable("command.enlightened.preset.loaded", name, n, learned).withStyle(ChatFormatting.GREEN), false);
        if (!LAST_MISSING.isEmpty()) {
            net.minecraft.network.chat.MutableComponent names = Component.empty();
            for (int i = 0; i < LAST_MISSING.size(); i++) {
                names.append(i == 0 ? Component.empty() : Component.literal(", ")).append(LAST_MISSING.get(i).displayName());
            }
            c.getSource().sendSuccess(() -> Component.translatable("command.enlightened.preset.missing", names).withStyle(ChatFormatting.GRAY), false);
        }
        return n;
    }

    private static int delete(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        CompoundTag all = presets(p);
        if (!all.contains(name)) {
            c.getSource().sendFailure(Component.translatable("command.enlightened.preset.none", name));
            return 0;
        }
        all.remove(name);
        store(p, all);
        c.getSource().sendSuccess(() -> Component.translatable("command.enlightened.preset.deleted", name), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var names = presets(p).getAllKeys();
        c.getSource().sendSuccess(() -> names.isEmpty() ? Component.translatable("command.enlightened.preset.empty")
                : Component.translatable("command.enlightened.preset.list", String.join(", ", new java.util.TreeSet<>(names))), false);
        return names.size();
    }

    private PresetCommand() {
    }
}
