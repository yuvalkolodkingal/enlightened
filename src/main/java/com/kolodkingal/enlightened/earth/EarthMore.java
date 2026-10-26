package com.kolodkingal.enlightened.earth;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BlockRights;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Raycasts;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.SoftLanding;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.Map;

/**
 * The earthbending arts of 1.2, learned from the other Avatar mods: the shockwave, the catapult, the earth grab, the
 * earth tunnel, the metalbender's extraction, and the passives: density shift (a firm landing on earth) and the
 * earth step.
 */
public final class EarthMore {

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.SHOCKWAVE, new Shockwave());
        logic.put(Ability.CATAPULT, new Catapult());
        logic.put(Ability.EARTH_GRAB, new Grab());
        logic.put(Ability.EARTH_TUNNEL, new Tunnel());
        logic.put(Ability.EXTRACTION, new Extraction());
        logic.put(Ability.DENSITY_SHIFT, new Passive());
        logic.put(Ability.EARTH_STEP, new Passive());
    }

    static final class Passive implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
        }
    }

    /** Density shift: an earthbender lands on earth without harm; a master's heavy landing shakes the ground. */
    public static void onFall(ServerPlayer p, BendingData data, LivingFallEvent event) {
        if (!data.has(Ability.DENSITY_SHIFT)) {
            return;
        }
        int level = data.level(Ability.DENSITY_SHIFT);
        ServerLevel world = p.serverLevel();
        BlockPos below = p.blockPosition().below();
        BlockState ground = world.getBlockState(below);
        if (ground.isAir()) {
            ground = world.getBlockState(below.below());
        }
        boolean earth = ground.is(ModTags.EARTH) || ground.is(ModTags.SAND);
        if (!earth && level < 2) {
            return;
        }
        if (event.getDistance() > 3.0F) {
            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), p.getX(), p.getY() + 0.1, p.getZ(), 24, 0.6, 0.1, 0.6, 0.1);
            world.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.EARTH_RISE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.7F);
            if (level >= 3 && event.getDistance() > 6.0F) {
                // A master's landing: the ground ripples and whoever stands near is knocked off his feet.
                for (LivingEntity e : Shots.area(p, p.position(), 3.5)) {
                    Shots.hurt(p, e, 2.0F, ModDamageTypes.EARTH);
                    Shots.push(e, p.position(), 0.7, 0.4);
                }
                Fx.at(world, Fx.SHOCKWAVE, p.position(), data.element().color, 3.5F);
            }
        }
        event.setDamageMultiplier(0.0F);
    }

    // ---- Shockwave: a wave of earth rolling out from the stomp ----

    static final class Shockwave implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            BlockPos ground = EarthBending.groundNear(level, p.position().add(0, -0.1, 0));
            if (ground == null || !p.onGround()) {
                c.refuse("message.enlightened.earth.no_earth");
                return;
            }
            double radius = c.by(4.5, 6.0, 8.0);
            float damage = c.byF(4.0, 6.0, 8.0);
            for (LivingEntity e : Shots.area(p, p.position(), radius)) {
                Shots.hurt(p, e, damage, ModDamageTypes.EARTH);
                Shots.push(e, p.position(), 1.2, 0.7);
                e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 15, 0, false, true, true));
            }
            BlockState state = level.getBlockState(ground);
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                double r = 1.5 + level.random.nextDouble() * (radius - 1.5);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX() + Math.cos(a) * r, p.getY() + 0.3, p.getZ() + Math.sin(a) * r,
                        3, 0.2, 0.3, 0.2, 0.1);
            }
            Fx.at(level, Fx.SHOCKWAVE, p.position(), c.color(), (float) radius);
            c.sound(ModSounds.EARTH_RISE.get(), 1.5F, 0.6F);
            c.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 0.5F);
            c.exhaust(0.4F);
            c.cooldownSeconds(c.by(12, 10, 8));
        }
    }

    // ---- Catapult: the earth flings its bender along his look ----

    static final class Catapult implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            BlockPos ground = EarthBending.groundNear(level, p.position().add(0, -0.1, 0));
            if (ground == null || !p.onGround()) {
                c.refuse("message.enlightened.earth.no_earth");
                return;
            }
            Vec3 look = p.getLookAngle();
            double power = c.by(1.3, 1.7, 2.1);
            Vec3 v = new Vec3(look.x * power, Math.max(0.6, look.y * power + 0.5), look.z * power);
            p.setDeltaMovement(v);
            p.hurtMarked = true;
            p.fallDistance = 0;
            SoftLanding.grant(p, 140);
            BlockState state = level.getBlockState(ground);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX(), p.getY() + 0.2, p.getZ(), 30, 0.5, 0.3, 0.5, 0.15);
            TempBlocks.place(p, level, ground.above(), state, 30);
            Fx.at(level, Fx.EARTH_RISE, p.position(), c.color(), 1.5F);
            c.sound(ModSounds.EARTH_RISE.get(), 1.2F, 1.1F);
            c.exhaust(0.3F);
            c.cooldownSeconds(c.by(6, 5, 4));
        }
    }

    // ---- Earth Grab: the ground closes round a creature's legs ----

    static final class Grab implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            Shots.Aim aim = Shots.aim(p, 16);
            LivingEntity target = aim.target();
            if (target == null) {
                // Nothing to grab: it pulls the loose things on the ground to him instead.
                int pulled = 0;
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8))) {
                    Vec3 to = p.position().add(0, 0.5, 0).subtract(item.position());
                    item.setDeltaMovement(to.normalize().scale(0.6));
                    item.hurtMarked = true;
                    pulled++;
                }
                if (pulled == 0) {
                    c.refuse("message.enlightened.no_target");
                    return;
                }
            } else {
                BlockPos ground = EarthBending.groundNear(level, target.position().add(0, -0.1, 0));
                if (ground == null) {
                    c.refuse("message.enlightened.earth.grab_no_earth");
                    return;
                }
                BlockState state = level.getBlockState(ground);
                int seconds = c.by(3, 5, 7);
                BlockPos feet = target.blockPosition();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos pos = feet.offset(dx, 0, dz);
                        if ((Math.abs(dx) + Math.abs(dz)) == 2 && c.level < 3) {
                            continue;
                        }
                        if (level.getBlockState(pos).isAir()) {
                            TempBlocks.place(p, level, pos, state, seconds * 20);
                        }
                    }
                }
                target.addEffect(new MobEffectInstance(ModEffects.QUICKSAND, seconds * 20, 0, false, true, true));
                Shots.hurt(p, target, 1.0F, ModDamageTypes.EARTH);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), target.getX(), target.getY() + 0.5, target.getZ(), 30, 0.6, 0.4, 0.6, 0.1);
                Fx.around(level, Fx.EARTH_GRAB, target, c.color(), 1.0F);
            }
            c.sound(ModSounds.EARTH_RISE.get(), 1.0F, 0.9F);
            c.exhaust(0.2F);
            c.cooldownSeconds(8);
        }
    }

    // ---- Earth Tunnel: while held, the earth ahead is dug away ----

    static final class Tunnel implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            c.setActive(down && c.fed() && !c.player.isSpectator());
            if (down) {
                c.sound(ModSounds.EARTH_RISE.get(), 0.8F, 1.4F);
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
            if (p.tickCount % 2 != 0) {
                return;
            }
            ServerLevel level = c.level();
            BlockHitResult hit = Raycasts.block(p, 4.0, 1.0F);
            if (hit.getType() != HitResult.Type.BLOCK) {
                return;
            }
            BlockPos center = hit.getBlockPos();
            int size = c.level >= 3 ? 1 : 0;
            int dug = 0;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-size, -size, -size), center.offset(size, size, size))) {
                BlockState state = level.getBlockState(pos);
                if (!state.is(ModTags.EARTH) && !state.is(ModTags.SAND) || !BlockRights.mayBreak(p, level, pos, state) || TempBlocks.isRaised(level, pos)) {
                    continue;
                }
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
                level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
                dug++;
                if (dug >= (c.level >= 2 ? 4 : 2)) {
                    break;
                }
            }
            if (dug > 0 && p.tickCount % 6 == 0) {
                c.sound(SoundEvents.GRAVEL_BREAK, 0.7F, 0.8F);
                c.exhaust(0.05F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
        }
    }

    // ---- Extraction: the metal is torn out of an ore ----

    static final class Extraction implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            BlockHitResult hit = Raycasts.block(p, 6.0, 1.0F);
            if (hit.getType() != HitResult.Type.BLOCK) {
                c.refuse("message.enlightened.no_target");
                return;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            ItemStack raw = rawOf(state);
            if (raw == null) {
                c.refuse("message.enlightened.earth.no_ore");
                return;
            }
            if (!BlockRights.mayBreak(p, level, pos, state)) {
                c.refuse("message.enlightened.protected");
                return;
            }
            int count = 1 + level.random.nextInt(c.level + 1);
            raw.setCount(count);
            BlockState left = state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.getBlock().toString().contains("deepslate") ? Blocks.DEEPSLATE.defaultBlockState()
                    : state.is(Blocks.NETHER_GOLD_ORE) ? Blocks.NETHERRACK.defaultBlockState() : Blocks.STONE.defaultBlockState();
            if (state.getBlock() == Blocks.DEEPSLATE_IRON_ORE || state.getBlock() == Blocks.DEEPSLATE_COPPER_ORE || state.getBlock() == Blocks.DEEPSLATE_GOLD_ORE) {
                left = Blocks.DEEPSLATE.defaultBlockState();
            }
            level.setBlock(pos, left, 3);
            Vec3 at = Vec3.atCenterOf(pos);
            ItemEntity drop = new ItemEntity(level, at.x, at.y, at.z, raw);
            Vec3 to = p.position().add(0, 1, 0).subtract(at).normalize().scale(0.4);
            drop.setDeltaMovement(to);
            level.addFreshEntity(drop);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.1);
            Fx.at(level, Fx.EXTRACT, at, c.color(), 1.0F);
            c.sound(ModSounds.METAL_CLANG.get(), 1.0F, 1.2F);
            c.exhaust(0.2F);
            c.cooldownSeconds(c.by(4, 3, 2));
        }

        private static ItemStack rawOf(BlockState state) {
            if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
                return new ItemStack(Items.RAW_IRON);
            }
            if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
                return new ItemStack(Items.RAW_COPPER);
            }
            if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE)) {
                return new ItemStack(Items.RAW_GOLD);
            }
            return null;
        }
    }

    private EarthMore() {
    }
}
