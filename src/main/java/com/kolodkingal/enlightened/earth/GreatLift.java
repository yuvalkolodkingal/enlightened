package com.kolodkingal.enlightened.earth;

import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.BlockRights;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.entity.FlyingSlabEntity;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Great Lift, for an earthbender who has opened all seven chakras: a whole stretch of the land in front of him
 * (<b>7 by 4 / 9 by 5 / 11 by 6</b> blocks, two deep) tears free with everything that stands on it (grass, flowers,
 * trees up to five blocks high) and hangs in the air before him. Press again to <b>hurl</b> it where he looks (it
 * crushes what it strikes, and falls apart into the land where it lands); sneak and press to <b>set it down</b> whole
 * where he looks.
 */
public final class GreatLift implements AbilityLogic {
    /** How much a hurled slab does to what it strikes, by level. */
    public static float damage(int level) {
        return 16.0F + 4.0F * level;
    }

    public static int width(int level) {
        return 5 + 2 * level;
    }

    public static int depth(int level) {
        return 3 + level;
    }

    private static final int ABOVE = 5;
    private final Map<UUID, Integer> held = new HashMap<>();

    /** The slab this bender holds, if he holds one. */
    public FlyingSlabEntity holding(ServerPlayer p) {
        Integer id = held.get(p.getUUID());
        if (id == null) {
            return null;
        }
        Entity e = p.serverLevel().getEntity(id);
        if (e instanceof FlyingSlabEntity slab && slab.isAlive() && slab.mode() == FlyingSlabEntity.Mode.HELD) {
            return slab;
        }
        held.remove(p.getUUID());
        return null;
    }

    @Override
    public void key(Cast c, boolean down) {
        if (!down) {
            return;
        }
        ServerPlayer p = c.player;
        ServerLevel level = c.level();
        FlyingSlabEntity slab = holding(p);
        if (slab != null) {
            held.remove(p.getUUID());
            if (p.isShiftKeyDown()) {
                Vec3 eye = p.getEyePosition();
                BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(32)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                Vec3 at = hit.getType() == HitResult.Type.BLOCK ? Vec3.atBottomCenterOf(hit.getBlockPos().above())
                        : eye.add(p.getLookAngle().scale(12));
                slab.setDown(level, at);
            } else {
                slab.throwAlong(p.getLookAngle(), 1.3);
            }
            c.sound(ModSounds.EARTH_RISE.get(), 1.6F, 0.5F);
            c.cooldown(c.by(600, 500, 400));
            return;
        }
        if (!c.ready()) {
            return;
        }
        Map<BlockPos, BlockState> land = tear(c);
        if (land.size() < 6) {
            c.refuse("message.enlightened.earth.no_earth");
            return;
        }
        slab = FlyingSlabEntity.of(level, land, p);
        slab.setDamage(damage(c.level));
        slab.setShatters(false);
        slab.hold(4.0 + depth(c.level));
        level.addFreshEntity(slab);
        held.put(p.getUUID(), slab.getId());
        Vec3 mid = slab.position();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), mid.x, mid.y + 0.5, mid.z,
                120, width(c.level) * 0.4, 0.6, depth(c.level) * 0.4, 0.2);
        c.sound(ModSounds.EARTH_RISE.get(), 2.0F, 0.4F);
        c.exhaust(2.0F);
    }

    /**
     * Takes the land in front of the bender out of the world: in each column the top two blocks of earth and whatever
     * stands on them, where he may break it and nothing in it is a container or unbreakable.
     */
    static Map<BlockPos, BlockState> tear(Cast c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level();
        Vec3 facing = Shots.facing(p);
        Vec3 side = new Vec3(-facing.z, 0, facing.x);
        int w = width(c.level);
        int d = depth(c.level);
        Map<BlockPos, BlockState> taken = new LinkedHashMap<>();
        Map<BlockPos, BlockState> toClear = new LinkedHashMap<>();
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < w; j++) {
                Vec3 col = p.position().add(facing.scale(2.0 + i)).add(side.scale(j - (w - 1) / 2.0));
                BlockPos foot = BlockPos.containing(col);
                BlockPos top = EarthBending.topEarth(level, foot.getX(), foot.getY() - 1, foot.getZ());
                if (top == null || taken.containsKey(top)) {
                    continue;
                }
                for (int dy = -1; dy <= ABOVE; dy++) {
                    BlockPos at = top.above(dy);
                    BlockState s = level.getBlockState(at);
                    if (s.isAir() || !s.getFluidState().isEmpty() && !s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                        if (dy > 0) {
                            break;
                        }
                        continue;
                    }
                    if (dy <= 0 && !s.is(ModTags.EARTH)) {
                        continue;
                    }
                    if (s.hasBlockEntity() || s.getDestroySpeed(level, at) < 0 || !BlockRights.mayBreak(p, level, at, s)) {
                        break;
                    }
                    taken.put(at.immutable(), s);
                }
            }
        }
        // Out of the world, from the top down, quietly (nothing pops off as an item).
        taken.keySet().stream().sorted((a, b) -> Integer.compare(b.getY(), a.getY()))
                .forEach(at -> level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        return taken;
    }

    @Override
    public void end(Cast c) {
        FlyingSlabEntity slab = holding(c.player);
        if (slab != null) {
            slab.setDown(c.level(), null);
        }
        held.remove(c.player.getUUID());
    }

    @Override
    public void forget(ServerPlayer player) {
        FlyingSlabEntity slab = holding(player);
        if (slab != null) {
            slab.setDown(player.serverLevel(), null);
        }
        held.remove(player.getUUID());
    }

    @Override
    public void clear() {
        held.clear();
    }
}
