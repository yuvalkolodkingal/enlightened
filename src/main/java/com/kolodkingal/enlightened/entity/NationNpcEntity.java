package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.talk.Speaker;
import com.kolodkingal.enlightened.talk.Talk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A person of the Fire Nation story who talks: {@link Kind#ZUKO}, Fire Lord on the palace's throne once his father is
 * beaten (1.0.28); {@link Kind#HERALD}, the Fire Nation herald at Wulong Forest, who asks whether you will face the
 * Fire Lord; {@link Kind#GUARD}, the guards of the Fire Palace. They keep their place; nothing harms them.
 */
public class NationNpcEntity extends PathfinderMob {
    public enum Kind { ZUKO, HERALD, GUARD }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(NationNpcEntity.class, EntityDataSerializers.INT);

    public NationNpcEntity(EntityType<? extends NationNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public Kind kind() {
        Kind[] all = Kind.values();
        int k = entityData.get(KIND);
        return k >= 0 && k < all.length ? all[k] : Kind.ZUKO;
    }

    public NationNpcEntity kind(Kind kind) {
        entityData.set(KIND, kind.ordinal());
        return this;
    }

    public Speaker speaker() {
        return switch (kind()) {
            case ZUKO -> Speaker.ZUKO;
            case HERALD -> Speaker.FIRE_HERALD;
            case GUARD -> Speaker.FIRE_GUARD;
        };
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            Talk.begin(server, this, speaker());
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.getEntity() instanceof Player p && p.isCreative()) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kind", kind().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            kind(Kind.valueOf(tag.getString("Kind")));
        } catch (IllegalArgumentException e) {
            kind(Kind.ZUKO);
        }
    }
}
