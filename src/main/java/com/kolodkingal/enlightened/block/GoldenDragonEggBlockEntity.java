package com.kolodkingal.enlightened.block;

import com.kolodkingal.enlightened.entity.DragonEntity;
import com.kolodkingal.enlightened.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** What a golden dragon egg keeps: when it was laid and by whom; it hatches {@link #HATCH} ticks after. */
public class GoldenDragonEggBlockEntity extends BlockEntity {
    /** Ten minutes. */
    public static final int HATCH = 12000;

    /** When it was laid (game time; {@link Long#MIN_VALUE} until it is). */
    private long laidAt = Long.MIN_VALUE;
    @Nullable
    private UUID owner;

    public GoldenDragonEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GOLDEN_DRAGON_EGG_ENTITY.get(), pos, state);
    }

    public void laid(long now, @Nullable UUID owner) {
        laidAt = now;
        this.owner = owner;
        setChanged();
    }

    public long laidAt() {
        return laidAt;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GoldenDragonEggBlockEntity egg) {
        long now = level.getGameTime();
        if (egg.laidAt == Long.MIN_VALUE) {
            egg.laid(now, null);
        }
        long age = now - egg.laidAt;
        if (age >= HATCH - 600 && age % 40 == 0 && level instanceof ServerLevel server) {
            // It stirs: sparks and knocks in its last half minute.
            server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.25, 0.3, 0.25, 0.02);
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.6F);
        }
        if (age >= HATCH && level instanceof ServerLevel server) {
            egg.hatch(server);
        }
    }

    /** The egg cracks and the pup comes out. */
    public void hatch(ServerLevel level) {
        BlockPos pos = getBlockPos();
        Player p = owner == null ? level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 16.0, false) : level.getPlayerByUUID(owner);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.NEUTRAL, 0.6F, 1.9F);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.05);
        DragonEntity pup = DragonEntity.hatch(level, pos, p);
        if (pup == null) {
            // Owner unknown and nobody near: the pup is wild, but still comes out.
            return;
        }
        if (p != null && p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) {
            p.sendSystemMessage(Component.translatable("message.enlightened.dragon.hatched",
                    Component.translatable("message.enlightened.dragon.colour." + pup.colour().id)).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LaidAt", laidAt);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        laidAt = tag.contains("LaidAt") ? tag.getLong("LaidAt") : Long.MIN_VALUE;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
