package com.kolodkingal.enlightened.block;

import com.kolodkingal.enlightened.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What a red cube of the Anomaly keeps: nothing; it is there to be drawn, as a small Anomaly of energy. */
public class AnomalyCubeBlockEntity extends BlockEntity {
    public AnomalyCubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ANOMALY_CUBE.get(), pos, state);
    }
}
