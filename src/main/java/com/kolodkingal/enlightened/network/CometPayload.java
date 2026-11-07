package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client (1.0.27): Sozin's Comet is in this world's sky, or has passed. */
public record CometPayload(boolean active) implements CustomPacketPayload {
    public static final Type<CometPayload> TYPE = new Type<>(Enlightened.id("comet"));
    public static final StreamCodec<ByteBuf, CometPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, CometPayload::active, CometPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(CometPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.kolodkingal.enlightened.client.sky.CometSky.set(payload.active()));
    }
}
