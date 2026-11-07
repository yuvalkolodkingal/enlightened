package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server → client (1.0.25): the ids of the combos the player has found, for the Bending Scroll's Combos tab. */
public record ComboPayload(List<String> discovered) implements CustomPacketPayload {
    public static final Type<ComboPayload> TYPE = new Type<>(Enlightened.id("combos"));
    public static final StreamCodec<ByteBuf, ComboPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(256)), ComboPayload::discovered,
            ComboPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ComboPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.kolodkingal.enlightened.client.ClientCombos.set(payload.discovered()));
    }
}
