package com.kolodkingal.enlightened.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A red cube of the Anomaly: not stone but a small Anomaly of pure energy hung in the air (drawn by its block entity),
 * as solid as a block to whoever strikes it. It cannot be broken; striking it wakes the Anomaly itself.
 */
public class AnomalyBlock extends BaseEntityBlock {
    public static final MapCodec<AnomalyBlock> CODEC = simpleCodec(AnomalyBlock::new);

    public AnomalyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnomalyCubeBlockEntity(pos, state);
    }
}
