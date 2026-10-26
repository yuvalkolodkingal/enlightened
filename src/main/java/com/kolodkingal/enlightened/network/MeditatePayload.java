package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Chakras;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: the Meditate key went down or came up. */
public record MeditatePayload(boolean down) implements CustomPacketPayload {
    public static final Type<MeditatePayload> TYPE = new Type<>(Enlightened.id("meditate"));

    public static final StreamCodec<ByteBuf, MeditatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MeditatePayload::down, MeditatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(MeditatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Chakras.meditate(player, payload.down());
            }
        });
    }
}
