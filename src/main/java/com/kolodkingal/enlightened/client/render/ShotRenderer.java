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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws what a bender shot or holds: a rock or sand as its block, turning slowly in the air; a metal blade as a
 * spinning iron sword; an ice knife as a shard; a snowball, a lava shuriken, a fire ball as the items they look like;
 * an air blade as nothing at all (its trail of cloud is the blade).
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
        poseStack.pushPose();
        switch (kind) {
            case ROCK, SAND -> {
                BlockState state = entity.blockState();
                if (state.getRenderShape() == RenderShape.MODEL) {
                    float size = kind == BendingShotEntity.Kind.SAND ? 0.7F : 0.85F;
                    poseStack.translate(0.0, 0.3, 0.0);
                    poseStack.mulPose(Axis.YP.rotationDegrees(t * (entity.held() ? 2F : 9F)));
                    poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(t * 0.1F) * 12F));
                    poseStack.scale(size, size, size);
                    poseStack.translate(-0.5, -0.5, -0.5);
                    blocks.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                }
            }
            case METAL_BLADE -> item(new ItemStack(Items.IRON_SWORD), poseStack, buffer, packedLight, entity, t * 25F, 0.9F);
            case ICE_KNIFE -> item(new ItemStack(Items.PRISMARINE_SHARD), poseStack, buffer, packedLight, entity, t * 20F, 0.8F);
            case SNOWBALL -> item(new ItemStack(Items.SNOWBALL), poseStack, buffer, packedLight, entity, 0F, 0.7F);
            case LAVA -> item(new ItemStack(Items.MAGMA_BLOCK), poseStack, buffer, packedLight, entity, t * 30F, 0.6F);
            case FIRE_BALL -> item(new ItemStack(Items.FIRE_CHARGE), poseStack, buffer, 0xF000F0, entity, t * 6F, 0.6F * entity.size());
            case AIR_BLADE -> {
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void item(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, BendingShotEntity entity, float spin, float scale) {
        poseStack.translate(0.0, 0.3, 0.0);
        // Faces the way it flies, and spins round that axis.
        float yaw = (float) Math.toDegrees(Math.atan2(-entity.getDeltaMovement().x, entity.getDeltaMovement().z));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        poseStack.scale(scale, scale, scale);
        items.renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
    }

    @Override
    public ResourceLocation getTextureLocation(BendingShotEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
