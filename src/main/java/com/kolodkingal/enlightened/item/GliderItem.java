package com.kolodkingal.enlightened.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.List;

/**
 * The airbender's glider staff. Held in either hand by an airbender, it opens as he falls and he glides on the air
 * (Air Jump sends him higher first). Dyed in a crafting table with any dye (it is in {@code minecraft:dyeable}), its
 * wings take the colour. In anyone else's hands it is a staff.
 */
public class GliderItem extends Item {
    public static final int DEFAULT_COLOR = 0xD9772A;

    public GliderItem(Properties properties) {
        super(properties);
    }

    public static int color(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, DEFAULT_COLOR);
    }

    /** Whether the player holds a glider in either hand. */
    public static boolean holds(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof GliderItem || entity.getOffhandItem().getItem() instanceof GliderItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enlightened.air_glider.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enlightened.air_glider.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
