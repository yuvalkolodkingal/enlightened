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
    private static final int CLEAR = 53;
    private static final int UP = 74;
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
                for (int dy = UP; dy >= 0; dy--) {
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
        for (int[] v : VILLAGERS) {
            BlockPos at = origin.offset(v[0], PLAZA + 1, v[1]);
            if (!box.isInside(at)) {
                continue;
            }
            Villager villager = EntityType.VILLAGER.create(level.getLevel());
            if (villager == null) {
                continue;
            }
            villager.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            villager.setVillagerData(villager.getVillagerData().setType(VillagerType.DESERT));
            villager.setPersistenceRequired();
            level.addFreshEntityWithPassengers(villager);
        }
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
}
