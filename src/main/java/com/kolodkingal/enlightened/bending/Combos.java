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
    FIRE_SPIN(Element.FIRE, 200, Ability.FIRE_SHIELD, Ability.FIRE_RING),
    // ---- 1.0.25: thirteen more, to be found in a fight ----
    /** Avatar: an earth wall, then a fire ball into it: the wall bursts into flame and burns whoever touches it. */
    BURNING_WALL(Element.AVATAR, 240, Ability.EARTH_WALL, Ability.FIRE_BALL),
    /** Earth (lava): an earth wall, then the lava path: the wall melts to glowing magma that burns whoever touches it. */
    MAGMA_WALL(Element.EARTH, 240, Ability.EARTH_WALL, Ability.LAVA_PATH),
    /** Fire: the jet, then lightning: you shoot ahead and lightning leaps from you to every enemy near. */
    THUNDER_JET(Element.FIRE, 240, Ability.FIRE_JET, Ability.LIGHTNING),
    /** Fire: a fist of fire, then a fire ball: a long breath of flame like a dragon's. */
    DRAGON_BREATH(Element.FIRE, 160, Ability.FIRE_FIST, Ability.FIRE_BALL),
    /** Air: the jump, then the burst: you fly up and slam the air down round you, flattening all near. */
    AIR_SLAM(Element.AIR, 160, Ability.AIR_JUMP, Ability.AIR_BURST),
    /** Air: suction, then the blast: what you pulled in is flung far away. */
    VACUUM_CANNON(Element.AIR, 160, Ability.AIR_SUCTION, Ability.AIR_BLAST),
    /** Earth: two rocks, then the shockwave: a storm of rocks flies ahead. */
    ROCK_STORM(Element.EARTH, 160, Ability.ROCK_THROW, Ability.ROCK_THROW, Ability.SHOCKWAVE),
    /** Earth: the grab, then the shockwave: an earthquake that stuns everyone round you. */
    EARTHQUAKE(Element.EARTH, 220, Ability.EARTH_GRAB, Ability.SHOCKWAVE),
    /** Water: two whips, then the spout: a great wave rolls ahead and washes everything away. */
    TIDAL_WAVE(Element.WATER, 200, Ability.WATER_WHIP, Ability.WATER_WHIP, Ability.WATER_SPOUT),
    /** Water: healing, then the bubble: a healing tide for you and every friend near. */
    HEALING_TIDE(Element.WATER, 400, Ability.WATER_HEALING, Ability.WATER_BUBBLE),
    /** Avatar: a water whip, then a fire ball: a scalding cloud of steam that blinds. */
    STEAM_CLOUD(Element.AVATAR, 200, Ability.WATER_WHIP, Ability.FIRE_BALL),
    /** Avatar: the tornado, then a fire ball: a tornado of fire. */
    FIRE_TORNADO(Element.AVATAR, 300, Ability.TORNADO, Ability.FIRE_BALL),
    /** Avatar: an air blast, then a rock: a sandstorm that blinds and slows. */
    SANDSTORM(Element.AVATAR, 200, Ability.AIR_BLAST, Ability.ROCK_THROW);

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
            if (discover(p, combo)) {
                // The first time: a title, a fanfare, and the scroll's Combos tab learns it.
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 50, 15));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                        Component.translatable("message.enlightened.combo.discovered").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(combo.displayName().copy().withStyle(ChatFormatting.AQUA)));
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.0F);
                com.kolodkingal.enlightened.network.ModNetwork.sendTo(p, listing(p));
            }
            return;
        }
        hint(p, data, recent, now);
    }

    // ---- discovery ----

    private static final String DISCOVERED = "enlightened_combos";
    /** How long a hint for one combo stays quiet after it is given. */
    public static final int HINT_REST = 20 * 45;
    private static final Map<UUID, Map<Combos, Long>> HINTED = new HashMap<>();

    /** The combos a player has found (kept through deaths). */
    public static java.util.Set<String> discovered(net.minecraft.world.entity.player.Player p) {
        net.minecraft.nbt.ListTag list = p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)
                .getList(DISCOVERED, net.minecraft.nbt.Tag.TAG_STRING);
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    /** Notes a combo found; true if it is the first time. */
    public static boolean discover(net.minecraft.world.entity.player.Player p, Combos combo) {
        net.minecraft.nbt.CompoundTag kept = p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        net.minecraft.nbt.ListTag list = kept.getList(DISCOVERED, net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(combo.id())) {
                return false;
            }
        }
        list.add(net.minecraft.nbt.StringTag.valueOf(combo.id()));
        kept.put(DISCOVERED, list);
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, kept);
        return true;
    }

    /** The combos found, for the scroll. */
    public static com.kolodkingal.enlightened.network.ComboPayload listing(ServerPlayer p) {
        return new com.kolodkingal.enlightened.network.ComboPayload(List.copyOf(discovered(p)));
    }

    /**
     * A hint in the heat of a fight: when the casts just made begin a combo the bender has not found yet (and can do:
     * he knows its arts, it is ready), the bar says a combo stirs and names the next art. Each combo hints once in a
     * while, so a fight is not a lecture.
     */
    private static void hint(ServerPlayer p, BendingData data, Deque<Recent> recent, long now) {
        java.util.Set<String> found = discovered(p);
        Map<Combos, Long> cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        Map<Combos, Long> hinted = HINTED.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        Recent[] tail = recent.toArray(new Recent[0]);
        for (Combos combo : VALUES) {
            if (found.contains(combo.id()) || !data.element().commands(combo.element) || !combo.known(data)
                    || cds.getOrDefault(combo, 0L) > now || hinted.getOrDefault(combo, Long.MIN_VALUE / 2) + HINT_REST > now) {
                continue;
            }
            int done = prefix(tail, combo, now);
            if (done > 0 && done < combo.sequence.size()) {
                hinted.put(combo, now);
                p.displayClientMessage(Component.translatable("message.enlightened.combo.hint", combo.sequence.get(done).displayName())
                        .withStyle(ChatFormatting.GOLD), true);
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);
                return;
            }
        }
    }

    /** How many first steps of a combo the latest casts make (the last cast being the last of them), or 0. */
    private static int prefix(Recent[] tail, Combos combo, long now) {
        int window = window();
        for (int k = combo.sequence.size() - 1; k >= 1; k--) {
            if (tail.length < k) {
                continue;
            }
            boolean ok = true;
            for (int i = 0; i < k && ok; i++) {
                Recent r = tail[tail.length - k + i];
                ok = r.ability == combo.sequence.get(i) && (i == 0 || r.tick - tail[tail.length - k + i - 1].tick <= window);
            }
            if (ok && now - tail[tail.length - 1].tick <= window) {
                return k;
            }
        }
        return 0;
    }

    // ---- the burning walls ----

    private record Burning(ServerLevel level, net.minecraft.world.phys.AABB box, long until, UUID owner) {
    }

    private static final List<Burning> BURNING = new java.util.ArrayList<>();

    /** Every tick: whoever touches a burning wall catches fire. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (BURNING.isEmpty()) {
            return;
        }
        BURNING.removeIf(b -> b.level.getGameTime() > b.until);
        for (Burning b : BURNING) {
            if (b.level.getGameTime() % 5 != 0) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(b.owner);
            for (LivingEntity e : b.level.getEntitiesOfClass(LivingEntity.class, b.box.inflate(0.4))) {
                if (e.getUUID().equals(b.owner) || e.fireImmune()) {
                    continue;
                }
                if (owner != null) {
                    Shots.hurt(owner, e, 2.0F, ModDamageTypes.FIRE);
                    com.kolodkingal.enlightened.fire.FireBending.ignite(owner, e, 4);
                } else {
                    e.igniteForSeconds(4);
                }
            }
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
        if (performMore(p, data, level, color, facing, strength)) {
            Fx.around(level, Fx.AIR_BURST, p, color, 0.0F);
            return;
        }
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
                Fx.send(level, Fx.BLAZE_ARC, p.position().add(0, 0.6, 0), p.position().add(facing), Variants.fireColour(data), (float) range);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.FIRE_BLAST.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.3F, 0.7F);
            }
            case FIRE_SPIN -> {
                double radius = 6 + strength * 1.5;
                for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                    Shots.hurt(p, e, 5.0F + strength * 1.5F, ModDamageTypes.FIRE);
                    com.kolodkingal.enlightened.fire.FireBending.ignite(p, e, 5);
                    Shots.push(e, p.position(), 2.2, 0.7);
                }
                Fx.at(level, Fx.FIRE_SPIN, p.position(), Variants.fireColour(data), (float) radius);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.FIRE_RING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
            }
        }
        Fx.around(level, Fx.AIR_BURST, p, color, 0.0F);
    }

    /** The combos of 1.0.25. */
    private boolean performMore(ServerPlayer p, BendingData data, ServerLevel level, int color, Vec3 facing, int strength) {
        int fire = Variants.fireColour(data);
        switch (this) {
            case BURNING_WALL, MAGMA_WALL -> {
                // A wall five wide and three high, two blocks ahead, of magma, with flames along its top.
                Vec3 side = new Vec3(-facing.z, 0, facing.x).normalize();
                Vec3 base = p.position().add(facing.scale(2.5));
                net.minecraft.world.phys.AABB box = null;
                int ticks = 20 * (6 + strength * 2);
                for (int s = -2; s <= 2; s++) {
                    BlockPos foot = BlockPos.containing(base.add(side.scale(s)));
                    for (int up = 0; up < 3; up++) {
                        BlockPos pos = foot.above(up);
                        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced() || TempBlocks.isRaised(level, pos)) {
                            TempBlocks.replace(p, level, pos, Blocks.MAGMA_BLOCK.defaultBlockState(), ticks);
                        }
                        box = box == null ? new net.minecraft.world.phys.AABB(pos) : box.minmax(new net.minecraft.world.phys.AABB(pos));
                    }
                    BlockPos top = foot.above(3);
                    if (level.getBlockState(top).isAir()) {
                        TempBlocks.place(p, level, top, Blocks.FIRE.defaultBlockState(), ticks);
                    }
                }
                if (box != null) {
                    BURNING.add(new Burning(level, box, level.getGameTime() + ticks, p.getUUID()));
                }
                for (LivingEntity e : Shots.area(p, base.add(0, 1, 0), 2.5)) {
                    com.kolodkingal.enlightened.fire.FireBending.ignite(p, e, 5);
                    Shots.push(e, p.position(), 0.8, 0.3);
                }
                Fx.at(level, Fx.LAVA_BURST, base, fire, 3.0F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.EARTH_RISE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.6F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
            }
            case THUNDER_JET -> {
                p.setDeltaMovement(facing.scale(1.6).add(0, 0.35, 0));
                p.hurtMarked = true;
                int bolts = 0;
                for (LivingEntity e : Shots.area(p, p.position().add(facing.scale(4)), 9.0)) {
                    if (bolts >= 3 + strength) {
                        break;
                    }
                    Shots.hurt(p, e, 6.0F + strength * 2, ModDamageTypes.LIGHTNING);
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 30, 0, false, true, true));
                    Fx.send(level, Fx.LIGHTNING, p.position().add(0, 1, 0), e.position().add(0, e.getBbHeight() * 0.5, 0), 0, 1.0F);
                    bolts++;
                }
                for (int i = 0; i < 12; i++) {
                    Vec3 at = p.position().subtract(facing.scale(i * 0.4));
                    level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 3, 0.15, 0.1, 0.15, 0.02);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 1, at.z, 2, 0.3, 0.3, 0.3, 0.1);
                }
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.LIGHTNING_STRIKE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.3F, 1.1F);
            }
            case DRAGON_BREATH -> {
                double range = 10 + strength * 2;
                for (LivingEntity e : Shots.cone(p, range, 28)) {
                    Shots.hurt(p, e, 5.0F + strength * 1.5F, ModDamageTypes.FLAME);
                    com.kolodkingal.enlightened.fire.FireBending.ignite(p, e, 6);
                }
                Fx.send(level, Fx.DRAGON_FIRE, p.getEyePosition().subtract(0, 0.2, 0), p.getEyePosition().add(facing.scale(range)), fire, (float) range);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.FIRE_BLAST.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.5F);
            }
            case AIR_SLAM -> {
                p.setDeltaMovement(p.getDeltaMovement().x, 0.9, p.getDeltaMovement().z);
                p.hurtMarked = true;
                double radius = 6 + strength;
                for (LivingEntity e : Shots.area(p, p.position(), radius)) {
                    Shots.hurt(p, e, 4.0F + strength, ModDamageTypes.AIR);
                    Shots.push(e, p.position(), 1.8, -0.2);
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 40, 0, false, true, true));
                }
                Fx.at(level, Fx.SHOCKWAVE, p.position(), color, (float) radius);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AIR_BLAST.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
            }
            case VACUUM_CANNON -> {
                double range = 12 + strength * 2;
                for (LivingEntity e : Shots.cone(p, range, 35)) {
                    Shots.hurt(p, e, 4.0F + strength, ModDamageTypes.AIR);
                    Shots.shove(e, facing, 2.6 + strength * 0.3, 0.6);
                }
                Fx.send(level, Fx.AIR_BLAST, p.getEyePosition(), p.getEyePosition().add(facing.scale(range)), color, (float) range);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AIR_BLAST.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.6F, 0.5F);
            }
            case ROCK_STORM -> {
                Shots.Aim aim = Shots.aim(p, 24);
                BlockState stone = level.getBlockState(p.blockPosition().below());
                if (!stone.isSolid()) {
                    stone = Blocks.STONE.defaultBlockState();
                }
                for (int i = 0; i < 6 + strength * 2; i++) {
                    Vec3 dir = aim.end().subtract(p.getEyePosition()).normalize()
                            .yRot((float) Math.toRadians((level.random.nextDouble() - 0.5) * 30))
                            .xRot((float) Math.toRadians((level.random.nextDouble() - 0.5) * 12));
                    Vec3 from = p.position().add(facing.scale(1.2)).add((level.random.nextDouble() - 0.5) * 2, 0.6 + level.random.nextDouble(), (level.random.nextDouble() - 0.5) * 2);
                    BendingShotEntity shot = new BendingShotEntity(level, BendingShotEntity.Kind.ROCK, from, p, color);
                    shot.setBlockState(stone);
                    shot.setDamage(3.0F + strength);
                    shot.launch(dir, 1.3 + level.random.nextDouble() * 0.4);
                    level.addFreshEntity(shot);
                }
                Fx.at(level, Fx.EARTH_RISE, p.position(), color, 2.0F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.EARTH_RISE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.8F);
            }
            case EARTHQUAKE -> {
                double radius = 9 + strength * 2;
                for (LivingEntity e : Shots.area(p, p.position(), radius)) {
                    if (!e.onGround()) {
                        continue;
                    }
                    Shots.hurt(p, e, 4.0F + strength, ModDamageTypes.EARTH);
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 50 + strength * 10, 0, false, true, true));
                    e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
                    e.hurtMarked = true;
                }
                for (int i = 0; i < 40; i++) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    double d = level.random.nextDouble() * radius;
                    BlockPos under = BlockPos.containing(p.getX() + Math.cos(a) * d, p.getY() - 1, p.getZ() + Math.sin(a) * d);
                    BlockState st = level.getBlockState(under);
                    if (!st.isAir()) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), under.getX() + 0.5, under.getY() + 1.1, under.getZ() + 0.5, 8, 0.4, 0.1, 0.4, 0.1);
                    }
                }
                Fx.at(level, Fx.SHOCKWAVE, p.position(), color, (float) radius);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 0.5F);
            }
            case TIDAL_WAVE -> {
                double range = 10 + strength * 2;
                for (LivingEntity e : Shots.cone(p, range, 50)) {
                    Shots.hurt(p, e, 4.0F + strength, ModDamageTypes.WATER);
                    Shots.shove(e, facing, 1.8 + strength * 0.2, 0.4);
                    e.clearFire();
                }
                Vec3 side = new Vec3(-facing.z, 0, facing.x);
                for (double d = 1; d <= range; d += 0.7) {
                    for (double s = -d * 0.5; s <= d * 0.5; s += 0.8) {
                        Vec3 at = p.position().add(facing.scale(d)).add(side.scale(s));
                        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.6, at.z, 2, 0.2, 0.3, 0.2, 0.2);
                    }
                }
                Fx.send(level, Fx.WATER_SPLASH, p.position(), p.position().add(facing.scale(range)), color, (float) range);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_SPLASH_HIGH_SPEED, net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.6F);
            }
            case HEALING_TIDE -> {
                double radius = 6 + strength;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius))) {
                    boolean friend = e == p || e instanceof net.minecraft.world.entity.player.Player
                            || e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p);
                    if (friend) {
                        e.heal(4.0F + strength * 2);
                        e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1, e.getZ(), 8, 0.4, 0.5, 0.4, 0.05);
                    }
                }
                Fx.at(level, Fx.WATER_HEAL, p.position(), color, (float) radius);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 1.2F);
            }
            case STEAM_CLOUD -> {
                double range = 8 + strength;
                for (LivingEntity e : Shots.cone(p, range, 50)) {
                    Shots.hurt(p, e, 3.0F + strength, ModDamageTypes.FIRE);
                    e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 0));
                }
                for (int i = 0; i < 60; i++) {
                    Vec3 at = p.getEyePosition().add(facing.scale(1 + level.random.nextDouble() * range))
                            .add((level.random.nextDouble() - 0.5) * 3, (level.random.nextDouble() - 0.5) * 2, (level.random.nextDouble() - 0.5) * 3);
                    level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0.02);
                }
                level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
            }
            case FIRE_TORNADO -> {
                TornadoEntity t = new TornadoEntity(level, p, p.position().add(facing.scale(2.5)), facing, 2.5F + strength * 0.5F, 160, 2.0F + strength, true);
                level.addFreshEntity(t);
                for (LivingEntity e : Shots.cone(p, 10, 40)) {
                    com.kolodkingal.enlightened.fire.FireBending.ignite(p, e, 6);
                    Shots.hurt(p, e, 3.0F + strength, ModDamageTypes.FIRE);
                }
                Fx.at(level, Fx.FIRE_SPIN, p.position().add(facing.scale(2.5)), fire, 4.0F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.TORNADO.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.7F);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.FIRE_RING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.6F);
            }
            case SANDSTORM -> {
                double range = 10 + strength;
                for (LivingEntity e : Shots.cone(p, range, 55)) {
                    Shots.hurt(p, e, 2.0F + strength, ModDamageTypes.EARTH);
                    e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0));
                    e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    Shots.shove(e, facing, 0.8, 0.2);
                }
                BlockParticleOption sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
                for (int i = 0; i < 80; i++) {
                    Vec3 at = p.position().add(facing.scale(1 + level.random.nextDouble() * range))
                            .add((level.random.nextDouble() - 0.5) * 4, level.random.nextDouble() * 2.5, (level.random.nextDouble() - 0.5) * 4);
                    level.sendParticles(sand, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0.1);
                }
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.AIR_BLAST.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.5F);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    public static void forget(ServerPlayer player) {
        RECENT.remove(player.getUUID());
        COOLDOWNS.remove(player.getUUID());
        HINTED.remove(player.getUUID());
    }

    public static void clear() {
        RECENT.clear();
        COOLDOWNS.clear();
        HINTED.clear();
        BURNING.clear();
    }
}
