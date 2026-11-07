package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.talk.Speaker;
import com.kolodkingal.enlightened.talk.Talk;
import com.kolodkingal.enlightened.world.Comet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fire Lord Ozai (1.0.27). He waits on the deck of his flagship over Wulong Forest. When someone answers the herald
 * "yes", Sozin's Comet comes ({@link Comet}) and he comes down on jets of fire to fight: fire blasts, streams of flame,
 * rings of fire, and lightning (with a moment's charge that warns of it), on the ground or hovering on his jets. Under
 * the comet his fire strikes twice as hard.
 *
 * <p>Fire barely touches him (a fifth of its harm), other blows little (two fifths); <b>water</b> strikes him fully.
 * Two ways beat him best: a firebender who has <b>mastered every art of fire</b>, fighting under the comet, strikes him
 * fully (and twice as hard, by the comet); or <b>energybending</b>, from close, takes his firebending away at once.
 * Beaten, he is taken down to the prison under the Fire Palace, powerless, and stays in his cell forever (he still
 * talks). If everyone who fought him falls or leaves, he goes back up to his ship and the comet passes.
 */
public class OzaiEntity extends Monster {
    public enum State { WAITING, DESCENDING, FIGHTING, RETURNING, PRISONER }

    /** How far from the field he looks for whom to fight. */
    public static final double SIGHT = 48.0;
    /** How long he waits with nobody to fight before going back up. */
    public static final int IDLE = 600;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(OzaiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> JETS = SynchedEntityData.defineId(OzaiEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.enlightened.ozai"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    @Nullable
    private BlockPos deck;
    @Nullable
    private BlockPos field;
    @Nullable
    private BlockPos cell;
    private int attackRest = 40;
    private int charging;
    private int idle;
    private int modeLeft;
    private boolean hover;
    @Nullable
    private LivingEntity target;

    public OzaiEntity(EntityType<? extends OzaiEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 300;
        bar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600.0).add(Attributes.ARMOR, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, SIGHT).add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, State.WAITING.ordinal());
        builder.define(JETS, false);
    }

    @Override
    protected void registerGoals() {
    }

    public State state() {
        State[] all = State.values();
        int s = entityData.get(STATE);
        return s >= 0 && s < all.length ? all[s] : State.WAITING;
    }

    private void setState(State s) {
        entityData.set(STATE, s.ordinal());
        bar.setVisible(s == State.DESCENDING || s == State.FIGHTING);
    }

    /** Whether he flies on his jets now (the renderer's pose, and the flames). */
    public boolean jets() {
        return entityData.get(JETS);
    }

    private void setJets(boolean on) {
        entityData.set(JETS, on);
        setNoGravity(on);
    }

    public boolean fighting() {
        return state() == State.DESCENDING || state() == State.FIGHTING;
    }

    public boolean prisoner() {
        return state() == State.PRISONER;
    }

    /** Where he waits, where he fights and where he will be held (set by the arena). */
    public void place(BlockPos deck, BlockPos field, BlockPos cell) {
        this.deck = deck;
        this.field = field;
        this.cell = cell;
    }

    @Nullable
    public BlockPos cell() {
        return cell;
    }

    // ---- the fight begins ----

    /** Someone said yes to the herald: the comet comes, and he comes down. False if he cannot fight now. */
    public boolean challenge(ServerPlayer by) {
        if (state() != State.WAITING || !(level() instanceof ServerLevel level)) {
            return false;
        }
        setHealth(getMaxHealth());
        setState(State.DESCENDING);
        setJets(true);
        Comet.keep(level);
        for (ServerPlayer p : near(level, 96)) {
            title(p, Component.translatable("message.enlightened.ozai.comet").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.translatable("message.enlightened.ozai.comet.sub").withStyle(ChatFormatting.GOLD));
        }
        level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2F, 0.7F);
        return true;
    }

    private List<ServerPlayer> near(ServerLevel level, double r) {
        Vec3 c = field != null ? Vec3.atCenterOf(field) : position();
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(c) <= r * r) {
                out.add(p);
            }
        }
        return out;
    }

    private static void title(ServerPlayer p, Component title, Component sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
    }

    // ---- living ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            if (jets()) {
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 right = Vec3.directionFromRotation(0, yBodyRot + 90).scale(0.2 * s);
                    level().addParticle(ParticleTypes.FLAME, getX() + right.x, getY() + 0.05, getZ() + right.z, 0, -0.15, 0);
                }
            }
            return;
        }
        bar.setProgress(getHealth() / getMaxHealth());
        switch (state()) {
            case WAITING -> {
                setDeltaMovement(Vec3.ZERO);
                if (deck != null && position().distanceToSqr(Vec3.atBottomCenterOf(deck)) > 4) {
                    moveTo(deck.getX() + 0.5, deck.getY(), deck.getZ() + 0.5, getYRot(), 0);
                }
                setNoGravity(true);
            }
            case DESCENDING -> {
                Comet.keep(level);
                Vec3 goal = field != null ? Vec3.atBottomCenterOf(field).add(0, 1, 0) : position();
                Vec3 to = goal.subtract(position());
                if (to.length() < 0.8) {
                    setState(State.FIGHTING);
                    setJets(false);
                    level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.4F);
                    level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 60, 1.5, 0.1, 1.5, 0.08);
                    for (ServerPlayer p : near(level, 64)) {
                        p.sendSystemMessage(Component.translatable("message.enlightened.ozai.arrive").withStyle(ChatFormatting.RED));
                    }
                } else {
                    Vec3 step = to.normalize().scale(Math.min(0.55, to.length()));
                    setDeltaMovement(Vec3.ZERO);
                    setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
                    lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, goal);
                    jetTrail(level);
                }
            }
            case FIGHTING -> fight(level);
            case RETURNING -> {
                Vec3 goal = deck != null ? Vec3.atBottomCenterOf(deck) : position().add(0, 30, 0);
                Vec3 to = goal.subtract(position());
                heal(2.0F);
                if (to.length() < 0.8) {
                    setState(State.WAITING);
                    setJets(false);
                    setNoGravity(true);
                } else {
                    Vec3 step = to.normalize().scale(Math.min(0.6, to.length()));
                    setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
                    jetTrail(level);
                }
            }
            case PRISONER -> {
                setTarget(null);
                if (cell != null && position().distanceToSqr(Vec3.atBottomCenterOf(cell)) > 9) {
                    moveTo(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, getYRot(), 0);
                }
            }
        }
    }

    private void jetTrail(ServerLevel level) {
        Vec3 right = Vec3.directionFromRotation(0, yBodyRot + 90).scale(0.22);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 foot = position().add(right.scale(s));
            level.sendParticles(ParticleTypes.FLAME, foot.x, foot.y, foot.z, 4, 0.05, 0.05, 0.05, 0.02);
            level.sendParticles(ParticleTypes.SMALL_FLAME, foot.x, foot.y - 0.4, foot.z, 3, 0.05, 0.3, 0.05, 0.01);
        }
        if (tickCount % 10 == 0) {
            playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 0.5F);
        }
    }

    private float power() {
        return Comet.active(level()) ? Comet.POWER : 1.0F;
    }

    private DamageSource source(ResourceKey<DamageType> type) {
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), this);
    }

    private boolean valid(LivingEntity e) {
        return e != null && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                && (field == null || e.position().distanceToSqr(Vec3.atCenterOf(field)) <= SIGHT * SIGHT);
    }

    private void fight(ServerLevel level) {
        Comet.keep(level);
        if (!valid(target)) {
            target = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer p : near(level, SIGHT)) {
                if (valid(p) && p.distanceToSqr(this) < best) {
                    best = p.distanceToSqr(this);
                    target = p;
                }
            }
        }
        if (target == null) {
            if (++idle > IDLE) {
                // Nobody left to fight: he goes back up, and the comet passes.
                idle = 0;
                setState(State.RETURNING);
                setJets(true);
            }
            return;
        }
        idle = 0;
        setTarget(target);
        getLookControl().setLookAt(target, 30, 30);
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        // Now on the ground, now on his jets.
        if (--modeLeft <= 0) {
            hover = !hover && random.nextInt(3) == 0;
            modeLeft = hover ? 80 + random.nextInt(60) : 100 + random.nextInt(80);
            setJets(hover);
        }
        double dist = distanceTo(target);
        if (hover) {
            Vec3 over = target.position().add(0, 6, 0).add(Vec3.directionFromRotation(0, tickCount * 3.0F).scale(7));
            Vec3 to = over.subtract(position());
            setDeltaMovement(to.scale(0.08));
            jetTrail(level);
        } else if (dist > 9) {
            getNavigation().moveTo(target, 1.2);
        } else if (dist < 4) {
            Vec3 away = position().subtract(target.position()).multiply(1, 0, 1).normalize();
            getNavigation().moveTo(getX() + away.x * 4, getY(), getZ() + away.z * 4, 1.2);
        } else {
            getNavigation().stop();
        }
        if (charging > 0) {
            // The lightning gathers: sparks round him, then it strikes.
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2, getZ(), 6, 0.5, 0.7, 0.5, 0.2);
            if (--charging == 0) {
                lightning(level);
            }
            return;
        }
        if (--attackRest > 0) {
            return;
        }
        boolean low = getHealth() < getMaxHealth() * 0.5F;
        int pick = random.nextInt(10);
        if (pick < 3 && dist < 24) {
            fireBlast(level);
            attackRest = low ? 20 : 30;
        } else if (pick < 5 && dist < 11) {
            fireStream(level);
            attackRest = low ? 25 : 35;
        } else if (pick < 7 && dist < 7) {
            fireRing(level);
            attackRest = low ? 30 : 45;
        } else if (pick < 9) {
            charging = 20;
            playSound(ModSounds.LIGHTNING_CHARGE.get(), 1.2F, 0.8F);
            attackRest = low ? 40 : 60;
        } else {
            jetDash(level);
            attackRest = 25;
        }
    }

    private Vec3 hand() {
        Vec3 right = Vec3.directionFromRotation(0, yBodyRot + 90).scale(0.35);
        return position().add(0, 1.35, 0).add(right).add(getLookAngle().scale(0.4));
    }

    private void fireBlast(ServerLevel level) {
        for (int i = -1; i <= 1; i++) {
            Vec3 dir = target.getEyePosition().subtract(hand()).normalize().yRot((float) Math.toRadians(i * 8));
            SmallFireball ball = new SmallFireball(level, this, dir.scale(1.2));
            ball.setPos(hand());
            level.addFreshEntity(ball);
        }
        Fx.send(level, Fx.FIRE_BURST, hand(), hand().add(getLookAngle()), 0xFF6A1A, 1.5F);
        playSound(ModSounds.FIRE_BLAST.get(), 1.3F, 0.7F);
    }

    private void fireStream(ServerLevel level) {
        Vec3 from = hand();
        Vec3 dir = target.getEyePosition().subtract(from).normalize();
        Fx.send(level, Fx.DRAGON_FIRE, from, from.add(dir.scale(11)), 0xFF6A1A, 11.0F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(11))) {
            if (e == this || !valid(e)) {
                continue;
            }
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            if (to.length() < 11 && to.normalize().dot(dir) > Math.cos(Math.toRadians(22))) {
                e.hurt(source(ModDamageTypes.FLAME), 6.0F * power());
                e.igniteForSeconds(5);
            }
        }
        playSound(ModSounds.FIRE_BLAST.get(), 1.5F, 0.5F);
    }

    private void fireRing(ServerLevel level) {
        Fx.at(level, Fx.FIRE_SPIN, position(), 0xFF6A1A, 7.0F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7))) {
            if (e == this || !valid(e)) {
                continue;
            }
            e.hurt(source(ModDamageTypes.FIRE), 6.0F * power());
            e.igniteForSeconds(4);
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1).normalize();
            e.push(away.x * 1.6, 0.5, away.z * 1.6);
            e.hurtMarked = true;
        }
        playSound(ModSounds.FIRE_RING.get(), 1.5F, 0.6F);
    }

    private void lightning(ServerLevel level) {
        if (!valid(target)) {
            return;
        }
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Fx.send(level, Fx.LIGHTNING, hand(), to, 0, 1.0F);
        target.hurt(source(ModDamageTypes.LIGHTNING), 10.0F * power());
        playSound(ModSounds.LIGHTNING_STRIKE.get(), 1.6F, 0.9F);
    }

    private void jetDash(ServerLevel level) {
        Vec3 dir = target.position().subtract(position()).normalize();
        setDeltaMovement(dir.scale(1.4).add(0, 0.3, 0));
        hurtMarked = true;
        jetTrail(level);
        playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
    }

    // ---- harm ----

    /** Whether a player has mastered every art of fire (each at its last level). */
    public static boolean masteredFire(Player p) {
        BendingData data = Benders.get(p);
        if (!data.element().commands(Element.FIRE)) {
            return false;
        }
        for (Ability a : Ability.values()) {
            if (a.element == Element.FIRE && data.level(a) < Ability.MAX_LEVEL) {
                return false;
            }
        }
        return true;
    }

    /** The share of a blow's harm that reaches him. */
    public float share(DamageSource source) {
        if (source.is(ModDamageTypes.WATER) || source.is(ModDamageTypes.ICE) || source.is(ModDamageTypes.BLOOD)) {
            return 1.0F;
        }
        boolean fire = source.is(ModDamageTypes.FIRE) || source.is(ModDamageTypes.FLAME) || source.is(ModDamageTypes.LIGHTNING)
                || source.is(DamageTypeTags.IS_FIRE);
        if (fire) {
            return source.getEntity() instanceof Player p && masteredFire(p) && Comet.active(level()) ? 1.0F : 0.2F;
        }
        return 0.4F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.getEntity() instanceof Player p && p.isCreative() && p.isShiftKeyDown()) {
            return super.hurt(source, amount);
        }
        if (state() != State.FIGHTING || source.getEntity() == this) {
            return false;
        }
        float dealt = amount * share(source);
        if (source.getEntity() instanceof LivingEntity attacker && attacker != target && valid(attacker)) {
            target = attacker;
        }
        if (getHealth() - dealt <= 1.0F) {
            defeat(source.getEntity() instanceof ServerPlayer p ? p : null, false);
            return true;
        }
        return super.hurt(source, dealt);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Energybending from close: his firebending is taken away, and the fight is over. */
    public boolean takeBending(ServerPlayer by) {
        if (state() != State.FIGHTING) {
            return false;
        }
        defeat(by, true);
        return true;
    }

    /** Beaten: the comet passes, and he is taken to his cell under the Fire Palace, to stay there forever. */
    public void defeat(@Nullable ServerPlayer by, boolean energy) {
        if (!(level() instanceof ServerLevel level) || state() == State.PRISONER) {
            return;
        }
        for (ServerPlayer p : near(level, 96)) {
            title(p, Component.translatable(energy ? "message.enlightened.ozai.taken" : "message.enlightened.ozai.defeated")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("message.enlightened.ozai.prison").withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(Component.translatable(energy ? "message.enlightened.ozai.taken.chat" : "message.enlightened.ozai.defeated.chat",
                    by != null ? by.getDisplayName() : Component.literal("?")).withStyle(ChatFormatting.GOLD));
            if (Benders.get(p).element().bends()) {
                com.kolodkingal.enlightened.bending.Mastery.addPoints(p, 5);
            }
        }
        if (energy) {
            Fx.send(level, Fx.ENERGYBEND, by != null ? by.position() : position(), position(), 0xFFFFFF, 0, getId());
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.8F, 1.4F);
        setState(State.PRISONER);
        setJets(false);
        setNoGravity(false);
        setTarget(null);
        target = null;
        setHealth(getMaxHealth());
        clearFire();
        removeAllEffects();
        getNavigation().stop();
        if (cell == null) {
            // The nearest Fire Palace: its prison, and Zuko on its throne.
            BlockPos palace = com.kolodkingal.enlightened.world.FirePalacePiece.find(level, field != null ? field : blockPosition());
            if (palace != null) {
                cell = com.kolodkingal.enlightened.world.FirePalacePiece.cell(palace);
                com.kolodkingal.enlightened.world.FirePalacePiece.crown(level, palace);
            }
        }
        if (cell != null) {
            level.getChunk(cell);
            moveTo(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, 180.0F, 0);
        }
        bar.removeAllPlayers();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && prisoner() && player instanceof ServerPlayer server) {
            Talk.begin(server, this, Speaker.OZAI);
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isPushable() {
        return state() == State.FIGHTING;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("State", state() == State.DESCENDING || state() == State.FIGHTING || state() == State.RETURNING ? State.RETURNING.name() : state().name());
        if (deck != null) {
            tag.put("Deck", NbtUtils.writeBlockPos(deck));
        }
        if (field != null) {
            tag.put("Field", NbtUtils.writeBlockPos(field));
        }
        if (cell != null) {
            tag.put("Cell", NbtUtils.writeBlockPos(cell));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        deck = NbtUtils.readBlockPos(tag, "Deck").orElse(null);
        field = NbtUtils.readBlockPos(tag, "Field").orElse(null);
        cell = NbtUtils.readBlockPos(tag, "Cell").orElse(null);
        try {
            setState(State.valueOf(tag.getString("State")));
        } catch (IllegalArgumentException e) {
            setState(State.WAITING);
        }
        if (state() == State.RETURNING) {
            setJets(true);
        }
    }
}
