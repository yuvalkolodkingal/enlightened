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

    /** A condition the script waits on (at most {@code holdLeft} ticks) before its next step. */
    private static java.util.function.BooleanSupplier hold;
    private static int holdLeft;
    /** The sea's surface in the water world of the lion turtle's tour. */
    private static volatile int sea = 62;
    private static volatile boolean islandUp;

    private static void until(java.util.function.BooleanSupplier condition, int maxTicks) {
        STEPS.add(new Step(0, () -> {
            hold = condition;
            holdLeft = maxTicks;
        }));
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
                // No chat in the pictures (faded out, not turned off: turned off, the commands could not be sent).
                mc.options.chatOpacity().set(0.0);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                LevelSettings settings = new LevelSettings("tour", GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                WorldOptions options = new WorldOptions(12345L, false, false);
                boolean sea = "lionturtle".equals(System.getProperty("enlightened.tour.only", ""));
                mc.createWorldOpenFlows().createFreshLevel("tour", settings, options, access -> {
                    net.minecraft.world.level.levelgen.WorldDimensions dims = access.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
                    if (!sea) {
                        return dims;
                    }
                    // The lion turtle's tour: the "water world" flat preset, ninety blocks of sea.
                    var flat = access.registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
                            .getHolderOrThrow(net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets.WATER_WORLD).value().settings();
                    return dims.replaceOverworldGenerator(access, new net.minecraft.world.level.levelgen.FlatLevelSource(flat));
                }, mc.screen);
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
        if (hold != null) {
            if (!hold.getAsBoolean() && --holdLeft > 0) {
                return;
            }
            hold = null;
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

    /** Clean shots (-Denlightened.tour.clean=true): the interface is hidden for each picture, for the gallery. */
    private static final boolean CLEAN = Boolean.getBoolean("enlightened.tour.clean");

    private static void shot(String name, int delay) {
        // No pop-ups (advancements, recipes) in the pictures.
        STEPS.add(new Step(1, () -> Minecraft.getInstance().getToasts().clear()));
        if (CLEAN) {
            STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
        }
        STEPS.add(new Step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {
            });
            diagnose(name);
            if (CLEAN) {
                mc.options.hideGui = false;
            }
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

    private static void logMeditation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        com.kolodkingal.enlightened.bending.BendingData d = com.kolodkingal.enlightened.bending.Benders.get(mc.player);
        com.kolodkingal.enlightened.Enlightened.LOGGER.info("[tour] meditating={} progress={} chakras={} taught={} onGround={} key={} pos={}", d.meditating(), d.meditation(),
                d.chakras(), d.guruTaught(), mc.player.onGround(), com.kolodkingal.enlightened.client.KeyBindings.MEDITATE.isDown(), mc.player.position());
    }

    /** Right-clicks the nearest sky bison, through the client as a player does. */
    private static void interactBison() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        net.minecraft.world.entity.Entity best = null;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof com.kolodkingal.enlightened.entity.SkyBisonEntity && (best == null || e.distanceToSqr(mc.player) < best.distanceToSqr(mc.player))) {
                best = e;
            }
        }
        if (best != null) {
            net.minecraft.world.InteractionResult r = mc.gameMode.interact(mc.player, best, net.minecraft.world.InteractionHand.MAIN_HAND);
            com.kolodkingal.enlightened.Enlightened.LOGGER.info("[tour] interact bison -> {} tame={} riding={}", r,
                    ((com.kolodkingal.enlightened.entity.SkyBisonEntity) best).isTame(), mc.player.getVehicle() != null);
        }
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
        if ("meditate".equals(System.getProperty("enlightened.tour.only", ""))) {
            element("air", "aang", "air_blast");
            cmd("enlightened guru @s taught", 2);
            cmd("gamemode survival", 5);
            for (int i = 0; i < 8; i++) {
                STEPS.add(new Step(5, Tour::logMeditation));
            }
            // One press of the key, let go at once: it sits him down until the next press.
            STEPS.add(new Step(2, () -> com.kolodkingal.enlightened.client.KeyBindings.MEDITATE.setDown(true)));
            STEPS.add(new Step(2, () -> com.kolodkingal.enlightened.client.KeyBindings.MEDITATE.setDown(false)));
            for (int i = 0; i < 12; i++) {
                STEPS.add(new Step(5, Tour::logMeditation));
            }
            camera(CameraType.THIRD_PERSON_FRONT, 2);
            shot("180_meditate_key", 5);
            camera(CameraType.THIRD_PERSON_BACK, 2);
            STEPS.add(new Step(2, () -> com.kolodkingal.enlightened.client.KeyBindings.MEDITATE.setDown(true)));
            STEPS.add(new Step(2, () -> com.kolodkingal.enlightened.client.KeyBindings.MEDITATE.setDown(false)));
            STEPS.add(new Step(10, Tour::logMeditation));
            cmd("setblock 0 -60 3 enlightened:meditation_point", 10);
            STEPS.add(new Step(5, () -> {
                Minecraft mc = Minecraft.getInstance();
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, -60, 3);
                net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
                net.minecraft.world.InteractionResult r = mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                com.kolodkingal.enlightened.Enlightened.LOGGER.info("[tour] use mat -> {}", r);
            }));
            for (int i = 0; i < 12; i++) {
                STEPS.add(new Step(5, Tour::logMeditation));
            }
            shot("181_meditate_mat", 5);
            return;
        }
        if ("cosmic".equals(System.getProperty("enlightened.tour.only", ""))) {
            cosmic();
            return;
        }
        if ("looks".equals(System.getProperty("enlightened.tour.only", ""))) {
            STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
            camera(CameraType.THIRD_PERSON_FRONT, 4);
            String[][] looks = {{"earth", "bumi"}, {"fire", "iroh"}, {"fire", "jeong_jeong"}, {"avatar", "pavi"}, {"avatar", "nisha"}};
            for (String[] l : looks) {
                cmd("enlightened element set @s " + l[0] + " " + l[1], 20);
                shot("230_look_" + l[1], 10);
            }
            return;
        }
        if ("momo".equals(System.getProperty("enlightened.tour.only", ""))) {
            server(p -> {
                com.kolodkingal.enlightened.entity.FlyingLemurEntity momo = com.kolodkingal.enlightened.registry.ModEntities.LEMUR.get().create(p.serverLevel());
                momo.moveTo(p.getX(), p.getY(), p.getZ() + 1);
                p.serverLevel().addFreshEntity(momo);
                momo.tame(p);
                momo.setEntityOnShoulder(p);
            }, 20);
            STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
            camera(CameraType.THIRD_PERSON_FRONT, 10);
            shot("220_momo_shoulder_front", 10);
            camera(CameraType.THIRD_PERSON_BACK, 10);
            shot("221_momo_shoulder_back", 10);
            return;
        }
        if ("v15".equals(System.getProperty("enlightened.tour.only", ""))) {
            fireColours();
            return;
        }
        if ("bison".equals(System.getProperty("enlightened.tour.only", ""))) {
            bisonFace();
            return;
        }
        if ("lionturtle".equals(System.getProperty("enlightened.tour.only", ""))) {
            lionTurtle();
            return;
        }
        if ("cities".equals(System.getProperty("enlightened.tour.only", ""))) {
            cities();
            return;
        }
        if ("anomaly".equals(System.getProperty("enlightened.tour.only", ""))) {
            anomaly();
            return;
        }
        if ("omashu".equals(System.getProperty("enlightened.tour.only", ""))) {
            omashu();
            return;
        }
        if ("spirit".equals(System.getProperty("enlightened.tour.only", ""))) {
            spirit();
            return;
        }
        if ("back".equals(System.getProperty("enlightened.tour.only", ""))) {
            cmd("item replace entity @s armor.chest with minecraft:iron_chestplate", 2);
            server(p -> com.kolodkingal.enlightened.item.BackSlot.set(p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)), 15);
            STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
            camera(CameraType.THIRD_PERSON_BACK, 20);
            shot("270_back_sword", 5);
            camera(CameraType.THIRD_PERSON_FRONT, 10);
            shot("271_back_sword_front", 5);
            cmd("item replace entity @s armor.chest with minecraft:air", 2);
            server(p -> com.kolodkingal.enlightened.item.BackSlot.set(p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW)), 10);
            camera(CameraType.THIRD_PERSON_BACK, 10);
            shot("272_back_bow", 5);
            return;
        }
        if ("glider".equals(System.getProperty("enlightened.tour.only", ""))) {
            gliderLook();
            return;
        }
        if ("havens".equals(System.getProperty("enlightened.tour.only", ""))) {
            sevenHavens();
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
        STEPS.add(new Step(5, () -> Minecraft.getInstance().options.renderDistance().set(10)));
        cmd("tp @s 0 -14 6 0 14", 120);
        shot("160_temple", 10);
        cmd("tp @s -58 -26 44 -55 8", 80);
        shot("161_temple_side", 10);
        cmd("tp @s -26 -56 80 90 15", 40);
        shot("166_gyatso", 10);
        cmd("tp @s 0 -15 84 180 40", 30);
        shot("162_pathik", 10);
        cmd("tp @s 0 -14 76 0 40", 20);
        shot("167_sanctuary_statues", 10);
        cmd("tp @s 0 -15 64 0 8", 20);
        shot("168_sanctuary_door", 10);
        cmd("tp @s -17 -46 65 -135 35", 20);
        shot("169_storage", 10);
        cmd("tp @s 0 -46 126 180 40", 30);
        shot("163_bison_calves", 10);
        cmd("tp @s 34 -50 70 60 15", 30);
        shot("163b_stairway", 10);
        STEPS.add(new Step(5, () -> Minecraft.getInstance().options.renderDistance().set(5)));
        cmd("gamemode creative", 5);
        cmd("tp @s 0 -60 16 0 10", 10);
        // A grown bison tamed, ridden and flown by right-clicks, as a player does it.
        cmd("kill @e[type=enlightened:sky_bison,distance=..20]", 2);
        cmd("summon enlightened:sky_bison 0 -60 20 {Rotation:[180f,0f],NoAI:1b}", 20);
        cmd("item replace entity @s weapon.mainhand with wheat 32", 5);
        for (int i = 0; i < 17; i++) {
            STEPS.add(new Step(3, Tour::interactBison));
        }
        cmd("item replace entity @s weapon.mainhand with air", 5);
        cmd("gamemode spectator", 5);
        cmd("tp @s 2 -58 12 -15 8", 20);
        shot("164_bison_face", 10);
        cmd("gamemode creative", 5);
        cmd("data merge entity @e[type=enlightened:sky_bison,limit=1,sort=nearest] {NoAI:0b}", 2);
        cmd("tp @s 0 -60 16 0 10", 15);
        STEPS.add(new Step(10, Tour::interactBison));
        shot("165_bison_ride", 10);
        // "Yip yip!": Jump held, the bison climbs.
        STEPS.add(new Step(50, () -> Minecraft.getInstance().options.keyJump.setDown(true)));
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.keyJump.setDown(false)));
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        shot("165b_bison_flying", 10);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        server(p -> p.stopRiding(), 5);
        cmd("tp @s 0 -60 0 0 10", 10);
        cosmic();
        cmd("tp @s " + (int) Math.floor(com.kolodkingal.enlightened.world.Cosmic.END.x) + " 101 66", 40);
        cmd("tp @s 0 -60 0 0 10", 10);

    }

    /** The fire colours, the lightning after Thor's beam, bloodbending without lightning (alone: -Denlightened.tour.only=v15). */
    private static void fireColours() {
        element("fire", "zuko", "fire_ball", "fire_fist", "fire_whip", "lightning");
        cmd("enlightened firecolor @s rainbow", 4);
        held("fire_fist", "200_rainbow_fire_fist", 18, 40);
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        held("fire_fist", "201_rainbow_fire_fist_front", 18, 40);
        held("fire_whip", "202_rainbow_fire_whip", 14, 30);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        held("fire_ball", "203_rainbow_fire_ball", 14, 16);
        shot("204_rainbow_fire_ball_flight", 6);
        wait(20);
        cmd("enlightened firecolor @s blue", 4);
        held("fire_fist", "205_blue_fire_fist", 18, 40);
        cmd("enlightened firecolor @s normal", 4);
        held("fire_fist", "206_normal_fire_fist", 18, 40);
        // The lightning, from the hand, as Thor's beam: cast at the zombie, then seen from the side.
        cmd("enlightened loadout @s 1 lightning", 6);
        cmd("tp @s 0 -60 2 0 5", 4);
        press(1, 2);
        shot("207_lightning_charge", 12);
        shot("208_lightning_strike", 6);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
        cmd("gamemode spectator", 4);
        cmd("tp @s 7 -58.5 3.5 90 5", 20);
        for (int i = 0; i < 2; i++) {
            server(p -> com.kolodkingal.enlightened.fx.Fx.send(p.serverLevel(), com.kolodkingal.enlightened.fx.Fx.LIGHTNING,
                    new net.minecraft.world.phys.Vec3(0.3, -58.6, 1.0), new net.minecraft.world.phys.Vec3(0.5, -59.0, 7.5), 0, 1.0F), 3);
            shot("209_lightning_side_" + i, 4);
        }
        cmd("gamemode creative", 4);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = false));
        cmd("tp @s 0 -60 0 0 10", 4);
        // Bloodbending: the zombie held, no lightning.
        element("water", "katara", "bloodbending");
        cmd("time set 18000", 4);
        cmd("tp @s 0 -60 4 0 10", 6);
        held("bloodbending", "211_bloodbending", 20, 40);
        cmd("time set 6000", 4);
    }

    /** The sky bison's face from the front, three quarters and the side (alone: -Denlightened.tour.only=bison). */
    private static void bisonFace() {
        cmd("kill @e[type=enlightened:sky_bison]", 2);
        cmd("summon enlightened:sky_bison 0 -60 8 {Rotation:[180f,0f],NoAI:1b}", 20);
        cmd("gamemode spectator", 5);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
        cmd("tp @s 0.5 -58.3 1.5 0 10", 20);
        shot("190_bison_front", 10);
        cmd("tp @s 4 -58 3 35 10", 20);
        shot("191_bison_three_quarters", 10);
        cmd("tp @s 7 -58.5 7 90 5", 20);
        shot("192_bison_side", 10);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = false));
        cmd("gamemode creative", 5);
    }

    /** Builds a piece whole, chunk by chunk, as the world would. */
    private static void build(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.levelgen.structure.StructurePiece piece) {
        net.minecraft.world.level.levelgen.structure.BoundingBox all = piece.getBoundingBox();
        net.minecraft.core.BlockPos pivot = new net.minecraft.core.BlockPos(all.getCenter());
        for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) {
            for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
                net.minecraft.world.level.levelgen.structure.BoundingBox box = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        cx << 4, all.minY(), cz << 4, (cx << 4) + 15, all.maxY(), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box,
                        new net.minecraft.world.level.ChunkPos(cx, cz), pivot);
            }
        }
    }

    /** Ba Sing Se and Omashu, built on the flat world (alone: -Denlightened.tour.only=cities). */
    private static void cities() {
        cmd("gamemode spectator", 5);
        cmd("time set noon", 2);
        STEPS.add(new Step(5, () -> {
            Minecraft.getInstance().options.renderDistance().set(16);
            Minecraft.getInstance().options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            Minecraft.getInstance().options.hideGui = true;
        }));
        server(p -> {
            build(p.serverLevel(), new com.kolodkingal.enlightened.world.BaSingSePiece(new net.minecraft.core.BlockPos(0, -61, 0)));
            p.teleportTo(p.serverLevel(), 0.5, -15, 135.5, 180.0F, 18.0F);
        }, 120);
        shot("280_ba_sing_se_outer_wall", 40);
        cmd("tp @s 70.5 -20 70.5 135 30", 60);
        shot("281_ba_sing_se_rings", 30);
        cmd("tp @s 0.5 -42 40.5 180 10", 60);
        shot("282_ba_sing_se_palace", 30);
        cmd("tp @s 0.5 -55 90.5 180 0", 60);
        shot("283_ba_sing_se_lower_ring", 30);
        omashuShots();
    }

    /** Omashu, King Bumi on his throne, and the guards of the three cities (alone: -Denlightened.tour.only=omashu). */
    private static void omashu() {
        cmd("gamemode spectator", 5);
        cmd("time set noon", 2);
        cmd("gamerule doMobSpawning false", 2);
        STEPS.add(new Step(5, () -> {
            Minecraft.getInstance().options.renderDistance().set(16);
            Minecraft.getInstance().options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            Minecraft.getInstance().options.hideGui = true;
        }));
        omashuShots();
        // The guards of the three cities side by side, three faces each.
        cmd("tp @s 200.5 -58.4 6.5 180 8", 40);
        for (int c = 0; c < 3; c++) {
            for (int v = 0; v < 3; v++) {
                double x = 196.5 + c * 3.2 + v * 0.9 - 0.9;
                double z = 1.5 - (v == 1 ? 0.6 : 0.0);
                cmd(String.format(java.util.Locale.ROOT, "summon enlightened:city_guard %.1f -60 %.1f {City:%d,Voice:%d,NoAI:1b,Rotation:[0f,0f]}", x, z, c, v), 2);
            }
        }
        shot("288_city_guards", 40);
        // A guard of Ellora cuts down a husk.
        cmd("tp @s 260.5 -57.5 7.5 160 15", 20);
        cmd("summon enlightened:city_guard 260.5 -60 0.5 {City:2,Voice:1}", 2);
        cmd("summon minecraft:husk 262.5 -60 -1.5", 30);
        shot("289_guard_fights", 2);
    }

    private static void omashuShots() {
        server(p -> {
            // Raise the flat world round the city forty blocks, so the chasm has its walls and its river.
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            net.minecraft.core.BlockPos.MutableBlockPos at = new net.minecraft.core.BlockPos.MutableBlockPos();
            for (int x = 400 - 120; x <= 400 + 120; x++) {
                for (int z = -120; z <= 120; z++) {
                    double r = Math.sqrt((x - 400) * (x - 400) + z * z);
                    if (r < com.kolodkingal.enlightened.world.OmashuPiece.CHASM - 1 || r > 120) {
                        continue;
                    }
                    level.getChunk(x >> 4, z >> 4);
                    for (int y = -60; y <= -21; y++) {
                        level.setBlock(at.set(x, y, z), y == -21 ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState()
                                : y > -25 ? net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()
                                : net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
            build(level, new com.kolodkingal.enlightened.world.OmashuPiece(new net.minecraft.core.BlockPos(400, -21, 0)));
        }, 300);
        cmd("tp @s 470.5 30 75.5 135 14", 80);
        shot("286_omashu_mountain", 300);
        cmd("tp @s 400.5 0 68.5 180 -22", 60);
        shot("287_omashu_stairway", 30);
        cmd("tp @s 430.5 22 30.5 160 15", 40);
        shot("287b_omashu_terraces", 30);
        cmd("tp @s 400.5 23.4 2.5 180 12", 40);
        shot("284_king_bumi_throne", 30);
        cmd("data merge entity @e[type=enlightened:king_bumi,limit=1] {NoAI:1b}", 2);
        cmd("execute as @e[type=enlightened:king_bumi] at @s run tp @s ~ ~ ~ 0 0", 2);
        cmd("tp @s 402.0 23.4 -4.0 155 4", 2);
        cmd("tp @e[type=enlightened:city_guard,distance=..8] 400.5 22 30.5", 30);
        shot("284b_king_bumi", 30);

        cmd("tp @s 412.5 -6 118.5 172 -8", 200);
        shot("285_omashu_bridge", 200);
    }

    /** A spirit gate, the crossing, the Spirit World and the Tree of Time (alone: -Denlightened.tour.only=spirit). */
    private static void spirit() {
        cmd("time set noon", 2);
        STEPS.add(new Step(5, () -> {
            Minecraft.getInstance().options.renderDistance().set(12);
            Minecraft.getInstance().options.hideGui = true;
        }));
        server(p -> build(p.serverLevel(), new com.kolodkingal.enlightened.world.SpiritPortalPiece(new net.minecraft.core.BlockPos(0, -61, 20))), 40);
        cmd("tp @s 0.5 -58 8.5 0 5", 30);
        shot("290_spirit_portal", 20);
        cmd("tp @s 9.5 -55 10.5 40 25", 20);
        shot("291_spirit_portal_stones", 20);
        server(p -> com.kolodkingal.enlightened.world.SpiritWorld.enter(p), 100);
        shot("292_spirit_world_arrival", 40);
        cmd("gamemode spectator", 5);
        server(p -> {
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            int x = p.getBlockX() + 40;
            int z = p.getBlockZ();
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            build(level, new com.kolodkingal.enlightened.world.TreeOfTimePiece(new net.minecraft.core.BlockPos(x, y, z)));
            p.teleportTo(level, x - 30.5, y + 18, z + 0.5, -90.0F, 5.0F);
        }, 120);
        shot("293_tree_of_time", 40);
        server(p -> p.teleportTo(p.serverLevel(), p.getX() - 20, p.getY() + 30, p.getZ() + 20, -135.0F, 35.0F), 80);
        shot("294_spirit_world_view", 40);
    }

    /** The glider of the show, open in a glide (alone: -Denlightened.tour.only=glider). */
    private static void gliderLook() {
        element("air", "aang", "air_jump");
        cmd("clear @s", 2);
        cmd("item replace entity @s armor.chest with minecraft:iron_chestplate", 2);
        server(p -> com.kolodkingal.enlightened.item.BackSlot.set(p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)), 15);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
        camera(CameraType.THIRD_PERSON_BACK, 20);
        shot("257_back_slot_sword", 5);
        server(p -> com.kolodkingal.enlightened.item.BackSlot.set(p, new net.minecraft.world.item.ItemStack(com.kolodkingal.enlightened.registry.ModItems.AIR_GLIDER.get())), 15);
        shot("259_glider_closed_on_back", 5);
        cmd("gamemode survival", 5);
        STEPS.add(new Step(10, () -> {
            Minecraft.getInstance().options.hideGui = false;
            Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player));
        }));
        shot("256_back_slot_inventory", 5);
        STEPS.add(new Step(5, () -> {
            Minecraft.getInstance().setScreen(null);
            Minecraft.getInstance().options.hideGui = true;
        }));
        cmd("gamemode survival", 5);
        cmd("effect give @s resistance 60 5 true", 2);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
        camera(CameraType.THIRD_PERSON_BACK, 2);
        cmd("tp @s 0 20 0 0 25", 5);
        shot("258_glider_opening", 20);
        shot("260_glider_back", 2);
        camera(CameraType.THIRD_PERSON_FRONT, 4);
        shot("261_glider_front", 2);
        cmd("tp @s 0 20 0 90 -5", 6);
        camera(CameraType.THIRD_PERSON_BACK, 4);
        shot("262_glider_side", 2);
        cmd("tp @s 0 20 0 0 80", 6);
        camera(CameraType.THIRD_PERSON_FRONT, 5);
        shot("263_glider_below", 2);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = false));
        cmd("gamemode creative", 5);
    }

    /** The four brigades' uniforms (the brigade trim on iron armor) and the city of Ellora (alone: -Denlightened.tour.only=havens). */
    private static void sevenHavens() {
        cmd("kill @e[type=armor_stand]", 2);
        String[][] brigades = {{"minecraft:lapis", "-4"}, {"minecraft:gold", "-2"}, {"minecraft:redstone", "0"}, {"enlightened:coal", "2"}, {"minecraft:netherite", "4"}};
        for (String[] b : brigades) {
            String trim = ",components:{\"minecraft:trim\":{pattern:\"enlightened:brigade\",material:\"" + b[0] + "\"}}";
            String[] pieces = {"iron_boots", "iron_leggings", "iron_chestplate", "iron_helmet"};
            StringBuilder armor = new StringBuilder();
            for (String piece : pieces) {
                armor.append(armor.length() == 0 ? "" : ",").append("{id:\"minecraft:").append(piece).append("\",count:1").append(trim).append("}");
            }
            cmd("summon armor_stand " + b[1] + ".5 -60 6.5 {ShowArms:1b,NoBasePlate:1b,Rotation:[180f,0f],ArmorItems:[" + armor + "]}", 4);
        }
        cmd("gamemode spectator", 5);
        cmd("time set noon", 2);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
        cmd("tp @s 0.5 -58.6 0.0 0 12", 30);
        shot("240_brigade_uniforms", 10);
        cmd("tp @s 0.5 -58.6 11.5 180 12", 20);
        shot("241_brigade_uniforms_back", 10);
        cmd("tp @s 3.5 -58.6 3.6 15 12", 20);
        shot("242_brigade_close", 10);
        // Ellora, built round 0, 120 on the flat world.
        STEPS.add(new Step(5, () -> {
            Minecraft.getInstance().options.renderDistance().set(16);
            Minecraft.getInstance().options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        }));
        server(p -> {
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            net.minecraft.core.BlockPos origin = new net.minecraft.core.BlockPos(0, -61, 120);
            com.kolodkingal.enlightened.world.ElloraPiece piece = new com.kolodkingal.enlightened.world.ElloraPiece(origin);
            net.minecraft.world.level.levelgen.structure.BoundingBox all = piece.getBoundingBox();
            for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) {
                for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
                    net.minecraft.world.level.levelgen.structure.BoundingBox box = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                            cx << 4, all.minY(), cz << 4, (cx << 4) + 15, all.maxY(), (cz << 4) + 15);
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box,
                            new net.minecraft.world.level.ChunkPos(cx, cz), origin);
                }
            }
            p.teleportTo(level, 0.5, -10, 230.5, 180.0F, 18.0F);
        }, 60);
        shot("251_ellora_front", 30);
        cmd("tp @s 60.5 -40 140.5 120 10", 60);
        shot("255_ellora_streets", 30);
        cmd("tp @s 0.5 -38 225.5 180 10", 60);
        shot("256_ellora_gate", 30);
        cmd("tp @s 40.5 -20 160.5 135 15", 60);
        shot("252_ellora_terraces", 30);
        cmd("tp @s 0.5 -1 152.5 180 -10", 40);
        shot("253_ellora_crown", 30);
        cmd("tp @s 0.5 -51 115.5 180 10", 40);
        shot("254_ellora_hall", 30);
        cmd("tp @s 110.5 40 230.5 135 25", 160);
        shot("250_ellora_far", 30);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = false));
        cmd("gamemode creative", 5);
    }

    /** A lion turtle in a sea dug for it, and the Avatar State seen through its own eyes (alone: -Denlightened.tour.only=lionturtle). */
    private static void lionTurtle() {
        element("avatar", "aang", "avatar_state", "energybending", "energy_restore", "fire_fist");
        cmd("enlightened guru @s taught", 2);
        cmd("enlightened chakras @s 7", 2);
        cmd("enlightened reset @s", 2);
        cmd("enlightened element set @s fire zuko", 5);
        cmd("gamemode spectator", 5);
        STEPS.add(new Step(5, () -> {
            Minecraft.getInstance().options.renderDistance().set(16);
            Minecraft.getInstance().options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        }));
        // A great lion turtle (size 10) in the open sea, heading north, its centre at 0, 0.
        server(p -> {
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            sea = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 0, 0) - 1;
            Enlightened.LOGGER.info("[tour] sea at {}", sea);
            p.teleportTo(level, 0.5, sea + 120, 260.5, 180.0F, 20.0F);
            com.kolodkingal.enlightened.world.LionTurtles.get(level).add(new net.minecraft.core.BlockPos(0, sea, 0), net.minecraft.core.Direction.NORTH, 10);
        }, 20);
        until(() -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                return false;
            }
            server.execute(() -> islandUp = com.kolodkingal.enlightened.world.LionTurtles.get(server.overworld()).islands().stream()
                    .allMatch(i -> i.phase != com.kolodkingal.enlightened.world.LionTurtles.Phase.BUILD));
            return islandUp;
        }, 6000);
        STEPS.add(new Step(100, () -> Enlightened.LOGGER.info("[tour] the island is up")));
        server(p -> Enlightened.LOGGER.info("[tour] heads on the server: {}", p.serverLevel().getEntities(
                com.kolodkingal.enlightened.registry.ModEntities.LION_TURTLE.get(), e -> true).stream().map(e -> e.blockPosition() + " " + e.getBoundingBox()).toList()), 2);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.renderDistance().set(20)));
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = true));
        // From where it was watched rising (its chunks long sent): the whole island from the south.
        server(p -> p.teleportTo(p.serverLevel(), 0.5, sea + 120, 260.5, 180.0F, 22.0F), 300);
        shot("170_lion_turtle", 10);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.renderDistance().set(8)));
        // (Within the view distance the server knows of: the tour's client asked for 5 chunks.)
        server(p -> p.teleportTo(p.serverLevel(), 25.5, sea + 22, -318.5, 20.0F, 6.0F), 300);
        shot("171_lion_turtle_face", 10);
        STEPS.add(new Step(2, () -> {
            int n = 0;
            for (net.minecraft.world.entity.Entity e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.kolodkingal.enlightened.entity.LionTurtleEntity) {
                    n++;
                }
            }
            Enlightened.LOGGER.info("[tour] heads on the client: {}", n);
        }));
        server(p -> {
            net.minecraft.world.level.ChunkPos c = new net.minecraft.world.level.ChunkPos(0 >> 4, -255 >> 4);
            Enlightened.LOGGER.info("[tour] head chunk in view {} pending {} view distance {} player at {}", p.getChunkTrackingView().contains(c),
                    p.connection.chunkSender.isPending(c.toLong()), p.requestedViewDistance(), p.blockPosition());
        }, 2);
        server(p -> p.teleportTo(p.serverLevel(), -66.5, sea + 30, -262.5, -82.0F, 10.0F), 200);
        shot("171b_lion_turtle_side", 10);
        // On its back, in its forest.
        cmd("gamemode creative", 5);
        server(p -> p.teleportTo(p.serverLevel(), 0.5, sea + 60, 40.5, 180.0F, 10.0F), 40);
        STEPS.add(new Step(60, () -> {
        }));
        shot("172_on_the_shell", 10);
        // It swims a block: every row moved, the one on it with it.
        server(p -> {
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            double z0 = p.getZ();
            var data = com.kolodkingal.enlightened.world.LionTurtles.get(level);
            for (var i : data.islands()) {
                data.swimNow(level, i, net.minecraft.core.Direction.NORTH);
            }
            Enlightened.LOGGER.info("[tour] a stroke: the player went from z {} to {}", z0, p.getZ());
            islandUp = false;
        }, 5);
        until(() -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                server.execute(() -> islandUp = true);
            }
            return islandUp;
        }, 400);
        STEPS.add(new Step(40, () -> {
        }));
        shot("173_on_the_shell_swam", 10);
        // A Hero of the Village, on the rim behind the head, speaks to it.
        cmd("advancement grant @s only minecraft:adventure/hero_of_the_village", 5);
        server(p -> {
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            // Beside its neck, at the very front of the shell (the island swam a block north).
            int z = -1 - 219;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 26, z);
            p.teleportTo(level, 27.5, y, z + 0.5, -90.0F, -10.0F);
            islandUp = false;
        }, 5);
        until(() -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                server.execute(() -> islandUp = true);
            }
            return islandUp;
        }, 400);
        STEPS.add(new Step(40, () -> {
        }));
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        STEPS.add(new Step(14, () -> {
            Minecraft mc = Minecraft.getInstance();
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof com.kolodkingal.enlightened.entity.LionTurtleEntity) {
                    net.minecraft.world.InteractionResult r = mc.gameMode.interact(mc.player, e, net.minecraft.world.InteractionHand.MAIN_HAND);
                    Enlightened.LOGGER.info("[tour] talk to the lion turtle -> {} (distance {})", r, e.distanceTo(mc.player));
                }
            }
        }));
        shot("174_lion_turtle_reaching", 26);
        shot("175_lion_turtle_gift", 30);
        shot("176_lion_turtle_gift_light", 30);
        camera(CameraType.THIRD_PERSON_BACK, 2);
        cmd("gamemode spectator", 5);
        server(p -> p.teleportTo(p.serverLevel(), 60.5, sea + 35, -230.5, 120.0F, 10.0F), 200);
        shot("177_lion_turtle_after", 10);
        STEPS.add(new Step(2, () -> Minecraft.getInstance().options.hideGui = false));
    }

    /** The cosmic plane (also alone, with -Denlightened.tour.only=cosmic). */
    /** A part of the Avatar State, the Anomaly and its path of seven circles (alone: -Denlightened.tour.only=anomaly). */
    private static void anomaly() {
        cmd("time set noon", 2);
        cmd("gamerule doMobSpawning false", 2);
        element("avatar", "korra", "avatar_state", "fire_fist");
        cmd("gamemode creative", 2);
        for (int n : new int[]{1, 4}) {
            server(p -> {
                var data = com.kolodkingal.enlightened.bending.Benders.get(p);
                data.setAvatarUntil(0);
                data.setAvatarCooldownUntil(0);
                data.setGuruTaught(true);
                data.setChakras(n);
                com.kolodkingal.enlightened.bending.Chakras.enterAtWill(p, data, 1.0F);
                com.kolodkingal.enlightened.bending.Benders.syncIfDirty(p);
            }, 30);
            camera(CameraType.THIRD_PERSON_FRONT, 10);
            shot(n == 1 ? "300_avatar_state_first_chakra" : "301_avatar_state_fourth_chakra", 10);
            camera(CameraType.THIRD_PERSON_BACK, 2);
        }
        server(p -> {
            var data = com.kolodkingal.enlightened.bending.Benders.get(p);
            data.setAvatarUntil(0);
            com.kolodkingal.enlightened.bending.Benders.syncIfDirty(p);
            net.minecraft.server.level.ServerLevel level = p.serverLevel();
            net.minecraft.core.BlockPos base = new net.minecraft.core.BlockPos(30, -60, 0);
            for (int[] c : com.kolodkingal.enlightened.world.ElloraPiece.ANOMALY_CUBES) {
                level.setBlock(base.offset(c[0], c[1] - 1, c[2]), com.kolodkingal.enlightened.registry.ModBlocks.ANOMALY_BLOCK.get().defaultBlockState(), 3);
            }
            p.teleportTo(level, 30.5, -59, -12.5, 0.0F, -12.0F);
        }, 30);
        shot("302_anomaly_cubes", 20);
        server(p -> com.kolodkingal.enlightened.event.CommonEvents.wakeAnomaly(p.serverLevel(), new net.minecraft.core.BlockPos(30, -60, 0)), 40);
        shot("303_anomaly", 5);
        cmd("gamemode survival", 2);
        cmd("effect give @s minecraft:resistance 60 4 true", 2);
        cmd("tp @s 30.5 -60 -9.5 0 -28", 2);
        camera(CameraType.FIRST_PERSON, 2);
        for (int i = 0; i < 8; i++) {
            shot("304_anomaly_strikes_" + i, 7);
        }
        camera(CameraType.THIRD_PERSON_FRONT, 2);
        for (int i = 0; i < 6; i++) {
            shot("304b_anomaly_front_" + i, 9);
        }
        camera(CameraType.THIRD_PERSON_BACK, 2);
        cmd("gamemode creative", 2);
        cmd("kill @e[type=enlightened:anomaly]", 10);
        server(com.kolodkingal.enlightened.world.Cosmic::sendForAnomaly, 100);
        camera(CameraType.FIRST_PERSON, 2);
        shot("305_anomaly_path", 20);
        cmd("tp @s " + com.kolodkingal.enlightened.world.Cosmic.ANOMALY_X + ".5 101 30.5 0 0", 30);
        shot("306_seven_circles", 20);
    }

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
