package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Chakras;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.entity.AnomalyEntity;
import com.kolodkingal.enlightened.event.CommonEvents;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.world.Cosmic;
import com.kolodkingal.enlightened.world.ElloraPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.FLOOR;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of the Avatar State by the chakras, and of the Anomaly of Ellora. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class AnomalyTests {

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void someChakrasOpenGiveTheirShareOfTheAvatarState(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.AVATAR, Ability.AVATAR_STATE, 1, Ability.FIRE_BALL, 3);
        data.setGuruTaught(true);
        helper.runAtTickTime(5, () -> {
            long now = player.level().getGameTime();
            float calm = Chakras.damageFactor(data, now);
            f.press(player, Ability.AVATAR_STATE, false);
            check(!data.avatar(player.level().getGameTime()), "with no chakra open the State does not come");
            data.setChakras(3);
            data.clearCooldowns();
            f.press(player, Ability.AVATAR_STATE, false);
            now = player.level().getGameTime();
            check(data.avatar(now), "with three open, he draws on it");
            check(Chakras.partial(data, now) && Chakras.power(data, now) == 3, "a part of it: three of seven");
            float part = Chakras.damageFactor(data, now);
            // His third chakra adds its five percent; the State adds three sevenths of its one and a half.
            float expect = (calm + 0.05F) * (1.0F + 1.5F * 3 / 7.0F);
            check(Math.abs(part - expect) < 0.01F, "three sevenths of its strength (" + part + " vs " + expect + ")");
            check(Chakras.colour(Chakras.power(data, now)) == Chakras.COLOURS[2], "in the third chakra's colour, yellow");
            check(Chakras.colour(1) == 0xE8302A, "the first is red");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theArtOpensAPartWithoutTheGuruAndDeathCallsAsDeepAsTheChakras(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.AVATAR, Ability.AVATAR_STATE, 1);
        data.setGuruTaught(false);
        data.setChakras(2);
        helper.runAtTickTime(5, () -> {
            f.press(player, Ability.AVATAR_STATE, false);
            long now = player.level().getGameTime();
            check(data.avatar(now) && Chakras.power(data, now) == 2, "two chakras open (no guru): the art draws on two sevenths");
            int[][] cases = {{0, Chakras.POWER_WHOLE}, {3, 3}, {7, Chakras.POWER_MASTER}};
            for (int[] k : cases) {
                data.setAvatarUntil(0);
                data.setAvatarCooldownUntil(0);
                data.setChakras(k[0]);
                check(Chakras.dire(player, data), "near death the State comes (" + k[0] + " chakras)");
                long t = player.level().getGameTime();
                check(Chakras.power(data, t) == k[1], "as deep as the chakras reach: " + k[0] + " open, depth " + Chakras.power(data, t));
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theMastersAvatarStateIsTwiceTheWhole(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10, FLOOR, 8);
        BendingData data = f.bender(player, Element.AVATAR, Ability.AVATAR_STATE, 1);
        data.setGuruTaught(true);
        data.setChakras(7);
        helper.runAtTickTime(5, () -> {
            Chakras.enterAvatar(player, data, true);
            long now = player.level().getGameTime();
            check(Chakras.power(data, now) == Chakras.POWER_WHOLE, "the State near death is the whole");
            float whole = Chakras.damageFactor(data, now);
            float wholeCool = Chakras.cooldownFactor(data, now);
            data.setAvatarUntil(0);
            Chakras.enterAtWill(player, data, 1.0F);
            now = player.level().getGameTime();
            check(Chakras.power(data, now) == Chakras.POWER_MASTER && !Chakras.partial(data, now), "the master's State");
            check(Math.abs(Chakras.damageFactor(data, now) - 2 * whole) < 0.01F, "every art twice as strong as in the whole State");
            check(Math.abs(Chakras.cooldownFactor(data, now) - wholeCool / 2) < 0.01F, "and ready twice as soon");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theAnomalySleepsInEllorasRedCubes(GameTestHelper helper) {
        int cubes = 0;
        for (int[] c : ElloraPiece.ANOMALY_CUBES) {
            var b = ElloraPiece.design(c[0], ElloraPiece.PLAZA + c[1], c[2]);
            if (b != null && b.is(ModBlocks.ANOMALY_BLOCK.get())) {
                cubes++;
            }
        }
        check(cubes == ElloraPiece.ANOMALY_CUBES.length, "the red cubes are in the keep's hall (" + cubes + ")");
        check(ModBlocks.ANOMALY_BLOCK.get().defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO) < 0, "no one can break them");
        BlockPos cube = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.getLevel().setBlock(cube, ModBlocks.ANOMALY_BLOCK.get().defaultBlockState(), 3);
        AnomalyEntity anomaly = CommonEvents.wakeAnomaly(helper.getLevel(), cube);
        check(anomaly != null && anomaly.isAlive(), "striking one wakes the Anomaly");
        check(CommonEvents.wakeAnomaly(helper.getLevel(), cube) == null, "only one at a time");
        check(anomaly.getY() > cube.getY() + 2, "it hangs in the air over the cube");
        float full = anomaly.getHealth();
        anomaly.hurt(helper.getLevel().damageSources().generic(), 20.0F);
        float afterBlow = anomaly.getHealth();
        check(full - afterBlow <= 15.5F && full - afterBlow > 10.5F, "a blow does three quarters of its harm (" + (full - afterBlow) + ")");
        anomaly.invulnerableTime = 0;
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(8, FLOOR, 4);
        f.bender(p, Element.AVATAR);
        check(anomaly.drain(p), "energybending reaches it");
        check(afterBlow - anomaly.getHealth() >= anomaly.getMaxHealth() * AnomalyEntity.DRAIN * 0.9F, "and strikes critically: half its life");
        anomaly.invulnerableTime = 0;
        anomaly.drain(p);
        check(!anomaly.isAlive() || anomaly.getHealth() <= 1.0F, "two touches finish it");
        if (anomaly.isAlive()) {
            anomaly.discard();
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theAnomalysPathEndsInSevenCircles(GameTestHelper helper) {
        check(Cosmic.CIRCLE_Z.length == Chakras.COUNT, "seven circles, one for each chakra");
        Vec3 inFirst = new Vec3(Cosmic.ANOMALY_X + 0.5, Cosmic.Y + 1, Cosmic.CIRCLE_Z[0] + 0.5);
        check(Cosmic.inCircle(inFirst, 0), "walking along the path he passes into the first, red circle");
        check(!Cosmic.inCircle(inFirst.add(0, 0, -3), 0), "not before he reaches it");
        check(!Cosmic.inCircle(inFirst.add(5, 0, 0), 0), "nor beside it");
        helper.succeed();
    }
}
