package com.kolodkingal.enlightened.item;

import com.kolodkingal.enlightened.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Eternal Flame of the Sun Warriors, the first fire, kept alight since the dragons taught it: the chief gives a
 * small flame of it, in a stone bowl, to whoever would be tested by Ran and Shaw. It goes out by itself after seven
 * minutes (and at once in water), and then it is gone; carried alight to the summit, it shows the masters you are
 * worthy. Its bar shows the time left.
 */
public class EternalFlameItem extends Item {
    /** How long it burns (ticks): seven minutes. */
    public static final int BURN = 7 * 60 * 20;

    public EternalFlameItem(Properties properties) {
        super(properties);
    }

    /** A flame lit now. */
    public static ItemStack lit(long now) {
        ItemStack stack = new ItemStack(ModItems.ETERNAL_FLAME.get());
        CompoundTag tag = new CompoundTag();
        tag.putLong("LitAt", now);
        tag.putInt("Left", BURN / 20);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** Ticks it has left to burn (0 when out; a flame never lit, from the creative tab, is lit when first carried). */
    public static long left(ItemStack stack, long now) {
        CompoundTag tag = tag(stack);
        if (!tag.contains("LitAt")) {
            return BURN;
        }
        return Math.max(0, BURN - (now - tag.getLong("LitAt")));
    }

    /** Whether the player carries a flame still alight. */
    public static boolean carries(Player player) {
        long now = player.level().getGameTime();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.ETERNAL_FLAME.get()) && left(stack, now) > 0) {
                return true;
            }
        }
        return player.getOffhandItem().is(ModItems.ETERNAL_FLAME.get()) && left(player.getOffhandItem(), now) > 0;
    }

    /** Takes one flame still alight from the player (given to the masters); whether there was one. */
    public static boolean give(Player player) {
        long now = player.level().getGameTime();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.ETERNAL_FLAME.get()) && left(stack, now) > 0) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    //#if >=1.21.5
    //$$ public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.EquipmentSlot slot) {
    //$$     // (1.21.5 ticks a held item on the server only: its flame is sent to the clients)
    //$$     if ((slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND || slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND) && level.random.nextInt(3) == 0) {
    //$$         level.sendParticles(ParticleTypes.FLAME, entity.getX() + (level.random.nextDouble() - 0.5) * 0.6, entity.getY() + 1.1,
    //$$                 entity.getZ() + (level.random.nextDouble() - 0.5) * 0.6, 0, 0, 1, 0, 0.03);
    //$$     }
    //$$     boolean selected = slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND;
    //$$     if (level.isClientSide) {
    //#else
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
    //#endif
            if ((selected || (entity instanceof Player p && p.getOffhandItem() == stack)) && level.random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME, entity.getX() + (level.random.nextDouble() - 0.5) * 0.6, entity.getY() + 1.1,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.6, 0, 0.03, 0);
            }
            return;
        }
        long now = level.getGameTime();
        CompoundTag tag = tag(stack);
        if (!tag.contains("LitAt")) {
            tag.putLong("LitAt", now);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        long left = left(stack, now);
        if (left <= 0 || entity.isInWater()) {
            stack.setCount(0);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.0F);
            if (entity instanceof ServerPlayer p) {
                p.displayClientMessage(Component.translatable("message.enlightened.flame.out").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        int secs = (int) ((left + 19) / 20);
        if (tag.getInt("Left") != secs && now % 20 == 0) {
            tag.putInt("Left", secs);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide && (left(stack, level.getGameTime()) <= 0 || entity.isInWater())) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6F, 1.0F);
            entity.discard();
            return true;
        }
        return false;
    }

    private static int shownLeft(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.contains("Left") ? tag.getInt("Left") : BURN / 20;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * shownLeft(stack) / (BURN / 20.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF8A1E;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int s = shownLeft(stack);
        tooltip.add(Component.translatable("item.enlightened.eternal_flame.left", s / 60, String.format("%02d", s % 60)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enlightened.eternal_flame.hint").withStyle(ChatFormatting.GRAY));
    }
}
