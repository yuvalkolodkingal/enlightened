package com.kolodkingal.enlightened.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * A structure built from a plan that is a pure function of the position ({@link #design}), so each chunk builds its
 * own share: Ba Sing Se, Omashu, the spirit portals and the Tree of Time. The origin is the ground at the middle.
 */
public abstract class DesignPiece extends StructurePiece {
    protected final BlockPos origin;
    private final int clear;

    protected DesignPiece(StructurePieceType type, BlockPos origin, int clear, int up, int down) {
        super(type, 0, new BoundingBox(origin.getX() - clear, origin.getY() - down, origin.getZ() - clear,
                origin.getX() + clear, origin.getY() + up, origin.getZ() + clear));
        this.origin = origin;
        this.clear = clear;
    }

    protected DesignPiece(StructurePieceType type, CompoundTag tag, int clear) {
        super(type, tag);
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        this.clear = clear;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
    }

    /** Whether this column is part of the structure at all. */
    protected abstract boolean footprint(int dx, int dz);

    /** How high over the ground this column is built (and cleared), and how far below it. */
    protected abstract int up(int dx, int dz);

    protected int down(int dx, int dz) {
        return 0;
    }

    /** Whether the ground under this column is filled down to the land (so nothing hangs in the air). */
    protected boolean roots(int dx, int dz) {
        return true;
    }

    /** The block at this offset from the origin, or null to leave what is there. */
    protected abstract BlockState design(int dx, int dy, int dz);

    /** What the plan cannot say: chests, creatures, a gate. Called for each chunk with its box. */
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
    }

    protected BlockState footing(int x, int y, int z) {
        return (y & 3) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), ox - clear); x <= Math.min(box.maxX(), ox + clear); x++) {
            for (int z = Math.max(box.minZ(), oz - clear); z <= Math.min(box.maxZ(), oz + clear); z++) {
                int dx = x - ox;
                int dz = z - oz;
                if (!footprint(dx, dz)) {
                    continue;
                }
                int down = down(dx, dz);
                for (int dy = up(dx, dz); dy >= -down; dy--) {
                    BlockState state = design(dx, dy, dz);
                    pos.set(x, oy + dy, z);
                    if (state != null && !level.isOutsideBuildHeight(pos) && level.getBlockState(pos) != state) {
                        level.setBlock(pos, state, 2);
                    }
                }
                if (roots(dx, dz)) {
                    for (int dy = -down - 1; dy >= -down - 40; dy--) {
                        pos.set(x, oy + dy, z);
                        if (level.isOutsideBuildHeight(pos)) {
                            break;
                        }
                        BlockState cur = level.getBlockState(pos);
                        if (!(cur.canBeReplaced() || !cur.getFluidState().isEmpty())) {
                            break;
                        }
                        level.setBlock(pos, footing(x, oy + dy, z), 2);
                    }
                }
            }
        }
        decorate(level, box, random);
    }

    protected static long hash(int x, int z) {
        long h = (x * 3129871L) ^ (z * 116129781L);
        return h * h * 42317861L + h * 11L;
    }

    protected static void villager(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos at, VillagerType type) {
        if (!box.isInside(at)) {
            return;
        }
        Villager v = EntityType.VILLAGER.create(level.getLevel());
        if (v == null) {
            return;
        }
        v.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        v.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        v.setVillagerData(v.getVillagerData().setType(type));
        v.setPersistenceRequired();
        level.addFreshEntityWithPassengers(v);
    }

    protected static void golem(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos at) {
        if (!box.isInside(at)) {
            return;
        }
        var g = EntityType.IRON_GOLEM.create(level.getLevel());
        if (g != null) {
            g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            g.setPersistenceRequired();
            level.addFreshEntityWithPassengers(g);
        }
    }
}
