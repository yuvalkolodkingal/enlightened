package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.client.fx.FxGeometry;
import com.kolodkingal.enlightened.entity.FireWallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A wall of fire: a curtain of burning fire three blocks high, rising when it is raised and sinking as it dies. */
public class FireWallRenderer extends EntityRenderer<FireWallEntity> {
    public FireWallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(FireWallEntity wall, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = wall.tickCount + partialTick;
        float life = wall.life() + partialTick;
        float rise = Mth.clamp(life / 8.0F, 0.0F, 1.0F);
        float fade = Mth.clamp((wall.lifetime() - life) / 15.0F, 0.0F, 1.0F);
        float height = 3.0F * rise * (0.6F + 0.4F * fade);
        float half = wall.width() * 0.5F;
        Vec3 along = wall.along();
        float[] fr = FxGeometry.frame(t, FxGeometry.FIRE_FRAMES, 1.0F);
        VertexConsumer fire = buffer.getBuffer(FxGeometry.fire(wall.colour()));
        if (wall.colour() == FxGeometry.FIRE_RAINBOW) {
            fire = FxGeometry.rainbow(fire, wall.getX(), wall.getY(), wall.getZ(), t);
        }
        // Three layers of flame with a flicker of their own, so the curtain looks deep and alive.
        for (int layer = -1; layer <= 1; layer++) {
            Vec3 off = new Vec3(-along.z, 0, along.x).scale(layer * 0.18);
            int segs = Math.max(4, (int) (wall.width() * 2));
            for (int i = 0; i < segs; i++) {
                float a0 = -half + wall.width() * i / segs;
                float a1 = -half + wall.width() * (i + 1) / segs;
                float h0 = height * (0.85F + 0.15F * Mth.sin(t * 0.5F + i * 1.7F + layer));
                float h1 = height * (0.85F + 0.15F * Mth.sin(t * 0.5F + (i + 1) * 1.7F + layer));
                Vec3 b0 = along.scale(a0).add(off);
                Vec3 b1 = along.scale(a1).add(off);
                List<Vec3> spine = List.of(b0.add(0, 0.05, 0), b1.add(0, 0.05, 0));
                float v0 = fr[0];
                float v1 = fr[1];
                // A sheet: a flat tube of two sides from the ground up (drawn as a ribbon of two edges).
                // Each segment carries a whole flame of the texture (a slice would be a stripe of one colour).
                sheet(fire, poseStack, b0, b1, h0, h1, 0.0F, 1.0F, v0, v1, 0.9F * fade, layer == 0 ? 1.0F : 0.6F);
            }
        }
        // The show's wall rolls over at the top like a breaking wave: tongues of flame curl along the crest.
        Vec3 cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 base = wall.getPosition(partialTick);
        int tongues = Math.max(3, (int) (wall.width() * 1.5F));
        for (int i = 0; i < tongues; i++) {
            float u = -half + wall.width() * (i + 0.5F) / tongues;
            float h = height * (0.95F + 0.25F * Mth.sin(t * 0.4F + i * 2.3F));
            Vec3 at = along.scale(u).add(Mth.sin(t * 0.3F + i) * 0.15, h, Mth.cos(t * 0.35F + i * 1.3F) * 0.15);
            FxGeometry.billboardTowards(poseStack.last(), fire, at, cam.subtract(base.add(at)), 0.9F + 0.3F * Mth.sin(t * 0.6F + i), fr[0], fr[1],
                    1.0F, 1.0F, 1.0F, 0.85F * fade, FxGeometry.FULL, true, t * 0.2F + i);
        }
        // The glow goes on another buffer, so it is asked for only once the flames are drawn.
        VertexConsumer light = buffer.getBuffer(FxGeometry.light());
        sheet(light, poseStack, along.scale(-half), along.scale(half), height * 0.5F, height * 0.5F, 0, 1, 0, 1, 0.35F * fade, 1.0F);
        super.render(wall, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void sheet(VertexConsumer vc, PoseStack ps, Vec3 b0, Vec3 b1, float h0, float h1, float u0, float u1, float v0, float v1, float a, float bright) {
        PoseStack.Pose pose = ps.last();
        float r = bright;
        // Both faces.
        quad(vc, pose, b0, b1, b1.add(0, h1, 0), b0.add(0, h0, 0), u0, u1, v0, v1, r, a);
        quad(vc, pose, b1, b0, b0.add(0, h0, 0), b1.add(0, h1, 0), u1, u0, v0, v1, r, a);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1, float v0, float v1, float bright, float alpha) {
        boolean textured = true;
        vertex(vc, pose, a, u0, v1, bright, alpha);
        vertex(vc, pose, b, u1, v1, bright, alpha);
        vertex(vc, pose, c, u1, v0, bright, alpha * 0.2F);
        vertex(vc, pose, d, u0, v0, bright, alpha * 0.2F);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, float bright, float alpha) {
        vc.addVertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).setColor(bright, bright, bright, alpha).setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FxGeometry.FULL).setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(FireWallEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
