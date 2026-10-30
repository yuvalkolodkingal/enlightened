package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.world.BaSingSePiece;
import com.kolodkingal.enlightened.world.OmashuPiece;
import com.kolodkingal.enlightened.world.SpiritWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of the world of 1.0: Ba Sing Se, Omashu, the spirit portals and the Spirit World. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class WorldTests {

    /** The plan of a design piece, built at a far origin so its design can be read. */
    private static net.minecraft.world.level.block.state.BlockState at(Object piece, int x, int y, int z) {
        try {
            Method m = piece.getClass().getDeclaredMethod("design", int.class, int.class, int.class);
            m.setAccessible(true);
            return (net.minecraft.world.level.block.state.BlockState) m.invoke(piece, x, y, z);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void baSingSeHasItsThreeWallsAndThePalace(GameTestHelper helper) {
        BaSingSePiece city = new BaSingSePiece(BlockPos.ZERO);
        check(at(city, 98, 20, 40).is(Blocks.STONE_BRICKS) || at(city, 98, 20, 40).is(Blocks.CRACKED_STONE_BRICKS), "the Outer Wall stands 26 high");
        check(at(city, 100, 17, 0).is(Blocks.GOLD_BLOCK), "the emblem over the gate");
        check(at(city, 98, 4, 0).isAir(), "the Outer Wall's gate is open");
        check(!at(city, 61, 8, 30).isAir(), "the Middle Wall");
        check(!at(city, 31, 8, 20).isAir(), "the Inner Wall");
        check(at(city, 45, 0, 20).is(Blocks.WATER), "the Middle Ring's canal");
        check(at(city, 0, 26, 0).is(Blocks.GOLD_BLOCK), "the palace's golden ridge");
        int roofs = 0;
        for (int x = -95; x <= 95; x++) {
            for (int z = -95; z <= 95; z++) {
                var b = at(city, x, 4, z);
                if (b != null && b.is(Blocks.GREEN_TERRACOTTA)) {
                    roofs++;
                }
            }
        }
        check(roofs > 1500, "rings of houses under green roofs (" + roofs + ")");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void omashuRisesFromItsChasmOverOneBridge(GameTestHelper helper) {
        OmashuPiece city = new OmashuPiece(BlockPos.ZERO);
        check(at(city, 60, -10, 0).isAir(), "a deep chasm rings the city");
        check(at(city, 60, -OmashuPiece.DEEP, 0).is(Blocks.WATER), "a river at its bottom");
        check(!at(city, 0, 2, 60).isAir(), "one bridge crosses it");
        check(OmashuPiece.surface(0, 30) > OmashuPiece.surface(0, 40) && OmashuPiece.surface(0, 10) > OmashuPiece.surface(0, 30), "the stairway climbs the mountain");
        check(OmashuPiece.surface(20, -20) == OmashuPiece.H[OmashuPiece.tier(Math.sqrt(800))], "the terraces are level");
        boolean gold = false;
        for (int y = 40; y < 56; y++) {
            var b = at(city, 0, y, 0);
            gold |= b != null && b.is(Blocks.GOLD_BLOCK);
        }
        check(gold, "the palace on the summit has its golden tip");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void aSpiritGateHoldsThePortal(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(6, 1, 6));
        SpiritWorld.gate(helper.getLevel(), base, Direction.Axis.X, true);
        check(helper.getLevel().getBlockState(base.above(3)).is(ModBlocks.SPIRIT_PORTAL.get()), "the gate holds the portal's light");
        check(helper.getLevel().getBlockState(base.offset(2, 2, 0)).is(Blocks.MOSSY_STONE_BRICKS), "between pillars of mossy stone");
        helper.succeed();
    }
}
