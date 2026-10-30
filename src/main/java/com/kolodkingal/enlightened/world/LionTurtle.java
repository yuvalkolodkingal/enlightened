package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Loadout;
import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import com.kolodkingal.enlightened.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The lion turtles of the Avatar world: creatures as big as islands, the first homes of people, who gave the first
 * benders their bending. Here an island of real blocks, a shell of moss, grass and forest floating in the deep ocean,
 * with the lion turtle's head at its front ({@link LionTurtleEntity}); {@link LionTurtles} keeps every island, builds it
 * a few rows at a time and makes it swim.
 *
 * <p>The swim: while anyone is near, the island moves a block, a row at a time from its front to its back, every row
 * taking what the row behind it held (blocks, chests and their contents, beds), and those standing on a row moving
 * with it. A seam of one doubled row runs from the front to the back of the island over some seconds; you do not see
 * it go, only find yourself far from where you began.
 *
 * <p>To a Hero of the Village (one who saved a village from a raid) the lion turtle gives energybending and its
 * giving back, reaching out its claws to his forehead and his chest, and lets him build on its shell and live there.
 * Nobody else may break or place a block on it.
 */
public final class LionTurtle {
    /** The island's half-length along the heading and half-width across it, at scale 1. */
    public static final int BASE_LENGTH = 22;
    public static final int BASE_WIDTH = 18;
    /** Flags for the swim's writes: to the clients, no shape updates, no drops. */
    static final int FLAGS = 2 | 16 | 32;
    private static final ResourceLocation HERO = ResourceLocation.withDefaultNamespace("adventure/hero_of_the_village");

    // ---- the shape ----

    /** The measures of an island of a given scale (1 to 10). */
    public record Shape(int scale) {
        public int length() {
            return BASE_LENGTH * scale;
        }

        public int width() {
            return BASE_WIDTH * scale;
        }

        /** How high above the sea the middle of its back rises. */
        public int peak() {
            return 1 + 4 * scale;
        }

        /** How far below the sea the shell reaches. */
        public int depth() {
            return 4 + 2 * scale;
        }

        /** How much room above the sea the island carries along (its forest, and the houses built on it). */
        public int above() {
            return peak() + 30 + 2 * scale;
        }
    }

    /** One column of the island: its ground, its underside, whether it is the rim, and the trees round it. */
    public record Column(int top, int bottom, boolean rim, List<int[]> trees) {
    }

    /** The height of the island's back at (u along the heading, v across), above the sea; below 1 where it has no back. */
    public static int top(Shape s, int u, int v) {
        double q2 = (double) u * u / ((double) s.length() * s.length()) + (double) v * v / ((double) s.width() * s.width());
        double fall = 1.0 - q2;
        double hills = s.scale() * 0.35 * fall * (Math.sin(u * 0.045) + Math.sin(v * 0.06 + 1.3) + Math.sin((u + v) * 0.03));
        return 1 + (int) Math.round(4.0 * s.scale() * fall + hills);
    }

    /** The column at (u, v), or null outside the island. */
    public static Column column(Shape s, int u, int v) {
        double q2 = (double) u * u / ((double) s.length() * s.length()) + (double) v * v / ((double) s.width() * s.width());
        if (q2 > 1.0) {
            return null;
        }
        double q = Math.sqrt(q2);
        int top = top(s, u, v);
        int bottom = -s.depth() + (int) Math.round(s.depth() * 0.55 * q2);
        boolean rim = q > 1.0 - (2.5 + 0.4 * s.scale()) / s.width();
        List<int[]> trees = new ArrayList<>(2);
        int c = 9;
        int cu = Math.floorDiv(u, c);
        int cv = Math.floorDiv(v, c);
        for (int i = cu - 1; i <= cu + 1; i++) {
            for (int j = cv - 1; j <= cv + 1; j++) {
                long h = mix(i * 73428767L + j * 912931L);
                if ((h & 127) >= 52) {
                    continue;
                }
                int tu = i * c + 1 + (int) ((h >>> 8) % (c - 2));
                int tv = j * c + 1 + (int) ((h >>> 16) % (c - 2));
                double tq = (double) tu * tu / ((double) s.length() * s.length()) + (double) tv * tv / ((double) s.width() * s.width());
                if (tq > 0.72) {
                    continue;
                }
                boolean great = s.scale() >= 4 && ((h >>> 24) & 31) == 0;
                int height = great ? 9 + (int) ((h >>> 30) % 3) : 4 + (int) ((h >>> 30) % 3);
                int radius = great ? 4 : 2;
                int du = u - tu;
                int dv = v - tv;
                if (du * du + dv * dv > (radius + 1) * (radius + 1)) {
                    continue;
                }
                boolean birch = !great && ((h >>> 40) % 5) == 0;
                // {ground under the trunk, trunk height, leaf radius, horizontal distance squared, birch}
                trees.add(new int[]{top(s, tu, tv), height, radius, du * du + dv * dv, birch ? 1 : 0});
            }
        }
        return new Column(top, bottom, rim, trees);
    }

    /**
     * The island's block in a column at dy from the sea, or null where the sea (or the air) is left as it is: a shell
     * of packed mud under a crust of dirt and grass, rimmed with plates of brown, its forest and flowers on its back.
     */
    public static BlockState at(Column col, int u, int v, int dy) {
        if (dy < col.bottom()) {
            return null;
        }
        int top = col.top();
        long h = mix(u * 73428767L ^ v * 912931L ^ dy * 438289L);
        int roll = (int) ((h >>> 20) & 15);
        if (dy < top) {
            if (col.rim()) {
                boolean seam = (Math.abs(u) + Math.abs(v * 2) + Math.abs(dy)) % 7 == 0;
                return seam ? Blocks.BROWN_TERRACOTTA.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
            }
            if (dy < top - 3) {
                return roll < 9 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
            }
            return Blocks.DIRT.defaultBlockState();
        }
        if (dy == top) {
            return col.rim() ? (roll < 6 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState())
                    : roll < 2 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        for (int[] t : col.trees()) {
            int k = dy - t[0];
            boolean trunk = t[3] == 0 && k >= 1 && k <= t[1];
            if (trunk) {
                return t[4] == 1 ? Blocks.BIRCH_LOG.defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
            }
            int crown = t[1] - (t[2] > 2 ? 1 : 0);
            int dk = k - crown;
            if (k > 2 && t[3] + dk * dk * 1.4 <= t[2] * t[2] + 1) {
                return (t[4] == 1 ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            }
        }
        if (dy == top + 1 && !col.rim()) {
            return roll == 0 ? Blocks.POPPY.defaultBlockState() : roll == 1 ? Blocks.DANDELION.defaultBlockState()
                    : roll < 6 ? Blocks.SHORT_GRASS.defaultBlockState() : null;
        }
        return null;
    }

    /** The highest dy of a column the island may fill (its back, its trees). */
    static int crown(Column col) {
        int up = col.top() + 1;
        for (int[] t : col.trees()) {
            up = Math.max(up, t[0] + t[1] + t[2]);
        }
        return up;
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h & Long.MAX_VALUE;
    }

    /** Where (u, v) of an island centred at {@code center} and heading {@code heading} lies in the world. */
    public static BlockPos.MutableBlockPos place(BlockPos.MutableBlockPos pos, BlockPos center, Direction heading, int u, int v, int dy) {
        Direction across = heading.getClockWise();
        return pos.set(center.getX() + heading.getStepX() * u + across.getStepX() * v, center.getY() + dy,
                center.getZ() + heading.getStepZ() * u + across.getStepZ() * v);
    }

    /** Lays one column of the island, writing only what differs; returns how many blocks it looked at. */
    public static int buildColumn(LevelAccessor level, Shape s, BlockPos center, Direction heading, int u, int v) {
        Column col = column(s, u, v);
        if (col == null) {
            return 1;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int up = crown(col);
        for (int dy = col.bottom(); dy <= up; dy++) {
            BlockState want = at(col, u, v, dy);
            if (want == null) {
                continue;
            }
            place(pos, center, heading, u, v, dy);
            if (level.getBlockState(pos) != want) {
                level.setBlock(pos, want, 2);
            }
        }
        return up - col.bottom() + 1;
    }

    /** Where the head sits: at the front of the shell, its chin in the sea. */
    public static BlockPos headPos(Shape s, BlockPos center, Direction heading) {
        return center.relative(heading, s.length() + (int) Math.round(3.5 * s.scale())).below(Math.max(1, s.scale() / 2));
    }

    // ---- the region an island fills, and its swim ----

    /** The ground an island carries: an ellipse a little wider than its shell, from below its keel to above its trees. */
    public record Region(int cx, int cz, int seaY, Direction heading, int a, int b, int yMin, int yMax) {
        public static Region of(Shape s, BlockPos center, Direction heading, Level level) {
            return new Region(center.getX(), center.getZ(), center.getY(), heading, s.length() + 6, s.width() + 6,
                    center.getY() - s.depth() - 2, Math.min(level.getMaxBuildHeight() - 1, center.getY() + s.above()));
        }

        public boolean contains(int x, int z) {
            double du = (x - cx) * heading.getStepX() + (z - cz) * heading.getStepZ();
            double dv = (x - cx) * heading.getClockWise().getStepX() + (z - cz) * heading.getClockWise().getStepZ();
            return du * du / ((double) a * a) + dv * dv / ((double) b * b) <= 1.0;
        }

        public boolean contains(BlockPos pos) {
            return pos.getY() >= yMin && pos.getY() <= yMax + 8 && contains(pos.getX(), pos.getZ());
        }

        public Region moved(Direction d) {
            return new Region(cx + d.getStepX(), cz + d.getStepZ(), seaY, heading, a, b, yMin, yMax);
        }

        /** Its reach along a horizontal direction (half its length or half its width). */
        public int along(Direction d) {
            return d.getAxis() == heading.getAxis() ? a : b;
        }

        public int across(Direction d) {
            return d.getAxis() == heading.getAxis() ? b : a;
        }
    }

    /**
     * One block of swim toward {@code dir}: the rows across the way, from the front (the first row beyond the region)
     * to the back, each taking the row behind it; the region's last row becomes sea and air. Resumable row by row.
     */
    public static final class Stroke {
        public final Region from;
        public final Direction dir;
        private int row;
        private final int last;

        public Stroke(Region from, Direction dir) {
            this(from, dir, dot(from.cx(), from.cz(), dir) + from.along(dir) + 1);
        }

        public Stroke(Region from, Direction dir, int row) {
            this.from = from;
            this.dir = dir;
            this.row = row;
            this.last = dot(from.cx(), from.cz(), dir) - from.along(dir);
        }

        public int row() {
            return row;
        }

        public boolean done() {
            return row < last;
        }

        /** Moves rows until about {@code budget} blocks were looked at; returns whether the stroke is over. */
        public boolean step(ServerLevel level, int budget) {
            int spent = 0;
            while (!done() && spent < budget) {
                spent += moveRow(level, row);
                row--;
            }
            return done();
        }

        /** Every row at once (for a small island, and the tests). */
        public void finish(ServerLevel level) {
            while (!done()) {
                moveRow(level, row--);
            }
        }

        private int moveRow(ServerLevel level, int t) {
            Direction side = dir.getClockWise();
            int mid = dot(from.cx(), from.cz(), side);
            int reach = from.across(dir);
            int seen = 0;
            BlockPos.MutableBlockPos to = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos src = new BlockPos.MutableBlockPos();
            for (int s = mid - reach; s <= mid + reach; s++) {
                int x = dir.getStepX() * t + side.getStepX() * s;
                int z = dir.getStepZ() * t + side.getStepZ() * s;
                int sx = x - dir.getStepX();
                int sz = z - dir.getStepZ();
                if (from.contains(sx, sz)) {
                    seen += copyColumn(level, sx, sz, x, z, src, to);
                } else if (from.contains(x, z)) {
                    seen += fillColumn(level, x, z, to);
                }
            }
            carry(level, t - 1, side, mid, reach);
            return Math.max(seen, 1);
        }

        private int surface(ServerLevel level, int x, int z) {
            return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        }

        private int copyColumn(ServerLevel level, int sx, int sz, int x, int z, BlockPos.MutableBlockPos src, BlockPos.MutableBlockPos to) {
            int up = Math.min(from.yMax(), Math.max(surface(level, sx, sz), surface(level, x, z)) + 1);
            for (int y = from.yMin(); y <= up; y++) {
                src.set(sx, y, sz);
                to.set(x, y, z);
                BlockState have = level.getBlockState(src);
                BlockState there = level.getBlockState(to);
                if (have == there && !have.hasBlockEntity()) {
                    continue;
                }
                CompoundTag tag = null;
                if (have.hasBlockEntity()) {
                    BlockEntity be = level.getBlockEntity(src);
                    if (be != null) {
                        tag = be.saveWithFullMetadata(level.registryAccess());
                    }
                }
                if (have != there) {
                    if (there.hasBlockEntity()) {
                        // Taken away first, so the chest that stood here does not spill what it held.
                        level.removeBlockEntity(to);
                    }
                    level.setBlock(to, have, FLAGS);
                }
                if (tag != null) {
                    BlockEntity be = level.getBlockEntity(to);
                    if (be != null) {
                        be.loadWithComponents(tag, level.registryAccess());
                        be.setChanged();
                        level.sendBlockUpdated(to, have, have, 2);
                    }
                }
            }
            return up - from.yMin() + 1;
        }

        private int fillColumn(ServerLevel level, int x, int z, BlockPos.MutableBlockPos to) {
            int up = Math.min(from.yMax(), surface(level, x, z) + 1);
            for (int y = from.yMin(); y <= up; y++) {
                to.set(x, y, z);
                BlockState want = y <= from.seaY() ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                BlockState there = level.getBlockState(to);
                if (there != want) {
                    if (there.hasBlockEntity()) {
                        level.removeBlockEntity(to);
                    }
                    level.setBlock(to, want, FLAGS);
                }
            }
            return up - from.yMin() + 1;
        }

        /** Those standing on row {@code t} (its ground now doubled one row on) move a block with it, and their beds. */
        private void carry(ServerLevel level, int t, Direction side, int mid, int reach) {
            int x0 = dir.getStepX() * t + side.getStepX() * (mid - reach);
            int z0 = dir.getStepZ() * t + side.getStepZ() * (mid - reach);
            int x1 = dir.getStepX() * t + side.getStepX() * (mid + reach);
            int z1 = dir.getStepZ() * t + side.getStepZ() * (mid + reach);
            AABB row = new AABB(Math.min(x0, x1), from.yMin() - 1, Math.min(z0, z1), Math.max(x0, x1) + 1, from.yMax() + 8, Math.max(z0, z1) + 1);
            for (Entity e : level.getEntities((Entity) null, row, e -> !(e instanceof LionTurtleEntity) && !e.isPassenger() && !e.isSpectator())) {
                BlockPos at = e.blockPosition();
                if (dot(at.getX(), at.getZ(), dir) != t || !from.contains(at.getX(), at.getZ())) {
                    continue;
                }
                LionTurtle.carry(level, e, dir);
            }
            for (ServerPlayer p : level.players()) {
                BlockPos home = p.getRespawnPosition();
                if (home != null && p.getRespawnDimension() == level.dimension() && dot(home.getX(), home.getZ(), dir) == t
                        && from.contains(home) ) {
                    p.setRespawnPosition(level.dimension(), home.relative(dir), p.getRespawnAngle(), p.isRespawnForced(), false);
                }
            }
        }
    }

    /** Moves a creature a block toward {@code dir}: a player without a jolt (his client moves by the difference only). */
    static void carry(ServerLevel level, Entity e, Direction dir) {
        if (e instanceof ServerPlayer p) {
            p.teleportTo(level, p.getX() + dir.getStepX(), p.getY(), p.getZ() + dir.getStepZ(), EnumSet.of(RelativeMovement.X,
                    RelativeMovement.Y, RelativeMovement.Z, RelativeMovement.X_ROT, RelativeMovement.Y_ROT), p.getYRot(), p.getXRot());
        } else {
            e.teleportTo(e.getX() + dir.getStepX(), e.getY(), e.getZ() + dir.getStepZ());
        }
    }

    static int dot(int x, int z, Direction d) {
        return x * d.getStepX() + z * d.getStepZ();
    }

    /** Whether the sea ahead of the region is open (deep water, no land, no ice, no other island), all of it loaded. */
    public static boolean openAhead(ServerLevel level, Region r, Direction d, int depth) {
        Direction side = d.getClockWise();
        int mid = dot(r.cx(), r.cz(), side);
        int centre = dot(r.cx(), r.cz(), d);
        int reach = r.across(d);
        for (int s = mid - reach; s <= mid + reach; s += 6) {
            int edge = Integer.MIN_VALUE;
            for (int t = centre + r.along(d); t >= centre; t--) {
                if (r.contains(d.getStepX() * t + side.getStepX() * s, d.getStepZ() * t + side.getStepZ() * s)) {
                    edge = t;
                    break;
                }
            }
            if (edge == Integer.MIN_VALUE) {
                continue;
            }
            for (int ahead = 2; ahead <= 4; ahead += 2) {
                int t = edge + ahead;
                BlockPos p = new BlockPos(d.getStepX() * t + side.getStepX() * s, r.seaY(), d.getStepZ() * t + side.getStepZ() * s);
                if (!level.isLoaded(p) || !level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER) || !level.getBlockState(p.above()).isAir()
                        || !level.getFluidState(p.below(depth + 2)).is(net.minecraft.tags.FluidTags.WATER)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---- who may build, and the gift ----

    /** Whether he saved a village from a raid (the Hero of the Village advancement). */
    public static boolean hero(ServerPlayer p) {
        AdvancementHolder adv = p.server.getAdvancements().get(HERO);
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** Whether he may break or place a block on the island: a hero, or a Creative player. */
    public static boolean mayBuild(Player p) {
        return p.isCreative() || p instanceof ServerPlayer sp && hero(sp);
    }

    /** He speaks to the lion turtle. */
    public static void talk(ServerPlayer p, LionTurtleEntity turtle) {
        if (!hero(p)) {
            say(p, "lion_turtle.enlightened.silent");
            return;
        }
        BendingData data = Benders.get(p);
        boolean had = data.has(Ability.ENERGYBENDING) && data.has(Ability.ENERGY_RESTORE);
        if (!had) {
            say(p, "lion_turtle.enlightened.hero");
            say(p, "lion_turtle.enlightened.gift");
            for (Ability a : List.of(Ability.ENERGYBENDING, Ability.ENERGY_RESTORE)) {
                if (data.level(a) < 1) {
                    data.setLevel(a, 1);
                }
                if (data.slotOf(a) < 0) {
                    Loadout.placeFree(data, a);
                }
            }
            Benders.syncIfDirty(p);
            // As it gave the first benders their bending: its claws on his forehead and his chest, and light.
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LionTurtleEntity.GIFT_TICKS, 6, false, false, false));
            turtle.beginGift(p);
            Fx.send(p.serverLevel(), Fx.LION_GIFT, p.position(), turtle.clawRoot(p), 0xFFF6D0, turtle.getId(), p.getId());
            p.serverLevel().playSound(null, p.blockPosition(), com.kolodkingal.enlightened.registry.ModSounds.AWAKEN.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.6F, 0.5F);
        } else {
            say(p, "lion_turtle.enlightened.again");
        }
        say(p, "lion_turtle.enlightened.home");
    }

    private static void say(ServerPlayer p, String key) {
        p.sendSystemMessage(Component.translatable("lion_turtle.enlightened.says", Component.translatable(key)).withStyle(ChatFormatting.GREEN));
    }

    /** The point between the eyes and the middle of the chest, where the claws touch. */
    public static Vec3[] touchPoints(Entity e, float partial) {
        Vec3 base = e.getPosition(partial);
        return new Vec3[]{base.add(0, e.getBbHeight() * 0.88, 0), base.add(0, e.getBbHeight() * 0.58, 0)};
    }

    private LionTurtle() {
    }
}
