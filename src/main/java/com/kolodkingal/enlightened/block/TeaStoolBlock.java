package com.kolodkingal.enlightened.block;

import com.kolodkingal.enlightened.entity.SeatEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A tea house stool (1.0.23): right-click to sit on it (and eat and drink there); jump or sneak to stand up. */
public class TeaStoolBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 7, 2, 14, 9, 14), Block.box(3, 0, 3, 5, 7, 5), Block.box(11, 0, 3, 13, 7, 5),
            Block.box(3, 0, 11, 5, 7, 13), Block.box(11, 0, 11, 13, 7, 13));

    public TeaStoolBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isPassenger() || player.isShiftKeyDown() || player.distanceToSqr(pos.getCenter()) > 9.0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (SeatEntity.taken(level, pos)) {
                return InteractionResult.CONSUME;
            }
            SeatEntity seat = ModEntities.SEAT.get().create(level);
            seat.moveTo(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, player.getYRot(), 0);
            level.addFreshEntity(seat);
            player.startRiding(seat);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
