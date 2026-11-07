package com.kolodkingal.enlightened.item;

import com.kolodkingal.enlightened.entity.IrohEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Iroh's tsungi horn (1.0.23), bought from him for 50 gold: blow it where you want your tea shop, and Iroh comes flying
 * down on jets of fire to sell his tea there. Blown again elsewhere, he moves there. Once a minute.
 */
public class TsungiHornItem extends Item {
    public static final int COOLDOWN = 1200;

    public TsungiHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.PLAYERS, 1.4F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        if (player instanceof ServerPlayer server) {
            IrohEntity.arrive(server);
            server.displayClientMessage(Component.translatable("message.enlightened.iroh.coming").withStyle(ChatFormatting.GOLD), true);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enlightened.tsungi_horn.desc").withStyle(ChatFormatting.GRAY));
    }
}
