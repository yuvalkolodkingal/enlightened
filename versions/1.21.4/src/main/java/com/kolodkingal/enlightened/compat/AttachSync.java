package com.kolodkingal.enlightened.compat;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.registry.ModAttachments;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.function.Supplier;

/**
 * 1.21.4 only: NeoForge 21.4 has no attachment sync, so the mod sends its two synced attachments itself, as NeoForge
 * 21.1 did: the whole value to everyone who sees the player (and to him) after a change, when someone starts seeing
 * him, and to him when he joins, respawns or changes dimension.
 */
public final class AttachSync {
    private record Synced<T>(Supplier<AttachmentType<T>> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
    }

    private static final List<Synced<?>> SYNCED = List.of(
            new Synced<>(ModAttachments.BENDING, BendingData.STREAM_CODEC),
            new Synced<>(ModAttachments.BACK, ItemStack.OPTIONAL_STREAM_CODEC));

    /** One attachment's value for one entity: the index in {@link #SYNCED} and the value's bytes. */
    public record Payload(int entityId, int index, byte[] bytes) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Enlightened.id("attachment"));
        public static final StreamCodec<FriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::entityId,
                ByteBufCodecs.VAR_INT, Payload::index,
                ByteBufCodecs.BYTE_ARRAY, Payload::bytes,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    private AttachSync() {
    }

    private static int indexOf(AttachmentType<?> type) {
        for (int i = 0; i < SYNCED.size(); i++) {
            if (SYNCED.get(i).type().get() == type) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T> Payload payload(Entity holder, int index) {
        Synced<T> synced = (Synced<T>) SYNCED.get(index);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), holder.registryAccess());
        try {
            synced.codec().encode(buf, holder.getData(synced.type()));
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new Payload(holder.getId(), index, bytes);
        } finally {
            buf.release();
        }
    }

    /** Sends {@code type}'s value of {@code holder} to everyone who sees it, and to it if it is a player. */
    public static void send(Entity holder, AttachmentType<?> type) {
        int index = indexOf(type);
        if (index >= 0 && !holder.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(holder, payload(holder, index));
        }
    }

    private static void sendAll(Entity holder, ServerPlayer to) {
        for (int i = 0; i < SYNCED.size(); i++) {
            if (holder.hasData(SYNCED.get(i).type().get())) {
                PacketDistributor.sendToPlayer(to, payload(holder, i));
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer to && event.getTarget() instanceof Player) {
            sendAll(event.getTarget(), to);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendAll(p, p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendAll(p, p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendAll(p, p);
        }
    }

    /** The client side: puts the value on the entity. */
    public static void receive(Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> Client.apply(payload));
    }

    /** Client classes only here. */
    private static final class Client {
        @SuppressWarnings("unchecked")
        static <T> void apply(Payload payload) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || payload.index() < 0 || payload.index() >= SYNCED.size()) {
                return;
            }
            Entity entity = mc.level.getEntity(payload.entityId());
            if (entity == null) {
                return;
            }
            Synced<T> synced = (Synced<T>) SYNCED.get(payload.index());
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.bytes()),
                    mc.level.registryAccess());
            entity.setData(synced.type(), synced.codec().decode(buf));
        }
    }
}
