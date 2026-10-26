package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.air.Projection;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.entity.MeditatingBodyEntity;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.FLOOR;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of the four elements, the scrolls, the progression and the loadout. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class EnlightenedTests {

    // ---- scrolls, points, learning ----

    @GameTest(template = ARENA)
    public static void scrollAwakensAnEarthbender(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 10);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EARTH_SCROLL.get()));
        ModItems.EARTH_SCROLL.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        BendingData data = Benders.get(player);
        check(data.element() == Element.EARTH, "the scroll makes an earthbender");
        check(data.level(Ability.ROCK_THROW) == 1, "the starter ability is learned");
        check(data.points() == EnlightenedConfig.STARTING_POINTS.get(), "the starting points are given");
        check(data.slot(0) == Ability.ROCK_THROW, "the starter sits on the first key");
        check(player.getMainHandItem().isEmpty(), "the scroll is used up");
        // Another element's scroll is not for him.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIRE_SCROLL.get()));
        ModItems.FIRE_SCROLL.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        check(Benders.element(player) == Element.EARTH, "a bender cannot read another element's scroll");
        check(!player.getMainHandItem().isEmpty(), "the other scroll is kept");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void killsWithBendingEarnPoints(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 10);
        BendingData data = f.bender(player, Element.FIRE);
        int before = data.points();
        int per = Mastery.xpPerPoint();
        for (int i = 0; i < per; i++) {
            Mob zombie = f.zombie(10, FLOOR, 14);
            zombie.hurt(ModDamageTypes.source(player.level(), ModDamageTypes.FIRE, player, player), 1000F);
            check(zombie.isDeadOrDying() || !zombie.isAlive(), "the zombie died of bending");
        }
        check(data.points() == before + 1, "every xpPerPoint kills give a point: " + data.points());
        check(data.xp() == 0, "the experience is spent");
        // A kill by hand earns nothing.
        Mob zombie = f.zombie(10, FLOOR, 14);
        zombie.hurt(player.damageSources().playerAttack(player), 1000F);
        check(data.xp() == 0 && data.points() == before + 1, "a kill without bending earns nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void learningSpendsPointsAndGatesSubBending(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 10);
        BendingData data = f.bender(player, Element.EARTH);
        data.setPoints(5);
        check(!Mastery.learn(player, Ability.SAND_WALL), "sand needs points spent on earth first");
        check(Mastery.learn(player, Ability.EARTH_ARMOR), "earth armour can be learned");
        check(data.level(Ability.EARTH_ARMOR) == 1 && data.points() == 4, "level 1 costs one point");
        check(Mastery.learn(player, Ability.EARTH_ARMOR), "the second level");
        check(data.level(Ability.EARTH_ARMOR) == 2 && data.points() == 2, "level 2 costs two points");
        check(data.spent() == 4, "the points spent are counted (starter + 1 + 2)");
        check(Mastery.learn(player, Ability.SAND_WALL), "the sand gate is open at " + EnlightenedConfig.SAND_GATE.get());
        check(data.slotOf(Ability.SAND_WALL) >= 0, "a learned ability takes a key");
        check(!Mastery.learn(player, Ability.FIRE_BALL), "another element's art cannot be learned");
        check(!Mastery.learn(player, Ability.FLIGHT), "flight is for the tetherless");
        Mastery.reset(player);
        check(data.level(Ability.EARTH_ARMOR) == 0 && data.level(Ability.ROCK_THROW) == 1, "a reset keeps only the starter");
        check(data.points() == 5, "a reset refunds the points");
        helper.succeed();
    }

    // ---- earth ----

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void earthWallRisesAndCrumbles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        f.bender(player, Element.EARTH, Ability.EARTH_WALL, 1);
        f.lookAt(player, new Vec3(10, FLOOR + 1, 20));
        f.press(player, Ability.EARTH_WALL, false);
        BlockPos wall = helper.absolutePos(new BlockPos(10, (int) FLOOR, 10));
        check(!helper.getLevel().getBlockState(wall).isAir(), "a wall rose two blocks ahead");
        check(helper.getLevel().getBlockState(wall.above(2)).is(Blocks.SMOOTH_STONE), "three blocks high, of the floor's stone");
        check(helper.getLevel().getBlockState(wall.east(2)).isAir(), "a novice's wall is three wide");
        check(TempBlocks.count() > 0, "the wall is remembered to be put back");
        int ticks = EnlightenedConfig.WALL_SECONDS.get() * 20 + 40;
        helper.runAtTickTime(ticks, () -> {
            check(helper.getLevel().getBlockState(wall).isAir(), "the wall crumbled after its time");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void rockThrowHurtsAZombie(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6);
        f.bender(player, Element.EARTH);
        Mob zombie = f.zombie(10, FLOOR, 13);
        float health = zombie.getHealth();
        f.lookAt(player, zombie);
        f.press(player, Ability.ROCK_THROW, false);
        check(!helper.getLevel().getEntitiesOfClass(BendingShotEntity.class, player.getBoundingBox().inflate(6)).isEmpty(), "a rock was thrown");
        check(!Benders.get(player).isReady(Ability.ROCK_THROW, player.level().getGameTime()), "the throw has a cooldown");
        helper.runAtTickTime(30, () -> {
            check(zombie.getHealth() < health, "the rock hurt the zombie");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void quicksandSparesTheStrongerEarthbender(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer novice = f.player(10, FLOOR, 6);
        f.bender(novice, Element.EARTH, Ability.QUICKSAND, 1);
        ServerPlayer master = f.player(10, FLOOR, 10);
        f.bender(master, Element.EARTH, Ability.EARTH_ARMOR, 3, Ability.EARTH_WALL, 3);
        helper.setBlock(new BlockPos(10, 1, 10), Blocks.SAND);
        f.lookAt(novice, master);
        f.press(novice, Ability.QUICKSAND, false);
        check(!master.hasEffect(ModEffects.QUICKSAND), "the stronger earthbender does not sink");
        // A zombie on sand does (off to the side, so the master is not in the way of the aim).
        Mob zombie = f.zombie(15, FLOOR, 6);
        helper.setBlock(new BlockPos(15, 1, 6), Blocks.SAND);
        Benders.get(novice).clearCooldown(Ability.QUICKSAND);
        f.lookAt(novice, zombie);
        f.press(novice, Ability.QUICKSAND, false);
        check(zombie.hasEffect(ModEffects.QUICKSAND), "the zombie sinks");
        helper.succeed();
    }

    // ---- air ----

    @GameTest(template = ARENA)
    public static void airBlastThrowsBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 7);
        f.bender(player, Element.AIR);
        Mob zombie = f.zombie(10, FLOOR, 10);
        f.lookAt(player, zombie);
        f.press(player, Ability.AIR_BLAST, false);
        check(zombie.getDeltaMovement().z > 0.5, "the zombie is thrown away: " + zombie.getDeltaMovement());
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void spiritProjectionLeavesABodyAndReturnsWhenHurt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 10);
        f.bender(player, Element.AIR, Ability.SPIRIT_PROJECTION, 1);
        helper.runAtTickTime(5, () -> {
            check(player.onGround(), "the player stands on the floor");
            f.press(player, Ability.SPIRIT_PROJECTION, false);
            check(Projection.isProjecting(player), "the spirit left");
            check(player.isSpectator(), "the spirit is a spectator");
            check(!helper.getLevel().getEntitiesOfClass(MeditatingBodyEntity.class, player.getBoundingBox().inflate(3)).isEmpty(), "the body stays behind");
        });
        helper.runAtTickTime(15, () -> {
            MeditatingBodyEntity body = helper.getLevel().getEntitiesOfClass(MeditatingBodyEntity.class, player.getBoundingBox().inflate(3)).getFirst();
            body.hurt(player.damageSources().generic(), 2.0F);
            check(!Projection.isProjecting(player), "a blow on the body calls the spirit back");
            check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "the player is in his body again");
            check(player.getHealth() < player.getMaxHealth(), "and the blow landed on him");
            helper.succeed();
        });
    }

    // ---- water ----

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void waterWhipNeedsWater(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6);
        f.bender(player, Element.WATER);
        Mob zombie = f.zombie(10, FLOOR, 10);
        float health = zombie.getHealth();
        f.lookAt(player, zombie);
        f.press(player, Ability.WATER_WHIP, false);
        check(zombie.getHealth() == health, "without water the whip does nothing");
        helper.setBlock(new BlockPos(12, 2, 6), Blocks.WATER);
        helper.runAtTickTime(25, () -> {
            check(Benders.get(player).water() > 0, "near water the bender carries water");
            Benders.get(player).clearCooldown(Ability.WATER_WHIP);
            f.lookAt(player, zombie);
            f.press(player, Ability.WATER_WHIP, false);
            check(zombie.getHealth() < health, "the whip hurt the zombie");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void bloodbendingNeedsTheFullMoonAndCloseness(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 7);
        BendingData data = f.bender(player, Element.WATER, Ability.BLOODBENDING, 1);
        Mob zombie = f.zombie(10, FLOOR, 10);
        f.dayTime(6000);
        f.lookAt(player, zombie);
        f.press(player, Ability.BLOODBENDING, false);
        check(!zombie.hasEffect(ModEffects.BLOODBENT), "by day a novice bloodbends nothing");
        f.dayTime(14000);
        // The sky's darkness is read once a tick: the night begins on the next one.
        helper.runAtTickTime(3, () -> {
            check(helper.getLevel().getMoonPhase() == 0 && helper.getLevel().isNight(), "the first night has a full moon");
            Mob far = f.zombie(16, FLOOR, 7); // off to the side, so the near zombie is not in the line of the aim
            f.lookAt(player, far);
            f.press(player, Ability.BLOODBENDING, false);
            check(!far.hasEffect(ModEffects.BLOODBENT) && !zombie.hasEffect(ModEffects.BLOODBENT), "six blocks away is too far");
            f.lookAt(player, zombie);
            f.press(player, Ability.BLOODBENDING, false);
            check(zombie.hasEffect(ModEffects.BLOODBENT), "up close, under the full moon, the grip takes");
            check(data.heldEntityId() == zombie.getId() && data.active(Ability.BLOODBENDING), "the data knows whom he holds");
            f.press(player, Ability.BLOODBENDING, false);
            check(!zombie.hasEffect(ModEffects.BLOODBENT) && data.heldEntityId() == -1, "a second press throws him");
            helper.succeed();
        });
    }

    // ---- fire ----

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void fireBallBurns(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6);
        f.bender(player, Element.FIRE, Ability.FIRE_BALL, 2);
        Mob zombie = f.zombie(10, FLOOR, 12);
        float health = zombie.getHealth();
        f.lookAt(player, zombie);
        f.press(player, Ability.FIRE_BALL, true);
        helper.runAtTickTime(12, () -> {
            f.lookAt(player, zombie);
            f.release(player, Ability.FIRE_BALL);
            check(!helper.getLevel().getEntitiesOfClass(BendingShotEntity.class, player.getBoundingBox().inflate(6),
                    s -> s.kind() == BendingShotEntity.Kind.FIRE_BALL && s.size() > 1.0F).isEmpty(), "a held key throws the big ball");
        });
        helper.runAtTickTime(40, () -> {
            check(zombie.getHealth() < health, "the fire ball hurt the zombie");
            check(zombie.isOnFire(), "and set it on fire");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void lightningStrikesAndStuns(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6);
        BendingData data = f.bender(player, Element.FIRE, Ability.LIGHTNING, 1);
        Mob zombie = f.zombie(10, FLOOR, 12);
        float health = zombie.getHealth();
        f.lookAt(player, zombie);
        f.press(player, Ability.LIGHTNING, false);
        check(data.active(Ability.LIGHTNING), "the lightning gathers");
        helper.runAtTickTime(30, () -> {
            // (A zombie's two points of armour take a little off the nine.)
            check(health - zombie.getHealth() >= EnlightenedConfig.LIGHTNING_DAMAGE.get().floatValue() * 0.9F || !zombie.isAlive(),
                    "the bolt did its damage: " + (health - zombie.getHealth()));
            check(!zombie.isAlive() || zombie.hasEffect(ModEffects.STUNNED), "and stunned him");
            check(!data.isReady(Ability.LIGHTNING, player.level().getGameTime()), "lightning takes long to recharge");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void firebendersSkinSoftensFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 10);
        f.bender(player, Element.FIRE, Ability.FIRE_SKIN, 3);
        player.invulnerableTime = 0;
        float health = player.getHealth();
        player.hurt(player.damageSources().inFire(), 4.0F);
        check(player.getHealth() == health, "a master's skin takes no fire damage");
        Benders.get(player).setLevel(Ability.FIRE_SKIN, 1);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().inFire(), 4.0F);
        check(health - player.getHealth() > 0 && health - player.getHealth() < 4.0F, "a novice's skin halves it: " + (health - player.getHealth()));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void suppressedBenderCannotBend(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.FIRE, Ability.FIRE_RING, 1);
        Mob zombie = f.zombie(10, FLOOR, 10);
        float health = zombie.getHealth();
        data.setSuppressedUntil(player.level().getGameTime() + 1000);
        f.press(player, Ability.FIRE_RING, false);
        check(zombie.getHealth() == health, "a suppressed bender's ring burns nothing");
        check(data.isReady(Ability.FIRE_RING, player.level().getGameTime()), "and no cooldown started");
        data.setSuppressedUntil(0);
        f.press(player, Ability.FIRE_RING, false);
        check(zombie.getHealth() < health, "free again, the ring burns");
        helper.succeed();
    }

    private EnlightenedTests() {
    }
}
