package com.kolodkingal.enlightened.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 26.3: the game has no buffer source any more (everything is submitted to a {@code SubmitNodeCollector} and drawn
 * later in the frame). The code written against 1.21.1 still draws into one: this one records what is drawn into it,
 * per render type, and submits each recording as custom geometry to the collector of the submit being answered, which
 * replays it when the frame is drawn. The vertices are recorded as they are given (already moved by the code's own
 * pose), so they are replayed with no pose of their own.
 */
public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType type);

    /** 1.21.1's source with fixed buffers per type (here every source keeps one recording per type). */
    static BufferSource immediateWithBuffers(java.util.SequencedMap<RenderType, ?> fixed, @Nullable Object shared) {
        return new BufferSource(null);
    }

    /** A source that submits to a collector: its own, or the one {@link RenderBridge} is answering now. */
    class BufferSource implements MultiBufferSource {
        @Nullable
        private final SubmitNodeCollector own;
        private final Map<RenderType, Recorder> open = new HashMap<>();

        public BufferSource(@Nullable SubmitNodeCollector collector) {
            this.own = collector;
        }

        /** The collector this source submits to (null: nothing is drawn). */
        @Nullable
        public SubmitNodeCollector collector() {
            return own != null ? own : RenderBridge.current();
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            SubmitNodeCollector collector = collector();
            Recorder recorder = open.get(type);
            if (recorder == null || recorder.target != collector || recorder.replayed) {
                recorder = new Recorder(collector);
                open.put(type, recorder);
                if (collector != null) {
                    collector.submitCustomGeometry(new PoseStack(), type, recorder);
                }
            }
            return recorder;
        }

        /** 1.21.1's end of a batch: what was drawn so far is submitted; the next vertices start new recordings. */
        public void endBatch() {
            open.clear();
        }

        public void endBatch(RenderType type) {
            open.remove(type);
        }

        public void endLastBatch() {
            open.clear();
        }
    }

    /** What was drawn for one render type, replayed into the frame's buffer when the collector draws it. */
    final class Recorder implements VertexConsumer, SubmitNodeCollector.CustomGeometryRenderer {
        private static final int VERTEX = 0, COLOR = 1, UV = 2, UV1 = 3, UV2 = 4, UV3 = 5, NORMAL = 6, LINE_WIDTH = 7, COLOR_ARGB = 8;
        @Nullable
        final SubmitNodeCollector target;
        boolean replayed;
        private int[] ops = new int[256];
        private int size;

        Recorder(@Nullable SubmitNodeCollector target) {
            this.target = target;
        }

        private void put(int op, int a, int b, int c, int d) {
            if (size + 5 > ops.length) {
                ops = Arrays.copyOf(ops, ops.length * 2);
            }
            ops[size++] = op;
            ops[size++] = a;
            ops[size++] = b;
            ops[size++] = c;
            ops[size++] = d;
        }

        private static int f(float x) {
            return Float.floatToRawIntBits(x);
        }

        private static float f(int x) {
            return Float.intBitsToFloat(x);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            put(VERTEX, f(x), f(y), f(z), 0);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            put(COLOR, r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            put(COLOR_ARGB, argb, 0, 0, 0);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            put(UV, f(u), f(v), 0, 0);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            put(UV1, u, v, 0, 0);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            put(UV2, u, v, 0, 0);
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            put(UV3, f(u), f(v), 0, 0);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            put(NORMAL, f(x), f(y), f(z), 0);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            put(LINE_WIDTH, f(width), 0, 0, 0);
            return this;
        }

        @Override
        public void render(PoseStack.Pose pose, VertexConsumer out) {
            replayed = true;
            int[] o = ops;
            for (int i = 0; i < size; i += 5) {
                switch (o[i]) {
                    case VERTEX -> out.addVertex(f(o[i + 1]), f(o[i + 2]), f(o[i + 3]));
                    case COLOR -> out.setColor(o[i + 1], o[i + 2], o[i + 3], o[i + 4]);
                    case COLOR_ARGB -> out.setColor(o[i + 1]);
                    case UV -> out.setUv(f(o[i + 1]), f(o[i + 2]));
                    case UV1 -> out.setUv1(o[i + 1], o[i + 2]);
                    case UV2 -> out.setUv2(o[i + 1], o[i + 2]);
                    case UV3 -> out.setUv3(f(o[i + 1]), f(o[i + 2]));
                    case NORMAL -> out.setNormal(f(o[i + 1]), f(o[i + 2]), f(o[i + 3]));
                    case LINE_WIDTH -> out.setLineWidth(f(o[i + 1]));
                    default -> {
                    }
                }
            }
        }
    }
}
