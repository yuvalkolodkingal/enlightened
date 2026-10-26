package com.kolodkingal.enlightened.water;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.BlockRights;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.item.WaterSkinItem;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a waterbender's water comes from. He carries a few units of water with him (the HUD shows them); they fill
 * up whenever a lake, a river or any water is within reach, and every ability spends one. Away from water he draws
 * on his Water Skin (one use a unit), and a bender who learned Plant Water takes it from the plants round him, which
 * wither.
 */
public final class WaterSources {

    public static int range() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.WATER_SOURCE_RANGE.get() : 6;
    }

    /** How much water the bender carries at most: three units, four for an adept, six for a master of the whip. */
    public static int capacity(Cast c) {
        int whip = c.data.level(Ability.WATER_WHIP);
        return whip >= 3 ? 6 : whip == 2 ? 4 : 3;
    }

    /** The nearest water block within reach, or null. */
    @Nullable
    public static BlockPos waterNear(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.getFluidState(pos).is(Fluids.WATER)) {
                double d = pos.distSqr(center);
                if (d < bestDist) {
                    bestDist = d;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    /** Every tick: near water the carried water fills up (with a trickle of droplets from the water to the hand). */
    public static void refill(Cast c) {
        ServerPlayer p = c.player;
        int cap = capacity(c);
        if (c.data.water() >= cap || p.tickCount % 10 != 0) {
            return;
        }
        BlockPos water = p.isInWater() ? p.blockPosition() : waterNear(c.level(), p.blockPosition(), range());
        if (water == null) {
            return;
        }
        c.data.setWater(Math.min(cap, c.data.water() + 1));
        Vec3 from = Vec3.atCenterOf(water);
        c.level().sendParticles(ParticleTypes.SPLASH, from.x, from.y + 0.5, from.z, 4, 0.3, 0.2, 0.3, 0.0);
        if (c.data.water() >= cap) {
            c.sound(ModSounds.WATER_GATHER.get(), 0.6F, 1.2F);
        }
    }

    /** Takes one unit of water for an ability, from what he carries, his skin or the plants. Tells him when there is none. */
    public static boolean take(Cast c) {
        if (c.player.isCreative()) {
            return true;
        }
        if (c.data.water() >= 1) {
            c.data.setWater(c.data.water() - 1);
            return true;
        }
        if (c.player.isInWater()) {
            return true;
        }
        BlockPos water = waterNear(c.level(), c.player.blockPosition(), range());
        if (water != null) {
            return true;
        }
        for (ItemStack stack : c.player.getInventory().items) {
            if (stack.getItem() instanceof WaterSkinItem && WaterSkinItem.drain(stack)) {
                c.level().sendParticles(ParticleTypes.SPLASH, c.player.getX(), c.player.getY() + 1, c.player.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
                return true;
            }
        }
        if (c.data.has(Ability.PLANT_WATER) && fromPlants(c)) {
            return true;
        }
        c.refuse("message.enlightened.water.no_water");
        return false;
    }

    /** The water of the plants round him (a radius that grows with Plant Water's level); the plant withers. */
    private static boolean fromPlants(Cast c) {
        int level = c.data.level(Ability.PLANT_WATER);
        int radius = level >= 3 ? 6 : level == 2 ? 4 : 3;
        ServerLevel world = c.level();
        BlockPos center = c.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            BlockState state = world.getBlockState(pos);
            if (!state.is(ModTags.WATER_PLANTS) || !BlockRights.mayBreak(c.player, world, pos.immutable(), state)) {
                continue;
            }
            BlockState dead = state.is(net.minecraft.tags.BlockTags.LEAVES) ? Blocks.AIR.defaultBlockState()
                    : state.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState()
                    : state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) ? Blocks.DEAD_BUSH.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            world.setBlock(pos, dead, 3);
            Vec3 from = Vec3.atCenterOf(pos);
            world.sendParticles(ParticleTypes.SPLASH, from.x, from.y, from.z, 8, 0.3, 0.3, 0.3, 0.0);
            c.level().sendParticles(ParticleTypes.FALLING_WATER, from.x, from.y, from.z, 4, 0.2, 0.2, 0.2, 0.0);
            c.sound(ModSounds.WATER_GATHER.get(), 0.6F, 0.8F);
            return true;
        }
        return false;
    }

    private WaterSources() {
    }
}
