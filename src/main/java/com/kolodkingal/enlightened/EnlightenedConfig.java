package com.kolodkingal.enlightened;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side balance numbers. Lives in world/serverconfig/enlightened-server.toml and is synced to clients. Every
 * number an ability uses that a server might want to change is here; the sections follow the elements.
 */
public final class EnlightenedConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ---- general ----
    public static final ModConfigSpec.IntValue STARTING_POINTS;
    public static final ModConfigSpec.IntValue XP_PER_POINT;
    public static final ModConfigSpec.IntValue BOSS_XP;
    public static final ModConfigSpec.IntValue PLAYER_XP;
    public static final ModConfigSpec.BooleanValue HURT_PASSIVES;
    public static final ModConfigSpec.BooleanValue HURT_EVERYTHING;
    public static final ModConfigSpec.BooleanValue SCROLLS_IN_CHESTS;
    public static final ModConfigSpec.IntValue RAISED_BLOCK_SECONDS;

    // ---- earth ----
    public static final ModConfigSpec.IntValue SAND_GATE;
    public static final ModConfigSpec.IntValue LAVA_GATE;
    public static final ModConfigSpec.IntValue METAL_GATE;
    public static final ModConfigSpec.DoubleValue ROCK_DAMAGE;
    public static final ModConfigSpec.IntValue ROCK_RANGE;
    public static final ModConfigSpec.IntValue ROCK_COOLDOWN;
    public static final ModConfigSpec.BooleanValue ROCK_LIFT_TAKES_BLOCKS;
    public static final ModConfigSpec.DoubleValue EARTH_ARMOR_SLOW;
    public static final ModConfigSpec.IntValue WALL_SECONDS;
    public static final ModConfigSpec.IntValue WALL_COOLDOWN;
    public static final ModConfigSpec.IntValue LAVA_SECONDS;
    public static final ModConfigSpec.DoubleValue METAL_BLADE_DAMAGE;
    public static final ModConfigSpec.IntValue METAL_PULL_RADIUS;
    public static final ModConfigSpec.BooleanValue WRECK_DROPS;

    // ---- air ----
    public static final ModConfigSpec.DoubleValue AIR_BLAST_PUSH;
    public static final ModConfigSpec.IntValue AIR_BLAST_COOLDOWN;
    public static final ModConfigSpec.DoubleValue AIR_SHIELD_STRENGTH;
    public static final ModConfigSpec.DoubleValue AIR_SHIELD_BURST_DAMAGE;
    public static final ModConfigSpec.DoubleValue AIR_BLADE_DAMAGE;
    public static final ModConfigSpec.IntValue PROJECTION_SECONDS;
    public static final ModConfigSpec.IntValue PROJECTION_RANGE;
    public static final ModConfigSpec.BooleanValue GLIDER_NEEDED;
    public static final ModConfigSpec.BooleanValue PET_KILL_SEVERS_TETHERS;
    public static final ModConfigSpec.DoubleValue TETHERLESS_DAMAGE_BONUS;

    // ---- water ----
    public static final ModConfigSpec.IntValue ICE_GATE;
    public static final ModConfigSpec.IntValue BLOOD_GATE;
    public static final ModConfigSpec.IntValue WATER_SOURCE_RANGE;
    public static final ModConfigSpec.IntValue WATER_SKIN_CHARGES;
    public static final ModConfigSpec.DoubleValue WATER_WHIP_DAMAGE;
    public static final ModConfigSpec.DoubleValue WATER_HEAL;
    public static final ModConfigSpec.DoubleValue ICE_KNIFE_DAMAGE;
    public static final ModConfigSpec.BooleanValue BLOOD_NEEDS_FULL_MOON;
    public static final ModConfigSpec.IntValue BLOOD_SECONDS;
    public static final ModConfigSpec.IntValue SUPPRESS_NOVICE_MINUTES;
    public static final ModConfigSpec.IntValue SUPPRESS_SKILLED_SECONDS;

    // ---- fire ----
    public static final ModConfigSpec.IntValue LIGHTNING_GATE;
    public static final ModConfigSpec.DoubleValue FIRE_BALL_DAMAGE;
    public static final ModConfigSpec.DoubleValue FIRE_STREAM_DPS;
    public static final ModConfigSpec.DoubleValue FIRE_WHIP_DAMAGE;
    public static final ModConfigSpec.DoubleValue FIRE_RING_DAMAGE;
    public static final ModConfigSpec.DoubleValue FIRE_HEAL;
    public static final ModConfigSpec.DoubleValue LIGHTNING_DAMAGE;
    public static final ModConfigSpec.DoubleValue LIGHTNING_TWO_HANDS_DAMAGE;
    public static final ModConfigSpec.IntValue LIGHTNING_COOLDOWN;
    public static final ModConfigSpec.IntValue LIGHTNING_STUN_TICKS;
    public static final ModConfigSpec.IntValue COMBUSTION_GATE;
    public static final ModConfigSpec.DoubleValue COMBUSTION_DAMAGE;
    public static final ModConfigSpec.BooleanValue COMBUSTION_BREAKS_BLOCKS;
    public static final ModConfigSpec.IntValue FIRE_WALL_SECONDS;

    // ---- chakras, the Avatar State, combos, steps ----
    public static final ModConfigSpec.IntValue CHAKRA_MEDITATION_SECONDS;
    public static final ModConfigSpec.IntValue CHAKRA_POINTS_EACH;
    public static final ModConfigSpec.IntValue MEDITATION_XP_SECONDS;
    public static final ModConfigSpec.IntValue AVATAR_STATE_SECONDS;
    public static final ModConfigSpec.IntValue AVATAR_STATE_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue AVATAR_STATE_ON_LOW_HEALTH;
    public static final ModConfigSpec.BooleanValue COMBOS;
    public static final ModConfigSpec.IntValue COMBO_WINDOW_TICKS;
    public static final ModConfigSpec.BooleanValue STEPS;

    static {
        B.push("general");
        STARTING_POINTS = B.comment("Ability points a new bender starts with (the starter ability is free on top of them).")
                .defineInRange("startingPoints", 1, 0, 100);
        XP_PER_POINT = B.comment("Kills with bending (one experience each; a boss gives bossXp) needed for one ability point.")
                .defineInRange("xpPerPoint", 3, 1, 100);
        BOSS_XP = B.comment("Experience a boss (the Wither, the Ender Dragon, the Warden, an Elder Guardian) gives.")
                .defineInRange("bossXp", 10, 1, 1000);
        PLAYER_XP = B.comment("Experience another player gives (with PvP on).").defineInRange("playerXp", 2, 0, 100);
        HURT_PASSIVES = B.comment("Whether bending hurts creatures that are not hostile (animals, villagers, golems); the bender's own pets and teammates are always spared.")
                .define("hurtPassives", true);
        HURT_EVERYTHING = B.comment("Whether bending hurts every creature in its way: the bender's own pets, his teammates, villagers, golems, everything alive but himself (players still by the server's PvP rule). On, hurtPassives does not matter.")
                .define("hurtEverything", true);
        SCROLLS_IN_CHESTS = B.comment("Whether the bending scrolls are found in chests (besides being crafted).")
                .define("scrollsInChests", true);
        RAISED_BLOCK_SECONDS = B.comment("How long a wall or pillar bending raised stands before it goes back.")
                .defineInRange("raisedBlockSeconds", 10, 1, 600);
        B.pop();

        B.push("earth");
        SAND_GATE = B.comment("Points spent on earth before sand bending can be learned.").defineInRange("sandGate", 3, 0, 100);
        LAVA_GATE = B.comment("Points spent on earth before lava bending can be learned.").defineInRange("lavaGate", 6, 0, 100);
        METAL_GATE = B.comment("Points spent on earth before metal bending can be learned.").defineInRange("metalGate", 9, 0, 100);
        ROCK_DAMAGE = B.comment("Damage of one thrown rock (plus one per level).").defineInRange("rockDamage", 4.0, 0.0, 100.0);
        ROCK_RANGE = B.comment("How far rocks and blades are aimed.").defineInRange("rockRange", 24, 4, 64);
        ROCK_COOLDOWN = B.comment("Ticks between two rock throws.").defineInRange("rockCooldown", 20, 0, 1200);
        ROCK_LIFT_TAKES_BLOCKS = B.comment("Whether Rock Lift tears the block out of the ground (false: it is conjured and the ground stays).")
                .define("rockLiftTakesBlocks", true);
        EARTH_ARMOR_SLOW = B.comment("How much slower an earthbender walks in earth armour at level 1 (0.3 = 30%).")
                .defineInRange("earthArmorSlow", 0.3, 0.0, 0.9);
        WALL_SECONDS = B.comment("How long an earth, sand or metal wall stands.").defineInRange("wallSeconds", 10, 1, 600);
        WALL_COOLDOWN = B.comment("Ticks before another wall can be raised.").defineInRange("wallCooldown", 200, 0, 6000);
        LAVA_SECONDS = B.comment("How long a lava path flows before the ground sets again.").defineInRange("lavaSeconds", 8, 1, 120);
        METAL_BLADE_DAMAGE = B.comment("Damage of one metal blade (plus one per level).").defineInRange("metalBladeDamage", 6.0, 0.0, 100.0);
        METAL_PULL_RADIUS = B.comment("Radius of the metal pull at level 1 (it grows with the level).").defineInRange("metalPullRadius", 6, 1, 32);
        WRECK_DROPS = B.comment("Whether a wrecked metal device drops its item.").define("wreckDrops", true);
        B.pop();

        B.push("air");
        AIR_BLAST_PUSH = B.comment("How far an air blast throws at level 1 (it grows with the level).").defineInRange("airBlastPush", 1.6, 0.0, 10.0);
        AIR_BLAST_COOLDOWN = B.comment("Ticks between two air blasts.").defineInRange("airBlastCooldown", 20, 0, 1200);
        AIR_SHIELD_STRENGTH = B.comment("Damage an air shield takes before it breaks at level 1.").defineInRange("airShieldStrength", 10.0, 1.0, 1000.0);
        AIR_SHIELD_BURST_DAMAGE = B.comment("Damage the burst of a breaking shield does (7 = three and a half hearts).")
                .defineInRange("airShieldBurstDamage", 7.0, 0.0, 100.0);
        AIR_BLADE_DAMAGE = B.comment("Damage of one air blade (5 = two and a half hearts).").defineInRange("airBladeDamage", 5.0, 0.0, 100.0);
        PROJECTION_SECONDS = B.comment("How long a spirit projection lasts at level 1 (it grows with the level).").defineInRange("projectionSeconds", 20, 5, 600);
        PROJECTION_RANGE = B.comment("How far the spirit may go from the body at level 1 (it grows with the level).").defineInRange("projectionRange", 32, 8, 512);
        GLIDER_NEEDED = B.comment("Whether gliding needs the Air Glider in hand (false: every airbender glides).").define("gliderNeeded", true);
        PET_KILL_SEVERS_TETHERS = B.comment("Whether an airbender who kills his own pet lets go of his earthly tethers (flight, suffocation).")
                .define("petKillSeversTethers", true);
        TETHERLESS_DAMAGE_BONUS = B.comment("Extra damage of a tetherless airbender's blasts and blades.").defineInRange("tetherlessDamageBonus", 2.0, 0.0, 20.0);
        B.pop();

        B.push("water");
        ICE_GATE = B.comment("Points spent on water before ice bending can be learned.").defineInRange("iceGate", 3, 0, 100);
        BLOOD_GATE = B.comment("Points spent on water before bloodbending can be learned.").defineInRange("bloodGate", 9, 0, 100);
        WATER_SOURCE_RANGE = B.comment("How close water must be to bend it.").defineInRange("waterSourceRange", 6, 1, 32);
        WATER_SKIN_CHARGES = B.comment("Uses a full water skin holds.").defineInRange("waterSkinCharges", 10, 1, 100);
        WATER_WHIP_DAMAGE = B.comment("Damage of a water whip (plus one per level).").defineInRange("waterWhipDamage", 3.0, 0.0, 100.0);
        WATER_HEAL = B.comment("Health water healing restores at level 1 (it grows with the level).").defineInRange("waterHeal", 4.0, 0.0, 100.0);
        ICE_KNIFE_DAMAGE = B.comment("Damage of one ice knife.").defineInRange("iceKnifeDamage", 4.0, 0.0, 100.0);
        BLOOD_NEEDS_FULL_MOON = B.comment("Whether bloodbending below mastery needs a full moon.").define("bloodNeedsFullMoon", true);
        BLOOD_SECONDS = B.comment("How long a bloodbender can hold someone at level 1 (it grows with the level).").defineInRange("bloodSeconds", 6, 1, 120);
        SUPPRESS_NOVICE_MINUTES = B.comment("Minutes a master bloodbender takes the bending of a weak bender for.").defineInRange("suppressNoviceMinutes", 10, 1, 120);
        SUPPRESS_SKILLED_SECONDS = B.comment("Seconds a master bloodbender takes the bending of a skilled bender for.").defineInRange("suppressSkilledSeconds", 90, 1, 3600);
        B.pop();

        B.push("fire");
        LIGHTNING_GATE = B.comment("Points spent on fire before lightning can be learned.").defineInRange("lightningGate", 6, 0, 100);
        FIRE_BALL_DAMAGE = B.comment("Damage of a fire ball at level 1 (it grows with the level).").defineInRange("fireBallDamage", 4.0, 0.0, 100.0);
        FIRE_STREAM_DPS = B.comment("Damage a second of the fire fist's stream at level 1.").defineInRange("fireStreamDps", 4.0, 0.0, 100.0);
        FIRE_WHIP_DAMAGE = B.comment("Damage of one lash of the fire whip.").defineInRange("fireWhipDamage", 3.0, 0.0, 100.0);
        FIRE_RING_DAMAGE = B.comment("Damage of the fire ring at level 1 (it grows with the level).").defineInRange("fireRingDamage", 4.0, 0.0, 100.0);
        FIRE_HEAL = B.comment("Health fire healing restores at level 1 (weaker than water).").defineInRange("fireHeal", 3.0, 0.0, 100.0);
        LIGHTNING_DAMAGE = B.comment("Damage of a lightning bolt (9 = four and a half hearts).").defineInRange("lightningDamage", 9.0, 0.0, 100.0);
        LIGHTNING_TWO_HANDS_DAMAGE = B.comment("Damage of a master's two-handed lightning (12 = six hearts).").defineInRange("lightningTwoHandsDamage", 12.0, 0.0, 100.0);
        LIGHTNING_COOLDOWN = B.comment("Ticks before lightning can be cast again (600 = 30 s; a master waits two thirds).").defineInRange("lightningCooldown", 600, 0, 12000);
        LIGHTNING_STUN_TICKS = B.comment("Ticks the struck creature cannot move (40 = 2 s).").defineInRange("lightningStunTicks", 40, 0, 1200);
        COMBUSTION_GATE = B.comment("Points spent on fire before combustion can be learned.").defineInRange("combustionGate", 9, 0, 100);
        COMBUSTION_DAMAGE = B.comment("Damage at the heart of a combustion blast at level 1 (it grows with the level).").defineInRange("combustionDamage", 10.0, 0.0, 200.0);
        COMBUSTION_BREAKS_BLOCKS = B.comment("Whether a combustion blast breaks blocks (the wrecked blocks drop).").define("combustionBreaksBlocks", false);
        FIRE_WALL_SECONDS = B.comment("How long a wall of fire burns at level 1 (it grows with the level).").defineInRange("fireWallSeconds", 6, 1, 120);
        B.pop();

        B.push("avatar");
        CHAKRA_MEDITATION_SECONDS = B.comment("Seconds of meditation that open one chakra (a third of it on a Meditation Point).")
                .defineInRange("chakraMeditationSeconds", 60, 1, 3600);
        CHAKRA_POINTS_EACH = B.comment("Points spent on the element that each chakra asks for (the n-th chakra needs n times this).")
                .defineInRange("chakraPointsEach", 2, 0, 50);
        MEDITATION_XP_SECONDS = B.comment("Seconds of meditation that earn one experience (0: meditation earns none).").defineInRange("meditationXpSeconds", 15, 0, 3600);
        AVATAR_STATE_SECONDS = B.comment("How long the Avatar State lasts.").defineInRange("avatarStateSeconds", 12, 1, 600);
        AVATAR_STATE_COOLDOWN_SECONDS = B.comment("Seconds before the Avatar State can be entered again.").defineInRange("avatarStateCooldownSeconds", 300, 0, 86400);
        AVATAR_STATE_ON_LOW_HEALTH = B.comment("Whether a bender with all seven chakras open enters the Avatar State by himself when near death.")
                .define("avatarStateOnLowHealth", true);
        COMBOS = B.comment("Whether sequences of casts trigger the combos (Twister, Earth Dome, Ice Wave, Fire Spin...).").define("combos", true);
        COMBO_WINDOW_TICKS = B.comment("Ticks between the casts of a combo (60 = 3 s).").defineInRange("comboWindowTicks", 60, 10, 600);
        STEPS = B.comment("Whether a double tap of a direction dashes a bender who learned his element's step.").define("steps", true);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private EnlightenedConfig() {
    }
}
