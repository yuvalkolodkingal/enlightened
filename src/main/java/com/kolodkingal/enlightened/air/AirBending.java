package com.kolodkingal.enlightened.air;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.entity.TornadoEntity;
import com.kolodkingal.enlightened.item.GliderItem;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Airbending: the blast that throws everyone back, the air shield (and its burst when it breaks), the jump off the
 * ground and the glide on the Air Glider, the air blades, the spirit projection, the air bubble under water, the air
 * scooter and the tornado; and, for an airbender who let go of his earthly tethers, flight and the taking of an
 * enemy's breath.
 */
public final class AirBending {
    private static final ResourceLocation SCOOTER_ID = Enlightened.id("air_scooter");
    private static final ResourceLocation SCOOTER_STEP_ID = Enlightened.id("air_scooter_step");

    /** What is left of each airbender's shield. */
    private static final Map<UUID, Float> SHIELD = new HashMap<>();
    /** Whose breath each tetherless airbender is taking. */
    private static final Map<UUID, Integer> SUFFOCATING = new HashMap<>();

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.AIR_BLAST, new Blast());
        logic.put(Ability.AIR_SHIELD, new Shield());
        logic.put(Ability.AIR_JUMP, new Jump());
        logic.put(Ability.AIR_BLADES, new Blades());
        logic.put(Ability.SPIRIT_PROJECTION, new Projection.Logic());
        logic.put(Ability.AIR_BUBBLE, new Bubble());
        logic.put(Ability.AIR_SCOOTER, new Scooter());
        logic.put(Ability.TORNADO, new Tornado());
        logic.put(Ability.FLIGHT, new Flight());
        logic.put(Ability.SUFFOCATE, new Suffocate());
        AirMore.register(logic);
    }

    public static void applyBody(ServerPlayer player, BendingData data) {
        AirMore.applyBody(player, data);
        boolean scooter = data.has(Ability.AIR_SCOOTER) && data.on(Ability.AIR_SCOOTER);
        double boost = scooter ? 0.3 + data.level(Ability.AIR_SCOOTER) * 0.3 : 0; // 0.6 / 0.9 / 1.2
        BendingTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SCOOTER_ID, boost, scooter, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        BendingTicker.setModifier(player, Attributes.STEP_HEIGHT, SCOOTER_STEP_ID, 0.5, scooter);
    }

    /** The extra damage a tetherless airbender's attacks do. */
    static float bonus(BendingData data) {
        if (!data.tetherless()) {
            return 0F;
        }
        return (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.TETHERLESS_DAMAGE_BONUS.get() : 2.0);
    }

    // ---- Air Blast ----

    static final class Blast implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            double range = c.by(6, 9, 12);
            double base = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.AIR_BLAST_PUSH.get() : 1.6;
            double push = base * c.by(1.0, 1.5, 2.2) * (c.data.tetherless() ? 0.7 : 1.0);
            Vec3 look = p.getLookAngle();
            float damage = 1.0F + bonus(c.data);
            for (LivingEntity e : Shots.cone(p, range, 35)) {
                Shots.hurt(p, e, damage, ModDamageTypes.AIR);
                e.setDeltaMovement(e.getDeltaMovement().add(look.x * push, 0.3 + Math.max(0, look.y) * push, look.z * push));
                e.hurtMarked = true;
                e.fallDistance = 0;
            }
            // Loose things and arrows go with it too.
            AABB box = p.getBoundingBox().inflate(range);
            for (Entity e : level.getEntities(p, box, e -> (e instanceof ItemEntity || e instanceof Projectile) && e.isAlive())) {
                Vec3 to = e.position().subtract(p.getEyePosition());
                if (to.length() <= range && to.normalize().dot(look) > Math.cos(Math.toRadians(35))) {
                    e.setDeltaMovement(e.getDeltaMovement().add(look.scale(push * 0.6)));
                    e.hurtMarked = true;
                }
            }
            Fx.send(level, Fx.AIR_BLAST, p.getEyePosition().add(look.scale(0.8)).add(0, -0.3, 0), p.getEyePosition().add(look.scale(range)), c.color(), (float) range);
            Vec3 mid = p.getEyePosition().add(look.scale(range * 0.5));
            level.sendParticles(ParticleTypes.GUST, mid.x, mid.y, mid.z, 2, 0.5, 0.5, 0.5, 0.0);
            c.sound(ModSounds.AIR_BLAST.get(), 1.0F, 1.0F);
            c.exhaust(0.15F);
            c.cooldown(EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.AIR_BLAST_COOLDOWN.get() : 20);
        }
    }

    // ---- Air Shield ----

    public static float shieldStrength(BendingData data) {
        double base = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.AIR_SHIELD_STRENGTH.get() : 10.0;
        int level = data.level(Ability.AIR_SHIELD);
        return (float) (base * (level >= 3 ? 2.0 : level == 2 ? 1.5 : 1.0));
    }

    public static double shieldRadius(BendingData data) {
        int level = data.level(Ability.AIR_SHIELD);
        return level >= 3 ? 3.5 : level == 2 ? 3.0 : 2.5;
    }

    public static boolean shieldUp(Player player, BendingData data) {
        return data.has(Ability.AIR_SHIELD) && data.on(Ability.AIR_SHIELD);
    }

    /** What is left of the shield (0 when it is down). */
    public static float shieldLeft(Player player) {
        return SHIELD.getOrDefault(player.getUUID(), 0F);
    }

    /**
     * The shield takes a blow for its airbender: returns the damage that gets through (0 while the shield holds). When
     * the blow empties it, the shield bursts: everyone round is thrown back and hurt.
     */
    public static float shieldAbsorb(ServerPlayer player, BendingData data, float amount) {
        float left = SHIELD.getOrDefault(player.getUUID(), 0F);
        if (left <= 0) {
            return amount;
        }
        float through = Math.max(0, amount - left);
        left -= amount;
        if (left <= 0) {
            burst(player, data);
            return through;
        }
        SHIELD.put(player.getUUID(), left);
        return 0F;
    }

    static void burst(ServerPlayer p, BendingData data) {
        SHIELD.remove(p.getUUID());
        data.setOn(Ability.AIR_SHIELD, false);
        data.startCooldown(Ability.AIR_SHIELD, p.level().getGameTime(), 300);
        ServerLevel level = p.serverLevel();
        double radius = shieldRadius(data) + 2.0;
        float damage = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.AIR_SHIELD_BURST_DAMAGE.get() : 7.0);
        for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
            Shots.hurt(p, e, damage, ModDamageTypes.AIR_BURST);
            Shots.push(e, p.position(), 1.8, 0.5);
        }
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        Fx.at(level, Fx.AIR_BURST, p.position().add(0, 1.0, 0), 0, (float) shieldRadius(data) + 1.5F);
        Shots.ring(level, p.position(), radius, ParticleTypes.CLOUD);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AIR_SHIELD_BREAK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);
        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.enlightened.air.shield_burst"), true);
    }

    static final class Shield implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.on()) {
                c.setOn(false);
                SHIELD.remove(p.getUUID());
                c.sound(ModSounds.AIR_SHIELD.get(), 0.8F, 0.7F);
                return;
            }
            if (!c.ready()) {
                return;
            }
            if (!c.fed()) {
                c.refuse("message.enlightened.hungry");
                return;
            }
            c.setOn(true);
            SHIELD.put(p.getUUID(), shieldStrength(c.data));
            c.sound(ModSounds.AIR_SHIELD.get(), 1.0F, 1.0F);
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!c.fed() || p.isSpectator()) {
                c.setOn(false);
                SHIELD.remove(p.getUUID());
                return;
            }
            if (!SHIELD.containsKey(p.getUUID())) {
                SHIELD.put(p.getUUID(), shieldStrength(c.data));
            }
            double radius = shieldRadius(c.data);
            Vec3 center = p.position().add(0, 1, 0);
            ServerLevel level = c.level();
            // Creatures are kept out; arrows are turned back.
            for (LivingEntity e : Shots.area(p, center, radius)) {
                Vec3 away = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(center);
                if (away.lengthSqr() < 1.0E-4) {
                    away = new Vec3(0, 0, 1);
                }
                e.setDeltaMovement(e.getDeltaMovement().add(away.normalize().scale(0.25)));
                e.hurtMarked = true;
            }
            for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(radius), s -> s.getOwner() != p)) {
                Vec3 v = shot.getDeltaMovement();
                Vec3 away = shot.position().subtract(center);
                if (away.lengthSqr() > 1.0E-4 && v.dot(away) < 0) {
                    shot.setDeltaMovement(v.scale(-0.8));
                    shot.hurtMarked = true;
                    shot.setOwner(p);
                }
            }
            if (p.tickCount % 3 == 0) {
                double a = p.tickCount * 0.25;
                for (int i = 0; i < 3; i++) {
                    double t = a + i * 2.094;
                    double y = Math.sin(p.tickCount * 0.1 + i) * radius * 0.8;
                    double r = Math.sqrt(Math.max(0, radius * radius - y * y));
                    level.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(t) * r, center.y + y, center.z + Math.sin(t) * r, 1, 0, 0, 0, 0.0);
                }
            }
            if (p.tickCount % 40 == 0) {
                c.exhaust(0.1F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
            SHIELD.remove(c.player.getUUID());
        }

        @Override
        public void forget(ServerPlayer player) {
            SHIELD.remove(player.getUUID());
        }

        @Override
        public void clear() {
            SHIELD.clear();
        }
    }

    // ---- Air Jump, and the glide on the Air Glider ----

    static boolean mayGlide(ServerPlayer p) {
        boolean needs = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.GLIDER_NEEDED.get();
        return !needs || GliderItem.holds(p);
    }

    public static boolean gliding(BendingData data) {
        return data.active(Ability.AIR_JUMP);
    }

    static final class Jump implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            double lift = c.by(0.9, 1.2, 1.5);
            if (p.getAbilities().flying) {
                return;
            }
            p.setDeltaMovement(p.getDeltaMovement().x, lift, p.getDeltaMovement().z);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, c.by(60, 80, 100), 0, false, false, false));
            for (LivingEntity e : Shots.area(p, p.position(), 2.5)) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, lift * 0.6, 0));
                e.hurtMarked = true;
            }
            c.level().sendParticles(ParticleTypes.GUST, p.getX(), p.getY() + 0.2, p.getZ(), 4, 0.5, 0.1, 0.5, 0.0);
            c.sound(ModSounds.AIR_JUMP.get(), 1.0F, 1.0F);
            c.exhaust(0.1F);
            c.cooldownSeconds(1.5);
        }

        /** The glide: while he falls with the glider in hand, the air carries him forward and down slowly. */
        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            Vec3 v = p.getDeltaMovement();
            boolean falling = !p.onGround() && !p.isInWater() && !p.getAbilities().flying && !p.isShiftKeyDown()
                    && v.y < -0.15 && !p.isPassenger() && mayGlide(p);
            if (!falling) {
                c.setActive(false);
                return;
            }
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(v.x, 0, v.z) : flat.normalize();
            double speed = 0.45 + c.level * 0.1;
            double fall = look.y < -0.5 ? -0.35 : -0.08;
            p.setDeltaMovement(flat.x * speed, Math.max(v.y, fall), flat.z * speed);
            p.hurtMarked = true;
            p.fallDistance = 0;
            c.setActive(true);
            if (p.tickCount % 3 == 0) {
                c.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
            }
        }

        @Override
        public void end(Cast c) {
            c.setActive(false);
        }
    }

    // ---- Air Blades ----

    static final class Blades implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, 24);
            float damage = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.AIR_BLADE_DAMAGE.get() : 5.0) + bonus(c.data);
            for (int i = 0; i < c.level; i++) {
                Vec3 from = Shots.hand(p, i % 2 == 1).add(0, (i / 2) * 0.3, 0);
                BendingShotEntity shot = new BendingShotEntity(c.level(), BendingShotEntity.Kind.AIR_BLADE, from, p, c.color());
                shot.setDamage(damage);
                shot.launch(aim.end().subtract(from), 1.7);
                c.level().addFreshEntity(shot);
            }
            c.sound(ModSounds.AIR_BLADE.get(), 1.0F, 1.0F + c.level * 0.1F);
            c.exhaust(0.1F);
            c.cooldownSeconds(1.0);
        }
    }

    // ---- Air Bubble: breath under water ----

    static final class Bubble implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
        }

        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            if (!p.isUnderWater()) {
                return;
            }
            if (c.level >= 2) {
                p.setAirSupply(p.getMaxAirSupply());
            } else {
                p.setAirSupply(Math.max(p.getAirSupply(), 120));
            }
            if (c.level >= 3 && p.tickCount % 40 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false, false));
            }
            if (p.tickCount % 5 == 0) {
                c.level().sendParticles(ParticleTypes.BUBBLE, p.getX(), p.getEyeY(), p.getZ(), 3, 0.4, 0.3, 0.4, 0.0);
            }
        }
    }

    // ---- Air Scooter ----

    static final class Scooter implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            BendingTicker.applyPassives(c.player, c.data);
            c.sound(ModSounds.AIR_SCOOTER.get(), 1.0F, c.on() ? 1.0F : 0.7F);
            c.say(c.on() ? "message.enlightened.air.scooter_on" : "message.enlightened.air.scooter_off");
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            p.fallDistance = 0;
            ride(p);
            if (p.tickCount % 4 == 0) {
                double a = p.tickCount * 0.6;
                c.level().sendParticles(ParticleTypes.CLOUD, p.getX() + Math.cos(a) * 0.7, p.getY() + 0.05, p.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0.0);
            }
            if (p.tickCount % 60 == 0 && (p.getDeltaMovement().horizontalDistanceSqr() > 0.01)) {
                c.exhaust(0.1F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }

        /** The ball of air under his feet carries him over gaps and lets him down softly; he never falls from it. */
        private static void ride(ServerPlayer p) {
            if (p.getAbilities().flying || p.isPassenger() || p.isInWater() || p.onGround()) {
                return;
            }
            Vec3 v = p.getDeltaMovement();
            if (v.y < -0.12) {
                p.setDeltaMovement(v.x, -0.12, v.z);
                p.hurtMarked = true;
            }
        }
    }

    // ---- Tornado ----

    static final class Tornado implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            Vec3 facing = Shots.facing(p);
            Vec3 at = p.position().add(facing.scale(2.0));
            TornadoEntity tornado = new TornadoEntity(c.level(), p, at, facing, c.byF(1.5, 2.5, 3.5), c.by(100, 140, 180),
                    c.byF(1.0, 1.5, 2.0), c.data.tetherless());
            c.level().addFreshEntity(tornado);
            c.sound(ModSounds.TORNADO.get(), 1.2F, 1.0F);
            c.exhaust(0.3F);
            c.cooldownSeconds(12);
        }
    }

    // ---- Flight (tetherless) ----

    static final class Flight implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            if (!c.on()) {
                c.setOn(true);
                BendingTicker.applyPassives(c.player, c.data);
                c.say("message.enlightened.air.flight_on");
            }
            BendingTicker.toggleFlight(c.player);
        }

        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            if (c.on() && p.getAbilities().flying && p.tickCount % 2 == 0) {
                c.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    // ---- Suffocate (tetherless): the breath taken from an enemy ----

    static final class Suffocate implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            ServerPlayer p = c.player;
            if (!down) {
                release(c);
                return;
            }
            if (!c.ready()) {
                return;
            }
            Shots.Aim aim = Shots.aim(p, 8);
            LivingEntity target = aim.target();
            if (target == null || !AreaTargets.mayHurt(p, target)) {
                c.refuse("message.enlightened.no_target");
                return;
            }
            SUFFOCATING.put(p.getUUID(), target.getId());
            c.setActive(true);
            c.sound(ModSounds.SUFFOCATE.get(), 1.0F, 1.0F);
        }

        private void release(Cast c) {
            if (c.active()) {
                c.setActive(false);
                SUFFOCATING.remove(c.player.getUUID());
                c.cooldownSeconds(6);
            }
        }

        @Override
        public void tick(Cast c) {
            if (!c.active()) {
                return;
            }
            ServerPlayer p = c.player;
            Integer id = SUFFOCATING.get(p.getUUID());
            Entity e = id == null ? null : c.level().getEntity(id);
            if (!(e instanceof LivingEntity target) || !target.isAlive() || target.distanceTo(p) > 12 || !c.fed()) {
                release(c);
                return;
            }
            target.setAirSupply(target.getAirSupply() - 12);
            if (target.getAirSupply() <= -20) {
                target.setAirSupply(0);
                Shots.hurt(p, target, 2.0F, ModDamageTypes.SUFFOCATE);
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, false, false));
            if (p.tickCount % 2 == 0) {
                c.level().sendParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getEyeY(), target.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
                Shots.line(c.level(), Shots.hand(p), target.getEyePosition(), ParticleTypes.CLOUD, 0.6);
            }
            if (p.tickCount % 20 == 0) {
                c.exhaust(0.2F);
            }
        }

        @Override
        public void end(Cast c) {
            SUFFOCATING.remove(c.player.getUUID());
            c.setActive(false);
        }

        @Override
        public void forget(ServerPlayer player) {
            SUFFOCATING.remove(player.getUUID());
        }

        @Override
        public void clear() {
            SUFFOCATING.clear();
        }
    }

    private AirBending() {
    }
}
