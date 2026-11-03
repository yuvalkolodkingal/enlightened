package com.kolodkingal.enlightened.bending;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Premade sets of abilities a newborn bender is given, one chosen by chance, so he begins with some powers and not
 * only his element's first art: a fighter's, a defender's, a traveller's set. Every art in them is open to any bender
 * of the element (no sub-bending), at the first level; the Avatar is given one set of each of the four.
 * Server option {@code general.starterKits}.
 */
public final class StarterKits {
    /** The sets of each element. */
    public static List<List<Ability>> kits(Element element) {
        return switch (element) {
            case EARTH -> List.of(
                    List.of(Ability.ROCK_LIFT, Ability.EARTH_PILLAR, Ability.SHOCKWAVE),
                    List.of(Ability.EARTH_WALL, Ability.EARTH_ARMOR, Ability.EARTH_GRAB),
                    List.of(Ability.CATAPULT, Ability.EARTH_TUNNEL, Ability.SEISMIC_SENSE),
                    List.of(Ability.MUD_WAVE, Ability.EARTH_PILLAR, Ability.EARTH_WALL));
            case AIR -> List.of(
                    List.of(Ability.AIR_BLADES, Ability.AIR_JUMP, Ability.AIR_SUCTION),
                    List.of(Ability.AIR_SHIELD, Ability.AIR_BURST, Ability.AIR_SPOUT),
                    List.of(Ability.AIR_SCOOTER, Ability.TORNADO, Ability.AIR_JUMP),
                    List.of(Ability.SPIRIT_PROJECTION, Ability.AIR_SHIELD, Ability.AIR_BLADES));
            case WATER -> List.of(
                    List.of(Ability.WATER_HEALING, Ability.WATER_SPOUT, Ability.WATER_SHIELD),
                    List.of(Ability.OCTOPUS_FORM, Ability.WATER_ARMS, Ability.PLANT_TETHER),
                    List.of(Ability.WATER_VORTEX, Ability.MUD_THROW, Ability.WATER_BUBBLE),
                    List.of(Ability.PLANT_GIANT, Ability.PLANT_TETHER, Ability.WATER_HEALING));
            case FIRE -> List.of(
                    List.of(Ability.FIRE_BALL, Ability.FIRE_FIST, Ability.FIRE_RING),
                    List.of(Ability.FIRE_WHIP, Ability.FIRE_SHIELD, Ability.FIRE_JET),
                    List.of(Ability.WALL_OF_FIRE, Ability.BLAZE, Ability.FIRE_HEALING),
                    List.of(Ability.FIRE_BALL, Ability.FIRE_JET, Ability.HEAT_CONTROL));
            default -> List.of();
        };
    }

    public static boolean on() {
        return !com.kolodkingal.enlightened.EnlightenedConfig.SPEC.isLoaded() || com.kolodkingal.enlightened.EnlightenedConfig.STARTER_KITS.get();
    }

    /** Gives a newborn bender a set (the Avatar one of each of the four), lays out his keys, and tells him. */
    public static void give(ServerPlayer p) {
        if (!on()) {
            return;
        }
        BendingData data = Benders.get(p);
        Element element = data.element();
        List<Element> from = element == Element.AVATAR ? Element.FOUR : List.of(element);
        RandomSource random = p.getRandom();
        MutableComponent names = Component.empty();
        boolean first = true;
        for (Element e : from) {
            List<List<Ability>> kits = kits(e);
            if (kits.isEmpty()) {
                continue;
            }
            for (Ability a : kits.get(random.nextInt(kits.size()))) {
                if (data.level(a) < 1) {
                    data.setLevel(a, 1);
                    if (a.kind == Ability.Kind.PASSIVE) {
                        data.setOn(a, true);
                    }
                    if (!first) {
                        names.append(", ");
                    }
                    names.append(a.displayName());
                    first = false;
                }
            }
        }
        if (first) {
            return;
        }
        Loadout.reset(data);
        BendingTicker.applyPassives(p, data);
        Benders.syncIfDirty(p);
        p.sendSystemMessage(Component.translatable("message.enlightened.starter_kit", names).withStyle(ChatFormatting.GOLD));
    }

    private StarterKits() {
    }
}
