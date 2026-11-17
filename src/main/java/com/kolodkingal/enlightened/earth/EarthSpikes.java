package com.kolodkingal.enlightened.earth;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Earth Spikes (1.1.0): the ground under the bender's mark rumbles, and a spike of stone shoots out of it, a block a
 * tick, throwing whoever stands there into the air; a moment later it crumbles back from the tip down. Distinct from
 * the pillar, which rises under the bender himself: the spike is a weapon, raised under someone else.
 *
 * <ul>
 * <li>Novice: one spike, two blocks high.</li>
 * <li>Adept: three blocks high, and four small spikes burst round it.</li>
 * <li>Master: four blocks high, eight small spikes round it, and a trail of small spikes races along the ground from
 * the bender to the mark before the great one bursts.</li>
 * </ul>
 * The spikes are earth the bender raises for a while ({@link TempBlocks}): the ground is whole again when they fall.
 */
public final class EarthSpikes implements AbilityLogic {
    /** Ticks the ground rumbles before the spike bursts out (a quick foe can still step away). */
    public static final int RUMBLE = 4;
    /** Ticks a small spike of the trail or the ring stands. */
    private static final int SMALL_STAND = 24;

    /**
     * One spike: where its foot is (the top of the ground), what it is made of, how high it grows, the tick it
     * bursts, and whom it already struck.
     */
    private static final class Spike {
        final ServerLevel level;
        final BlockPos ground;
        final BlockState shaft;
        final int height;
        final long burst;
        final float damage;
        final boolean main;
        final int stand;
        final Set<Integer> struck = new HashSet<>();
        int grown;

        Spike(ServerLevel level, BlockPos ground, BlockState shaft, int height, long burst, float damage, boolean main, int stand) {
            this.level = level;
            this.ground = ground;
            this.shaft = shaft;
            this.height = height;
            this.burst = burst;
            this.damage = damage;
            this.main = main;
            this.stand = stand;
        }
    }

    private final Map<UUID, List<Spike>> spikes = new HashMap<>();

    static double range() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.EARTH_SPIKE_RANGE.get() : 20;
    }

    static float baseDamage() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.EARTH_SPIKE_DAMAGE.get().floatValue() : 6.0F;
    }

    static int standTicks() {
        return 20 * (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.EARTH_SPIKE_SECONDS.get() : 3);
    }

    /** What a spike out of this ground is made of: rock under soil, sandstone under sand, the stone itself elsewhere. */
    static BlockState shaftOf(BlockState ground) {
        if (ground.is(ModTags.SAND)) {
            return Blocks.SANDSTONE.defaultBlockState();
        }
        if (Mud.muddy(ground) || !ground.isSolid() || ground.hasBlockEntity()) {
            return Blocks.STONE.defaultBlockState();
        }
        return ground.getBlock().defaultBlockState();
    }

    /** The point of a spike: pointed dripstone, its tip up. */
    static BlockState tip() {
        //#if >=26.3
        //$$ // (26.1: the dripstone's thickness is a speleothem's)
        //$$ return Blocks.POINTED_DRIPSTONE.defaultBlockState()
        //$$         .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.VERTICAL_DIRECTION, Direction.UP)
        //$$         .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SPELEOTHEM_THICKNESS,
        //$$                 net.minecraft.world.level.block.state.properties.SpeleothemThickness.TIP);
        //#else
        return Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.VERTICAL_DIRECTION, Direction.UP)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DRIPSTONE_THICKNESS,
                        net.minecraft.world.level.block.state.properties.DripstoneThickness.TIP);
        //#endif
    }

    @Override
    public void key(Cast c, boolean down) {
        if (!down || !c.ready()) {
            return;
        }
        ServerPlayer p = c.player;
        ServerLevel level = c.level();
        Shots.Aim aim = Shots.aim(p, range());
        Vec3 mark = aim.target() != null ? aim.target().position() : aim.end();
        BlockPos ground = EarthBending.topEarth(level, BlockPos.containing(mark).getX(), BlockPos.containing(mark.add(0, -0.2, 0)).getY(),
                BlockPos.containing(mark).getZ());
        if (ground == null) {
            c.refuse("message.enlightened.earth.spike_no_earth");
            return;
        }
        BlockState shaft = shaftOf(level.getBlockState(ground));
        long now = c.now();
        float damage = baseDamage() * c.byF(1.0, 1.4, 1.8);
        int height = c.by(2, 3, 4);
        List<Spike> mine = spikes.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
        long burst = now + RUMBLE;
        if (c.level >= 3) {
            // The trail: small spikes race over the ground from the bender's feet to the mark, a block and a half a tick.
            Vec3 from = p.position();
            Vec3 to = Vec3.atBottomCenterOf(ground.above());
            Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
            double length = flat.length();
            int steps = (int) (length / 1.5);
            for (int i = 1; i < steps; i++) {
                Vec3 at = from.add(flat.scale(i / (double) steps));
                BlockPos g = EarthBending.topEarth(level, BlockPos.containing(at).getX(), BlockPos.containing(at).getY() - 1, BlockPos.containing(at).getZ());
                if (g != null && !g.equals(ground)) {
                    mine.add(new Spike(level, g, shaftOf(level.getBlockState(g)), 1, now + 1 + i, damage * 0.5F, false, SMALL_STAND / 2));
                }
            }
            burst = Math.max(burst, now + 1 + steps);
        }
        mine.add(new Spike(level, ground, shaft, height, burst, damage, true, standTicks()));
        if (c.level >= 2) {
            // The ring: small spikes burst round the great one (four for an adept, eight for a master).
            int[][] ring = c.level >= 3
                    ? new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}}
                    : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
            for (int[] o : ring) {
                BlockPos g = EarthBending.topEarth(level, ground.getX() + o[0], ground.getY(), ground.getZ() + o[1]);
                if (g != null) {
                    mine.add(new Spike(level, g, shaftOf(level.getBlockState(g)), c.level >= 3 ? 2 : 1, burst + 2, damage * 0.5F, false, SMALL_STAND));
                }
            }
        }
        // The rumble: the ground under the mark cracks and shudders first.
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(ground));
        level.sendParticles(dust, ground.getX() + 0.5, ground.getY() + 1.05, ground.getZ() + 0.5, 24, 0.6, 0.05, 0.6, 0.05);
        Shots.ring(level, Vec3.atBottomCenterOf(ground.above()), 1.2, dust);
        c.soundAt(Vec3.atCenterOf(ground), SoundEvents.GRAVEL_BREAK, 1.0F, 0.6F);
        c.sound(ModSounds.EARTH_RISE.get(), 0.8F, 1.3F);
        c.exhaust(0.25F);
        int cooldown = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.EARTH_SPIKE_COOLDOWN.get() : 120;
        c.cooldown(c.level >= 3 ? cooldown * 2 / 3 : cooldown);
    }

    @Override
    public void tick(Cast c) {
        List<Spike> mine = spikes.get(c.player.getUUID());
        if (mine == null || mine.isEmpty()) {
            return;
        }
        Iterator<Spike> it = mine.iterator();
        while (it.hasNext()) {
            Spike s = it.next();
            long age = s.level.getGameTime() - s.burst;
            if (age < 0) {
                if (s.main && s.level.getGameTime() % 2 == 0) {
                    // Still rumbling.
                    s.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s.level.getBlockState(s.ground)),
                            s.ground.getX() + 0.5, s.ground.getY() + 1.05, s.ground.getZ() + 0.5, 6, 0.4, 0.02, 0.4, 0.02);
                }
                continue;
            }
            if (age == 0) {
                burst(c.player, s);
            }
            if (s.grown < s.height) {
                grow(c.player, s);
            } else {
                it.remove();
            }
        }
    }

    /** The spike bursts out: whoever stands over it is struck and thrown up. */
    private static void burst(ServerPlayer p, Spike s) {
        ServerLevel level = s.level;
        double half = s.main ? 0.8 : 0.6;
        AABB over = new AABB(s.ground.getX() + 0.5 - half, s.ground.getY() + 0.5, s.ground.getZ() + 0.5 - half,
                s.ground.getX() + 0.5 + half, s.ground.getY() + 1.5 + s.height, s.ground.getZ() + 0.5 + half);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, over, e -> e.isAlive() && e != p)) {
            if (!AreaTargets.mayHurt(p, e) || !s.struck.add(e.getId())) {
                continue;
            }
            Shots.hurt(p, e, s.damage, ModDamageTypes.EARTH);
            double up = s.main ? 0.75 + s.height * 0.12 : 0.55;
            e.setDeltaMovement(e.getDeltaMovement().x * 0.3, up, e.getDeltaMovement().z * 0.3);
            e.hurtMarked = true;
            if (s.main) {
                e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 20, 0, false, true, true));
            }
        }
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, s.shaft);
        level.sendParticles(dust, s.ground.getX() + 0.5, s.ground.getY() + 1.2, s.ground.getZ() + 0.5, s.main ? 40 : 12, 0.4, 0.4, 0.4, 0.15);
        if (s.main) {
            Fx.at(level, Fx.EARTH_RISE, Vec3.atBottomCenterOf(s.ground.above()), com.kolodkingal.enlightened.bending.Element.EARTH.glow, 1.6F);
            level.playSound(null, s.ground, ModSounds.ROCK_HIT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.7F);
            level.playSound(null, s.ground, ModSounds.EARTH_RISE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.4F);
        } else if (level.random.nextInt(3) == 0) {
            level.playSound(null, s.ground, SoundEvents.GRAVEL_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.2F);
        }
    }

    /** The spike grows a block: shaft, the point on top. What stands in the way rides up on it. */
    private static void grow(ServerPlayer p, Spike s) {
        ServerLevel level = s.level;
        int i = s.grown++;
        BlockPos pos = s.ground.above(1 + i);
        boolean top = i == s.height - 1;
        BlockState state = top ? tip() : s.shaft;
        AABB cell = new AABB(pos);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, cell, LivingEntity::isAlive)) {
            // Lifted onto the new block rather than shut in it.
            double y = pos.getY() + 1.01;
            if (e instanceof Player player) {
                player.teleportTo(e.getX(), y, e.getZ());
            } else {
                e.setPos(e.getX(), y, e.getZ());
            }
            e.fallDistance = 0;
        }
        // The point crumbles first, then the shaft, top down: each block a little sooner than the one under it.
        int ticks = s.stand + (s.height - i) * 3 - i;
        if (!TempBlocks.place(p, level, pos, state, ticks)) {
            s.grown = s.height; // something solid: the spike stops here
            return;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s.shaft), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.35, 0.2, 0.35, 0.08);
        if (top && s.main) {
            level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    @Override
    public void end(Cast c) {
        spikes.remove(c.player.getUUID());
    }

    @Override
    public void forget(ServerPlayer player) {
        spikes.remove(player.getUUID());
    }

    @Override
    public void clear() {
        spikes.clear();
    }
}
