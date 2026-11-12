package com.kolodkingal.enlightened.mixin;

import com.kolodkingal.enlightened.item.BackSlot;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In creative mode the client says itself what goes in a slot, and the server takes it only for the game's own 45
 * inventory slots: the back slot (one past them) is taken here too, or what was put there in creative was never really
 * on the player's back. (At the end, so on the server thread and after the game's own checks on the item.)
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeBackSlotMixin {
    @Inject(method = "handleSetCreativeModeSlot", at = @At("TAIL"))
    private void enlightened$backSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        int index = packet.slotNum();
        if (!player.hasInfiniteMaterials() || index < 0 || index >= player.inventoryMenu.slots.size()
                || !(player.inventoryMenu.getSlot(index) instanceof BackSlot slot)) {
            return;
        }
        ItemStack stack = packet.itemStack();
        if (stack.isEmpty() || stack.getCount() <= stack.getMaxStackSize()) {
            slot.setByPlayer(stack.copyWithCount(Math.min(stack.getCount(), 1)));
            player.inventoryMenu.broadcastChanges();
        }
    }
}
