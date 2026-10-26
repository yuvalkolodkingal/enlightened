package com.kolodkingal.enlightened.network;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.client.fx.WorldFx;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: one effect of bending to draw ({@link com.kolodkingal.enlightened.fx.Fx}). */
public record FxPayload(int kind, double x, double y, double z, double tx, double ty, double tz, int color, float data, int entity)
        implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(Enlightened.id("fx"));

    public static final StreamCodec<ByteBuf, FxPayload> STREAM_CODEC = StreamCodec.of(FxPayload::write, FxPayload::read);

    private static void write(ByteBuf buf, FxPayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.kind);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeDouble(p.tx);
        buf.writeDouble(p.ty);
        buf.writeDouble(p.tz);
        buf.writeInt(p.color);
        buf.writeFloat(p.data);
        ByteBufCodecs.VAR_INT.encode(buf, p.entity + 1);
    }

    private static FxPayload read(ByteBuf buf) {
        int kind = ByteBufCodecs.VAR_INT.decode(buf);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double tx = buf.readDouble();
        double ty = buf.readDouble();
        double tz = buf.readDouble();
        int color = buf.readInt();
        float data = buf.readFloat();
        int entity = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        return new FxPayload(kind, x, y, z, tx, ty, tz, color, data, entity);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(FxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WorldFx.add(payload));
    }
}
