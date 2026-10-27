package com.kolodkingal.enlightened.bending;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Blocks bending raises for a while (earth, sand, metal and ice walls, pillars, lava paths): each is put down with
 * the block that was there before and the game time it must go back, and the server tick puts them back in order,
 * newest last. A cell a player changed meanwhile is left as he made it. Everything is put back when the server stops
 * (so a wall never outlives its bender's session) and when a bender loses his element.
 */
public final class TempBlocks {
    private record Placed(ResourceKey<Level> dimension, BlockPos pos, BlockState before, BlockState placed, long until, UUID owner) {
    }

    private static final List<Placed> PLACED = new ArrayList<>();
    private static final Map<Long, Integer> COUNT_BY_CELL = new HashMap<>();

    /**
     * Puts {@code state} at {@code pos} for {@code ticks}, if the player may ({@link BlockRights#mayPlace}) and the
     * cell is not already raised. Returns true if the block was placed.
     */
    public static boolean place(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (isRaised(level, pos) || !BlockRights.mayPlace(player, level, pos)) {
            return false;
        }
        BlockState before = level.getBlockState(pos);
        if (!level.setBlock(pos, state, 3)) {
            return false;
        }
        PLACED.add(new Placed(level.dimension(), pos.immutable(), before, state, level.getGameTime() + ticks, player.getUUID()));
        return true;
    }

    /** Keeps a raised block up for at least {@code ticks} more (the bubble of air that follows a swimmer). Returns whether it was raised. */
    public static boolean extend(ServerLevel level, BlockPos pos, int ticks) {
        long until = level.getGameTime() + ticks;
        for (int i = 0; i < PLACED.size(); i++) {
            Placed p = PLACED.get(i);
            if (p.dimension == level.dimension() && p.pos.equals(pos)) {
                if (p.until < until) {
                    PLACED.set(i, new Placed(p.dimension, p.pos, p.before, p.placed, until, p.owner));
                }
                return true;
            }
        }
        return false;
    }

    /** Whether the cell holds a block bending raised and has not yet put back. */
    public static boolean isRaised(ServerLevel level, BlockPos pos) {
        for (Placed p : PLACED) {
            if (p.dimension == level.dimension() && p.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Puts back every raised block whose time has come. Called once per server tick. */
    public static void tick(MinecraftServer server) {
        if (PLACED.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Placed> it = PLACED.iterator();
        while (it.hasNext()) {
            Placed p = it.next();
            if (p.until > now) {
                continue;
            }
            it.remove();
            restore(server, p);
        }
    }

    private static void restore(MinecraftServer server, Placed p) {
        ServerLevel level = server.getLevel(p.dimension);
        if (level == null || !level.isLoaded(p.pos)) {
            return;
        }
        BlockState now = level.getBlockState(p.pos);
        if (now != p.placed && !now.is(p.placed.getBlock())) {
            return; // someone changed it meanwhile: it is theirs now
        }
        level.setBlock(p.pos, p.before.isAir() ? Blocks.AIR.defaultBlockState() : p.before, 3);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, p.placed), p.pos.getX() + 0.5, p.pos.getY() + 0.5, p.pos.getZ() + 0.5,
                6, 0.3, 0.3, 0.3, 0.05);
        if (level.random.nextInt(4) == 0) {
            level.playSound(null, p.pos, p.placed.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5F, 0.8F);
        }
    }

    /** Puts back every block this bender raised, now. */
    public static void restoreAll(MinecraftServer server, UUID owner) {
        Iterator<Placed> it = PLACED.iterator();
        List<Placed> mine = new ArrayList<>();
        while (it.hasNext()) {
            Placed p = it.next();
            if (p.owner.equals(owner)) {
                it.remove();
                mine.add(p);
            }
        }
        for (Placed p : mine) {
            restore(server, p);
        }
    }

    /** The server stops: every raised block goes back before the world is saved. */
    public static void restoreEverything(MinecraftServer server) {
        List<Placed> all = new ArrayList<>(PLACED);
        PLACED.clear();
        for (Placed p : all) {
            restore(server, p);
        }
    }

    public static int count() {
        return PLACED.size();
    }

    public static void clear() {
        PLACED.clear();
        COUNT_BY_CELL.clear();
    }

    private TempBlocks() {
    }
}
