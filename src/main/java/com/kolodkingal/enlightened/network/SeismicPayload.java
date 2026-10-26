package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.client.SeismicGlow;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server → client: the creatures an earthbender's seismic sense feels through the ground right now (their entity
 * ids), to be outlined for {@code ticks}.
 */
public record SeismicPayload(List<Integer> ids, int ticks) implements CustomPacketPayload {
    public static final Type<SeismicPayload> TYPE = new Type<>(Enlightened.id("seismic"));

    public static final StreamCodec<ByteBuf, SeismicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), SeismicPayload::ids,
            ByteBufCodecs.VAR_INT, SeismicPayload::ticks,
            SeismicPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SeismicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SeismicGlow.show(payload.ids(), payload.ticks()));
    }
}
