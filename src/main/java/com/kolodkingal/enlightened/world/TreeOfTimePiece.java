package com.kolodkingal.enlightened.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Tree of Time in the Spirit World, where Aang meditated: a vast pale tree on great roots, a hollow between them
 * at its foot, golden lights in its pale wood, and a crown of blossom lit gold from within, with lights hanging in it.
 */
public class TreeOfTimePiece extends DesignPiece {
    private static final int CLEAR = 16;
    private static final int CROWN_Y = 34;

    public TreeOfTimePiece(BlockPos origin) {
        super(ModStructures.TREE_OF_TIME_PIECE.get(), origin, CLEAR, 46, 2);
    }

    public TreeOfTimePiece(CompoundTag tag) {
        super(ModStructures.TREE_OF_TIME_PIECE.get(), tag, CLEAR);
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        return dx * dx + dz * dz <= CLEAR * CLEAR;
    }

    @Override
    protected int up(int dx, int dz) {
        return 46;
    }

    @Override
    protected boolean roots(int dx, int dz) {
        return dx * dx + dz * dz <= 36;
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        double r = Math.sqrt(dx * dx + dz * dz);
        double a = Math.atan2(dz, dx);
        BlockState wood = Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState();
        // The trunk, tapering, its foot widened by six great roots that run out over the ground.
        double trunk = 4.0 - dy * 0.07;
        double root = 0.0;
        for (int i = 0; i < 6; i++) {
            double ra = i * Math.PI / 3 + 0.3;
            double diff = Math.abs(Math.atan2(Math.sin(a - ra), Math.cos(a - ra)));
            root = Math.max(root, (1.0 - diff / 0.35) * (11.0 - dy * 3.0));
        }
        if (dy >= -2 && dy <= 30 && (r <= trunk || r <= root && dy <= 3)) {
            // The hollow at its foot, facing south.
            if (dz > 0 && Math.abs(dx) <= 1 && dy >= 0 && dy <= 3 && r < trunk + 0.5) {
                return Blocks.AIR.defaultBlockState();
            }
            return (hash(dx * 3 + dy, dz) & 15) == 0 ? Blocks.OCHRE_FROGLIGHT.defaultBlockState() : wood;
        }
        // Boughs from the top of the trunk into the crown.
        if (dy >= 22 && dy <= CROWN_Y) {
            for (int i = 0; i < 5; i++) {
                double ba = i * Math.PI * 2 / 5;
                double t = (dy - 22) / 12.0;
                double bx = Math.cos(ba) * t * 9;
                double bz = Math.sin(ba) * t * 9;
                if ((dx - bx) * (dx - bx) + (dz - bz) * (dz - bz) <= 2.2) {
                    return wood;
                }
            }
        }
        // The crown: a broad dome of leaves, lit gold from within, lights hanging under it.
        double cy = (dy - CROWN_Y) / 9.0;
        double cr = r / 15.0;
        double d = cr * cr + cy * cy;
        long h = hash(dx * 5 + dy * 13, dz * 7 - dy);
        if (d <= 1.0 && d >= 0.45) {
            int roll = (int) ((h >>> 12) % 16);
            if (roll == 0) {
                return Blocks.OCHRE_FROGLIGHT.defaultBlockState();
            }
            BlockState leaves = roll < 4 ? Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState() : Blocks.CHERRY_LEAVES.defaultBlockState();
            return leaves.setValue(LeavesBlock.PERSISTENT, true);
        }
        if (d < 0.45 && cy < 0 && (h & 63) == 0) {
            return Blocks.END_ROD.defaultBlockState();
        }
        if (dy == 1 && r > 6 && r < CLEAR && (h & 7) == 0) {
            return Blocks.GLOW_LICHEN.defaultBlockState().setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.DOWN), true);
        }
        return null;
    }
}
