package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * King Bumi of Omashu, the mad genius, on his throne in the hall under the summit of his city. He never leaves it;
 * nothing harms him. Spoken to (right-click), he laughs, offers lettuce, sets riddles and gives the advice of an old
 * earthbender, a little more each time.
 */
public class KingBumiEntity extends PathfinderMob {
    /** How many things the king has to say. */
    public static final int LINES = 8;

    private int talks;

    public KingBumiEntity(EntityType<? extends KingBumiEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            talk(server);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** What the king says to someone who speaks to him (the translation key of his line). */
    public String line(ServerPlayer player) {
        int n = talks++;
        if (n == 0 && Benders.get(player).element() == Element.AVATAR) {
            return "bumi.enlightened.avatar";
        }
        return "bumi.enlightened.line." + Math.floorMod(n, LINES);
    }

    public void talk(ServerPlayer player) {
        getLookControl().setLookAt(player);
        playSound(SoundEvents.VILLAGER_YES, 0.7F, 0.7F);
        player.sendSystemMessage(Component.translatable("bumi.enlightened.says", Component.translatable(line(player)))
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // The king is not to be moved: only the void, or a Creative player, can.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.getEntity() instanceof Player p && p.isCreative()) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Talks", talks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        talks = tag.getInt("Talks");
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }
}
