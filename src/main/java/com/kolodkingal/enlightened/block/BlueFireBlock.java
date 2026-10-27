package com.kolodkingal.enlightened.block;

import com.kolodkingal.enlightened.fire.FireBending;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Azula's fire where it lands: a blue flame that stands wherever ordinary fire would (and on soul soil), burns whoever
 * steps in it with a blue burn, does not spread, and dies down by itself after a while.
 */
public class BlueFireBlock extends BaseFireBlock {
    public static final MapCodec<BlueFireBlock> CODEC = simpleCodec(BlueFireBlock::new);

    public BlueFireBlock(Properties properties) {
        super(properties, 2.0F);
    }

    @Override
    protected MapCodec<BlueFireBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return this.canSurvive(state, level, currentPos) ? this.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        return ground.isFaceSturdy(level, below, Direction.UP) || SoulFireBlock.canSurviveOnBlock(ground);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        level.scheduleTick(pos, this, 40 + level.random.nextInt(30));
    }

    /** It never spreads; it burns down in a minute or so. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.canSurvive(state, level, pos) || random.nextInt(3) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        level.scheduleTick(pos, this, 40 + random.nextInt(30));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity instanceof LivingEntity living && level instanceof ServerLevel server && living.isOnFire()) {
            FireBending.markBlue(server, living, 8);
        }
    }
}
