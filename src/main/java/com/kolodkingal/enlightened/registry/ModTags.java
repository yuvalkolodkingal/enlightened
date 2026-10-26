package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** The mod's tags (data/enlightened/tags). */
public final class ModTags {
    /** Every damage of bending: a kill with it earns the bender experience. */
    public static final TagKey<DamageType> BENDING = TagKey.create(Registries.DAMAGE_TYPE, Enlightened.id("bending"));
    /** Fire bending (fire protection and a firebender's skin soften it). */
    public static final TagKey<DamageType> FIRE_BENDING = TagKey.create(Registries.DAMAGE_TYPE, Enlightened.id("fire_bending"));

    /** The blocks an earthbender bends: stone, dirt, gravel, clay, ores... (never ore blocks of value, never bedrock). */
    public static final TagKey<Block> EARTH = BlockTags.create(Enlightened.id("earth"));
    /** The blocks of sand bending: sand, red sand, sandstone, gravel. */
    public static final TagKey<Block> SAND = BlockTags.create(Enlightened.id("sand"));
    /** The blocks metalbending moves: iron, gold, copper, netherite blocks, anvils, chains, bars, rails... */
    public static final TagKey<Block> METAL = BlockTags.create(Enlightened.id("metal"));
    /** Metal devices a metalbender can wreck: furnaces, pistons, hoppers, rails, iron doors, cauldrons, lightning rods. */
    public static final TagKey<Block> METAL_DEVICES = BlockTags.create(Enlightened.id("metal_devices"));
    /** Ores metal pulling draws metal out of. */
    public static final TagKey<Block> METAL_ORES = BlockTags.create(Enlightened.id("metal_ores"));
    /** Plants a master waterbender takes the water out of (they wither). */
    public static final TagKey<Block> WATER_PLANTS = BlockTags.create(Enlightened.id("water_plants"));

    /** Items that are metal (a metal pull takes them from the ground). */
    public static final TagKey<Item> METAL_ITEMS = ItemTags.create(Enlightened.id("metal_items"));

    private ModTags() {
    }
}
