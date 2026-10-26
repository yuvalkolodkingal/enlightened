package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Steps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: a double tap of a direction, for the element step (the direction in the player's frame, already turned to the world). */
public record StepPayload(float x, float y, float z) implements CustomPacketPayload {
    public static final Type<StepPayload> TYPE = new Type<>(Enlightened.id("step"));

    public static final StreamCodec<ByteBuf, StepPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StepPayload::x,
            ByteBufCodecs.FLOAT, StepPayload::y,
            ByteBufCodecs.FLOAT, StepPayload::z,
            StepPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(StepPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Steps.step(player, new Vec3(payload.x(), payload.y(), payload.z()));
            }
        });
    }
}
