package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.GuruPathikEntity;
import com.kolodkingal.enlightened.entity.SkyBisonEntity;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Southern Air Temple as the show draws it: a giant temple built up a mountain of its own. A rocky peak rises in
 * four tiers, each a ledge of the mountain ringed by a low wall, its cliffs pale rock and snow, and stairways cut into
 * the cliffs wind from tier to tier, each on another side of the peak; halls and spires stand in notches cut into the
 * cliffs. On the base tier, the courtyard where the sky
 * bison calves live, and Monk Gyatso's seated statue under an arch; on the ledges above, halls (a storage room of food,
 * materials and gliders; a meditation room) and slender spires, white stone under tall blue cone roofs with golden
 * tips; on the summit, the great tower: the sanctuary, its floor the Air Nomad swirl, the statues of the Avatars round
 * its wall and Guru Pathik meditating at its heart, its north door the sealed round door with the swirl in brass, and
 * above, a tall tower in tiers of blue roofs. Flying lemurs live about it.
 *
 * <p>The plan is a pure function of the position ({@link #design}), so each chunk builds its own share.
 */
public class AirTemplePiece extends StructurePiece {
    public static final ResourceKey<LootTable> LOOT_FOOD = ResourceKey.create(Registries.LOOT_TABLE, Enlightened.id("chests/air_temple_food"));
    public static final ResourceKey<LootTable> LOOT_MATERIALS = ResourceKey.create(Registries.LOOT_TABLE, Enlightened.id("chests/air_temple_materials"));
    public static final ResourceKey<LootTable> LOOT_GLIDERS = ResourceKey.create(Registries.LOOT_TABLE, Enlightened.id("chests/air_temple_gliders"));

    /** The tiers: their floor heights and outer radii (each edge wavers round the peak, as rock does). */
    private static final int[] TIER_Y = {0, 12, 26, 40};
    private static final double[] TIER_R = {38.0, 26.0, 18.0, 11.0};
    private static final int SUMMIT = TIER_Y[3];
    private static final int CLEAR = 42;
    private static final int UP = 82;
    private static final int DOWN = 50;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WALL = Blocks.CALCITE.defaultBlockState();
    private static final BlockState BAND = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState ROOF = Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
    private static final BlockState ROOF_RIM = Blocks.BLUE_CONCRETE.defaultBlockState();
    private static final BlockState CEILING = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState FLOOR_RING = Blocks.POLISHED_DIORITE.defaultBlockState();
    private static final BlockState PARAPET = Blocks.DIORITE_WALL.defaultBlockState();
    private static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState SPIRE = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState SWIRL = Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
    private static final BlockState SWIRL_GROUND = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState HAY = Blocks.HAY_BLOCK.defaultBlockState();

    /** A tower: centre, wall radius, wall height, roof radius, roof height, the level of its floor, and its kind. */
    private record Tower(int cx, int cz, double r, int h, double roofR, int roofH, int base, Kind kind) {
    }

    private enum Kind { GREAT, HALL, SPIRE }

    /** The storage hall (on the first ledge) and the meditation hall (on the second). */
    private static final int[] STORE = {-16, -16, TIER_Y[1]};
    private static final int[] MEDITATION_ROOM = {10, -10, TIER_Y[2]};

    private static final Tower[] TOWERS = {
            // The summit: the great tower, the sanctuary, crowned by roofs in tiers.
            new Tower(0, 0, 6.5, 20, 9.0, 13, SUMMIT, Kind.GREAT),
            // The first ledge.
            new Tower(STORE[0], STORE[1], 3.5, 10, 5.0, 8, TIER_Y[1], Kind.HALL),
            new Tower(-16, 16, 3.5, 11, 5.0, 8, TIER_Y[1], Kind.HALL),
            new Tower(0, -22, 1.5, 14, 2.6, 6, TIER_Y[1], Kind.SPIRE),
            new Tower(16, -16, 1.5, 16, 2.6, 6, TIER_Y[1], Kind.SPIRE),
            new Tower(16, 16, 1.5, 13, 2.6, 6, TIER_Y[1], Kind.SPIRE),
            // The second ledge.
            new Tower(MEDITATION_ROOM[0], MEDITATION_ROOM[1], 3.5, 10, 5.0, 8, TIER_Y[2], Kind.HALL),
            new Tower(-10, -10, 1.5, 15, 2.6, 6, TIER_Y[2], Kind.SPIRE),
            new Tower(-14, 2, 1.5, 12, 2.6, 6, TIER_Y[2], Kind.SPIRE),
            new Tower(13, 5, 1.5, 13, 2.6, 6, TIER_Y[2], Kind.SPIRE),
            // The base tier: spires round its edge.
            new Tower(30, -18, 1.5, 15, 2.6, 6, 0, Kind.SPIRE),
            new Tower(-24, -26, 1.5, 17, 2.6, 6, 0, Kind.SPIRE),
            new Tower(-30, 16, 1.5, 14, 2.6, 6, 0, Kind.SPIRE),
            new Tower(22, 26, 1.5, 16, 2.6, 6, 0, Kind.SPIRE),
    };

    private static final int[][] HAY_AT = {{-6, 33}, {5, 35}, {0, 36}, {9, 31}};

    private final BlockPos origin;

    public AirTemplePiece(BlockPos origin) {
        super(ModStructures.AIR_TEMPLE_PIECE.get(), 0, new BoundingBox(origin.getX() - CLEAR - 1, origin.getY() - DOWN, origin.getZ() - CLEAR - 1,
                origin.getX() + CLEAR + 1, origin.getY() + UP, origin.getZ() + CLEAR + 1));
        this.origin = origin;
    }

    public AirTemplePiece(CompoundTag tag) {
        super(ModStructures.AIR_TEMPLE_PIECE.get(), tag);
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
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > CLEAR + 0.5) {
                    continue;
                }
                for (int dy = UP; dy >= 0; dy--) {
                    BlockState state = design(dx, dy, dz, r);
                    pos.set(x, oy + dy, z);
                    if (state != null && !level.isOutsideBuildHeight(pos) && level.getBlockState(pos) != state) {
                        level.setBlock(pos, state, 2);
                    }
                }
                // The roots of the mountain: rock down to the ground under the base tier.
                if (top(dx, dz) >= 0) {
                    for (int dy = -1; dy >= -DOWN; dy--) {
                        pos.set(x, oy + dy, z);
                        if (level.isOutsideBuildHeight(pos)) {
                            break;
                        }
                        BlockState cur = level.getBlockState(pos);
                        if (!(cur.canBeReplaced() || !cur.getFluidState().isEmpty() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.LOGS)
                                || cur.is(Blocks.POWDER_SNOW) || cur.is(Blocks.SNOW_BLOCK))) {
                            break;
                        }
                        level.setBlock(pos, rock(x, oy + dy, z, false), 2);
                    }
                }
            }
        }
        // The storage room: chests of food, of materials and of gliders, and barrels of food.
        int sx = STORE[0];
        int sz = STORE[1];
        int sy = STORE[2] + 1;
        createChest(level, box, random, origin.offset(sx + 2, sy, sz), LOOT_FOOD, null);
        createChest(level, box, random, origin.offset(sx, sy, sz - 2), LOOT_MATERIALS, null);
        createChest(level, box, random, origin.offset(sx + 1, sy, sz - 2), LOOT_GLIDERS, null);
        barrel(level, box, random, origin.offset(sx - 2, sy, sz));
        barrel(level, box, random, origin.offset(sx - 1, sy, sz - 2));
        // The guru on his mat at the heart of the sanctuary.
        spawn(level, box, origin.offset(0, SUMMIT + 1, 0), () -> {
            GuruPathikEntity guru = ModEntities.GURU_PATHIK.get().create(level.getLevel());
            if (guru != null) {
                guru.setCustomName(Component.translatable("entity.enlightened.guru_pathik"));
                guru.setYRot(0.0F);
                guru.yHeadRot = 0.0F;
                guru.yBodyRot = 0.0F;
            }
            return guru;
        });
        int[][] calves = {{-4, 32}, {3, 34}, {7, 30}};
        for (int[] c : calves) {
            spawn(level, box, origin.offset(c[0], 1, c[1]), () -> {
                SkyBisonEntity calf = ModEntities.SKY_BISON.get().create(level.getLevel());
                if (calf != null) {
                    calf.setAge(SkyBisonEntity.CALF_AGE);
                }
                return calf;
            });
        }
        int[][] lemurs = {{-29, -10, 1}, {28, 10, 1}, {-20, 8, TIER_Y[1] + 1}, {3, 3, SUMMIT + 1}};
        for (int[] l : lemurs) {
            spawn(level, box, origin.offset(l[0], l[2], l[1]), () -> ModEntities.LEMUR.get().create(level.getLevel()));
        }
    }

    private void barrel(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos at) {
        if (!box.isInside(at)) {
            return;
        }
        level.setBlock(at, Blocks.BARREL.defaultBlockState(), 2);
        if (level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
            barrel.setLootTable(LOOT_FOOD, random.nextLong());
        }
    }

    private static void spawn(WorldGenLevel level, BoundingBox box, BlockPos at, Supplier<? extends Mob> make) {
        if (!box.isInside(at)) {
            return;
        }
        Mob mob = make.get();
        if (mob == null) {
            return;
        }
        // On a meditation mat (two pixels high) he sits on it, not in it.
        double up = level.getBlockState(at).getBlock() instanceof com.kolodkingal.enlightened.block.MeditationPointBlock ? 0.125 : 0.0;
        mob.moveTo(at.getX() + 0.5, at.getY() + up, at.getZ() + 0.5, mob.getYRot(), 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        if (mob instanceof SkyBisonEntity calf) {
            calf.setAge(SkyBisonEntity.CALF_AGE);
        }
        level.addFreshEntityWithPassengers(mob);
    }

    // ---- the mountain ----

    /** The outer radius of tier {@code i} at this angle: the rock wavers round the peak. */
    private static double edge(int i, double angle) {
        return TIER_R[i] + 1.6 * Math.sin(3 * angle + i * 1.3) + 1.1 * Math.sin(7 * angle + i * 2.1) + 0.6 * Math.sin(13 * angle + i);
    }

    /** The tier a column stands in (the highest whose rock reaches it), or -1 beyond the mountain. */
    private static int tier(int dx, int dz) {
        double r = Math.sqrt(dx * dx + dz * dz);
        double a = Math.atan2(dz, dx);
        for (int i = TIER_Y.length - 1; i >= 0; i--) {
            if (r <= edge(i, a)) {
                return i;
            }
        }
        return -1;
    }

    /** The stairways: one per cliff, each on another side of the peak, cut into the rock of the tier above. */
    private record Stair(int tier, Direction up) {
    }

    private static final Stair[] STAIRS = {new Stair(1, Direction.SOUTH), new Stair(2, Direction.WEST), new Stair(3, Direction.NORTH)};

    /**
     * The height of the stairway's floor in this column, or -1 if the column is not on one. In the stair's own frame
     * (a out from the peak, b along the climb) it runs three wide just inside the upper tier's edge, from a landing on
     * the tier below, up one block a step, to a landing on the tier above.
     */
    private static int stairFloor(Stair s, int dx, int dz) {
        int a;
        int b;
        switch (s.up) {
            case SOUTH -> { a = dx; b = dz; }
            case WEST -> { a = dz; b = -dx; }
            case NORTH -> { a = -dx; b = -dz; }
            default -> { a = -dz; b = dx; }
        }
        int rise = TIER_Y[s.tier] - TIER_Y[s.tier - 1];
        int lane = (int) Math.round(TIER_R[s.tier]) - 3;
        if (a < lane || a > lane + 2) {
            return -1;
        }
        int start = -rise / 2;
        if (b < start - 4 || b > start + rise + 2) {
            return -1;
        }
        int step = Math.max(0, Math.min(rise, b - start + 1));
        return TIER_Y[s.tier - 1] + step;
    }

    /** The column's floor height (the tier's, or a stairway's), or -1 beyond the mountain. */
    static int top(int dx, int dz) {
        // A hall or a spire stands in its own notch cut into the cliff, at its ledge's height, as they do in the show.
        for (Tower t : TOWERS) {
            double x = dx - t.cx;
            double z = dz - t.cz;
            if (t.kind != Kind.GREAT && x * x + z * z <= (t.r + 0.5) * (t.r + 0.5)) {
                return t.base;
            }
        }
        for (Stair s : STAIRS) {
            int f = stairFloor(s, dx, dz);
            if (f >= 0) {
                return f;
            }
        }
        int t = tier(dx, dz);
        return t < 0 ? -1 : TIER_Y[t];
    }

    private static Stair stairOf(int dx, int dz) {
        for (Stair s : STAIRS) {
            if (stairFloor(s, dx, dz) >= 0) {
                return s;
            }
        }
        return null;
    }

    // ---- the plan ----

    /** The plan: what stands at (dx, dy, dz) from the foot of the peak's axis, dy 0 the base tier; null leaves the world. */
    public static BlockState design(int dx, int dy, int dz, double r) {
        int top = top(dx, dz);
        if (top < 0) {
            // Beyond the mountain: the air cleared over the ground near it, the world left as it is.
            return dy > 0 && r <= CLEAR ? AIR : null;
        }
        if (dy < top) {
            return rock(dx, dy, dz, dy >= top - 1);
        }
        if (dy == top) {
            Stair s = stairOf(dx, dz);
            if (s != null) {
                boolean landing = top == TIER_Y[s.tier] || top == TIER_Y[s.tier - 1];
                return landing ? FLOOR_RING : Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, s.up);
            }
            BlockState inside = towerFloor(dx, dy, dz);
            if (inside != null) {
                return inside;
            }
            return floor(dx, dz, r);
        }
        BlockState s = gyatso(dx, dy, dz);
        if (s != null) {
            return s;
        }
        s = sanctuaryDoor(dx, dy, dz);
        if (s != null) {
            return s;
        }
        for (Tower t : TOWERS) {
            s = tower(t, dx, dy, dz);
            if (s != null) {
                return s;
            }
        }
        s = furnish(dx, dy, dz);
        if (s != null) {
            return s;
        }
        if (dy == top + 1) {
            // The low wall along every drop, open where a stairway or a way comes up.
            if (stairOf(dx, dz) == null && dropsAway(dx, dz, top) && !gate(dx, dz)) {
                return PARAPET;
            }
            if (top == 0) {
                for (int[] h : HAY_AT) {
                    if (h[0] == dx && h[1] == dz) {
                        return HAY;
                    }
                }
            }
        }
        return AIR;
    }

    /** Whether the ground falls away from this column on any side (a ledge's edge, or the mountain's). */
    private static boolean dropsAway(int dx, int dz, int top) {
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : n) {
            int t = top(dx + d[0], dz + d[1]);
            if (t < top - 1 && stairOf(dx + d[0], dz + d[1]) == null) {
                return true;
            }
        }
        return false;
    }

    /** The ways onto the base tier through its outer wall: north, east and south (the west holds the statue). */
    private static boolean gate(int dx, int dz) {
        return tier(dx, dz) == 0 && (Math.abs(dx) <= 2 && dz != 0 || Math.abs(dz) <= 2 && dx > 0);
    }

    private static BlockState floor(int dx, int dz, double r) {
        int t = tier(dx, dz);
        // Paths of pale stone out along the four ways, and a ring round each tier's inner foot.
        if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) {
            return FLOOR_RING;
        }
        if (t >= 0 && t < TIER_Y.length - 1) {
            double inner = edge(t + 1, Math.atan2(dz, dx));
            if (r > inner && r < inner + 1.5) {
                return FLOOR_RING;
            }
        }
        return FLOOR;
    }

    /** Pale mountain rock: stone, andesite, calcite and diorite, snow on the ledges' lips. */
    private static BlockState rock(int x, int y, int z, boolean lip) {
        long h = (x * 3129871L) ^ (z * 116129781L) ^ (y * 7834951L);
        h = h * h * 42317861L + h * 11L;
        int roll = (int) ((h >>> 16) & 31);
        if (lip && roll < 14) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        // Pale rock, as the show paints the mountain: mostly calcite and diorite, veined with stone.
        return roll < 11 ? Blocks.CALCITE.defaultBlockState() : roll < 18 ? Blocks.DIORITE.defaultBlockState()
                : roll < 24 ? Blocks.STONE.defaultBlockState() : roll < 28 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
    }

    /** Monk Gyatso, seated in the lotus on a pedestal at the west of the base tier, facing in, under an arch. */
    private static BlockState gyatso(int dx, int dy, int dz) {
        int x = dx + 31;
        if (x < -2 || x > 1 || Math.abs(dz) > 3 || dy < 1 || dy > 8) {
            return null;
        }
        BlockState stone = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState robe = Blocks.ANDESITE.defaultBlockState();
        if (x == -2) {
            if (Math.abs(dz) == 3 && dy <= 7) {
                return WALL;
            }
            if (dy == 7 && Math.abs(dz) <= 3 || dy == 8 && Math.abs(dz) <= 1) {
                return BAND;
            }
            return null;
        }
        if (Math.abs(dz) == 3) {
            return null;
        }
        if (dy == 1) {
            return x <= 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : null;
        }
        if (dy == 2 && x <= 0 && Math.abs(dz) <= 1) {
            return robe; // the crossed legs
        }
        if (dy == 3 && x == -1 && Math.abs(dz) <= 1) {
            return robe; // the body
        }
        if (dy == 3 && x == 0 && dz == 0) {
            return Blocks.ANDESITE_SLAB.defaultBlockState(); // the hands in the lap
        }
        if (dy == 4 && x == -1 && dz == 0) {
            return robe;
        }
        if (dy == 4 && x == 0 && dz == 0) {
            return Blocks.GOLD_BLOCK.defaultBlockState(); // the medallion
        }
        if (dy == 4 && x == -1 && Math.abs(dz) == 1) {
            return stone; // the shoulders
        }
        if (dy == 5 && x == -1 && dz == 0) {
            return stone; // the head
        }
        return null;
    }

    /** The sealed sanctuary door on the north of the great tower: a round door of wood, the swirl on it in brass. */
    private static BlockState sanctuaryDoor(int dx, int dy, int dz) {
        if (dz != -7 || Math.abs(dx) > 3 || dy < SUMMIT + 1 || dy > SUMMIT + 7) {
            return null;
        }
        double cy = SUMMIT + 4.0;
        double d2 = dx * dx + (dy - cy) * (dy - cy);
        if (d2 > 9.5) {
            return null;
        }
        if (d2 < 0.5 || (Math.abs(dx) == 1 && dy == SUMMIT + 5) || (dx == 0 && dy == SUMMIT + 3)) {
            return Blocks.WAXED_CUT_COPPER.defaultBlockState();
        }
        return d2 > 6.5 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
    }

    /** What stands inside: the Avatars' statues and the mats in the sanctuary, mats in the meditation room. */
    private static BlockState furnish(int dx, int dy, int dz) {
        if (dy == SUMMIT + 1 && (dx == 0 && dz == 0 || Math.abs(dx) + Math.abs(dz) == 3 && (dx == 0 || dz == 0))) {
            return ModBlocks.MEDITATION_POINT.get().defaultBlockState();
        }
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r > 4.0 && r < 5.2 && Math.abs(dx) > 1 && Math.abs(dz) > 1 && dy > SUMMIT && dy <= SUMMIT + 4) {
            int k = dy - SUMMIT;
            return k == 1 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : k == 4 ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                    : Blocks.ANDESITE.defaultBlockState();
        }
        int mx = dx - MEDITATION_ROOM[0];
        int mz = dz - MEDITATION_ROOM[1];
        if (dy == MEDITATION_ROOM[2] + 1 && Math.abs(mx) <= 1 && Math.abs(mz) <= 1 && (mx + mz) % 2 == 0) {
            return ModBlocks.MEDITATION_POINT.get().defaultBlockState();
        }
        return null;
    }

    /** A tower's floor, where the column's floor lies inside one. */
    private static BlockState towerFloor(int dx, int dy, int dz) {
        for (Tower t : TOWERS) {
            if (dy != t.base) {
                continue;
            }
            double x = dx - t.cx;
            double z = dz - t.cz;
            double d = Math.sqrt(x * x + z * z);
            if (d > t.r) {
                continue;
            }
            if (t.kind == Kind.SPIRE) {
                return WALL;
            }
            if (t.kind == Kind.HALL) {
                return d > t.r - 1.0 ? WALL : PLANKS;
            }
            if (d > t.r - 1.0) {
                return WALL;
            }
            // The sanctuary floor: the Air Nomad swirl, three arms of blue on white.
            double arm = (Math.atan2(z, x) * 3.0 / (2.0 * Math.PI) + d * 0.22) % 1.0;
            if (arm < 0) {
                arm += 1.0;
            }
            return d >= 1.0 && arm < 0.28 ? SWIRL : SWIRL_GROUND;
        }
        return null;
    }

    private static BlockState tower(Tower t, int dx, int dy, int dz) {
        double x = dx - t.cx;
        double z = dz - t.cz;
        double d = Math.sqrt(x * x + z * z);
        if (d > t.roofR + 2.6) {
            return null;
        }
        int ly = dy - t.base;
        double angle = Math.atan2(z, x);
        if (ly >= 1 && ly <= t.h && d <= t.r) {
            if (t.kind == Kind.SPIRE) {
                return ly == t.h ? BAND : (ly % 4 == 2 && d > t.r - 1.0 ? Blocks.POLISHED_DIORITE.defaultBlockState() : WALL);
            }
            if (d <= t.r - 1.0) {
                return null; // inside: the plan's furnishings, or air
            }
            if (ly <= (t.kind == Kind.GREAT ? 4 : 3) && door(t, dx, dz)) {
                return AIR;
            }
            double step = t.kind == Kind.GREAT ? Math.PI / 4 : Math.PI / 2;
            double off = ((angle + Math.PI + step / 2) % step) - step / 2;
            boolean window = t.kind == Kind.GREAT ? ly % 7 >= 4 && ly % 7 <= 5 && ly > 5 : ly >= t.h - 4 && ly <= t.h - 2;
            if (window && Math.abs(off) * d < 0.6) {
                return AIR;
            }
            if (ly == 4 && Math.abs(((angle + Math.PI + step) % step) - step / 2) * d < 0.6) {
                return LAMP;
            }
            if (t.kind == Kind.GREAT && ly % 7 == 0) {
                return BAND; // the bands between its storeys, as the show's tower is striped
            }
            return ly == t.h ? BAND : WALL;
        }
        // The great tower's skirts: a ring of blue roof round it at each storey, the tiers of the show's tower.
        if (t.kind == Kind.GREAT && ly >= 7 && ly < t.h && ly % 7 == 0 && d > t.r && d <= t.r + 2.2) {
            return d > t.r + 1.4 ? ROOF_RIM : ROOF;
        }
        if (d > t.roofR + 0.6) {
            return null;
        }
        if (ly == t.h + 1 && d <= t.r + 0.5) {
            return CEILING;
        }
        int k = ly - t.h - 1;
        if (k >= 0 && k <= t.roofH) {
            double rr = t.roofR * (1.0 - (double) k / t.roofH);
            if (d <= rr && d > rr - 1.4) {
                return k <= 1 ? ROOF_RIM : ROOF;
            }
            if (d <= rr - 1.4 && ly > t.h + 1) {
                return AIR;
            }
            return null;
        }
        if (d < 0.9 && k > t.roofH && k <= t.roofH + 3) {
            return k == t.roofH + 3 ? Blocks.LIGHTNING_ROD.defaultBlockState() : SPIRE;
        }
        return null;
    }

    /** The doorways: the sanctuary opens to the east, south and west (the north is the sealed door); each hall opens out onto its ledge. */
    private static boolean door(Tower t, int dx, int dz) {
        if (t.kind == Kind.GREAT) {
            return Math.abs(dz) <= 1 || Math.abs(dx) <= 1 && dz > 0;
        }
        double len = Math.sqrt(t.cx * t.cx + t.cz * t.cz);
        double px = t.cx + t.cx / len * t.r;
        double pz = t.cz + t.cz / len * t.r;
        double ddx = dx - px;
        double ddz = dz - pz;
        return ddx * ddx + ddz * ddz <= 2.6;
    }

    /** The points of the plan a test may look at: the tiers' heights. */
    public static List<Integer> tierHeights() {
        List<Integer> list = new ArrayList<>();
        for (int y : TIER_Y) {
            list.add(y);
        }
        return list;
    }
}
