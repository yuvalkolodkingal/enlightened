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

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void theMasterAvatarsArtTakesHimInAndItShields(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.AVATAR, Ability.AVATAR_STATE, 1, Ability.FIRE_BALL, 3, Ability.ROCK_THROW, 3);
        data.setGuruTaught(true);
        data.setChakras(7);
        // Meditating is a toggle now, and no longer takes him into the State: only his art does.
        helper.runAtTickTime(10, () -> {
            com.kolodkingal.enlightened.bending.Chakras.meditate(player, true);
            check(data.meditating(), "the Avatar meditates (one press)");
            com.kolodkingal.enlightened.bending.Chakras.meditate(player, false);
            check(data.meditating(), "letting go of the key does not stand him up");
        });
        helper.runAtTickTime(40, () -> {
            check(!data.avatar(player.level().getGameTime()), "meditation alone does not bring the Avatar State");
            com.kolodkingal.enlightened.bending.Chakras.meditate(player, true);
            check(!data.meditating(), "a second press stands him up");
            f.press(player, Ability.AVATAR_STATE, false);
            check(data.avatar(player.level().getGameTime()), "the art takes him into the Avatar State");
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

    // ---- 1.3: the glider, the octopus's lash, the vortex, blue fire, the palm forge, the Avatar ----

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void gliderOpensAndAirJumpHasNoSlowFalling(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.AIR, Ability.AIR_JUMP, 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.kolodkingal.enlightened.registry.ModItems.AIR_GLIDER.get()));
        com.kolodkingal.enlightened.air.AirBending.setGlider(player, data, true, false);
        check(data.gliderOpen(), "the staff opened into its wings");
        f.press(player, Ability.AIR_JUMP, false);
        // The server zeroes a player's velocity every tick (the client moves him), so the throw shows at once.
        check(player.getDeltaMovement().y > 0.5, "the air jump threw him up");
        helper.runAtTickTime(3, () -> {
            check(!player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "the air jump gives no slow falling");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void octopusFormLashesAtTheKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8, GameType.CREATIVE);
        f.bender(player, Element.WATER, Ability.OCTOPUS_FORM, 2);
        Mob zombie = f.zombie(10, FLOOR, 11);
        float health = zombie.getHealth();
        f.lookAt(player, zombie);
        f.press(player, Ability.OCTOPUS_FORM, false);
        helper.runAtTickTime(5, () -> {
            check(Benders.get(player).on(Ability.OCTOPUS_FORM), "the form is up");
            f.press(player, Ability.OCTOPUS_FORM, false);
        });
        helper.runAtTickTime(12, () -> {
            check(zombie.getHealth() < health, "the arms lashed the zombie");
            check(Benders.get(player).on(Ability.OCTOPUS_FORM), "a press lashes and keeps the form up");
            f.press(player, Ability.OCTOPUS_FORM, true);
        });
        helper.runAtTickTime(30, () -> {
            check(!Benders.get(player).on(Ability.OCTOPUS_FORM), "a held key folds the form away");
            f.release(player, Ability.OCTOPUS_FORM);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void waterVortexLiftsAndHurts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6, GameType.CREATIVE);
        f.bender(player, Element.WATER, Ability.WATER_VORTEX, 1);
        Mob zombie = f.zombie(10, FLOOR, 11);
        float health = zombie.getHealth();
        f.lookAt(player, zombie);
        f.press(player, Ability.WATER_VORTEX, false);
        helper.runAtTickTime(40, () -> {
            check(zombie.getDeltaMovement().y > 0.05 || zombie.getY() > helper.absoluteVec(new Vec3(0, FLOOR, 0)).y + 0.5, "the vortex lifts the zombie");
            check(zombie.getHealth() < health, "the vortex hurts the zombie");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void azulasBlazeLeavesBlueFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6, GameType.CREATIVE);
        f.bender(player, Element.FIRE, Ability.BLAZE, 2);
        Benders.setVariant(player, com.kolodkingal.enlightened.bending.Variants.get(Element.FIRE, "azula"));
        BendingData data = Benders.get(player);
        check(com.kolodkingal.enlightened.bending.Variants.blueFire(data), "Azula's fire is blue (look " + data.variant() + ")");
        check(com.kolodkingal.enlightened.fire.FireBending.fireBlock(data).is(com.kolodkingal.enlightened.registry.ModBlocks.BLUE_FIRE.get()), "her fire block is the blue one");
        f.lookAt(player, new Vec3(10, FLOOR, 12));
        f.press(player, Ability.BLAZE, false);
        helper.runAtTickTime(10, () -> {
            boolean blue = false;
            // The blaze is an arc: it lands sideways of the look too.
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, (int) FLOOR - 2, 4), new BlockPos(18, (int) FLOOR + 2, 18))) {
                if (helper.getBlockState(pos).is(com.kolodkingal.enlightened.registry.ModBlocks.BLUE_FIRE.get())) {
                    blue = true;
                }
            }
            check(blue, "Azula's blaze lit blue fire on the ground");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void palmForgeSmeltsRawIronInHand(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        f.bender(player, Element.FIRE, Ability.PALM_FORGE, 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 3));
        f.press(player, Ability.PALM_FORGE, true);
        helper.runAtTickTime(45, () -> {
            check(player.getInventory().countItem(net.minecraft.world.item.Items.IRON_INGOT) >= 1, "a piece of raw iron became an ingot in the hand");
            f.release(player, Ability.PALM_FORGE);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void theAvatarBendsAllFour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 6, GameType.CREATIVE);
        BendingData data = f.bender(player, Element.AVATAR, Ability.ROCK_THROW, 1, Ability.AIR_BLAST, 1, Ability.WATER_WHIP, 1, Ability.FIRE_RING, 1);
        check(data.element().commands(Element.FIRE) && data.element().commands(Element.WATER), "the Avatar commands every element");
        check(com.kolodkingal.enlightened.bending.Mastery.canLearn(data, Ability.FIRE_FIST), "the Avatar may learn a fire art");
        Mob zombie = f.zombie(10, FLOOR, 10);
        f.lookAt(player, zombie);
        f.press(player, Ability.ROCK_THROW, false);
        f.press(player, Ability.FIRE_RING, false);
        f.press(player, Ability.AIR_BLAST, false);
        helper.runAtTickTime(5, () -> {
            long now = player.level().getGameTime();
            check(!data.isReady(Ability.ROCK_THROW, now), "the rock went off");
            check(!data.isReady(Ability.FIRE_RING, now), "the fire ring went off");
            check(!data.isReady(Ability.AIR_BLAST, now), "the air blast went off");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void energybendingTakesAndGivesBackBending(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer avatar = f.player(10, FLOOR, 8, GameType.CREATIVE);
        BendingData mine = f.bender(avatar, Element.AVATAR, Ability.ENERGYBENDING, 3, Ability.ENERGY_RESTORE, 1, Ability.ROCK_THROW, 3,
                Ability.FIRE_BALL, 3, Ability.AIR_BLAST, 3);
        ServerPlayer bender = f.player(10, FLOOR, 10);
        BendingData theirs = f.bender(bender, Element.FIRE, Ability.FIRE_BALL, 1);
        f.lookAt(avatar, bender);
        long now = avatar.level().getGameTime();
        mine.setChakras(6);
        f.press(avatar, Ability.ENERGYBENDING, false);
        check(!theirs.suppressed(now), "with a chakra closed the Avatar cannot bend energy");
        mine.setChakras(7);
        mine.clearCooldowns();
        f.press(avatar, Ability.ENERGYBENDING, false);
        check(theirs.suppressed(now), "the firebender's bending is taken");
        long left = theirs.suppressedUntil() - now;
        check(left > 119 * 1200L && left <= 120 * 1200L, "for two hours (" + left + " ticks)");
        mine.clearCooldowns();
        f.press(avatar, Ability.ENERGY_RESTORE, false);
        check(!theirs.suppressed(now), "the second art gives it back");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theLionTurtlesGiftTakesForMinutesAndTurnsOnTheWeak(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        long now = helper.getLevel().getGameTime();
        // An airbender with the gift takes a bending for six minutes.
        ServerPlayer air = f.player(4, FLOOR, 4);
        f.bender(air, Element.AIR, Ability.AIR_BLAST, 3, Ability.ENERGYBENDING, 1, Ability.ENERGY_RESTORE, 1);
        ServerPlayer fire = f.player(4, FLOOR, 6);
        BendingData burnt = f.bender(fire, Element.FIRE, Ability.FIRE_BALL, 1);
        f.lookAt(air, fire);
        f.press(air, Ability.ENERGYBENDING, false);
        check(burnt.suppressed(now), "the airbender took it (strength " + com.kolodkingal.enlightened.bending.Mastery.energyStrength(air)
                + " against " + com.kolodkingal.enlightened.bending.Mastery.energyStrength(fire) + ")");
        long left = burnt.suppressedUntil() - now;
        check(left > 350 * 20L && left <= 360 * 20L, "for six minutes (" + left + " ticks)");
        Benders.get(air).clearCooldowns();
        f.press(air, Ability.ENERGY_RESTORE, false);
        check(!burnt.suppressed(now), "and gave it back");
        // Someone who bends nothing, given the gift, takes it for three.
        ServerPlayer plain = f.player(14, FLOOR, 4);
        BendingData giver = f.bender(plain, Element.NONE, Ability.ENERGYBENDING, 1, Ability.ENERGY_RESTORE, 1);
        check(giver.canBend() && giver.slotOf(Ability.ENERGYBENDING) >= 0, "the gift sits on a key though he bends nothing");
        ServerPlayer earth = f.player(14, FLOOR, 6);
        BendingData stoned = f.bender(earth, Element.EARTH, Ability.ROCK_THROW, 1);
        f.lookAt(plain, earth);
        f.press(plain, Ability.ENERGYBENDING, false);
        left = stoned.suppressedUntil() - now;
        check(left > 170 * 20L && left <= 180 * 20L, "for three minutes (" + left + " ticks)");
        // Against a stronger energy it turns: the airbender loses his own bending.
        ServerPlayer master = f.player(2, FLOOR, 4);
        BendingData strong = f.bender(master, Element.WATER, Ability.WATER_WHIP, 5, Ability.OCTOPUS_FORM, 5);
        BendingData weak = Benders.get(air);
        weak.clearCooldowns();
        f.lookAt(air, master);
        f.press(air, Ability.ENERGYBENDING, false);
        check(!strong.suppressed(now), "the master keeps his bending");
        check(weak.suppressed(now), "the weaker one lost his own");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aFirebenderChoosesTheColourOfHisFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer azula = f.player(10, FLOOR, 8);
        BendingData data = f.bender(azula, Element.FIRE, Ability.FIRE_BALL, 1, Ability.FIRE_JET, 1);
        Benders.setVariant(azula, com.kolodkingal.enlightened.bending.Variants.get(Element.FIRE, "azula"));
        check(com.kolodkingal.enlightened.bending.Variants.fireColour(data) == com.kolodkingal.enlightened.bending.Variants.FIRE_BLUE, "Azula's look burns blue");
        data.setFireChoice(1 + com.kolodkingal.enlightened.bending.Variants.FIRE_RAINBOW);
        check(com.kolodkingal.enlightened.bending.Variants.fireColour(data) == com.kolodkingal.enlightened.bending.Variants.FIRE_RAINBOW, "she chose the rainbow");
        data.setOn(Ability.FIRE_JET, true);
        check(data.on(Ability.FIRE_JET), "a toggle beside the choice still works");
        data.clearToggles();
        check(!data.on(Ability.FIRE_JET) && data.fireChoice() == 1 + com.kolodkingal.enlightened.bending.Variants.FIRE_RAINBOW, "clearing the toggles keeps the choice");
        check(new com.kolodkingal.enlightened.bending.Cast(azula, data, Ability.FIRE_BALL).color() == com.kolodkingal.enlightened.bending.Variants.RAINBOW_MARK,
                "her fire balls carry the rainbow");
        data.setFireChoice(1 + com.kolodkingal.enlightened.bending.Variants.FIRE_NORMAL);
        check(!com.kolodkingal.enlightened.bending.Variants.blueFire(data), "and the colour of fire, if she wants it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void spiritbendingCalmsAZombie(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer avatar = f.player(10, FLOOR, 8, GameType.CREATIVE);
        f.bender(avatar, Element.AVATAR, Ability.SPIRITBENDING, 1);
        Mob zombie = f.zombie(10, FLOOR, 11);
        zombie.setTarget(avatar);
        f.lookAt(avatar, zombie);
        f.press(avatar, Ability.SPIRITBENDING, false);
        helper.runAtTickTime(5, () -> {
            check(zombie.hasEffect(com.kolodkingal.enlightened.registry.ModEffects.PACIFIED), "the zombie is calmed");
            check(zombie.getTarget() == null, "it forgot its prey");
            zombie.setTarget(avatar);
            check(zombie.getTarget() == null, "it takes no new prey while calm");
            helper.succeed();
        });
    }
}
