package com.kolodkingal.enlightened.client.render;

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

/**
 * Draws what a bender shot or holds. Everything arrives with a pop (it grows out of nothing over its first ticks
 * with a little overshoot) and tumbles on an axis of its own: a rock or sand as its block, hovering with a slow bob
 * while held and spinning faster once thrown; a metal blade as a sword whirling about its line of flight; an ice
 * knife as a shard; a snowball; a lava shuriken as a spinning magma disc; a fire ball pulsing with its heat; an air
 * blade as nothing at all (its trail of cloud is the blade).
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
                    // Held: a slow turn and a lazy wobble. Thrown: a tumble about an axis of its own.
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
            case ICE_KNIFE -> item(new ItemStack(Items.PRISMARINE_SHARD), poseStack, buffer, packedLight, entity, t * 22F + seed * 11F, 0.8F * pop, 0.0F);
            case SNOWBALL -> item(new ItemStack(Items.SNOWBALL), poseStack, buffer, packedLight, entity, t * 4F, 0.7F * pop, 0.0F);
            case LAVA -> item(new ItemStack(Items.MAGMA_BLOCK), poseStack, buffer, packedLight, entity, t * 34F, 0.6F * pop, 90.0F);
            case FIRE_BALL -> {
                // A flicker of heat: the ball swells and shrinks a little, faster the longer it has flown.
                float pulse = 1.0F + 0.08F * Mth.sin(t * 0.9F) + 0.04F * Mth.sin(t * 2.3F + 1.0F);
                item(new ItemStack(Items.FIRE_CHARGE), poseStack, buffer, 0xF000F0, entity, t * 8F, 0.6F * entity.size() * pop * pulse, 0.0F);
            }
            case AIR_BLADE -> {
            }
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

    private void item(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, BendingShotEntity entity, float spin, float scale, float flat) {
        // Faces the way it flies, and spins round that axis.
        Vec3 v = entity.getDeltaMovement();
        float yaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        float pitch = (float) Math.toDegrees(Math.atan2(v.y, v.horizontalDistance()));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        if (flat != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(flat));
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        }
        poseStack.scale(scale, scale, scale);
        items.renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
    }

    @Override
    public ResourceLocation getTextureLocation(BendingShotEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
