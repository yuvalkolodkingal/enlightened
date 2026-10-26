package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.air.AirBending;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Draws every airbender's air shield: a faint, pale sphere of swirling bands round him, as wide as his shield
 * reaches, drawn after the particles so it never hides them.
 */
public final class AirShieldRenderer {
    private static final int LAT = 10;
    private static final int LON = 20;

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        boolean any = false;
        for (AbstractClientPlayer player : mc.level.players()) {
            BendingData data = Benders.get(player);
            if (!AirBending.shieldUp(player, data) || player.isInvisible()) {
                continue;
            }
            any = true;
            double radius = AirBending.shieldRadius(data);
            Vec3 at = player.getPosition(partial).add(0, 1.0, 0).subtract(camera);
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            float spin = (player.tickCount + partial) * 0.02F;
            pose.mulPose(com.mojang.math.Axis.YP.rotation(spin));
            sphere(pose.last().pose(), buffer.getBuffer(RenderType.lightning()), (float) radius, player.tickCount + partial);
            pose.popPose();
        }
        if (any) {
            buffer.endBatch(RenderType.lightning());
        }
    }

    private static void sphere(Matrix4f matrix, VertexConsumer consumer, float r, float time) {
        for (int i = 0; i < LAT; i++) {
            double t0 = Math.PI * i / LAT;
            double t1 = Math.PI * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                double p0 = Math.PI * 2 * j / LON;
                double p1 = Math.PI * 2 * (j + 1) / LON;
                // Bands: every other row a little brighter, drifting with time.
                float band = (float) (0.5 + 0.5 * Math.sin(i * 1.3 + time * 0.15));
                float a = 0.05F + 0.12F * band;
                quad(matrix, consumer, r, t0, p0, t1, p1, a);
            }
        }
    }

    private static void quad(Matrix4f m, VertexConsumer c, float r, double t0, double p0, double t1, double p1, float a) {
        vertex(m, c, r, t0, p0, a);
        vertex(m, c, r, t1, p0, a);
        vertex(m, c, r, t1, p1, a);
        vertex(m, c, r, t0, p1, a);
        // Both faces, so the sphere is seen from inside too.
        vertex(m, c, r, t0, p1, a);
        vertex(m, c, r, t1, p1, a);
        vertex(m, c, r, t1, p0, a);
        vertex(m, c, r, t0, p0, a);
    }

    private static void vertex(Matrix4f m, VertexConsumer c, float r, double theta, double phi, float a) {
        float x = (float) (r * Math.sin(theta) * Math.cos(phi));
        float y = (float) (r * Math.cos(theta));
        float z = (float) (r * Math.sin(theta) * Math.sin(phi));
        c.addVertex(m, x, y, z).setColor(0.85F, 0.95F, 1.0F, a);
    }

    private AirShieldRenderer() {
    }
}
