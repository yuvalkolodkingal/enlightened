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
import com.kolodkingal.enlightened.client.anim.AnimMath;
import com.kolodkingal.enlightened.client.anim.Gesture;
import com.kolodkingal.enlightened.client.anim.Gestures;
import com.kolodkingal.enlightened.entity.FlyingLemurEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Items;
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

    // ---- animals, the animation tables, the lemur ----

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void bendingHurtsAnimalsByDefault(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6);
        f.bender(player, Element.EARTH);
        Cow cow = EntityType.COW.create(helper.getLevel());
        check(cow != null, "a cow can be made");
        Vec3 at = helper.absoluteVec(new Vec3(10, FLOOR, 12));
        cow.moveTo(at.x, at.y, at.z, 180.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        f.onClose(cow::discard);
        // The default is on; a stale config of an older run must not decide the test.
        boolean was = EnlightenedConfig.HURT_PASSIVES.get();
        EnlightenedConfig.HURT_PASSIVES.set(true);
        f.onClose(() -> EnlightenedConfig.HURT_PASSIVES.set(was));
        check(EnlightenedConfig.HURT_PASSIVES.getDefault(), "hurtPassives is on by default");
        float health = cow.getHealth();
        f.lookAt(player, cow);
        f.press(player, Ability.ROCK_THROW, false);
        helper.runAtTickTime(30, () -> {
            check(cow.getHealth() < health, "the rock hurt the cow (general.hurtPassives is on by default)");
            helper.succeed();
        });
    }

    /** Every gesture has keys, in order, inside its length; a spin ends facing the way it began. */
    @GameTest(template = ARENA)
    public static void gestureTablesAreWellFormed(GameTestHelper helper) {
        java.util.List<String> problems = Gestures.problems();
        check(problems.isEmpty(), "the gesture tables: " + String.join("; ", problems));
        check(Gestures.all().size() == Gesture.VALUES.length, "every gesture is keyed");
        helper.succeed();
    }

    /** The maths of the animation behaves: eases settle, envelopes rise and fall, springs come to rest without blowing up. */
    @GameTest(template = ARENA)
    public static void animationMathSettles(GameTestHelper helper) {
        float x = 0.0F;
        for (int i = 0; i < 60; i++) {
            x = AnimMath.ease(x, 1.0F, 1.0F, 3.0F);
        }
        check(x > 0.99F, "an ease reaches its target");
        float coarse = AnimMath.ease(0.0F, 1.0F, 4.0F, 3.0F);
        float fine = 0.0F;
        for (int i = 0; i < 8; i++) {
            fine = AnimMath.ease(fine, 1.0F, 0.5F, 3.0F);
        }
        check(Math.abs(coarse - fine) < 1.0E-4F, "an ease is the same at any frame rate");
        check(AnimMath.envelope(0.0F, 2, 10, 4) == 0.0F && AnimMath.envelope(5.0F, 2, 10, 4) == 1.0F && AnimMath.envelope(10.0F, 2, 10, 4) == 0.0F,
                "a gesture's envelope rises, holds and falls");
        AnimMath.Spring spring = new AnimMath.Spring();
        spring.snap(1.0F);
        float peak = 0.0F;
        for (int i = 0; i < 200; i++) {
            spring.damped(0.0F, 0.8F, 0.45F, 0.5F);
            peak = Math.max(peak, -spring.x);
        }
        check(Math.abs(spring.x) < 1.0E-3F && Math.abs(spring.v) < 1.0E-3F, "a damped spring comes to rest");
        check(peak > 0.05F && peak < 0.5F, "a damped spring overshoots a little and no more");
        float[] arm = com.kolodkingal.enlightened.client.anim.AimMath.arm(new org.joml.Vector3f(0, 0, 1), 0, 0, 0);
        check(Math.abs(arm[0] + Math.PI / 2) < 1.0E-3F && Math.abs(arm[1]) < 1.0E-3F, "an arm aimed straight ahead points level forward");
        helper.succeed();
    }

    /** A flying lemur is fed until it is tamed, then sits when told. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void flyingLemurIsTamedAndSits(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        FlyingLemurEntity lemur = ModEntities.LEMUR.get().create(helper.getLevel());
        check(lemur != null, "a lemur can be made");
        Vec3 at = helper.absoluteVec(new Vec3(10, FLOOR, 10));
        lemur.moveTo(at.x, at.y, at.z, 180.0F, 0.0F);
        lemur.setNoAi(true);
        helper.getLevel().addFreshEntity(lemur);
        f.onClose(lemur::discard);
        check(new ItemStack(Items.APPLE).is(ModTags.LEMUR_FOOD), "an apple is lemur food");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 64));
        int fed = 0;
        while (!lemur.isTame() && fed < 100) {
            lemur.mobInteract(player, InteractionHand.MAIN_HAND);
            fed++;
        }
        check(lemur.isTame(), "the lemur was tamed by feeding");
        check(lemur.isOwnedBy(player), "the lemur belongs to its feeder");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        lemur.mobInteract(player, InteractionHand.MAIN_HAND);
        check(lemur.isOrderedToSit(), "a tamed lemur sits when told");
        lemur.mobInteract(player, InteractionHand.MAIN_HAND);
        check(!lemur.isOrderedToSit(), "and stands when told again");
        helper.succeed();
    }

    // ---- the arts of 1.2: the shockwave, combos, steps, the wall of fire, chakras and the Avatar State ----

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void shockwaveHurtsAndThrowsBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        f.bender(player, Element.EARTH, Ability.SHOCKWAVE, 1);
        Mob zombie = f.zombie(10, FLOOR, 11);
        float health = zombie.getHealth();
        double before = zombie.distanceTo(player);
        f.press(player, Ability.SHOCKWAVE, false);
        helper.runAtTickTime(15, () -> {
            check(zombie.getHealth() < health, "the shockwave hurt the zombie");
            // A mob without AI is never moved by the server, so the throw shows in its velocity.
            check(zombie.getDeltaMovement().horizontalDistance() > 0.5 || zombie.distanceTo(player) > before + 0.5, "the shockwave threw the zombie back");
            check(!Benders.get(player).isReady(Ability.SHOCKWAVE, player.level().getGameTime()), "the shockwave has a cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void fireSpinComboFollowsTheShieldAndTheRing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        f.bender(player, Element.FIRE, Ability.FIRE_SHIELD, 1, Ability.FIRE_RING, 1);
        Mob zombie = f.zombie(10, FLOOR, 12);
        double before = zombie.distanceTo(player);
        f.press(player, Ability.FIRE_SHIELD, true);
        helper.runAtTickTime(5, () -> f.release(player, Ability.FIRE_SHIELD));
        helper.runAtTickTime(10, () -> f.press(player, Ability.FIRE_RING, false));
        helper.runAtTickTime(25, () -> {
            // The ring alone reaches 3 blocks; the Fire Spin reaches six and more, and throws far.
            check(zombie.distanceTo(player) > before + 2.0 || zombie.getHealth() < zombie.getMaxHealth() - 6, "the Fire Spin followed the shield and the ring");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void elementStepDashes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        f.bender(player, Element.AIR, Ability.AIR_STEP, 1);
        com.kolodkingal.enlightened.bending.Steps.step(player, new Vec3(0, 0, 1));
        check(player.getDeltaMovement().z > 0.5, "the step threw the airbender forward");
        check(!Benders.get(player).isReady(Ability.AIR_STEP, player.level().getGameTime()), "the step has a cooldown");
        check(com.kolodkingal.enlightened.bending.SoftLanding.active(player), "the landing after a step is soft");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void wallOfFireBurnsWhoCrossesIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6);
        f.bender(player, Element.FIRE, Ability.WALL_OF_FIRE, 1);
        f.lookAt(player, new Vec3(10, FLOOR, 11));
        f.press(player, Ability.WALL_OF_FIRE, false);
        var walls = helper.getLevel().getEntitiesOfClass(com.kolodkingal.enlightened.entity.FireWallEntity.class, player.getBoundingBox().inflate(12));
        check(!walls.isEmpty(), "a wall of fire was raised");
        Vec3 wallAt = helper.relativeVec(walls.getFirst().position());
        Mob zombie = f.zombie(wallAt.x, FLOOR, wallAt.z);
        float health = zombie.getHealth();
        helper.runAtTickTime(40, () -> {
            check(zombie.getHealth() < health || zombie.isOnFire(), "the wall burned the zombie standing in it");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void meditationOpensTheLastChakraAndTheAvatarStateShields(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.FIRE, Ability.FIRE_BALL, 3, Ability.FIRE_FIST, 3, Ability.FIRE_WHIP, 3);
        check(data.spent() >= com.kolodkingal.enlightened.bending.Chakras.pointsFor(7), "enough points are spent for the seventh chakra");
        data.setChakras(6);
        data.setMeditation(com.kolodkingal.enlightened.bending.Chakras.ticksPerChakra() - 2);
        // The awakening swings the arm, and a swing ends a meditation: so it begins once the arm is still.
        helper.runAtTickTime(10, () -> {
            com.kolodkingal.enlightened.bending.Chakras.meditate(player, true);
            check(data.meditating(), "the bender meditates");
        });
        helper.runAtTickTime(16, () -> check(data.chakras() == 7, "the seventh chakra opened by meditation (meditating " + data.meditating()
                + ", progress " + data.meditation() + ", swinging " + player.swinging + ")"));
        // Still meditating with all seven open: the Avatar State comes after a breath.
        helper.runAtTickTime(50, () -> {
            check(data.avatar(player.level().getGameTime()), "the Avatar State was entered");
            float health = player.getHealth();
            player.hurt(player.damageSources().generic(), 6.0F);
            check(player.getHealth() >= health - 0.01F, "nothing hurts the Avatar");
            check(com.kolodkingal.enlightened.bending.Chakras.damageFactor(data, player.level().getGameTime()) > 2.0F, "bending is twice as strong");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void bendingHurtsTheBendersOwnPetByDefault(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6);
        f.bender(player, Element.EARTH);
        net.minecraft.world.entity.animal.Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        check(wolf != null, "a wolf can be made");
        Vec3 at = helper.absoluteVec(new Vec3(10, FLOOR, 12));
        wolf.moveTo(at.x, at.y, at.z, 180.0F, 0.0F);
        wolf.setNoAi(true);
        wolf.tame(player);
        helper.getLevel().addFreshEntity(wolf);
        f.onClose(wolf::discard);
        boolean was = EnlightenedConfig.HURT_EVERYTHING.get();
        EnlightenedConfig.HURT_EVERYTHING.set(true);
        f.onClose(() -> EnlightenedConfig.HURT_EVERYTHING.set(was));
        check(EnlightenedConfig.HURT_EVERYTHING.getDefault(), "hurtEverything is on by default");
        check(com.kolodkingal.enlightened.bending.AreaTargets.mayHurt(player, wolf), "the bender's own wolf is a target");
        float health = wolf.getHealth();
        f.lookAt(player, wolf);
        f.press(player, Ability.ROCK_THROW, false);
        helper.runAtTickTime(30, () -> {
            check(wolf.getHealth() < health, "the rock hurt the bender's own wolf (general.hurtEverything)");
            helper.succeed();
        });
    }
}
