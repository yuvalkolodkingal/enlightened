package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Chakras;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Loadout;
import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * The lion turtles of the Avatar world: creatures as big as islands, the first homes of people, who gave the first
 * benders their bending. Here an island of real blocks, a shell of moss, grass and trees floating in the deep ocean,
 * with the lion turtle's head and flippers at its front ({@link LionTurtleEntity}). You may walk on it; while anyone
 * is near, it swims, a block at a time, so slowly that you notice only that you are far from where you started. Its
 * shell, and all that stands on it, swims with it.
 *
 * <p>To a Hero of the Village (one who saved a village from a raid) the lion turtle gives energybending and its
 * giving back, and lets him build on its shell and live there, as people lived on the lion turtles before the Avatar.
 * Nobody else may break or place a block on it.
 */
public final class LionTurtle {
    /** The shell's half-length along the heading and half-width across it. */
    public static final int LENGTH = 22;
    public static final int WIDTH = 18;
    /** How far below the sea the shell reaches, and how high above it the island's box (trees, houses) goes. */
    public static final int DEPTH = 6;
    public static final int HEIGHT = 26;
    private static final ResourceLocation HERO = ResourceLocation.withDefaultNamespace("adventure/hero_of_the_village");

    /** The box the island and everything on it fills, round its centre (at sea level). */
    public static BoundingBox box(BlockPos center) {
        int r = LENGTH + 2;
        return new BoundingBox(center.getX() - r, center.getY() - DEPTH, center.getZ() - r, center.getX() + r, center.getY() + HEIGHT, center.getZ() + r);
    }

    // ---- the island ----

    /**
     * The island's block at (u along the heading, v across it, dy from the sea) from its centre, or null where the sea
     * (or the air) is left as it is: a domed shell, rimmed in brown plates, moss and grass on its back, trees on it.
     */
    public static BlockState shell(int u, int v, int dy) {
        double q = Math.sqrt((double) u * u / (LENGTH * LENGTH) + (double) v * v / (WIDTH * WIDTH));
        if (q > 1.0) {
            return null;
        }
        int top = 1 + (int) Math.round(6.0 * (1.0 - q * q));
        int bottom = -DEPTH + (int) Math.round(3.0 * q * q);
        if (dy < bottom || dy > top + 9) {
            return null;
        }
        long h = (u * 73428767L) ^ (v * 912931L) ^ (dy * 438289L);
        h = h * h * 1103515245L + 12345L;
        int roll = (int) ((h >>> 20) & 15);
        if (dy < top) {
            if (q > 0.86) {
                // The rim of the shell: plates of brown, edged in darker brown.
                boolean seam = (Math.abs(u) + Math.abs(v * 2)) % 7 == 0;
                return seam ? Blocks.BROWN_TERRACOTTA.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
            }
            if (dy < top - 3) {
                return roll < 8 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
            }
            return Blocks.DIRT.defaultBlockState();
        }
        if (dy == top) {
            return q > 0.86 ? (roll < 6 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState())
                    : roll < 2 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        // Above the back: trees, grass and flowers.
        for (int[] t : TREES) {
            int du = u - t[0];
            int dv = v - t[1];
            int ttop = 1 + (int) Math.round(6.0 * (1.0 - ((double) t[0] * t[0] / (LENGTH * LENGTH) + (double) t[1] * t[1] / (WIDTH * WIDTH))));
            int k = dy - ttop;
            if (du == 0 && dv == 0 && k >= 1 && k <= 5) {
                return Blocks.OAK_LOG.defaultBlockState();
            }
            if (k >= 4 && k <= 7 && du * du + dv * dv + (k - 5) * (k - 5) <= 6 && !(du == 0 && dv == 0 && k <= 5)) {
                return Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
            }
        }
        if (dy == top + 1 && q < 0.8) {
            return roll == 0 ? Blocks.POPPY.defaultBlockState() : roll == 1 ? Blocks.DANDELION.defaultBlockState()
                    : roll < 6 ? Blocks.SHORT_GRASS.defaultBlockState() : null;
        }
        return null;
    }

    private static final int[][] TREES = {{-8, -5}, {4, 7}, {-2, -11}, {10, -2}, {-12, 6}};

    /** Lays the island in the world round {@code center} (sea level), its head toward {@code heading}. */
    public static void build(LevelAccessor level, BlockPos center, Direction heading, BoundingBox clip) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int u = -LENGTH; u <= LENGTH; u++) {
            for (int v = -WIDTH; v <= WIDTH; v++) {
                for (int dy = -DEPTH; dy <= 16; dy++) {
                    BlockState s = shell(u, v, dy);
                    if (s == null) {
                        continue;
                    }
                    pos.set(center.getX() + heading.getStepX() * u + heading.getClockWise().getStepX() * v, center.getY() + dy,
                            center.getZ() + heading.getStepZ() * u + heading.getClockWise().getStepZ() * v);
                    if (clip == null || clip.isInside(pos)) {
                        level.setBlock(pos, s, 2);
                    }
                }
            }
        }
    }

    /** Where the head sits: at the front of the shell, its chin in the sea. */
    public static BlockPos headPos(BlockPos center, Direction heading) {
        return center.relative(heading, LENGTH + 2).below(1);
    }

    /** Builds an island and its lion turtle at {@code center} (sea level) heading {@code heading}. */
    public static LionTurtleEntity create(ServerLevel level, BlockPos center, Direction heading) {
        build(level, center, heading, null);
        LionTurtleEntity turtle = ModEntities.LION_TURTLE.get().create(level);
        if (turtle != null) {
            turtle.setIsland(center, heading);
            level.addFreshEntity(turtle);
        }
        return turtle;
    }

    // ---- the swim ----

    /**
     * Moves everything in {@code box} one block toward {@code dir}: its blocks (with their contents) and the creatures
     * standing in it, players without a jolt. What it leaves behind is sea below {@code seaY} and air above.
     */
    public static void shift(ServerLevel level, BoundingBox box, Direction dir, int seaY) {
        BlockPos min = new BlockPos(box.minX(), box.minY(), box.minZ());
        StructureTemplate template = capture(level, box);
        // Clear the old place first (the block entities with it), from the top down so nothing falls.
        clear(level, box, seaY);
        BlockPos to = min.relative(dir);
        template.placeInWorld(level, to, to, new StructurePlaceSettings(), level.getRandom(), 2 | 16);
        // A bed on it is a home that swims: who sleeps there wakes there still.
        for (ServerPlayer p : level.players()) {
            BlockPos home = p.getRespawnPosition();
            if (home != null && p.getRespawnDimension() == level.dimension() && box.isInside(home)) {
                p.setRespawnPosition(level.dimension(), home.relative(dir), p.getRespawnAngle(), p.isRespawnForced(), false);
            }
        }
        // Those on it go with it.
        for (Entity e : riders(level, box, seaY)) {
            if (e instanceof ServerPlayer p) {
                // Absolute coordinates; the flags make the client move by the difference only, keeping its look and its motion.
                p.teleportTo(level, p.getX() + dir.getStepX(), p.getY(), p.getZ() + dir.getStepZ(), EnumSet.of(RelativeMovement.X,
                        RelativeMovement.Y, RelativeMovement.Z, RelativeMovement.X_ROT, RelativeMovement.Y_ROT), p.getYRot(), p.getXRot());
            } else {
                e.teleportTo(e.getX() + dir.getStepX(), e.getY(), e.getZ() + dir.getStepZ());
            }
        }
    }

    /**
     * Turns the island about its centre from {@code from} to {@code to} (a quarter or half turn): its blocks, and those
     * standing on it, who turn with it.
     */
    public static void turn(ServerLevel level, BlockPos center, Direction from, Direction to) {
        net.minecraft.world.level.block.Rotation rot = to == from.getClockWise() ? net.minecraft.world.level.block.Rotation.CLOCKWISE_90
                : to == from.getCounterClockWise() ? net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90
                : to == from.getOpposite() ? net.minecraft.world.level.block.Rotation.CLOCKWISE_180 : net.minecraft.world.level.block.Rotation.NONE;
        if (rot == net.minecraft.world.level.block.Rotation.NONE) {
            return;
        }
        BoundingBox box = box(center);
        BlockPos min = new BlockPos(box.minX(), box.minY(), box.minZ());
        StructureTemplate template = capture(level, box);
        clear(level, box, center.getY());
        int r = LENGTH + 2;
        template.placeInWorld(level, min, min, new StructurePlaceSettings().setRotation(rot).setRotationPivot(new BlockPos(r, 0, r)),
                level.getRandom(), 2 | 16);
        double cx = center.getX() + 0.5;
        double cz = center.getZ() + 0.5;
        float turnYaw = rot == net.minecraft.world.level.block.Rotation.CLOCKWISE_90 ? 90.0F
                : rot == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90 ? -90.0F : 180.0F;
        for (Entity e : riders(level, box, center.getY())) {
            double dx = e.getX() - cx;
            double dz = e.getZ() - cz;
            double nx = rot == net.minecraft.world.level.block.Rotation.CLOCKWISE_90 ? -dz : rot == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90 ? dz : -dx;
            double nz = rot == net.minecraft.world.level.block.Rotation.CLOCKWISE_90 ? dx : rot == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90 ? -dx : -dz;
            if (e instanceof ServerPlayer p) {
                p.teleportTo(level, cx + nx, e.getY(), cz + nz, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT), p.getYRot() + turnYaw, p.getXRot());
            } else {
                e.moveTo(cx + nx, e.getY(), cz + nz, e.getYRot() + turnYaw, e.getXRot());
            }
        }
    }

    private static StructureTemplate capture(ServerLevel level, BoundingBox box) {
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, new BlockPos(box.minX(), box.minY(), box.minZ()),
                new net.minecraft.core.Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan()), false, Blocks.STRUCTURE_VOID);
        return template;
    }

    /** Leaves the box sea below {@code seaY} and air above, emptying the chests in it first (their contents moved on). */
    private static void clear(ServerLevel level, BoundingBox box, int seaY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = box.maxY(); y >= box.minY(); y--) {
            BlockState fill = y <= seaY ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos) != fill) {
                        level.removeBlockEntity(pos);
                        level.setBlock(pos, fill, 2 | 16);
                    }
                }
            }
        }
    }

    /** The creatures and things on the island (not the lion turtle itself, nor those riding something). */
    private static List<Entity> riders(ServerLevel level, BoundingBox box, int seaY) {
        AABB area = new AABB(box.minX(), seaY - 1, box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        return level.getEntities((Entity) null, area, e -> !(e instanceof LionTurtleEntity) && !e.isPassenger() && !e.isSpectator());
    }

    /** Ticks between each block the lion turtle swims. */
    public static int moveTicks() {
        return (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LION_TURTLE_MOVE_SECONDS.get() : 40) * 20;
    }

    /** Whether the sea ahead of the island is open (no land, no ice, no other shell). */
    public static boolean openAhead(Level level, BlockPos center, Direction heading) {
        int sea = center.getY();
        for (int v = -WIDTH; v <= WIDTH; v += 6) {
            for (int u = LENGTH + 4; u <= LENGTH + 6; u++) {
                BlockPos p = new BlockPos(center.getX() + heading.getStepX() * u + heading.getClockWise().getStepX() * v, sea,
                        center.getZ() + heading.getStepZ() * u + heading.getClockWise().getStepZ() * v);
                if (!level.isLoaded(p) || !level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER) || !level.getBlockState(p.above()).isAir()
                        || !level.getFluidState(p.below(DEPTH + 1)).is(net.minecraft.tags.FluidTags.WATER)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---- who may build, and the gift ----

    /** The lion turtle whose island holds this block, if any. */
    public static LionTurtleEntity islandAt(Level level, BlockPos pos) {
        List<LionTurtleEntity> near = level.getEntitiesOfClass(LionTurtleEntity.class, new AABB(pos).inflate(LENGTH + 8, HEIGHT + 8, LENGTH + 8));
        for (LionTurtleEntity t : near) {
            if (t.island() != null && box(t.island()).isInside(pos)) {
                return t;
            }
        }
        return null;
    }

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
            p.serverLevel().playSound(null, p.blockPosition(), com.kolodkingal.enlightened.registry.ModSounds.AWAKEN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.5F);
        } else {
            say(p, "lion_turtle.enlightened.again");
        }
        if (data.element() == Element.AVATAR && data.chakras() < Chakras.COUNT) {
            say(p, "lion_turtle.enlightened.avatar_chakras");
        }
        say(p, "lion_turtle.enlightened.home");
    }

    private static void say(ServerPlayer p, String key) {
        p.sendSystemMessage(Component.translatable("lion_turtle.enlightened.says", Component.translatable(key)).withStyle(ChatFormatting.GREEN));
    }

    private LionTurtle() {
    }
}
