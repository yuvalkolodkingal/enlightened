package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.DragonEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: the rider of a dragon attacked (left-click): his dragon breathes fire where he looks. */
public record DragonBreathPayload() implements CustomPacketPayload {
    public static final Type<DragonBreathPayload> TYPE = new Type<>(Enlightened.id("dragon_breath"));
    public static final DragonBreathPayload INSTANCE = new DragonBreathPayload();
    public static final StreamCodec<ByteBuf, DragonBreathPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(DragonBreathPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof DragonEntity dragon) {
                dragon.riderBreathes(player);
            }
        });
    }
}
