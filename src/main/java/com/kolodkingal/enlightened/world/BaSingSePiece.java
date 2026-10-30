package com.kolodkingal.enlightened.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Ba Sing Se, the great city of the Earth Kingdom: three ring walls one inside the other, each with gates on four
 * streets. The Outer Wall, 26 high with a walk behind its crenels, gate towers under green roofs and the Earth Kingdom's
 * emblem over each gate; inside it the Lower Ring of close mud-brick houses; the Middle Wall, and the Middle Ring's
 * sandstone houses round a canal crossed by the streets' bridges; the Inner Wall, capped in green, and the Upper Ring
 * of fine houses and gardens; and at the heart, on a raised platform, the Earth King's palace, its two halls under
 * stepped green roofs trimmed in gold, the throne within. No monster comes inside its walls, and no wither or other
 * boss can be raised there (see {@code CommonEvents}).
 */
public class BaSingSePiece extends DesignPiece {
    public static final ResourceKey<LootTable> LOOT_HOUSE = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/village/village_plains_house"));
    public static final int OUT_IN = 96, OUT = 100, OUT_H = 26;
    public static final int MID_IN = 60, MID = 62, MID_H = 14;
    public static final int IN_IN = 30, IN = 32, IN_H = 12;
    public static final int PAL = 18, PAL_H = 4;
    public static final int CANAL_IN = 44, CANAL_OUT = 46;
    private static final int LOT = 9;
    private static final int CLEAR = 102;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState BAND = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState WALK = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState ROOF = Blocks.GREEN_TERRACOTTA.defaultBlockState();
    private static final BlockState TRIM = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
    private static final BlockState SAND = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState POST = Blocks.DARK_OAK_LOG.defaultBlockState();
    private static final BlockState STREET = Blocks.POLISHED_ANDESITE.defaultBlockState();

    public BaSingSePiece(BlockPos origin) {
        super(ModStructures.BA_SING_SE_PIECE.get(), origin, CLEAR, 38, 2);
    }

    public BaSingSePiece(CompoundTag tag) {
        super(ModStructures.BA_SING_SE_PIECE.get(), tag, CLEAR);
    }

    public static int ring(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    private static int minor(int dx, int dz) {
        return Math.min(Math.abs(dx), Math.abs(dz));
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        return ring(dx, dz) <= CLEAR;
    }

    @Override
    protected int up(int dx, int dz) {
        int m = ring(dx, dz);
        return m >= OUT_IN - 6 ? 38 : m <= PAL ? 30 : 18;
    }

    @Override
    protected int down(int dx, int dz) {
        return canal(dx, dz) ? 2 : 0;
    }

    private static boolean canal(int dx, int dz) {
        int m = ring(dx, dz);
        return m >= CANAL_IN && m <= CANAL_OUT && minor(dx, dz) > 3;
    }

    public static boolean street(int dx, int dz) {
        int m = ring(dx, dz);
        return minor(dx, dz) <= 3 && m > PAL && m < OUT_IN;
    }

    /** The gate tower a column stands in (its centre), or null: two by each gate of the Outer Wall. */
    private static int[] tower(int dx, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        int major = Math.max(ax, az);
        int minor = Math.min(ax, az);
        if (Math.abs(major - 98) <= 3 && Math.abs(minor - 7) <= 3) {
            return new int[]{major - 98, minor - 7};
        }
        return null;
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        int m = ring(dx, dz);
        int minor = minor(dx, dz);
        int[] tw = tower(dx, dz);
        if (tw != null) {
            return tower(tw[0], tw[1], dy);
        }
        if (m >= OUT_IN && m <= OUT) {
            return outerWall(dx, dy, dz, m, minor);
        }
        if (m > OUT) {
            return null;
        }
        if (dy < 0) {
            return canal(dx, dz) ? (dy == -1 ? Blocks.WATER.defaultBlockState() : STONE) : null;
        }
        if (m >= MID_IN && m <= MID) {
            return ringWall(dy, m == MID, minor, MID_H, 2, 6, STONE, BAND, WALK, (dx + dz & 1) == 0 ? STONE : AIR);
        }
        if (m >= IN_IN && m <= IN) {
            return ringWall(dy, m == IN, minor, IN_H, 2, 5, SAND, ROOF, ROOF, (dx + dz & 1) == 0 ? ROOF : AIR);
        }
        if (m <= PAL) {
            return palace(dx, dy, dz, m, minor);
        }
        if (canal(dx, dz)) {
            return dy == 0 ? Blocks.WATER.defaultBlockState() : AIR;
        }
        if ((m == CANAL_IN - 1 || m == CANAL_OUT + 1) && minor > 3) {
            return dy == 0 ? STONE : dy == 1 ? Blocks.STONE_BRICK_WALL.defaultBlockState() : AIR;
        }
        if (street(dx, dz)) {
            return dy == 0 ? (minor == 0 ? STONE : STREET) : AIR;
        }
        if (minor == 4 && m % 10 == 0) {
            return dy == 0 ? STREET : dy <= 2 ? Blocks.STONE_BRICK_WALL.defaultBlockState() : dy == 3 ? Blocks.LANTERN.defaultBlockState() : AIR;
        }
        House h = houseAt(dx, dz);
        if (h != null) {
            BlockState b = house(h, dx - h.x, dy, dz - h.z);
            if (b != null) {
                return b;
            }
        }
        if (dy == 0) {
            if (m > MID) {
                return (hash(dx, dz) & 3) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
            }
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return AIR;
    }

    /** The Outer Wall: stone, banded, a walk behind crenels, gates on the streets with the emblem over them. */
    private static BlockState outerWall(int dx, int dy, int dz, int m, int minor) {
        if (dy < 0) {
            return null;
        }
        if (minor <= 3 && dy <= 9 && dy >= 1) {
            return AIR;
        }
        if (dy == 0) {
            return minor <= 3 ? STREET : STONE;
        }
        if (dy < OUT_H) {
            if (m == OUT && minor <= 5 && dy >= 12 && dy <= 22) {
                double d = Math.sqrt(minor * minor + (dy - 17) * (dy - 17));
                if (d <= 1.6) {
                    return Blocks.GOLD_BLOCK.defaultBlockState();
                }
                if (d <= 3.6) {
                    return Blocks.GREEN_CONCRETE.defaultBlockState();
                }
                if (d <= 5.0) {
                    return TRIM;
                }
            }
            if (dy % 6 == 0) {
                return BAND;
            }
            return (hash(dx * 3 + dy, dz) & 15) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : STONE;
        }
        if (dy == OUT_H) {
            return WALK;
        }
        if (dy == OUT_H + 1) {
            if (m == OUT) {
                return (dx + dz & 1) == 0 ? STONE : AIR;
            }
            if (m == OUT_IN) {
                return Blocks.STONE_BRICK_WALL.defaultBlockState();
            }
        }
        return AIR;
    }

    private static BlockState ringWall(int dy, boolean outer, int minor, int height, int gate, int gateH, BlockState wall, BlockState band,
                                       BlockState top, BlockState crenel) {
        if (minor <= gate && dy >= 1 && dy <= gateH) {
            return AIR;
        }
        if (dy == 0) {
            return minor <= gate ? STREET : wall;
        }
        if (dy < height) {
            return dy == height / 2 ? band : wall;
        }
        if (dy == height) {
            return top;
        }
        if (dy == height + 1 && outer) {
            return crenel;
        }
        return AIR;
    }

    /** A gate tower: square, stone, banded, under a stepped green roof with a golden tip. */
    private static BlockState tower(int a, int b, int dy) {
        int e = Math.max(Math.abs(a), Math.abs(b));
        if (dy == 0) {
            return STONE;
        }
        if (dy <= 30) {
            if (e > 2) {
                return AIR;
            }
            if (e < 2) {
                return dy % 10 == 0 ? WALK : AIR;
            }
            if (dy % 6 == 0) {
                return BAND;
            }
            return dy % 8 == 4 && (a == 0 || b == 0) ? Blocks.IRON_BARS.defaultBlockState() : STONE;
        }
        int k = dy - 31;
        if (k <= 3 && e <= 3 - k) {
            return k == 0 && e == 3 ? TRIM : ROOF;
        }
        if (dy == 35 && e == 0) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        return AIR;
    }

    /** The Earth King's palace on its platform: stairs up from the four streets, two halls under stepped green roofs. */
    private static BlockState palace(int dx, int dy, int dz, int m, int minor) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (dy == 0) {
            return STONE;
        }
        // The platform, with stairs climbing to it from each street.
        if (dy <= PAL_H) {
            if (minor <= 3 && m >= PAL - 3) {
                int step = PAL + 1 - m;
                if (dy < step) {
                    return STONE;
                }
                if (dy == step) {
                    Direction in = ax >= az ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, in);
                }
                return AIR;
            }
            return dy == PAL_H ? WALK : STONE;
        }
        if (dy == PAL_H + 1 && m == PAL && minor > 3) {
            return Blocks.STONE_BRICK_WALL.defaultBlockState();
        }
        // The great hall.
        if (ax <= 12 && az <= 8 && dy <= 13) {
            boolean edge = ax == 12 || az == 8;
            if (!edge) {
                if (dy == 5 && dx == 0 && dz == -6) {
                    return Blocks.GOLD_BLOCK.defaultBlockState();
                }
                if (dz == -7 && dx == 0 && dy <= 7) {
                    return Blocks.GOLD_BLOCK.defaultBlockState();
                }
                if ((ax == 8 || ax == 4) && az == 5) {
                    return POST;
                }
                return AIR;
            }
            if (az == 8 && dz > 0 && ax <= 1 && dy <= 8) {
                return AIR;
            }
            if (ax % 4 == 0 && az == 8 || az % 4 == 0 && ax == 12 || ax == 12 && az == 8) {
                return POST;
            }
            if (dy >= 8 && dy <= 10 && (ax % 4 == 2 || az % 4 == 2)) {
                return Blocks.GLASS.defaultBlockState();
            }
            return dy == 13 ? TRIM : SAND;
        }
        // Its roof, stepping in, trimmed in gold at the eaves.
        if (dy >= 14 && dy <= 17) {
            int k = dy - 14;
            if (ax <= 14 - 2 * k && az <= 10 - 2 * k) {
                return k == 0 && (ax == 14 || az == 10) ? TRIM : ROOF;
            }
        }
        // The upper hall and its roof, a golden ridge on top.
        if (ax <= 6 && az <= 4 && dy >= 18 && dy <= 21) {
            boolean edge = ax == 6 || az == 4;
            return !edge ? AIR : (dy == 20 && (ax % 2 == 0 && az == 4) ? Blocks.GLASS.defaultBlockState() : SAND);
        }
        if (dy >= 22 && dy <= 25) {
            int k = dy - 22;
            if (ax <= 8 - 2 * k && az <= 6 - 2 * k) {
                return k == 0 && (ax == 8 || az == 6) ? TRIM : ROOF;
            }
        }
        if (dy == 26 && ax <= 2 && dz == 0) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        return AIR;
    }

    record House(int x, int z, int hw, int hd, int h, int ring, boolean garden, boolean chest, boolean doorX, int doorSide) {
    }

    /** The house on a lot, or null where none stands (on a street, a wall, the canal, the palace's ground). */
    static House house(int lx, int lz) {
        int cx = lx * LOT;
        int cz = lz * LOT;
        int m = ring(cx, cz);
        int ringOf = m > MID + 1 && m < OUT_IN - 1 ? 0 : m > IN + 1 && m < MID_IN - 1 ? 1 : m > PAL + 1 && m < IN_IN - 1 ? 2 : -1;
        if (ringOf < 0) {
            return null;
        }
        for (int a = -3; a <= 3; a += 3) {
            for (int b = -3; b <= 3; b += 3) {
                int x = cx + a;
                int z = cz + b;
                int mm = ring(x, z);
                int r2 = mm > MID && mm < OUT_IN ? 0 : mm > IN && mm < MID_IN ? 1 : mm > PAL && mm < IN_IN ? 2 : -1;
                if (r2 != ringOf || minor(x, z) <= 5 || mm >= CANAL_IN - 2 && mm <= CANAL_OUT + 2 || minor(x, z) == 4) {
                    return null;
                }
            }
        }
        long h = hash(lx * 11 + 5, lz * 17 + 3);
        int hw = ringOf == 0 ? 2 : 2 + (int) (h & 1);
        int hd = ringOf == 0 ? 2 : 2 + (int) (h >>> 1 & 1);
        int height = ringOf == 0 ? 3 + (int) ((h >>> 2) % 2) : ringOf == 1 ? 4 + (int) ((h >>> 2) % 3) : 5;
        boolean garden = ringOf == 2 ? (h >>> 4) % 2 == 0 : (h >>> 4) % 9 == 0;
        boolean chest = (h >>> 8) % 4 == 0;
        boolean doorX = Math.abs(cx) >= Math.abs(cz);
        int side = doorX ? -Integer.signum(cx) : -Integer.signum(cz);
        return new House(cx, cz, hw, hd, height, ringOf, garden, chest, doorX, side == 0 ? 1 : side);
    }

    private static House houseAt(int dx, int dz) {
        return house(Math.floorDiv(dx + LOT / 2, LOT), Math.floorDiv(dz + LOT / 2, LOT));
    }

    private static BlockState house(House h, int rx, int dy, int rz) {
        int ax = Math.abs(rx);
        int az = Math.abs(rz);
        if (h.garden) {
            return tree(rx, dy, rz);
        }
        boolean in = ax <= h.hw && az <= h.hd;
        if (dy == 0) {
            return in ? Blocks.SPRUCE_PLANKS.defaultBlockState() : null;
        }
        if (dy < h.h) {
            if (!in) {
                return null;
            }
            boolean ex = ax == h.hw;
            boolean ez = az == h.hd;
            if (!ex && !ez) {
                return AIR;
            }
            if (ex && ez) {
                return POST;
            }
            if (dy <= 2 && (h.doorX ? ex && rx == h.doorSide * h.hw && rz == 0 : ez && rz == h.doorSide * h.hd && rx == 0)) {
                return AIR;
            }
            int along = ex ? rz : rx;
            if (dy == 2 && along != 0 && (along & 1) == 0) {
                return Blocks.GLASS.defaultBlockState();
            }
            return h.ring == 0 ? Blocks.MUD_BRICKS.defaultBlockState() : SAND;
        }
        // The roof: green tiles stepping in from eaves past the walls; the Upper Ring's ridges in gold.
        int k = dy - h.h;
        int rw = h.hw + 1 - k;
        int rd = h.hd + 1 - k;
        if (rw >= 0 && rd >= 0 && ax <= rw && az <= rd) {
            if (h.ring == 2 && (rw == 0 || rd == 0)) {
                return TRIM;
            }
            return ROOF;
        }
        return null;
    }

    private static BlockState tree(int rx, int dy, int rz) {
        int ax = Math.abs(rx);
        int az = Math.abs(rz);
        if (dy == 0) {
            return ax <= 3 && az <= 3 ? Blocks.GRASS_BLOCK.defaultBlockState() : null;
        }
        if (rx == 0 && rz == 0 && dy <= 4) {
            return Blocks.OAK_LOG.defaultBlockState();
        }
        int d = ax * ax + az * az + (dy - 5) * (dy - 5);
        if (dy >= 4 && dy <= 7 && d <= 6) {
            return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        }
        if (dy == 1 && ax <= 3 && az <= 3 && (hash(rx, rz) & 3) == 0) {
            return Blocks.PEONY.defaultBlockState();
        }
        return null;
    }

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int ox = origin.getX();
        int oz = origin.getZ();
        for (int lx = Math.floorDiv(box.minX() - ox + LOT / 2, LOT); lx <= Math.floorDiv(box.maxX() - ox + LOT / 2, LOT); lx++) {
            for (int lz = Math.floorDiv(box.minZ() - oz + LOT / 2, LOT); lz <= Math.floorDiv(box.maxZ() - oz + LOT / 2, LOT); lz++) {
                House h = house(lx, lz);
                if (h != null && !h.garden && h.chest) {
                    createChest(level, box, random, origin.offset(h.x + h.hw - 1, 1, h.z + h.hd - 1), LOOT_HOUSE, null);
                }
            }
        }
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            for (int dist : new int[]{24, 40, 52, 70, 86}) {
                villager(level, box, random, origin.offset(d[0] * dist + d[1] * 2, 1, d[1] * dist + d[0] * 2), VillagerType.PLAINS);
            }
            golem(level, box, random, origin.offset(d[0] * 92, 1, d[1] * 92));
            golem(level, box, random, origin.offset(d[0] * 28, 1, d[1] * 28));
        }
        villager(level, box, random, origin.offset(0, PAL_H + 1, 3), VillagerType.PLAINS);
    }
}
