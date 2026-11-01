package com.kolodkingal.enlightened.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.block.GoldenDragonEggBlockEntity;
import com.kolodkingal.enlightened.entity.DragonEntity;
import com.kolodkingal.enlightened.entity.SunWarriorEntity;
import com.kolodkingal.enlightened.item.EternalFlameItem;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.registry.ModItems;
import com.kolodkingal.enlightened.world.SunWarriorPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of the Sun Warriors' city: its plan, the chief's offer, the Eternal Flame, Ran and Shaw, the egg and the dragon. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class SunWarriorTests {

    private static BlockState at(SunWarriorPiece piece, int x, int y, int z) {
        try {
            Method m = SunWarriorPiece.class.getDeclaredMethod("design", int.class, int.class, int.class);
            m.setAccessible(true);
            return (BlockState) m.invoke(piece, x, y, z);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject lang(String name) {
        try (var in = SunWarriorTests.class.getResourceAsStream("/assets/enlightened/lang/" + name + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theCityRunsToTheMountainOfTheMasters(GameTestHelper helper) {
        SunWarriorPiece city = new SunWarriorPiece(BlockPos.ZERO);
        int m = SunWarriorPiece.MOUNTAIN;
        int top = SunWarriorPiece.SUMMIT;
        check(at(city, 0, top, m).is(Blocks.GOLD_BLOCK), "the sun's golden heart in the middle of the summit");
        check(at(city, 0, top + 1, m).isAir() && at(city, 6, top + 1, m + 4).isAir(), "an open platform");
        for (int side = -1; side <= 1; side += 2) {
            int cx = side * SunWarriorPiece.CAVE;
            check(at(city, cx, top + 3, m).isAir() && at(city, side * (SunWarriorPiece.PLATFORM - 2), top + 2, m).isAir(), "a cave on each side, open to the platform");
            check(!at(city, cx, top, m).isAir() && !at(city, cx, top + 13, m).isAir(), "with a floor, under a crag of rock");
        }
        // The stairway climbs one block a step, from the foot to the summit, broken only by the terrace.
        for (int t = SunWarriorPiece.FOOT; t > SunWarriorPiece.PLATFORM; t--) {
            int h = SunWarriorPiece.stepHeight(t);
            check(Math.abs(SunWarriorPiece.stepHeight(t - 1) - h) <= 1, "no step higher than one (" + t + ")");
            int z = SunWarriorPiece.t2z(t);
            if (SunWarriorPiece.stairway(0, z)) {
                check(at(city, 0, h, z).is(Blocks.STONE_BRICK_STAIRS) && at(city, 0, h + 1, z).isAir(), "a stair to climb at " + t);
                check(at(city, 3, h + 1, z).is(Blocks.STONE_BRICK_WALL) || at(city, 3, h + 1, z).is(Blocks.MOSSY_STONE_BRICK_WALL), "between railings");
            }
        }
        check(SunWarriorPiece.stepHeight(SunWarriorPiece.FOOT) == 0 && SunWarriorPiece.stepHeight(SunWarriorPiece.PLATFORM + 1) == top - 1, "from the ground to the summit");
        check(at(city, 0, SunWarriorPiece.TERRACE + 1, SunWarriorPiece.pitZ()).is(Blocks.CAMPFIRE), "the ceremony's fire on the terrace before the last stair");
        // The city: the traps in the doorways and on the pyramids, the statues, the columns.
        int[] house = SunWarriorPiece.HOUSES[1];
        BlockState trap = at(city, house[0] - 2, 0, house[1]);
        check(trap.is(Blocks.DISPENSER) && trap.getValue(DispenserBlock.FACING) == Direction.UP, "a dispenser hidden in a house's floor");
        check(at(city, house[0] - 2, 1, house[1]).is(Blocks.STONE_PRESSURE_PLATE) && at(city, house[0] - 3, 1, house[1]).isAir(), "under a plate inside its door");
        int[] pyr = SunWarriorPiece.PYRAMIDS[1];
        check(at(city, pyr[0] - 1, 15, pyr[1]).is(Blocks.DISPENSER) && at(city, pyr[0] - 1, 16, pyr[1]).is(Blocks.STONE_PRESSURE_PLATE), "and atop each pyramid");
        check(at(city, pyr[0] - 9, 1, pyr[1] + 5).is(Blocks.STONE_BRICKS) || at(city, pyr[0] - 9, 1, pyr[1] + 5).is(Blocks.MOSSY_STONE_BRICKS)
                || at(city, pyr[0] - 9, 1, pyr[1] + 5).is(Blocks.CRACKED_STONE_BRICKS), "a step pyramid's base");
        int red = 0;
        int blue = 0;
        for (int[] s : SunWarriorPiece.STATUES) {
            for (int y = 3; y < 14; y++) {
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockState b = at(city, s[0] + dx, y, s[1] + dz);
                        red += b != null && b.is(Blocks.RED_CONCRETE) ? 1 : 0;
                        blue += b != null && b.is(Blocks.BLUE_CONCRETE) ? 1 : 0;
                    }
                }
            }
        }
        check(red > 40 && blue > 40, "red dragons and blue in stone (" + red + ", " + blue + ")");
        check(!at(city, 5, 3, 36).isAir(), "a column by the avenue");
        helper.succeed();
    }

    private static SunWarriorEntity warrior(GameTestHelper helper, SunWarriorEntity.Role role, int x, int z) {
        SunWarriorEntity w = ModEntities.SUN_WARRIOR.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        w.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        w.station(role, 0, at);
        helper.getLevel().addFreshEntity(w);
        return w;
    }

    private static int flames(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(ModItems.ETERNAL_FLAME.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theChiefOffersTheTestAndGivesTheFlame(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        f.bender(p, Element.FIRE);
        SunWarriorEntity chief = warrior(helper, SunWarriorEntity.Role.CHIEF, 3, 3);
        check(chief.line(p).equals("sun.enlightened.chief.offer") && flames(p) == 0, "first he asks");
        check(chief.line(p).equals("sun.enlightened.chief.given") && flames(p) == 1, "asked again, he gives the Eternal Flame");
        check(EternalFlameItem.carries(p), "alight");
        check(chief.line(p).equals("sun.enlightened.chief.hurry") && flames(p) == 1, "only one; hurry");
        Benders.get(p).setDragonBlessed(true);
        check(chief.line(p).equals("sun.enlightened.chief.blessed"), "the blessed need no test");
        SunWarriorEntity w = warrior(helper, SunWarriorEntity.Role.WARRIOR, 5, 5);
        SunWarriorEntity d = warrior(helper, SunWarriorEntity.Role.DANCER, 6, 6);
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        java.util.Set<String> keys = new java.util.HashSet<>(List.of("sun.enlightened.chief.offer", "sun.enlightened.chief.given",
                "sun.enlightened.chief.hurry", "sun.enlightened.chief.blessed", "sun.enlightened.busy", "sun.enlightened.says",
                "entity.enlightened.sun_warrior.chief", "entity.enlightened.sun_warrior.dancer", "entity.enlightened.sun_warrior.warrior",
                "item.enlightened.eternal_flame", "block.enlightened.golden_dragon_egg", "entity.enlightened.dragon"));
        for (int i = 0; i < 8; i++) {
            keys.add(w.line(p));
            keys.add(d.line(p));
        }
        for (String k : keys) {
            check(en.has(k) && he.has(k), k + " in English and in Hebrew");
        }
        check(!w.canAttack(chief) && w.fireImmune(), "Sun Warriors never fight each other, and fire does not harm them");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theEternalFlameGoesOutInSevenMinutes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        long now = helper.getLevel().getGameTime();
        check(EternalFlameItem.BURN == 8400, "seven minutes");
        ItemStack fresh = EternalFlameItem.lit(now);
        check(EternalFlameItem.left(fresh, now + 100) == EternalFlameItem.BURN - 100, "it burns down");
        p.getInventory().add(EternalFlameItem.lit(now - EternalFlameItem.BURN + 5));
        check(EternalFlameItem.carries(p), "still alight, just");
        helper.runAfterDelay(20, () -> {
            check(flames(p) == 0 && !EternalFlameItem.carries(p), "and then it is gone");
            helper.succeed();
        });
    }

    private static DragonEntity master(GameTestHelper helper, DragonEntity.Colour colour, int caveX) {
        DragonEntity d = ModEntities.DRAGON.get().create(helper.getLevel());
        d.makeMaster(colour, helper.absolutePos(new BlockPos(caveX, 6, 10)), helper.absolutePos(new BlockPos(10, 2, 10)));
        helper.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest(template = ARENA, timeoutTicks = 420)
    public static void ranAndShawBlessWhoeverBringsTheFlame(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, TestKit.FLOOR, 10.5);
        BendingData data = f.bender(p, Element.FIRE);
        p.getInventory().add(EternalFlameItem.lit(helper.getLevel().getGameTime()));
        DragonEntity ran = master(helper, DragonEntity.Colour.RED, 2);
        DragonEntity shaw = master(helper, DragonEntity.Colour.BLUE, 18);
        check(!ran.hurt(helper.getLevel().damageSources().generic(), 100.0F), "nothing harms the masters");
        helper.runAfterDelay(30, () -> check(ran.phase() != DragonEntity.Phase.SLEEP && shaw.phase() != DragonEntity.Phase.SLEEP, "they wake when someone comes"));
        helper.succeedWhen(() -> {
            check(data.dragonBlessed(), "they bless him");
            check(flames(p) == 0, "he gives them the flame");
            check(p.getInventory().countItem(ModBlocks.GOLDEN_DRAGON_EGG_ITEM.get()) == 1, "Ran gives him one golden egg");
            check(p.getHealth() == p.getMaxHealth(), "and he is not burnt");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void ranAndShawBurnWhoeverComesWithoutIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, TestKit.FLOOR, 10.5);
        BendingData data = f.bender(p, Element.FIRE);
        master(helper, DragonEntity.Colour.RED, 2);
        helper.succeedWhen(() -> {
            check(p.getHealth() < p.getMaxHealth() || !p.isAlive(), "they burn him");
            check(!data.dragonBlessed() && p.getInventory().countItem(ModBlocks.GOLDEN_DRAGON_EGG_ITEM.get()) == 0, "no blessing, no egg");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theGoldenEggHatchesTheHatchersDragon(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        BlockPos at = helper.absolutePos(new BlockPos(5, 2, 5));
        helper.getLevel().setBlock(at, ModBlocks.GOLDEN_DRAGON_EGG.get().defaultBlockState(), 3);
        GoldenDragonEggBlockEntity egg = (GoldenDragonEggBlockEntity) helper.getLevel().getBlockEntity(at);
        check(egg != null, "the egg keeps its time");
        egg.laid(helper.getLevel().getGameTime() - GoldenDragonEggBlockEntity.HATCH + 5, p.getUUID());
        check(GoldenDragonEggBlockEntity.HATCH == 12000, "ten minutes");
        helper.succeedWhen(() -> {
            check(helper.getLevel().getBlockState(at).isAir(), "it cracks open (" + helper.getLevel().getBlockState(at) + " be=" + helper.getLevel().getBlockEntity(at)
                    + (helper.getLevel().getBlockEntity(at) instanceof GoldenDragonEggBlockEntity e ? " laid=" + e.laidAt() + " now=" + helper.getLevel().getGameTime() : "") + ")");
            List<DragonEntity> pups = helper.getLevel().getEntitiesOfClass(DragonEntity.class, new net.minecraft.world.phys.AABB(at).inflate(3));
            check(pups.size() == 1, "a dragon comes out");
            DragonEntity pup = pups.get(0);
            check(pup.isBaby() && pup.isTame() && pup.isOwnedBy(p) && !pup.master(), "a pup, its hatcher's");
            check(pup.getMaxHealth() >= 200.0F, "with a great deal of health");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void aDragonBreathesFireAtEnemiesNotItsOwner(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(10.5, TestKit.FLOOR, 4.5);
        DragonEntity dragon = ModEntities.DRAGON.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(10, 2, 8));
        dragon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        dragon.setColour(DragonEntity.Colour.BLUE);
        dragon.tame(p);
        dragon.setOrderedToSit(true);
        helper.getLevel().addFreshEntity(dragon);
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(10, 2, 15));
        husk.setNoAi(true);
        check(dragon.spares(p) && !dragon.spares(husk), "its fire spares its owner, not a monster");
        check(dragon.colour().fire == com.kolodkingal.enlightened.bending.Variants.FIRE_BLUE, "a blue dragon breathes blue fire");
        helper.runAfterDelay(2, () -> {
            Vec3 dir = husk.getEyePosition().subtract(dragon.mouth());
            check(dragon.breathe(dir), "it breathes");
        });
        helper.succeedWhen(() -> {
            check(husk.getHealth() < husk.getMaxHealth() || !husk.isAlive(), "the monster burns");
            check(p.getHealth() == p.getMaxHealth(), "the owner does not");
        });
    }
}
