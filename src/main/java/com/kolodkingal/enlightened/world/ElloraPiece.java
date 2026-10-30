package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Ellora, the golden city of Seven Havens, after the show's backgrounds: a city of red sandstone in the desert, laid
 * out as a star. At its heart the keep, a tall square hall with rows of arched windows and a golden rim, crowned by
 * three great golden crystals (open diamonds of gold round amber glass, leaning out) about a golden spire; round the
 * keep a plaza behind a low parapet, reached by grand stairs on its four sides; and from its corners four long arms
 * stepping down to the desert in terraces, walled, with fields of moss and grass, pools and little sandstone houses,
 * a sunken path stepping down their middle. Desert villagers live on the plaza.
 *
 * <p>The keep's hall keeps a chest of the Seven Havens brigades: the brigade armor trim's smithing template, always,
 * with lapis, gold, redstone and coal; the top terrace of each arm has a market chest, which may hold another.
 * The plan is a pure function of the position ({@link #design}), so each chunk builds its own share.
 */
public class ElloraPiece extends StructurePiece {
    public static final ResourceKey<LootTable> LOOT_BRIGADE = ResourceKey.create(Registries.LOOT_TABLE, Enlightened.id("chests/ellora_brigade"));
    public static final ResourceKey<LootTable> LOOT_MARKET = ResourceKey.create(Registries.LOOT_TABLE, Enlightened.id("chests/ellora_market"));

    /** The plaza's height above the desert, and its half-width. */
    public static final int PLAZA = 8;
    public static final int PLAZA_R = 22;
    /** The keep's half-width and the height of its roof. */
    public static final int KEEP = 12;
    public static final int KEEP_TOP = 34;
    /** The arms: from the keep's corner out, half as wide as this, a terrace of three down every six blocks. */
    public static final int ARM_IN = 14;
    public static final int ARM_OUT = 49;
    public static final int ARM_W = 7;
    public static final int STEP = 6;
    /** The grand stairs from the desert to the plaza. */
    public static final int STAIR_W = 3;
    /** The city wall: an octagon from this far out to that, so high; its eight corner towers, and the towers of its gates. */
    public static final int WALL_IN = 88;
    public static final int WALL_OUT = 91;
    public static final int WALL_H = 14;
    private static final double TOWER_AT = 89.5;
    /** The ring road round the city, between the arms' ends and the districts by the wall. */
    public static final int RING_IN = 56;
    public static final int RING_OUT = 59;
    /** The districts are laid out in lots this wide, a house or a garden on each. */
    public static final int LOT = 11;
    private static final int CLEAR = 97;
    private static final int UP = 74;
    /** How high the city outside the keep and its arms is cleared and built. */
    private static final int CITY_UP = 34;
    private static final int DOWN = 30;
    private static final double ROOT2 = Math.sqrt(2.0);

    /** The golden crystals on the keep: centre (x, z), the angle of their face, their size. */
    private static final double[][] CRYSTALS = {{0, -5, 0, 13}, {-6, 4, 60, 11}, {6, 4, -60, 11}};
    private static final int CRYSTAL_Y = KEEP_TOP + 9;
    private static final double LEAN = 0.45;
    private static final int SPIRE_TOP = KEEP_TOP + 38;

    /** The market chests, one on each arm's top terrace (x, z; the y is the terrace's). */
    private static final int[][] MARKET = {{13, 9}, {-13, 9}, {13, -9}, {-13, -9}};
    private static final int[][] VILLAGERS = {{17, 5}, {-17, -5}, {5, 17}, {-5, -17}, {17, -7}, {-7, 17}};
    /** Townsfolk in the streets (x, z, at the ground) and the guards at the gates. */
    private static final int[][] STREET_FOLK = {{40, 1}, {-40, -1}, {1, 40}, {-1, -40}, {66, 0}, {-66, 0}, {0, 66}, {0, -66},
            {40, 41}, {-41, 40}, {41, -40}, {-40, -41}};
    private static final int[][] GUARDS = {{84, 4}, {-84, -4}, {4, 84}, {-4, -84}};
    /** The towers: centre x, z, radius, height. The eight at the wall's corners, then two either side of each gate. */
    private static final double[][] TOWERS = towers();

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WALL = Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState();
    private static final BlockState BAND = Blocks.CUT_RED_SANDSTONE.defaultBlockState();
    private static final BlockState RIM = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
    private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState AMBER = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
    private static final BlockState WINDOW = Blocks.BROWN_STAINED_GLASS.defaultBlockState();
    private static final BlockState FLOOR = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState FLOOR_LINE = Blocks.CUT_SANDSTONE.defaultBlockState();
    private static final BlockState LIGHT = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState ROOF = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
    private static final BlockState GROUND = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState DOME = Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState();
    private static final BlockState ZIG_A = Blocks.WHITE_TERRACOTTA.defaultBlockState();
    private static final BlockState ZIG_B = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
    private static final BlockState[] HOUSE_WALLS = {Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState()};

    private static double[][] towers() {
        double k = TOWER_AT * (ROOT2 - 1.0);
        java.util.List<double[]> list = new java.util.ArrayList<>();
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                list.add(new double[]{sx * TOWER_AT, sz * k, 5, 22});
                list.add(new double[]{sx * k, sz * TOWER_AT, 5, 22});
            }
        }
        for (int s = -1; s <= 1; s += 2) {
            for (int side = -1; side <= 1; side += 2) {
                list.add(new double[]{s * TOWER_AT, side * 6, 3, 19});
                list.add(new double[]{side * 6, s * TOWER_AT, 3, 19});
            }
        }
        return list.toArray(new double[0][]);
    }

    private final BlockPos origin;

    public ElloraPiece(BlockPos origin) {
        super(ModStructures.ELLORA_PIECE.get(), 0, new BoundingBox(origin.getX() - CLEAR, origin.getY() - DOWN, origin.getZ() - CLEAR,
                origin.getX() + CLEAR, origin.getY() + UP, origin.getZ() + CLEAR));
        this.origin = origin;
    }

    public ElloraPiece(CompoundTag tag) {
        super(ModStructures.ELLORA_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), ox - CLEAR); x <= Math.min(box.maxX(), ox + CLEAR); x++) {
            for (int z = Math.max(box.minZ(), oz - CLEAR); z <= Math.min(box.maxZ(), oz + CLEAR); z++) {
                int dx = x - ox;
                int dz = z - oz;
                if (top(dx, dz) < 0) {
                    continue;
                }
                for (int dy = coreTop(dx, dz) >= 0 ? UP : CITY_UP; dy >= 0; dy--) {
                    BlockState state = design(dx, dy, dz);
                    pos.set(x, oy + dy, z);
                    if (state != null && !level.isOutsideBuildHeight(pos) && level.getBlockState(pos) != state) {
                        level.setBlock(pos, state, 2);
                    }
                }
                // The city's footing: sandstone down to the desert floor under it.
                for (int dy = -1; dy >= -DOWN; dy--) {
                    pos.set(x, oy + dy, z);
                    if (level.isOutsideBuildHeight(pos)) {
                        break;
                    }
                    BlockState cur = level.getBlockState(pos);
                    if (!(cur.canBeReplaced() || !cur.getFluidState().isEmpty())) {
                        break;
                    }
                    level.setBlock(pos, (dy & 3) == 0 ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState(), 2);
                }
            }
        }
        // The brigades' chest in the keep's hall, against its north wall.
        createChest(level, box, random, origin.offset(0, PLAZA + 1, -KEEP + 2), LOOT_BRIGADE,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        for (int[] m : MARKET) {
            createChest(level, box, random, origin.offset(m[0], top(m[0], m[1]) + 1, m[1]), LOOT_MARKET, null);
        }
        // A chest in some of the houses.
        for (int lx = Math.floorDiv(box.minX() - ox + LOT / 2, LOT); lx <= Math.floorDiv(box.maxX() - ox + LOT / 2, LOT); lx++) {
            for (int lz = Math.floorDiv(box.minZ() - oz + LOT / 2, LOT); lz <= Math.floorDiv(box.maxZ() - oz + LOT / 2, LOT); lz++) {
                House h = house(lx, lz);
                if (h != null && !h.garden && h.chest) {
                    createChest(level, box, random, origin.offset(h.x + h.hw - 1, 1, h.z + h.hd - 1), LOOT_MARKET, null);
                }
            }
        }
        for (int[] v : VILLAGERS) {
            villager(level, box, random, origin.offset(v[0], PLAZA + 1, v[1]));
        }
        for (int[] v : STREET_FOLK) {
            villager(level, box, random, origin.offset(v[0], 1, v[1]));
        }
        for (int[] g : GUARDS) {
            BlockPos at = origin.offset(g[0], 1, g[1]);
            if (!box.isInside(at)) {
                continue;
            }
            net.minecraft.world.entity.animal.IronGolem golem = EntityType.IRON_GOLEM.create(level.getLevel());
            if (golem != null) {
                golem.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                golem.setPersistenceRequired();
                level.addFreshEntityWithPassengers(golem);
            }
        }
    }

    private static void villager(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos at) {
        if (!box.isInside(at)) {
            return;
        }
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) {
            return;
        }
        villager.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillagerType.DESERT));
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    // ---- the plan ----

    /** How far along its arm a column is (from the centre, along the diagonal) and how far across. */
    public static double along(int dx, int dz) {
        return (Math.abs(dx) + Math.abs(dz)) / ROOT2;
    }

    public static double across(int dx, int dz) {
        return (Math.abs(dx) - Math.abs(dz)) / ROOT2;
    }

    /** The terrace's height at this distance along an arm: three lower every six blocks. */
    public static int terrace(double u) {
        return 16 - 3 * (int) Math.floor((u - ARM_IN) / STEP);
    }

    public static boolean inArm(int dx, int dz) {
        double u = along(dx, dz);
        return u >= ARM_IN && u <= ARM_OUT && Math.abs(across(dx, dz)) <= ARM_W;
    }

    public static boolean onPath(int dx, int dz) {
        return inArm(dx, dz) && Math.abs(across(dx, dz)) <= 1.0;
    }

    /** The city's ground at a column: the height of its walking surface above the desert, or -1 outside the city. */
    public static int top(int dx, int dz) {
        int core = coreTop(dx, dz);
        if (core >= 0) {
            return core;
        }
        return oct(dx, dz) <= WALL_OUT || tower(dx, dz) != null ? 0 : -1;
    }

    /** The keep, its plaza, the stairs and the arms: their surface, or -1 for the rest of the city. */
    public static int coreTop(int dx, int dz) {
        int t = -1;
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (Math.max(ax, az) <= PLAZA_R) {
            t = PLAZA;
        }
        if (inArm(dx, dz)) {
            double u = along(dx, dz);
            // The path down the middle steps down a block every two, sunk into the terraces.
            int arm = onPath(dx, dz) ? 16 - (int) Math.floor((u - ARM_IN) / 2.0) : terrace(u);
            t = Math.max(t, Math.max(1, arm));
        }
        int axis = Math.max(ax, az);
        if (axis > PLAZA_R && axis <= PLAZA_R + PLAZA && Math.min(ax, az) <= STAIR_W) {
            t = Math.max(t, PLAZA - (axis - PLAZA_R));
        }
        return t;
    }

    private static long hash(int x, int z) {
        long h = (x * 3129871L) ^ (z * 116129781L);
        return h * h * 42317861L + h * 11L;
    }

    private static boolean isMarket(int dx, int dz) {
        for (int[] m : MARKET) {
            if (m[0] == dx && m[1] == dz) {
                return true;
            }
        }
        return false;
    }

    /** What a field of a terrace is: 0 moss, 1 grass, 2 a pool, 3 a house, 4 paving. */
    private static int field(int dx, int dz) {
        int roll = (int) ((hash(Math.floorDiv(dx, 3), Math.floorDiv(dz, 3)) >>> 20) & 15);
        return roll < 5 ? 0 : roll < 8 ? 1 : roll < 10 ? 2 : roll < 13 ? 3 : 4;
    }

    private static boolean level(int dx, int dz, int t) {
        return top(dx + 1, dz) == t && top(dx - 1, dz) == t && top(dx, dz + 1) == t && top(dx, dz - 1) == t;
    }

    /** The block at (dx, dy, dz) from the origin, or null to leave the world as it is. */
    public static BlockState design(int dx, int dy, int dz) {
        int t = top(dx, dz);
        if (t < 0) {
            return null;
        }
        if (coreTop(dx, dz) < 0) {
            return city(dx, dy, dz);
        }
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        boolean keep = ax <= KEEP && az <= KEEP;
        if (dy < t) {
            return (dy & 3) == 0 ? BAND : WALL;
        }
        if (dy == t) {
            return surface(dx, dz, t, keep);
        }
        if (dy > KEEP_TOP) {
            BlockState sky = crown(dx, dy, dz);
            if (sky != null) {
                return sky;
            }
        }
        if (keep) {
            return keep(dx, dy, dz);
        }
        int ry = dy - t;
        if (inArm(dx, dz)) {
            double v = Math.abs(across(dx, dz));
            if (v > ARM_W - 1 && !onPath(dx, dz)) {
                return ry == 1 ? WALL : ry == 2 ? RIM : AIR;
            }
            if (!onPath(dx, dz) && !isMarket(dx, dz) && field(dx, dz) == 3 && Math.floorMod(dx, 3) < 2 && Math.floorMod(dz, 3) < 2 && v <= ARM_W - 2) {
                return ry <= 2 ? FLOOR : ry == 3 ? ROOF : AIR;
            }
            return AIR;
        }
        // The plaza's parapet, open where the stairs come up.
        if (t == PLAZA && Math.max(ax, az) == PLAZA_R && Math.min(ax, az) > STAIR_W && ry == 1) {
            return Blocks.RED_SANDSTONE_WALL.defaultBlockState();
        }
        return AIR;
    }

    private static BlockState surface(int dx, int dz, int t, boolean keep) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (keep) {
            if (ax == 6 && az == 6) {
                return LIGHT;
            }
            return (dx + dz & 3) == 0 && (dx - dz & 3) == 0 ? Blocks.CHISELED_SANDSTONE.defaultBlockState() : FLOOR;
        }
        int axis = Math.max(ax, az);
        if (axis > PLAZA_R && Math.min(ax, az) <= STAIR_W && t == PLAZA - (axis - PLAZA_R) && !inArm(dx, dz)) {
            // The grand stairs, climbing toward the keep.
            Direction in = ax >= az ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
            return Blocks.SMOOTH_SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, in);
        }
        if (inArm(dx, dz)) {
            if (onPath(dx, dz)) {
                return FLOOR_LINE;
            }
            if (Math.abs(across(dx, dz)) > ARM_W - 1 || isMarket(dx, dz)) {
                return FLOOR;
            }
            return switch (field(dx, dz)) {
                case 0 -> Blocks.MOSS_BLOCK.defaultBlockState();
                case 1 -> Blocks.GRASS_BLOCK.defaultBlockState();
                case 2 -> level(dx, dz, t) ? Blocks.WATER.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
                default -> FLOOR;
            };
        }
        // The plaza: sandstone paving in squares, lamps in the corners of the inner ring.
        if ((ax == 17 && az == 17) || (ax == 17 && az == 0) || (ax == 0 && az == 17)) {
            return LIGHT;
        }
        return (ax & 3) == 0 || (az & 3) == 0 ? FLOOR_LINE : FLOOR;
    }

    /** The keep: walls with rows of arched windows and gates on its four sides, a hall with a gallery, a flat roof. */
    private static BlockState keep(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        int ry = dy - PLAZA;
        if (dy > KEEP_TOP) {
            return AIR;
        }
        if (ax == KEEP || az == KEEP) {
            int along = ax == KEEP ? dz : dx;
            if (Math.abs(along) <= 1 && ry <= 4 && Math.min(ax, az) <= 1) {
                return AIR;
            }
            if (dy == KEEP_TOP) {
                return GOLD;
            }
            if (dy == KEEP_TOP - 1 || ry == 13) {
                return BAND;
            }
            boolean bay = Math.floorMod(along, 4) == 0 && Math.abs(along) <= 8;
            if (bay && ((ry >= 4 && ry <= 8) || (ry >= 16 && ry <= 21))) {
                return WINDOW;
            }
            return Math.floorMod(along, 4) == 2 ? BAND : WALL;
        }
        if (dy == KEEP_TOP) {
            return ax <= 1 && az <= 1 ? LIGHT : WALL;
        }
        // The gallery round the hall, halfway up.
        if (ry == 13 && (ax >= 8 || az >= 8)) {
            return FLOOR;
        }
        // Four pillars in the hall with lamps.
        if (ax == 6 && az == 6 && ry >= 1 && ry <= 12) {
            return ry == 6 ? LIGHT : BAND;
        }
        return AIR;
    }

    /** Above the keep's roof: the golden spire and the three crystals round it. */
    private static BlockState crown(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (dy <= SPIRE_TOP) {
            if (ax == 0 && az == 0) {
                return dy == SPIRE_TOP ? Blocks.END_ROD.defaultBlockState() : GOLD;
            }
            if (ax <= 1 && az <= 1 && dy <= KEEP_TOP + 5) {
                return GOLD;
            }
        }
        for (double[] c : CRYSTALS) {
            double a = Math.toRadians(c[2]);
            double fx = Math.cos(a);
            double fz = Math.sin(a);
            double nx = -fz;
            double nz = fx;
            if (nx * c[0] + nz * c[1] < 0) {
                nx = -nx;
                nz = -nz;
            }
            double rx = dx - c[0];
            double rz = dz - c[1];
            double ry = dy - CRYSTAL_Y;
            double w = rx * nx + rz * nz - ry * LEAN;
            if (Math.abs(w) > 0.75) {
                continue;
            }
            double across = rx * fx + rz * fz;
            double d = Math.abs(across) + Math.abs(ry);
            double r = c[3];
            if (d <= r && d >= r - 2.5) {
                return GOLD;
            }
            if (d < r - 2.5 && d >= r - 5.5) {
                return AMBER;
            }
        }
        return null;
    }

    // ---- the city round the keep: its wall, towers and gates, its streets and its districts ----

    /** The octagonal distance from the centre, by which the wall and the ring road run. */
    public static double oct(int dx, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        return Math.max(Math.max(ax, az), (ax + az) / ROOT2);
    }

    /** The tower a column stands in, or null. */
    private static double[] tower(int dx, int dz) {
        for (double[] t : TOWERS) {
            double rx = dx - t[0];
            double rz = dz - t[1];
            if (rx * rx + rz * rz <= (t[2] + 0.5) * (t[2] + 0.5)) {
                return t;
            }
        }
        return null;
    }

    /** The main streets from the stairs to the gates, and the ring road. */
    public static boolean street(int dx, int dz) {
        int major = Math.max(Math.abs(dx), Math.abs(dz));
        int minor = Math.min(Math.abs(dx), Math.abs(dz));
        double o = oct(dx, dz);
        return o < WALL_IN && ((major >= PLAZA_R + PLAZA && minor <= 2) || (o >= RING_IN && o <= RING_OUT));
    }

    /** A house (or a garden) on a lot: its centre, half-sizes, height, and what it has. */
    record House(int x, int z, int hw, int hd, int h, boolean garden, boolean dome, boolean chest, BlockState wall, boolean doorX, int doorSide) {
    }

    /** The house on lot (lx, lz), or null where no house stands: on a street, the keep's ground or the arms, or by the wall. */
    static House house(int lx, int lz) {
        int cx = lx * LOT;
        int cz = lz * LOT;
        for (int a = -4; a <= 4; a += 4) {
            for (int b = -4; b <= 4; b += 4) {
                int x = cx + a;
                int z = cz + b;
                if (oct(x, z) > WALL_IN - 4 || coreTop(x, z) >= 0 || street(x, z) || street(x + Integer.signum(a), z + Integer.signum(b))) {
                    return null;
                }
            }
        }
        long h = hash(lx * 7 + 3, lz * 13 + 1);
        int jx = (int) ((h >>> 8) % 3) - 1;
        int jz = (int) ((h >>> 10) % 3) - 1;
        int hw = 2 + (int) (h >>> 1 & 1);
        int hd = 2 + (int) (h >>> 2 & 1);
        int height = 5 + (int) ((h >>> 3) % 5);
        boolean garden = (h >>> 20 & 7) == 0;
        boolean dome = (h >>> 12) % 3 == 0;
        boolean chest = (h >>> 15 & 3) == 0;
        BlockState wall = HOUSE_WALLS[(int) ((h >>> 17) % HOUSE_WALLS.length)];
        // The door faces the middle of the city.
        boolean doorX = Math.abs(cx) >= Math.abs(cz);
        int side = doorX ? -Integer.signum(cx) : -Integer.signum(cz);
        return new House(cx + jx, cz + jz, hw, hd, height, garden, dome, chest, wall, doorX, side == 0 ? 1 : side);
    }

    private static House houseAt(int dx, int dz) {
        return house(Math.floorDiv(dx + LOT / 2, LOT), Math.floorDiv(dz + LOT / 2, LOT));
    }

    private static BlockState city(int dx, int dy, int dz) {
        double[] tw = tower(dx, dz);
        if (tw != null) {
            return tower(tw, dx, dy, dz);
        }
        double o = oct(dx, dz);
        if (o > WALL_OUT) {
            return null;
        }
        boolean wall = o >= WALL_IN;
        int major = Math.max(Math.abs(dx), Math.abs(dz));
        int minor = Math.min(Math.abs(dx), Math.abs(dz));
        if (dy == 0) {
            if (wall || street(dx, dz)) {
                return wall ? WALL : FLOOR_LINE;
            }
            House h = houseAt(dx, dz);
            if (h != null && h.garden) {
                return Math.abs(dx - h.x) <= 4 && Math.abs(dz - h.z) <= 4 ? Blocks.GRASS_BLOCK.defaultBlockState() : GROUND;
            }
            if (h != null && Math.abs(dx - h.x) <= h.hw && Math.abs(dz - h.z) <= h.hd) {
                return dx == h.x && dz == h.z ? LIGHT : FLOOR;
            }
            return GROUND;
        }
        if (wall) {
            return wall(dx, dy, dz, o, major, minor);
        }
        // Lamps along the main streets.
        if (minor == 3 && major % 8 == 0 && major > PLAZA_R + PLAZA && o < WALL_IN - 2) {
            return dy <= 2 ? Blocks.RED_SANDSTONE_WALL.defaultBlockState() : dy == 3 ? Blocks.LANTERN.defaultBlockState() : AIR;
        }
        House h = houseAt(dx, dz);
        if (h != null) {
            BlockState b = h.garden ? palm(dx - h.x, dy, dz - h.z) : house(h, dx - h.x, dy, dz - h.z);
            if (b != null) {
                return b;
            }
        }
        return AIR;
    }

    /** The city wall: banded red sandstone with a zigzag frieze, a walk on top behind crenels, gates on the four streets. */
    private static BlockState wall(int dx, int dy, int dz, double o, int major, int minor) {
        if (minor <= 2 && dy <= 6 || minor <= 1 && dy == 7) {
            return AIR;
        }
        if (dy < WALL_H) {
            if (dy == 10 || dy == 11) {
                return ((dx + dz + dy) & 3) < 2 ? ZIG_A : ZIG_B;
            }
            return dy % 5 == 0 ? BAND : WALL;
        }
        if (dy == WALL_H) {
            return FLOOR;
        }
        if (dy == WALL_H + 1) {
            if (o >= WALL_OUT - 0.6) {
                return ((dx + dz) & 1) == 0 ? BAND : AIR;
            }
            if (o < WALL_IN + 0.6) {
                return Blocks.RED_SANDSTONE_WALL.defaultBlockState();
            }
        }
        return AIR;
    }

    /** A round tower: red sandstone, a zigzag frieze, slit windows, a teal dome with a golden tip. */
    private static BlockState tower(double[] t, int dx, int dy, int dz) {
        double rx = dx - t[0];
        double rz = dz - t[1];
        double d = Math.sqrt(rx * rx + rz * rz);
        double r = t[2] + 0.5;
        int height = (int) t[3];
        if (dy == 0) {
            return WALL;
        }
        if (dy < height) {
            if (d < r - 1.2) {
                return dy == WALL_H ? FLOOR : AIR;
            }
            if (dy == height - 3 || dy == height - 4) {
                return ((dx + dz + dy) & 3) < 2 ? ZIG_A : ZIG_B;
            }
            if (dy % 6 == 3 && (Math.abs(rx) < 0.6 || Math.abs(rz) < 0.6)) {
                return WINDOW;
            }
            return dy % 5 == 0 ? BAND : WALL;
        }
        if (dy == height) {
            return d >= r - 1.0 ? BAND : FLOOR;
        }
        // The dome.
        double rr = t[2];
        double up = dy - height;
        double s = Math.sqrt(rx * rx + rz * rz + up * up * 1.4);
        if (s <= rr + 0.3 && s >= rr - 1.0) {
            return DOME;
        }
        if (Math.abs(rx) < 0.6 && Math.abs(rz) < 0.6 && up > rr / 1.18 && up <= rr / 1.18 + 2.0) {
            return GOLD;
        }
        return AIR;
    }

    /** A house of the districts: walls with a door toward the middle of the city, windows, a zigzag frieze under the roof, a flat roof behind a parapet or a teal dome. */
    private static BlockState house(House h, int rx, int dy, int rz) {
        int ax = Math.abs(rx);
        int az = Math.abs(rz);
        boolean in = ax <= h.hw && az <= h.hd;
        if (dy < h.h) {
            if (!in) {
                return null;
            }
            boolean edgeX = ax == h.hw;
            boolean edgeZ = az == h.hd;
            if (!edgeX && !edgeZ) {
                return AIR;
            }
            if (edgeX && edgeZ) {
                return BAND;
            }
            if (dy <= 2 && (h.doorX ? edgeX && rx == h.doorSide * h.hw && rz == 0 : edgeZ && rz == h.doorSide * h.hd && rx == 0)) {
                return AIR;
            }
            int along = edgeX ? rz : rx;
            if (dy >= h.h - 2) {
                return ((along + dy) & 3) < 2 ? ZIG_A : ZIG_B;
            }
            if (dy == 2 && (along & 1) == 0 && along != 0) {
                return WINDOW;
            }
            return h.wall;
        }
        if (dy == h.h) {
            return in ? FLOOR : null;
        }
        if (h.dome) {
            int r = Math.min(h.hw, h.hd);
            double up = dy - h.h;
            double s = Math.sqrt(rx * rx + rz * rz + up * up);
            if (s <= r + 0.4 && s >= r - 0.8) {
                return DOME;
            }
            if (rx == 0 && rz == 0 && up > r && up <= r + 1) {
                return GOLD;
            }
            return in && dy == h.h + 1 && (ax == h.hw || az == h.hd) && s > r + 0.4 && ((ax + az) & 1) == 0 ? h.wall : null;
        }
        if (dy == h.h + 1 && in && (ax == h.hw || az == h.hd) && ((ax + az) & 1) == 0) {
            return h.wall;
        }
        return null;
    }

    /** A date palm in a garden: a trunk, and fronds out and drooping. */
    private static BlockState palm(int rx, int dy, int rz) {
        BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        if (rx == 0 && rz == 0 && dy <= 6) {
            return Blocks.JUNGLE_LOG.defaultBlockState();
        }
        int ax = Math.abs(rx);
        int az = Math.abs(rz);
        if (rx == 0 && rz == 0 && dy == 7) {
            return leaves;
        }
        if ((ax == 0 || az == 0) && ax + az <= 2 && dy == 7) {
            return leaves;
        }
        if ((ax == 0 || az == 0) && ax + az == 3 && dy == 6) {
            return leaves;
        }
        if (ax == 1 && az == 1 && dy == 7) {
            return leaves;
        }
        return null;
    }
}
