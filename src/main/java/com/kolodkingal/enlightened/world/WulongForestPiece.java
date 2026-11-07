package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.entity.NationNpcEntity;
import com.kolodkingal.enlightened.entity.OzaiEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Wulong Forest (1.0.27), where Fire Lord Ozai waits for Sozin's Comet: a field of bare earth ringed by a forest of
 * tall rock spires; three Fire Nation airships hang over it (red hulls ribbed in black, the Fire Nation emblem on
 * their flanks, a gondola under each), Ozai on the deck of the flagship in the middle; the Fire Nation herald on the
 * field; and on the north side a small Fire Palace, red walls and a tiered dark roof, a throne before a wall of
 * flame, and beneath it, down a ladder, the prison: a corridor and four barred cells, one of them waiting for Ozai.
 */
public class WulongForestPiece extends DesignPiece {
    private static final int CLEAR = 72;
    /** The open field in the middle, where the fight is. */
    public static final int FIELD = 24;
    /** The palace's footprint (north of the field). */
    public static final int PAL_X = 14;
    public static final int PAL_Z0 = -64;
    public static final int PAL_Z1 = -46;
    /** The prison under it: floor, rooms, ceiling. */
    public static final int PRISON_FLOOR = -6;
    /** Ozai's cell (the centre of its floor, relative to the origin). */
    public static final BlockPos OZAI_CELL = new BlockPos(3, PRISON_FLOOR + 1, -55);
    /** The flagship: centre of its hull, length and radius. */
    public static final int SHIP_X = 0;
    public static final int SHIP_Y = 50;
    public static final int SHIP_Z = 6;
    public static final int SHIP_LEN = 40;
    public static final int SHIP_R = 7;
    private static final int[][] SHIPS = {{SHIP_X, SHIP_Y, SHIP_Z, SHIP_LEN, SHIP_R}, {-32, 42, -26, 30, 5}, {30, 45, 32, 30, 5}};

    /** Where Ozai waits on the flagship's deck. */
    public static BlockPos ozaiDeck() {
        return new BlockPos(SHIP_X, SHIP_Y - SHIP_R - 2, SHIP_Z);
    }

    public static BlockPos heraldSpot() {
        return new BlockPos(0, 1, 18);
    }

    public WulongForestPiece(BlockPos origin) {
        super(ModStructures.WULONG_FOREST_PIECE.get(), origin, CLEAR, 64, 8);
    }

    public WulongForestPiece(CompoundTag tag) {
        super(ModStructures.WULONG_FOREST_PIECE.get(), tag, CLEAR);
    }

    private static double r(int dx, int dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean palace(int dx, int dz) {
        return Math.abs(dx) <= PAL_X && dz >= PAL_Z0 && dz <= PAL_Z1;
    }

    @Override
    protected boolean footprint(int dx, int dz) {
        return r(dx, dz) <= CLEAR;
    }

    @Override
    protected int up(int dx, int dz) {
        return 64;
    }

    @Override
    protected int down(int dx, int dz) {
        return palace(dx, dz) ? 8 : 0;
    }

    // ---- the spires ----

    private static final int CELL = 12;

    /** The spire whose rock fills this block, if any: by the spires of the nine cells round the column. */
    private static boolean spire(int dx, int dy, int dz) {
        int cx = Math.floorDiv(dx + CELL / 2, CELL);
        int cz = Math.floorDiv(dz + CELL / 2, CELL);
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int j = cz - 1; j <= cz + 1; j++) {
                long h = hash(i * 31 + 7, j * 17 - 3);
                if ((h & 7) == 0) {
                    continue;
                }
                int sx = i * CELL + (int) ((h >> 3) % 7) - 3;
                int sz = j * CELL + (int) ((h >> 7) % 7) - 3;
                double rr = r(sx, sz);
                if (rr < FIELD + 4 || rr > CLEAR - 5 || palace(sx, sz) || Math.abs(sx) <= PAL_X + 4 && sz >= PAL_Z0 - 4 && sz <= PAL_Z1 + 6) {
                    continue;
                }
                int base = 2 + (int) ((h >> 11) % 3);
                int height = 12 + (int) ((h >> 14) % 22);
                if (dy > height) {
                    continue;
                }
                double t = dy / (double) height;
                double radius = base * (1.0 - t * 0.7) + 0.3 * Math.sin(dy * 0.7 + i);
                double d = Math.sqrt((dx - sx) * (dx - sx) + (dz - sz) * (dz - sz));
                if (d <= radius) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockState rock(int dx, int dy, int dz) {
        long h = hash(dx * 3 + dy * 131, dz * 5 - dy * 17);
        int k = (int) (Math.abs(h) % 10);
        return k < 5 ? Blocks.STONE.defaultBlockState() : k < 7 ? Blocks.ANDESITE.defaultBlockState()
                : k < 9 ? Blocks.TUFF.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
    }

    // ---- the airships ----

    private static BlockState ship(int dx, int dy, int dz) {
        for (int[] s : SHIPS) {
            BlockState b = ship(dx - s[0], dy - s[1], dz - s[2], s[3], s[4]);
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    /** An airship in its own frame (along x): the hull, ribs, emblem, nose, fins and gondola. */
    private static BlockState ship(int x, int y, int z, int len, int rad) {
        double half = len / 2.0;
        if (Math.abs(x) > half + 4 || Math.abs(z) > rad + 2 || y < -rad - 4 || y > rad + 4) {
            return null;
        }
        double e = (x / half) * (x / half) + (y / (double) rad) * (y / (double) rad) + (z / (double) rad) * (z / (double) rad);
        double inner = (x / (half - 1.2)) * (x / (half - 1.2)) + (y / (rad - 1.2)) * (y / (rad - 1.2)) + (z / (rad - 1.2)) * (z / (rad - 1.2));
        if (e <= 1.0 && inner > 1.0) {
            // The emblem on both flanks: a dark disc with the flame in it.
            if (Math.abs(z) >= rad - 2 && x * x + y * y <= 9) {
                boolean flame = (x == 0 && y >= -1 && y <= 2) || (Math.abs(x) == 1 && (y == 0 || y == -1)) || (x == 0 && y == -2);
                return flame ? Blocks.ORANGE_TERRACOTTA.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            }
            if (x >= half - 3) {
                return Blocks.NETHER_BRICKS.defaultBlockState();
            }
            if (Math.floorMod(x, 5) == 0) {
                return Blocks.NETHER_BRICKS.defaultBlockState();
            }
            return Blocks.RED_TERRACOTTA.defaultBlockState();
        }
        if (e <= 1.0) {
            return null;
        }
        // The fins at the tail.
        if (x <= -half + 3 && x >= -half - 2 && z == 0 && Math.abs(y) <= rad + 3 && Math.abs(y) >= rad - 1) {
            return Blocks.NETHER_BRICK_FENCE.defaultBlockState();
        }
        if (x <= -half + 3 && x >= -half - 2 && y == 0 && Math.abs(z) <= rad + 2 && Math.abs(z) >= rad - 1) {
            return Blocks.NETHER_BRICK_FENCE.defaultBlockState();
        }
        // The gondola under the hull.
        int gx = len / 4;
        if (Math.abs(x) <= gx && Math.abs(z) <= 2) {
            if (y == -rad - 3) {
                return Blocks.DARK_OAK_PLANKS.defaultBlockState();
            }
            if (y == -rad - 2 && (Math.abs(z) == 2 || Math.abs(x) == gx)) {
                return Blocks.IRON_BARS.defaultBlockState();
            }
            if (y == -rad - 1 && Math.abs(z) <= 1 && Math.abs(x) % 4 == 0) {
                return Blocks.CHAIN.defaultBlockState();
            }
        }
        return null;
    }

    // ---- the palace and its prison ----

    private static BlockState palaceBlock(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        boolean edge = ax == PAL_X || dz == PAL_Z0 || dz == PAL_Z1;
        if (dy < PRISON_FLOOR || dy > 13) {
            return dy < PRISON_FLOOR ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (dy < 0) {
            return prison(dx, dy, dz);
        }
        // The shaft down to the prison, with its ladder.
        if (dx == 13 && dz == -49 && dy <= 1) {
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        }
        if (dy == 0) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (dy >= 9) {
            int inset = (dy - 9) * 2;
            if (dy == 13) {
                return dz == (PAL_Z0 + PAL_Z1) / 2 && ax <= 6 ? Blocks.YELLOW_TERRACOTTA.defaultBlockState() : Blocks.AIR.defaultBlockState();
            }
            return ax <= PAL_X - inset && dz >= PAL_Z0 + inset && dz <= PAL_Z1 - inset ? Blocks.NETHER_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (edge) {
            if (dz == PAL_Z1 && ax <= 2 && dy <= 4) {
                return Blocks.AIR.defaultBlockState();
            }
            if (dy == 8) {
                return Blocks.YELLOW_TERRACOTTA.defaultBlockState();
            }
            boolean pillar = (ax == PAL_X && (dz - PAL_Z0) % 4 == 0) || ((dz == PAL_Z0 || dz == PAL_Z1) && ax % 4 == 2);
            if (pillar) {
                return Blocks.NETHER_BRICKS.defaultBlockState();
            }
            return dy >= 3 && dy <= 5 && ax % 4 == 0 && dz != PAL_Z0 ? Blocks.RED_STAINED_GLASS.defaultBlockState() : Blocks.RED_TERRACOTTA.defaultBlockState();
        }
        // The hall: the throne before a wall of fire, red carpet to the door, lanterns.
        if (dy == 1) {
            if (ax <= 3 && dz >= PAL_Z0 + 1 && dz <= PAL_Z0 + 3) {
                return dz == PAL_Z0 + 1 ? Blocks.CAMPFIRE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            }
            if (ax <= 1 && dz > PAL_Z0 + 3) {
                return Blocks.RED_CARPET.defaultBlockState();
            }
        }
        if (dy == 2 && dx == 0 && dz == PAL_Z0 + 2) {
            return Blocks.RED_NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        }
        if (dy == 7 && (ax == 7 || ax == 0) && (dz - PAL_Z0) % 6 == 3) {
            return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        }
        if (dx == 13 && dz == -49 && dy <= 1) {
            return Blocks.AIR.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    /** Under the palace: a corridor along x, four barred cells to its north, the ladder's foot in the east. */
    private static BlockState prison(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        if (dy == PRISON_FLOOR) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (dy == -1) {
            return dx == 13 && dz == -49 ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST) : Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (dy == -2) {
            if (dx == 13 && dz == -49) {
                return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
            }
            return (ax == 6 || ax == 0) && dz == -49 ? Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true) : Blocks.STONE_BRICKS.defaultBlockState();
        }
        // dy -5..-3: rooms.
        if (dx == 13 && dz == -49) {
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        }
        if (dz >= -50 && dz <= -48 && ax <= 13) {
            return Blocks.AIR.defaultBlockState();
        }
        if (dz == -51 && ax <= 12) {
            boolean wall = ax == 0 || ax == 6 || ax == 12;
            return wall ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState();
        }
        if (dz >= -57 && dz <= -52 && ax <= 12) {
            boolean wall = ax == 0 || ax == 6 || ax == 12;
            if (wall) {
                return Blocks.STONE_BRICKS.defaultBlockState();
            }
            if (dy == PRISON_FLOOR + 1 && dz == -57 && (dx == 2 || dx == -4 || dx == 8 || dx == -10)) {
                return Blocks.CHAIN.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    @Override
    protected BlockState design(int dx, int dy, int dz) {
        double r = r(dx, dz);
        if (palace(dx, dz)) {
            return palaceBlock(dx, dy, dz);
        }
        if (dy < 0) {
            return null;
        }
        BlockState s = dy > 14 ? ship(dx, dy, dz) : null;
        if (s != null) {
            return s;
        }
        if (dy == 0) {
            long h = hash(dx, dz);
            if (r <= FIELD) {
                int k = (int) (Math.abs(h) % 6);
                return k < 3 ? Blocks.COARSE_DIRT.defaultBlockState() : k < 5 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            }
            if (spire(dx, 0, dz)) {
                return rock(dx, 0, dz);
            }
            int k = (int) (Math.abs(h) % 8);
            return k < 4 ? Blocks.COARSE_DIRT.defaultBlockState() : k < 6 ? Blocks.STONE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (dy <= 36 && r > FIELD && spire(dx, dy, dz)) {
            // The top of a spire wears a cap of grass.
            return !spire(dx, dy + 1, dz) && (hash(dx, dz) & 1) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : rock(dx, dy, dz);
        }
        return dy <= 36 ? Blocks.AIR.defaultBlockState() : null;
    }

    @Override
    protected void decorate(WorldGenLevel level, BoundingBox box, RandomSource random) {
        BlockPos deck = origin.offset(ozaiDeck());
        if (box.isInside(deck)) {
            OzaiEntity ozai = ModEntities.OZAI.get().create(level.getLevel());
            if (ozai != null) {
                ozai.place(deck, origin.above(), origin.offset(OZAI_CELL));
                ozai.moveTo(deck.getX() + 0.5, deck.getY(), deck.getZ() + 0.5, 180.0F, 0.0F);
                level.addFreshEntityWithPassengers(ozai);
            }
        }
        BlockPos herald = origin.offset(heraldSpot());
        if (box.isInside(herald)) {
            NationNpcEntity npc = ModEntities.NATION_NPC.get().create(level.getLevel());
            if (npc != null) {
                npc.kind(NationNpcEntity.Kind.HERALD);
                npc.moveTo(herald.getX() + 0.5, herald.getY(), herald.getZ() + 0.5, 180.0F, 0.0F);
                npc.setYHeadRot(180.0F);
                level.addFreshEntityWithPassengers(npc);
            }
        }
    }
}
