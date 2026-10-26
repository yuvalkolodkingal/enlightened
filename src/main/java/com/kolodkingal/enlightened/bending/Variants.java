package com.kolodkingal.enlightened.bending;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every element's looks, in the order the Change Look key cycles through them. The first one is the default; the
 * last is always "your own skin".
 */
public final class Variants {
    private static final Map<Element, List<Variant>> BY_ELEMENT = new EnumMap<>(Element.class);

    static {
        // Earthbenders: Toph Beifong, King Bumi, Lin Beifong, Suyin Beifong, Ghazan (whose lava glows red) and Bolin.
        add(Element.EARTH,
                look("toph", Element.EARTH, "toph", true, Element.EARTH.color, Element.EARTH.glow),
                look("bumi", Element.EARTH, "bumi", false, 0x7FA64A, 0xD9C48A),
                look("lin", Element.EARTH, "lin", true, 0x5E6B4A, 0xB7B7A4),
                look("suyin", Element.EARTH, "suyin", true, 0x6E8B5A, 0xC9D6B0),
                look("ghazan", Element.EARTH, "ghazan", false, 0xD9531E, 0xFFB347),
                look("bolin", Element.EARTH, "bolin", false, 0x6B8E23, 0xC9A96E),
                own(Element.EARTH));
        // Airbenders: Tenzin, Zaheer, Jinora, Monk Gyatso, and Jae and Kartik of the Seven Havens.
        add(Element.AIR,
                look("tenzin", Element.AIR, "tenzin", false, Element.AIR.color, Element.AIR.glow),
                look("zaheer", Element.AIR, "zaheer", false, 0xB08C4A, 0xE6E6E6),
                look("jinora", Element.AIR, "jinora", true, 0xF2CC72, 0xE8F6FF),
                look("gyatso", Element.AIR, "gyatso", false, 0xE2B85A, 0xF5F5F5),
                look("jae", Element.AIR, "jae", false, 0xD9A84A, 0xDDF3FF),
                look("kartik", Element.AIR, "kartik", false, 0xC99A3F, 0xDDF3FF),
                own(Element.AIR));
        // Waterbenders: Katara, Amon (Noatak), Ming-Hua and Master Pakku in the robes of the White Lotus.
        add(Element.WATER,
                look("katara", Element.WATER, "katara", true, Element.WATER.color, Element.WATER.glow),
                look("amon", Element.WATER, "amon", false, 0x1F4E79, 0xC7D6E8),
                look("ming_hua", Element.WATER, "ming_hua", true, 0x2A6FAD, 0xBFE8FF),
                look("pakku", Element.WATER, "pakku", false, 0x3A7FC1, 0xE6F2FF),
                own(Element.WATER));
        // Firebenders: Zuko (after he joined Team Avatar), Uncle Iroh of the White Lotus, Azula (blue fire), Jeong
        // Jeong of the White Lotus and Fire Lord Ozai.
        add(Element.FIRE,
                look("zuko", Element.FIRE, "zuko", false, Element.FIRE.color, Element.FIRE.glow),
                look("iroh", Element.FIRE, "iroh", false, 0xD9772A, 0xFFE0A3),
                look("azula", Element.FIRE, "azula", true, 0x2F7BFF, 0xBFE0FF),
                look("jeong_jeong", Element.FIRE, "jeong_jeong", false, 0xC9451C, 0xFFD27A),
                look("ozai", Element.FIRE, "ozai", false, 0xB8200F, 0xFFC247),
                own(Element.FIRE));
    }

    private static Variant look(String id, Element element, String skin, boolean slim, int color, int glow) {
        return new Variant(id, element, Variant.suit(skin), slim, color, glow);
    }

    private static Variant own(Element element) {
        return new Variant(Variant.OWN, element, null, false, element.color, element.glow);
    }

    private static void add(Element element, Variant... looks) {
        BY_ELEMENT.put(element, List.of(looks));
    }

    public static List<Variant> of(Element element) {
        return BY_ELEMENT.getOrDefault(element, List.of());
    }

    /** The look with this id, or the element's default for an unknown id. */
    public static Variant get(Element element, String id) {
        List<Variant> looks = of(element);
        if (looks.isEmpty()) {
            return new Variant(Variant.OWN, element, null, false, element.color, element.glow);
        }
        for (Variant v : looks) {
            if (v.id().equals(id)) {
                return v;
            }
        }
        return looks.getFirst();
    }

    public static Variant current(BendingData data) {
        return get(data.element(), data.variant());
    }

    public static boolean hasChoice(Element element) {
        return of(element).size() > 1;
    }

    /** The look {@code step} places after (or before) the current one, round the list. */
    public static Variant step(Element element, String current, int step) {
        List<Variant> looks = of(element);
        int index = 0;
        for (int i = 0; i < looks.size(); i++) {
            if (looks.get(i).id().equals(current)) {
                index = i;
                break;
            }
        }
        int next = Math.floorMod(index + step, looks.size());
        return looks.get(next);
    }

    /** Whether the fire of this look is blue (Azula's). */
    public static boolean blueFire(BendingData data) {
        return data.element() == Element.FIRE && "azula".equals(data.variant());
    }

    private Variants() {
    }
}
