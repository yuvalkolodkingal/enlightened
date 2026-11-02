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
}
