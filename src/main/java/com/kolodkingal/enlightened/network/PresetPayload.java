package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.command.PresetCommand;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * The Bending Scroll's Presets tab (1.0.23): {@link Act} asks the server to list, save, load or delete a preset of the
 * keys (the same presets as {@code /bendpreset}); {@link Listing} tells the client the presets, each as its name and
 * the ids of the abilities on its ten slots ("" for an empty slot), after every act.
 */
public final class PresetPayload {
    public static final int LIST = 0;
    public static final int SAVE = 1;
    public static final int LOAD = 2;
    public static final int DELETE = 3;
    /** The longest name the scroll lets you type. */
    public static final int MAX_NAME = 16;

    public record Act(int action, String name) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(Enlightened.id("preset_act"));
        public static final StreamCodec<ByteBuf, Act> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Act::action,
                ByteBufCodecs.stringUtf8(64), Act::name,
                Act::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Listing(List<String> names, List<String> slots) implements CustomPacketPayload {
        public static final Type<Listing> TYPE = new Type<>(Enlightened.id("preset_list"));
        public static final StreamCodec<ByteBuf, Listing> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(PresetCommand.MAX)), Listing::names,
                ByteBufCodecs.stringUtf8(1024).apply(ByteBufCodecs.list(PresetCommand.MAX)), Listing::slots,
                Listing::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A name the scroll may save under: 1 to 16 letters, digits, '-' or '_' (any alphabet). */
    public static boolean validName(String name) {
        if (name.isEmpty() || name.length() > MAX_NAME) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                return false;
            }
        }
        return true;
    }

    static void handleAct(Act act, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String name = act.name().trim();
            switch (act.action()) {
                case SAVE -> {
                    if (!validName(name)) {
                        return;
                    }
                    if (PresetCommand.save(player, name)) {
                        player.displayClientMessage(Component.translatable("command.enlightened.preset.saved", name).withStyle(ChatFormatting.GREEN), true);
                    } else {
                        player.displayClientMessage(Component.translatable("command.enlightened.preset.full", PresetCommand.MAX).withStyle(ChatFormatting.RED), true);
                    }
                }
                case LOAD -> {
                    int n = PresetCommand.load(player, name);
                    if (n >= 0) {
                        player.displayClientMessage(Component.translatable("command.enlightened.preset.loaded", name, n, PresetCommand.lastLearned())
                                .withStyle(ChatFormatting.GREEN), true);
                    }
                }
                case DELETE -> {
                    if (PresetCommand.delete(player, name)) {
                        player.displayClientMessage(Component.translatable("command.enlightened.preset.deleted", name), true);
                    }
                }
                default -> {
                }
            }
            ModNetwork.sendTo(player, PresetCommand.listing(player));
            ModNetwork.sendTo(player, com.kolodkingal.enlightened.bending.Combos.listing(player));
        });
    }

    static void handleListing(Listing listing, IPayloadContext context) {
        context.enqueueWork(() -> com.kolodkingal.enlightened.client.ClientPresets.set(listing.names(), listing.slots()));
    }

    private PresetPayload() {
    }
}
