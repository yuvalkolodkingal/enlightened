package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Every lion turtle of a world, kept with the world (saved data {@code enlightened_lion_turtles}): where its island
 * lies, the way its head faces, its size, and what it is doing: being built (a few rows a tick, so a great island
 * rises over some seconds without a pause), resting, or swimming a block ({@link LionTurtle.Stroke}).
 *
 * <p>An island as big as a lion turtle's reaches beyond what a player keeps loaded, so while a player is near it (or
 * it is being built, or in the middle of a stroke) it holds its own chunks loaded with a ticket, and lets them go a
 * while after everyone has left.
 */
public final class LionTurtles extends SavedData {
    private static final String NAME = "enlightened_lion_turtles";
    private static final TicketType<ChunkPos> TICKET = TicketType.create("enlightened_lion_turtle", Comparator.comparingLong(ChunkPos::toLong));
    /** Blocks the builder and the swim look at in a tick. */
    private static final int BUDGET = 40000;
    /** Islands chosen by the world generator (off the server thread), waiting to be raised. */
    private static final ConcurrentLinkedQueue<Pending> PENDING = new ConcurrentLinkedQueue<>();

    private record Pending(ResourceKey<Level> level, BlockPos center, Direction heading, int scale) {
    }

    public enum Phase { BUILD, REST, SWIM }

    /** One lion turtle and its island. */
    public static final class Island {
        public final int id;
        public BlockPos center;
        public final Direction heading;
        public final int scale;
        public Phase phase = Phase.BUILD;
        /** Building: the next column (row by row). Swimming: the next row of the stroke. */
        public int cursor;
        public Direction swimming;
        public int rest;
        public UUID head;
        private final Set<Long> held = new HashSet<>();
        private int lonely;

        Island(int id, BlockPos center, Direction heading, int scale) {
            this.id = id;
            this.center = center.immutable();
            this.heading = heading;
            this.scale = scale;
        }

        public LionTurtle.Shape shape() {
            return new LionTurtle.Shape(scale);
        }

        public LionTurtle.Region region(Level level) {
            return LionTurtle.Region.of(shape(), center, heading, level);
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("Id", id);
            t.putLong("Center", center.asLong());
            t.putInt("Heading", heading.get2DDataValue());
            t.putInt("Scale", scale);
            t.putString("Phase", phase.name());
            t.putInt("Cursor", cursor);
            t.putInt("Swimming", swimming == null ? -1 : swimming.get2DDataValue());
            t.putInt("Rest", rest);
            if (head != null) {
                t.putUUID("Head", head);
            }
            return t;
        }

        static Island load(CompoundTag t) {
            Island i = new Island(t.getInt("Id"), BlockPos.of(t.getLong("Center")), Direction.from2DDataValue(t.getInt("Heading")), Math.max(1, t.getInt("Scale")));
            try {
                i.phase = Phase.valueOf(t.getString("Phase"));
            } catch (IllegalArgumentException e) {
                i.phase = Phase.REST;
            }
            i.cursor = t.getInt("Cursor");
            int s = t.getInt("Swimming");
            i.swimming = s < 0 ? null : Direction.from2DDataValue(s);
            i.rest = t.getInt("Rest");
            if (t.hasUUID("Head")) {
                i.head = t.getUUID("Head");
            }
            return i;
        }
    }

    private final List<Island> islands = new ArrayList<>();
    private int nextId = 1;

    public static LionTurtles get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(LionTurtles::new, LionTurtles::load, null), NAME);
    }

    public List<Island> islands() {
        return islands;
    }

    /** Raises a lion turtle at {@code center} (the sea's surface), unless another is too near. */
    public Island add(BlockPos center, Direction heading, int scale) {
        LionTurtle.Shape s = new LionTurtle.Shape(scale);
        for (Island i : islands) {
            long reach = (long) (s.length() + i.shape().length()) + 16;
            if (i.center.distSqr(center) < reach * reach) {
                return null;
            }
        }
        Island i = new Island(nextId++, center, heading, scale);
        islands.add(i);
        setDirty();
        return i;
    }

    /** The island whose ground holds this block, if any. */
    public static Island islandAt(ServerLevel level, BlockPos pos) {
        for (Island i : get(level).islands) {
            if (i.region(level).contains(pos)) {
                return i;
            }
        }
        return null;
    }

    /** Called by the world generator: the island is raised on the server's next tick. */
    public static void queue(ResourceKey<Level> level, BlockPos center, Direction heading, int scale) {
        PENDING.add(new Pending(level, center.immutable(), heading, scale));
    }

    public static int configuredScale() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LION_TURTLE_SIZE.get() : 10;
    }

    /** Ticks between the end of one stroke and the start of the next. */
    public static int restTicks() {
        return (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LION_TURTLE_MOVE_SECONDS.get() : 40) * 20;
    }

    // ---- each tick ----

    public static void tick(MinecraftServer server) {
        Pending p;
        while ((p = PENDING.poll()) != null) {
            ServerLevel level = server.getLevel(p.level());
            if (level != null) {
                get(level).add(p.center(), p.heading(), p.scale());
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            LionTurtles data = level.getDataStorage().get(new SavedData.Factory<>(LionTurtles::new, LionTurtles::load, null), NAME);
            if (data != null) {
                for (Island i : data.islands) {
                    data.tick(level, i);
                }
            }
        }
    }

    private void tick(ServerLevel level, Island i) {
        boolean near = playerNear(level, i);
        if (near || i.phase != Phase.REST) {
            i.lonely = 0;
            if (i.held.isEmpty()) {
                hold(level, i);
            }
        } else if (!i.held.isEmpty() && ++i.lonely > 200) {
            release(level, i);
        }
        if (i.held.isEmpty() || !loaded(level, i)) {
            return;
        }
        switch (i.phase) {
            case BUILD -> {
                if (build(level, i)) {
                    i.phase = Phase.REST;
                    i.cursor = 0;
                    spawnHead(level, i);
                }
                setDirty();
            }
            case REST -> {
                if (!near) {
                    return;
                }
                if (++i.rest >= restTicks()) {
                    i.rest = 0;
                    Direction way = chooseWay(level, i);
                    if (way != null) {
                        i.swimming = way;
                        i.phase = Phase.SWIM;
                        i.cursor = new LionTurtle.Stroke(i.region(level), way).row();
                    }
                }
                setDirty();
            }
            case SWIM -> {
                LionTurtle.Stroke stroke = new LionTurtle.Stroke(i.region(level), i.swimming, i.cursor);
                if (stroke.step(level, BUDGET)) {
                    finishStroke(level, i);
                } else {
                    i.cursor = stroke.row();
                }
                setDirty();
            }
        }
    }

    /** Ends a stroke: the island's centre a block on, the head with it. */
    private void finishStroke(ServerLevel level, Island i) {
        Direction d = i.swimming;
        i.center = i.center.relative(d);
        i.phase = Phase.REST;
        i.swimming = null;
        i.cursor = 0;
        if (i.head != null && level.getEntity(i.head) instanceof LionTurtleEntity head) {
            LionTurtle.carry(level, head, d);
        }
        // The chunks it holds follow it.
        release(level, i);
        hold(level, i);
    }

    /** Swims the island a whole block at once (a command, the tests). */
    public void swimNow(ServerLevel level, Island i, Direction d) {
        new LionTurtle.Stroke(i.region(level), d).finish(level);
        i.swimming = d;
        finishStroke(level, i);
        setDirty();
    }

    /** Builds the island a budget's worth; returns whether it is finished. */
    private boolean build(ServerLevel level, Island i) {
        LionTurtle.Shape s = i.shape();
        int width = 2 * s.width() + 1;
        int total = (2 * s.length() + 1) * width;
        int spent = 0;
        while (i.cursor < total && spent < BUDGET / 4) {
            int u = i.cursor / width - s.length();
            int v = i.cursor % width - s.width();
            spent += LionTurtle.buildColumn(level, s, i.center, i.heading, u, v);
            i.cursor++;
        }
        return i.cursor >= total;
    }

    /** Builds an island at once (the tests, a small island from a command). */
    public void buildNow(ServerLevel level, Island i) {
        LionTurtle.Shape s = i.shape();
        for (int u = -s.length(); u <= s.length(); u++) {
            for (int v = -s.width(); v <= s.width(); v++) {
                LionTurtle.buildColumn(level, s, i.center, i.heading, u, v);
            }
        }
        i.phase = Phase.REST;
        i.cursor = 0;
        spawnHead(level, i);
        setDirty();
    }

    private void spawnHead(ServerLevel level, Island i) {
        if (i.head != null && level.getEntity(i.head) != null) {
            return;
        }
        LionTurtleEntity head = ModEntities.LION_TURTLE.get().create(level);
        if (head != null) {
            head.setIsland(i);
            boolean added = level.addFreshEntity(head);
            i.head = head.getUUID();
            com.kolodkingal.enlightened.Enlightened.LOGGER.debug("Lion turtle {} rose at {} (head at {}, added {})", i.id, i.center, head.blockPosition(), added);
        }
    }

    /** The way to swim: ahead if the sea is open, else to a side, else back the way it came; null if it is shut in. */
    private Direction chooseWay(ServerLevel level, Island i) {
        LionTurtle.Region r = i.region(level);
        for (Direction d : new Direction[]{i.heading, i.heading.getClockWise(), i.heading.getCounterClockWise(), i.heading.getOpposite()}) {
            if (LionTurtle.openAhead(level, r, d, i.shape().depth())) {
                return d;
            }
        }
        return null;
    }

    private static boolean playerNear(ServerLevel level, Island i) {
        LionTurtle.Region r = i.region(level);
        LionTurtle.Region wide = new LionTurtle.Region(r.cx(), r.cz(), r.seaY(), r.heading(), r.a() + 96, r.b() + 96, r.yMin(), r.yMax());
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && wide.contains(p.getBlockX(), p.getBlockZ())) {
                return true;
            }
        }
        return false;
    }

    /** The chunks the island needs: those its region touches, a chunk round, and the head's. */
    private static Set<Long> chunks(ServerLevel level, Island i) {
        LionTurtle.Region r = i.region(level);
        Set<Long> out = new HashSet<>();
        int reach = Math.max(r.a(), r.b()) + 16;
        // A chunk is needed if its nearest point to the centre lies in the region widened by a chunk.
        LionTurtle.Region wide = new LionTurtle.Region(r.cx(), r.cz(), r.seaY(), r.heading(), r.a() + 18, r.b() + 18, r.yMin(), r.yMax());
        for (int cx = (r.cx() - reach) >> 4; cx <= (r.cx() + reach) >> 4; cx++) {
            for (int cz = (r.cz() - reach) >> 4; cz <= (r.cz() + reach) >> 4; cz++) {
                int x = Math.max(cx << 4, Math.min(r.cx(), (cx << 4) + 15));
                int z = Math.max(cz << 4, Math.min(r.cz(), (cz << 4) + 15));
                if (wide.contains(x, z)) {
                    out.add(ChunkPos.asLong(cx, cz));
                }
            }
        }
        BlockPos head = LionTurtle.headPos(i.shape(), i.center, i.heading);
        out.add(ChunkPos.asLong(head.getX() >> 4, head.getZ() >> 4));
        return out;
    }

    private static void hold(ServerLevel level, Island i) {
        for (long c : chunks(level, i)) {
            ChunkPos pos = new ChunkPos(c);
            level.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
            i.held.add(c);
        }
    }

    private static void release(ServerLevel level, Island i) {
        for (long c : i.held) {
            ChunkPos pos = new ChunkPos(c);
            level.getChunkSource().removeRegionTicket(TICKET, pos, 0, pos);
        }
        i.held.clear();
    }

    private static boolean loaded(ServerLevel level, Island i) {
        for (long c : i.held) {
            if (level.getChunkSource().getChunkNow(ChunkPos.getX(c), ChunkPos.getZ(c)) == null) {
                return false;
            }
        }
        return true;
    }

    // ---- saving ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Island i : islands) {
            list.add(i.save());
        }
        tag.put("Islands", list);
        tag.putInt("NextId", nextId);
        return tag;
    }

    public static LionTurtles load(CompoundTag tag, HolderLookup.Provider registries) {
        LionTurtles data = new LionTurtles();
        for (Tag t : tag.getList("Islands", Tag.TAG_COMPOUND)) {
            data.islands.add(Island.load((CompoundTag) t));
        }
        data.nextId = Math.max(1, tag.getInt("NextId"));
        return data;
    }
}
