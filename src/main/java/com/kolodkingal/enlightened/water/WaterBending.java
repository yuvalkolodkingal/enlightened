package com.kolodkingal.enlightened.water;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.Raycasts;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Waterbending: the whip, healing, the octopus form, the spout, the water arms and the water of plants; the ice arts
 * (knives, freezing, snow, a wall of ice); and bloodbending, with its limits (a full moon below mastery, a stronger
 * waterbender is never held, and the grip is taken up close), and the master's taking of another bender's bending.
 */
public final class WaterBending {
    private static final ResourceLocation ARMS_REACH_ID = Enlightened.id("water_arms_reach");
    private static final ResourceLocation ARMS_DAMAGE_ID = Enlightened.id("water_arms_damage");

    /** Whom each bloodbender holds, and until when. */
    private static final Map<UUID, Grip> GRIPS = new HashMap<>();

    private record Grip(int target, long until) {
    }

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.WATER_WHIP, new Whip());
        logic.put(Ability.WATER_HEALING, new Healing());
        logic.put(Ability.OCTOPUS_FORM, new Octopus());
        logic.put(Ability.WATER_SPOUT, new Spout());
        logic.put(Ability.WATER_ARMS, new Arms());
        logic.put(Ability.PLANT_WATER, new AbilityLogic() {
            @Override
            public void key(Cast c, boolean down) {
            }
        });
        logic.put(Ability.ICE_KNIVES, new IceKnives());
        logic.put(Ability.FREEZE, new Freeze());
        logic.put(Ability.SNOW_THROW, new Snow());
        logic.put(Ability.ICE_WALL, new IceWall());
        logic.put(Ability.BLOODBENDING, new Blood());
        logic.put(Ability.BLOOD_SUPPRESS, new Suppress());
    }

    public static void applyBody(ServerPlayer player, BendingData data) {
        boolean arms = data.has(Ability.WATER_ARMS) && data.on(Ability.WATER_ARMS);
        int level = data.level(Ability.WATER_ARMS);
        BendingTicker.setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, ARMS_REACH_ID, 1.0 + level, arms);
        BendingTicker.setModifier(player, Attributes.ATTACK_DAMAGE, ARMS_DAMAGE_ID, level, arms);
    }

    /** How much of a blow the octopus form's arms take (0 to 0.4). */
    public static float octopusGuard(BendingData data) {
        if (!data.has(Ability.OCTOPUS_FORM) || !data.on(Ability.OCTOPUS_FORM)) {
            return 0F;
        }
        return 0.1F + 0.1F * data.level(Ability.OCTOPUS_FORM);
    }

    /** The octopus form strikes back at whoever the bender hits (and whoever is next to them). */
    public static void octopusStrike(ServerPlayer p, LivingEntity hit) {
        BendingData data = Benders.get(p);
        if (!data.has(Ability.OCTOPUS_FORM) || !data.on(Ability.OCTOPUS_FORM)) {
            return;
        }
        float damage = 1.0F + data.level(Ability.OCTOPUS_FORM);
        for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), 3.5)) {
            if (e != hit) {
                Shots.hurt(p, e, damage, ModDamageTypes.WATER);
                Shots.push(e, p.position(), 0.4, 0.2);
            }
        }
        tentacles(p.serverLevel(), p, 2);
    }

    static void tentacles(ServerLevel level, ServerPlayer p, int perArm) {
        int color = Benders.color(p);
        for (int arm = 0; arm < 8; arm++) {
            double a = arm * Math.PI / 4 + p.tickCount * 0.05;
            for (int i = 1; i <= perArm; i++) {
                double r = 0.8 + i * 0.9;
                double y = 0.6 + Math.sin(p.tickCount * 0.2 + arm) * 0.4 * i;
                level.sendParticles(new com.kolodkingal.enlightened.particle.EnergyParticleOptions(color, 1.2F),
                        p.getX() + Math.cos(a) * r, p.getY() + y, p.getZ() + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    static double whipRange(Cast c) {
        return c.by(8, 10, 12);
    }

    /** A creature the bender may heal: himself, or the player, pet or villager he looks at. */
    @Nullable
    static LivingEntity healTarget(ServerPlayer p) {
        Shots.Aim aim = Shots.aim(p, 6);
        LivingEntity t = aim.target();
        if (t instanceof Player || t instanceof Villager || t instanceof TamableAnimal pet && pet.isTame()
                || t != null && t.isAlliedTo(p)) {
            return t;
        }
        return p;
    }

    /** The creature a healer heals: the ally he looks at, else himself (never null). */
    public static LivingEntity healTargetFor(ServerPlayer p) {
        LivingEntity t = healTarget(p);
        return t == null ? p : t;
    }

    static void heal(Cast c, LivingEntity target, float amount) {
        target.heal(amount);
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(MobEffects.WITHER);
        target.clearFire();
    }

    // ---- Water Whip ----

    static final class Whip implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!WaterSources.take(c)) {
                return;
            }
            Shots.Aim aim = Shots.aim(p, whipRange(c));
            Vec3 from = Shots.hand(p);
            Shots.line(c.level(), from, aim.end(), ParticleTypes.SPLASH, 2.0);
            Shots.line(c.level(), from, aim.end(), c.color(), c.glow());
            float damage = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.WATER_WHIP_DAMAGE.get() : 3.0) + c.level;
            if (aim.target() != null) {
                Shots.hurt(p, aim.target(), damage, ModDamageTypes.WATER);
                Shots.push(aim.target(), p.position(), 0.5, 0.2);
                aim.target().clearFire();
            }
            c.sound(ModSounds.WATER_WHIP.get(), 1.0F, 1.0F);
            c.exhaust(0.1F);
            c.cooldownSeconds(0.8);
        }

        /** The whip is the waterbender's first art: its tick keeps his carried water filled near water. */
        @Override
        public void tick(Cast c) {
            WaterSources.refill(c);
        }
    }

    // ---- Healing ----

    static final class Healing implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            LivingEntity target = healTarget(p);
            if (target == null) {
                return;
            }
            if (!WaterSources.take(c)) {
                return;
            }
            float base = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.WATER_HEAL.get() : 4.0);
            heal(c, target, base * c.byF(1.0, 1.5, 2.0));
            c.level().sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 5, 0.4, 0.4, 0.4, 0.0);
            c.level().sendParticles(new com.kolodkingal.enlightened.particle.EnergyParticleOptions(c.glow(), 1.3F), target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            c.sound(ModSounds.HEAL.get(), 1.0F, 1.0F);
            c.exhaust(0.2F);
            c.cooldownSeconds(8);
        }
    }

    // ---- Octopus Form ----

    static final class Octopus implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            if (c.on()) {
                c.setOn(false);
                c.sound(ModSounds.OCTOPUS.get(), 0.8F, 0.7F);
                return;
            }
            if (!WaterSources.take(c)) {
                return;
            }
            c.setOn(true);
            c.sound(ModSounds.OCTOPUS.get(), 1.0F, 1.0F);
            c.say("message.enlightened.water.octopus_on");
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            if (p.tickCount % 2 == 0) {
                tentacles(c.level(), p, 2);
            }
            if (p.tickCount % 30 == 0) {
                float damage = 1.0F + c.level;
                for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), 3.0)) {
                    if (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == p) {
                        Shots.hurt(p, e, damage, ModDamageTypes.WATER);
                        Shots.push(e, p.position(), 0.3, 0.1);
                    }
                }
            }
            if (p.tickCount % 200 == 0 && !WaterSources.take(c)) {
                c.setOn(false);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    // ---- Water Spout ----

    @Nullable
    static BlockPos waterBelow(ServerLevel level, ServerPlayer p, int depth) {
        BlockPos.MutableBlockPos pos = p.blockPosition().mutable();
        for (int i = 0; i <= depth; i++) {
            if (level.getFluidState(pos).is(Fluids.WATER)) {
                return pos.immutable();
            }
            if (level.getBlockState(pos).isSolid()) {
                return null;
            }
            pos.move(0, -1, 0);
        }
        return null;
    }

    static final class Spout implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            if (c.on()) {
                c.setOn(false);
                c.sound(ModSounds.SPOUT.get(), 0.8F, 0.7F);
                return;
            }
            if (waterBelow(c.level(), c.player, 3) == null) {
                c.refuse("message.enlightened.water.no_water_below");
                return;
            }
            c.setOn(true);
            c.sound(ModSounds.SPOUT.get(), 1.0F, 1.0F);
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            int height = c.by(6, 12, 20);
            BlockPos water = waterBelow(c.level(), p, height + 2);
            if (water == null || !c.fed()) {
                c.setOn(false);
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false));
                return;
            }
            double top = water.getY() + 1 + height;
            double y = p.getY();
            Vec3 v = p.getDeltaMovement();
            double wanted = p.isShiftKeyDown() ? Math.max(-0.3, (water.getY() + 1 - y) * 0.1) : (top - y) * 0.12;
            wanted = Math.max(-0.5, Math.min(0.6, wanted));
            p.setDeltaMovement(v.x, wanted, v.z);
            p.hurtMarked = true;
            p.fallDistance = 0;
            // The column of water: a spiral of splashes from the surface to his feet, and whoever stands in it is thrown.
            double column = y - water.getY();
            for (int i = 0; i < 4; i++) {
                double h = c.level().random.nextDouble() * column;
                double a = h * 1.5 + p.tickCount * 0.3;
                c.level().sendParticles(ParticleTypes.SPLASH, p.getX() + Math.cos(a) * 0.8, water.getY() + 1 + h, p.getZ() + Math.sin(a) * 0.8, 2, 0.1, 0.1, 0.1, 0.0);
            }
            if (p.tickCount % 20 == 0) {
                AABB box = new AABB(p.getX() - 1.5, water.getY(), p.getZ() - 1.5, p.getX() + 1.5, y, p.getZ() + 1.5);
                for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && AreaTargets.mayHurt(p, e))) {
                    Shots.hurt(p, e, c.byF(3, 5, 7), ModDamageTypes.WATER);
                    Shots.push(e, p.position(), 1.2, 0.6);
                }
                c.exhaust(0.1F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    // ---- Water Arms ----

    static final class Arms implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            if (c.on()) {
                c.setOn(false);
            } else {
                if (!WaterSources.take(c)) {
                    return;
                }
                c.setOn(true);
            }
            BendingTicker.applyPassives(c.player, c.data);
            c.sound(ModSounds.WATER_WHIP.get(), 0.8F, c.on() ? 1.2F : 0.7F);
            c.say(c.on() ? "message.enlightened.water.arms_on" : "message.enlightened.water.arms_off");
        }

        @Override
        public void tick(Cast c) {
            if (!c.on() || c.player.tickCount % 4 != 0) {
                return;
            }
            ServerPlayer p = c.player;
            Vec3 look = p.getLookAngle();
            for (int side = -1; side <= 1; side += 2) {
                Vec3 hand = Shots.hand(p, side < 0);
                Vec3 tip = hand.add(look.scale(1.0 + c.level * 0.7));
                c.level().sendParticles(new com.kolodkingal.enlightened.particle.EnergyParticleOptions(c.color(), 1.0F), tip.x, tip.y, tip.z, 1, 0.1, 0.1, 0.1, 0.0);
                c.level().sendParticles(ParticleTypes.SPLASH, hand.lerp(tip, 0.5).x, hand.lerp(tip, 0.5).y, hand.lerp(tip, 0.5).z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    // ---- Ice Knives ----

    static final class IceKnives implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready() || !WaterSources.take(c)) {
                return;
            }
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, 24);
            float damage = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.ICE_KNIFE_DAMAGE.get() : 4.0);
            for (int i = 0; i < c.level; i++) {
                Vec3 from = Shots.hand(p, i % 2 == 1).add(0, (i / 2) * 0.3, 0);
                BendingShotEntity shot = new BendingShotEntity(c.level(), BendingShotEntity.Kind.ICE_KNIFE, from, p, c.color());
                shot.setDamage(damage);
                shot.setTouch(c.by(2, 3, 4));
                shot.launch(aim.end().subtract(from), 1.6);
                c.level().addFreshEntity(shot);
            }
            c.sound(ModSounds.ICE_KNIFE.get(), 1.0F, 1.0F);
            c.exhaust(0.1F);
            c.cooldownSeconds(1.2);
        }
    }

    // ---- Freeze: a creature, or the water in front of him ----

    static final class Freeze implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, 12);
            if (aim.target() != null) {
                if (!WaterSources.take(c)) {
                    return;
                }
                aim.target().addEffect(new MobEffectInstance(ModEffects.FROZEN, c.by(3, 5, 7) * 20, 0));
                Shots.hurt(p, aim.target(), 2.0F, ModDamageTypes.ICE);
                Shots.line(c.level(), Shots.hand(p), aim.end(), ParticleTypes.SNOWFLAKE, 2.0);
                c.sound(ModSounds.ICE_CRACK.get(), 1.0F, 1.0F);
                c.exhaust(0.15F);
                c.cooldownSeconds(6);
                return;
            }
            BlockHitResult hit = Raycasts.blockOrWater(p, 12, 1.0F);
            if (hit.getType() != HitResult.Type.BLOCK) {
                c.refuse("message.enlightened.no_target");
                return;
            }
            int radius = c.by(2, 3, 4);
            int frozen = 0;
            BlockPos center = hit.getBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
                if (!pos.closerThan(center, radius + 0.5) || !c.level().getFluidState(pos).is(Fluids.WATER) || !c.level().getFluidState(pos).isSource()) {
                    continue;
                }
                if (TempBlocks.place(p, c.level(), pos.immutable(), Blocks.ICE.defaultBlockState(), 20 * 30)) {
                    frozen++;
                }
            }
            if (frozen == 0) {
                c.refuse("message.enlightened.water.nothing_to_freeze");
                return;
            }
            c.level().sendParticles(ParticleTypes.SNOWFLAKE, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 30, radius, 0.3, radius, 0.02);
            c.sound(ModSounds.ICE_CRACK.get(), 1.0F, 0.8F);
            c.exhaust(0.15F);
            c.cooldownSeconds(6);
        }
    }

    // ---- Snow Throw ----

    static final class Snow implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready() || !WaterSources.take(c)) {
                return;
            }
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, 20);
            for (int i = 0; i < 3; i++) {
                Vec3 from = Shots.hand(p, i == 1).add(0, i * 0.2, 0);
                BendingShotEntity shot = new BendingShotEntity(c.level(), BendingShotEntity.Kind.SNOWBALL, from, p, c.color());
                shot.setDamage(1.0F + c.level);
                shot.setTouch(c.by(2, 3, 4));
                shot.launch(aim.end().subtract(from).add((i - 1) * 0.3, 0, 0), 1.4);
                c.level().addFreshEntity(shot);
            }
            c.sound(SoundEvents.SNOW_BREAK, 1.0F, 0.8F);
            c.exhaust(0.1F);
            c.cooldownSeconds(1.0);
        }
    }

    // ---- Ice Wall ----

    static final class IceWall implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready() || !WaterSources.take(c)) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            Vec3 facing = Shots.facing(p);
            Vec3 side = new Vec3(-facing.z, 0, facing.x);
            int width = c.by(3, 5, 7);
            int seconds = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.WALL_SECONDS.get() : 10;
            Vec3 center = p.position().add(facing.scale(2.5));
            int placed = 0;
            for (int w = 0; w < width; w++) {
                Vec3 column = center.add(side.scale(w - (width - 1) / 2.0));
                BlockPos foot = BlockPos.containing(column);
                for (int h = 0; h < 3; h++) {
                    BlockPos pos = foot.above(h);
                    if (p.getBoundingBox().intersects(new AABB(pos))) {
                        continue;
                    }
                    if (TempBlocks.place(p, level, pos, Blocks.ICE.defaultBlockState(), seconds * 20 + w * 2)) {
                        placed++;
                    }
                }
            }
            if (placed == 0) {
                c.refuse("message.enlightened.protected");
                return;
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1, center.z, 40, width * 0.4, 1.0, width * 0.4, 0.05);
            c.sound(ModSounds.ICE_CRACK.get(), 1.2F, 0.9F);
            c.exhaust(0.3F);
            c.cooldown(EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.WALL_COOLDOWN.get() : 200);
        }
    }

    // ---- Bloodbending ----

    static boolean fullMoon(ServerLevel level) {
        return level.getMoonPhase() == 0 && level.isNight();
    }

    @Nullable
    public static LivingEntity gripped(ServerPlayer p) {
        Grip grip = GRIPS.get(p.getUUID());
        if (grip == null) {
            return null;
        }
        Entity e = p.serverLevel().getEntity(grip.target);
        return e instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    static void letGo(Cast c, boolean cooldown) {
        GRIPS.remove(c.player.getUUID());
        c.data.setHeldEntityId(-1);
        c.setActive(false);
        if (cooldown) {
            c.cooldownSeconds(20);
        }
    }

    static final class Blood implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            LivingEntity held = gripped(p);
            if (held != null) {
                // Thrown like a doll.
                held.removeEffect(ModEffects.BLOODBENT);
                Vec3 look = p.getLookAngle();
                held.setDeltaMovement(look.scale(1.6).add(0, 0.4, 0));
                held.hurtMarked = true;
                Shots.hurt(p, held, 2.0F + c.level, ModDamageTypes.BLOOD);
                c.sound(ModSounds.BLOOD_GRIP.get(), 1.0F, 1.4F);
                letGo(c, true);
                return;
            }
            if (!c.ready()) {
                return;
            }
            boolean needsMoon = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.BLOOD_NEEDS_FULL_MOON.get();
            if (c.level < 3 && needsMoon && !fullMoon(c.level())) {
                c.refuse("message.enlightened.water.blood_moon");
                return;
            }
            Shots.Aim aim = Shots.aim(p, 4);
            LivingEntity target = aim.target();
            if (target == null) {
                c.refuse("message.enlightened.water.blood_close");
                return;
            }
            if (!AreaTargets.mayHurt(p, target)) {
                c.refuse("message.enlightened.not_allowed");
                return;
            }
            if (target instanceof ServerPlayer other && Benders.is(other, Element.WATER) && Mastery.strength(other) >= Mastery.strength(p)) {
                c.refuse("message.enlightened.water.blood_stronger", other.getDisplayName());
                c.cooldownSeconds(5);
                return;
            }
            int seconds = (int) ((EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.BLOOD_SECONDS.get() : 6) * c.by(1.0, 1.5, 2.0));
            target.addEffect(new MobEffectInstance(ModEffects.BLOODBENT, seconds * 20 + 10, 0, false, true, true));
            GRIPS.put(p.getUUID(), new Grip(target.getId(), c.now() + seconds * 20L));
            c.data.setHeldEntityId(target.getId());
            c.setActive(true);
            c.sound(ModSounds.BLOOD_GRIP.get(), 1.0F, 0.8F);
            c.exhaust(0.4F);
        }

        @Override
        public void tick(Cast c) {
            if (!c.active()) {
                return;
            }
            ServerPlayer p = c.player;
            Grip grip = GRIPS.get(p.getUUID());
            LivingEntity held = gripped(p);
            if (grip == null || held == null || c.now() > grip.until || held.distanceTo(p) > 12 || !held.hasEffect(ModEffects.BLOODBENT)) {
                if (held != null) {
                    held.removeEffect(ModEffects.BLOODBENT);
                }
                letGo(c, true);
                return;
            }
            // The puppet hangs where he looks, three blocks out.
            Vec3 target = p.getEyePosition().add(p.getLookAngle().scale(3.0)).add(0, -held.getBbHeight() * 0.5, 0);
            Vec3 to = target.subtract(held.position());
            held.setDeltaMovement(to.scale(0.4));
            held.hurtMarked = true;
            held.fallDistance = 0;
            if (p.tickCount % 2 == 0) {
                c.level().sendParticles(new com.kolodkingal.enlightened.particle.EnergyParticleOptions(0x8B0000, 1.0F),
                        held.getX(), held.getY() + held.getBbHeight() * 0.5, held.getZ(), 2, 0.3, 0.4, 0.3, 0.0);
            }
        }

        @Override
        public void end(Cast c) {
            LivingEntity held = gripped(c.player);
            if (held != null) {
                held.removeEffect(ModEffects.BLOODBENT);
            }
            letGo(c, false);
        }

        @Override
        public void forget(ServerPlayer player) {
            GRIPS.remove(player.getUUID());
        }

        @Override
        public void clear() {
            GRIPS.clear();
        }
    }

    // ---- Taking another bender's bending (a master's bloodbending, on whom he holds) ----

    static final class Suppress implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.data.level(Ability.BLOODBENDING) < 3) {
                c.refuse("message.enlightened.water.suppress_needs_mastery");
                return;
            }
            LivingEntity held = gripped(p);
            if (!(held instanceof ServerPlayer victim) || !Benders.element(victim).bends()) {
                c.refuse("message.enlightened.water.suppress_no_bender");
                return;
            }
            int strength = Mastery.strength(victim);
            long ticks = strength < 4
                    ? (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.SUPPRESS_NOVICE_MINUTES.get() : 10) * 1200L
                    : (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.SUPPRESS_SKILLED_SECONDS.get() : 90) * 20L;
            BendingData other = Benders.get(victim);
            BendingTicker.endAll(victim, other);
            other.setSuppressedUntil(c.now() + ticks);
            Benders.syncIfDirty(victim);
            victim.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.enlightened.water.suppressed_by", p.getDisplayName()), false);
            c.say("message.enlightened.water.suppressed", victim.getDisplayName(), ticks / 20);
            c.sound(ModSounds.BLOOD_GRIP.get(), 1.2F, 0.5F);
            held.removeEffect(ModEffects.BLOODBENT);
            GRIPS.remove(p.getUUID());
            c.data.setHeldEntityId(-1);
            c.data.setActive(Ability.BLOODBENDING, false);
            c.cooldownSeconds(60);
        }
    }

    private WaterBending() {
    }
}
