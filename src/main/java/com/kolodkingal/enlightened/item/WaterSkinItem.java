package com.kolodkingal.enlightened.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * The waterbender's water skin (a cow's hide and some thread): carries {@link #CHARGES} uses of water, so he can bend
 * far from any lake. Right-click water to fill it; it also fills slowly in the rain. Its durability bar is the water
 * left.
 */
public class WaterSkinItem extends Item {
    public static final int CHARGES = 10;

    public WaterSkinItem(Properties properties) {
        super(properties);
    }

    /** Uses of water left in the skin. */
    public static int charges(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    public static void setCharges(ItemStack stack, int charges) {
        stack.setDamageValue(Math.max(0, stack.getMaxDamage() - Math.max(0, Math.min(stack.getMaxDamage(), charges))));
    }

    /** Takes one use of water from the skin, if it has one. */
    public static boolean drain(ItemStack stack) {
        int left = charges(stack);
        if (left <= 0) {
            return false;
        }
        setCharges(stack, left - 1);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            if (level.getFluidState(pos).is(Fluids.WATER)) {
                if (!level.isClientSide) {
                    setCharges(stack, CHARGES);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("message.enlightened.water_skin.filled"), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        if (player.isInWater()) {
            if (!level.isClientSide) {
                setCharges(stack, CHARGES);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // In the rain it fills slowly: one use every 15 seconds under the open sky.
        if (!level.isClientSide && level.getGameTime() % 300 == 0 && charges(stack) < CHARGES
                && level.isRaining() && level.canSeeSky(entity.blockPosition())) {
            setCharges(stack, charges(stack) + 1);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x2E86DE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enlightened.water_skin.charges", charges(stack), CHARGES).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enlightened.water_skin.desc").withStyle(ChatFormatting.GRAY));
    }
}
