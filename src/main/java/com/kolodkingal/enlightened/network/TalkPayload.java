package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.talk.Talk;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * A talk (1.0.23): {@link Open} (server → client) shows a line of a speaker and the answers offered; {@link Choose}
 * (client → server) is the answer chosen, or "" when the screen closes.
 */
public final class TalkPayload {
    public static final int MAX_KEY = 128;
    public static final int MAX_ARGS = 4;

    public record Open(int entityId, int speaker, String line, List<String> args, List<String> options) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Enlightened.id("talk_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::entityId,
                ByteBufCodecs.VAR_INT, Open::speaker,
                ByteBufCodecs.stringUtf8(MAX_KEY), Open::line,
                ByteBufCodecs.stringUtf8(MAX_KEY).apply(ByteBufCodecs.list(MAX_ARGS)), Open::args,
                ByteBufCodecs.stringUtf8(MAX_KEY).apply(ByteBufCodecs.list(com.kolodkingal.enlightened.talk.Dialogue.MAX_OPTIONS)), Open::options,
                Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Choose(int entityId, String option) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(Enlightened.id("talk_choose"));
        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Choose::entityId,
                ByteBufCodecs.stringUtf8(MAX_KEY), Choose::option,
                Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void handleOpen(Open open, IPayloadContext context) {
        context.enqueueWork(() -> com.kolodkingal.enlightened.client.screen.TalkScreen.open(open));
    }

    static void handleChoose(Choose choose, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isAlive()) {
                Talk.choose(player, choose.entityId(), choose.option());
            }
        });
    }

    private TalkPayload() {
    }
}
