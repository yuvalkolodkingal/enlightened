package com.kolodkingal.enlightened.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The server's count of the ticks a player has floated with nothing under him: past 80 it kicks him for flying. A
 * bender held up by his spout, his glider or his scooter is not flying, so the count is reset while he rides.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
    @Accessor("aboveGroundTickCount")
    void enlightened$setAboveGroundTickCount(int ticks);
}
