package com.kolodkingal.enlightened.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Combos;
import com.kolodkingal.enlightened.bending.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** 1.0.25: the new combos, and finding them. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public class ComboTests {
    private static JsonObject lang(String name) {
        try (var in = ComboTests.class.getResourceAsStream("/assets/enlightened/lang/" + name + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void everyComboIsNamedAndNoneHidesAnother(GameTestHelper helper) {
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> missing = new ArrayList<>();
        for (Combos c : Combos.VALUES) {
            for (String k : List.of("combo.enlightened." + c.id(), "combo.enlightened." + c.id() + ".desc")) {
                if (!en.has(k) || !he.has(k)) {
                    missing.add(k);
                }
            }
        }
        check(missing.isEmpty(), "missing: " + missing);
        check(Combos.VALUES.length >= 21, "thirteen more combos (" + Combos.VALUES.length + ")");
        // No combo's sequence is the end of another's that the same bender could do (it would always win first).
        for (Combos a : Combos.VALUES) {
            for (Combos b : Combos.VALUES) {
                if (a == b || a.sequence.size() > b.sequence.size()) {
                    continue;
                }
                List<Ability> tail = b.sequence.subList(b.sequence.size() - a.sequence.size(), b.sequence.size());
                check(!tail.equals(a.sequence), a + " hides " + b);
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void theBurningWallBurnsWhoeverTouchesIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(4.5, TestKit.FLOOR, 1.5);
        BendingData data = f.bender(p, Element.AVATAR, Ability.EARTH_WALL, 1, Ability.FIRE_BALL, 1);
        p.setYRot(0.0F);
        p.setYHeadRot(0.0F);
        check(!Combos.discovered(p).contains("burning_wall"), "not found yet");
        Combos.cast(p, data, Ability.EARTH_WALL);
        Combos.cast(p, data, Ability.FIRE_BALL);
        BlockPos wall = BlockPos.containing(p.position().add(0, 0, 2.5));
        check(helper.getLevel().getBlockState(wall).is(Blocks.MAGMA_BLOCK), "a wall of magma ahead: " + helper.getLevel().getBlockState(wall));
        check(Combos.discovered(p).contains("burning_wall"), "found");
        check(!Combos.discover(p, Combos.BURNING_WALL), "found once only");
        Husk husk = EntityType.HUSK.create(helper.getLevel());
        BlockPos at = wall.north();
        husk.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        husk.setNoAi(true);
        helper.getLevel().addFreshEntity(husk);
        helper.succeedWhen(() -> check(husk.isOnFire() || !husk.isAlive(), "touching the wall, it burns"));
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void theThunderJetStrikesTheEnemiesNear(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        BendingData data = f.bender(p, Element.FIRE, Ability.FIRE_JET, 1, Ability.LIGHTNING, 1);
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(3, 2, 5));
        husk.setNoAi(true);
        float before = husk.getHealth();
        Combos.cast(p, data, Ability.FIRE_JET);
        Combos.cast(p, data, Ability.LIGHTNING);
        check(husk.getHealth() < before || !husk.isAlive(), "the lightning leaps to the husk");
        check(Combos.discovered(p).contains("thunder_jet"), "found");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aComboNeedsItsElementAndItsArts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        BendingData data = f.bender(p, Element.EARTH, Ability.EARTH_WALL, 1);
        Combos.cast(p, data, Ability.EARTH_WALL);
        Combos.cast(p, data, Ability.FIRE_BALL);
        check(!Combos.discovered(p).contains("burning_wall"), "an earthbender has no fire to burn the wall with");
        helper.succeed();
    }
}
