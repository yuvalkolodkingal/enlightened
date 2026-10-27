package com.kolodkingal.enlightened.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A low stone mat with a lotus: a bender who meditates on it opens his chakras three times as fast. Right-click it
 * and you sit down on it in the lotus and begin to meditate (as with the Meditate key), until you move.
 */
public class MeditationPointBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public MeditationPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                                                  net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        // CONSUME, not SUCCESS: a success swings the arm, and a swing ends a meditation.
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) {
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        if (!com.kolodkingal.enlightened.bending.Benders.get(server).element().bends()) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        // Sit on the mat: in its middle, still, then meditate.
        server.teleportTo(pos.getX() + 0.5, pos.getY() + 0.125, pos.getZ() + 0.5);
        server.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        server.setOnGround(true);
        com.kolodkingal.enlightened.bending.Chakras.meditate(server, true);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
