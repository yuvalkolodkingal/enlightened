package com.kolodkingal.enlightened.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.entity.CityGuardEntity;
import com.kolodkingal.enlightened.entity.KingBumiEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.world.OmashuPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of the cities' people: the guards of Omashu, Ba Sing Se and Ellora, and King Bumi on his throne. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class CityTests {

    private static CityGuardEntity guard(GameTestHelper helper, CityGuardEntity.City city, int voice, int x, int z) {
        CityGuardEntity g = ModEntities.CITY_GUARD.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        g.station(city, voice, at);
        helper.getLevel().addFreshEntity(g);
        return g;
    }

    private static JsonObject lang(String name) {
        try (var in = CityTests.class.getResourceAsStream("/assets/enlightened/lang/" + name + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void aGuardFightsTheMonsterThatComesNear(GameTestHelper helper) {
        CityGuardEntity g = guard(helper, CityGuardEntity.City.OMASHU, 0, 2, 2);
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(6, 2, 6));
        helper.succeedWhen(() -> {
            check(!husk.isAlive(), "the guard cuts the husk down");
            check(g.isAlive(), "and stands");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aGuardNeverTurnsOnHisOwn(GameTestHelper helper) {
        CityGuardEntity a = guard(helper, CityGuardEntity.City.BA_SING_SE, 0, 2, 2);
        CityGuardEntity b = guard(helper, CityGuardEntity.City.ELLORA, 1, 4, 2);
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 2, 4));
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(5, 2, 5));
        check(!a.canAttack(b) && !a.canAttack(v), "not another guard, not the townsfolk");
        check(a.canAttack(husk), "but a monster, yes");
        check(a.hasRestriction() && a.getRestrictCenter().equals(helper.absolutePos(new BlockPos(2, 2, 2))), "he keeps his post");
        check(a.getMainHandItem().is(net.minecraft.world.item.Items.IRON_SWORD) && b.getMainHandItem().is(net.minecraft.world.item.Items.GOLDEN_SWORD),
                "each city arms its guards its own way");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void eachCitysGuardsTalkOfTheirOwnCity(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        f.bender(p, Element.EARTH);
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        java.util.Set<String> heard = new java.util.HashSet<>();
        for (CityGuardEntity.City city : CityGuardEntity.City.values()) {
            check(en.has("entity.enlightened.city_guard." + city.id) && he.has("entity.enlightened.city_guard." + city.id), city.id + " guards are named");
            for (int voice = 0; voice < CityGuardEntity.VOICES; voice++) {
                CityGuardEntity g = guard(helper, city, voice, 2 + voice, 3);
                String first = g.line(p);
                check(first.equals("guard.enlightened." + city.id + ".greet." + voice), "a guard greets first, in his own words (" + first + ")");
                heard.add(first);
                for (int i = 0; i < CityGuardEntity.LINES; i++) {
                    String line = g.line(p);
                    check(line.startsWith("guard.enlightened." + city.id + ".line."), "then the talk of " + city.id + " (" + line + ")");
                    heard.add(line);
                }
                g.discard();
            }
            check(heard.add("guard.enlightened." + city.id + ".avatar"), "the Avatar is greeted apart");
        }
        check(heard.size() == 3 * (CityGuardEntity.VOICES + CityGuardEntity.LINES + 1), "every line is heard (" + heard.size() + ")");
        heard.add("guard.enlightened.busy");
        heard.add("guard.enlightened.says");
        for (String key : heard) {
            check(en.has(key) && he.has(key), key + " is in English and in Hebrew");
        }
        // The Avatar hears the city's word for him after the greeting; a guard in a fight has no time to talk.
        f.bender(p, Element.AVATAR);
        CityGuardEntity g = guard(helper, CityGuardEntity.City.OMASHU, 2, 4, 4);
        g.line(p);
        check(g.line(p).equals("guard.enlightened.omashu.avatar"), "the Avatar is known");
        g.setTarget(helper.spawn(EntityType.HUSK, new BlockPos(6, 2, 6)));
        check(g.line(p).equals("guard.enlightened.busy"), "not now, there's a monster about");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void kingBumiSitsOnHisThroneInOmashu(GameTestHelper helper) throws ReflectiveOperationException {
        OmashuPiece city = new OmashuPiece(BlockPos.ZERO);
        Method design = OmashuPiece.class.getDeclaredMethod("design", int.class, int.class, int.class);
        design.setAccessible(true);
        BlockPos t = OmashuPiece.THRONE;
        var seat = (net.minecraft.world.level.block.state.BlockState) design.invoke(city, t.getX(), t.getY(), t.getZ());
        var arm = (net.minecraft.world.level.block.state.BlockState) design.invoke(city, t.getX() + 1, t.getY(), t.getZ());
        var floor = (net.minecraft.world.level.block.state.BlockState) design.invoke(city, 0, t.getY() - 1, 3);
        var roof = (net.minecraft.world.level.block.state.BlockState) design.invoke(city, 0, t.getY() + 8, 0);
        var back = (net.minecraft.world.level.block.state.BlockState) design.invoke(city, t.getX(), t.getY(), t.getZ() - 1);
        check(seat.is(Blocks.SMOOTH_SANDSTONE_SLAB) && arm.is(Blocks.GOLD_BLOCK) && back.is(Blocks.GOLD_BLOCK), "a golden throne in the hall");
        check(floor.is(Blocks.GREEN_CARPET), "a green aisle leads to it");
        check(!roof.isAir(), "the hall is under the pyramid");
        KingBumiEntity king = ModEntities.KING_BUMI.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
        king.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(king);
        check(!king.hurt(helper.getLevel().damageSources().generic(), 50.0F) && king.getHealth() == king.getMaxHealth(), "nothing harms the king");
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        f.bender(p, Element.EARTH);
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        for (int i = 0; i < KingBumiEntity.LINES; i++) {
            String line = king.line(p);
            check(line.equals("bumi.enlightened.line." + i) && en.has(line) && he.has(line), "the king's talk, in both tongues (" + line + ")");
        }
        check(en.has("bumi.enlightened.avatar") && he.has("bumi.enlightened.avatar") && he.has("entity.enlightened.king_bumi"), "and his word for the Avatar");
        helper.succeed();
    }
}
