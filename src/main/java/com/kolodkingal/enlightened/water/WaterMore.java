package com.kolodkingal.enlightened.water;

import net.minecraft.util.Mth;
import java.util.UUID;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
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
        logic.put(Ability.WATER_VORTEX, new Vortex());
        logic.put(Ability.FROST_WALK, new FrostWalk());
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

    /**
     * The water bubble of the show: a real pocket of air round the bender under the water, 4 / 6 / 8 blocks across,
     * that follows him as he walks the sea floor, for him and whoever is beside him; the water closes behind. Toggle.
     */
    static final class Bubble implements AbilityLogic {
        private static final Set<UUID> IN_BUBBLE = new HashSet<>();

        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            if (!c.on()) {
                IN_BUBBLE.remove(c.player.getUUID());
            }
            c.sound(ModSounds.WATER_GATHER.get(), 0.9F, c.on() ? 1.3F : 0.8F);
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            int radius = c.by(2, 3, 4);
            BlockPos center = BlockPos.containing(p.getX(), p.getY() + 0.9, p.getZ());
            boolean inWater = p.isInWater() || p.isUnderWater();
            // The pocket opens when he goes under, and lasts while water is still round him; it closes when he walks out.
            if (inWater) {
                IN_BUBBLE.add(p.getUUID());
            } else if (IN_BUBBLE.contains(p.getUUID()) && !waterWithin(level, center, radius + 1)) {
                IN_BUBBLE.remove(p.getUUID());
            }
            if (!IN_BUBBLE.contains(p.getUUID())) {
                return;
            }
            // Every tick: the pocket is air inside, and its outer shell an invisible wall water cannot flow into (a
            // structure void), so the sea round it never pours back in; a cell the water did reach is cleared again.
            int r2 = radius * radius;
            int inner = (radius - 1) * (radius - 1);
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
                double d2 = pos.distSqr(center);
                if (d2 > r2) {
                    continue;
                }
                BlockState want = d2 > inner ? Blocks.STRUCTURE_VOID.defaultBlockState() : Blocks.AIR.defaultBlockState();
                BlockPos at = pos.immutable();
                BlockState state = level.getBlockState(at);
                boolean pocket = state.isAir() || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.WATER);
                if (pocket && TempBlocks.hold(level, at, want, 10)) {
                    continue;
                }
                if (state.is(Blocks.WATER)) {
                    TempBlocks.place(p, level, at, want, 10);
                    continue;
                }
                // Water held in a block: a waterlogged block is drained (it stays, dry), a water plant (seagrass, kelp, a
                // bubble column) vanishes; both come back once the bubble has passed.
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                    if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                        BlockState dry = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false);
                        if (!TempBlocks.hold(level, at, dry, 10)) {
                            TempBlocks.replace(p, level, at, dry, 10);
                        }
                    } else {
                        TempBlocks.extend(level, at, 10);
                    }
                } else if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && !state.hasBlockEntity()) {
                    if (!TempBlocks.hold(level, at, want, 10)) {
                        TempBlocks.replace(p, level, at, want, 10);
                    }
                }
            }
            p.setAirSupply(p.getMaxAirSupply());
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e -> e != p && e.isUnderWater())) {
                e.setAirSupply(e.getMaxAirSupply());
            }
            if (p.tickCount % 4 == 0) {
                double a = p.tickCount * 0.3;
                level.sendParticles(ParticleTypes.BUBBLE, p.getX() + Math.cos(a) * radius, p.getY() + 1 + Math.sin(a * 0.7) * radius * 0.6, p.getZ() + Math.sin(a) * radius, 2, 0.1, 0.1, 0.1, 0.0);
            }
        }

        private static boolean waterWithin(ServerLevel level, BlockPos center, int radius) {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
                if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
            IN_BUBBLE.remove(c.player.getUUID());
        }

        @Override
        public void forget(ServerPlayer player) {
            IN_BUBBLE.remove(player.getUUID());
        }

        @Override
        public void clear() {
            IN_BUBBLE.clear();
        }
    }

    /**
     * The water vortex: a whirlpool twisting up from the point the bender aims at (as the column Aang rode up a ship's
     * hull), 2.5 / 3.5 / 4.5 blocks wide, for five seconds: everyone in it is pulled in, lifted, spun and hurt, and
     * flung out at the end. Needs water: the skin, or water near.
     */
    static final class Vortex implements AbilityLogic {
        private record Spin(ServerLevel level, UUID owner, Vec3 center, double radius, float damage, long born, long until, int color) {
        }

        private static final List<Spin> SPINS = new ArrayList<>();
        private static final int LIFE = 100;
        /** Who rides a vortex of his own over the water, as Aang rode one up a hull: carried fast where he looks. */
        private static final Set<UUID> RIDERS = new HashSet<>();

        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            if (RIDERS.remove(p.getUUID())) {
                c.setActive(false);
                c.sound(ModSounds.WATER_GATHER.get(), 1.0F, 0.7F);
                return;
            }
            Shots.Aim aim = Shots.aim(p, 14);
            Vec3 at = aim.end();
            // Cast at his own feet over water, the vortex is his to ride: it lifts him and races him over the sea.
            // The ride ignores the whirlpool's cooldown: he may step onto his vortex right after throwing one.
            boolean atFeet = aim.target() == null && at.subtract(p.position()).horizontalDistanceSqr() < 6.0 && at.y < p.getY() + 1.0;
            if (atFeet && (p.isInWater() || WaterBending.waterBelow(c.level(), p, 6) != null)) {
                if (!WaterSources.take(c)) {
                    return;
                }
                RIDERS.add(p.getUUID());
                c.setActive(true);
                c.sound(ModSounds.WATER_GATHER.get(), 1.4F, 0.8F);
                c.say("message.enlightened.water.vortex_ride");
                c.exhaust(0.3F);
                return;
            }
            if (!c.ready() || !WaterSources.take(c)) {
                return;
            }
            ServerLevel level = c.level();
            // The vortex stands on the ground (or the water) under the point aimed at.
            BlockPos.MutableBlockPos pos = BlockPos.containing(at).mutable();
            for (int i = 0; i < 12 && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && !level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER); i++) {
                pos.move(0, -1, 0);
            }
            Vec3 center = new Vec3(at.x, pos.getY() + 1.0, at.z);
            double radius = c.by(2.5, 3.5, 4.5);
            long now = level.getGameTime();
            SPINS.add(new Spin(level, p.getUUID(), center, radius, c.byF(2.0, 3.0, 4.0), now, now + LIFE, c.color()));
            Fx.send(level, Fx.WATER_VORTEX, center, center.add(0, 3.5 + c.level * 0.5, 0), c.color(), (float) radius);
            c.sound(ModSounds.WATER_GATHER.get(), 1.4F, 0.6F);
            c.exhaust(0.5F);
            c.cooldownSeconds(c.by(12, 10, 8));
        }

        /** The ride: a column of water under him, carrying him 3 blocks over the sea at a sprint where he looks. */
        private static void ride(Cast c) {
            ServerPlayer p = c.player;
            BlockPos water = p.isInWater() ? p.blockPosition() : WaterBending.waterBelow(c.level(), p, 7);
            if (water == null || p.isShiftKeyDown() || p.getAbilities().flying || p.isPassenger()) {
                RIDERS.remove(p.getUUID());
                c.setActive(false);
                c.say("message.enlightened.water.vortex_ride_off");
                return;
            }
            double surface = water.getY() + 1.0;
            double want = surface + 3.0 + c.level * 0.5;
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
            double speed = c.by(0.85, 1.05, 1.25);
            // Only going somewhere when he looks somewhere; a straight-down look holds him in place.
            double go = Mth.clamp(1.0 - Math.max(0.0, -look.y - 0.6) * 2.5, 0.0, 1.0);
            double vy = Mth.clamp((want - p.getY()) * 0.3, -0.5, 0.5);
            p.setDeltaMovement(flat.x * speed * go, vy, flat.z * speed * go);
            p.hurtMarked = true;
            p.fallDistance = 0;
            p.setAirSupply(p.getMaxAirSupply());
            if (p.tickCount % 2 == 0) {
                double a = p.tickCount * 0.5;
                c.level().sendParticles(ParticleTypes.SPLASH, p.getX() + Math.cos(a) * 1.1, surface + (p.tickCount % 30) * 0.1, p.getZ() + Math.sin(a) * 1.1, 2, 0.2, 0.2, 0.2, 0.0);
            }
            if (p.tickCount % 40 == 0) {
                c.exhaust(0.15F);
            }
        }

        @Override
        public void tick(Cast c) {
            if (RIDERS.contains(c.player.getUUID())) {
                ride(c);
            }
            if (SPINS.isEmpty()) {
                return;
            }
            ServerPlayer p = c.player;
            long now = c.level().getGameTime();
            Iterator<Spin> it = SPINS.iterator();
            while (it.hasNext()) {
                Spin s = it.next();
                if (!s.owner.equals(p.getUUID()) || s.level != c.level()) {
                    continue;
                }
                boolean last = now >= s.until;
                double top = s.center.y + 3.0 + c.level * 0.5;
                for (LivingEntity e : Shots.area(p, s.center.add(0, 1.5, 0), s.radius + 1.0)) {
                    Vec3 rel = e.position().subtract(s.center);
                    Vec3 flat = new Vec3(rel.x, 0, rel.z);
                    double d = flat.length();
                    Vec3 in = d < 0.05 ? Vec3.ZERO : flat.scale(-1.0 / d);
                    Vec3 around = new Vec3(-in.z, 0, in.x);
                    Vec3 v = e.getDeltaMovement();
                    if (last) {
                        Shots.push(e, s.center, 1.6, 0.9);
                        continue;
                    }
                    double lift = e.getY() < top ? 0.22 : -0.02;
                    e.setDeltaMovement(v.x * 0.6 + in.x * 0.18 + around.x * 0.28, lift, v.z * 0.6 + in.z * 0.18 + around.z * 0.28);
                    e.hurtMarked = true;
                    e.fallDistance = 0;
                    if ((now - s.born) % 20 == 10) {
                        Shots.hurt(p, e, s.damage, ModDamageTypes.WATER);
                    }
                }
                if (now % 2 == 0) {
                    double a = now * 0.5;
                    double r = s.radius * 0.8;
                    s.level.sendParticles(ParticleTypes.SPLASH, s.center.x + Math.cos(a) * r, s.center.y + (now % 40) * 0.08, s.center.z + Math.sin(a) * r, 2, 0.2, 0.2, 0.2, 0.0);
                }
                if (last) {
                    s.level.sendParticles(ParticleTypes.SPLASH, s.center.x, s.center.y + 1.0, s.center.z, 60, s.radius * 0.6, 1.5, s.radius * 0.6, 0.2);
                    it.remove();
                }
            }
        }

        @Override
        public void end(Cast c) {
            SPINS.removeIf(s -> s.owner.equals(c.player.getUUID()));
            RIDERS.remove(c.player.getUUID());
            c.setActive(false);
        }

        @Override
        public void forget(ServerPlayer player) {
            SPINS.removeIf(s -> s.owner.equals(player.getUUID()));
            RIDERS.remove(player.getUUID());
        }

        @Override
        public void clear() {
            SPINS.clear();
            RIDERS.clear();
        }
    }

    /**
     * Frost Walk, a toggle: the water freezes under the waterbender's feet as he walks, a disc of ice 2 / 3 / 4 blocks
     * round him (radius by level), so he walks over lakes and the sea; the ice melts back a few seconds after he passed.
     */
    static final class FrostWalk implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            c.sound(net.minecraft.sounds.SoundEvents.GLASS_PLACE, 0.8F, c.on() ? 1.3F : 0.8F);
            c.say(c.on() ? "message.enlightened.water.frost_walk_on" : "message.enlightened.water.frost_walk_off");
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            if (p.isSpectator() || p.isInWater() && p.isUnderWater() || p.getAbilities().flying) {
                return;
            }
            ServerLevel level = c.level();
            int radius = 1 + c.level;
            BlockPos center = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ()).below(p.isInWater() ? 0 : 1);
            // Standing in water up to the ankles (he just stepped in): freeze the surface he is in, and lift him onto it.
            boolean wading = p.isInWater();
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            int lasting = 120 + c.level * 40;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius + radius) {
                        continue;
                    }
                    at.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(at);
                    if (TempBlocks.isRaised(level, at) && state.is(Blocks.ICE)) {
                        TempBlocks.extend(level, at, lasting);
                        continue;
                    }
                    if (state.is(Blocks.WATER) && level.getFluidState(at).isSource() && level.getBlockState(at.above()).isAir()) {
                        TempBlocks.place(p, level, at.immutable(), Blocks.ICE.defaultBlockState(), lasting);
                    }
                }
            }
            if (wading && level.getBlockState(center).is(Blocks.ICE)) {
                p.teleportTo(p.getX(), center.getY() + 1.0, p.getZ());
            }
            if (p.tickCount % 6 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 0.1, p.getZ(), 3, 0.4, 0.05, 0.4, 0.01);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

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
