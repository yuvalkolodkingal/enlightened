package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.NationNpcEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Fire Nation's Royal Palace (1.0.28), a structure of its own, far from Wulong Forest: a walled courtyard entered by
 * a gate between two towers, lamp-lined paths to a raised platform and its grand stairs, and on it the great hall, red
 * walls between dark pillars, a gold band, a stepped dark roof in two tiers with golden corners and ridge. Inside, a red
 * carpet to the throne on its dais, a wall of blue flame behind it. Beneath the platform, down a ladder by the hall's
 * east wall, is the prison: a corridor and four barred cells. Guards keep the gate, the stairs and the hall.
 *
 * <p>When Ozai is beaten he is brought to his cell here ({@link #cell}); the blue fire behind the throne goes out, guards
 * stand where it burned, and Fire Lord Zuko sits on the throne ({@link #crown}).
 */
public class FirePalacePiece extends DesignPiece {
    private static final int CLEAR = 32;
    private static final int WALL = 30;
    /** The platform and the hall on it. */
    public static final int PLAT_X = 20;
    public static final int PLAT_Z0 = -26;
    public static final int PLAT_Z1 = 6;
    public static final int PLAT_H = 4;
    public static final int HALL_X = 16;
    public static final int HALL_Z0 = -24;
    public static final int HALL_Z1 = 4;
    /** The throne (its block) and the row of blue fire behind it. */
    public static final BlockPos THRONE = new BlockPos(0, PLAT_H + 3, -22);
    public static final int FIRE_Z = -23;
    /** The prison under the platform. */
    public static final int PRISON_FLOOR = -6;
    public static final BlockPos OZAI_CELL = new BlockPos(3, PRISON_FLOOR + 1, -12);
    public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE, Enlightened.id("fire_palace"));

    public FirePalacePiece(BlockPos origin) {
        super(ModStructures.FIRE_PALACE_PIECE.get(), origin, CLEAR, 30, 8);
    }

    public FirePalacePiece(CompoundTag tag) {
        super(ModStructures.FIRE_PALACE_PIECE.get(), tag, CLEAR);
    }

    public BlockPos origin() {
        return origin;
    }

    private static int ring(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    private static boolean platform(int dx, int dz) {
        return Math.abs(dx) <= PLAT_X && dz >= PLAT_Z0 && dz <= PLAT_Z1;
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        return ring(dx, dz) <= CLEAR;
    }

    @Override
    protected int up(int dx, int dz) {
        return 30;
    }

    @Override
    protected int down(int dx, int dz) {
        return platform(dx, dz) ? 8 : 1;
    }

    private static BlockState fireStep(int dy) {
        return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.FACING, Direction.SOUTH);
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int m = ring(dx, dz);
        if (m > WALL) {
            return dy <= 0 ? null : Blocks.AIR.defaultBlockState();
        }
        if (platform(dx, dz)) {
            BlockState b = palace(dx, dy, dz);
            if (b != null) {
                return b;
            }
        }
        if (dy < 0) {
            return dy == -1 ? Blocks.STONE.defaultBlockState() : null;
        }
        // The gate towers.
        if (Math.abs(ax - 5) <= 1 && Math.abs(dz - WALL) <= 1) {
            return dy <= 8 ? Blocks.NETHER_BRICKS.defaultBlockState() : dy == 9 ? Blocks.YELLOW_TERRACOTTA.defaultBlockState()
                    : dy == 10 && ax == 5 && dz == WALL ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        // The outer wall.
        if (m == WALL) {
            if (dy == 0) {
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (dz == WALL && ax <= 3 && dy <= 4) {
                return Blocks.AIR.defaultBlockState();
            }
            if (dy <= 5) {
                return (dx + dz) % 5 == 0 ? Blocks.NETHER_BRICKS.defaultBlockState() : Blocks.RED_TERRACOTTA.defaultBlockState();
            }
            if (dy == 6) {
                return Blocks.NETHER_BRICKS.defaultBlockState();
            }
            return dy == 7 && ((dx + dz) & 1) == 0 ? Blocks.NETHER_BRICK_WALL.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        // The grand stairs up to the platform.
        if (ax <= 4 && dz > PLAT_Z1 && dz <= PLAT_Z1 + PLAT_H) {
            int top = PLAT_H - (dz - PLAT_Z1 - 1);
            if (dy == 0) {
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (dy < top) {
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (dy == top) {
                return Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            }
            return Blocks.AIR.defaultBlockState();
        }
        // The courtyard: a red path from the gate, stone round it, lamps.
        if (dy == 0) {
            if (ax <= 2 && dz > PLAT_Z1) {
                return Blocks.RED_NETHER_BRICKS.defaultBlockState();
            }
            return ((dx + dz) & 1) == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
        }
        if (ax == 8 && dz > PLAT_Z1 + 2 && (dz - PLAT_Z1) % 6 == 2) {
            return dy <= 2 ? Blocks.NETHER_BRICK_FENCE.defaultBlockState() : dy == 3 ? Blocks.LANTERN.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    /** The platform, the hall on it, its roofs, and the prison beneath. */
    @Nullable
    private static BlockState palace(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        if (dy < PRISON_FLOOR) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (dy < 0) {
            return prison(dx, dy, dz);
        }
        // The ladder from the prison up into the hall, by its east wall.
        if (dx == HALL_X - 1 && dz == -7 && dy <= PLAT_H + 1) {
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        }
        boolean inHall = ax <= HALL_X && dz >= HALL_Z0 && dz <= HALL_Z1;
        if (dy <= PLAT_H) {
            boolean rim = ax == PLAT_X || dz == PLAT_Z0 || dz == PLAT_Z1;
            return rim ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                    : dy == PLAT_H ? (inHall ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState())
                    : Blocks.STONE_BRICKS.defaultBlockState();
        }
        int h = dy - PLAT_H;
        if (!inHall) {
            // The terrace round the hall: a railing along the platform's edge (open at the stairs), and the eaves above.
            boolean edge = ax == PLAT_X || dz == PLAT_Z0 || dz == PLAT_Z1;
            if (h == 1 && edge && !(dz == PLAT_Z1 && ax <= 4)) {
                return Blocks.NETHER_BRICK_FENCE.defaultBlockState();
            }
            return roof(dx, dy, dz);
        }
        boolean wall = ax == HALL_X || dz == HALL_Z0 || dz == HALL_Z1;
        if (h <= 8) {
            if (wall) {
                if (dz == HALL_Z1 && ax <= 2 && h <= 5) {
                    return Blocks.AIR.defaultBlockState();
                }
                boolean pillar = (dz == HALL_Z0 || dz == HALL_Z1) ? ax % 4 == 0 : (dz - HALL_Z0) % 4 == 0;
                if (h == 1) {
                    return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                }
                if (h == 8) {
                    return pillar ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.YELLOW_TERRACOTTA.defaultBlockState();
                }
                if (pillar) {
                    return Blocks.NETHER_BRICKS.defaultBlockState();
                }
                return h >= 3 && h <= 5 && dz != HALL_Z0 ? Blocks.RED_STAINED_GLASS.defaultBlockState() : Blocks.RED_TERRACOTTA.defaultBlockState();
            }
            return hall(dx, h, dz);
        }
        return roof(dx, dy, dz);
    }

    /** Inside the hall (h: height over its floor). */
    private static BlockState hall(int dx, int h, int dz) {
        int ax = Math.abs(dx);
        if (dx == HALL_X - 1 && dz == -7 && h == 1) {
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        }
        // The dais in two steps, the throne, and the wall of blue fire behind it.
        if (dz >= HALL_Z0 + 1 && dz <= -19 && ax <= 6 && h == 1) {
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (dz >= HALL_Z0 + 1 && dz <= -21 && ax <= 3 && h == 2) {
            return dz == FIRE_Z && dx != 0 ? fireStep(h) : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (dz == FIRE_Z && ax <= 6 && ax > 3 && h == 2) {
            return fireStep(h);
        }
        if (dx == 0 && dz == THRONE.getZ() && h == 3) {
            return Blocks.RED_NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        }
        if (dx == 0 && dz == FIRE_Z && h <= 4 && h >= 3) {
            return Blocks.RED_NETHER_BRICKS.defaultBlockState();
        }
        if (h == 1 && ax <= 1 && dz > -19) {
            return Blocks.RED_CARPET.defaultBlockState();
        }
        if (h == 7 && (ax == 8 || ax == 0) && (dz - HALL_Z0) % 8 == 4) {
            return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        }
        return Blocks.AIR.defaultBlockState();
    }

    /** The stepped roof in two tiers, with golden corners and ridge. */
    private static BlockState roof(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int h = dy - PLAT_H;
        // Lower tier: eaves at h 9 (two over the walls), stepping in.
        if (h >= 9 && h <= 13) {
            int in = (h - 9) * 2;
            int x = HALL_X + 2 - in;
            int z0 = HALL_Z0 - 2 + in;
            int z1 = HALL_Z1 + 2 - in;
            if (ax <= x && dz >= z0 && dz <= z1) {
                boolean corner = h == 9 && ax == x && (dz == z0 || dz == z1);
                return corner ? Blocks.YELLOW_TERRACOTTA.defaultBlockState() : Blocks.NETHER_BRICKS.defaultBlockState();
            }
            if (h == 10 && ax == HALL_X + 2 && (dz == HALL_Z0 - 2 || dz == HALL_Z1 + 2)) {
                return Blocks.GOLD_BLOCK.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        // Upper tier: a smaller hall on top, its own eaves and steps, the golden ridge and spire.
        if (h >= 14 && h <= 16 && ax <= 8 && dz >= -16 && dz <= -4) {
            boolean wall = ax == 8 || dz == -16 || dz == -4;
            if (!wall) {
                return Blocks.AIR.defaultBlockState();
            }
            return ax % 4 == 0 && (dz == -16 || dz == -4) || (ax == 8 && (dz + 16) % 4 == 0) ? Blocks.NETHER_BRICKS.defaultBlockState()
                    : Blocks.RED_TERRACOTTA.defaultBlockState();
        }
        if (h >= 17 && h <= 20) {
            int in = (h - 17) * 2;
            int x = 10 - in;
            int z0 = -18 + in;
            int z1 = -2 - in;
            if (ax <= x && dz >= z0 && dz <= z1) {
                boolean corner = h == 17 && ax == x && (dz == z0 || dz == z1);
                return corner ? Blocks.YELLOW_TERRACOTTA.defaultBlockState() : Blocks.NETHER_BRICKS.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        if (h == 21 && ax <= 4 && dz == -10) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if ((h == 22 || h == 23) && dx == 0 && dz == -10) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    /** Under the platform: a corridor along x, four barred cells to its north, the ladder's foot in the east. */
    private static BlockState prison(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        boolean ladder = dx == HALL_X - 1 && dz == -7;
        if (dy == PRISON_FLOOR) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (ladder) {
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        }
        if (dy == -2) {
            return (ax == 6 || ax == 0) && dz == -7 ? Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true)
                    : Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (dy == -1) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        // dy -5..-3: the rooms.
        if (dz >= -8 && dz <= -6 && ax <= HALL_X - 1) {
            return Blocks.AIR.defaultBlockState();
        }
        if (dz == -9 && ax <= 12) {
            return ax == 0 || ax == 6 || ax == 12 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState();
        }
        if (dz >= -15 && dz <= -10 && ax <= 12) {
            if (ax == 0 || ax == 6 || ax == 12) {
                return Blocks.STONE_BRICKS.defaultBlockState();
            }
            if (dy == PRISON_FLOOR + 1 && dz == -15 && (dx == 2 || dx == -4 || dx == 8 || dx == -10)) {
                return Blocks.CHAIN.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static void guard(WorldGenLevel level, BoundingBox box, BlockPos at, float yaw) {
        if (!box.isInside(at)) {
            return;
        }
        NationNpcEntity g = ModEntities.NATION_NPC.get().create(level.getLevel());
        if (g != null) {
            g.kind(NationNpcEntity.Kind.GUARD);
            g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
            g.setYHeadRot(yaw);
            level.addFreshEntityWithPassengers(g);
        }
    }

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        for (int side : new int[]{-3, 3}) {
            guard(level, box, origin.offset(side, 1, WALL - 2), 0.0F);
            guard(level, box, origin.offset(side * 2, 1, PLAT_Z1 + PLAT_H + 2), 0.0F);
            guard(level, box, origin.offset(side, PLAT_H + 1, HALL_Z1 - 2), 0.0F);
        }
    }

    // ---- Ozai's fate ----

    /** The origin of the Fire Palace nearest a place (searching far), or null if the world has none near. */
    @Nullable
    public static BlockPos find(ServerLevel level, BlockPos from) {
        BlockPos found = level.findNearestMapStructure(TAG, from, 100, false);
        if (found == null) {
            return null;
        }
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(Enlightened.id("fire_palace"));
        ChunkAccess chunk = level.getChunk(found.getX() >> 4, found.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = structure == null ? null : chunk.getStartForStructure(structure);
        if (start == null || !start.isValid()) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof FirePalacePiece palace) {
                return palace.origin();
            }
        }
        return null;
    }

    /** The cell where Ozai is held, in the palace whose origin this is. */
    public static BlockPos cell(BlockPos origin) {
        return origin.offset(OZAI_CELL);
    }

    /**
     * The Fire Lord is beaten: the blue fire behind the throne goes out, guards stand where it burned, and Fire Lord Zuko
     * takes the throne (once).
     */
    public static void crown(ServerLevel level, BlockPos origin) {
        BlockPos throne = origin.offset(THRONE);
        level.getChunk(throne);
        List<NationNpcEntity> there = level.getEntitiesOfClass(NationNpcEntity.class, new net.minecraft.world.phys.AABB(throne).inflate(3),
                n -> n.kind() == NationNpcEntity.Kind.ZUKO);
        if (!there.isEmpty()) {
            return;
        }
        for (int dx = -6; dx <= 6; dx++) {
            for (int up = 1; up <= 3; up++) {
                BlockPos pos = origin.offset(dx, PLAT_H + up, FIRE_Z);
                if (level.getBlockState(pos).is(Blocks.SOUL_CAMPFIRE)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int dx : new int[]{-5, -2, 2, 5}) {
            int floor = PLAT_H + 2;
            NationNpcEntity g = ModEntities.NATION_NPC.get().create(level);
            g.kind(NationNpcEntity.Kind.GUARD);
            g.moveTo(origin.getX() + dx + 0.5, origin.getY() + floor + 0.0, origin.getZ() + FIRE_Z + 0.5, 0.0F, 0.0F);
            g.setYHeadRot(0.0F);
            level.addFreshEntity(g);
        }
        NationNpcEntity zuko = ModEntities.NATION_NPC.get().create(level);
        zuko.kind(NationNpcEntity.Kind.ZUKO);
        zuko.moveTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5, 0.0F, 0.0F);
        zuko.setYHeadRot(0.0F);
        level.addFreshEntity(zuko);
    }
}
