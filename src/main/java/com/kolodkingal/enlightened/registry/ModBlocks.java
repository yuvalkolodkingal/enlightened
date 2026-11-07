package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.block.MeditationPointBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Enlightened.MODID);

    /** The Meditation Point of the temples (and of a crafting table): meditate on it to open the chakras faster. */
    public static final DeferredBlock<MeditationPointBlock> MEDITATION_POINT = BLOCKS.register("meditation_point",
            () -> new MeditationPointBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).strength(1.5F).noOcclusion()));

    /** Azula's blue fire where it lands: looks like soul fire, stands where fire does, burns out by itself. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.BlueFireBlock> BLUE_FIRE = BLOCKS.register("blue_fire",
            () -> new com.kolodkingal.enlightened.block.BlueFireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_FIRE)));

    /** The light inside a spirit portal: walk into it to cross to the Spirit World and back. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.SpiritPortalBlock> SPIRIT_PORTAL = BLOCKS.register("spirit_portal",
            () -> new com.kolodkingal.enlightened.block.SpiritPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL)
                    .lightLevel(s -> 12).noLootTable()));

    /**
     * A red cube of the Anomaly, in Ellora: a small Anomaly of pure energy, as hard as a block to strike; it cannot be
     * broken, and whoever tries wakes the Anomaly itself.
     */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.AnomalyBlock> ANOMALY_BLOCK = BLOCKS.register("anomaly_block",
            () -> new com.kolodkingal.enlightened.block.AnomalyBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                    .strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 10).noOcclusion()
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)));

    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Enlightened.MODID);

    /** What draws a red cube as a small Anomaly. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.kolodkingal.enlightened.block.AnomalyCubeBlockEntity>> ANOMALY_CUBE =
            BLOCK_ENTITIES.register("anomaly_cube", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(com.kolodkingal.enlightened.block.AnomalyCubeBlockEntity::new, ANOMALY_BLOCK.get()).build(null));

    /** The golden dragon egg of Ran and Shaw: set down, a dragon pup hatches from it in ten minutes. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.GoldenDragonEggBlock> GOLDEN_DRAGON_EGG = BLOCKS.register("golden_dragon_egg",
            () -> new com.kolodkingal.enlightened.block.GoldenDragonEggBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.GOLD)
                    .strength(1.5F, 1200.0F).lightLevel(s -> 7).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));

    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.kolodkingal.enlightened.block.GoldenDragonEggBlockEntity>> GOLDEN_DRAGON_EGG_ENTITY =
            BLOCK_ENTITIES.register("golden_dragon_egg", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(com.kolodkingal.enlightened.block.GoldenDragonEggBlockEntity::new, GOLDEN_DRAGON_EGG.get()).build(null));

    public static final net.neoforged.neoforge.registries.DeferredItem<BlockItem> GOLDEN_DRAGON_EGG_ITEM = ModItems.ITEMS.register("golden_dragon_egg",
            () -> new BlockItem(GOLDEN_DRAGON_EGG.get(), new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    /** A tea house stool: sit on it. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.TeaStoolBlock> TEA_STOOL = BLOCKS.register("tea_stool",
            () -> new com.kolodkingal.enlightened.block.TeaStoolBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(1.5F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.WOOD).ignitedByLava()));

    public static void registerItems() {
        ModItems.ITEMS.register("tea_stool", () -> new BlockItem(TEA_STOOL.get(), new Item.Properties()));
        ModItems.ITEMS.register("meditation_point", () -> new BlockItem(MEDITATION_POINT.get(), new Item.Properties()));
        ModItems.ITEMS.register("anomaly_block", () -> new BlockItem(ANOMALY_BLOCK.get(), new Item.Properties()));
    }

    private ModBlocks() {
    }
}
