package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Loadout;
import com.kolodkingal.enlightened.bending.Mastery;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: a button of the Bending Scroll screen. {@link #LEARN} spends points on the next level of an
 * ability; {@link #ASSIGN} puts an ability (or nothing: -1) on a slot; {@link #RESET_LOADOUT} lays the loadout out
 * afresh.
 *
 * @param action  one of the three
 * @param slot    the slot (0-9) for ASSIGN
 * @param ability the ability's ordinal, or -1
 */
public record ScrollActionPayload(int action, int slot, int ability) implements CustomPacketPayload {
    public static final int LEARN = 0;
    public static final int ASSIGN = 1;
    public static final int RESET_LOADOUT = 2;

    public static final Type<ScrollActionPayload> TYPE = new Type<>(Enlightened.id("scroll_action"));

    public static final StreamCodec<ByteBuf, ScrollActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScrollActionPayload::action,
            ByteBufCodecs.VAR_INT, ScrollActionPayload::slot,
            ByteBufCodecs.INT, ScrollActionPayload::ability,
            ScrollActionPayload::new);

    public static ScrollActionPayload learn(Ability ability) {
        return new ScrollActionPayload(LEARN, 0, ability.ordinal());
    }

    public static ScrollActionPayload assign(int slot, Ability ability) {
        return new ScrollActionPayload(ASSIGN, slot, ability == null ? -1 : ability.ordinal());
    }

    public static ScrollActionPayload resetLoadout() {
        return new ScrollActionPayload(RESET_LOADOUT, 0, -1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ScrollActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BendingData data = Benders.get(player);
            if (!data.canBend()) {
                return;
            }
            Ability[] all = Ability.values();
            Ability ability = payload.ability() >= 0 && payload.ability() < all.length ? all[payload.ability()] : null;
            switch (payload.action()) {
                case LEARN -> {
                    if (ability != null) {
                        Mastery.learn(player, ability);
                    }
                }
                case ASSIGN -> Loadout.assign(data, payload.slot(), ability);
                case RESET_LOADOUT -> Loadout.reset(data);
                default -> {
                }
            }
            Benders.syncIfDirty(player);
        });
    }
}
