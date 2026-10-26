package com.kolodkingal.enlightened.bending;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Whether bending may change a block of the world: never in spawn protection, never for a player in Adventure or
 * Spectator mode, never a block that cannot be broken (bedrock, barriers), never outside the world border, and only
 * where the claims (the mods that listen to the block break and place events) let the player.
 */
public final class BlockRights {

    /** Whether the game's own rules let the player change the cell. */
    public static boolean vanillaRights(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE || player.isSpectator()) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(pos) || level.getServer().isUnderSpawnProtection(level, pos, player)) {
            return false;
        }
        return level.mayInteract(player, pos) && level.isLoaded(pos);
    }

    /** Whether the player may break the block at {@code pos} (the game's rules, then the claims). */
    public static boolean mayBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!vanillaRights(player, level, pos)) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    /** Whether the player may put a block at {@code pos} (an air, water or replaceable cell, and the claims agree). */
    public static boolean mayPlace(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!vanillaRights(player, level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced() && !state.isAir()) {
            return false;
        }
        return !EventHooks.onBlockPlace(player, net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos),
                net.minecraft.core.Direction.UP);
    }

    private BlockRights() {
    }
}
