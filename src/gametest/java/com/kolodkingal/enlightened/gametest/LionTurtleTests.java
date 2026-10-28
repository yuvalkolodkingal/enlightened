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
        BlockPos a = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos b = helper.absolutePos(new BlockPos(9, 6, 9));
        BoundingBox box = BoundingBox.fromCorners(a, b);
        helper.runAtTickTime(2, () -> {
            LionTurtle.shift(helper.getLevel(), box, Direction.EAST, box.minY() - 1);
            net.minecraft.server.level.ServerLevel level = helper.getLevel();
            BlockPos was = helper.absolutePos(new BlockPos(5, 3, 5));
            BlockPos chestAt = was.east();
            check(level.getBlockState(chestAt).is(Blocks.CHEST), "the chest is a block east");
            check(!level.getBlockState(was).is(Blocks.CHEST), "and gone from where it stood");
            check(level.getBlockState(was.below().east()).is(Blocks.STONE), "the ground under it moved with it");
            check(level.getBlockEntity(chestAt) instanceof Container c && c.getItem(0).is(Items.DIAMOND) && c.getItem(0).getCount() == 3,
                    "the chest took its diamonds along");
            check(Math.abs(rider.getX() - (x0 + 1.0)) < 0.01, "the one standing on it moved a block with it (" + (rider.getX() - x0) + ")");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theShellIsAnIslandWithItsHeadAtTheFront(GameTestHelper helper) {
        check(LionTurtle.shell(0, 0, 0) != null, "the middle of the shell is solid at the sea");
        net.minecraft.world.level.block.state.BlockState back = LionTurtle.shell(0, 0, 7);
        check(back != null && (back.is(Blocks.GRASS_BLOCK) || back.is(Blocks.MOSS_BLOCK)), "its back is grass, seven above the sea");
        check(LionTurtle.shell(LionTurtle.LENGTH + 1, 0, 0) == null, "nothing beyond its length");
        check(LionTurtle.shell(0, LionTurtle.WIDTH + 1, 0) == null, "nothing beyond its width");
        check(LionTurtle.shell(0, 0, -LionTurtle.DEPTH - 1) == null, "nothing below its depth");
        BlockPos center = new BlockPos(0, 62, 0);
        BlockPos head = LionTurtle.headPos(center, Direction.NORTH);
        check(head.getZ() < -LionTurtle.LENGTH && LionTurtle.box(center).isInside(head), "the head is at the front, inside the island's box");
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
