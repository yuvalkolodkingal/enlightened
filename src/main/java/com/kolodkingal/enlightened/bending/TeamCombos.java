package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.entity.TornadoEntity;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The team combos (1.1.0): two benders of different elements working one move together, the way Aang and Katara or
 * Toph and Zuko did. One bender casts an art of a combo's first side (a tornado, an air blast, a water whip, an earth
 * wall...); within {@code team.teamComboWindowTicks} another bender, of another element, casts an art of its second
 * side into it (aimed within {@code team.teamComboRange} blocks of the first, or for a tornado, through the tornado
 * itself). A fire ball, a rock, an ice knife or a lava shuriken that flies into another bender's tornado joins it too.
 * Either order works, except that a tornado is always the one that is joined.
 *
 * <p>Both benders take the combo's cooldown, both find it (the Bending Scroll's Combos tab lists it), and neither is
 * hurt by it. Its strength is the two benders' together. When one bender casts an art that begins a team combo the
 * other has not found yet, the other's bar names the art to cast into it.
 */
public final class TeamCombos {
    /** One cast that may join another: who, where he aimed (from his eyes to the point), and when. */
    private record TeamCast(UUID player, ResourceKey<Level> dimension, Ability ability, long tick, Vec3 eye, Vec3 focus) {
    }

    /** Where a team combo goes off: a point, the way the first bender's art faced, and the tornado it joins, if any. */
    record Anchor(Vec3 at, Vec3 dir, @Nullable TornadoEntity tornado) {
    }

    private static final List<TeamCast> RECENT = new ArrayList<>();
    private static final Map<UUID, Map<Combos, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<Combos, Long>> HINTED = new HashMap<>();

    // ---- the settings ----

    static boolean enabled() {
        return !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.TEAM_COMBOS.get();
    }

    static int window() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.TEAM_COMBO_WINDOW_TICKS.get() : 60;
    }

    static double range() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.TEAM_COMBO_RANGE.get() : 8.0;
    }

    static float baseDamage() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.TEAM_COMBO_DAMAGE.get().floatValue() : 8.0F;
    }

    static double fireTornadoLife() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_TORNADO_LIFE.get() : 0.35;
    }

    static float fireTornadoDamage() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.FIRE_TORNADO_DAMAGE.get().floatValue() : 3.0F;
    }

    /** Whether any team combo has this art on one of its sides. */
    static boolean involved(Ability ability) {
        for (Combos c : Combos.VALUES) {
            if (c.team && (c.first.contains(ability) || c.second.contains(ability))) {
                return true;
            }
        }
        return false;
    }

    // ---- the casts ----

    /** A cast went off: it may join another bender's recent cast, or wait for another to join it. */
    public static void cast(ServerPlayer p, BendingData data, Ability ability) {
        if (!enabled() || !involved(ability)) {
            return;
        }
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        RECENT.removeIf(r -> now - r.tick > window() || r.player.equals(p.getUUID()) && r.ability == ability);
        Vec3 eye = p.getEyePosition();
        TeamCast mine = new TeamCast(p.getUUID(), level.dimension(), ability, now, eye, Shots.aim(p, 32).end());
        MinecraftServer server = level.getServer();
        for (TeamCast other : new ArrayList<>(RECENT)) {
            if (other.player.equals(p.getUUID()) || other.dimension != level.dimension() || other.ability.element == ability.element) {
                continue;
            }
            ServerPlayer partner = server.getPlayerList().getPlayer(other.player);
            if (partner == null || !partner.isAlive()) {
                continue;
            }
            for (Combos combo : Combos.VALUES) {
                if (!combo.team) {
                    continue;
                }
                boolean mineFirst = combo.first.contains(ability) && combo.second.contains(other.ability);
                boolean mineSecond = combo.second.contains(ability) && combo.first.contains(other.ability);
                if (!mineFirst && !mineSecond) {
                    continue;
                }
                // A tornado is always the one joined: it has to be there before the other art is cast into it.
                if (mineFirst && ability == Ability.TORNADO) {
                    continue;
                }
                ServerPlayer first = mineFirst ? p : partner;
                ServerPlayer second = mineFirst ? partner : p;
                if (resting(first, combo, now) || resting(second, combo, now)) {
                    continue;
                }
                Anchor anchor = anchor(level, combo, mineFirst ? mine : other, mineFirst ? other : mine);
                if (anchor == null) {
                    continue;
                }
                RECENT.remove(other);
                fire(combo, first, second, anchor);
                return;
            }
        }
        RECENT.add(mine);
        hint(p, ability, now);
    }

    /** Where the second cast meets the first, or null if it does not come near it. */
    @Nullable
    private static Anchor anchor(ServerLevel level, Combos combo, TeamCast first, TeamCast second) {
        double range = range();
        Vec3 dir = flat(first.focus.subtract(first.eye));
        if (first.ability == Ability.TORNADO) {
            // Through the tornado: the nearest of the first bender's tornadoes of plain air that the second one's aim passes.
            AABB box = new AABB(second.eye, second.focus).inflate(range + 6);
            TornadoEntity best = null;
            double bestD = Double.MAX_VALUE;
            for (TornadoEntity t : level.getEntitiesOfClass(TornadoEntity.class, box, t -> t.isAlive() && t.kind() == TornadoEntity.Kind.AIR)) {
                if (!first.player.equals(t.owner())) {
                    continue;
                }
                Vec3 heart = t.position().add(0, t.size() * 1.2, 0);
                double d = segmentDistance(second.eye, second.focus, heart);
                if (d <= range * 0.5 + t.size() && d < bestD) {
                    best = t;
                    bestD = d;
                }
            }
            return best == null ? null : new Anchor(best.position(), dir, best);
        }
        double d = Math.min(segmentDistance(second.eye, second.focus, first.focus), first.focus.distanceTo(second.focus));
        if (d > range) {
            return null;
        }
        Vec3 near = closest(second.eye, second.focus, first.focus);
        return new Anchor(first.focus.lerp(near, 0.5), dir, null);
    }

    /**
     * A shot of another bender's element flew into a tornado (a fire ball, a lava shuriken, a rock, sand, an ice knife,
     * a snowball): the tornado takes it, a team combo. Returns whether the shot was taken (it is gone then).
     */
    public static boolean shotIntoTornado(ServerLevel level, BendingShotEntity shot, @Nullable ServerPlayer shooter) {
        if (!enabled() || shooter == null) {
            return false;
        }
        Combos combo = switch (shot.kind()) {
            case FIRE_BALL, LAVA -> Combos.FIRESTORM;
            case ROCK, SAND -> Combos.DUST_DEVIL;
            case ICE_KNIFE, SNOWBALL -> Combos.BLIZZARD;
            default -> null;
        };
        if (combo == null) {
            return false;
        }
        for (TornadoEntity t : level.getEntitiesOfClass(TornadoEntity.class, shot.getBoundingBox().inflate(5), t -> t.isAlive() && t.kind() == TornadoEntity.Kind.AIR)) {
            UUID owner = t.owner();
            if (owner == null || owner.equals(shooter.getUUID())) {
                continue;
            }
            Vec3 to = shot.position().subtract(t.position());
            double height = t.size() * 2.5;
            if (Math.sqrt(to.x * to.x + to.z * to.z) > t.size() + 0.5 || to.y < -0.5 || to.y > height + 0.5) {
                continue;
            }
            ServerPlayer air = level.getServer().getPlayerList().getPlayer(owner);
            long now = level.getGameTime();
            if (air == null || resting(air, combo, now) || resting(shooter, combo, now)) {
                continue;
            }
            fire(combo, air, shooter, new Anchor(t.position(), flat(shot.getDeltaMovement()), t));
            shot.discard();
            return true;
        }
        return false;
    }

    private static boolean resting(ServerPlayer p, Combos combo, long now) {
        return COOLDOWNS.getOrDefault(p.getUUID(), Map.of()).getOrDefault(combo, 0L) > now;
    }

    /** The two benders' strength together, 1 to 3. */
    static int strength(ServerPlayer a, ServerPlayer b) {
        return Math.max(1, Math.min(3, 1 + (Mastery.strength(a) + Mastery.strength(b)) / 12));
    }

    /** The combo goes off: both rest, both see it, both find it. */
    static void fire(Combos combo, ServerPlayer first, ServerPlayer second, Anchor anchor) {
        long now = first.level().getGameTime();
        for (ServerPlayer p : List.of(first, second)) {
            COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(combo, now + combo.cooldown);
        }
        perform(combo, first, second, anchor, strength(first, second));
        for (ServerPlayer p : List.of(first, second)) {
            ServerPlayer other = p == first ? second : first;
            p.displayClientMessage(Component.translatable("message.enlightened.combo.team", combo.displayName(), other.getDisplayName())
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.POINT.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
            Combos.celebrate(p, combo);
        }
    }

    /**
     * A hint for the others near: when a bender casts an art that begins a team combo, every bender of another element
     * within reach who knows an art of its other side and has not found it yet reads on his bar what to cast into it.
     */
    private static void hint(ServerPlayer p, Ability ability, long now) {
        for (ServerPlayer other : p.serverLevel().players()) {
            if (other == p || other.distanceTo(p) > 24) {
                continue;
            }
            BendingData od = Benders.get(other);
            if (!od.canBend()) {
                continue;
            }
            Set<String> found = Combos.discovered(other);
            Map<Combos, Long> hinted = HINTED.computeIfAbsent(other.getUUID(), k -> new HashMap<>());
            for (Combos combo : Combos.VALUES) {
                if (!combo.team || found.contains(combo.id()) || hinted.getOrDefault(combo, Long.MIN_VALUE / 2) + Combos.HINT_REST > now
                        || resting(other, combo, now)) {
                    continue;
                }
                java.util.Set<Ability> answers = combo.first.contains(ability) ? combo.second : combo.second.contains(ability) && ability != Ability.TORNADO ? combo.first : null;
                if (answers == null) {
                    continue;
                }
                Ability answer = null;
                for (Ability a : answers) {
                    if (od.has(a) && a.element != ability.element && a != Ability.TORNADO) {
                        answer = a;
                        break;
                    }
                }
                if (answer == null) {
                    continue;
                }
                hinted.put(combo, now);
                other.displayClientMessage(Component.translatable("message.enlightened.combo.team_hint", answer.displayName(), p.getDisplayName(),
                        ability.displayName()).withStyle(ChatFormatting.GOLD), true);
                other.level().playSound(null, other.getX(), other.getY(), other.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
                break;
            }
        }
    }

    // ---- the moves ----

    /** Whom a team combo may hurt: what either bender's bending may hurt, never either of them. */
    static List<LivingEntity> targets(ServerPlayer a, ServerPlayer b, Vec3 center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : Shots.area(a, center, radius)) {
            if (e != b && AreaTargets.mayHurt(b, e)) {
                out.add(e);
            }
        }
        return out;
    }

    private static void perform(Combos combo, ServerPlayer first, ServerPlayer second, Anchor anchor, int strength) {
        ServerLevel level = first.serverLevel();
        float damage = baseDamage() * (0.8F + strength * 0.2F);
        int fire = Variants.fireColour(Benders.get(second));
        TornadoEntity t = anchor.tornado;
        switch (combo) {
            case FIRESTORM -> {
                if (t != null && t.infuse(TornadoEntity.Kind.FIRE, second.getUUID(), 1.25F, fireTornadoLife(), fireTornadoDamage())) {
                    // The tornado takes the fire: a burst of flame as it catches, then a short, fierce life.
                    for (LivingEntity e : targets(first, second, t.position().add(0, 1, 0), t.size() + 1.5)) {
                        com.kolodkingal.enlightened.fire.FireBending.ignite(second, e, 4);
                    }
                    Fx.send(level, Fx.FIRE_SPIN, t.position(), t.position().add(0, t.size() * 2.5, 0), fire, t.size() + 1.0F);
                    burst(level, t.position().add(0, t.size(), 0), ParticleTypes.FLAME, 60, t.size());
                    sound(level, t.position(), ModSounds.FIRE_RING.get(), 1.6F, 0.6F);
                    sound(level, t.position(), SoundEvents.FIRECHARGE_USE, 1.4F, 0.7F);
                } else {
                    gale(first, second, anchor, damage * 1.6F, 12 + strength * 2, ModDamageTypes.FLAME, Gale.FIRE, fire);
                }
            }
            case HURRICANE -> {
                if (t != null && t.infuse(TornadoEntity.Kind.WATER, second.getUUID(), 1.6F, 1.8, 1.5F)) {
                    Fx.send(level, Fx.WATER_VORTEX, t.position(), t.position().add(0, t.size() * 2.5, 0), Element.WATER.color, t.size());
                    burst(level, t.position().add(0, t.size(), 0), ParticleTypes.SPLASH, 80, t.size());
                    sound(level, t.position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.6F, 0.6F);
                    sound(level, t.position(), ModSounds.WATER_GATHER.get(), 1.4F, 0.5F);
                }
            }
            case DUST_DEVIL -> {
                if (t != null && t.infuse(TornadoEntity.Kind.SAND, second.getUUID(), 1.3F, 1.2, 1.5F)) {
                    burst(level, t.position().add(0, t.size(), 0), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), 80, t.size());
                    sound(level, t.position(), ModSounds.SAND_HISS.get(), 1.6F, 0.6F);
                    sound(level, t.position(), ModSounds.TORNADO.get(), 1.2F, 0.7F);
                } else {
                    gale(first, second, anchor, damage, 12 + strength * 2, ModDamageTypes.EARTH, Gale.SAND, Element.EARTH.glow);
                }
            }
            case BLIZZARD -> {
                if (t != null && t.infuse(TornadoEntity.Kind.ICE, second.getUUID(), 1.2F, 1.0, 2.0F)) {
                    burst(level, t.position().add(0, t.size(), 0), ParticleTypes.SNOWFLAKE, 80, t.size());
                    sound(level, t.position(), ModSounds.ICE_CRACK.get(), 1.6F, 0.6F);
                    sound(level, t.position(), ModSounds.TORNADO.get(), 1.2F, 0.9F);
                } else {
                    gale(first, second, anchor, damage, 12 + strength * 2, ModDamageTypes.ICE, Gale.ICE, Element.WATER.glow);
                }
            }
            case SCALDING_STEAM -> {
                double radius = 3.5 + strength * 0.75;
                int seconds = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.STEAM_CLOUD_SECONDS.get() : 6;
                LINGERING.add(new Lingering(Lingering.Kind.STEAM, level, anchor.at, radius, damage * 0.3F, level.getGameTime() + seconds * 20L,
                        first.getUUID(), second.getUUID()));
                burst(level, anchor.at.add(0, 1, 0), ParticleTypes.CLOUD, 120, radius);
                Fx.at(level, Fx.WATER_SPLASH, anchor.at, 0xFFFFFF, (float) radius);
                sound(level, anchor.at, SoundEvents.FIRE_EXTINGUISH, 2.0F, 0.5F);
                sound(level, anchor.at, SoundEvents.LAVA_EXTINGUISH, 1.6F, 0.7F);
            }
            case MAGMA_ERUPTION -> erupt(first, second, anchor, damage, strength, fire);
            case MUD_FLOOD -> {
                LINGERING.add(new Lingering(Lingering.Kind.MUD, level, anchor.at, 6 + strength * 1.5, damage * 0.6F, level.getGameTime() + 30,
                        second.getUUID(), first.getUUID()));
                burst(level, anchor.at.add(0, 0.5, 0), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), 60, 1.5);
                sound(level, anchor.at, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.6F, 0.5F);
                sound(level, anchor.at, ModSounds.EARTH_RISE.get(), 1.4F, 0.6F);
            }
            case STORM_SURGE -> {
                double radius = 6 + strength;
                Vec3 heart = anchor.at.add(0, 1, 0);
                int struck = 0;
                for (LivingEntity e : targets(second, first, anchor.at, radius * 1.5)) {
                    boolean wet = e.isInWaterOrRain() || e.position().distanceTo(anchor.at) <= radius * 0.5;
                    if (!wet || e.position().distanceTo(anchor.at) > (e.isInWaterOrRain() ? radius * 1.5 : radius)) {
                        continue;
                    }
                    Shots.hurt(second, e, damage * 1.5F, ModDamageTypes.LIGHTNING);
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 40, 0, false, true, true));
                    Fx.send(level, Fx.LIGHTNING, heart, e.position().add(0, e.getBbHeight() * 0.5, 0), 0, 1.0F);
                    struck++;
                }
                burst(level, heart, ParticleTypes.ELECTRIC_SPARK, 80, radius * 0.6);
                burst(level, anchor.at.add(0, 0.3, 0), ParticleTypes.SPLASH, 60, radius * 0.6);
                Fx.at(level, Fx.WATER_SPLASH, anchor.at, Element.WATER.color, (float) radius * 0.5F);
                sound(level, anchor.at, ModSounds.LIGHTNING_STRIKE.get(), 1.8F, 0.8F);
                sound(level, anchor.at, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2F, 0.7F);
            }
            case LIGHTNING_ROD -> {
                // The bolt goes into the metal and leaps from it to the nearest enemy, from him to the next, and on.
                Set<LivingEntity> hit = new HashSet<>();
                Vec3 from = anchor.at.add(0, 1, 0);
                int jumps = 3 + strength;
                double reach = 9;
                for (int i = 0; i < jumps; i++) {
                    LivingEntity next = null;
                    double best = Double.MAX_VALUE;
                    for (LivingEntity e : targets(second, first, from, reach)) {
                        double d = e.position().distanceTo(from);
                        if (!hit.contains(e) && d < best) {
                            next = e;
                            best = d;
                        }
                    }
                    if (next == null) {
                        break;
                    }
                    hit.add(next);
                    Vec3 to = next.position().add(0, next.getBbHeight() * 0.5, 0);
                    Shots.hurt(second, next, damage * 1.4F, ModDamageTypes.LIGHTNING);
                    next.addEffect(new MobEffectInstance(ModEffects.STUNNED, 30, 0, false, true, true));
                    Fx.send(level, Fx.LIGHTNING, from, to, 0, 1.0F);
                    from = to;
                    reach = 6;
                }
                Fx.at(level, Fx.METAL_PULL, anchor.at, 0xD0D8E0, 3.0F);
                burst(level, anchor.at.add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK, 50, 1.5);
                sound(level, anchor.at, ModSounds.LIGHTNING_STRIKE.get(), 1.8F, 1.0F);
                sound(level, anchor.at, ModSounds.METAL_CLANG.get(), 1.4F, 0.6F);
            }
            default -> {
            }
        }
    }

    /** What a gale carries: fire, sand or frost. */
    enum Gale { FIRE, SAND, ICE }

    /**
     * An air blast that took another element: a broad gust rolling on from the meeting point the way the air was going,
     * hurting, pushing, and burning, blinding or freezing what it meets.
     */
    private static void gale(ServerPlayer first, ServerPlayer second, Anchor anchor, float damage, double range, ResourceKey<DamageType> type, Gale gale, int color) {
        ServerLevel level = first.serverLevel();
        Vec3 dir = anchor.dir;
        Vec3 from = anchor.at.subtract(dir.scale(1.5)).add(0, 0.8, 0);
        for (LivingEntity e : targets(first, second, from.add(dir.scale(range * 0.5)), range * 0.5 + 3)) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double along = to.dot(dir);
            double side = to.subtract(dir.scale(along)).length();
            if (along < -1 || along > range || side > 2.0 + along * 0.25) {
                continue;
            }
            ServerPlayer blame = gale == Gale.FIRE ? second : first;
            Shots.hurt(blame, e, damage, type);
            Shots.shove(e, dir, 1.4, 0.35);
            switch (gale) {
                case FIRE -> com.kolodkingal.enlightened.fire.FireBending.ignite(second, e, 6);
                case SAND -> {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                }
                case ICE -> {
                    e.setTicksFrozen(e.getTicksRequiredToFreeze() + 80);
                    e.addEffect(new MobEffectInstance(ModEffects.FROZEN, 50, 0));
                }
            }
        }
        ParticleOptions grain = switch (gale) {
            case FIRE -> ParticleTypes.FLAME;
            case SAND -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
            case ICE -> ParticleTypes.SNOWFLAKE;
        };
        for (int i = 0; i < 90; i++) {
            double along = level.random.nextDouble() * range;
            double spread = 0.8 + along * 0.25;
            Vec3 at = from.add(dir.scale(along)).add((level.random.nextDouble() - 0.5) * spread * 2, (level.random.nextDouble() - 0.3) * 1.6,
                    (level.random.nextDouble() - 0.5) * spread * 2);
            level.sendParticles(grain, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.05);
        }
        if (gale == Gale.FIRE) {
            Fx.send(level, Fx.DRAGON_FIRE, from, from.add(dir.scale(range)), color, 2.5F);
            sound(level, from, ModSounds.FIRE_BLAST.get(), 1.6F, 0.5F);
        } else {
            Fx.send(level, Fx.AIR_BLAST, from, from.add(dir.scale(range)), color, (float) range);
        }
        sound(level, from, ModSounds.AIR_BLAST.get(), 1.6F, 0.6F);
    }

    /** Earth and fire: the ground at the meeting point melts to magma, everyone on it is thrown up burning, molten rock flies. */
    private static void erupt(ServerPlayer first, ServerPlayer second, Anchor anchor, float damage, int strength, int fire) {
        ServerLevel level = first.serverLevel();
        BlockPos ground = groundUnder(level, anchor.at);
        Vec3 heart = ground == null ? anchor.at : Vec3.atBottomCenterOf(ground.above());
        int r = strength >= 3 ? 3 : 2;
        int seconds = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.MAGMA_SECONDS.get() : 6;
        if (ground != null) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) {
                        continue;
                    }
                    BlockPos cell = groundUnder(level, Vec3.atCenterOf(ground.offset(dx, 1, dz)));
                    if (cell != null && level.getBlockState(cell).is(ModTags.EARTH) && Math.abs(cell.getY() - ground.getY()) <= 2) {
                        TempBlocks.replace(first, level, cell, Blocks.MAGMA_BLOCK.defaultBlockState(), seconds * 20 + level.random.nextInt(20));
                    }
                }
            }
        }
        for (LivingEntity e : targets(first, second, heart.add(0, 0.5, 0), r + 1.5)) {
            Shots.hurt(second, e, damage * 1.3F, ModDamageTypes.LAVA);
            com.kolodkingal.enlightened.fire.FireBending.ignite(second, e, 5);
            e.setDeltaMovement(e.getDeltaMovement().x, 0.9, e.getDeltaMovement().z);
            e.hurtMarked = true;
        }
        // Molten rock thrown up out of the ground, falling round the eruption.
        for (int i = 0; i < 5 + strength * 2; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            Vec3 dir = new Vec3(Math.cos(a) * 0.35, 1.0, Math.sin(a) * 0.35);
            BendingShotEntity rock = new BendingShotEntity(level, BendingShotEntity.Kind.LAVA, heart.add(0, 0.6, 0), first, fire);
            rock.setDamage(damage * 0.4F);
            rock.setTouch(3);
            rock.launch(dir, 0.7 + level.random.nextDouble() * 0.3);
            level.addFreshEntity(rock);
        }
        level.sendParticles(ParticleTypes.LAVA, heart.x, heart.y + 0.5, heart.z, 40, r * 0.5, 0.4, r * 0.5, 0.2);
        level.sendParticles(ParticleTypes.FLAME, heart.x, heart.y + 0.5, heart.z, 60, r * 0.6, 0.8, r * 0.6, 0.08);
        level.sendParticles(ParticleTypes.EXPLOSION, heart.x, heart.y + 0.8, heart.z, 2, 0.5, 0.3, 0.5, 0.0);
        Fx.at(level, Fx.LAVA_BURST, heart, fire, r + 1.0F);
        level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
        sound(level, heart, ModSounds.LAVA_FLOW.get(), 1.6F, 0.7F);
        sound(level, heart, SoundEvents.FIRECHARGE_USE, 1.4F, 0.5F);
    }

    /** The top solid block at or just under a point (from two above to four below), or null. */
    @Nullable
    static BlockPos groundUnder(ServerLevel level, Vec3 at) {
        BlockPos start = BlockPos.containing(at);
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos pos = start.offset(0, dy, 0);
            BlockState state = level.getBlockState(pos);
            if (state.isSolid() && !level.getBlockState(pos.above()).isSolid()) {
                return pos;
            }
        }
        return null;
    }

    // ---- what lingers: the steam cloud, the mud flood ----

    private static final class Lingering {
        enum Kind { STEAM, MUD }

        final Kind kind;
        final ServerLevel level;
        final Vec3 center;
        final double radius;
        final float damage;
        final long until;
        /** Whom it is blamed on, and the other bender (spared too). */
        final UUID blame;
        final UUID other;
        final Set<Integer> struck = new HashSet<>();
        final long born;

        Lingering(Kind kind, ServerLevel level, Vec3 center, double radius, float damage, long until, UUID blame, UUID other) {
            this.kind = kind;
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.damage = damage;
            this.until = until;
            this.blame = blame;
            this.other = other;
            this.born = level.getGameTime();
        }
    }

    private static final List<Lingering> LINGERING = new ArrayList<>();

    /** Every server tick: the steam scalds, the mud rolls on. */
    public static void tick(MinecraftServer server) {
        if (LINGERING.isEmpty()) {
            return;
        }
        Iterator<Lingering> it = LINGERING.iterator();
        while (it.hasNext()) {
            Lingering l = it.next();
            long now = l.level.getGameTime();
            ServerPlayer blame = server.getPlayerList().getPlayer(l.blame);
            ServerPlayer other = server.getPlayerList().getPlayer(l.other);
            if (now > l.until || blame == null || other == null) {
                it.remove();
                continue;
            }
            switch (l.kind) {
                case STEAM -> steam(l, blame, other, now);
                case MUD -> mud(l, blame, other, now);
            }
        }
    }

    private static void steam(Lingering l, ServerPlayer blame, ServerPlayer other, long now) {
        ServerLevel level = l.level;
        if (now % 2 == 0) {
            for (int i = 0; i < 10; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(level.random.nextDouble()) * l.radius;
                level.sendParticles(ParticleTypes.CLOUD, l.center.x + Math.cos(a) * d, l.center.y + 0.3 + level.random.nextDouble() * 2.5,
                        l.center.z + Math.sin(a) * d, 1, 0.1, 0.1, 0.1, 0.02);
            }
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, l.center.x, l.center.y + 1.5, l.center.z, 1, l.radius * 0.4, 0.4, l.radius * 0.4, 0.01);
        }
        if (now % 10 == 0) {
            for (LivingEntity e : targets(blame, other, l.center.add(0, 1, 0), l.radius)) {
                Shots.hurt(blame, e, l.damage, ModDamageTypes.FIRE);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
            if (now % 30 == 0) {
                level.playSound(null, l.center.x, l.center.y, l.center.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 0.5F);
            }
        }
    }

    private static void mud(Lingering l, ServerPlayer blame, ServerPlayer other, long now) {
        ServerLevel level = l.level;
        // The flood rolls out half a block a tick.
        double reach = Math.min(l.radius, (now - l.born) * 0.5 + 1.0);
        int ring = Math.max(8, (int) (reach * 7));
        BlockState mud = Blocks.MUD.defaultBlockState();
        for (int i = 0; i < ring; i++) {
            double a = i * Math.PI * 2 / ring + level.random.nextDouble() * 0.1;
            Vec3 at = l.center.add(Math.cos(a) * reach, 0, Math.sin(a) * reach);
            BlockPos ground = groundUnder(level, at.add(0, 0.5, 0));
            if (ground == null) {
                continue;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, mud), at.x, ground.getY() + 1.2, at.z, 2, 0.2, 0.2, 0.2, 0.1);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SPLASH, at.x, ground.getY() + 1.1, at.z, 2, 0.2, 0.1, 0.2, 0.1);
            }
            // The soil the flood passes over turns to mud for a while.
            BlockState was = level.getBlockState(ground);
            if (com.kolodkingal.enlightened.earth.Mud.muddy(was) && !was.is(Blocks.MUD) && level.random.nextInt(3) == 0) {
                TempBlocks.replace(blame, level, ground, mud, 20 * 8 + level.random.nextInt(40));
            }
        }
        for (LivingEntity e : targets(blame, other, l.center, reach + 0.5)) {
            if (!l.struck.add(e.getId())) {
                continue;
            }
            Shots.hurt(blame, e, l.damage, ModDamageTypes.EARTH);
            com.kolodkingal.enlightened.earth.Mud.bog(e);
            e.clearFire();
            Shots.push(e, l.center, 1.2, 0.35);
        }
        if ((now - l.born) % 6 == 0) {
            level.playSound(null, l.center.x, l.center.y, l.center.z, SoundEvents.MUD_STEP, SoundSource.PLAYERS, 1.4F, 0.6F);
        }
    }

    // ---- helpers ----

    private static void burst(ServerLevel level, Vec3 at, ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread * 0.5, spread * 0.5, spread * 0.5, 0.1);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    /** The point of the segment from {@code a} to {@code b} nearest to {@code p}. */
    static Vec3 closest(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        if (len < 1.0E-6) {
            return a;
        }
        double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
        return a.add(ab.scale(t));
    }

    static double segmentDistance(Vec3 a, Vec3 b, Vec3 p) {
        return closest(a, b, p).distanceTo(p);
    }

    /** Whether a bender's rest after this team combo is over (true if he never did it). */
    public static boolean ready(ServerPlayer p, Combos combo) {
        return !resting(p, combo, p.level().getGameTime());
    }

    public static void forget(ServerPlayer player) {
        RECENT.removeIf(r -> r.player.equals(player.getUUID()));
        COOLDOWNS.remove(player.getUUID());
        HINTED.remove(player.getUUID());
    }

    public static void clear() {
        RECENT.clear();
        COOLDOWNS.clear();
        HINTED.clear();
        LINGERING.clear();
    }

    private TeamCombos() {
    }
}
