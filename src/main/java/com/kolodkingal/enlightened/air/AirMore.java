package com.kolodkingal.enlightened.air;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * The airbending arts of 1.2: air suction (a pull), the air burst (everything round him thrown back), the air spout
 * (a column of wind that lifts him), and the passives: air agility (faster and higher) and the air step.
 */
public final class AirMore {
    private static final ResourceLocation AGILITY_SPEED = Enlightened.id("air_agility_speed");
    private static final ResourceLocation AGILITY_JUMP = Enlightened.id("air_agility_jump");

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.AIR_SUCTION, new Suction());
        logic.put(Ability.AIR_BURST, new Burst());
        logic.put(Ability.AIR_SPOUT, new Spout());
        logic.put(Ability.AIR_AGILITY, new Passive());
        logic.put(Ability.AIR_STEP, new Passive());
    }

    static final class Passive implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
        }
    }

    /** Air agility: lighter on his feet, the airbender runs faster and jumps higher. */
    public static void applyBody(ServerPlayer player, BendingData data) {
        boolean on = data.has(Ability.AIR_AGILITY);
        int level = data.level(Ability.AIR_AGILITY);
        double speed = on ? 0.05 + 0.05 * level : 0.0;          // 10/15/20 percent
        double jump = on && level >= 2 ? (level >= 3 ? 0.18 : 0.10) : 0.0;
        BendingTicker.setModifier(player, Attributes.MOVEMENT_SPEED, AGILITY_SPEED, speed, on, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        BendingTicker.setModifier(player, Attributes.JUMP_STRENGTH, AGILITY_JUMP, jump, on && level >= 2);
    }

    // ---- Air Suction: a pull on whatever he looks at ----

    static final class Suction implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            double range = c.by(10, 14, 18);
            double pull = c.by(1.0, 1.4, 1.8);
            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getLookAngle();
            int pulled = 0;
            for (LivingEntity e : Shots.cone(p, range, 25)) {
                Vec3 to = eye.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
                double d = to.length();
                Vec3 v = to.normalize().scale(Math.min(pull, d * 0.25)).add(0, 0.25, 0);
                e.setDeltaMovement(v);
                e.hurtMarked = true;
                e.fallDistance = 0;
                Shots.hurt(p, e, 1.0F + AirBending.bonus(c.data) * 0.5F, ModDamageTypes.AIR);
                pulled++;
            }
            AABB box = p.getBoundingBox().inflate(range);
            for (Entity e : level.getEntities(p, box, e -> (e instanceof ItemEntity || e instanceof Projectile) && e.isAlive())) {
                Vec3 to = eye.subtract(e.position());
                if (to.length() <= range && to.normalize().dot(look.scale(-1.0)) > Math.cos(Math.toRadians(25))) {
                    e.setDeltaMovement(to.normalize().scale(0.6));
                    e.hurtMarked = true;
                    pulled++;
                }
            }
            Vec3 far = eye.add(look.scale(range));
            Fx.send(level, Fx.AIR_BLAST, far, eye.add(look.scale(1.0)), c.color(), (float) range);
            level.sendParticles(ParticleTypes.GUST, eye.x + look.x * 2, eye.y + look.y * 2, eye.z + look.z * 2, 2, 0.4, 0.4, 0.4, 0.0);
            c.sound(ModSounds.AIR_BLAST.get(), 1.0F, 0.7F);
            c.exhaust(0.15F);
            c.cooldownSeconds(pulled > 0 ? 3 : 1.5);
        }
    }

    // ---- Air Burst: a sphere of air bursting out from the body ----

    static final class Burst implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            double radius = c.by(5.0, 7.0, 9.0);
            double push = c.by(1.4, 1.9, 2.4);
            for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                Shots.hurt(p, e, 2.0F + AirBending.bonus(c.data), ModDamageTypes.AIR_BURST);
                Shots.push(e, p.position(), push, 0.6);
                e.fallDistance = 0;
            }
            AABB box = p.getBoundingBox().inflate(radius);
            for (Entity e : level.getEntities(p, box, e -> (e instanceof ItemEntity || e instanceof Projectile) && e.isAlive())) {
                Shots.push(e, p.position(), push * 0.6, 0.3);
            }
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
            Fx.at(level, Fx.AIR_BURST, p.position().add(0, 1.0, 0), c.color(), (float) radius);
            c.sound(ModSounds.AIR_SHIELD_BREAK.get(), 1.0F, 1.3F);
            c.exhaust(0.3F);
            c.cooldownSeconds(c.by(10, 8, 6));
        }
    }

    // ---- Air Spout: a column of wind under the airbender ----

    static final class Spout implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            if (c.on()) {
                c.setOn(false);
                c.sound(ModSounds.AIR_JUMP.get(), 0.8F, 0.7F);
                return;
            }
            if (!c.fed()) {
                c.refuse("message.enlightened.hungry");
                return;
            }
            c.setOn(true);
            c.sound(ModSounds.AIR_JUMP.get(), 1.0F, 1.0F);
        }

        @Override
        public void tick(Cast c) {
            if (!c.on()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!c.fed() || p.getAbilities().flying || p.isPassenger()) {
                c.setOn(false);
                return;
            }
            int height = com.kolodkingal.enlightened.bending.Spouts.height(false, c.level);
            ServerLevel level = c.level();
            // The ground under him, within the spout's reach. The ride itself is driven on his client (Spouts.drive).
            double base = com.kolodkingal.enlightened.bending.Spouts.base(level, p, height + 2, false);
            if (Double.isNaN(base)) {
                c.setOn(false);
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false));
                return;
            }
            int ground = (int) base;
            double y = p.getY();
            p.fallDistance = 0;
            if (p.tickCount % 3 == 0) {
                double h = level.random.nextDouble() * (y - ground);
                double a = h * 1.5 + p.tickCount * 0.3;
                level.sendParticles(ParticleTypes.CLOUD, p.getX() + Math.cos(a) * 0.7, ground + h, p.getZ() + Math.sin(a) * 0.7, 1, 0, 0.05, 0, 0.0);
            }
            if (p.tickCount % 40 == 0) {
                c.exhaust(0.1F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    private AirMore() {
    }
}
