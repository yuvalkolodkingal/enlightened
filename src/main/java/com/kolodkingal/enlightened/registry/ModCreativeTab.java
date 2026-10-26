package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Enlightened.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("enlightened", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.enlightened"))
            .icon(() -> new ItemStack(ModItems.EARTH_SCROLL.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.EARTH_SCROLL.get());
                output.accept(ModItems.AIR_SCROLL.get());
                output.accept(ModItems.WATER_SCROLL.get());
                output.accept(ModItems.FIRE_SCROLL.get());
                output.accept(ModItems.WATER_SKIN.get());
                output.accept(ModItems.AIR_GLIDER.get());
                output.accept(ModItems.LEMUR_SPAWN_EGG.get());
                output.accept(ModBlocks.MEDITATION_POINT.get());
            })
            .build());

    private ModCreativeTab() {
    }
}
