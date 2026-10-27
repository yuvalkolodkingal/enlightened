package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.FlyingLemurEntity;
import com.kolodkingal.enlightened.entity.GuruPathikEntity;
import com.kolodkingal.enlightened.entity.SkyBisonEntity;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.Supplier;

/**
 * The Southern Air Temple, built block by block from a plan that is a pure function of the position (so each chunk
 * builds its own share). A round terrace of stone on a plinth that fills down to the mountain, its top cut clear;
 * a great tower in the middle (the sanctuary, its floor the Air Nomad swirl in blue, Guru Pathik meditating at its
 * heart among meditation points) and four lesser towers, all of white stone under tall blue cone roofs with golden
 * spires; a courtyard on the south with hay where the sky bison calves live; flying lemurs about the terrace; chests
 * (gliders, a saddle, food) in the sanctuary and two of the towers.
 */
public class AirTemplePiece extends StructurePiece {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Enlightened.id("chests/air_temple"));
    private static final int TERRACE = 20;
    private static final int CLEAR = 24;
    private static final int UP = 42;
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

    /** The great tower and the four lesser ones: centre, wall radius, wall height, roof radius, roof height. */
    private record Tower(int cx, int cz, double r, int h, double roofR, int roofH, boolean great) {
    }

    private static final Tower[] TOWERS = {
            new Tower(0, 0, 6.5, 15, 9.0, 12, true),
            new Tower(13, 13, 3.5, 11, 5.0, 8, false),
            new Tower(-13, 13, 3.5, 11, 5.0, 8, false),
            new Tower(13, -13, 3.5, 11, 5.0, 8, false),
            new Tower(-13, -13, 3.5, 12, 5.0, 9, false),
    };
    private static final int[][] HAY_AT = {{-7, 16}, {6, 17}, {-2, 18}, {7, 10}, {-8, 10}};

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
                // The plinth: stone down to the mountain under the terrace.
                if (r <= TERRACE + 0.5) {
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
                        level.setBlock(pos, plinth(x, oy + dy, z), 2);
                    }
                }
            }
        }
        // The chests: the sanctuary, and two of the towers.
        createChest(level, box, random, origin.offset(-4, 1, 3), LOOT, null);
        createChest(level, box, random, origin.offset(13, 1, -13), LOOT, null);
        createChest(level, box, random, origin.offset(-13, 1, 13), LOOT, null);
        // Those who live here.
        spawn(level, box, origin.offset(0, 1, 0), () -> {
            GuruPathikEntity guru = ModEntities.GURU_PATHIK.get().create(level.getLevel());
            if (guru != null) {
                guru.setCustomName(Component.translatable("entity.enlightened.guru_pathik"));
                guru.setYRot(180.0F);
                guru.yHeadRot = 180.0F;
                guru.yBodyRot = 180.0F;
            }
            return guru;
        });
        int[][] calves = {{-4, 13}, {3, 15}, {0, 11}};
        for (int[] c : calves) {
            spawn(level, box, origin.offset(c[0], 1, c[1]), () -> {
                SkyBisonEntity calf = ModEntities.SKY_BISON.get().create(level.getLevel());
                if (calf != null) {
                    calf.setAge(SkyBisonEntity.CALF_AGE);
                }
                return calf;
            });
        }
        int[][] lemurs = {{-9, -6}, {9, 6}, {3, 3}};
        for (int[] l : lemurs) {
            spawn(level, box, origin.offset(l[0], 1, l[1]), () -> ModEntities.LEMUR.get().create(level.getLevel()));
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
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, mob.getYRot(), 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        if (mob instanceof SkyBisonEntity calf) {
            calf.setAge(SkyBisonEntity.CALF_AGE);
        }
        level.addFreshEntityWithPassengers(mob);
    }

    /** The plan: what stands at (dx, dy, dz) from the centre of the terrace, dy 0 its floor; null leaves the world as it is. */
    public static BlockState design(int dx, int dy, int dz, double r) {
        for (Tower t : TOWERS) {
            BlockState s = tower(t, dx, dy, dz);
            if (s != null) {
                return s;
            }
        }
        if (dy == 0) {
            if (r > TERRACE + 0.5) {
                return null;
            }
            // Rings of pale stone round the towers, and a path out to each of the four ways.
            if (Math.abs(r - 9.5) < 0.5 || Math.abs(r - 17.5) < 0.5 || (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) && r > 9) {
                return FLOOR_RING;
            }
            return FLOOR;
        }
        if (r > TERRACE - 0.5 && r <= TERRACE + 0.5 && dy == 1) {
            // The low wall round the terrace, open at the four ways.
            return Math.abs(dx) <= 2 || Math.abs(dz) <= 2 ? AIR : PARAPET;
        }
        if (dy == 1) {
            for (int[] h : HAY_AT) {
                if (h[0] == dx && h[1] == dz) {
                    return HAY;
                }
            }
        }
        return r <= CLEAR ? AIR : null;
    }

    private static BlockState tower(Tower t, int dx, int dy, int dz) {
        double x = dx - t.cx;
        double z = dz - t.cz;
        double d = Math.sqrt(x * x + z * z);
        if (d > t.roofR + 0.6) {
            return null;
        }
        double angle = Math.atan2(z, x);
        if (dy == 0 && d <= t.r) {
            if (!t.great) {
                return PLANKS;
            }
            // The sanctuary floor: the Air Nomad swirl, three arms of blue on white; a meditation point at the
            // heart where the guru sits, and four round it.
            if (dx == 0 && dz == 0 || d > 2.5 && d < 3.5 && (dx == 0 || dz == 0) && Math.abs(Math.abs(dx) + Math.abs(dz) - 3) == 0) {
                return ModBlocks.MEDITATION_POINT.get().defaultBlockState();
            }
            double arm = (angle * 3.0 / (2.0 * Math.PI) + d * 0.22) % 1.0;
            if (arm < 0) {
                arm += 1.0;
            }
            return d >= 1.0 && d <= t.r - 1.0 && arm < 0.28 ? SWIRL : SWIRL_GROUND;
        }
        if (dy >= 1 && dy <= t.h && d <= t.r) {
            if (d <= t.r - 1.0) {
                return AIR;
            }
            // The wall: doors, windows high up, lamps set in it, a band under the eaves.
            if (dy <= (t.great ? 4 : 3) && door(t, dx, dz)) {
                return AIR;
            }
            double step = t.great ? Math.PI / 4 : Math.PI / 2;
            double off = ((angle + Math.PI + step / 2) % step) - step / 2;
            if (dy >= t.h - 4 && dy <= t.h - 2 && Math.abs(off) * d < 0.6) {
                return AIR;
            }
            if (dy == 4 && Math.abs(((angle + Math.PI + step) % step) - step / 2) * d < 0.6) {
                return LAMP;
            }
            return dy == t.h ? BAND : WALL;
        }
        if (dy == t.h + 1 && d <= t.r + 0.5) {
            return CEILING;
        }
        int k = dy - t.h - 1;
        if (k >= 0 && k <= t.roofH) {
            double rr = t.roofR * (1.0 - (double) k / t.roofH);
            if (d <= rr && d > rr - 1.4) {
                return k <= 1 ? ROOF_RIM : ROOF;
            }
            if (d <= rr - 1.4 && dy > t.h + 1) {
                return AIR;
            }
            return null;
        }
        if (d < 0.9 && k > t.roofH && k <= t.roofH + 3) {
            return k == t.roofH + 3 ? Blocks.LIGHTNING_ROD.defaultBlockState() : SPIRE;
        }
        return null;
    }

    /** The doorways: the sanctuary opens to the four ways; each lesser tower faces the sanctuary. */
    private static boolean door(Tower t, int dx, int dz) {
        if (t.great) {
            return Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
        }
        double len = Math.sqrt(t.cx * t.cx + t.cz * t.cz);
        double px = t.cx - t.cx / len * t.r;
        double pz = t.cz - t.cz / len * t.r;
        double ddx = dx - px;
        double ddz = dz - pz;
        return ddx * ddx + ddz * ddz <= 2.6;
    }

    private static BlockState plinth(int x, int y, int z) {
        long h = (x * 3129871L) ^ (z * 116129781L) ^ (y * 7834951L);
        h = h * h * 42317861L + h * 11L;
        int roll = (int) ((h >>> 16) & 15);
        return roll < 2 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : roll < 4 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
    }
}
