package com.kolodkingal.enlightened.fire;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import com.kolodkingal.enlightened.EnlightenedConfig;
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
import com.kolodkingal.enlightened.bending.Variants;
import com.kolodkingal.enlightened.entity.FireWallEntity;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The firebending arts of 1.2: the fire jet, the fire shield, the wall of fire, blaze, heat control, illumination,
 * combustion, and the fire step.
 */
public final class FireMore {
    /** How many ticks of jet each firebender has left. */
    private static final Map<UUID, Integer> JETS = new HashMap<>();
    private static final Map<UUID, Long> COMBUSTING = new HashMap<>();

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.FIRE_JET, new Jet());
        logic.put(Ability.FIRE_SHIELD, new Shield());
        logic.put(Ability.WALL_OF_FIRE, new Wall());
        logic.put(Ability.BLAZE, new Blaze());
        logic.put(Ability.HEAT_CONTROL, new Heat());
        logic.put(Ability.ILLUMINATION, new Illumination());
        logic.put(Ability.PALM_FORGE, new PalmForge());
        logic.put(Ability.COMBUSTION, new Combustion());
        logic.put(Ability.FIRE_STEP, new AbilityLogic() {
            @Override
            public void key(Cast c, boolean down) {
            }
        });
    }

    /** The fire shield takes a blow: returns what gets through (nothing of arrows and bending, a little of the rest). */
    public static float shieldAbsorb(ServerPlayer player, BendingData data, float amount, boolean projectileOrBending) {
        if (!data.active(Ability.FIRE_SHIELD)) {
            return amount;
        }
        int level = data.level(Ability.FIRE_SHIELD);
        if (projectileOrBending) {
            return 0.0F;
        }
        return amount * (level >= 3 ? 0.5F : level == 2 ? 0.65F : 0.8F);
    }

    public static boolean jetting(BendingData data) {
        return data.active(Ability.FIRE_JET);
    }

    // ---- Fire Jet: flames from the feet throw him along his look ----

    static final class Jet implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            if (!c.fed()) {
                c.refuse("message.enlightened.hungry");
                return;
            }
            JETS.put(c.player.getUUID(), c.by(10, 14, 18));
            c.setActive(true);
            c.sound(ModSounds.FIRE_STREAM.get(), 1.2F, 1.1F);
            c.exhaust(0.3F);
            c.cooldownSeconds(c.by(5, 4, 3));
        }

        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            Integer left = JETS.get(p.getUUID());
            if (left == null) {
                if (c.active()) {
                    c.setActive(false);
                }
                return;
            }
            if (left <= 0 || p.isInWater()) {
                JETS.remove(p.getUUID());
                c.setActive(false);
                SoftLanding.grant(p, 100);
                return;
            }
            JETS.put(p.getUUID(), left - 1);
            Vec3 look = p.getLookAngle();
            double speed = c.by(0.9, 1.1, 1.3);
            p.setDeltaMovement(look.scale(speed).add(0, 0.08, 0));
            p.hurtMarked = true;
            p.fallDistance = 0;
            Vec3 feet = p.position().add(0, 0.2, 0);
            for (int i = 0; i < 3; i++) {
                Vec3 v = look.scale(-0.6).add(p.getRandom().nextGaussian() * 0.1, p.getRandom().nextGaussian() * 0.1, p.getRandom().nextGaussian() * 0.1);
                c.level().sendParticles(FireBending.flame(c.data), feet.x, feet.y, feet.z, 0, v.x, v.y, v.z, 0.5);
            }
            for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), 1.5)) {
                Shots.hurt(p, e, 2.0F, ModDamageTypes.FLAME);
                FireBending.ignite(p, e, 3);
            }
        }

        @Override
        public void end(Cast c) {
            JETS.remove(c.player.getUUID());
            c.setActive(false);
        }

        @Override
        public void forget(ServerPlayer player) {
            JETS.remove(player.getUUID());
        }

        @Override
        public void clear() {
            JETS.clear();
        }
    }

    // ---- Fire Shield: a dome of fire held up while the key is down ----

    static final class Shield implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (down) {
                if (!c.ready() || !c.fed()) {
                    return;
                }
                c.setActive(true);
                c.sound(ModSounds.FIRE_RING.get(), 0.9F, 1.2F);
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
            if (!c.fed() || p.isSpectator()) {
                c.setActive(false);
                return;
            }
            double radius = c.by(2.0, 2.4, 2.8);
            AABB box = p.getBoundingBox().inflate(radius + 0.5);
            Vec3 center = p.position().add(0, 1, 0);
            for (Entity e : level.getEntities(p, box, e -> e.isAlive())) {
                if (e.position().distanceTo(center) > radius + 0.5) {
                    continue;
                }
                if (e instanceof Projectile) {
                    level.sendParticles(ParticleTypes.LAVA, e.getX(), e.getY(), e.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
                    e.discard();
                } else if (e instanceof LivingEntity living && AreaTargets.mayHurt(p, living) && p.tickCount % 10 == 0) {
                    Shots.hurt(p, living, 1.0F + 0.5F * c.level, ModDamageTypes.FLAME);
                    FireBending.ignite(p, living, 3);
                    Shots.push(living, p.position(), 0.5, 0.2);
                }
            }
            if (p.tickCount % 20 == 0) {
                c.exhaust(0.15F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
        }
    }

    // ---- Wall of Fire ----

    static final class Wall implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            HitResult hit = Raycasts.block(p, 10, 1.0F);
            Vec3 at = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : p.getEyePosition().add(p.getLookAngle().scale(6));
            // On the ground under that point.
            BlockPos.MutableBlockPos pos = BlockPos.containing(at).mutable();
            for (int i = 0; i < 6 && !level.getBlockState(pos).isSolid(); i++) {
                pos.move(0, -1, 0);
            }
            if (!level.getBlockState(pos).isSolid()) {
                c.refuse("message.enlightened.no_target");
                return;
            }
            Vec3 foot = new Vec3(at.x, pos.getY() + 1.0, at.z);
            float yaw = p.getYRot();
            int seconds = (int) ((EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_WALL_SECONDS.get() : 6) * c.by(1.0, 1.4, 1.8));
            FireWallEntity wall = new FireWallEntity(level, p, foot, yaw, c.byF(5, 7, 9), seconds * 20, 1.5F + c.level * 0.5F, Variants.blueFire(c.data));
            level.addFreshEntity(wall);
            c.sound(ModSounds.FIRE_RING.get(), 1.2F, 0.9F);
            c.exhaust(0.3F);
            c.cooldownSeconds(c.by(12, 10, 8));
        }
    }

    // ---- Blaze: an arc of fire runs over the ground ahead ----

    static final class Blaze implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            Vec3 facing = Shots.facing(p);
            double range = c.by(6, 8, 10);
            int lit = 0;
            for (double d = 1.5; d <= range; d += 1.0) {
                for (double side = -d * 0.5; side <= d * 0.5; side += 1.0) {
                    Vec3 at = p.position().add(facing.scale(d)).add(-facing.z * side, 0, facing.x * side);
                    BlockPos pos = BlockPos.containing(at);
                    BlockPos ground = pos;
                    for (int i = 0; i < 3 && !level.getBlockState(ground).isSolid(); i++) {
                        ground = ground.below();
                    }
                    BlockPos fire = ground.above();
                    if (level.getBlockState(ground).isSolid() && level.getBlockState(fire).isAir() && BlockRights.mayPlace(p, level, fire)
                            && !level.getBlockState(ground).is(BlockTags.LEAVES)) {
                        if (TempBlocks.place(p, level, fire, FireBending.fireBlock(c.data), c.by(40, 60, 80))) {
                            lit++;
                        }
                    }
                }
            }
            for (LivingEntity e : Shots.cone(p, range, 40)) {
                Shots.hurt(p, e, c.byF(3, 4, 5), ModDamageTypes.FLAME);
                FireBending.ignite(p, e, c.by(3, 4, 5));
            }
            Fx.send(level, Fx.BLAZE_ARC, p.position(), p.position().add(facing), Variants.blueFire(c.data) ? 1 : 0, (float) range);
            c.sound(ModSounds.FIRE_BLAST.get(), 1.1F, 0.8F);
            c.exhaust(0.25F);
            c.cooldownSeconds(c.by(8, 7, 6));
        }
    }

    // ---- Heat Control ----

    static final class Heat implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            c.sound(SoundEvents.FIRE_AMBIENT, 1.0F, c.on() ? 1.2F : 0.7F);
            c.say(c.on() ? "message.enlightened.fire.heat_on" : "message.enlightened.fire.heat_off");
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            p.clearFire();
            p.setTicksFrozen(0);
            if (p.tickCount % 10 != 0) {
                return;
            }
            ServerLevel level = c.level();
            int radius = c.by(2, 3, 4);
            BlockPos center = p.blockPosition();
            int melted = 0;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 2, radius))) {
                BlockState state = level.getBlockState(pos);
                if (TempBlocks.isRaised(level, pos) || !BlockRights.mayBreak(p, level, pos, state)) {
                    continue;
                }
                if (state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW)) {
                    level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
                    melted++;
                } else if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.SNOW_BLOCK)) {
                    level.setBlock(pos.immutable(), Blocks.WATER.defaultBlockState(), 3);
                    melted++;
                } else if (c.level >= 2 && level.getFluidState(pos).isSource() && level.getFluidState(pos).is(net.minecraft.world.level.material.Fluids.WATER)
                        && pos.getY() >= center.getY() - 1 && pos.closerThan(center, 1.6)) {
                    // A master's heat boils the water at his feet away for a moment.
                    TempBlocks.place(p, level, pos.immutable(), Blocks.AIR.defaultBlockState(), 40);
                    level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.02);
                }
                if (melted >= 6) {
                    break;
                }
            }
            if (melted > 0) {
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 6, radius * 0.5, 0.3, radius * 0.5, 0.02);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    // ---- Illumination: a flame in the hand lights the way ----

    /**
     * The palm forge: the fire in a firebender's hand cooks what he holds, as Iroh heated his shackle and Baku his
     * steel: hold the key with food, an ore, raw metal, sand or anything a furnace, a blast furnace, a smoker or a
     * campfire would take in the main hand, and one piece is cooked or smelted every 3 / 2.25 / 1.5 seconds, with the
     * furnace's experience.
     */
    static final class PalmForge implements AbilityLogic {
        private static final Map<UUID, Integer> HEAT = new HashMap<>();

        @Override
        public void key(Cast c, boolean down) {
            ServerPlayer p = c.player;
            if (!down) {
                c.setActive(false);
                HEAT.remove(p.getUUID());
                return;
            }
            if (!c.ready()) {
                return;
            }
            if (recipe(c.level(), p.getMainHandItem()) == null) {
                c.say("message.enlightened.fire.forge_nothing");
                return;
            }
            c.setActive(true);
            HEAT.put(p.getUUID(), 0);
            c.sound(SoundEvents.FIRECHARGE_USE, 0.7F, 1.3F);
        }

        @Nullable
        static net.minecraft.world.item.crafting.AbstractCookingRecipe recipe(ServerLevel level, ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }
            net.minecraft.world.item.crafting.SingleRecipeInput input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
            for (net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> type
                    : java.util.List.of(net.minecraft.world.item.crafting.RecipeType.SMELTING, net.minecraft.world.item.crafting.RecipeType.BLASTING,
                    net.minecraft.world.item.crafting.RecipeType.SMOKING, net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING)) {
                var found = level.getRecipeManager().getRecipeFor(type, input, level);
                if (found.isPresent()) {
                    return found.get().value();
                }
            }
            return null;
        }

        @Override
        public void tick(Cast c) {
            if (!c.active()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            ItemStack stack = p.getMainHandItem();
            net.minecraft.world.item.crafting.AbstractCookingRecipe recipe = recipe(level, stack);
            if (recipe == null) {
                c.setActive(false);
                HEAT.remove(p.getUUID());
                return;
            }
            int heat = HEAT.merge(p.getUUID(), 1, Integer::sum);
            Vec3 hand = Shots.hand(p, false);
            if (p.tickCount % 3 == 0) {
                level.sendParticles(FireBending.flame(c.data), hand.x, hand.y, hand.z, 1, 0.08, 0.05, 0.08, 0.01);
            }
            if (p.tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, hand.x, hand.y + 0.1, hand.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
            int need = c.by(60, 45, 30);
            if (heat < need) {
                return;
            }
            HEAT.put(p.getUUID(), 0);
            ItemStack result = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level.registryAccess()).copy();
            stack.shrink(1);
            if (!p.getInventory().add(result)) {
                p.drop(result, false);
            }
            int xp = Math.round(recipe.getExperience() * 2.0F);
            if (xp > 0) {
                p.giveExperiencePoints(xp);
            }
            level.sendParticles(ParticleTypes.LAVA, hand.x, hand.y, hand.z, 3, 0.1, 0.1, 0.1, 0.0);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);
            c.exhaust(0.15F);
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
            HEAT.remove(c.player.getUUID());
        }

        @Override
        public void forget(ServerPlayer player) {
            HEAT.remove(player.getUUID());
        }

        @Override
        public void clear() {
            HEAT.clear();
        }
    }

    static final class Illumination implements AbilityLogic {
        private static final Map<UUID, BlockPos> LIGHTS = new HashMap<>();

        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            c.sound(SoundEvents.FIRECHARGE_USE, 0.6F, c.on() ? 1.5F : 0.9F);
            if (!c.on()) {
                putOut(c.player);
            }
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            BlockPos at = p.blockPosition().above();
            BlockPos old = LIGHTS.get(p.getUUID());
            if (old != null && old.equals(at)) {
                if (p.tickCount % 5 == 0) {
                    Vec3 hand = Shots.hand(p);
                    level.sendParticles(FireBending.flame(c.data), hand.x, hand.y, hand.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
                return;
            }
            putOut(p);
            if (level.getBlockState(at).isAir()) {
                int light = c.by(10, 13, 15);
                level.setBlock(at, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, light), 3);
                LIGHTS.put(p.getUUID(), at);
            }
        }

        private static void putOut(ServerPlayer p) {
            BlockPos old = LIGHTS.remove(p.getUUID());
            if (old != null && p.serverLevel().getBlockState(old).is(Blocks.LIGHT)) {
                p.serverLevel().setBlock(old, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
            putOut(c.player);
        }

        @Override
        public void forget(ServerPlayer player) {
            putOut(player);
        }

        @Override
        public void clear() {
            LIGHTS.clear();
        }
    }

    // ---- Combustion: a blast from the third eye ----

    static final class Combustion implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready() || COMBUSTING.containsKey(c.player.getUUID())) {
                return;
            }
            if (!c.fed()) {
                c.refuse("message.enlightened.hungry");
                return;
            }
            COMBUSTING.put(c.player.getUUID(), c.now() + c.by(16, 13, 10));
            c.setActive(true);
            c.sound(ModSounds.LIGHTNING_CHARGE.get(), 0.8F, 0.6F);
        }

        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            Long at = COMBUSTING.get(p.getUUID());
            if (at == null) {
                if (c.active()) {
                    c.setActive(false);
                }
                return;
            }
            Vec3 eye = p.getEyePosition().add(p.getLookAngle().scale(0.3)).add(0, 0.15, 0);
            c.level().sendParticles(ParticleTypes.SMALL_FLAME, eye.x, eye.y, eye.z, 2, 0.1, 0.1, 0.1, 0.01);
            if (c.now() < at) {
                return;
            }
            COMBUSTING.remove(p.getUUID());
            c.setActive(false);
            ServerLevel level = c.level();
            double range = c.by(24, 32, 40);
            HitResult hit = Raycasts.any(p, range, e -> e instanceof LivingEntity && e != p, 1.0F);
            Vec3 end = hit.getType() == HitResult.Type.MISS ? p.getEyePosition().add(p.getLookAngle().scale(range)) : hit.getLocation();
            float power = c.byF(2.0, 2.5, 3.0);
            boolean breaks = EnlightenedConfig.SPEC.isLoaded() && EnlightenedConfig.COMBUSTION_BREAKS_BLOCKS.get();
            float damage = (float) ((EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.COMBUSTION_DAMAGE.get() : 10.0) * c.by(1.0, 1.4, 1.8));
            for (LivingEntity e : Shots.area(p, end, power * 1.5)) {
                double d = e.position().distanceTo(end);
                float f = (float) Math.max(0.2, 1.0 - d / (power * 1.5));
                Shots.hurt(p, e, damage * f, ModDamageTypes.FIRE);
                FireBending.ignite(p, e, 4);
                Shots.push(e, end, 1.5 * f, 0.5 * f);
            }
            level.explode(p, ModDamageTypes.source(level, ModDamageTypes.FIRE, p, p), null, end.x, end.y, end.z, power,
                    false, breaks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER,
                    SoundEvents.GENERIC_EXPLODE);
            Fx.send(level, Fx.COMBUSTION, eye, end, c.color(), power);
            // The grey-black mushroom of smoke that hangs over the blast.
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, end.x, end.y + 0.5, end.z, (int) (power * 10), power * 0.5, power * 0.6, power * 0.5, 0.03);
            level.sendParticles(ParticleTypes.LAVA, end.x, end.y + 0.3, end.z, (int) (power * 6), power * 0.3, 0.2, power * 0.3, 0.0);
            c.exhaust(0.8F);
            c.cooldownSeconds(c.by(30, 24, 18));
        }

        @Override
        public void end(Cast c) {
            COMBUSTING.remove(c.player.getUUID());
            c.setActive(false);
        }

        @Override
        public void forget(ServerPlayer player) {
            COMBUSTING.remove(player.getUUID());
        }

        @Override
        public void clear() {
            COMBUSTING.clear();
        }
    }

    private FireMore() {
    }
}
