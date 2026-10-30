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
     * A red cube of the Anomaly, in Ellora: it cannot be broken, and whoever tries wakes the Anomaly itself.
     */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> ANOMALY_BLOCK = BLOCKS.register("anomaly_block",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED)
                    .strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 10).emissiveRendering((s, l, p) -> true)
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)));

    public static void registerItems() {
        ModItems.ITEMS.register("meditation_point", () -> new BlockItem(MEDITATION_POINT.get(), new Item.Properties()));
        ModItems.ITEMS.register("anomaly_block", () -> new BlockItem(ANOMALY_BLOCK.get(), new Item.Properties()));
    }

    private ModBlocks() {
    }
}
