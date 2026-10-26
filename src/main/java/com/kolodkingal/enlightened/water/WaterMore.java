package com.kolodkingal.enlightened.water;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Raycasts;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * The waterbending arts of 1.2: the water shield (a dome held up against blows), the water bubble (air under
 * water), phase change (water to ice and ice to water), frost breath, the plant tether, and the passives: the fast
 * swim and the water step.
 */
public final class WaterMore {

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.WATER_SHIELD, new Shield());
        logic.put(Ability.WATER_BUBBLE, new Bubble());
        logic.put(Ability.PHASE_CHANGE, new PhaseChange());
        logic.put(Ability.FROST_BREATH, new FrostBreath());
        logic.put(Ability.PLANT_TETHER, new Tether());
        logic.put(Ability.FAST_SWIM, new FastSwim());
        logic.put(Ability.WATER_STEP, new AbilityLogic() {
            @Override
            public void key(Cast c, boolean down) {
            }
        });
    }

    /** The water shield takes a blow: returns the damage that gets through. */
    public static float shieldAbsorb(ServerPlayer player, BendingData data, float amount) {
        if (!data.active(Ability.WATER_SHIELD)) {
            return amount;
        }
        int level = data.level(Ability.WATER_SHIELD);
        float guard = level >= 3 ? 0.9F : level == 2 ? 0.75F : 0.6F;
        player.serverLevel().sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1, player.getZ(), 10, 0.6, 0.6, 0.6, 0.0);
        return amount * (1.0F - guard);
    }

    // ---- Water Shield: a dome of water held up while the key is down ----

    static final class Shield implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (down) {
                if (!c.ready() || !WaterSources.take(c)) {
                    return;
                }
                c.setActive(true);
                c.sound(ModSounds.WATER_GATHER.get(), 1.0F, 0.9F);
            } else if (c.active()) {
                c.setActive(false);
                c.cooldownSeconds(3);
            }
        }

        @Override
        public void tick(Cast c) {
            if (!c.active()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            double radius = c.by(2.0, 2.4, 2.8);
            // Arrows and bending shots that reach the water lose their way.
            AABB box = p.getBoundingBox().inflate(radius);
            for (Entity e : level.getEntities(p, box, e -> e instanceof Projectile && e.isAlive())) {
                if (e.position().distanceTo(p.position().add(0, 1, 0)) <= radius + 0.5) {
                    Vec3 away = e.position().subtract(p.position().add(0, 1, 0)).normalize();
                    e.setDeltaMovement(away.scale(0.3).add(0, 0.1, 0));
                    e.hurtMarked = true;
                    level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY(), e.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
                }
            }
            if (p.tickCount % 60 == 0 && !WaterSources.take(c)) {
                c.setActive(false);
                c.cooldownSeconds(3);
            }
            if (p.tickCount % 20 == 0) {
                c.exhaust(0.1F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
        }
    }

    // ---- Water Bubble: a pocket of air under the water ----

    static final class Bubble implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            c.sound(ModSounds.WATER_GATHER.get(), 0.9F, c.on() ? 1.3F : 0.8F);
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            double radius = c.by(1.5, 2.5, 3.5);
            if (!p.isInWater() && !p.isUnderWater()) {
                return;
            }
            p.setAirSupply(p.getMaxAirSupply());
            for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
                    e -> e != p && (e instanceof Player || e.isAlliedTo(p)) && e.isUnderWater())) {
                e.setAirSupply(e.getMaxAirSupply());
            }
            if (p.tickCount % 4 == 0) {
                c.level().sendParticles(ParticleTypes.BUBBLE, p.getX(), p.getEyeY(), p.getZ(), 3, radius * 0.6, 0.4, radius * 0.6, 0.0);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    // ---- Phase Change: water freezes, ice melts ----

    static final class PhaseChange implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            BlockHitResult hit = Raycasts.blockOrWater(p, 14, 1.0F);
            if (hit.getType() != HitResult.Type.BLOCK) {
                c.refuse("message.enlightened.no_target");
                return;
            }
            BlockPos center = hit.getBlockPos();
            BlockState at = level.getBlockState(center);
            int radius = c.by(2, 3, 5);
            int changed = 0;
            boolean melting = at.is(BlockTags.ICE) || at.is(Blocks.SNOW_BLOCK) || at.is(Blocks.SNOW);
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
                if (!pos.closerThan(center, radius + 0.5)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (melting) {
                    if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                        if (TempBlocks.isRaised(level, pos)) {
                            continue;
                        }
                        level.setBlock(pos.immutable(), Blocks.WATER.defaultBlockState(), 3);
                        changed++;
                    } else if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.PACKED_ICE) && c.level >= 3) {
                        level.setBlock(pos.immutable(), state.is(Blocks.SNOW) ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), 3);
                        changed++;
                    }
                } else if (level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos).isSource()) {
                    if (TempBlocks.place(p, level, pos.immutable(), Blocks.ICE.defaultBlockState(), 20 * c.by(30, 60, 120))) {
                        changed++;
                    }
                }
            }
            if (changed == 0) {
                c.refuse("message.enlightened.water.nothing_to_freeze");
                return;
            }
            Vec3 where = Vec3.atBottomCenterOf(center).add(0, 1.0, 0);
            if (melting) {
                level.sendParticles(ParticleTypes.SPLASH, where.x, where.y, where.z, 30, radius, 0.3, radius, 0.0);
                Fx.at(level, Fx.WATER_SPLASH, where, c.color(), (float) radius);
                c.sound(ModSounds.WATER_GATHER.get(), 1.0F, 0.7F);
            } else {
                Fx.at(level, Fx.FREEZE, where, c.color(), (float) radius);
                c.sound(ModSounds.ICE_CRACK.get(), 1.0F, 0.9F);
            }
            c.exhaust(0.2F);
            c.cooldownSeconds(c.by(6, 5, 4));
        }
    }

    // ---- Frost Breath: a cone of frost from the mouth while the key is down ----

    static final class FrostBreath implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (down && (!c.fed() || !WaterSources.take(c))) {
                return;
            }
            c.setActive(down && !c.player.isSpectator());
            if (down) {
                c.sound(ModSounds.ICE_CRACK.get(), 0.8F, 1.3F);
            }
        }

        @Override
        public void tick(Cast c) {
            if (!c.active()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!c.fed() || p.isSpectator()) {
                c.setActive(false);
                return;
            }
            double range = c.by(5, 7, 9);
            if (p.tickCount % 4 == 0) {
                for (LivingEntity e : Shots.cone(p, range, 25)) {
                    Shots.hurt(p, e, 0.6F + c.level * 0.2F, ModDamageTypes.ICE);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, true, true));
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 40, e.getTicksFrozen() + 12));
                    e.clearFire();
                    if (e.getTicksFrozen() >= e.getTicksRequiredToFreeze() && !e.hasEffect(ModEffects.FROZEN)) {
                        e.addEffect(new MobEffectInstance(ModEffects.FROZEN, c.by(2, 3, 4) * 20, 0));
                    }
                }
                c.exhaust(0.05F);
            }
            Vec3 look = p.getLookAngle();
            Vec3 from = p.getEyePosition().add(look.scale(0.4)).add(0, -0.1, 0);
            for (int i = 0; i < 2; i++) {
                Vec3 v = look.scale(0.5 + p.getRandom().nextDouble() * 0.3).add(p.getRandom().nextGaussian() * 0.06, p.getRandom().nextGaussian() * 0.06, p.getRandom().nextGaussian() * 0.06);
                c.level().sendParticles(ParticleTypes.SNOWFLAKE, from.x, from.y, from.z, 0, v.x, v.y, v.z, 0.6);
            }
            if (p.tickCount % 60 == 0 && !WaterSources.take(c)) {
                c.setActive(false);
            }
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
        }
    }

    // ---- Plant Tether: a vine from the plants lashes a creature and drags it, or the bender, to them ----

    static final class Tether implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            BlockPos plant = plantNear(level, p.blockPosition(), c.by(5, 7, 9));
            if (plant == null) {
                c.refuse("message.enlightened.water.no_plants");
                return;
            }
            Vec3 root = Vec3.atCenterOf(plant);
            Shots.Aim aim = Shots.aim(p, 16);
            if (aim.target() != null) {
                LivingEntity target = aim.target();
                Vec3 to = root.subtract(target.position());
                target.setDeltaMovement(to.normalize().scale(Math.min(1.4, to.length() * 0.3)).add(0, 0.3, 0));
                target.hurtMarked = true;
                Shots.hurt(p, target, c.byF(2, 3, 4), ModDamageTypes.WATER);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, c.by(40, 60, 80), 1, false, true, true));
                Fx.send(level, Fx.VINE, root, target.position().add(0, target.getBbHeight() * 0.5, 0), 0x3F8F3A, 1.0F, target.getId());
            } else {
                Vec3 to = root.subtract(p.position());
                p.setDeltaMovement(to.normalize().scale(Math.min(1.2, to.length() * 0.25)).add(0, 0.35, 0));
                p.hurtMarked = true;
                p.fallDistance = 0;
                com.kolodkingal.enlightened.bending.SoftLanding.grant(p, 60);
                Fx.send(level, Fx.VINE, root, p.position().add(0, 1.0, 0), 0x3F8F3A, 1.0F, p.getId());
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, root.x, root.y, root.z, 10, 0.4, 0.4, 0.4, 0.0);
            c.sound(net.minecraft.sounds.SoundEvents.GRASS_BREAK, 1.0F, 0.8F);
            c.exhaust(0.2F);
            c.cooldownSeconds(c.by(8, 6, 5));
        }

        private static BlockPos plantNear(ServerLevel level, BlockPos center, int radius) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LEAVES) || state.is(ModTags.WATER_PLANTS) || state.is(Blocks.VINE) || state.is(BlockTags.LOGS)
                        || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO)) {
                    double d = pos.distSqr(center);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos.immutable();
                    }
                }
            }
            return best;
        }
    }

    // ---- Fast Swim: the water carries its bender ----

    static final class FastSwim implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
        }

        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            if (!p.isInWater()) {
                return;
            }
            if (p.tickCount % 20 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 50, c.level >= 2 ? 1 : 0, false, false, false));
                if (c.level >= 3) {
                    p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 50, 0, false, false, false));
                } else if (c.level == 2 && p.getAirSupply() < p.getMaxAirSupply()) {
                    p.setAirSupply(Math.min(p.getMaxAirSupply(), p.getAirSupply() + 10));
                }
            }
        }
    }

    private WaterMore() {
    }
}
