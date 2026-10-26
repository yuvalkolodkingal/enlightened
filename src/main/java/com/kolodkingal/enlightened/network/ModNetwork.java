package com.kolodkingal.enlightened.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client → server messages (ability keys, Change Look, the scroll's actions) and server → client messages (the
 * seismic sense); everything else the client must know (the bending data) travels as a synced attachment.
 */
public final class ModNetwork {
    /** A client and a server with different versions can't play together. */
    public static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(AbilityKeyPayload.TYPE, AbilityKeyPayload.STREAM_CODEC, AbilityKeyPayload::handle);
        registrar.playToServer(ChangeLookPayload.TYPE, ChangeLookPayload.STREAM_CODEC, ChangeLookPayload::handle);
        registrar.playToServer(ScrollActionPayload.TYPE, ScrollActionPayload.STREAM_CODEC, ScrollActionPayload::handle);
        registrar.playToClient(SeismicPayload.TYPE, SeismicPayload.STREAM_CODEC, SeismicPayload::handle);
    }

    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private ModNetwork() {
    }
}
