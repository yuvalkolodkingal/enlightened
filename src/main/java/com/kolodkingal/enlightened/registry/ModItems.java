package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.item.BendingScrollItem;
import com.kolodkingal.enlightened.item.GliderItem;
import com.kolodkingal.enlightened.item.WaterSkinItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Enlightened.MODID);

    /** The four scrolls: read one and become a bender of its element (then it opens the Bending Scroll screen). */
    public static final DeferredItem<BendingScrollItem> EARTH_SCROLL = scroll("earth_scroll", Element.EARTH);
    public static final DeferredItem<BendingScrollItem> AIR_SCROLL = scroll("air_scroll", Element.AIR);
    public static final DeferredItem<BendingScrollItem> WATER_SCROLL = scroll("water_scroll", Element.WATER);
    public static final DeferredItem<BendingScrollItem> FIRE_SCROLL = scroll("fire_scroll", Element.FIRE);
    /** The four scrolls bound round a star: whoever reads it becomes the Avatar, a bender of all four. */
    public static final DeferredItem<BendingScrollItem> AVATAR_SCROLL = scroll("avatar_scroll", Element.AVATAR);

    /** The waterbender's water skin: filled at any water, it carries ten uses of water. */
    public static final DeferredItem<WaterSkinItem> WATER_SKIN = ITEMS.register("water_skin",
            () -> new WaterSkinItem(props("water_skin").stacksTo(1).durability(WaterSkinItem.CHARGES).rarity(Rarity.UNCOMMON)));

    /** The airbender's glider: held, it lets him glide; dyed in a crafting table, it is any colour. */
    public static final DeferredItem<GliderItem> AIR_GLIDER = ITEMS.register("air_glider",
            () -> new GliderItem(props("air_glider").stacksTo(1).rarity(Rarity.RARE)));

    /** The flying lemur's spawn egg (cream and dark grey, the lemur's colours). */
    public static final DeferredItem<Item> LEMUR_SPAWN_EGG = egg("flying_lemur_spawn_egg", ModEntities.LEMUR, 0xEDE3C8, 0x4A4A4A);

    /** The sky bison's spawn egg (white with the brown of its arrow). */
    public static final DeferredItem<Item> SKY_BISON_SPAWN_EGG = egg("sky_bison_spawn_egg", ModEntities.SKY_BISON, 0xEEEADE, 0x7A5032);

    public static final DeferredItem<Item> CITY_GUARD_SPAWN_EGG = egg("city_guard_spawn_egg", ModEntities.CITY_GUARD, 0x4A6E3A, 0xC8A050);

    /** The Seven Havens brigade armor trim: set in lapis, gold, redstone or coal it is the water, air, fire or earth brigade's uniform; in netherite the Avatar's, all four. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.armortrim.TrimPattern> BRIGADE_TRIM =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.TRIM_PATTERN, Enlightened.id("brigade"));
    public static final DeferredItem<net.minecraft.world.item.SmithingTemplateItem> BRIGADE_TRIM_TEMPLATE = ITEMS.register("brigade_armor_trim_smithing_template",
            //#if >=1.21.4
            //$$ () -> net.minecraft.world.item.SmithingTemplateItem.createArmorTrimTemplate(props("brigade_armor_trim_smithing_template")));
            //#else
            () -> net.minecraft.world.item.SmithingTemplateItem.createArmorTrimTemplate(BRIGADE_TRIM));
            //#endif

    /** The Eternal Flame of the Sun Warriors, carried up to Ran and Shaw: it goes out after seven minutes. */
    public static final DeferredItem<com.kolodkingal.enlightened.item.EternalFlameItem> ETERNAL_FLAME = ITEMS.register("eternal_flame",
            () -> new com.kolodkingal.enlightened.item.EternalFlameItem(props("eternal_flame").stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<Item> SUN_WARRIOR_SPAWN_EGG = egg("sun_warrior_spawn_egg", ModEntities.SUN_WARRIOR, 0xE8DCC0, 0xB0402A);

    public static final DeferredItem<Item> DARK_SPIRIT_SPAWN_EGG = egg("dark_spirit_spawn_egg", ModEntities.DARK_SPIRIT, 0x2A1838, 0xF0D040);

    /** A dragon's spawn egg: a pup, wild, of any colour. */
    public static final DeferredItem<Item> DRAGON_SPAWN_EGG = egg("dragon_spawn_egg", ModEntities.DRAGON, 0xB82A22, 0xF0E6D0);

    /** Iroh's teas (1.0.23). */
    public static final DeferredItem<com.kolodkingal.enlightened.item.TeaItem> JASMINE_TEA = ITEMS.register("jasmine_tea",
            () -> new com.kolodkingal.enlightened.item.TeaItem(tea(props("jasmine_tea").stacksTo(16), 3, 0.8F,
                    () -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 0))));
    public static final DeferredItem<com.kolodkingal.enlightened.item.TeaItem> GINSENG_TEA = ITEMS.register("ginseng_tea",
            () -> new com.kolodkingal.enlightened.item.TeaItem(tea(props("ginseng_tea").stacksTo(16), 3, 0.8F,
                    () -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 900, 0),
                    () -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 900, 0))));
    public static final DeferredItem<com.kolodkingal.enlightened.item.TeaItem> DRAGON_TEA = ITEMS.register("dragon_tea",
            () -> new com.kolodkingal.enlightened.item.TeaItem(tea(props("dragon_tea").stacksTo(16).rarity(Rarity.UNCOMMON), 4, 1.0F,
                    () -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 2400, 0),
                    () -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 2400, 0))));
    /** Iroh's tsungi horn: blown at your home, he comes to sell tea there. */
    public static final DeferredItem<com.kolodkingal.enlightened.item.TsungiHornItem> TSUNGI_HORN = ITEMS.register("tsungi_horn",
            () -> new com.kolodkingal.enlightened.item.TsungiHornItem(props("tsungi_horn").stacksTo(1).rarity(Rarity.RARE)));

    /** Item properties carrying the item's id (from 1.21.2 an item needs its id before it is made). */
    public static Item.Properties props(String name) {
        //#if >=1.21.4
        //$$ return new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, Enlightened.id(name)));
        //#else
        return new Item.Properties();
        //#endif
    }

    /** A block's item's properties: its id, and the block's name. */
    public static Item.Properties blockProps(String name) {
        //#if >=1.21.4
        //$$ return props(name).useBlockDescriptionPrefix();
        //#else
        return new Item.Properties();
        //#endif
    }

    /** A spawn egg of a creature, in its two colours. */
    private static DeferredItem<Item> egg(String name, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
                                          int base, int spots) {
        //#if >=1.21.4
        //$$ // (the colours are in the item's model from 1.21.4: assets/enlightened/items/<name>.json)
        //$$ return ITEMS.register(name, () -> new net.minecraft.world.item.SpawnEggItem(type.get(), props(name)));
        //#else
        return ITEMS.register(name, () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(type, base, spots, props(name)));
        //#endif
    }

    /** A tea's properties: always drinkable food that gives its effects. */
    @SafeVarargs
    private static Item.Properties tea(Item.Properties properties, int nutrition, float saturation,
                                       java.util.function.Supplier<net.minecraft.world.effect.MobEffectInstance>... effects) {
        net.minecraft.world.food.FoodProperties.Builder food = new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(nutrition).saturationModifier(saturation).alwaysEdible();
        //#if >=1.21.4
        //$$ java.util.List<net.minecraft.world.effect.MobEffectInstance> list = new java.util.ArrayList<>();
        //$$ for (var effect : effects) {
        //$$     list.add(effect.get());
        //$$ }
        //$$ return properties.food(food.build(), net.minecraft.world.item.component.Consumables.defaultDrink()
        //$$         .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(list)).build());
        //#else
        for (var effect : effects) {
            food.effect(effect, 1.0F);
        }
        return properties.food(food.build());
        //#endif
    }

    private static DeferredItem<BendingScrollItem> scroll(String name, Element element) {
        return ITEMS.register(name, () -> new BendingScrollItem(element, props(name).stacksTo(1).rarity(Rarity.EPIC)));
    }

    private ModItems() {
    }
}
