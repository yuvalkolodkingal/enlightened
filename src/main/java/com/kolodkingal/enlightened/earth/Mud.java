package com.kolodkingal.enlightened.earth;

import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mud, which both earthbenders and waterbenders can bend: mud, clay or soil must be near.
 *
 * <ul>
 * <li><b>Mud Wave</b> (earth): the ground ahead turns to mud and rolls forward in a hard, straight line, knocking up
 * and bogging down everything in its way.</li>
 * <li><b>Mud Throw</b> (water): balls of mud drawn up and hurled at what the bender looks at.</li>
 * </ul>
 * Whoever the mud hits is slowed for {@link #SLOW_SECONDS} seconds.
 */
public final class Mud {
    public static final int SLOW_SECONDS = 7;
    public static final int SOURCE_RADIUS = 6;

    public static AbilityLogic wave() {
        return new Wave();
    }

    public static AbilityLogic throwing() {
        return new Throw();
    }

    /** Whether a block is mud, or what mud is made of (soil, clay). */
    public static boolean muddy(BlockState s) {
        return s.is(Blocks.MUD) || s.is(Blocks.CLAY) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT)
                || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM) || s.is(Blocks.FARMLAND)
                || s.is(Blocks.PACKED_MUD) || s.is(Blocks.MUDDY_MANGROVE_ROOTS) || s.is(Blocks.DIRT_PATH);
    }

    /** Whether there is mud (or soil) to bend within reach. */
    public static boolean near(ServerLevel level, BlockPos center) {
        int r = SOURCE_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -3, -r), center.offset(r, 2, r))) {
            if (muddy(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    /** What mud does to whom it hits: slows him for seven seconds. */
    public static void bog(LivingEntity e) {
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_SECONDS * 20, 1));
    }

    // ---- Mud Wave ----

    private record Rolling(Vec3 at, Vec3 dir, int left, float damage, Set<Integer> struck) {
    }

    static final class Wave implements AbilityLogic {
        private final Map<UUID, Rolling> waves = new HashMap<>();

        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!near(c.level(), p.blockPosition())) {
                c.refuse("message.enlightened.mud.none");
                return;
            }
            Vec3 dir = Shots.facing(p);
            waves.put(p.getUUID(), new Rolling(p.position().add(dir.scale(1.5)), dir, c.by(10, 14, 18), c.byF(5, 6, 7), new HashSet<>()));
            c.sound(SoundEvents.MUD_BREAK, 1.2F, 0.6F);
            c.exhaust(0.3F);
            c.cooldown(c.by(100, 80, 60));
        }

        @Override
        public void tick(Cast c) {
            Rolling w = waves.get(c.player.getUUID());
            if (w == null) {
                return;
            }
            ServerLevel level = c.level();
            ServerPlayer p = c.player;
            Vec3 at = w.at.add(w.dir);
            BlockPos column = BlockPos.containing(at);
            BlockPos ground = EarthBending.topEarth(level, column.getX(), column.getY() - 1, column.getZ());
            if (ground == null || w.left <= 0) {
                waves.remove(p.getUUID());
                return;
            }
            // The ground turns to mud a while, and heaves up.
            TempBlocks.replace(p, level, ground, Blocks.MUD.defaultBlockState(), 100);
            Vec3 top = Vec3.atBottomCenterOf(ground.above());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), top.x, top.y + 0.3, top.z, 18, 0.7, 0.4, 0.7, 0.15);
            if (w.left % 3 == 0) {
                c.soundAt(top, SoundEvents.MUD_STEP, 1.0F, 0.6F);
            }
            for (LivingEntity e : Shots.area(p, top.add(0, 0.8, 0), 1.6)) {
                if (w.struck.add(e.getId())) {
                    Shots.hurt(p, e, w.damage, ModDamageTypes.EARTH);
                    Shots.shove(e, w.dir, 0.6, 0.45);
                    bog(e);
                }
            }
            waves.put(p.getUUID(), new Rolling(new Vec3(at.x, ground.getY() + 1, at.z), w.dir, w.left - 1, w.damage, w.struck));
        }

        @Override
        public void end(Cast c) {
            waves.remove(c.player.getUUID());
        }

        @Override
        public void forget(ServerPlayer player) {
            waves.remove(player.getUUID());
        }

        @Override
        public void clear() {
            waves.clear();
        }
    }

    // ---- Mud Throw ----

    static final class Throw implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!near(c.level(), p.blockPosition())) {
                c.refuse("message.enlightened.mud.none");
                return;
            }
            Shots.Aim aim = Shots.aim(p, 24);
            Vec3 look = p.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
            int count = c.level;
            for (int i = 0; i < count; i++) {
                double side = (i - (count - 1) / 2.0) * 0.7;
                Vec3 from = Shots.hand(p).add(right.scale(side));
                BendingShotEntity shot = EarthBending.shoot(c, BendingShotEntity.Kind.ROCK, from, aim.end().add(right.scale(side * 0.5)), c.byF(3, 4, 5), 0, 1.4);
                shot.setBlockState(Blocks.MUD.defaultBlockState());
                shot.setSize(0.7F);
            }
            c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), p.getX(), p.getY() + 1.0, p.getZ(),
                    16, 0.4, 0.3, 0.4, 0.1);
            c.sound(SoundEvents.MUD_PLACE, 1.0F, 0.8F);
            c.exhaust(0.2F);
            c.cooldown(c.by(40, 32, 24));
        }
    }

    private Mud() {
    }
}
