package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.air.Projection;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.UUID;

/**
 * The body an airbender leaves behind in meditation while his spirit looks about (Spirit Projection). It sits still
 * with his skin, can be hurt, and whatever hurts it calls the spirit back at once (and the blow lands on him). When
 * the spirit returns, the body is gone.
 */
public class MeditatingBodyEntity extends LivingEntity {
    private static final EntityDataAccessor<String> DATA_OWNER_NAME = SynchedEntityData.defineId(MeditatingBodyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_OWNER_UUID = SynchedEntityData.defineId(MeditatingBodyEntity.class, EntityDataSerializers.STRING);

    public MeditatingBodyEntity(EntityType<? extends MeditatingBodyEntity> type, Level level) {
        super(type, level);
        setNoGravity(false);
    }

    public MeditatingBodyEntity(Level level, ServerPlayer owner) {
        this(ModEntities.MEDITATING_BODY.get(), level);
        entityData.set(DATA_OWNER_NAME, owner.getGameProfile().getName());
        entityData.set(DATA_OWNER_UUID, owner.getUUID().toString());
        setPos(owner.getX(), owner.getY(), owner.getZ());
        setYRot(owner.getYRot());
        setYHeadRot(owner.getYHeadRot());
        yBodyRot = owner.yBodyRot;
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0).add(NeoForgeMod.CREATIVE_FLIGHT, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_OWNER_UUID, "");
    }

    public String ownerName() {
        return entityData.get(DATA_OWNER_NAME);
    }

    @Nullable
    public UUID ownerUuid() {
        String s = entityData.get(DATA_OWNER_UUID);
        try {
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        setAirSupply(getMaxAirSupply());
        if (level() instanceof ServerLevel level) {
            if (tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 1, 0.3, 0.3, 0.3, 0.0);
            }
            // A body without its spirit on the server (a crash, a logout) vanishes.
            UUID owner = ownerUuid();
            if (tickCount % 20 == 0 && (owner == null || !Projection.isProjecting(level.getServer(), owner, this))) {
                discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source)) {
            return false;
        }
        UUID owner = ownerUuid();
        if (owner != null) {
            Projection.onBodyHurt(((ServerLevel) level()).getServer(), owner, source, amount);
        }
        return true;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_OWNER_NAME, tag.getString("owner_name"));
        entityData.set(DATA_OWNER_UUID, tag.getString("owner_uuid"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("owner_name", ownerName());
        tag.putString("owner_uuid", entityData.get(DATA_OWNER_UUID));
    }
}
