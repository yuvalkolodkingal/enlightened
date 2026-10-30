package com.kolodkingal.enlightened.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Omashu, King Bumi's city: a mountain of stone terraces rising out of a deep chasm, reached only by one long bridge.
 * Each terrace is a ring street behind a parapet, lined with tall stone houses under stepped green roofs; a grand
 * stairway is cut up the mountain's face from the bridge to the summit, where the palace stands, a stepped green
 * crown with a golden tip. A river runs at the bottom of the chasm.
 */
public class OmashuPiece extends DesignPiece {
    /** The terraces' outer radii and their surfaces' heights: the stairway climbs a block for each block in. */
    public static final int[] R = {44, 36, 28, 20, 12, 6};
    public static final int[] H = {2, 10, 18, 26, 34, 40};
    public static final int CHASM = 76;
    public static final int DEEP = 40;
    private static final int CLEAR = 81;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState PAVE = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState WALL = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState ROOF = Blocks.GREEN_TERRACOTTA.defaultBlockState();

    public OmashuPiece(BlockPos origin) {
        super(ModStructures.OMASHU_PIECE.get(), origin, CLEAR, 56, DEEP + 2);
    }

    public OmashuPiece(CompoundTag tag) {
        super(ModStructures.OMASHU_PIECE.get(), tag, CLEAR);
    }

    static double r(int dx, int dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** The terrace a radius is on, or -1 outside the mountain. */
    public static int tier(double r) {
        int k = -1;
        for (int i = 0; i < R.length; i++) {
            if (r <= R[i] + 0.5) {
                k = i;
            }
        }
        return k;
    }

    static boolean stairway(int dx, int dz) {
        double r = r(dx, dz);
        return Math.abs(dx) <= 2 && dz > 0 && r <= R[0] + 0.5 && r > R[5] + 0.5;
    }

    static boolean bridge(int dx, int dz) {
        return Math.abs(dx) <= 2 && dz > R[0] && dz <= CLEAR - 1;
    }

    /** The mountain's surface at a column (the stairway's step there), or the bridge deck. */
    public static int surface(int dx, int dz) {
        double r = r(dx, dz);
        if (stairway(dx, dz)) {
            return Math.min(H[5], (int) Math.floor(2 + R[0] - r));
        }
        int k = tier(r);
        return k < 0 ? 2 : H[k];
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        double r = r(dx, dz);
        return r <= CHASM + 0.5 || bridge(dx, dz) && Math.abs(dx) <= 3;
    }

    @Override
    protected int up(int dx, int dz) {
        return r(dx, dz) <= R[0] + 1 ? 56 : 30;
    }

    @Override
    protected int down(int dx, int dz) {
        return DEEP + 2;
    }

    @Override
    protected boolean roots(int dx, int dz) {
        return false;
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        double r = r(dx, dz);
        if (r > R[0] + 0.5) {
            // The chasm, the river at its bottom, and the bridge over it.
            if (bridge(dx, dz)) {
                double ramp = r > CHASM - 2 ? Math.max(0, 2 - (r - (CHASM - 2))) : 2;
                int deck = (int) Math.round(ramp);
                if (dy == deck) {
                    return Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                            net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
                }
                if (dy == deck - 1) {
                    return WALL;
                }
                if (Math.abs(dx) == 2 && dy == deck + 1) {
                    return Blocks.STONE_BRICK_WALL.defaultBlockState();
                }
                if (dy > deck) {
                    return AIR;
                }
                // Arches under the deck every twelve blocks.
                if (dz % 12 == 0 && dy < deck - 1) {
                    return Blocks.STONE.defaultBlockState();
                }
            }
            if (Math.abs(dx) == 3 && bridge(dx, dz) && r > CHASM) {
                return null;
            }
            if (r > CHASM + 0.5) {
                return null;
            }
            if (dy <= -DEEP - 1) {
                return Blocks.STONE.defaultBlockState();
            }
            if (dy == -DEEP) {
                return Blocks.WATER.defaultBlockState();
            }
            return AIR;
        }
        int top = surface(dx, dz);
        int k = tier(r);
        if (dy < top) {
            // The rock of the mountain, faced in dressed stone along each terrace's edge.
            boolean face = k >= 0 && r > R[k] - 1.5;
            long h = hash(dx * 3 + dy, dz);
            if (face && dy > top - 6) {
                return WALL;
            }
            return (h & 7) == 0 ? Blocks.ANDESITE.defaultBlockState() : (h & 7) == 1 ? Blocks.TUFF.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (stairway(dx, dz)) {
            if (dy == top) {
                return Math.abs(dx) == 2 ? WALL : Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            }
            return AIR;
        }
        if (dy == top) {
            return (hash(dx, dz) & 7) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : PAVE;
        }
        // The parapet along each terrace's edge.
        if (dy == top + 1 && k >= 0 && r > R[k] - 1.0 && !(Math.abs(dx) <= 3 && dz > 0)) {
            return Blocks.STONE_BRICK_WALL.defaultBlockState();
        }
        if (k == 5) {
            return palace(dx, dy - top, dz);
        }
        int[] lot = lot(dx, dz, k);
        if (lot != null) {
            BlockState b = house(lot, dx - lot[0], dy - top, dz - lot[1], k);
            if (b != null) {
                return b;
            }
        }
        return AIR;
    }

    /** The house on a terrace nearest a column: its centre and its hash; none on the stairway's line. */
    static int[] lot(int dx, int dz, int k) {
        if (k < 0 || k >= 4) {
            return null;
        }
        double rm = (R[k] + R[k + 1]) / 2.0;
        int n = Math.max(6, (int) (2 * Math.PI * rm / 9));
        double a = Math.atan2(dz, dx);
        if (a < 0) {
            a += 2 * Math.PI;
        }
        int i = (int) (a / (2 * Math.PI / n));
        double ca = (i + 0.5) * 2 * Math.PI / n;
        int cx = (int) Math.round(Math.cos(ca) * rm);
        int cz = (int) Math.round(Math.sin(ca) * rm);
        if (Math.abs(cx) <= 6 && cz > 0) {
            return null;
        }
        return new int[]{cx, cz, (int) (hash(k * 31 + i, n) >>> 16)};
    }

    /** A tall stone house under a stepped green roof, a door toward the street. */
    private static BlockState house(int[] lot, int rx, int ry, int rz, int k) {
        int ax = Math.abs(rx);
        int az = Math.abs(rz);
        int h = lot[2];
        int height = 4 + Math.floorMod(h, 4) + (k == 0 ? 0 : 1);
        int half = 2;
        if (ry < height) {
            if (ax > half || az > half) {
                return null;
            }
            boolean edge = ax == half || az == half;
            if (!edge) {
                return AIR;
            }
            if (ax == half && az == half) {
                return Blocks.POLISHED_ANDESITE.defaultBlockState();
            }
            if (ry <= 2 && ry >= 1 && (rx == 0 || rz == 0) && Math.floorMod(h >> 3, 4) == (rx > 0 ? 0 : rx < 0 ? 1 : rz > 0 ? 2 : 3)) {
                return AIR;
            }
            if (ry == 3 && (rx == 0 || rz == 0)) {
                return Blocks.GLASS.defaultBlockState();
            }
            return ry == height - 1 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState();
        }
        int s = ry - height;
        if (s <= 3 && ax <= 3 - s && az <= 3 - s) {
            return ROOF;
        }
        if (s == 4 && ax == 0 && az == 0) {
            return Blocks.EMERALD_BLOCK.defaultBlockState();
        }
        return null;
    }

    /** King Bumi's palace on the summit: walls of stone, a green band, a stepped green crown, a golden tip. */
    private static BlockState palace(int dx, int ry, int dz) {
        int e = Math.max(Math.abs(dx), Math.abs(dz));
        if (ry <= 0) {
            return AIR;
        }
        if (ry <= 6) {
            if (e > 4) {
                return AIR;
            }
            if (e < 4) {
                return AIR;
            }
            if (dz == 4 && Math.abs(dx) <= 1 && ry <= 3) {
                return AIR;
            }
            return ry == 4 ? ROOF : Blocks.SMOOTH_STONE.defaultBlockState();
        }
        int s = ry - 7;
        if (s <= 4 && e <= 5 - s) {
            return ROOF;
        }
        if (s >= 5 && s <= 6 && e == 0) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        return AIR;
    }

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        for (int k = 0; k < 5; k++) {
            int rr = R[k] - 2;
            villager(level, box, random, origin.offset(-rr, H[k] + 1, 0), VillagerType.PLAINS);
            villager(level, box, random, origin.offset(rr, H[k] + 1, 0), VillagerType.PLAINS);
            villager(level, box, random, origin.offset(0, H[k] + 1, -rr), VillagerType.PLAINS);
        }
        golem(level, box, random, origin.offset(0, 3, R[0] + 3));
        // Chests in some of the houses.
        for (int k = 0; k < 4; k++) {
            double rm = (R[k] + R[k + 1]) / 2.0;
            int n = Math.max(6, (int) (2 * Math.PI * rm / 9));
            for (int i = 0; i < n; i += 3) {
                double ca = (i + 0.5) * 2 * Math.PI / n;
                int cx = (int) Math.round(Math.cos(ca) * rm);
                int cz = (int) Math.round(Math.sin(ca) * rm);
                if (Math.abs(cx) <= 6 && cz > 0) {
                    continue;
                }
                createChest(level, box, random, origin.offset(cx + 1, H[k] + 1, cz + 1), BaSingSePiece.LOOT_HOUSE, null);
            }
        }
    }
}
