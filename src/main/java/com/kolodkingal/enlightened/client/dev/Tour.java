package com.kolodkingal.enlightened.client.dev;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.client.KeyBindings;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A development tour, off unless the JVM is started with {@code -Denlightened.tour=true}: makes a flat creative world,
 * walks through the elements casting every ability in front of a zombie, and saves a screenshot of each to the
 * game directory's {@code screenshots} folder, then quits. It lets the effects be looked at on a machine without a
 * player at the keyboard.
 */
@EventBusSubscriber(modid = Enlightened.MODID, value = Dist.CLIENT)
public final class Tour {
    private record Step(int delay, Runnable action) {
    }

    private static final boolean ENABLED = Boolean.getBoolean("enlightened.tour");
    private static final boolean QUICK = Boolean.getBoolean("enlightened.tour.quick");
    private static final List<Step> STEPS = new ArrayList<>();
    private static boolean worldAsked;
    private static boolean scripted;
    private static int wait;
    private static int index;
    private static int settle;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!worldAsked && mc.screen instanceof TitleScreen) {
                worldAsked = true;
                mc.options.renderDistance().set(5);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                LevelSettings settings = new LevelSettings("tour", GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                WorldOptions options = new WorldOptions(12345L, false, false);
                mc.createWorldOpenFlows().createFreshLevel("tour", settings, options,
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        mc.screen);
            }
            return;
        }
        if (settle < 80) {
            settle++;
            return;
        }
        if (!scripted) {
            scripted = true;
            script();
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (index >= STEPS.size()) {
            mc.stop();
            return;
        }
        Step step = STEPS.get(index++);
        step.action.run();
        wait = step.delay;
    }

    // ---- the script ----

    private static void cmd(String command, int delay) {
        STEPS.add(new Step(delay, () -> Minecraft.getInstance().player.connection.sendCommand(command)));
    }

    private static void shot(String name, int delay) {
        STEPS.add(new Step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {
            });
            diagnose(name);
        }));
    }

    /** Logs where the renderer thinks the hands and the effects are at this screenshot, for a look at the log. */
    private static void diagnose(String name) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        var st = com.kolodkingal.enlightened.client.anim.Animator.peek(p);
        StringBuilder stances = new StringBuilder();
        if (st != null) {
            for (com.kolodkingal.enlightened.client.anim.Stance stance : com.kolodkingal.enlightened.client.anim.Stance.values()) {
                if (st.stance(stance) > 0.01F) {
                    stances.append(stance).append('=').append(String.format("%.2f", st.stance(stance))).append(' ');
                }
            }
        }
        Enlightened.LOGGER.info("[tour] {}: pos={} eye={} cam={} type={} handR={} handL={} guessR={} fx={} stances={} wVanilla={} posed={}",
                name, p.position(), p.getEyePosition(), mc.gameRenderer.getMainCamera().getPosition(), mc.options.getCameraType(),
                com.kolodkingal.enlightened.client.anim.HandTracker.hand(p, net.minecraft.world.entity.HumanoidArm.RIGHT, 0),
                com.kolodkingal.enlightened.client.anim.HandTracker.hand(p, net.minecraft.world.entity.HumanoidArm.LEFT, 0),
                com.kolodkingal.enlightened.client.anim.HandTracker.handOrGuess(p, net.minecraft.world.entity.HumanoidArm.RIGHT, 0),
                com.kolodkingal.enlightened.client.fx.WorldFx.all().size(), stances, st == null ? "-" : st.wVanilla, st == null ? "-" : st.posed);
    }

    private static void press(int key, int delay) {
        STEPS.add(new Step(delay, () -> {
            KeyMapping k = KeyBindings.ability(key);
            k.setDown(true);
            KeyMapping.click(k.getKey());
        }));
        STEPS.add(new Step(2, () -> KeyBindings.ability(key).setDown(false)));
    }

    private static void hold(int key, int delay) {
        STEPS.add(new Step(delay, () -> {
            KeyMapping k = KeyBindings.ability(key);
            k.setDown(true);
            KeyMapping.click(k.getKey());
        }));
    }

    private static void release(int key, int delay) {
        STEPS.add(new Step(delay, () -> KeyBindings.ability(key).setDown(false)));
    }

    private static void camera(CameraType type, int delay) {
        STEPS.add(new Step(delay, () -> Minecraft.getInstance().options.setCameraType(type)));
    }

    private static void forward(boolean on, int delay) {
        STEPS.add(new Step(delay, () -> Minecraft.getInstance().options.keyUp.setDown(on)));
    }

    /** Runs on the integrated server, with the tour's player (what no command can do: tame and saddle a bison). */
    private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action, int delay) {
        STEPS.add(new Step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null && mc.player != null) {
                java.util.UUID id = mc.player.getUUID();
                server.execute(() -> {
                    net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(id);
                    if (p != null) {
                        action.accept(p);
                    }
                });
            }
        }));
    }

    private static void wait(int delay) {
        STEPS.add(new Step(delay, () -> {
        }));
    }

    /** Puts an ability on key 1 and casts it once (a press), taking a screenshot {@code after} ticks later. */
    private static void cast(String ability, String name, int after) {
        cmd("enlightened loadout @s 1 " + ability, 6);
        press(1, 2);
        shot(name, after);
        wait(20);
    }

    /** Puts an ability on key 1, holds it, screenshots mid-hold, lets go. */
    private static void held(String ability, String name, int during, int total) {
        cmd("enlightened loadout @s 1 " + ability, 6);
        hold(1, 2);
        shot(name, during);
        release(1, total - during);
        wait(20);
    }

    /** Toggles an ability on, screenshots, toggles it off. */
    private static void toggled(String ability, String name, int during) {
        cmd("enlightened loadout @s 1 " + ability, 6);
        press(1, 2);
        shot(name, during);
        press(1, 2);
        wait(16);
    }

    private static void element(String element, String look, String... abilities) {
        cmd("enlightened element set @s " + element + " " + look, 10);
        cmd("enlightened points add @s 99", 2);
        for (String a : abilities) {
            cmd("enlightened learn @s " + a + " 3", 1);
        }
        cmd("tp @s 0 -60 0 0 10", 10);
        // What the last element raised (a dome, pillars, a wall) is cleared, or the next bender stands in it.
        cmd("fill -12 -60 -12 12 -52 16 air", 5);
        cmd("tp @s 0 -60 0 0 10", 5);
        wait(50);
    }

    /** A short tour of the effects anchored at the hands, for a quick look. */
    private static void quick() {
        element("fire", "zuko", "fire_ball", "fire_fist", "fire_whip", "fire_ring");
        wait(80);
        held("fire_fist", "q11_fire_fist", 18, 40);
        held("fire_whip", "q13_fire_whip", 20, 40);
        held("fire_ball", "q14_fire_ball_charge", 14, 16);
        element("water", "katara", "water_whip", "water_arms", "octopus_form");
        cmd("tp @s 0 -60 4 0 10", 10);
        wait(60);
        cast("water_whip", "q30_water_whip", 4);
        cast("water_whip", "q30b_water_whip", 8);
        toggled("water_arms", "q32_water_arms", 24);
        element("air", "tenzin", "air_blast", "air_shield", "air_scooter");
        wait(60);
        cast("air_blast", "q41_air_blast", 5);
        cast("air_blast", "q41b_air_blast", 10);
        toggled("air_scooter", "q44_scooter", 30);
        forward(true, 2);
        cmd("enlightened loadout @s 1 air_scooter", 6);
        press(1, 2);
        shot("q44b_scooter_riding", 20);
        forward(false, 2);
        press(1, 2);
    }

    private static void script() {
        cmd("gamemode creative", 2);
        cmd("time set 6000", 2);
        cmd("weather clear", 2);
        cmd("gamerule doDaylightCycle false", 2);
        cmd("gamerule doMobSpawning false", 2);
        cmd("fill -16 -61 -16 16 -61 16 grass_block", 5);
        cmd("fill -3 -61 10 3 -61 16 water", 5);
        cmd("fill 5 -61 2 8 -61 5 sand", 5);
        cmd("setblock -5 -60 3 iron_block", 2);
        cmd("setblock -5 -60 5 iron_block", 2);
        cmd("summon zombie 0 -60 7 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cmd("tp @s 0 -60 0 0 10", 10);
        shot("00_scene", 20);
        if (QUICK) {
            quick();
            return;
        }
        if ("v14".equals(System.getProperty("enlightened.tour.only", ""))) {
            v14();
            return;
        }
        if ("suffocate".equals(System.getProperty("enlightened.tour.only", ""))) {
            element("air", "zaheer", "suffocate");
            cmd("setblock 0 -60 6 stone", 2);
            cmd("summon zombie 0 -59 6 {NoAI:1b,PersistenceRequired:1b,Silent:1b}", 5);
            cmd("tp @s 0 -60 0 0 -5", 5);
            camera(CameraType.FIRST_PERSON, 2);
            held("suffocate", "153_suffocate", 20, 40);
            return;
        }
        if ("cosmic".equals(System.getProperty("enlightened.tour.only", ""))) {
            cosmic();
            return;
        }

        // ---- fire ----
        element("fire", "zuko", "fire_ball", "fire_fist", "fire_whip", "fire_ring", "fire_healing", "lightning", "lightning_redirect");
        shot("10_fire_look", 20);
        held("fire_fist", "11_fire_fist", 18, 40);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        wait(40);
        held("fire_fist", "12_fire_fist_front", 18, 40);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        held("fire_whip", "13_fire_whip", 20, 40);
        held("fire_ball", "14_fire_ball_charge", 14, 16);
        shot("15_fire_ball_flight", 6);
        cast("fire_ring", "16_fire_ring", 5);
        cast("fire_healing", "17_fire_healing", 12);
        cmd("enlightened loadout @s 1 lightning", 6);
        press(1, 2);
        shot("18_lightning_charge", 12);
        shot("19_lightning_strike", 11);
        toggled("lightning_redirect", "20_redirect_stance", 20);
        camera(CameraType.FIRST_PERSON, 2);
        held("fire_fist", "21_fire_fist_first_person", 18, 40);
        camera(CameraType.THIRD_PERSON_BACK, 2);

        // ---- water ----
        element("water", "katara", "water_whip", "water_healing", "octopus_form", "water_spout", "water_arms", "ice_knives", "freeze", "snow_throw", "ice_wall");
        cmd("tp @s 0 -60 4 0 10", 10);
        wait(40);
        cast("water_whip", "30_water_whip", 4);
        cast("water_healing", "31_water_healing", 12);
        toggled("water_arms", "32_water_arms", 24);
        toggled("octopus_form", "33_octopus", 30);
        cmd("tp @s 0 -60 12 0 10", 10);
        cmd("enlightened loadout @s 1 water_spout", 6);
        press(1, 2);
        shot("34_spout", 40);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        shot("35_spout_front", 10);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        press(1, 2);
        cmd("tp @s 0 -60 0 0 10", 20);
        cast("ice_knives", "36_ice_knives", 5);
        cast("freeze", "37_freeze", 8);
        cast("ice_wall", "38_ice_wall", 8);
        cast("snow_throw", "39_snow", 5);

        // ---- air ----
        element("air", "tenzin", "air_blast", "air_shield", "air_jump", "air_blades", "air_scooter", "tornado", "flight", "suffocate");
        cmd("enlightened tetherless @s true", 2);
        cmd("give @s enlightened:air_glider", 2);
        cmd("enlightened learn @s flight 3", 2);
        shot("40_air_look", 20);
        cast("air_blast", "41_air_blast", 3);
        toggled("air_shield", "42_air_shield", 24);
        cast("air_blades", "43_air_blades", 4);
        cmd("enlightened loadout @s 1 air_scooter", 6);
        press(1, 2);
        forward(true, 10);
        shot("44_scooter", 20);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        shot("45_scooter_front", 12);
        forward(false, 2);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        press(1, 2);
        cmd("tp @s 0 -60 0 0 10", 20);
        cast("tornado", "46_tornado", 24);
        cmd("enlightened loadout @s 1 air_jump", 6);
        press(1, 2);
        shot("47_glide", 22);
        wait(30);
        cmd("tp @s 0 -60 0 0 10", 10);
        cmd("enlightened loadout @s 1 flight", 6);
        press(1, 2);
        wait(5);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.keyJump.setDown(true)));
        STEPS.add(new Step(6, () -> Minecraft.getInstance().options.keyJump.setDown(false)));
        forward(true, 10);
        shot("48_flight", 25);
        forward(false, 2);
        press(1, 2);
        cmd("tp @s 0 -60 0 0 10", 20);

        // ---- earth ----
        element("earth", "toph", "rock_throw", "rock_lift", "earth_armor", "earth_pillar", "earth_wall", "seismic_sense", "sand_wall", "quicksand", "lava_shuriken", "metal_pull", "metal_blades");
        shot("50_earth_look", 20);
        cast("rock_throw", "51_rock_throw", 6);
        held("rock_lift", "52_rock_lift", 24, 30);
        cast("earth_wall", "53_earth_wall", 10);
        toggled("earth_armor", "54_earth_armor", 20);
        toggled("seismic_sense", "55_seismic", 14);
        cast("metal_pull", "56_metal_pull", 6);
        cast("lava_shuriken", "57_lava_shuriken", 5);
        cast("metal_blades", "58_metal_blades", 5);
        cmd("tp @s 6 -60 0 90 10", 10);
        cmd("summon zombie 2 -60 0 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cast("quicksand", "59_quicksand", 10);
        cmd("tp @s 0 -60 0 0 10", 10);
        cast("earth_pillar", "60_earth_pillar", 10);
        cmd("tp @s 0 -60 0 0 10", 10);

        // ---- the arts of 1.2 ----
        element("earth", "bolin", "rock_throw", "shockwave", "catapult", "earth_grab", "earth_tunnel", "extraction", "earth_wall", "earth_pillar", "density_shift", "earth_step");
        cmd("summon zombie 0 -60 6 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cast("shockwave", "80_shockwave", 6);
        cast("earth_grab", "81_earth_grab", 8);
        held("earth_tunnel", "82_earth_tunnel", 16, 30);
        cmd("setblock 0 -59 4 iron_ore", 2);
        cmd("tp @s 0 -60 1 0 20", 6);
        cast("extraction", "83_extraction", 6);
        cmd("tp @s 0 -60 0 0 10", 6);
        cmd("enlightened loadout @s 1 catapult", 6);
        press(1, 2);
        shot("84_catapult", 10);
        wait(50);
        cmd("tp @s 0 -60 0 0 10", 10);
        // The Earth Dome combo: a wall, then the shockwave.
        cmd("enlightened loadout @s 1 earth_wall", 4);
        cmd("enlightened loadout @s 2 shockwave", 4);
        press(1, 10);
        press(2, 2);
        shot("85_combo_earth_dome", 12);
        wait(30);
        cmd("tp @s 0 -60 0 0 10", 10);

        element("air", "jinora", "air_blast", "air_suction", "air_burst", "air_spout", "air_agility", "air_step", "air_shield", "tornado", "air_blades");
        cmd("summon zombie 0 -60 6 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cast("air_suction", "86_air_suction", 4);
        cast("air_burst", "87_air_burst", 4);
        cmd("enlightened loadout @s 1 air_spout", 6);
        press(1, 2);
        shot("88_air_spout", 36);
        press(1, 2);
        wait(30);
        cmd("tp @s 0 -60 0 0 10", 10);

        element("water", "ming_hua", "water_whip", "water_shield", "water_bubble", "phase_change", "frost_breath", "plant_tether", "fast_swim", "water_step", "ice_wall", "ice_knives");
        cmd("tp @s 0 -60 4 0 10", 10);
        cmd("setblock 3 -60 6 oak_leaves", 2);
        cmd("setblock 3 -59 6 oak_leaves", 2);
        cmd("summon zombie 0 -60 8 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        held("water_shield", "89_water_shield", 20, 40);
        held("frost_breath", "90_frost_breath", 20, 40);
        cast("plant_tether", "91_plant_tether", 5);
        cmd("tp @s 0 -60 4 0 30", 10);
        cast("phase_change", "92_phase_change", 8);
        cmd("tp @s 0 -60 0 0 10", 10);

        element("fire", "azula", "fire_ball", "fire_jet", "fire_shield", "wall_of_fire", "blaze", "heat_control", "illumination", "combustion", "fire_ring", "fire_fist");
        cmd("summon zombie 0 -60 8 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        held("fire_shield", "93_fire_shield_blue", 20, 40);
        cast("wall_of_fire", "94_wall_of_fire", 14);
        cast("blaze", "95_blaze", 6);
        toggled("illumination", "96_illumination", 14);
        cmd("enlightened loadout @s 1 combustion", 6);
        press(1, 2);
        shot("97_combustion_charge", 8);
        shot("98_combustion_blast", 10);
        wait(10);
        cmd("enlightened loadout @s 1 fire_jet", 6);
        press(1, 2);
        shot("99_fire_jet", 6);
        wait(40);
        cmd("tp @s 0 -60 0 0 10", 10);
        // The Fire Spin combo: the shield, then the ring.
        cmd("enlightened loadout @s 1 fire_shield", 4);
        cmd("enlightened loadout @s 2 fire_ring", 4);
        hold(1, 6);
        release(1, 2);
        press(2, 2);
        shot("100_combo_fire_spin", 8);
        wait(30);
        cmd("enlightened points add @s 99", 2);
        cmd("enlightened learn @s fire_ball 3", 1);
        cmd("enlightened learn @s fire_fist 3", 1);
        cmd("enlightened learn @s fire_whip 3", 1);
        cmd("enlightened learn @s fire_ring 3", 1);
        cmd("enlightened learn @s fire_healing 3", 1);
        cmd("enlightened learn @s lightning 3", 1);
        cmd("enlightened chakras @s 6", 4);
        STEPS.add(new Step(2, () -> KeyBindings.MEDITATE.setDown(true)));
        shot("101_meditation", 40);
        STEPS.add(new Step(2, () -> KeyBindings.MEDITATE.setDown(false)));
        cmd("enlightened chakras @s 7", 4);
        STEPS.add(new Step(2, () -> KeyBindings.MEDITATE.setDown(true)));
        shot("102_avatar_rise", 30);
        STEPS.add(new Step(2, () -> KeyBindings.MEDITATE.setDown(false)));
        shot("103_avatar_state", 20);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        shot("104_avatar_state_front", 10);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        held("fire_fist", "105_avatar_fire_fist", 16, 30);
        wait(10);

        // ---- the lemur ----
        // ---- 1.3: the glider, the scooter on water, the octopus's lash, the vortex, the bubble, blue fire, the forge, the Avatar ----
        element("air", "tenzin", "air_jump", "air_scooter", "air_blast");
        cmd("give @s enlightened:air_glider", 2);
        cmd("fill -3 -61 10 3 -61 16 water", 5);
        wait(10);
        cmd("enlightened loadout @s 1 air_jump", 6);
        STEPS.add(new Step(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }));
        shot("110_glider_open", 12);
        press(1, 2);
        shot("111_glider_jump", 6);
        camera(CameraType.THIRD_PERSON_FRONT, 1);
        shot("112_glider_front", 3);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        wait(40);
        cmd("tp @s 0 -60 9 0 10", 10);
        toggled("air_scooter", "113_scooter_shore", 20);
        cmd("enlightened loadout @s 1 air_scooter", 6);
        press(1, 2);
        forward(true, 2);
        shot("114_scooter_on_water", 30);
        forward(false, 2);
        press(1, 2);
        cmd("tp @s 0 -60 0 0 10", 10);
        element("water", "katara", "octopus_form", "water_vortex", "water_bubble");
        cmd("summon zombie 0 -60 4 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cmd("enlightened loadout @s 1 octopus_form", 6);
        press(1, 2);
        wait(20);
        press(1, 2);
        shot("120_octopus_lash", 4);
        wait(20);
        // A step's delay is the wait after it: the key stays down 20 ticks, long enough to fold the form.
        hold(1, 20);
        release(1, 4);
        cmd("tp @s 0 -60 2 0 20", 10);
        cast("water_vortex", "121_water_vortex", 30);
        cmd("tp @s 0 -60 12 0 60", 10);
        wait(20);
        cmd("enlightened loadout @s 1 water_vortex", 6);
        press(1, 2);
        shot("122_vortex_ride", 30);
        press(1, 2);
        cmd("fill -4 -64 20 4 -58 28 water", 5);
        cmd("tp @s 0 -62 24 0 10", 10);
        toggled("water_bubble", "123_water_bubble", 40);
        cmd("tp @s 0 -60 0 0 10", 10);
        element("fire", "azula", "blaze", "fire_ball", "palm_forge");
        cmd("summon zombie 0 -60 6 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cast("blaze", "130_blue_fire_ground", 20);
        cmd("item replace entity @s weapon.mainhand with raw_iron 8", 2);
        held("palm_forge", "131_palm_forge", 30, 80);
        cmd("tp @s 0 -60 0 0 10", 10);
        cmd("give @s enlightened:avatar_scroll", 2);
        element("avatar", "aang", "avatar_state", "energybending", "spiritbending", "elemental_sphere", "fire_fist", "water_whip", "rock_throw", "air_blast");
        cmd("enlightened chakras @s 7", 2);
        cmd("enlightened avatar @s rest", 2);
        cmd("summon zombie 0 -60 5 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cast("spiritbending", "140_spiritbending", 15);
        cast("avatar_state", "141_avatar_state", 20);
        toggled("elemental_sphere", "142_elemental_sphere", 30);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        shot("143_avatar_front", 5);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        held("fire_fist", "144_avatar_fire_fist", 15, 30);
        cast("water_whip", "145_avatar_water_whip", 5);

        v14();

        cmd("summon enlightened:flying_lemur 1 -60 3", 5);
        cmd("summon enlightened:flying_lemur -1 -58 3", 5);
        shot("70_lemur", 30);
        shot("71_lemur_later", 30);
        wait(10);
    }

    /** The 1.4 section (also alone, with -Denlightened.tour.only=v14). */
    private static void v14() {
        // ---- 1.4: the spouts, the octopus body, frost walk, suffocation, the temple, the bison, the cosmic plane ----
        element("water", "katara", "water_spout", "octopus_form", "frost_walk", "water_whip");
        cmd("fill -2 -61 -2 2 -61 2 ice", 3);
        cmd("enlightened loadout @s 1 water_spout", 6);
        press(1, 60);
        shot("150_water_spout_ice", 5);
        press(1, 30);
        cmd("tp @s 0 -60 0 0 10", 10);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        toggled("octopus_form", "151_octopus_body", 30);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        cmd("fill -6 -61 5 6 -61 17 water", 3);
        cmd("tp @s 0 -60 7 0 35", 5);
        cmd("enlightened loadout @s 1 frost_walk", 6);
        press(1, 25);
        shot("152_frost_walk", 5);
        press(1, 4);
        cmd("tp @s 0 -60 0 0 10", 5);
        // The bubble held for three seconds: it must not collapse.
        element("water", "katara", "water_bubble");
        cmd("fill -6 -64 18 6 -56 30 water", 5);
        cmd("tp @s 0 -62 24 0 10", 10);
        cmd("enlightened loadout @s 1 water_bubble", 6);
        press(1, 60);
        press(1, 10);
        cmd("fill -6 -64 18 6 -56 30 air", 5);
        cmd("fill -6 -64 18 6 -62 30 dirt", 5);
        cmd("fill -6 -61 18 6 -61 30 grass_block", 5);
        cmd("tp @s 0 -60 0 0 10", 5);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        element("fire", "iroh", "fire_ball");
        shot("156_iroh_white_lotus", 10);
        element("fire", "jeong_jeong", "fire_ball");
        shot("157_jeong_jeong", 10);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        element("air", "zaheer", "suffocate", "air_spout", "air_blast");
        cmd("setblock 0 -60 6 stone", 2);
        cmd("summon zombie 0 -59 6 {NoAI:1b,PersistenceRequired:1b,Silent:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1}]}", 5);
        cmd("tp @s 0 -60 0 0 -5", 5);
        camera(CameraType.FIRST_PERSON, 2);
        held("suffocate", "153_suffocate", 20, 40);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        cmd("setblock 0 -60 6 air", 2);
        cmd("kill @e[type=zombie]", 2);
        cmd("enlightened loadout @s 1 air_spout", 6);
        press(1, 50);
        shot("154_air_spout", 5);
        press(1, 30);
        cmd("tp @s 0 -60 0 0 10", 10);
        // The temple, built on the flat world by its command.
        cmd("enlightened temple 0 -60 80", 60);
        // The views from the air are taken as a spectator, who hangs where he is put.
        cmd("gamemode spectator", 5);
        cmd("tp @s 0 -36 34 0 22", 40);
        shot("160_temple", 10);
        cmd("tp @s -32 -42 58 -62 15", 30);
        shot("161_temple_side", 10);
        cmd("tp @s 0 -50 98 180 40", 30);
        shot("163_bison_calves", 10);
        cmd("tp @s 0 -56 74 0 30", 30);
        shot("162_pathik", 10);
        cmd("gamemode creative", 5);
        cmd("tp @s 0 -59 64 0 10", 10);
        server(p -> {
            com.kolodkingal.enlightened.entity.SkyBisonEntity b = com.kolodkingal.enlightened.registry.ModEntities.SKY_BISON.get().create(p.serverLevel());
            if (b != null) {
                b.moveTo(p.getX(), p.getY(), p.getZ() - 4, 0.0F, 0.0F);
                b.tame(p);
                b.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
                p.serverLevel().addFreshEntity(b);
                p.startRiding(b);
            }
        }, 20);
        shot("164_bison_ride", 20);
        // "Yip yip!": Jump held, the bison climbs.
        STEPS.add(new Step(50, () -> Minecraft.getInstance().options.keyJump.setDown(true)));
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.keyJump.setDown(false)));
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        shot("165_bison_flying", 10);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        server(p -> p.stopRiding(), 5);
        cmd("tp @s 0 -60 0 0 10", 10);
        cosmic();
        cmd("tp @s " + (int) Math.floor(com.kolodkingal.enlightened.world.Cosmic.END.x) + " 101 66", 40);
        cmd("tp @s 0 -60 0 0 10", 10);

    }

    /** The cosmic plane (also alone, with -Denlightened.tour.only=cosmic). */
    private static void cosmic() {
        // The cosmic plane of the seventh chakra.
        element("avatar", "aang", "avatar_state", "fire_fist");
        cmd("enlightened chakras @s 6", 2);
        server(com.kolodkingal.enlightened.world.Cosmic::send, 100);
        camera(CameraType.FIRST_PERSON, 2);
        shot("170_cosmic_path", 20);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        cmd("tp @s " + com.kolodkingal.enlightened.world.Cosmic.pathX(44) + " 101 44 0 0", 30);
        camera(CameraType.FIRST_PERSON, 2);
        shot("171_cosmic_giant", 10);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        shot("172_cosmic_front", 5);
        camera(CameraType.THIRD_PERSON_BACK, 2);
    }

    private Tour() {
    }
}
