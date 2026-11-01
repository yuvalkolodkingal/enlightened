package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.entity.DragonEntity;
import com.kolodkingal.enlightened.entity.SunWarriorEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The ancient city of the Sun Warriors, in the jungle, as Zuko and Aang found it: an avenue of broken columns between
 * overgrown step pyramids and roofless houses (whose doorways still hide the old traps: a pressure plate over a
 * dispenser of arrows or fire), guarded by statues of red and blue dragons, running north to the foot of a mountain.
 * A raised stairway climbs the mountain's face to a terrace where the Sun Warriors dance round their fire, then on to
 * the summit: a round platform paved with the sun, between two crags, each with a cave. In the west cave sleeps Ran,
 * the red dragon; in the east, Shaw, the blue. The chief of the Sun Warriors waits at the foot of the stairway to offer
 * the test (see {@link SunWarriorEntity}, {@link DragonEntity}).
 *
 * <p>The plan is a pure function of the offset from the origin (the ground in the middle of the avenue). The
 * mountain's middle is {@link #MOUNTAIN} blocks north of the origin; {@code t} is the distance south of it along the
 * stairway.
 */
public class SunWarriorPiece extends DesignPiece {
    /** The mountain's middle (z offset from the origin), the summit's height and the platform's radius. */
    public static final int MOUNTAIN = -44;
    public static final int SUMMIT = 48;
    public static final int PLATFORM = 14;
    /** The mountain's radius at its foot. */
    public static final int BASE = 51;
    /** The ceremony terrace: its height, where it starts and ends along the stairway (t), its half width. */
    public static final int TERRACE = 28;
    public static final int TERRACE_NEAR = 34;
    public static final int TERRACE_FAR = 44;
    public static final int TERRACE_HALF = 11;
    /** The stairway's foot (t), where the chief waits. */
    public static final int FOOT = 72;
    /** The two caves' middles (x offsets, at the mountain's z): Ran's to the west, Shaw's to the east. */
    public static final int CAVE = 21;
    /** The step pyramids (x, z of each middle) and the houses (x, z) of the city. */
    public static final int[][] PYRAMIDS = {{-28, 50}, {28, 50}, {-28, 84}, {28, 84}};
    public static final int[][] HOUSES = {{-12, 36}, {12, 36}, {-12, 64}, {12, 64}, {-12, 76}, {12, 76}, {-41, 36}, {41, 36}, {-41, 64}, {41, 64}};
    /** The dragon statues (x, z): red to the west, blue to the east. */
    public static final int[][] STATUES = {{-8, 31}, {8, 31}, {-8, 92}, {8, 92}};
    public static final int CITY_NEAR = 4;
    public static final int CITY_FAR = 95;
    public static final int CITY_HALF = 44;
    private static final int CLEAR = 96;

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.withDefaultNamespace("chests/jungle_temple"));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState MOSSY = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    private static final BlockState RAIL = Blocks.STONE_BRICK_WALL.defaultBlockState();
    private static final BlockState MOSSY_RAIL = Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState();
    private static final BlockState STEP = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
    private static final BlockState ROCK = Blocks.STONE.defaultBlockState();
    private static final BlockState FIRE = Blocks.CAMPFIRE.defaultBlockState();
    private static final BlockState PLATE = Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
    private static final BlockState TRAP = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP);

    public SunWarriorPiece(BlockPos origin) {
        super(ModStructures.SUN_WARRIORS_PIECE.get(), origin, CLEAR, 72, 4);
    }

    public SunWarriorPiece(CompoundTag tag) {
        super(ModStructures.SUN_WARRIORS_PIECE.get(), tag, CLEAR);
    }

    // ---- the shape ----

    static double dist(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    /** The distance south of the mountain's middle along the stairway's line. */
    static int t(int dz) {
        return dz - MOUNTAIN;
    }

    /** Whether a column is the stairway (not the terrace). */
    public static boolean stairway(int dx, int dz) {
        int t = t(dz);
        return Math.abs(dx) <= 3 && t > PLATFORM && t <= FOOT && !(t > TERRACE_NEAR && t <= TERRACE_FAR);
    }

    public static boolean terrace(int dx, int dz) {
        int t = t(dz);
        return Math.abs(dx) <= TERRACE_HALF && t > TERRACE_NEAR && t <= TERRACE_FAR;
    }

    /** The height of the stairway's step (or the terrace) at a distance t south of the mountain's middle. */
    public static int stepHeight(int t) {
        if (t <= PLATFORM) {
            return SUMMIT;
        }
        if (t <= TERRACE_NEAR) {
            return SUMMIT - (t - PLATFORM);
        }
        if (t <= TERRACE_FAR) {
            return TERRACE;
        }
        return Math.max(0, TERRACE - (t - TERRACE_FAR));
    }

    /** A gentle unevenness of the mountain's slope, -2..2. */
    static int bump(int dx, int dz) {
        double n = Math.sin(dx * 0.31) + Math.cos(dz * 0.27) + Math.sin((dx + dz) * 0.17) * 0.8;
        return (int) Math.round(n * 0.7);
    }

    /** The mountain's surface at a column (the summit inside the platform), or -1 outside it. */
    public static int mountain(int dx, int dz) {
        double r = dist(dx, dz - MOUNTAIN);
        if (r > BASE + 0.5) {
            return -1;
        }
        if (r <= PLATFORM + 0.5) {
            return SUMMIT;
        }
        return Math.max(0, Math.min(SUMMIT - 1, (int) Math.round(SUMMIT - (r - PLATFORM) * 1.3) + bump(dx, dz)));
    }

    static boolean city(int dx, int dz) {
        return Math.abs(dx) <= CITY_HALF && dz >= CITY_NEAR && dz <= CITY_FAR;
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        return mountain(dx, dz) >= 0 || city(dx, dz) || stairway(dx, dz) || terrace(dx, dz);
    }

    @Override
    protected int up(int dx, int dz) {
        return dist(dx, dz - MOUNTAIN) <= BASE + 0.5 || stairway(dx, dz) ? 72 : 40;
    }

    @Override
    protected int down(int dx, int dz) {
        return 4;
    }

    private static BlockState brick(int dx, int dy, int dz) {
        long h = hash(dx * 7 + dy * 31, dz * 13 - dy);
        int k = (int) Math.floorMod(h, 7);
        return k == 0 ? MOSSY : k == 1 ? CRACKED : BRICK;
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        if (stairway(dx, dz)) {
            return stairs(dx, dy, dz);
        }
        if (terrace(dx, dz)) {
            return terrace(dx, dy, dz);
        }
        int m = mountain(dx, dz);
        if (m > 0 || (m == 0 && !city(dx, dz))) {
            return mountain(dx, dy, dz, m);
        }
        return city(dx, dy, dz);
    }

    // ---- the stairway and the terrace ----

    private static BlockState stairs(int dx, int dy, int dz) {
        int t = t(dz);
        int h = stepHeight(t);
        int ax = Math.abs(dx);
        if (dy < h) {
            return dy < h - 1 ? brick(dx, dy, dz) : ax == 3 ? CHISELED : BRICK;
        }
        if (dy == h) {
            return ax == 3 ? BRICK : STEP;
        }
        if (ax == 3 && dy == h + 1) {
            return (hash(dx, dz) & 3) == 0 ? MOSSY_RAIL : RAIL;
        }
        if (ax == 3 && dy == h + 2 && t % 8 == 0) {
            return Blocks.LANTERN.defaultBlockState();
        }
        return AIR;
    }

    /** The middle of the terrace's fire pit (z offset), round which the dancers go. */
    public static int pitZ() {
        return MOUNTAIN + (TERRACE_NEAR + TERRACE_FAR + 1) / 2;
    }

    private static BlockState terrace(int dx, int dy, int dz) {
        int t = t(dz);
        int ax = Math.abs(dx);
        boolean edge = ax == TERRACE_HALF || t == TERRACE_NEAR + 1 || t == TERRACE_FAR;
        boolean pillar = ax == TERRACE_HALF && (t == TERRACE_NEAR + 1 || t == TERRACE_FAR || t == (TERRACE_NEAR + TERRACE_FAR) / 2);
        double rp = dist(dx, dz - pitZ());
        if (dy < TERRACE) {
            return edge && dy == TERRACE - 1 ? CHISELED : brick(dx, dy, dz);
        }
        if (dy == TERRACE) {
            if (rp <= 1.5) {
                return Blocks.RED_TERRACOTTA.defaultBlockState();
            }
            if (Math.abs(rp - 4.0) <= 0.5) {
                return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            }
            return (dx + dz & 1) == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : BRICK;
        }
        if (pillar) {
            if (dy <= TERRACE + 4) {
                return dy == TERRACE + 4 ? CHISELED : BRICK;
            }
            return dy == TERRACE + 5 ? FIRE : AIR;
        }
        if (dy == TERRACE + 1) {
            if (dx == 0 && dz == pitZ()) {
                return FIRE;
            }
            if (edge && ax > 3) {
                return RAIL;
            }
        }
        return AIR;
    }

    // ---- the mountain, the summit and the caves ----

    /** Whether a block is inside one of the two caves (or their mouths, which open on the platform). */
    public static boolean cave(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        if (dy < SUMMIT + 1) {
            return false;
        }
        double ex = (ax - CAVE) / 6.0;
        double ey = (dy - (SUMMIT + 5)) / 4.5;
        double ez = (dz - MOUNTAIN) / 5.0;
        if (ex * ex + ey * ey + ez * ez < 1.0) {
            return true;
        }
        return Math.abs(dz - MOUNTAIN) <= 3 && dy <= SUMMIT + 6 && ax >= PLATFORM - 4 && ax <= CAVE;
    }

    /** The top of a crag over a column, or -1 where there is none. */
    public static int crag(int dx, int dz) {
        double rl = dist(Math.abs(dx) - CAVE, dz - MOUNTAIN);
        if (rl > 9.5) {
            return -1;
        }
        return SUMMIT + 2 + (int) ((9.5 - rl) * 1.8) + bump(dx * 2, dz * 2);
    }

    private BlockState mountain(int dx, int dy, int dz, int m) {
        double r = dist(dx, dz - MOUNTAIN);
        int crag = crag(dx, dz);
        if (crag > 0 && dy > m - 1 && dy <= crag) {
            if (cave(dx, dy, dz)) {
                return AIR;
            }
            if (dy == SUMMIT && (cave(dx, dy + 1, dz))) {
                return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            }
            long h = hash(dx * 3 + dy, dz * 5 - dy * 2);
            return (h & 7) == 0 ? Blocks.ANDESITE.defaultBlockState() : (h & 7) == 1 ? Blocks.COBBLESTONE.defaultBlockState()
                    : dy == crag && (h & 1) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : ROCK;
        }
        if (r <= PLATFORM + 0.5) {
            if (dy < SUMMIT) {
                return dy >= SUMMIT - 3 ? BRICK : ROCK;
            }
            if (dy == SUMMIT) {
                return sun(dx, dz - MOUNTAIN);
            }
            if (dy == SUMMIT + 1 && r > PLATFORM - 0.8 && !(Math.abs(dx) <= 3 && dz > MOUNTAIN) && !cave(dx, dy, dz)
                    && crag < 0) {
                return RAIL;
            }
            return AIR;
        }
        if (dy > m) {
            return AIR;
        }
        if (dy == m) {
            long h = hash(dx, dz);
            if (r > 38 || (h & 3) == 0) {
                return Blocks.GRASS_BLOCK.defaultBlockState();
            }
            return (h & 7) == 1 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : (h & 7) == 2 ? Blocks.ANDESITE.defaultBlockState() : ROCK;
        }
        return dy >= m - 3 && r > 38 ? Blocks.DIRT.defaultBlockState() : ROCK;
    }

    /** The summit's paving: the sun, a golden heart in a ring of orange, eight red rays, on grey stone. */
    static BlockState sun(int x, int z) {
        double r = dist(x, z);
        if (r <= 1.5) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if (r <= 3.5) {
            return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        }
        if (r <= 4.3) {
            return Blocks.YELLOW_TERRACOTTA.defaultBlockState();
        }
        double a = Math.atan2(z, x);
        double ray = Math.abs(Math.IEEEremainder(a, Math.PI / 4)) * r;
        if (r <= 10.5 && ray <= 0.9) {
            return Blocks.RED_TERRACOTTA.defaultBlockState();
        }
        if (Math.abs(r - 11.5) <= 0.5) {
            return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        }
        return ((int) r & 1) == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState();
    }

    // ---- the city ----

    private BlockState city(int dx, int dy, int dz) {
        if (dy < 0) {
            return dy >= -2 ? Blocks.DIRT.defaultBlockState() : ROCK;
        }
        for (int[] p : PYRAMIDS) {
            BlockState s = pyramid(dx - p[0], dy, dz - p[1], p[0] > 0 ? 1 : -1);
            if (s != null) {
                return s;
            }
        }
        for (int[] h : HOUSES) {
            BlockState s = house(dx - h[0], dy, dz - h[1], h[0] > 0 ? -1 : 1);
            if (s != null) {
                return s;
            }
        }
        for (int i = 0; i < STATUES.length; i++) {
            int[] s = STATUES[i];
            BlockState b = statue(dx - s[0], dy, dz - s[1], s[0] < 0 ? 1 : -1, s[0] < 0);
            if (b != null) {
                return b;
            }
        }
        if (Math.abs(dx) == 5 && Math.floorMod(dz, 8) == 4 && dz >= 32 && dz <= CITY_FAR - 3) {
            int top = (hash(dx, dz) % 3 == 0) ? 3 : 6;
            if (dy >= 1 && dy <= top) {
                return dy == top ? CHISELED : brick(dx, dy, dz);
            }
        }
        if (dy == 0) {
            if (Math.abs(dx) <= 3) {
                return brick(dx, 0, dz);
            }
            long h = hash(dx, dz);
            return (h % 11 == 0) ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : (h % 7 == 0) ? Blocks.COARSE_DIRT.defaultBlockState()
                    : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return AIR;
    }

    /**
     * A step pyramid: five tiers three high, each two in from the one below, a stair up the face toward the avenue
     * (local x negative, {@code sx} turns it), a fire and a chest on top, and a pressure plate where the stair comes
     * out (over a dispenser of fire charges).
     */
    static BlockState pyramid(int lx0, int dy, int lz, int sx) {
        int lx = lx0 * sx;
        if (Math.abs(lz) <= 1 && lx <= -2 && lx >= -15) {
            int h = 16 + lx;
            if (dy < 1) {
                return null;
            }
            if (dy < h) {
                return brick(lx, dy, lz);
            }
            if (dy == h) {
                return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, sx > 0 ? Direction.EAST : Direction.WEST);
            }
            return dy <= 17 ? AIR : null;
        }
        int a = Math.max(Math.abs(lx), Math.abs(lz));
        if (a > 9 || dy < 1) {
            return null;
        }
        int hw = tierWidth(dy);
        if (hw >= 0 && a <= hw) {
            if (dy == 15 && lx == -1 && lz == 0) {
                return TRAP;
            }
            int k = (dy - 1) / 3;
            boolean band = (dy - 1) % 3 == 2 && a == hw;
            if (band) {
                return k % 2 == 0 ? Blocks.RED_TERRACOTTA.defaultBlockState() : Blocks.YELLOW_TERRACOTTA.defaultBlockState();
            }
            return brick(lx + 40, dy, lz);
        }
        if (dy == 16 && a <= 1) {
            if (lx == -1 && lz == 0) {
                return PLATE;
            }
            if (lx == 1 && lz == 1) {
                return FIRE;
            }
            return AIR;
        }
        return null;
    }

    /** The half width of a pyramid's tier at a height (-1 above the top). */
    static int tierWidth(int dy) {
        int k = (dy - 1) / 3;
        return k > 4 ? -1 : 9 - 2 * k;
    }

    /**
     * A roofless house of seven by seven, its walls broken in places, a doorway toward the avenue (local x, {@code s}
     * the side the door is on); just inside the door a pressure plate over a dispenser that shoots up.
     */
    static BlockState house(int lx, int dy, int lz, int s) {
        int ax = Math.abs(lx);
        int az = Math.abs(lz);
        if (ax > 3 || az > 3 || dy > 5) {
            return null;
        }
        if (lx == 2 * s && lz == 0) {
            if (dy == 0) {
                return TRAP;
            }
            if (dy == 1) {
                return PLATE;
            }
        }
        if (dy == 0) {
            return brick(lx + 9, 0, lz + 17);
        }
        boolean wall = ax == 3 || az == 3;
        if (!wall) {
            return AIR;
        }
        if (lx == 3 * s && lz == 0 && dy <= 2) {
            return AIR;
        }
        long h = hash(lx * 5 + dy * 17 + s, lz * 3 - dy * 7);
        if (dy == 5 || (dy >= 2 && Math.floorMod(h, 6) == 0) || (dy == 4 && Math.floorMod(h, 3) == 0)) {
            return dy == 5 && ax == 3 && az == 3 ? CHISELED : AIR;
        }
        return brick(lx, dy, lz);
    }

    /**
     * A dragon statue: a pedestal, a body coiling up, a head looking along {@code face} (x) with golden eyes and white
     * horns and mane; red or blue.
     */
    static BlockState statue(int lx, int dy, int lz, int face, boolean red) {
        if (Math.abs(lx) > 4 || Math.abs(lz) > 3 || dy < 1 || dy > 15) {
            return null;
        }
        if (dy <= 2) {
            return Math.abs(lx) <= 2 && Math.abs(lz) <= 2 ? (dy == 2 ? CHISELED : BRICK) : null;
        }
        BlockState body = red ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.BLUE_CONCRETE.defaultBlockState();
        BlockState belly = red ? Blocks.ORANGE_TERRACOTTA.defaultBlockState() : Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
        BlockState white = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        if (dy <= 11) {
            int ox = (int) Math.round(Math.sin(dy * 0.8) * 1.2);
            int oz = (int) Math.round(Math.cos(dy * 0.8) * 1.2);
            int d = Math.abs(lx - ox) + Math.abs(lz - oz);
            if (d == 0) {
                return belly;
            }
            if (d == 1) {
                return lx - ox == -face && dy % 2 == 0 ? white : body;
            }
            return null;
        }
        int ox = (int) Math.round(Math.sin(11 * 0.8) * 1.2);
        int oz = (int) Math.round(Math.cos(11 * 0.8) * 1.2);
        int fx = (lx - ox) * face;
        int fz = lz - oz;
        if (dy <= 13 && fx >= -1 && fx <= 3 && Math.abs(fz) <= 1) {
            if (dy == 13 && fx == 1 && Math.abs(fz) == 1) {
                return Blocks.GOLD_BLOCK.defaultBlockState();
            }
            if (dy == 12 && fx == 3) {
                return belly;
            }
            return body;
        }
        if (dy <= 13 && fx == -2 && Math.abs(fz) <= 1) {
            return white;
        }
        if (dy >= 14 && fx == -1 - (dy - 14) && Math.abs(fz) == 1) {
            return white;
        }
        return null;
    }

    // ---- the people, the traps, the chests, the masters ----

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        // The chief at the foot of the stairway, warriors with him and about the city.
        warrior(level, box, random, origin.offset(5, 1, t2z(FOOT) - 2), SunWarriorEntity.Role.CHIEF, 0.0F);
        warrior(level, box, random, origin.offset(-5, 1, t2z(FOOT) - 2), SunWarriorEntity.Role.WARRIOR, 0.0F);
        warrior(level, box, random, origin.offset(-6, 1, 44), SunWarriorEntity.Role.WARRIOR, Float.NaN);
        warrior(level, box, random, origin.offset(6, 1, 58), SunWarriorEntity.Role.WARRIOR, Float.NaN);
        warrior(level, box, random, origin.offset(-6, 1, 72), SunWarriorEntity.Role.WARRIOR, Float.NaN);
        warrior(level, box, random, origin.offset(6, 1, 86), SunWarriorEntity.Role.WARRIOR, Float.NaN);
        // The ceremony: dancers round the fire on the terrace, two warriors at the stair beyond it.
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            warrior(level, box, random, origin.offset((int) Math.round(Math.cos(a) * 4), TERRACE + 1, pitZ() + (int) Math.round(Math.sin(a) * 4)),
                    SunWarriorEntity.Role.DANCER, Float.NaN, origin.offset(0, TERRACE + 1, pitZ()));
        }
        warrior(level, box, random, origin.offset(-5, TERRACE + 1, t2z(TERRACE_NEAR + 2)), SunWarriorEntity.Role.WARRIOR, 180.0F);
        warrior(level, box, random, origin.offset(5, TERRACE + 1, t2z(TERRACE_NEAR + 2)), SunWarriorEntity.Role.WARRIOR, 180.0F);
        // Ran and Shaw, asleep in their caves.
        BlockPos centre = origin.offset(0, SUMMIT + 1, MOUNTAIN);
        master(level, box, origin.offset(-CAVE, SUMMIT + 1, MOUNTAIN), DragonEntity.Colour.RED, centre);
        master(level, box, origin.offset(CAVE, SUMMIT + 1, MOUNTAIN), DragonEntity.Colour.BLUE, centre);
        // The traps: arrows in the houses, fire on the pyramids; the old treasure.
        for (int i = 0; i < HOUSES.length; i++) {
            int[] h = HOUSES[i];
            int s = h[0] > 0 ? -1 : 1;
            arm(level, box, origin.offset(h[0] + 2 * s, 0, h[1]), i % 3 == 2 ? new ItemStack(Items.FIRE_CHARGE, 16) : new ItemStack(Items.ARROW, 32));
            if (i % 2 == 0) {
                createChest(level, box, random, origin.offset(h[0] - 2 * s, 1, h[1] + 2), LOOT, null);
            }
        }
        for (int[] p : PYRAMIDS) {
            int sx = p[0] > 0 ? 1 : -1;
            arm(level, box, origin.offset(p[0] - sx, 15, p[1]), new ItemStack(Items.FIRE_CHARGE, 16));
            createChest(level, box, random, origin.offset(p[0] + sx, 16, p[1]), LOOT, null);
        }
    }

    /** The z offset of a distance t south of the mountain's middle. */
    public static int t2z(int t) {
        return MOUNTAIN + t;
    }

    private static void arm(WorldGenLevel level, BoundingBox box, BlockPos at, ItemStack ammo) {
        if (box.isInside(at) && level.getBlockEntity(at) instanceof DispenserBlockEntity d) {
            d.setItem(4, ammo);
        }
    }

    static void warrior(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos at, SunWarriorEntity.Role role, float yaw) {
        warrior(level, box, random, at, role, yaw, at);
    }

    static void warrior(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos at, SunWarriorEntity.Role role, float yaw, BlockPos post) {
        if (!box.isInside(at)) {
            return;
        }
        SunWarriorEntity w = ModEntities.SUN_WARRIOR.get().create(level.getLevel());
        if (w == null) {
            return;
        }
        float y = Float.isNaN(yaw) ? random.nextFloat() * 360.0F : yaw;
        w.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, y, 0.0F);
        w.setYHeadRot(y);
        w.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        w.station(role, random.nextInt(SunWarriorEntity.FACES), post);
        level.addFreshEntityWithPassengers(w);
    }

    static void master(WorldGenLevel level, BoundingBox box, BlockPos cave, DragonEntity.Colour colour, BlockPos centre) {
        if (!box.isInside(cave)) {
            return;
        }
        DragonEntity d = ModEntities.DRAGON.get().create(level.getLevel());
        if (d == null) {
            return;
        }
        d.makeMaster(colour, cave, centre);
        level.addFreshEntityWithPassengers(d);
    }
}
