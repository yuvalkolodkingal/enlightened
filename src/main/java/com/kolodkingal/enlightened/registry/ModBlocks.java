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
            () -> new MeditationPointBlock(id("meditation_point", BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE)).strength(1.5F).noOcclusion()));

    /** Azula's blue fire where it lands: looks like soul fire, stands where fire does, burns out by itself. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.BlueFireBlock> BLUE_FIRE = BLOCKS.register("blue_fire",
            () -> new com.kolodkingal.enlightened.block.BlueFireBlock(id("blue_fire", BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_FIRE))));

    /** The light inside a spirit portal: walk into it to cross to the Spirit World and back. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.SpiritPortalBlock> SPIRIT_PORTAL = BLOCKS.register("spirit_portal",
            () -> new com.kolodkingal.enlightened.block.SpiritPortalBlock(id("spirit_portal", BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL))
                    .lightLevel(s -> 12).noLootTable()));

    /**
     * A red cube of the Anomaly, in Ellora: a small Anomaly of pure energy, as hard as a block to strike; it cannot be
     * broken, and whoever tries wakes the Anomaly itself.
     */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.AnomalyBlock> ANOMALY_BLOCK = BLOCKS.register("anomaly_block",
            () -> new com.kolodkingal.enlightened.block.AnomalyBlock(id("anomaly_block", BlockBehaviour.Properties.of()).mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                    .strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 10).noOcclusion()
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)));

    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Enlightened.MODID);

    /** What draws a red cube as a small Anomaly. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.kolodkingal.enlightened.block.AnomalyCubeBlockEntity>> ANOMALY_CUBE =
            BLOCK_ENTITIES.register("anomaly_cube", () -> blockEntity(com.kolodkingal.enlightened.block.AnomalyCubeBlockEntity::new, ANOMALY_BLOCK));

    /** The golden dragon egg of Ran and Shaw: set down, a dragon pup hatches from it in ten minutes. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.GoldenDragonEggBlock> GOLDEN_DRAGON_EGG = BLOCKS.register("golden_dragon_egg",
            () -> new com.kolodkingal.enlightened.block.GoldenDragonEggBlock(id("golden_dragon_egg", BlockBehaviour.Properties.of()).mapColor(net.minecraft.world.level.material.MapColor.GOLD)
                    .strength(1.5F, 1200.0F).lightLevel(s -> 7).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));

    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.kolodkingal.enlightened.block.GoldenDragonEggBlockEntity>> GOLDEN_DRAGON_EGG_ENTITY =
            BLOCK_ENTITIES.register("golden_dragon_egg", () -> blockEntity(com.kolodkingal.enlightened.block.GoldenDragonEggBlockEntity::new, GOLDEN_DRAGON_EGG));

    public static final net.neoforged.neoforge.registries.DeferredItem<BlockItem> GOLDEN_DRAGON_EGG_ITEM = ModItems.ITEMS.register("golden_dragon_egg",
            () -> new BlockItem(GOLDEN_DRAGON_EGG.get(), ModItems.blockProps("golden_dragon_egg").stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    /** A tea house stool: sit on it. */
    public static final DeferredBlock<com.kolodkingal.enlightened.block.TeaStoolBlock> TEA_STOOL = BLOCKS.register("tea_stool",
            () -> new com.kolodkingal.enlightened.block.TeaStoolBlock(id("tea_stool", BlockBehaviour.Properties.of()).mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(1.5F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.WOOD).ignitedByLava()));

    public static void registerItems() {
        ModItems.ITEMS.register("tea_stool", () -> new BlockItem(TEA_STOOL.get(), ModItems.blockProps("tea_stool")));
        ModItems.ITEMS.register("meditation_point", () -> new BlockItem(MEDITATION_POINT.get(), ModItems.blockProps("meditation_point")));
        ModItems.ITEMS.register("anomaly_block", () -> new BlockItem(ANOMALY_BLOCK.get(), ModItems.blockProps("anomaly_block")));
    }

    /** Block properties carrying the block's id (from 1.21.2 a block needs its id before it is made). */
    private static BlockBehaviour.Properties id(String name, BlockBehaviour.Properties properties) {
        //#if >=1.21.4
        //$$ return properties.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, Enlightened.id(name)));
        //#else
        return properties;
        //#endif
    }

    /** A block entity type for one block. */
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier<T> factory, java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block) {
        //#if >=1.21.4
        //$$ return new net.minecraft.world.level.block.entity.BlockEntityType<>(factory, block.get());
        //#else
        return net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(factory, block.get()).build(null);
        //#endif
    }

    private ModBlocks() {
    }
}
