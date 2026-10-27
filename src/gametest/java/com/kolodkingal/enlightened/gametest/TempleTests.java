package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Chakras;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Guru;
import com.kolodkingal.enlightened.bending.Spouts;
import com.kolodkingal.enlightened.entity.GuruPathikEntity;
import com.kolodkingal.enlightened.entity.SkyBisonEntity;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.water.WaterBending;
import com.kolodkingal.enlightened.world.AirTemplePiece;
import com.kolodkingal.enlightened.world.Cosmic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.FLOOR;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of 1.4: the Southern Air Temple, its sky bison and its guru, the spouts, the octopus body, frost walk. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class TempleTests {

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void theChakrasOpenOnlyAfterTheGurusLesson(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.FIRE, Ability.FIRE_BALL, 1);
        data.setChakras(2);
        check(data.spent() < Chakras.pointsFor(3), "too few points for the third chakra on his own (spent " + data.spent() + ")");
        data.setMeditation(Chakras.ticksPerChakra() - 2);
        helper.runAtTickTime(5, () -> Chakras.meditate(player, true));
        helper.runAtTickTime(14, () -> {
            check(data.meditating(), "he meditates");
            check(data.chakras() == 2, "untaught, his chakras stay closed");
            GuruPathikEntity guru = helper.spawn(ModEntities.GURU_PATHIK.get(), new Vec3(10, FLOOR, 11));
            Guru.talk(player, guru);
            check(data.guruTaught(), "the guru taught him");
            data.setMeditation(Chakras.ticksPerChakra() - 12);
        });
        helper.runAtTickTime(24, () -> {
            check(data.chakras() == 3, "before the guru the third chakra opened fast, with no points asked (chakras " + data.chakras() + ")");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void onlyTheAvatarHasTheAvatarState(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer fire = f.player(6, FLOOR, 8);
        BendingData fd = f.bender(fire, Element.FIRE, Ability.FIRE_BALL, 3);
        fd.setGuruTaught(true);
        fd.setChakras(7);
        long now = fire.level().getGameTime();
        Chakras.enterAvatar(fire, fd, false);
        check(!fd.avatar(now), "a firebender never enters the Avatar State");
        check(!Chakras.dire(fire, fd), "not even near death");
        ServerPlayer avatar = f.player(14, FLOOR, 8);
        BendingData ad = f.bender(avatar, Element.AVATAR, Ability.AVATAR_STATE, 1, Ability.FIRE_BALL, 1);
        f.press(avatar, Ability.AVATAR_STATE, false);
        check(!ad.avatar(avatar.level().getGameTime()), "untaught, the Avatar cannot enter it at will");
        check(Chakras.dire(avatar, ad), "untaught, it takes the Avatar near death");
        check(ad.avatar(avatar.level().getGameTime()), "the Avatar is in the Avatar State");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theMasterAvatarEntersAndLeavesAtWill(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer avatar = f.player(10, FLOOR, 8);
        BendingData data = f.bender(avatar, Element.AVATAR, Ability.AVATAR_STATE, 1, Ability.FIRE_BALL, 1);
        data.setGuruTaught(true);
        data.setChakras(7);
        f.press(avatar, Ability.AVATAR_STATE, false);
        check(data.avatar(avatar.level().getGameTime()), "the master Avatar enters at will");
        helper.runAtTickTime(10, () -> {
            f.press(avatar, Ability.AVATAR_STATE, false);
            long now = avatar.level().getGameTime();
            check(!data.avatar(now), "and leaves at will");
            long rest = data.avatarCooldownUntil() - now;
            check(rest >= 200 && rest < 1000, "a short rest after a short stay (" + rest + " ticks)");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theAvatarsFireIsFireColoured(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer avatar = f.player(10, FLOOR, 8);
        BendingData data = f.bender(avatar, Element.AVATAR, Ability.FIRE_BALL, 1, Ability.WATER_WHIP, 1);
        check(new Cast(avatar, data, Ability.FIRE_BALL).color() == Element.FIRE.color, "the Avatar's fire is fire, not blue");
        check(new Cast(avatar, data, Ability.WATER_WHIP).color() == Element.WATER.color, "the Avatar's water is water");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void aSkyBisonIsTamedWithWheatSaddledAndCarriesSix(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer owner = f.player(4, FLOOR, 4);
        SkyBisonEntity bison = helper.spawn(ModEntities.SKY_BISON.get(), new Vec3(10, FLOOR, 10));
        bison.setNoAi(true);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 64));
        for (int i = 0; i < SkyBisonEntity.WHEAT_TO_TAME - 1; i++) {
            bison.mobInteract(owner, InteractionHand.MAIN_HAND);
        }
        check(!bison.isTame(), "not yet: it needs a lot of wheat");
        bison.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(bison.isTame() && bison.isOwnedBy(owner), "tamed by the one who fed it");
        check(owner.getMainHandItem().getCount() == 64 - SkyBisonEntity.WHEAT_TO_TAME, "the wheat was eaten");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        bison.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(bison.isSaddled(), "saddled");
        bison.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(owner.getVehicle() == bison, "the owner rides");
        check(bison.getControllingPassenger() == owner, "and steers");
        for (int i = 0; i < 5; i++) {
            ServerPlayer friend = f.player(4 + i, FLOOR, 6);
            check(friend.startRiding(bison), "rider " + (i + 2) + " climbs on");
        }
        ServerPlayer seventh = f.player(12, FLOOR, 4);
        check(!seventh.startRiding(bison), "a seventh does not fit");
        Set<String> seats = new HashSet<>();
        for (var e : bison.getPassengers()) {
            Vec3 at = bison.getPassengerRidingPosition(e);
            seats.add(Math.round(at.x * 10) + "," + Math.round(at.z * 10));
        }
        check(seats.size() == SkyBisonEntity.MAX_RIDERS, "every rider has his own seat (" + seats.size() + ")");
        // A calf may be tamed, but is too young for a saddle.
        SkyBisonEntity calf = helper.spawn(ModEntities.SKY_BISON.get(), new Vec3(16, FLOOR, 16));
        calf.setNoAi(true);
        calf.setAge(SkyBisonEntity.CALF_AGE);
        ServerPlayer feeder = f.player(14, FLOOR, 14);
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK, 2));
        calf.mobInteract(feeder, InteractionHand.MAIN_HAND);
        calf.mobInteract(feeder, InteractionHand.MAIN_HAND);
        check(calf.isTame(), "two hay bales (eighteen wheat) tame a calf");
        check(calf.isBaby(), "it is still a calf");
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        calf.mobInteract(feeder, InteractionHand.MAIN_HAND);
        check(!calf.isSaddled(), "a calf takes no saddle");
        for (var e : bison.getPassengers().toArray(new net.minecraft.world.entity.Entity[0])) {
            e.stopRiding();
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 30)
    public static void theSpoutsStandOnIceAndRaceWhereTheKeysPoint(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR + 3, 10);
        helper.setBlock(new BlockPos(10, 1, 10), Blocks.ICE);
        double base = Spouts.base(helper.getLevel(), player, 10, true);
        check(!Double.isNaN(base) && Math.abs(base - helper.absoluteVec(new Vec3(0, FLOOR, 0)).y) < 0.01, "the water spout stands on ice (" + base + ")");
        helper.setBlock(new BlockPos(10, 1, 10), Blocks.STONE);
        check(Double.isNaN(Spouts.base(helper.getLevel(), player, 10, true)), "but not on stone");
        check(!Double.isNaN(Spouts.base(helper.getLevel(), player, 10, false)), "the air spout stands on stone");
        player.setYRot(0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        for (int i = 0; i < 12; i++) {
            Spouts.drive(player, 1.0F, 0.0F, base, 6, Spouts.speed(3));
        }
        Vec3 v = player.getDeltaMovement();
        check(v.z > 0.6 && Math.abs(v.x) < 0.05, "forward, fast (" + v + ")");
        player.setDeltaMovement(Vec3.ZERO);
        for (int i = 0; i < 12; i++) {
            Spouts.drive(player, -1.0F, 1.0F, base, 6, Spouts.speed(3));
        }
        v = player.getDeltaMovement();
        check(v.z < -0.3 && v.x > 0.3, "back and to the left at once (" + v + ")");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 30)
    public static void theMasteredOctopusWrapsTheBodyOrKeepsToTheArms(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 10);
        BendingData data = f.bender(player, Element.WATER, Ability.OCTOPUS_FORM, 3);
        data.setOn(Ability.OCTOPUS_FORM, true);
        check(WaterBending.octopusBody(data), "mastered, the form wraps the whole body");
        check(WaterBending.octopusGuard(data) > 0.5F, "and guards more");
        check(WaterBending.switchOctopus(player), "the Change Look key switches it");
        check(data.octopusArms() && !WaterBending.octopusBody(data), "to the arms alone");
        check(WaterBending.switchOctopus(player) && WaterBending.octopusBody(data), "and back");
        data.setLevel(Ability.OCTOPUS_FORM, 2);
        check(!WaterBending.octopusBody(data) && !WaterBending.switchOctopus(player), "below mastery: the arms only, nothing to switch");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void frostWalkFreezesTheWaterUnderfoot(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int x = 6; x <= 14; x++) {
            for (int z = 6; z <= 14; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
            }
        }
        ServerPlayer player = f.player(10, FLOOR, 10);
        BendingData data = f.bender(player, Element.WATER, Ability.FROST_WALK, 2);
        f.press(player, Ability.FROST_WALK, false);
        check(data.on(Ability.FROST_WALK), "frost walk is on");
        helper.runAtTickTime(5, () -> {
            check(helper.getBlockState(new BlockPos(10, 1, 10)).is(Blocks.ICE), "the water under him froze");
            check(helper.getBlockState(new BlockPos(13, 1, 10)).is(Blocks.ICE), "three blocks round him at level two");
            check(helper.getBlockState(new BlockPos(14, 1, 14)).is(Blocks.WATER), "but not far off");
            f.press(player, Ability.FROST_WALK, false);
            check(!data.on(Ability.FROST_WALK), "and off again");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void theWaterBubbleHoldsWithoutCollapsing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (int x = 4; x <= 16; x++) {
            for (int z = 4; z <= 16; z++) {
                for (int y = 2; y <= 12; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
        ServerPlayer player = f.player(10, 5, 10);
        BendingData data = f.bender(player, Element.WATER, Ability.WATER_BUBBLE, 3);
        f.press(player, Ability.WATER_BUBBLE, false);
        check(data.on(Ability.WATER_BUBBLE), "the bubble is on");
        // Three seconds on, the pocket is still open: water flowing back would fill it within a second.
        helper.runAtTickTime(60, () -> {
            BlockPos center = new BlockPos(10, 5, 10);
            check(helper.getBlockState(center).isAir(), "air at the heart");
            check(helper.getBlockState(center.east(2)).isAir() && helper.getBlockState(center.above(2)).isAir(), "air two blocks out");
            check(helper.getBlockState(center.west(4)).is(Blocks.STRUCTURE_VOID), "an invisible wall holds the water back at the edge");
            check(helper.getBlockState(center.west(6)).is(Blocks.WATER), "and the sea beyond");
            f.press(player, Ability.WATER_BUBBLE, false);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theTemplePlan(GameTestHelper helper) {
        check(AirTemplePiece.design(0, 0, 0, 0).is(ModBlocks.MEDITATION_POINT.get()), "the guru sits on a meditation point at the heart");
        check(AirTemplePiece.design(6, 6, 0, 6).is(Blocks.CALCITE), "the sanctuary's wall is white stone");
        check(AirTemplePiece.design(6, 2, 0, 6).isAir(), "with a door to each of the four ways");
        check(AirTemplePiece.design(0, 31, 0, 0).is(Blocks.LIGHTNING_ROD), "under a spire");
        check(AirTemplePiece.design(0, 20, 6, 6).is(Blocks.LIGHT_BLUE_CONCRETE), "and a blue roof");
        check(AirTemplePiece.design(-7, 1, 16, Math.hypot(7, 16)).is(Blocks.HAY_BLOCK), "hay in the bison's courtyard");
        check(AirTemplePiece.design(30, 5, 0, 30) == null, "the world beyond is left alone");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void theSeventhChakraOpensAtTheEndOfTheCosmicPath(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.FIRE, Ability.FIRE_BALL, 3, Ability.FIRE_FIST, 3, Ability.FIRE_WHIP, 3);
        data.setGuruTaught(true);
        data.setChakras(6);
        check(data.spent() >= Chakras.pointsFor(7), "enough points are spent for the seventh chakra");
        data.setMeditation(Chakras.ticksPerChakra() - 2);
        if (helper.getLevel().getServer().getLevel(Cosmic.LEVEL) == null) {
            // The GameTest server builds the overworld alone (a world of the game has the plane, from the mod's data):
            // without the plane the chakra opens where he sits.
            helper.runAtTickTime(5, () -> Chakras.meditate(player, true));
            helper.runAtTickTime(14, () -> {
                check(data.chakras() == 7, "without the plane, the seventh chakra opened where he sat");
                helper.succeed();
            });
            return;
        }
        helper.runAtTickTime(5, () -> Chakras.meditate(player, true));
        helper.runAtTickTime(14, () -> {
            check(player.level().dimension().equals(Cosmic.LEVEL), "his spirit went to the cosmic plane (chakras " + data.chakras() + ", meditating "
                    + data.meditating() + ", progress " + data.meditation() + " of " + Chakras.ticksPerChakra() + ")");
            check(data.chakras() == 6, "the chakra waits for the end of the path");
            ServerLevel cosmic = (ServerLevel) player.level();
            check(cosmic.getBlockState(BlockPos.containing(Cosmic.END.x, Cosmic.Y, Cosmic.END.z)).is(Blocks.AMETHYST_BLOCK), "the path is laid");
            player.teleportTo(cosmic, Cosmic.END.x, Cosmic.END.y, Cosmic.END.z, 0.0F, 0.0F);
        });
        helper.runAtTickTime(24, () -> {
            check(data.chakras() == 7, "at the end of the path the seventh chakra opened");
            check(player.level() == helper.getLevel(), "and he came back where he sat");
            check(!data.avatar(player.level().getGameTime()), "a firebender gets no Avatar State from it");
            helper.succeed();
        });
    }
}
