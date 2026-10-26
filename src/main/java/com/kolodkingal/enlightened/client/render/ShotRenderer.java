package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.client.fx.FxGeometry;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws what a bender shot or holds, each of its own element. Everything arrives with a pop (it grows out of nothing
 * over its first ticks with a little overshoot) and tumbles on an axis of its own: a rock or sand as its block,
 * hovering with a slow bob while held and spinning once thrown; a fire ball as a sphere of fire with a burning heart
 * and a tail of flame; an ice knife as a shard of ice; a lava shuriken as a whirling disc of lava; a metal blade as a
 * sword whirling about its line of flight; a snowball; an air blade as a crescent of compressed air.
 */
public class ShotRenderer extends EntityRenderer<BendingShotEntity> {
    private final BlockRenderDispatcher blocks;
    private final ItemRenderer items;

    public ShotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.3F;
        this.blocks = context.getBlockRenderDispatcher();
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(BendingShotEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = entity.tickCount + partialTick;
        BendingShotEntity.Kind kind = entity.kind();
        float pop = pop(t);
        int seed = entity.getId();
        poseStack.pushPose();
        poseStack.translate(0.0, 0.3, 0.0);
        switch (kind) {
            case ROCK, SAND -> {
                BlockState state = entity.blockState();
                if (state.getRenderShape() == RenderShape.MODEL) {
                    float size = (kind == BendingShotEntity.Kind.SAND ? 0.7F : 0.85F) * pop;
                    boolean held = entity.held();
                    float bob = held ? 0.06F * Mth.sin(t * 0.15F + seed) : 0.0F;
                    poseStack.translate(0.0, bob, 0.0);
                    float spin = held ? t * 2.0F + seed * 7.0F : t * 11.0F + seed * 7.0F;
                    poseStack.mulPose(Axis.YP.rotationDegrees(spin));
                    poseStack.mulPose(Axis.XP.rotationDegrees(held ? Mth.sin(t * 0.1F + seed) * 10.0F : t * 6.0F * ((seed & 1) == 0 ? 1.0F : -1.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(held ? 0.0F : (seed % 5) * 17.0F));
                    poseStack.scale(size, size, size);
                    poseStack.translate(-0.5, -0.5, -0.5);
                    blocks.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                }
            }
            case METAL_BLADE -> item(new ItemStack(Items.IRON_SWORD), poseStack, buffer, packedLight, entity, t * 28F + seed * 11F, 0.9F * pop, 0.0F);
            case SNOWBALL -> item(new ItemStack(Items.SNOWBALL), poseStack, buffer, packedLight, entity, t * 4F, 0.7F * pop, 0.0F);
            case ICE_KNIFE -> iceShard(entity, t, pop, poseStack, buffer, packedLight, seed);
            case LAVA -> lavaDisc(entity, t, pop, poseStack, buffer);
            case FIRE_BALL -> fireBall(entity, t, pop, poseStack, buffer);
            case AIR_BLADE -> airBlade(entity, t, pop, poseStack, buffer);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** The arrival: from nothing to full size over the first four ticks, with a touch of overshoot. */
    private static float pop(float t) {
        if (t >= 4.0F) {
            return 1.0F;
        }
        float u = Math.max(0.0F, t) / 4.0F;
        float v = u - 1.0F;
        return Math.max(0.05F, 1.0F + 2.70158F * v * v * v + 1.70158F * v * v);
    }

    /** Turns the pose stack to face the way the shot flies. */
    private static void face(PoseStack poseStack, BendingShotEntity entity) {
        Vec3 v = entity.getDeltaMovement();
        if (v.lengthSqr() < 1.0E-6) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        float pitch = (float) Math.toDegrees(Math.atan2(v.y, v.horizontalDistance()));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }

    private void item(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, BendingShotEntity entity, float spin, float scale, float flat) {
        face(poseStack, entity);
        if (flat != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(flat));
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        }
        poseStack.scale(scale, scale, scale);
        items.renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
    }

    /** A fire ball: a sphere of burning fire with a bright heart, flickering, and a tail of flame behind it. */
    private void fireBall(BendingShotEntity entity, float t, float pop, PoseStack poseStack, MultiBufferSource buffer) {
        boolean blue = isBlue(entity.color());
        float size = entity.size() * pop;
        float radius = 0.28F * size * (1.0F + 0.08F * Mth.sin(t * 0.9F) + 0.04F * Mth.sin(t * 2.3F + 1.0F));
        float[] fr = FxGeometry.frame(t, FxGeometry.FIRE_FRAMES, 1.0F);
        Vec3 c = Vec3.ZERO;
        Quaternionf rot = new Quaternionf().rotationY(t * 0.25F).mul(new Quaternionf().rotationX(t * 0.17F));
        FxGeometry.sphere(poseStack.last(), buffer.getBuffer(FxGeometry.fire(blue)), c, radius, 8, 14, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 0.95F, 0, 0, FxGeometry.FULL, true, rot);
        Quaternionf rot2 = new Quaternionf().rotationZ(t * 0.3F).mul(new Quaternionf().rotationY(-t * 0.2F));
        FxGeometry.sphere(poseStack.last(), buffer.getBuffer(FxGeometry.fire(blue)), c, radius * 1.25F, 6, 10, fr[1] - (fr[1] - fr[0]) * 0.5F, fr[1], 1.0F, 1.0F, 1.0F, 0.45F, 0, 0, FxGeometry.FULL, true, rot2);
        float[] core = blue ? new float[]{0.55F, 0.80F, 1.0F} : new float[]{1.0F, 0.80F, 0.35F};
        FxGeometry.glowBlob(poseStack.last(), buffer.getBuffer(FxGeometry.glowTexture()), c, radius * 3.2F, core[0], core[1], core[2], 0.75F, t * 0.3F);
        // The tail: a cone of fire trailing against the flight.
        Vec3 v = entity.getDeltaMovement();
        if (v.lengthSqr() > 1.0E-4) {
            Vec3 back = v.normalize().scale(-1.0);
            FxGeometry.cone(poseStack.last(), buffer.getBuffer(FxGeometry.fire(blue)), c, back, 1.6F * size, radius * 0.9F, 0.05F, 8, 5,
                    fr[0], fr[1], 2.0F, t * 0.2F, 1.0F, 1.0F, 1.0F, 0.8F, 0.0F, FxGeometry.FULL, true, 0.15F, t * 0.5F);
        }
    }

    private static boolean isBlue(int color) {
        int r = (color >> 16) & 0xFF;
        int b = color & 0xFF;
        return b > r + 60;
    }

    /** An ice knife: a long shard of ice, spinning about its line of flight, with a glint. */
    private void iceShard(BendingShotEntity entity, float t, float pop, PoseStack poseStack, MultiBufferSource buffer, int light, int seed) {
        face(poseStack, entity);
        poseStack.mulPose(Axis.ZP.rotationDegrees(t * 18F + seed * 11F));
        poseStack.scale(pop, pop, pop);
        List<Vec3> spine = List.of(new Vec3(0, 0, -0.5), new Vec3(0, 0, 0.0), new Vec3(0, 0, 0.5));
        FxGeometry.tube(poseStack.last(), buffer.getBuffer(FxGeometry.ice()), spine, 0.02F, 0.11F, 5, 0, 1, 1, 0, 0.85F, 0.95F, 1.0F, 0.95F, 0.95F, light, true);
        FxGeometry.tube(poseStack.last(), buffer.getBuffer(FxGeometry.ice()), List.of(new Vec3(0, 0, 0.0), new Vec3(0, 0, 0.55)), 0.11F, 0.01F, 5, 0, 1, 1, 0, 0.85F, 0.95F, 1.0F, 0.95F, 0.95F, light, true);
        FxGeometry.billboard(poseStack.last(), buffer.getBuffer(FxGeometry.glowTexture()), new Vec3(0, 0, -0.45), 0.25F, 0, 1, 0.8F, 0.95F, 1.0F, 0.6F, FxGeometry.FULL, true, t * 0.4F);
    }

    /** A lava shuriken: a whirling disc of lava, glowing, with a ring of darker crust. */
    private void lavaDisc(BendingShotEntity entity, float t, float pop, PoseStack poseStack, MultiBufferSource buffer) {
        face(poseStack, entity);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 30F));
        float[] fr = FxGeometry.frame(t, FxGeometry.LAVA_FRAMES, 0.6F);
        float r = 0.45F * pop;
        FxGeometry.disc(poseStack.last(), buffer.getBuffer(FxGeometry.lava()), Vec3.ZERO, 0.0F, r, 6, fr[0], fr[1], 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, FxGeometry.FULL, true, 0);
        FxGeometry.ringBand(poseStack.last(), buffer.getBuffer(FxGeometry.lava()), new Vec3(0, -0.03, 0), r * 0.98F, 0.06F, 6, fr[0], fr[1], 0.5F, 0.3F, 0.2F, 1.0F, FxGeometry.FULL, true, 0, 0);
        FxGeometry.glowBlob(poseStack.last(), buffer.getBuffer(FxGeometry.glowTexture()), Vec3.ZERO, r * 2.4F, 1.0F, 0.55F, 0.15F, 0.55F, t * 0.2F);
    }

    /** An air blade: a crescent of compressed air, spinning edge-on along its flight, faint and fast. */
    private void airBlade(BendingShotEntity entity, float t, float pop, PoseStack poseStack, MultiBufferSource buffer) {
        face(poseStack, entity);
        poseStack.mulPose(Axis.ZP.rotationDegrees(t * 40F));
        List<Vec3> arc = new ArrayList<>();
        for (int k = 0; k <= 12; k++) {
            double a = -Math.PI * 0.45 + Math.PI * 0.9 * k / 12;
            arc.add(new Vec3(Math.sin(a) * 0.9 * pop, 0, (Math.cos(a) - 0.6) * 0.5 * pop));
        }
        FxGeometry.tube(poseStack.last(), buffer.getBuffer(FxGeometry.light()), arc, 0.03F, 0.03F, 5, 0, 1, 1, 0, 0.85F, 0.95F, 1.0F, 0.6F, 0.6F, FxGeometry.FULL, false);
        List<Vec3> arc2 = new ArrayList<>();
        for (Vec3 p : arc) {
            arc2.add(p.scale(0.75));
        }
        FxGeometry.tube(poseStack.last(), buffer.getBuffer(FxGeometry.light()), arc2, 0.06F, 0.06F, 5, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 0.25F, 0.25F, FxGeometry.FULL, false);
    }

    @Override
    public ResourceLocation getTextureLocation(BendingShotEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
