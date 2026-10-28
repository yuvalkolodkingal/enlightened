package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.world.LionTurtle;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.FLOOR;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of the lion turtle: its swim, its shell, its gift and who may build on it. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class LionTurtleTests {

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theShellSwimsWithAllOnIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        helper.setBlock(5, 2, 5, Blocks.STONE);
        helper.setBlock(7, 2, 7, Blocks.STONE);
        helper.setBlock(5, 3, 5, Blocks.CHEST);
        if (helper.getBlockEntity(new BlockPos(5, 3, 5)) instanceof Container chest) {
            chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        }
        ServerPlayer rider = f.player(7.5, 3, 7.5);
        double x0 = rider.getX();
        BlockPos c = helper.absolutePos(new BlockPos(6, 2, 6));
        // A small island's ground: an ellipse 3 round, from the floor's top up four.
        LionTurtle.Region region = new LionTurtle.Region(c.getX(), c.getZ(), c.getY() - 1, Direction.NORTH, 3, 3, c.getY(), c.getY() + 4);
        helper.runAtTickTime(2, () -> {
            net.minecraft.server.level.ServerLevel level = helper.getLevel();
            new LionTurtle.Stroke(region, Direction.EAST).finish(level);
            BlockPos was = helper.absolutePos(new BlockPos(5, 3, 5));
            BlockPos chestAt = was.east();
            check(level.getBlockState(chestAt).is(Blocks.CHEST), "the chest is a block east");
            check(!level.getBlockState(was).is(Blocks.CHEST), "and gone from where it stood");
            check(level.getBlockState(was.below().east()).is(Blocks.STONE), "the ground under it moved with it");
            check(level.getBlockEntity(chestAt) instanceof Container box && box.getItem(0).is(Items.DIAMOND) && box.getItem(0).getCount() == 3,
                    "the chest took its diamonds along");
            check(level.getBlockState(helper.absolutePos(new BlockPos(3, 2, 6))).isAir(), "the back row is left empty");
            check(Math.abs(rider.getX() - (x0 + 1.0)) < 0.01, "the one standing on it moved a block with it (" + (rider.getX() - x0) + ")");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theShellIsAnIslandWithItsHeadAtTheFront(GameTestHelper helper) {
        for (int scale : new int[]{1, 10}) {
            LionTurtle.Shape s = new LionTurtle.Shape(scale);
            LionTurtle.Column mid = LionTurtle.column(s, 0, 0);
            check(mid != null && LionTurtle.at(mid, 0, 0, 0) != null, "the middle of the shell is solid at the sea (" + scale + ")");
            net.minecraft.world.level.block.state.BlockState back = LionTurtle.at(mid, 0, 0, mid.top());
            check(back != null && (back.is(Blocks.GRASS_BLOCK) || back.is(Blocks.MOSS_BLOCK)), "its back is grass");
            check(mid.top() >= 4 * scale, "its back rises " + mid.top() + " above the sea");
            check(LionTurtle.column(s, s.length() + 1, 0) == null, "nothing beyond its length");
            check(LionTurtle.column(s, 0, s.width() + 1) == null, "nothing beyond its width");
            check(LionTurtle.at(mid, 0, 0, -s.depth() - 1) == null, "nothing below its keel");
            BlockPos center = new BlockPos(0, 62, 0);
            BlockPos head = LionTurtle.headPos(s, center, Direction.NORTH);
            check(head.getZ() < -s.length(), "the head is at the front");
        }
        check(new LionTurtle.Shape(10).length() == 220, "the great lion turtle is 440 blocks long");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void anIslandKnowsItsGroundAndKeepsOthersAway(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        com.kolodkingal.enlightened.world.LionTurtles data = com.kolodkingal.enlightened.world.LionTurtles.get(level);
        BlockPos far = new BlockPos(5_000_000, 62, 5_000_000);
        com.kolodkingal.enlightened.world.LionTurtles.Island i = data.add(far, Direction.NORTH, 1);
        try {
            check(i != null, "the island is raised");
            check(data.add(far.east(30), Direction.NORTH, 1) == null, "no second island on top of it");
            check(com.kolodkingal.enlightened.world.LionTurtles.islandAt(level, far.above(3)) == i, "its ground is its own");
            check(com.kolodkingal.enlightened.world.LionTurtles.islandAt(level, far.north(80)) == null, "the open sea is not");
        } finally {
            // Taken away before the next tick, so it is never built in the test world.
            data.islands().remove(i);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theLionTurtleGivesEnergybendingOnlyToAHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer stranger = f.player(6, FLOOR, 6);
        BendingData data = f.bender(stranger, Element.FIRE, Ability.FIRE_BALL, 1);
        LionTurtleEntity turtle = helper.spawn(ModEntities.LION_TURTLE.get(), new Vec3(10, FLOOR, 10));
        LionTurtle.talk(stranger, turtle);
        check(!data.has(Ability.ENERGYBENDING), "it gives nothing to a stranger");
        check(!LionTurtle.mayBuild(stranger), "nor lets him build");
        AdvancementHolder hero = helper.getLevel().getServer().getAdvancements().get(ResourceLocation.withDefaultNamespace("adventure/hero_of_the_village"));
        check(hero != null, "the Hero of the Village advancement exists");
        for (String criterion : hero.value().criteria().keySet()) {
            stranger.getAdvancements().award(hero, criterion);
        }
        LionTurtle.talk(stranger, turtle);
        check(data.has(Ability.ENERGYBENDING) && data.has(Ability.ENERGY_RESTORE), "to the hero it gives both arts");
        check(data.slotOf(Ability.ENERGYBENDING) >= 0 && data.slotOf(Ability.ENERGY_RESTORE) >= 0, "on his keys");
        check(LionTurtle.mayBuild(stranger), "and lets him build on its shell");
        ServerPlayer creative = f.player(8, FLOOR, 6, GameType.CREATIVE);
        check(LionTurtle.mayBuild(creative), "a Creative player builds anywhere");
        check(Benders.get(stranger).canBend(), "he still bends");
        helper.succeed();
    }
}
