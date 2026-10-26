package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.fire.FireBending;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The element step: a double tap of a direction dashes a bender who learned his element's step, wrapped in his
 * element (dust, a gust, a sheet of water, a flare of fire), and the landing is soft. The client finds the double tap
 * and sends the direction; the server moves him.
 */
public final class Steps {

    @Nullable
    public static Ability stepOf(Element element) {
        return switch (element) {
            case EARTH -> Ability.EARTH_STEP;
            case AIR -> Ability.AIR_STEP;
            case WATER -> Ability.WATER_STEP;
            case FIRE -> Ability.FIRE_STEP;
            default -> null;
        };
    }

    /** A step along {@code dir} (horizontal, or up for a hop): if he learned it and it is ready. */
    public static void step(ServerPlayer p, Vec3 dir) {
        BendingData data = Benders.get(p);
        Ability step = stepOf(data.element());
        if (step == null || !data.has(step) || !p.isAlive() || !BendingHandler.mayBend(p, data)) {
            return;
        }
        if (EnlightenedConfig.SPEC.isLoaded() && !EnlightenedConfig.STEPS.get()) {
            return;
        }
        Cast c = new Cast(p, data, step);
        if (!c.ready() || !c.fed()) {
            return;
        }
        if (p.isPassenger() || p.getAbilities().flying) {
            return;
        }
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? Shots.facing(p) : dir.normalize();
        double power = c.by(0.9, 1.1, 1.3);
        boolean hop = d.y > 0.5;
        Vec3 v = hop ? new Vec3(0, 0.85 + 0.1 * c.level, 0) : new Vec3(d.x * power, 0.22, d.z * power);
        p.setDeltaMovement(v);
        p.hurtMarked = true;
        p.fallDistance = 0;
        SoftLanding.grant(p, 40);
        ServerLevel level = c.level();
        Vec3 at = p.position().add(0, 0.9, 0);
        Fx.send(level, Fx.STEP, at, at.add(v.scale(2.5)), c.color(), data.element().ordinal());
        switch (data.element()) {
            case EARTH -> {
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(p.blockPosition().below())),
                        p.getX(), p.getY() + 0.1, p.getZ(), 20, 0.4, 0.2, 0.4, 0.1);
                c.sound(ModSounds.EARTH_RISE.get(), 0.8F, 1.3F);
            }
            case AIR -> {
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                c.sound(ModSounds.AIR_JUMP.get(), 0.9F, 1.3F);
            }
            case WATER -> {
                level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.6, p.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                c.sound(ModSounds.WATER_GATHER.get(), 0.9F, 1.4F);
            }
            case FIRE -> {
                level.sendParticles(FireBending.flame(data), p.getX(), p.getY() + 0.4, p.getZ(), 16, 0.3, 0.3, 0.3, 0.05);
                c.sound(ModSounds.FIRE_BLAST.get(), 0.8F, 1.5F);
            }
            default -> {
            }
        }
        c.exhaust(0.15F);
        c.cooldownSeconds(c.by(2.0, 1.5, 1.0));
        Benders.syncIfDirty(p);
    }

    private Steps() {
    }
}
