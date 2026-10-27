package com.kolodkingal.enlightened.client.cosmic;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.client.fx.FxGeometry;
import com.kolodkingal.enlightened.client.render.MeditatingBodyModel;
import com.kolodkingal.enlightened.world.Cosmic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

/**
 * The cosmic plane, drawn on the client: the bender's own self sitting vast in the lotus at the end of the path, a
 * violet, see-through likeness of his own skin with the arrow and the eyes alight (as Aang saw himself), wrapped in a purple glow,
 * and a few great clouds of violet light far off among the stars.
 */
public final class CosmicRenderer {
    private static final ResourceLocation EYES = Enlightened.id("textures/entity/avatar_eyes.png");
    private static final ResourceLocation ARROWS = Enlightened.id("textures/entity/avatar_arrows.png");
    private static final float SCALE = 32.0F;
    private static MeditatingBodyModel<AbstractClientPlayer> model;

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimension().equals(Cosmic.LEVEL)) {
            return;
        }
        if (model == null) {
            model = new MeditatingBodyModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER));
        }
        AbstractClientPlayer self = mc.player;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float age = self.tickCount + partial;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = FxGeometry.source();

        // Entity shaders read the lightmap, which is off by this stage of the frame: on for the giant, or he is black.
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        ps.pushPose();
        float breath = 0.15F * Mth.sin(age * 0.03F);
        // The model's origin is its neck: set there, the head rises above it and the body in the lotus sits below,
        // the seat (three quarters of a model block down) a few blocks over the path.
        double neck = Cosmic.Y + 4.0 + 0.75 * SCALE;
        ps.translate(Cosmic.GIANT.x - cam.x, neck - cam.y + breath, Cosmic.GIANT.z - cam.z);
        ps.scale(SCALE, SCALE, SCALE);
        ps.scale(-1.0F, -1.0F, 1.0F);
        model.setupAnim(self, 0.0F, 0.0F, 1000.0F + age, 0.0F, 0.0F);
        int full = FxGeometry.FULL;
        ResourceLocation skin = self.getSkin().texture();
        // The body: his own skin in a cosmic violet, a little see-through.
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucent(skin)), full, OverlayTexture.NO_OVERLAY, 0xD8B494F0);
        // The arrow and the eyes, alight.
        float pulse = 0.75F + 0.25F * Mth.sin(age * 0.08F);
        int glow = ((int) (255 * pulse) << 24) | 0xC8D8FF;
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.eyes(ARROWS)), full, OverlayTexture.NO_OVERLAY, glow);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.eyes(EYES)), full, OverlayTexture.NO_OVERLAY, glow);
        ps.popPose();
        buffers.endBatch();
        mc.gameRenderer.lightTexture().turnOffLightLayer();

        // The purple glow round him, and clouds of violet light far off.
        MultiBufferSource.BufferSource fx = buffers;
        VertexConsumer light = fx.getBuffer(FxGeometry.light());
        PoseStack.Pose pose = ps.last();
        Vec3 heart = new Vec3(Cosmic.GIANT.x, Cosmic.Y + 4.0 + 0.6 * SCALE, Cosmic.GIANT.z).subtract(cam);
        FxGeometry.sphere(pose, light, heart, SCALE * 1.05F, 10, 20, 0, 1, 0.55F, 0.3F, 0.95F, 0.06F, 0.0F, 0.0F, full, false, new Quaternionf());
        FxGeometry.sphere(pose, light, heart, SCALE * 1.4F, 10, 20, 0, 1, 0.45F, 0.2F, 0.85F, 0.035F, 0.0F, 0.0F, full, false, new Quaternionf());
        double[][] clouds = {{-90, 60, 140}, {110, 40, 60}, {-60, -20, -80}, {70, 90, -40}, {0, 120, 220}};
        for (int i = 0; i < clouds.length; i++) {
            Vec3 c = new Vec3(clouds[i][0], Cosmic.Y + clouds[i][1], clouds[i][2]).subtract(cam);
            FxGeometry.billboard(pose, light, c, 70.0F + 20.0F * i, 0, 1, 0.5F, 0.25F, 0.9F, 0.08F, full, false, age * 0.001F + i);
            FxGeometry.billboard(pose, light, c, 30.0F + 8.0F * i, 0, 1, 0.75F, 0.5F, 1.0F, 0.07F, full, false, -age * 0.002F + i);
        }
        fx.endBatch();
    }

    private CosmicRenderer() {
    }
}
