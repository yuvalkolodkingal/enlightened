package com.kolodkingal.enlightened;

import com.kolodkingal.enlightened.network.ModNetwork;
import com.kolodkingal.enlightened.registry.ModAttachments;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.registry.ModCreativeTab;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.registry.ModItems;
import com.kolodkingal.enlightened.registry.ModParticles;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Enlightened: become a bender of the Avatar world (earth, air, water or fire), learn the sub-bending arts and choose
 * the abilities you take into battle.
 */
@Mod(Enlightened.MODID)
public class Enlightened {
    public static final String MODID = "enlightened";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Enlightened(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.registerItems();
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        com.kolodkingal.enlightened.world.ModStructures.TYPES.register(modBus);
        com.kolodkingal.enlightened.world.ModStructures.PIECES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModEntities::createAttributes);
        modBus.addListener(ModEntities::registerSpawnPlacements);
        container.registerConfig(ModConfig.Type.SERVER, EnlightenedConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
