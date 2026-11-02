package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.entity.GreatSpiritEntity;
import com.kolodkingal.enlightened.quest.AvatarQuest;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * Raava's glade: a ring of eight white pillars, each crowned with a pale soul lantern, round a still pool, a circle of
 * mossy stone between them; Raava hovers over the pool, waiting for one to help her (on the quest path, while the world
 * has room for an Avatar: else the glade stands empty).
 */
public class RaavaGladePiece extends DesignPiece {
    private static final int CLEAR = 9;

    public RaavaGladePiece(BlockPos origin) {
        super(ModStructures.RAAVA_GLADE_PIECE.get(), origin, CLEAR, 12, 2);
    }

    public RaavaGladePiece(CompoundTag tag) {
        super(ModStructures.RAAVA_GLADE_PIECE.get(), tag, CLEAR);
    }

    static double r(int dx, int dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        return r(dx, dz) <= CLEAR + 0.5;
    }

    @Override
    protected int up(int dx, int dz) {
        return 12;
    }

    @Override
    protected int down(int dx, int dz) {
        return 2;
    }

    /** Whether a column is one of the eight pillars. */
    static boolean pillar(int dx, int dz) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            if (Math.round(Math.cos(a) * 6.5) == dx && Math.round(Math.sin(a) * 6.5) == dz) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        double r = r(dx, dz);
        if (dy < -1) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (r <= 2.3) {
            if (dy == -1) {
                return Blocks.CLAY.defaultBlockState();
            }
            if (dy == 0) {
                return Blocks.WATER.defaultBlockState();
            }
            return dy == 1 && dx == 1 && dz == 0 ? Blocks.LILY_PAD.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (dy == -1) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (dy == 0) {
            if (r <= 3.5) {
                return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            }
            return Math.abs(r - 6.5) <= 0.6 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (pillar(dx, dz)) {
            if (dy <= 3) {
                return Blocks.QUARTZ_PILLAR.defaultBlockState();
            }
            return dy == 4 ? Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false) : Blocks.AIR.defaultBlockState();
        }
        if (dy == 1 && r > 3.5 && r < 6 && (hash(dx, dz) & 3) == 0) {
            return (hash(dz, dx) & 1) == 0 ? Blocks.LILY_OF_THE_VALLEY.defaultBlockState() : Blocks.AZURE_BLUET.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        BlockPos at = origin.above(3);
        if (box.isInside(at) && AvatarQuest.questPath()) {
            level.addFreshEntityWithPassengers(GreatSpiritEntity.raavaAt(level.getLevel(), Vec3.atBottomCenterOf(at)));
        }
    }
}
