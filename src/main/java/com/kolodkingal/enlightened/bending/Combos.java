package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.entity.TornadoEntity;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The combos, after ProjectKorra: a sequence of casts within a few seconds of each other that ends in a move bigger
 * than any one of them. A combo needs every ability in its sequence learned; it has a cooldown of its own. The
 * sequences are shown in the Bending Scroll's combo page and on the wiki.
 */
public enum Combos {
    /** Air: the shield, the tornado, the blast: a cyclone that runs along the ground. */
    TWISTER(Element.AIR, 200, Ability.AIR_SHIELD, Ability.TORNADO, Ability.AIR_BLAST),
    /** Air: two blades, then the burst: a fan of blades that sweeps the field. */
    AIR_SWEEP(Element.AIR, 120, Ability.AIR_BLADES, Ability.AIR_BLADES, Ability.AIR_BURST),
    /** Earth: a wall, then the shockwave: a dome of earth round the bender. */
    EARTH_DOME(Element.EARTH, 200, Ability.EARTH_WALL, Ability.SHOCKWAVE),
    /** Earth: the shockwave, then two pillars: pillars under every enemy near, flinging them up. */
    EARTH_PILLARS(Element.EARTH, 160, Ability.SHOCKWAVE, Ability.EARTH_PILLAR, Ability.EARTH_PILLAR),
    /** Water: the spout, then freeze: a wave of ice that freezes all ahead. */
    ICE_WAVE(Element.WATER, 160, Ability.WATER_SPOUT, Ability.FREEZE),
    /** Water: an ice wall, then two volleys of knives: a storm of ice bullets. */
    ICE_BULLETS(Element.WATER, 160, Ability.ICE_WALL, Ability.ICE_KNIVES, Ability.ICE_KNIVES),
    /** Fire: two fire balls, then the ring: a kick of fire that arcs from the feet. */
    FIRE_KICK(Element.FIRE, 120, Ability.FIRE_BALL, Ability.FIRE_BALL, Ability.FIRE_RING),
    /** Fire: the shield, then the ring: a whirl of flame spinning out far and wide. */
    FIRE_SPIN(Element.FIRE, 200, Ability.FIRE_SHIELD, Ability.FIRE_RING);

    public final Element element;
    /** The combo's own cooldown, in ticks. */
    public final int cooldown;
    public final List<Ability> sequence;

    Combos(Element element, int cooldown, Ability... sequence) {
        this.element = element;
        this.cooldown = cooldown;
        this.sequence = List.of(sequence);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("combo.enlightened." + id());
    }

    public Component description() {
        return Component.translatable("combo.enlightened." + id() + ".desc");
    }

    /** Whether the bender knows every ability of the combo. */
    public boolean known(BendingData data) {
        for (Ability a : sequence) {
            if (!data.has(a)) {
                return false;
            }
        }
        return true;
    }

    public static final Combos[] VALUES = values();

    private record Recent(Ability ability, long tick) {
    }

    private static final Map<UUID, Deque<Recent>> RECENT = new HashMap<>();
    private static final Map<UUID, Map<Combos, Long>> COOLDOWNS = new HashMap<>();

    private static int window() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.COMBO_WINDOW_TICKS.get() : 60;
    }

    /** A cast went off (its cooldown began): remember it, and see whether it completes a combo. */
    public static void cast(ServerPlayer p, BendingData data, Ability ability) {
        if (EnlightenedConfig.SPEC.isLoaded() && !EnlightenedConfig.COMBOS.get()) {
            return;
        }
        long now = p.level().getGameTime();
        Deque<Recent> recent = RECENT.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        recent.addLast(new Recent(ability, now));
        while (recent.size() > 6) {
            recent.removeFirst();
        }
        for (Combos combo : VALUES) {
            if (!data.element().commands(combo.element) || !combo.known(data) || !matches(recent, combo, now)) {
                continue;
            }
            Map<Combos, Long> cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
            if (cds.getOrDefault(combo, 0L) > now) {
                continue;
            }
            cds.put(combo, now + combo.cooldown);
            recent.clear();
            combo.perform(p, data);
            p.displayClientMessage(Component.translatable("message.enlightened.combo", combo.displayName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.POINT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
            return;
        }
    }

    private static boolean matches(Deque<Recent> recent, Combos combo, long now) {
        int n = combo.sequence.size();
        if (recent.size() < n) {
            return false;
        }
        Recent[] tail = recent.toArray(new Recent[0]);
        int window = window();
        for (int i = 0; i < n; i++) {
            Recent r = tail[tail.length - n + i];
            if (r.ability != combo.sequence.get(i)) {
                return false;
            }
            if (i > 0 && r.tick - tail[tail.length - n + i - 1].tick > window) {
                return false;
            }
        }
        return true;
    }

    private void perform(ServerPlayer p, BendingData data) {
        ServerLevel level = p.serverLevel();
        // The Avatar's combos take their element's own colour (his fire combos are fire, not his light).
        int color = data.element() == Element.AVATAR ? element.color : Benders.color(p);
        Vec3 facing = Shots.facing(p);
        int strength = Math.max(1, Math.min(3, 1 + Mastery.strength(p) / 6));
        switch (this) {
            case TWISTER -> {
                TornadoEntity t = new TornadoEntity(level, p, p.position().add(facing.scale(2.0)), facing, 2.0F + strength * 0.5F, 160, 2.0F + strength, true);
                level.addFreshEntity(t);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.TORNADO.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.8F);
            }
            case AIR_SWEEP -> {
                Shots.Aim aim = Shots.aim(p, 24);
                for (int i = -3; i <= 3; i++) {
                    Vec3 dir = aim.end().subtract(p.getEyePosition()).normalize().yRot((float) Math.toRadians(i * 9));
                    Vec3 from = Shots.hand(p, i < 0);
                    BendingShotEntity shot = new BendingShotEntity(level, BendingShotEntity.Kind.AIR_BLADE, from, p, color);
                    shot.setDamage(3.0F + strength);
                    shot.launch(dir, 1.6);
                    level.addFreshEntity(shot);
                }
                for (LivingEntity e : Shots.cone(p, 8, 60)) {
                    Shots.push(e, p.position(), 1.6, 0.5);
                }
                Fx.send(level, Fx.AIR_BLAST, p.getEyePosition(), p.getEyePosition().add(facing.scale(8)), color, 8.0F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AIR_BLADE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
            }
            case EARTH_DOME -> {
                BlockPos ground = p.blockPosition().below();
                BlockState state = level.getBlockState(ground);
                if (!state.isSolid()) {
                    state = Blocks.STONE.defaultBlockState();
                }
                int r = 2 + (strength >= 3 ? 1 : 0);
                BlockPos center = p.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, 0, -r), center.offset(r, r + 1, r))) {
                    double d = Math.sqrt(pos.distSqr(center));
                    if (d >= r - 0.5 && d <= r + 0.5 && !p.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos)) && level.getBlockState(pos).isAir()) {
                        TempBlocks.place(p, level, pos.immutable(), state, 20 * 8);
                    }
                }
                Fx.at(level, Fx.EARTH_RISE, p.position(), color, r + 0.5F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.EARTH_RISE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.7F);
            }
            case EARTH_PILLARS -> {
                int flung = 0;
                for (LivingEntity e : Shots.area(p, p.position(), 8.0)) {
                    BlockPos under = e.blockPosition().below();
                    BlockState state = level.getBlockState(under);
                    if (!state.isSolid()) {
                        continue;
                    }
                    e.setDeltaMovement(e.getDeltaMovement().x, 1.1 + strength * 0.1, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                    Shots.hurt(p, e, 3.0F + strength, ModDamageTypes.EARTH);
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 30, 0, false, true, true));
                    TempBlocks.place(p, level, e.blockPosition(), state, 40);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), e.getX(), e.getY() + 0.5, e.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
                    flung++;
                }
                Fx.at(level, Fx.SHOCKWAVE, p.position(), color, 8.0F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.EARTH_RISE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.6F);
            }
            case ICE_WAVE -> {
                double range = 8 + strength * 2;
                for (LivingEntity e : Shots.cone(p, range, 45)) {
                    Shots.hurt(p, e, 3.0F + strength, ModDamageTypes.ICE);
                    e.addEffect(new MobEffectInstance(ModEffects.FROZEN, 60 + strength * 20, 0));
                    e.setTicksFrozen(e.getTicksRequiredToFreeze() + 60);
                }
                Vec3 side = new Vec3(-facing.z, 0, facing.x);
                for (double d = 1; d <= range; d += 1) {
                    for (double s = -d * 0.6; s <= d * 0.6; s += 1) {
                        BlockPos pos = BlockPos.containing(p.position().add(facing.scale(d)).add(side.scale(s)));
                        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                            TempBlocks.place(p, level, pos, Blocks.SNOW.defaultBlockState(), 20 * 10);
                        }
                    }
                }
                Fx.send(level, Fx.ICE_WAVE, p.position(), p.position().add(facing), color, (float) range);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.ICE_CRACK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.6F);
            }
            case ICE_BULLETS -> {
                Shots.Aim aim = Shots.aim(p, 24);
                for (int i = 0; i < 6 + strength * 2; i++) {
                    Vec3 dir = aim.end().subtract(p.getEyePosition()).normalize()
                            .yRot((float) Math.toRadians((level.random.nextDouble() - 0.5) * 24))
                            .xRot((float) Math.toRadians((level.random.nextDouble() - 0.5) * 16));
                    Vec3 from = Shots.hand(p, i % 2 == 1).add(0, (i / 2) * 0.15, 0);
                    BendingShotEntity shot = new BendingShotEntity(level, BendingShotEntity.Kind.ICE_KNIFE, from, p, color);
                    shot.setDamage(2.5F + strength * 0.5F);
                    shot.setTouch(3);
                    shot.launch(dir, 1.6 + level.random.nextDouble() * 0.4);
                    level.addFreshEntity(shot);
                }
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.ICE_KNIFE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
            }
            case FIRE_KICK -> {
                double range = 5 + strength;
                for (LivingEntity e : Shots.cone(p, range, 50)) {
                    Shots.hurt(p, e, 5.0F + strength * 1.5F, ModDamageTypes.FLAME);
                    com.kolodkingal.enlightened.fire.FireBending.ignite(p, e, 5);
                    Shots.push(e, p.position(), 1.0, 0.5);
                }
                Fx.send(level, Fx.BLAZE_ARC, p.position().add(0, 0.6, 0), p.position().add(facing), Variants.blueFire(data) ? 1 : 0, (float) range);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.FIRE_BLAST.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.3F, 0.7F);
            }
            case FIRE_SPIN -> {
                double radius = 6 + strength * 1.5;
                for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                    Shots.hurt(p, e, 5.0F + strength * 1.5F, ModDamageTypes.FIRE);
                    com.kolodkingal.enlightened.fire.FireBending.ignite(p, e, 5);
                    Shots.push(e, p.position(), 2.2, 0.7);
                }
                Fx.at(level, Fx.FIRE_SPIN, p.position(), Variants.blueFire(data) ? 1 : 0, (float) radius);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.FIRE_RING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
            }
        }
        Fx.around(level, Fx.AIR_BURST, p, color, 0.0F);
    }

    public static void forget(ServerPlayer player) {
        RECENT.remove(player.getUUID());
        COOLDOWNS.remove(player.getUUID());
    }

    public static void clear() {
        RECENT.clear();
        COOLDOWNS.clear();
    }
}
