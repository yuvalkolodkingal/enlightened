package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.particle.EnergyParticleOptions;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What every ability shares to aim, hit and show it: the first creature along the bender's view, the creatures of an
 * area or a cone that his area attacks may hurt ({@link AreaTargets#mayHurt}: never himself, his pets, players with
 * PvP off), and lines, rings and bursts of particles in the element's colours.
 */
public final class Shots {
    /**
     * Where a shot along the bender's view ends: on the first creature (then {@link Aim#target} is it), else on the
     * first block, else at full range.
     */
    public record Aim(@Nullable LivingEntity target, Vec3 end) {
    }

    public static Aim aim(ServerPlayer player, double range) {
        HitResult hit = Raycasts.any(player, range, e -> e != player && e instanceof LivingEntity && e.isAlive()
                && !e.isSpectator() && !player.isPassengerOfSameVehicle(e), 1.0F);
        Vec3 end = hit.getLocation();
        LivingEntity target = null;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            target = living;
        }
        return new Aim(target, end);
    }

    /** The creatures within {@code radius} of {@code center} that the bender's area powers may hurt. */
    public static List<LivingEntity> area(ServerPlayer player, Vec3 center, double radius) {
        List<LivingEntity> list = new ArrayList<>();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(center) <= radius + e.getBbWidth() * 0.5
                    && AreaTargets.mayHurt(player, e)) {
                list.add(e);
            }
        }
        return list;
    }

    /** The creatures in front of the bender, within {@code range} and {@code degrees} either side of where he looks. */
    public static List<LivingEntity> cone(ServerPlayer player, double range, double degrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(degrees));
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : area(player, eye, range)) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double d = to.length();
            if (d < 0.8 || to.scale(1.0 / d).dot(look) >= cos) {
                list.add(e);
            }
        }
        return list;
    }

    /** Hurts with the bender's element: damage type {@code bending}, blamed on him. */
    public static boolean hurt(ServerPlayer player, Entity target, float amount) {
        return hurt(player, target, amount, ModDamageTypes.BENDING);
    }

    public static boolean hurt(ServerPlayer player, Entity target, float amount, ResourceKey<DamageType> type) {
        BendingData data = Benders.get(player);
        amount *= Chakras.damageFactor(data, player.level().getGameTime());
        // The Avatar strikes harder with the element he was born to.
        if (data.element() == Element.AVATAR && data.avatarBase() != Element.NONE && Avatar.elementOf(type) == data.avatarBase()) {
            amount *= Avatar.BORN_DAMAGE;
        }
        if (amount <= 0) {
            return false;
        }
        return target.hurt(ModDamageTypes.source(player.level(), type, player, player), amount);
    }

    /** Pushes away from {@code from} (horizontally, and a little up). */
    public static void push(Entity target, Vec3 from, double strength, double up) {
        Vec3 away = target.position().subtract(from);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(0, 0, 1);
        }
        away = away.normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().add(away.x, up, away.z));
        target.hurtMarked = true;
    }

    /** Pushes along a direction. */
    public static void shove(Entity target, Vec3 direction, double strength, double up) {
        Vec3 d = direction.normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().add(d.x, up, d.z));
        target.hurtMarked = true;
    }

    /** A line of particles of the element's colour (and its glow at its core), from {@code from} to {@code to}. */
    public static void line(ServerLevel level, Vec3 from, Vec3 to, int color, int glow) {
        double length = from.distanceTo(to);
        int count = Math.min(96, Math.max(2, (int) (length * 2.5)));
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.lerp(to, i / (double) count);
            level.sendParticles(new EnergyParticleOptions(color, 1.1F), at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
            if (i % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(glow, 0.6F), at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** A line of any particle. */
    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double perBlock) {
        double length = from.distanceTo(to);
        int count = Math.min(120, Math.max(2, (int) (length * perBlock)));
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.lerp(to, i / (double) count);
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** A ring of particles of the element's colour on the ground round {@code center}. */
    public static void ring(ServerLevel level, Vec3 center, double radius, int color) {
        ring(level, center, radius, new EnergyParticleOptions(color, 1.3F));
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        int count = Math.min(90, Math.max(12, (int) (radius * 9)));
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.15,
                    center.z + Math.sin(a) * radius, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Where the bender's hand is, for a shot that leaves it (a little in front of him, at chest height, to the right). */
    public static Vec3 hand(ServerPlayer player) {
        return hand(player, false);
    }

    public static Vec3 hand(ServerPlayer player, boolean left) {
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        return player.getEyePosition().add(look.scale(0.6)).add(right.scale(left ? -0.35 : 0.35)).add(0, -0.35, 0);
    }

    /** The horizontal direction the player faces (unit length, never zero). */
    public static Vec3 facing(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private Shots() {
    }
}
