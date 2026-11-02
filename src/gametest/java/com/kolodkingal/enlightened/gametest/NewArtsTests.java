package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.earth.EarthBending;
import com.kolodkingal.enlightened.entity.FlyingSlabEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of the arts of 1.0.13 and after: the struck wall, mud, the plant giant, the great lift. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class NewArtsTests {

    private static BlockPos wall(GameTestHelper helper, ServerPlayer p, net.minecraft.world.level.block.state.BlockState state) {
        BlockPos first = null;
        for (int x = 8; x <= 12; x++) {
            for (int y = 2; y <= 4; y++) {
                BlockPos at = helper.absolutePos(new BlockPos(x, y, 6));
                check(TempBlocks.place(p, helper.getLevel(), at, state, 400), "the wall rises");
                if (first == null) {
                    first = at;
                }
            }
        }
        return first;
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void aMasterStrikesHisWallAndItFlies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, TestKit.FLOOR, 3.5);
        f.bender(p, Element.EARTH, Ability.EARTH_WALL, 2, Ability.METAL_WALL, 3);
        BlockPos stone = wall(helper, p, Blocks.STONE.defaultBlockState());
        check(!EarthBending.strikeWall(p, stone), "an earth wall at the second level does not fly");
        TempBlocks.restoreAll(helper.getLevel().getServer(), p.getUUID());
        BlockPos iron = wall(helper, p, Blocks.IRON_BLOCK.defaultBlockState());
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(10, 2, 12));
        husk.setNoAi(true);
        check(EarthBending.strikeWall(p, iron), "a metal wall at the third level flies when struck");
        check(helper.getLevel().getBlockState(iron).isAir(), "the wall leaves its place");
        var slabs = helper.getLevel().getEntitiesOfClass(FlyingSlabEntity.class, new AABB(iron).inflate(8));
        check(slabs.size() == 1 && slabs.get(0).pieces().size() == 15, "all of it, as one");
        check(EarthBending.STRUCK_METAL > EarthBending.STRUCK_EARTH, "metal strikes harder than earth");
        helper.succeedWhen(() -> check(husk.getHealth() <= husk.getMaxHealth() - 10.0F || !husk.isAlive(),
                "it hits the monster behind it (" + husk.getHealth() + ")"));
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void aMudWaveRollsAndBogsDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, TestKit.FLOOR, 3.5);
        f.bender(p, Element.EARTH, Ability.MUD_WAVE, 3);
        for (int x = 8; x <= 12; x++) {
            for (int z = 2; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(10, 2, 9));
        husk.setNoAi(true);
        check(com.kolodkingal.enlightened.earth.Mud.near(helper.getLevel(), p.blockPosition()), "the arena's ground is soil to bend");
        f.press(p, Ability.MUD_WAVE, false);
        helper.succeedWhen(() -> {
            check(husk.getHealth() < husk.getMaxHealth(), "the wave strikes");
            check(husk.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    && husk.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN).getDuration() > 100, "and bogs it down for seven seconds");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void aWaterbenderThrowsMud(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, TestKit.FLOOR, 3.5);
        f.bender(p, Element.WATER, Ability.MUD_THROW, 2);
        helper.setBlock(new BlockPos(9, 1, 3), Blocks.MUD);
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(10, 2, 10));
        husk.setNoAi(true);
        f.lookAt(p, husk);
        f.press(p, Ability.MUD_THROW, false);
        var shots = helper.getLevel().getEntitiesOfClass(com.kolodkingal.enlightened.entity.BendingShotEntity.class, new AABB(p.blockPosition()).inflate(4));
        check(shots.size() == 2 && shots.get(0).blockState().is(Blocks.MUD), "two balls of mud fly");
        helper.succeedWhen(() -> check(husk.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) && husk.getHealth() < husk.getMaxHealth(),
                "the mud hits and slows"));
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void thePlantGiantNeedsPlantsAndFearsFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, TestKit.FLOOR, 10.5);
        var data = f.bender(p, Element.WATER, Ability.PLANT_GIANT, 3);
        f.press(p, Ability.PLANT_GIANT, false);
        check(!com.kolodkingal.enlightened.water.PlantGiant.wearing(data), "no giant without plants");
        for (int x = 4; x <= 16; x += 2) {
            for (int z = 4; z <= 16; z += 3) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.OAK_LEAVES);
            }
        }
        data.clearCooldowns();
        float scale = p.getScale();
        f.press(p, Ability.PLANT_GIANT, false);
        check(com.kolodkingal.enlightened.water.PlantGiant.wearing(data), "among plants the giant rises");
        check(p.getScale() > scale * 1.5F, "a giant (" + p.getScale() + ")");
        float vigor = com.kolodkingal.enlightened.water.PlantGiant.vigor(p);
        float taken = com.kolodkingal.enlightened.water.PlantGiant.absorb(p, data, 4.0F, false);
        check(taken < 4.0F, "the plants take most of a blow");
        float afterBlow = com.kolodkingal.enlightened.water.PlantGiant.vigor(p);
        float burnt = com.kolodkingal.enlightened.water.PlantGiant.absorb(p, data, 4.0F, true);
        check(burnt > 4.0F, "fire hurts the giant more");
        check(afterBlow - com.kolodkingal.enlightened.water.PlantGiant.vigor(p) > 3.0F * (vigor - afterBlow), "and tears it apart faster");
        for (int i = 0; i < 10 && com.kolodkingal.enlightened.water.PlantGiant.wearing(data); i++) {
            com.kolodkingal.enlightened.water.PlantGiant.absorb(p, data, 6.0F, true);
        }
        check(!com.kolodkingal.enlightened.water.PlantGiant.wearing(data) && p.getScale() < scale * 1.1F, "burnt through, the plants fall away");
        helper.succeed();
    }
}
