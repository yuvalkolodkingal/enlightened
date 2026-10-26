package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.BendingHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: an ability key (1-5) went down or came up, with the client's Sneak at that moment (the server's
 * crouch lags behind a quick Sneak + key, and the Sneak chooses the slot).
 */
public record AbilityKeyPayload(int key, boolean down, boolean sneak) implements CustomPacketPayload {
    public static final Type<AbilityKeyPayload> TYPE = new Type<>(Enlightened.id("ability_key"));

    public static final StreamCodec<ByteBuf, AbilityKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityKeyPayload::key,
            ByteBufCodecs.BOOL, AbilityKeyPayload::down,
            ByteBufCodecs.BOOL, AbilityKeyPayload::sneak,
            AbilityKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(AbilityKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BendingHandler.onKey(player, payload.key(), payload.down(), payload.sneak());
            }
        });
    }
}
