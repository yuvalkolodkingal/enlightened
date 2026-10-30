package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.entity.CityGuardEntity;
import com.kolodkingal.enlightened.entity.KingBumiEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Omashu, King Bumi's city, as the show draws it: a mountain of sandstone rising out of a deep chasm, its cliff ribbed
 * with pointed buttresses, reached only by one long bridge on tall arches. A wall rings the city at the cliff's edge,
 * with a gate where the bridge comes in. Above it the terraces climb, each a ring street behind a parapet lined with
 * sandstone houses and towers under green pagoda roofs; a grand stairway is cut up the mountain's face from the gate to
 * the summit, where a great stepped pyramid stands, gabled on its four sides. Inside it is the throne hall, and on
 * the throne sits King Bumi. Guards keep the gate, the stairway, the terraces and the hall.
 */
public class OmashuPiece extends DesignPiece {
    /** The terraces' outer radii and their surfaces' heights: the stairway climbs a block for each block in. */
    public static final int[] R = {60, 52, 44, 36, 28, 20};
    public static final int[] H = {2, 10, 18, 26, 34, 42};
    public static final int CHASM = 86;
    public static final int DEEP = 40;
    /** The summit pyramid's half width at its base, and the throne hall's half width inside it. */
    public static final int PYRAMID = 14;
    public static final int HALL = 8;
    /** Where the throne's seat is (offsets from the origin). */
    public static final BlockPos THRONE = new BlockPos(0, 44, -7);
    private static final int CLEAR = 91;
    private static final int UP = 66;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState SMOOTH = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState CUT = Blocks.CUT_SANDSTONE.defaultBlockState();
    private static final BlockState STONE = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_SANDSTONE.defaultBlockState();
    private static final BlockState ROCK = Blocks.TERRACOTTA.defaultBlockState();
    private static final BlockState WINDOW = Blocks.BROWN_TERRACOTTA.defaultBlockState();
    private static final BlockState ROOF = Blocks.GREEN_TERRACOTTA.defaultBlockState();
    private static final BlockState RAIL = Blocks.SANDSTONE_WALL.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState HUNG = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

    public OmashuPiece(BlockPos origin) {
        super(ModStructures.OMASHU_PIECE.get(), origin, CLEAR, UP, DEEP + 2);
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
        return r(dx, dz) <= CHASM + 0.5 || bridge(dx, dz);
    }

    @Override
    protected int up(int dx, int dz) {
        return r(dx, dz) <= R[0] + 5 ? UP : 30;
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
            return outside(dx, dy, dz, r);
        }
        int top = surface(dx, dz);
        int k = tier(r);
        if (dy < top) {
            return rock(dx, dy, dz, r, k, top);
        }
        if (stairway(dx, dz)) {
            if (dy == top) {
                return Math.abs(dx) == 2 ? CUT : Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            }
            if (Math.abs(dx) == 2 && dy == top + 1) {
                return RAIL;
            }
            BlockState w = wall(dx, dy, dz, r);
            return w != null ? w : AIR;
        }
        if (dy == top) {
            if (k == 5 && Math.max(Math.abs(dx), Math.abs(dz)) <= HALL) {
                return (dx + dz & 1) == 0 ? CUT : SMOOTH;
            }
            return (hash(dx, dz) & 7) == 0 ? CUT : SMOOTH;
        }
        if (k == 0) {
            BlockState w = wall(dx, dy, dz, r);
            if (w != null) {
                return w;
            }
        } else if (dy == top + 1 && r > R[k] - 1.0 && !(Math.abs(dx) <= 3 && dz > 0)) {
            // The parapet along each terrace's edge.
            return RAIL;
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

    /** The chasm, the river at its bottom, the buttresses of the cliff, and the bridge on its arches. */
    private static BlockState outside(int dx, int dy, int dz, double r) {
        if (bridge(dx, dz)) {
            int deck = r > CHASM ? Math.max(0, 2 - (int) ((r - CHASM) / 2)) : 2;
            if (dy == deck) {
                return SMOOTH;
            }
            if (Math.abs(dx) == 2 && dy == deck + 1) {
                return RAIL;
            }
            int u = Math.floorMod(dz - R[0], 12);
            if (Math.abs(dx) == 2 && dy == deck + 2 && u == 0) {
                return LANTERN;
            }
            if (dy > deck) {
                return AIR;
            }
            if (r > CHASM) {
                return dy == deck - 1 ? CUT : null;
            }
            // Tall arches: a pier every twelve blocks down to the river, a round arch between.
            if (u <= 1) {
                return dy == -DEEP + 1 || dy == deck - 1 ? CUT : SMOOTH;
            }
            double span = u - 6.5;
            double soffit = deck - 7 + Math.sqrt(Math.max(0, 25 - span * span));
            if (dy >= soffit) {
                return dy == deck - 1 ? CUT : SMOOTH;
            }
        }
        if (r > CHASM + 0.5) {
            return null;
        }
        if (dy <= -DEEP - 1) {
            return STONE;
        }
        if (dy == -DEEP) {
            return Blocks.WATER.defaultBlockState();
        }
        if (buttress(dx, dy, dz, r)) {
            return SMOOTH;
        }
        return AIR;
    }

    /**
     * The cliff's buttresses: ribs standing out from the rock every ten blocks round, as deep as three blocks under
     * the wall, narrowing to points far down the cliff.
     */
    static boolean buttress(int dx, int dy, int dz, double r) {
        double out = r - R[0];
        if (out <= 0.5 || out > 3.5 || dy > 1) {
            return false;
        }
        double f = dy >= -6 ? 1.0 : (dy + 28) / 22.0;
        if (f <= 0) {
            return false;
        }
        double a = Math.atan2(dz, dx);
        double s = a * R[0];
        double off = Math.abs(Math.floorMod((int) Math.round(s), 10) - 5);
        return out <= 0.5 + 3 * f && off <= 2 * f;
    }

    /** The rock of the mountain, faced in sandstone along each terrace's edge, with dark windows. */
    private static BlockState rock(int dx, int dy, int dz, double r, int k, int top) {
        long h = hash(dx * 3 + dy, dz);
        if (k >= 0 && r > R[k] - 1.5) {
            if (dy > top - 9) {
                if (dy == top - 1) {
                    return CUT;
                }
                int s = (int) Math.round(Math.atan2(dz, dx) * R[k]);
                if ((dy == top - 4 || dy == top - 5) && Math.floorMod(s, 6) == 0) {
                    return WINDOW;
                }
                return SMOOTH;
            }
            if (k == 0 && dy < 2) {
                // The cliff over the chasm: sandstone and the darker clay, in bands.
                return Math.floorMod(dy, 7) < 2 ? CUT : (h & 3) == 0 ? ROCK : SMOOTH;
            }
        }
        return (h & 3) == 0 ? ROCK : STONE;
    }

    /** The city wall on the cliff's edge, battlemented, with the gate where the bridge comes in. */
    private static BlockState wall(int dx, int dy, int dz, double r) {
        if (r <= R[0] - 1.5 || dy <= 2) {
            return null;
        }
        boolean gate = Math.abs(dx) <= 2 && dz > 0;
        if (gate) {
            if (dy <= 6 || dy == 7 && Math.abs(dx) <= 1) {
                return AIR;
            }
            if (dy <= 8) {
                return CHISELED;
            }
        }
        if (dy <= 7) {
            return dy == 7 ? CUT : SMOOTH;
        }
        if (dy == 8) {
            int s = (int) Math.round(Math.atan2(dz, dx) * R[0]);
            return Math.floorMod(s, 2) == 0 ? SMOOTH : AIR;
        }
        return null;
    }

    /** The house on a terrace nearest a column: its centre and its hash; none on the stairway's line. */
    static int[] lot(int dx, int dz, int k) {
        if (k < 0 || k >= 5) {
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

    /** Whether the lot holds a tall narrow tower rather than a house. */
    static boolean tower(int[] lot, int k) {
        return k > 0 && Math.floorMod(lot[2] >> 7, 5) == 0;
    }

    /** A sandstone house (or a tower) under a green pagoda roof with wide eaves, a door toward the street. */
    private static BlockState house(int[] lot, int rx, int ry, int rz, int k) {
        int ax = Math.abs(rx);
        int az = Math.abs(rz);
        int h = lot[2];
        boolean tower = tower(lot, k);
        int half = tower ? 1 : 2;
        int height = tower ? 8 + Math.floorMod(h, 4) : 4 + Math.floorMod(h, 3);
        if (ry < height) {
            if (ax > half || az > half) {
                return null;
            }
            boolean edge = ax == half || az == half;
            if (!edge) {
                return AIR;
            }
            if (ax == half && az == half) {
                return CUT;
            }
            int door = Math.floorMod(h >> 3, 4);
            if (ry <= 2 && ry >= 1 && (rx == 0 || rz == 0) && door == (rx > 0 ? 0 : rx < 0 ? 1 : rz > 0 ? 2 : 3)) {
                return AIR;
            }
            if ((ry == 3 || tower && ry % 3 == 0) && (rx == 0 || rz == 0)) {
                return WINDOW;
            }
            return ry == height - 1 ? CUT : SMOOTH;
        }
        int s = ry - height;
        int eave = half + 1;
        if (s == 0 && ax <= eave && az <= eave) {
            return ROOF;
        }
        if (s >= 1 && ax <= eave - s && az <= eave - s) {
            return s >= 2 ? Blocks.LIME_TERRACOTTA.defaultBlockState() : ROOF;
        }
        if (s == eave + 1 && ax == 0 && az == 0) {
            return (h & 1) == 0 ? Blocks.EMERALD_BLOCK.defaultBlockState() : Blocks.LIME_TERRACOTTA.defaultBlockState();
        }
        return null;
    }

    /** Whether a block over the summit plaza is in the pyramid: the great step pyramid and its four gables. */
    static boolean pyramid(int dx, int ry, int dz) {
        if (ry < 1) {
            return false;
        }
        if (Math.max(Math.abs(dx), Math.abs(dz)) <= PYRAMID + 1 - ry) {
            return true;
        }
        for (int[] g : new int[][]{{12, 0}, {-12, 0}, {0, 12}, {0, -12}}) {
            if (Math.max(Math.abs(dx - g[0]), Math.abs(dz - g[1])) <= 7 - ry) {
                return true;
            }
        }
        return false;
    }

    /** King Bumi's palace on the summit: the pyramid, the passage in from the stairway, and the throne hall. */
    private static BlockState palace(int dx, int ry, int dz) {
        int e = Math.max(Math.abs(dx), Math.abs(dz));
        int ax = Math.abs(dx);
        if (e <= HALL && ry >= 1 && ry <= 5) {
            return hall(dx, ry, dz);
        }
        if (dz > HALL && dz <= PYRAMID + 6 && (ax <= 1 && ry <= 3 || dx == 0 && ry == 4)) {
            return AIR;
        }
        if (pyramid(dx, ry, dz)) {
            if (e == HALL + 1 && ry <= 5) {
                return ry == 4 ? ROOF : CUT;
            }
            if (e <= HALL && ry == 6) {
                return SMOOTH;
            }
            if (dz > HALL && dz <= PYRAMID + 6 && (ax == 2 && ry <= 4 || ax <= 1 && ry == 5 && pyramid(dx, ry, dz + 1) != pyramid(dx, ry, dz))) {
                return CHISELED;
            }
            if (!pyramid(dx, ry + 1, dz)) {
                return (hash(dx * 7 + ry, dz) % 9 == 0) ? ROOF : SMOOTH;
            }
            boolean face = !pyramid(dx + 1, ry, dz) || !pyramid(dx - 1, ry, dz) || !pyramid(dx, ry, dz + 1) || !pyramid(dx, ry, dz - 1);
            if (face) {
                return hash(dx + ry * 5, dz * 3 + ry) % 6 == 0 ? WINDOW : (ry & 1) == 0 ? CUT : SMOOTH;
            }
            return STONE;
        }
        if (dx == 0 && dz == 0 && ry <= PYRAMID + 3) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        return AIR;
    }

    /** The throne hall: a green aisle to the golden throne on its dais, pillars, hanging lanterns. */
    private static BlockState hall(int dx, int ry, int dz) {
        int ax = Math.abs(dx);
        int seatZ = THRONE.getZ();
        if (ry == 5 && ax == 4 && (dz == -4 || dz == 0 || dz == 4)) {
            return HUNG;
        }
        if (ax == 6 && (dz == -5 || dz == 0 || dz == 5)) {
            return ry == 1 || ry == 5 ? CHISELED : CUT;
        }
        if (dz <= seatZ + 1 && dz >= -HALL && ax <= 3 && ry == 1) {
            return CUT;
        }
        if (ry == 2 && dz == seatZ && ax <= 1) {
            // A low seat between golden arms.
            return dx == 0 ? Blocks.SMOOTH_SANDSTONE_SLAB.defaultBlockState() : Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if (ry == 2 && dz == seatZ + 1 && ax == 3) {
            return LANTERN;
        }
        if (dz == seatZ - 1 && ax <= 1 && ry >= 2 && ry <= 3) {
            // A low golden back, a jade post at each end.
            if (ry == 3) {
                return dx == 0 ? AIR : Blocks.EMERALD_BLOCK.defaultBlockState();
            }
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if (ry == 1 && ax <= 1 && dz > seatZ + 1) {
            return Blocks.GREEN_CARPET.defaultBlockState();
        }
        return AIR;
    }

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        CityGuardEntity.City city = CityGuardEntity.City.OMASHU;
        for (int k = 0; k < 5; k++) {
            // On the street between the houses and the parapet.
            int rr = R[k] - 1;
            villager(level, box, random, origin.offset(-rr, H[k] + 1, 0), VillagerType.PLAINS);
            villager(level, box, random, origin.offset(rr, H[k] + 1, 0), VillagerType.PLAINS);
            guard(level, box, random, origin.offset(0, H[k] + 1, -rr), city, Float.NaN);
            // Two guards by the stairway on each terrace.
            guard(level, box, random, origin.offset(-4, H[k] + 1, R[k] - 2), city, 0.0F);
            guard(level, box, random, origin.offset(4, H[k] + 1, R[k] - 2), city, 0.0F);
        }
        // The gate, on the bridge and inside the wall.
        guard(level, box, random, origin.offset(-1, 3, R[0] + 3), city, 0.0F);
        guard(level, box, random, origin.offset(1, 3, R[0] + 3), city, 0.0F);
        // The hall: two by the throne, two at the door.
        int top = H[5];
        guard(level, box, random, origin.offset(-3, top + 2, THRONE.getZ() + 1), city, 0.0F);
        guard(level, box, random, origin.offset(3, top + 2, THRONE.getZ() + 1), city, 0.0F);
        guard(level, box, random, origin.offset(-3, top + 1, PYRAMID + 5), city, 0.0F);
        guard(level, box, random, origin.offset(3, top + 1, PYRAMID + 5), city, 0.0F);
        BlockPos seat = origin.offset(THRONE);
        if (box.isInside(seat)) {
            KingBumiEntity king = ModEntities.KING_BUMI.get().create(level.getLevel());
            if (king != null) {
                king.moveTo(seat.getX() + 0.5, seat.getY() + 0.5, seat.getZ() + 0.5, 0.0F, 0.0F);
                king.setYHeadRot(0.0F);
                king.setYBodyRot(0.0F);
                level.addFreshEntityWithPassengers(king);
            }
        }
        // Chests in some of the houses.
        for (int k = 0; k < 5; k++) {
            double rm = (R[k] + R[k + 1]) / 2.0;
            int n = Math.max(6, (int) (2 * Math.PI * rm / 9));
            for (int i = 0; i < n; i += 3) {
                double ca = (i + 0.5) * 2 * Math.PI / n;
                int cx = (int) Math.round(Math.cos(ca) * rm);
                int cz = (int) Math.round(Math.sin(ca) * rm);
                int[] lot = lot(cx, cz, k);
                if (lot == null || tower(lot, k)) {
                    continue;
                }
                createChest(level, box, random, origin.offset(lot[0] + 1, H[k] + 1, lot[1] + 1), BaSingSePiece.LOOT_HOUSE, null);
            }
        }
    }
}
