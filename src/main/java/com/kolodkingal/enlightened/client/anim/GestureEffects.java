package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.client.EnlightenedClientConfig;
import com.kolodkingal.enlightened.fire.FireBending;
import com.kolodkingal.enlightened.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The small flourishes that go with the acting: flames licking the fists of a fire fist, sparks crawling between the
 * hands of a lightning charge, dust kicked up by an earthbender's stomp, mist off a waterbender's hands, wisps round
 * an airbender's palms. Each comes from the places the model really has hands and feet ({@link HandTracker}), is
 * thinned by distance, and is off with the {@code effects} switch of the client config.
 */
public final class GestureEffects {
    private enum Kind {
        /** Dust from the block under the planted foot. */
        STOMP_DUST,
        /** Motes of the element gathering into both hands. */
        GATHER_HANDS,
        /** A burst of the element from the main hand. */
        BURST_HAND,
        /** A burst from both hands. */
        BURST_BOTH,
        /** A ring of the element on the ground round the feet. */
        GROUND_RING
    }

    private static final class Timed {
        final int player;
        final long from;
        final long to;
        final Kind kind;

        Timed(int player, long from, long to, Kind kind) {
            this.player = player;
            this.from = from;
            this.to = to;
            this.kind = kind;
        }
    }

    private static final List<Timed> TIMED = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    /** A gesture just began for this bender: start what goes with it. */
    static void started(AbstractClientPlayer player, AnimState state, Gesture id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible() || !EnlightenedClientConfig.effects() || far(player)) {
            return;
        }
        long now = level.getGameTime();
        switch (id) {
            case EARTH_ROCK_THROW, EARTH_WALL, EARTH_SEISMIC, EARTH_ARMOR_ON, EARTH_LAND -> add(player, now + 1, now + 3, Kind.STOMP_DUST);
            case EARTH_RISE, EARTH_SAND_LIFT -> add(player, now + 2, now + 8, Kind.GROUND_RING);
            case EARTH_METAL_PULL, EARTH_WRECK -> add(player, now + 4, now + 11, Kind.GATHER_HANDS);
            case EARTH_QUICKSAND, EARTH_LAVA_PATH, WATER_FREEZE -> add(player, now + 4, now + 8, Kind.GROUND_RING);
            case AIR_BLAST, AIR_BLADES, FIRE_BALL, EARTH_LAVA_FLICK, WATER_WHIP, WATER_SNOW, FIRE_LIGHTNING -> add(player, now + 3, now + 5, Kind.BURST_HAND);
            case FIRE_BALL_BIG, FIRE_LIGHTNING_BOTH, EARTH_METAL_BLADES, WATER_ICE_KNIVES, AIR_TORNADO -> add(player, now + 4, now + 7, Kind.BURST_BOTH);
            case AIR_SHIELD_ON, WATER_OCTOPUS_ON, WATER_ARMS_ON, WATER_SPOUT_ON, AIR_FLIGHT_ON, FIRE_REDIRECT_ON -> add(player, now + 2, now + 10, Kind.GATHER_HANDS);
            case WATER_HEAL, FIRE_HEAL -> add(player, now + 4, now + 20, Kind.GATHER_HANDS);
            case FIRE_RING -> add(player, now + 4, now + 12, Kind.GROUND_RING);
            case FIRE_REDIRECTED, WATER_BLOOD_GRAB, WATER_SUPPRESS -> add(player, now + 2, now + 8, Kind.GATHER_HANDS);
            default -> {
            }
        }
    }

    private static boolean far(AbstractClientPlayer player) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return player.position().distanceToSqr(camera) > 48.0 * 48.0;
    }

    private static void add(AbstractClientPlayer player, long from, long to, Kind kind) {
        TIMED.add(new Timed(player.getId(), from, to, kind));
    }

    /** Every client tick: the timed effects, and the steady ones of the stances. */
    static void tick(ClientLevel level) {
        long now = level.getGameTime();
        if (!EnlightenedClientConfig.effects()) {
            TIMED.clear();
            return;
        }
        TIMED.removeIf(t -> {
            if (now > t.to) {
                return true;
            }
            if (now >= t.from && level.getEntity(t.player) instanceof AbstractClientPlayer p && !p.isInvisible()) {
                play(level, p, t.kind);
            }
            return false;
        });
        for (AbstractClientPlayer p : level.players()) {
            AnimState s = Animator.peek(p);
            if (s == null || p.isInvisible() || far(p)) {
                continue;
            }
            stances(level, p, s);
        }
    }

    private static void play(ClientLevel level, AbstractClientPlayer p, Kind kind) {
        BendingData data = Benders.get(p);
        Element element = data.element();
        switch (kind) {
            case STOMP_DUST -> {
                Vec3 foot = HandTracker.foot(p, HumanoidArm.RIGHT, 1.0F);
                if (foot == null) {
                    foot = p.position();
                }
                BlockPos under = BlockPos.containing(foot.x, foot.y - 0.1, foot.z);
                BlockState state = level.getBlockState(under);
                if (state.isAir()) {
                    state = level.getBlockState(under.below());
                }
                if (!state.isAir()) {
                    ParticleOptions dust = new BlockParticleOption(ParticleTypes.BLOCK, state);
                    for (int i = 0; i < 8; i++) {
                        double a = RANDOM.nextDouble() * Math.PI * 2.0;
                        level.addParticle(dust, foot.x, foot.y + 0.05, foot.z, Math.cos(a) * 0.12, 0.08 + RANDOM.nextDouble() * 0.1, Math.sin(a) * 0.12);
                    }
                }
            }
            case GATHER_HANDS -> {
                for (HumanoidArm arm : HumanoidArm.values()) {
                    Vec3 hand = HandTracker.handOrGuess(p, arm, 1.0F);
                    for (int i = 0; i < 2; i++) {
                        Vec3 at = hand.add(spread(0.35));
                        Vec3 v = hand.subtract(at).scale(0.15);
                        level.addParticle(mote(data, element, 0.8F), at.x, at.y, at.z, v.x, v.y + 0.01, v.z);
                    }
                }
            }
            case BURST_HAND, BURST_BOTH -> {
                for (HumanoidArm arm : HumanoidArm.values()) {
                    if (kind == Kind.BURST_HAND && arm != p.getMainArm()) {
                        continue;
                    }
                    Vec3 hand = HandTracker.handOrGuess(p, arm, 1.0F);
                    Vec3 look = p.getViewVector(1.0F);
                    for (int i = 0; i < 4; i++) {
                        Vec3 v = look.scale(0.08 + RANDOM.nextDouble() * 0.1).add(spread(0.04));
                        level.addParticle(mote(data, element, 1.0F), hand.x, hand.y, hand.z, v.x, v.y, v.z);
                    }
                }
            }
            case GROUND_RING -> {
                Vec3 feet = p.position();
                for (int i = 0; i < 6; i++) {
                    double a = RANDOM.nextDouble() * Math.PI * 2.0;
                    double r = 0.8 + RANDOM.nextDouble() * 0.6;
                    level.addParticle(mote(data, element, 0.9F), feet.x + Math.cos(a) * r, feet.y + 0.1, feet.z + Math.sin(a) * r, 0.0, 0.04, 0.0);
                }
            }
        }
    }

    /** The steady effects of the stances: flames at a fire fist, sparks of a lightning charge, mist off water arms. */
    private static void stances(ClientLevel level, AbstractClientPlayer p, AnimState s) {
        BendingData data = Benders.get(p);
        Element element = data.element();
        float w;
        if ((w = Math.max(s.stance(Stance.FIRE_FIST), Math.max(s.stance(Stance.FIRE_WHIP), s.stance(Stance.FIRE_BALL_CHARGE)))) > 0.3F) {
            ParticleOptions flame = FireBending.flame(data);
            boolean both = s.stance(Stance.FIRE_FIST) > 0.3F;
            for (HumanoidArm arm : HumanoidArm.values()) {
                if (!both && arm != p.getMainArm()) {
                    continue;
                }
                Vec3 hand = HandTracker.handOrGuess(p, arm, 1.0F);
                if (RANDOM.nextFloat() < w) {
                    Vec3 at = hand.add(spread(0.12));
                    level.addParticle(flame, at.x, at.y, at.z, 0.0, 0.02, 0.0);
                }
            }
        }
        if ((w = s.stance(Stance.FIRE_BREATH)) > 0.3F) {
            Vec3 mouth = p.getEyePosition(1.0F).add(0.0, -0.2, 0.0).add(p.getViewVector(1.0F).scale(0.3));
            for (int i = 0; i < 2; i++) {
                Vec3 v = p.getViewVector(1.0F).scale(0.12).add(spread(0.03));
                level.addParticle(FireBending.flame(data), mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            }
        }
        if ((w = s.stance(Stance.LIGHTNING_CHARGE)) > 0.3F) {
            Vec3 right = HandTracker.handOrGuess(p, HumanoidArm.RIGHT, 1.0F);
            Vec3 left = HandTracker.handOrGuess(p, HumanoidArm.LEFT, 1.0F);
            for (int i = 0; i < 3; i++) {
                double f = RANDOM.nextDouble();
                Vec3 at = right.lerp(left, f).add(spread(0.08));
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
            }
        }
        if ((w = s.stance(Stance.REDIRECT)) > 0.5F && level.getGameTime() % 4 == 0) {
            Vec3 hand = HandTracker.handOrGuess(p, p.getMainArm(), 1.0F);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 0.0, 0.0, 0.0);
        }
        if ((w = Math.max(s.stance(Stance.WATER_ARMS), Math.max(s.stance(Stance.OCTOPUS), s.stance(Stance.BLOODBENDING)))) > 0.3F) {
            boolean blood = s.stance(Stance.BLOODBENDING) > 0.3F;
            for (HumanoidArm arm : HumanoidArm.values()) {
                if (RANDOM.nextFloat() < 0.5F * w) {
                    Vec3 at = HandTracker.handOrGuess(p, arm, 1.0F).add(spread(0.15));
                    ParticleOptions drop = blood ? new EnergyParticleOptions(0xB01030, 0.8F) : ParticleTypes.FALLING_WATER;
                    level.addParticle(drop, at.x, at.y, at.z, 0.0, blood ? 0.02 : -0.02, 0.0);
                }
            }
        }
        if ((w = s.stance(Stance.ROCK_LIFT)) > 0.3F && level.getGameTime() % 3 == 0) {
            Vec3 at = HandTracker.handOrGuess(p, p.getMainArm(), 1.0F).add(spread(0.1));
            level.addParticle(new EnergyParticleOptions(element.color, 0.7F), at.x, at.y, at.z, 0.0, 0.04, 0.0);
        }
        if ((w = s.stance(Stance.AIR_SHIELD)) > 0.3F && level.getGameTime() % 2 == 0) {
            for (HumanoidArm arm : HumanoidArm.values()) {
                Vec3 at = HandTracker.handOrGuess(p, arm, 1.0F).add(spread(0.2));
                level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, 0.0, 0.01, 0.0);
            }
        }
        if (s.wGlide > 0.5F && level.getGameTime() % 2 == 0 && !EnlightenedClientConfig.reducedMotion()) {
            Vec3 at = p.getPosition(1.0F).add(spread(0.5)).add(0.0, 1.0, 0.0);
            level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    /** The mote of an element: its energy colour, or the flame of a firebender's look. */
    private static ParticleOptions mote(BendingData data, Element element, float scale) {
        if (element == Element.FIRE) {
            return FireBending.flame(data);
        }
        if (element == Element.AIR) {
            return ParticleTypes.CLOUD;
        }
        return new EnergyParticleOptions(element.color, scale);
    }

    private static Vec3 spread(double r) {
        return new Vec3((RANDOM.nextDouble() - 0.5) * 2.0 * r, (RANDOM.nextDouble() - 0.5) * 2.0 * r, (RANDOM.nextDouble() - 0.5) * 2.0 * r);
    }

    static void clear() {
        TIMED.clear();
    }

    private GestureEffects() {
    }
}
