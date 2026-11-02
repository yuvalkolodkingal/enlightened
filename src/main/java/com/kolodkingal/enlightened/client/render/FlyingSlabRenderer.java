package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.entity.FlyingSlabEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** A flying wall or slab of land: each of its blocks drawn as the block it is. */
public class FlyingSlabRenderer extends EntityRenderer<FlyingSlabEntity> {
    private final BlockRenderDispatcher blocks;

    public FlyingSlabRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.0F;
    }

    @Override
    public void render(FlyingSlabEntity slab, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        for (FlyingSlabEntity.Piece p : slab.pieces()) {
            pose.pushPose();
            pose.translate(p.rel().getX() - 0.5, p.rel().getY(), p.rel().getZ() - 0.5);
            blocks.renderSingleBlock(p.state(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        super.render(slab, yaw, partial, pose, buffers, light);
    }

    @Override
    public boolean shouldRender(FlyingSlabEntity slab, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(slab.getBoundingBoxForCulling());
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(FlyingSlabEntity slab) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
