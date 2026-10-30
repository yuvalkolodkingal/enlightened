package com.kolodkingal.enlightened.item;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The back slot: one more slot in the player's inventory, beside the off hand, for whatever he wears on his back over
 * his armour (a glider, a sword, a banner, anything). What it holds is saved and synced to everyone who can see him,
 * and drawn on his back; a glider there may be flown as one in hand.
 */
public class BackSlot extends Slot {
    /** Where it sits in the survival inventory (beside the player, over the off hand) and in the creative inventory tab. */
    public static final int X = 77;
    public static final int Y = 44;
    public static final int CREATIVE_X = 127;
    public static final int CREATIVE_Y = 20;
    public static final ResourceLocation EMPTY_ICON = Enlightened.id("item/empty_slot_back");

    public BackSlot(Player owner) {
        super(new Back(owner), 0, X, Y);
        setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_ICON);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** What the player carries on his back. */
    public static ItemStack get(Player player) {
        return player.getData(ModAttachments.BACK);
    }

    public static void set(Player player, ItemStack stack) {
        player.setData(ModAttachments.BACK, stack);
        if (player instanceof ServerPlayer server) {
            server.syncData(ModAttachments.BACK);
        }
    }

    /** The one-slot container behind the slot: the player's back. */
    public static final class Back implements Container {
        private final Player owner;

        Back(Player owner) {
            this.owner = owner;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return get(owner).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return get(owner);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = get(owner);
            if (stack.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = stack.copyWithCount(Math.min(amount, stack.getCount()));
            ItemStack rest = stack.copyWithCount(stack.getCount() - taken.getCount());
            set(owner, rest.isEmpty() ? ItemStack.EMPTY : rest);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = get(owner);
            set(owner, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            set(owner, stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return player == owner;
        }

        @Override
        public void clearContent() {
            set(owner, ItemStack.EMPTY);
        }
    }
}
