package com.kolodkingal.enlightened.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

/** 26.3: a block drawn by the mod (1.21.1's {@code renderSingleBlock}) is resolved and submitted. */
public final class BlockDraw {
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver resolver;

    public BlockDraw(BlockModelResolver resolver) {
        this.resolver = resolver;
    }

    public void renderSingleBlock(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        SubmitNodeCollector collector = buffers instanceof MultiBufferSource.BufferSource source ? source.collector() : RenderBridge.current();
        if (collector == null) {
            return;
        }
        BlockModelRenderState model = new BlockModelRenderState();
        resolver.update(model, state, CONTEXT);
        model.submit(pose, collector, light, overlay, 0);
    }
}
