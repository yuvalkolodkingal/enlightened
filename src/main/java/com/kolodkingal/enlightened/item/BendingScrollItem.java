package com.kolodkingal.enlightened.item;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.client.ClientScroll;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A scroll of one bending art. Read (used) by a player with no bending, it makes him a bender of its element and is
 * used up. Read by a bender of the same element, it opens the Bending Scroll screen (the abilities, the points, the
 * loadout), the same as the K key. A bender of another element cannot read it.
 */
public class BendingScrollItem extends Item {
    public final Element element;

    public BendingScrollItem(Element element, Properties properties) {
        super(properties);
        this.element = element;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BendingData data = Benders.get(player);
        if (data.element() == Element.NONE) {
            if (player instanceof ServerPlayer server) {
                Benders.awaken(server, element);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (data.element() == element) {
            if (level.isClientSide) {
                ClientScroll.open();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.enlightened.scroll.other_element",
                    data.element().displayName()).withStyle(ChatFormatting.RED), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enlightened.scroll.desc", element.displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enlightened.scroll.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
