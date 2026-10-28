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
                // Sneaking, a firebender (or the Avatar) changes the colour of his fire instead of his look.
                if (player.isShiftKeyDown() && Benders.element(player).commands(com.kolodkingal.enlightened.bending.Element.FIRE)) {
                    com.kolodkingal.enlightened.bending.BendingData data = Benders.get(player);
                    int next = Math.floorMod(com.kolodkingal.enlightened.bending.Variants.fireColour(data) + (payload.step() < 0 ? -1 : 1), 3);
                    data.setFireChoice(next + 1);
                    Benders.syncIfDirty(player);
                    player.displayClientMessage(Component.translatable("message.enlightened.fire_colour",
                            Component.translatable("fire_colour.enlightened." + com.kolodkingal.enlightened.bending.Variants.FIRE_COLOURS[next])), true);
                    return;
                }
                if (!Benders.cycleVariant(player, payload.step() < 0 ? -1 : 1) && Benders.element(player).bends()) {
                    player.displayClientMessage(Component.translatable("message.enlightened.look.none"), true);
                }
            }
        });
    }
}
