package com.kolodkingal.enlightened.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * The calls whose form differs between the Minecraft versions the jar supports (docs/MULTIVERSION.md), in one place:
 * the rest of the mod calls these, and only this file has version blocks for them.
 */
public final class Compat {
    private Compat() {
    }

    /** Moves a player to a place in a level (another dimension too), facing yaw and pitch. */
    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        //#if >=1.21.4
        //$$ player.teleportTo(level, x, y, z, java.util.Set.of(), yaw, pitch, true);
        //#else
        player.teleportTo(level, x, y, z, yaw, pitch);
        //#endif
    }

    /** Sends an entity's synced attachment to the players who see it (after a change inside its value). */
    public static void sync(Entity holder, net.neoforged.neoforge.attachment.AttachmentType<?> type) {
        //#if >=1.21.4
        //$$ AttachSync.send(holder, type);
        //#else
        holder.syncData(type);
        //#endif
    }
}
