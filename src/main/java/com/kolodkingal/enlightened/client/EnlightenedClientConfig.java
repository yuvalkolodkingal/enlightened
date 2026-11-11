package com.kolodkingal.enlightened.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client's own switches: the animation layer, reduced motion, the hand effects, the first-person hands. */
public final class EnlightenedClientConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ANIMATIONS;
    public static final ModConfigSpec.BooleanValue REDUCED_MOTION;
    public static final ModConfigSpec.BooleanValue EFFECTS;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_HANDS;
    public static final ModConfigSpec.EnumValue<com.kolodkingal.enlightened.client.hud.PowerPanels.Style> HUD_STYLE;
    public static final ModConfigSpec.BooleanValue HUD_RIGHT;
    public static final ModConfigSpec SPEC;

    static {
        B.push("animation");
        ANIMATIONS = B.comment("The benders' body language: stances, gestures, the lean of flight and the glider. Off draws everyone as vanilla does.")
                .translation("enlightened.configuration.animations").define("animations", true);
        REDUCED_MOTION = B.comment("Calmer motion: no trembles, no hover drift, smaller bobs.")
                .translation("enlightened.configuration.reducedMotion").define("reducedMotion", false);
        EFFECTS = B.comment("The flourishes that go with the gestures: flames at the fists, dust at a stomp, mist at the hands.")
                .translation("enlightened.configuration.effects").define("effects", true);
        FIRST_PERSON_HANDS = B.comment("Move the first-person hands with the stances and gestures.")
                .translation("enlightened.configuration.firstPersonHands").define("firstPersonHands", true);
        B.pop();
        B.push("hud");
        HUD_STYLE = B.comment("How the bending panel is drawn. CLASSIC: the text panel of every version until now. STACK: one bar per key. "
                        + "DIAL: icons around an emblem. COMIC: comic panels. SCROLL: a scroll pinned to the corner. CLUSTER: a small cluster of icons.")
                .translation("enlightened.configuration.hudStyle")
                .defineEnum("hudStyle", com.kolodkingal.enlightened.client.hud.PowerPanels.Style.SCROLL);
        HUD_RIGHT = B.comment("Draw the new panel styles in the top-right corner (off: top-left). The classic panel is always top-left.")
                .translation("enlightened.configuration.hudOnTheRight").define("hudOnTheRight", true);
        B.pop();
        SPEC = B.build();
    }

    public static boolean animations() {
        return !SPEC.isLoaded() || ANIMATIONS.get();
    }

    public static boolean reducedMotion() {
        return SPEC.isLoaded() && REDUCED_MOTION.get();
    }

    public static boolean effects() {
        return !SPEC.isLoaded() || EFFECTS.get();
    }

    public static boolean firstPersonHands() {
        return !SPEC.isLoaded() || FIRST_PERSON_HANDS.get();
    }

    public static com.kolodkingal.enlightened.client.hud.PowerPanels.Style hudStyle() {
        return SPEC.isLoaded() ? HUD_STYLE.get() : com.kolodkingal.enlightened.client.hud.PowerPanels.Style.SCROLL;
    }

    public static boolean hudRight() {
        return !SPEC.isLoaded() || HUD_RIGHT.get();
    }

    private EnlightenedClientConfig() {
    }
}
