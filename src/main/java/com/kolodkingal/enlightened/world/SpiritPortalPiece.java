package com.kolodkingal.enlightened.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A spirit portal in the forest: a gate of old mossy stone with the shimmering light of the Spirit World between its
 * pillars ({@link SpiritWorld#gate}), in a clearing ringed by six standing stones, moss and flowers at their feet.
 */
public class SpiritPortalPiece extends DesignPiece {
    private static final int CLEAR = 10;

    public SpiritPortalPiece(BlockPos origin) {
        super(ModStructures.SPIRIT_PORTAL_PIECE.get(), origin, CLEAR, 9, 1);
    }

    public SpiritPortalPiece(CompoundTag tag) {
        super(ModStructures.SPIRIT_PORTAL_PIECE.get(), tag, CLEAR);
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        return dx * dx + dz * dz <= CLEAR * CLEAR;
    }

    @Override
    protected int up(int dx, int dz) {
        return 9;
    }

    /** The standing stone at a column, by its index, or -1. */
    private static int stone(int dx, int dz) {
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + Math.PI / 6;
            if (Math.round(Math.cos(a) * 7.5) == dx && Math.round(Math.sin(a) * 7.5) == dz) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        double r = Math.sqrt(dx * dx + dz * dz);
        int s = stone(dx, dz);
        if (dy == 0) {
            // The clearing's floor: moss and grass, a path of old stone through the gate.
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 9 || s >= 0) {
                return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            }
            return (hash(dx, dz) & 3) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (s >= 0) {
            int h = 2 + s % 3;
            return dy <= h ? (dy == h ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.ANDESITE.defaultBlockState()) : Blocks.AIR.defaultBlockState();
        }
        if (dy == 1 && r > 3.5 && r < 9 && (hash(dx * 7, dz * 5) & 7) == 0) {
            long h = hash(dx, dz * 3) >>> 8;
            return switch ((int) (h % 4)) {
                case 0 -> Blocks.ALLIUM.defaultBlockState();
                case 1 -> Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
                case 2 -> Blocks.BLUE_ORCHID.defaultBlockState();
                default -> Blocks.FERN.defaultBlockState();
            };
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        if (box.isInside(origin)) {
            SpiritWorld.gate(level, origin, Direction.Axis.X, false);
        }
    }
}
