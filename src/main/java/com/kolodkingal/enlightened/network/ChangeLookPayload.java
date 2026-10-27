package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Benders;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: the Change Look key was pressed (step +1 for the next look, -1 for the previous one: Sneak). */
public record ChangeLookPayload(int step) implements CustomPacketPayload {
    public static final Type<ChangeLookPayload> TYPE = new Type<>(Enlightened.id("change_look"));

    public static final StreamCodec<ByteBuf, ChangeLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChangeLookPayload::step,
            ChangeLookPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ChangeLookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
                if (com.kolodkingal.enlightened.water.WaterBending.switchOctopus(player)) {
                    return;
                }
                if (!Benders.cycleVariant(player, payload.step() < 0 ? -1 : 1) && Benders.element(player).bends()) {
                    player.displayClientMessage(Component.translatable("message.enlightened.look.none"), true);
                }
            }
        });
    }
}
