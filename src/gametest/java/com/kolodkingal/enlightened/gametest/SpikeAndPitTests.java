package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.earth.EarthPit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.FLOOR;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** 1.1.0: the earth spikes and the hidden pit. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class SpikeAndPitTests {

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void anEarthSpikeThrowsTheTargetAndCrumbles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, FLOOR, 3.5);
        f.bender(p, Element.EARTH, Ability.EARTH_SPIKES, 2);
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(10, 2, 11));
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        f.lookAt(p, husk);
        f.press(p, Ability.EARTH_SPIKES, false);
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(10, 2, 11));
        BlockPos side = helper.absolutePos(new BlockPos(12, 2, 11));
        check(level.getBlockState(first).isAir(), "the ground only rumbles at first");
        helper.startSequence()
                .thenWaitUntil(() -> {
                    check(husk.getHealth() < husk.getMaxHealth(), "the spike strikes (" + husk.getHealth() + ")");
                    check(TempBlocks.isRaised(level, first) && TempBlocks.isRaised(level, first.above()), "a spike three high stands");
                    check(level.getBlockState(first.above(2)).is(Blocks.POINTED_DRIPSTONE), "with its point on top");
                    check(TempBlocks.isRaised(level, side), "and an adept's small spikes round it");
                    check(husk.getY() >= first.getY() + 0.9, "it threw the husk up");
                })
                .thenWaitUntil(() -> check(level.getBlockState(first).isAir() && level.getBlockState(first.above(2)).isAir(),
                        "the spike crumbles back into the ground"))
                .thenExecute(() -> check(level.getBlockState(first.below()).is(Blocks.SMOOTH_STONE), "and the ground is as it was"))
                .thenSucceed();
    }

    /** A dirt platform three high on the arena floor (relative y 2 to 4), so a pit three deep has earth to be dug in. */
    private static void platform(GameTestHelper helper) {
        for (int x = 4; x <= 16; x++) {
            for (int z = 2; z <= 16; z++) {
                for (int y = 2; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), y == 4 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                }
            }
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void aHiddenPitSwallowsAnEnemyAndTheGroundIsWholeAgain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        platform(helper);
        ServerPlayer p = f.player(10.5, 5.0, 3.5);
        f.bender(p, Element.EARTH, Ability.EARTH_PIT, 2);
        f.lookAt(p, new Vec3(10.5, 5.0, 10.5));
        f.press(p, Ability.EARTH_PIT, false);
        ServerLevel level = helper.getLevel();
        BlockPos top = helper.absolutePos(new BlockPos(10, 4, 10));
        check(EarthPit.of(p.getUUID()).size() == 1, "a pit is dug");
        EarthPit.Pit pit = EarthPit.of(p.getUUID()).get(0);
        check(level.getBlockState(top).is(Blocks.GRASS_BLOCK), "its top looks like any other ground");
        check(level.getBlockState(top.below()).isAir() && level.getBlockState(top.below(3)).isAir(), "but the earth under it is hollow");
        check(pit.holes().size() == 27 && pit.tops().size() == 9, "an adept's pit: three wide, three deep (" + pit.holes().size() + ")");
        Husk husk = helper.spawn(EntityType.HUSK, new Vec3(10.5, 5.0, 10.5));
        husk.goalSelector.removeAllGoals(g -> true);
        husk.targetSelector.removeAllGoals(g -> true);
        husk.setPersistenceRequired();
        helper.startSequence()
                .thenWaitUntil(() -> check(pit.sprung(), "the husk steps on it and the ground caves in"))
                .thenWaitUntil(() -> {
                    check(husk.getY() < top.getY() - 1, "it falls in (" + husk.getY() + ")");
                    check(pit.closed() && level.getBlockState(top).is(Blocks.GRASS_BLOCK), "and the ground closes over its head");
                    check(husk.getHealth() < husk.getMaxHealth(), "the earth strikes it");
                })
                .thenWaitUntil(() -> check(EarthPit.of(p.getUUID()).isEmpty(), "in a few seconds the pit gives it back"))
                .thenExecute(() -> {
                    check(husk.getY() >= top.getY() + 0.9, "the husk is set on the ground (" + husk.getY() + ")");
                    check(level.getBlockState(top.below()).is(Blocks.DIRT) && level.getBlockState(top.below(3)).is(Blocks.SMOOTH_STONE),
                            "and the earth is whole again");
                    check(level.getBlockState(top).is(Blocks.GRASS_BLOCK), "its top too");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void aHiddenPitNeverSwallowsItsBender(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        platform(helper);
        ServerPlayer p = f.player(10.5, 5.0, 10.5);
        f.bender(p, Element.EARTH, Ability.EARTH_PIT, 1);
        f.lookAt(p, new Vec3(10.5, 4.5, 10.5));
        f.press(p, Ability.EARTH_PIT, false);
        check(EarthPit.of(p.getUUID()).size() == 1, "a pit under his own feet");
        EarthPit.Pit pit = EarthPit.of(p.getUUID()).get(0);
        check(pit.holes().size() == 2, "a novice's pit: one wide, two deep");
        helper.runAfterDelay(40, () -> {
            check(!pit.sprung(), "it waits for someone else");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aHiddenPitIsNotDugBesideWater(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        platform(helper);
        helper.setBlock(new BlockPos(11, 3, 10), Blocks.WATER);
        ServerPlayer p = f.player(10.5, 5.0, 3.5);
        f.bender(p, Element.EARTH, Ability.EARTH_PIT, 1);
        f.lookAt(p, new Vec3(10.5, 5.0, 10.5));
        f.press(p, Ability.EARTH_PIT, false);
        check(EarthPit.of(p.getUUID()).isEmpty(), "water beside it would pour in: no pit");
        check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, 3, 10))).is(Blocks.DIRT), "and nothing dug");
        helper.succeed();
    }
}
