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
            () -> new WaterSkinItem(new Item.Properties().stacksTo(1).durability(WaterSkinItem.CHARGES).rarity(Rarity.UNCOMMON)));

    /** The airbender's glider: held, it lets him glide; dyed in a crafting table, it is any colour. */
    public static final DeferredItem<GliderItem> AIR_GLIDER = ITEMS.register("air_glider",
            () -> new GliderItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** The flying lemur's spawn egg (cream and dark grey, the lemur's colours). */
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> LEMUR_SPAWN_EGG = ITEMS.register("flying_lemur_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.LEMUR, 0xEDE3C8, 0x4A4A4A, new Item.Properties()));

    private static DeferredItem<BendingScrollItem> scroll(String name, Element element) {
        return ITEMS.register(name, () -> new BendingScrollItem(element, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    }

    private ModItems() {
    }
}
