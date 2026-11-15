package com.kolodkingal.enlightened.client.dev;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Variant;
import com.kolodkingal.enlightened.bending.Variants;
import com.kolodkingal.enlightened.client.KeyBindings;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A development tool (1.0.36, off by default), unlike the tour made to run on every version the jar supports: it makes a
 * world and runs the game through the mod, writing {@code Probe:} lines so that an error in the log can be put to what
 * was being done. {@code -Denlightened.probe=all}: every element, every ability of it learned to the full and cast from
 * the R key (a press, a hold, with Sneak), and every look; {@code =world}: every creature of the mod summoned, every
 * realm of the mod visited, and a long flight for new chunks (and the mod's places in them). Screenshots
 * {@code probe_*.png}; it quits at the end. {@code -Denlightened.probe.from=<element>} starts at that element.
 */
public final class Probe {
    private static final String MODE = System.getProperty("enlightened.probe", "");

    private record Step(int ticks, String name, Consumer<LocalPlayer> each) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static final List<String> REALMS = new ArrayList<>();
    private static boolean asked;
    private static boolean scripted;
    private static int settle;
    private static int index = -1;
    private static int left;

    private Probe() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (MODE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (scripted) {
                // (thrown out of the world: as bad as a crash to a player)
                Enlightened.LOGGER.error("Probe: thrown out of the world at step {} ({})", index,
                        index >= 0 && index < STEPS.size() ? STEPS.get(index).name() : "-");
                mc.stop();
                return;
            }
            if (!asked && mc.screen instanceof TitleScreen) {
                asked = true;
                mc.options.chatOpacity().set(0.0);
                //#if >=26.3
                //$$ LevelSettings settings = new LevelSettings("probe", GameType.CREATIVE,
                //$$         new LevelSettings.DifficultySettings(Difficulty.EASY, false, false), true, WorldDataConfiguration.DEFAULT);
                //$$ mc.createWorldOpenFlows().createFreshLevel("probe", settings, new WorldOptions(4242L, true, false),
                //$$         access -> access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value()
                //$$                 .createWorldDimensions(), mc.screen);
                //#else
                LevelSettings settings = new LevelSettings("probe", GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("probe", settings, new WorldOptions(4242L, true, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value()
                                .createWorldDimensions(), mc.screen);
                //#endif
            }
            return;
        }
        if (settle++ < 100) {
            return;
        }
        if (!scripted) {
            scripted = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (MODE.equals("world")) {
                world();
            } else {
                all();
            }
            Enlightened.LOGGER.info("Probe: {} steps", STEPS.size());
        }
        if (left <= 0) {
            index++;
            if (index >= STEPS.size()) {
                Enlightened.LOGGER.info("Probe: the end");
                mc.stop();
                return;
            }
            left = STEPS.get(index).ticks();
        }
        left--;
        try {
            STEPS.get(index).each().accept(mc.player);
        } catch (RuntimeException e) {
            Enlightened.LOGGER.error("Probe: step {} failed", STEPS.get(index).name(), e);
        }
    }

    // ---- the steps ----

    private static void step(int ticks, String name, Consumer<LocalPlayer> each) {
        STEPS.add(new Step(ticks, name, each));
    }

    private static void once(String name, Consumer<LocalPlayer> action) {
        boolean[] done = {false};
        STEPS.add(new Step(1, name, p -> {
            if (!done[0]) {
                done[0] = true;
                action.accept(p);
            }
        }));
    }

    private static void waitTicks(int ticks) {
        step(ticks, "wait", p -> {
        });
    }

    /** Runs on the server thread, as the player; then waits. */
    private static void server(String name, Consumer<ServerPlayer> action, int wait) {
        once(name, local -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                return;
            }
            java.util.UUID id = local.getUUID();
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    try {
                        action.accept(p);
                    } catch (RuntimeException e) {
                        Enlightened.LOGGER.error("Probe: server step {} failed", name, e);
                    }
                }
            });
        });
        waitTicks(wait);
    }

    private static void command(ServerPlayer p, String command) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), command);
    }

    private static void shot(String name) {
        once(name, p -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {
            });
        });
        waitTicks(8);
    }

    private static void camera(CameraType type) {
        once("camera " + type, p -> Minecraft.getInstance().options.setCameraType(type));
    }

    /** Key 1 (R) pressed as the keyboard would, held {@code hold} ticks, with Sneak or not. */
    private static void key(String what, int hold, boolean sneak) {
        once(what, p -> {
            Enlightened.LOGGER.info("Probe: {}", what);
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                mc.setScreen(null);
            }
            mc.options.keyShift.setDown(sneak);
            KeyMapping k = KeyBindings.ability(1);
            k.setDown(true);
            KeyMapping.click(k.getKey());
        });
        step(hold, what + " (held)", p -> {
            Minecraft.getInstance().options.keyShift.setDown(sneak);
            KeyBindings.ability(1).setDown(true);
        });
        once(what + " (let go)", p -> {
            KeyBindings.ability(1).setDown(false);
            Minecraft.getInstance().options.keyShift.setDown(false);
        });
    }

    /** The place every element is tried at: grass, with water, sand, stone, iron and lava before it, and a target. */
    private static void scene(ServerPlayer p) {
        command(p, "execute in minecraft:overworld run tp @s 0 100 0 0 15");
        command(p, "execute in minecraft:overworld run kill @e[type=!minecraft:player,distance=..60]");
        command(p, "execute in minecraft:overworld run fill -16 98 -16 16 98 16 minecraft:stone");
        command(p, "execute in minecraft:overworld run fill -16 99 -16 16 99 16 minecraft:grass_block");
        command(p, "execute in minecraft:overworld run fill -16 100 -16 16 114 16 minecraft:air");
        command(p, "execute in minecraft:overworld run fill -3 99 8 3 99 12 minecraft:water");
        command(p, "execute in minecraft:overworld run fill 5 99 3 8 99 6 minecraft:sand");
        command(p, "execute in minecraft:overworld run fill -8 99 3 -6 99 5 minecraft:lava");
        command(p, "execute in minecraft:overworld run setblock -4 100 2 minecraft:iron_block");
        command(p, "execute in minecraft:overworld run setblock -4 100 4 minecraft:iron_block");
        command(p, "execute in minecraft:overworld run summon minecraft:zombie 0 100 6 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        command(p, "effect clear @s");
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.getAbilities().invulnerable = true;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    // ---- "all": every element, ability and look ----

    private static void all() {
        String from = System.getProperty("enlightened.probe.from", "");
        server("setup", p -> {
            command(p, "gamerule doMobSpawning false");
            command(p, "gamerule doDaylightCycle false");
            command(p, "gamerule doWeatherCycle false");
            command(p, "time set 6000");
            command(p, "weather clear");
            p.setGameMode(GameType.SURVIVAL);
        }, 10);
        boolean started = from.isEmpty();
        for (Element element : Element.values()) {
            if (element == Element.NONE) {
                continue;
            }
            String e = element.name().toLowerCase(Locale.ROOT);
            if (!started && !e.equals(from)) {
                continue;
            }
            started = true;
            server(e + " start", p -> {
                Enlightened.LOGGER.info("Probe: === {} ===", e);
                scene(p);
                command(p, "enlightened element set @s " + e);
                command(p, "enlightened points add @s 999");
                for (Ability a : Ability.values()) {
                    if (element == Element.AVATAR || a.element == element) {
                        command(p, "enlightened learn @s " + a.id() + " " + Ability.MAX_LEVEL);
                    }
                }
            }, 40);
            for (Ability a : Ability.values()) {
                if (!(element == Element.AVATAR ? a.element == Element.AVATAR : a.element == element) || !a.bindable()) {
                    continue;
                }
                String what = e + " " + a.id();
                server(what + " on R", p -> {
                    scene(p);
                    command(p, "enlightened loadout @s 1 " + a.id());
                }, 12);
                once(what + " look", p -> {
                    p.setYRot(0.0F);
                    p.setXRot(15.0F);
                });
                key(what + " press", 3, false);
                waitTicks(30);
                shot("probe_" + e + "_" + a.id());
                key(what + " hold", 40, false);
                waitTicks(25);
                key(what + " sneak", 3, true);
                waitTicks(25);
                // a toggled ability is put off again (a second press), so the next one starts clean
                if (a.kind == Ability.Kind.TOGGLE) {
                    key(what + " off", 3, false);
                    waitTicks(10);
                }
            }
            // the looks, from the front
            server(e + " looks", p -> scene(p), 10);
            camera(CameraType.THIRD_PERSON_FRONT);
            for (Variant look : Variants.of(element)) {
                server(e + " look " + look.id(), p -> {
                    Enlightened.LOGGER.info("Probe: {} look {}", e, look.id());
                    command(p, "enlightened element set @s " + e + " " + look.id());
                }, 15);
                shot("probe_" + e + "_look_" + look.id());
            }
            camera(CameraType.THIRD_PERSON_BACK);
            server(e + " done", p -> Enlightened.LOGGER.info("Probe: {} done", e), 2);
        }
    }

    // ---- "world": the mod's creatures, realms and places ----

    private static void world() {
        server("setup", p -> {
            command(p, "gamerule doMobSpawning false");
            command(p, "time set 6000");
            scene(p);
            command(p, "enlightened element set @s air");
        }, 20);
        camera(CameraType.FIRST_PERSON);
        List<String> types = new ArrayList<>();
        for (var key : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (key.getNamespace().equals(Enlightened.MODID)) {
                types.add(key.toString());
            }
        }
        Collections.sort(types);
        for (String type : types) {
            server("summon " + type, p -> {
                Enlightened.LOGGER.info("Probe: summon {}", type);
                command(p, "execute in minecraft:overworld run kill @e[type=!minecraft:player,distance=..60]");
                command(p, "execute in minecraft:overworld run tp @s 0 100 0 0 10");
                command(p, "execute in minecraft:overworld run summon " + type + " 0 100 5");
            }, 40);
            shot("probe_world_" + type.replace(':', '_'));
        }
        server("realms", p -> {
            for (var key : p.server.levelKeys()) {
                if (key.location().getNamespace().equals(Enlightened.MODID)) {
                    REALMS.add(key.location().toString());
                }
            }
            Enlightened.LOGGER.info("Probe: realms {}", REALMS);
        }, 5);
        camera(CameraType.THIRD_PERSON_BACK);
        for (int i = 0; i < 8; i++) {
            int n = i;
            server("realm " + i, p -> {
                if (n < REALMS.size()) {
                    Enlightened.LOGGER.info("Probe: realm {}", REALMS.get(n));
                    command(p, "execute in " + REALMS.get(n) + " run tp @s 0 120 0");
                    p.getAbilities().mayfly = true;
                    p.getAbilities().flying = true;
                    p.onUpdateAbilities();
                }
            }, 200);
            shot("probe_world_realm_" + i);
        }
        server("flight", p -> {
            Enlightened.LOGGER.info("Probe: the long flight");
            command(p, "execute in minecraft:overworld run tp @s 0 160 0");
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }, 20);
        for (int i = 1; i <= 8; i++) {
            int n = i;
            server("far " + i, p -> {
                Enlightened.LOGGER.info("Probe: far {}", n);
                command(p, "execute in minecraft:overworld run tp @s " + (n * 700) + " 170 " + (-n * 500));
            }, 300);
            shot("probe_world_far_" + i);
        }
        server("scroll", p -> Enlightened.LOGGER.info("Probe: the Bending Scroll"), 5);
        once("open the scroll", p -> {
            KeyMapping k = KeyBindings.SCROLL;
            KeyMapping.click(k.getKey());
        });
        waitTicks(30);
        shot("probe_world_scroll");
        once("close", p -> Minecraft.getInstance().setScreen(null));
        once("open the wheel", p -> {
            KeyMapping.click(KeyBindings.WHEEL.getKey());
            KeyBindings.WHEEL.setDown(true);
        });
        waitTicks(20);
        shot("probe_world_wheel");
        once("let the wheel go", p -> {
            KeyBindings.WHEEL.setDown(false);
            Minecraft.getInstance().setScreen(null);
        });
        waitTicks(10);
    }
}
