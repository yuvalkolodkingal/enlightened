package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Combos;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.TeamCombos;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.entity.TornadoEntity;
import com.kolodkingal.enlightened.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.FLOOR;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** 1.1.0: the team combos, two benders of different elements casting into each other's bending. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class TeamComboTests {

    private static Husk husk(GameTestHelper helper, int x, int z) {
        Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(x, 2, z));
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        return husk;
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void everyPairOfElementsHasATeamCombo(GameTestHelper helper) {
        List<Element> four = List.of(Element.AIR, Element.WATER, Element.EARTH, Element.FIRE);
        for (int i = 0; i < four.size(); i++) {
            for (int j = i + 1; j < four.size(); j++) {
                Set<Element> pair = EnumSet.of(four.get(i), four.get(j));
                boolean found = false;
                for (Combos c : Combos.VALUES) {
                    if (c.team && pair.equals(EnumSet.of(c.sequence.get(0).element, c.sequence.get(1).element))) {
                        found = true;
                    }
                }
                check(found, "a team combo for " + pair);
            }
        }
        for (Combos c : Combos.VALUES) {
            if (!c.team) {
                continue;
            }
            for (Ability a : c.first) {
                for (Ability b : c.second) {
                    check(a.element != b.element, c + ": its two sides are of different elements");
                }
            }
            check(c.shownTo(c.sequence.get(0).element) && c.shownTo(c.sequence.get(1).element), c + " is shown to both sides");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void fireCastIntoATornadoMakesAFireTornado(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer air = f.player(10.5, FLOOR, 3.5);
        ServerPlayer fire = f.player(16.5, FLOOR, 5.5);
        f.bender(air, Element.AIR, Ability.TORNADO, 2);
        BendingData fd = f.bender(fire, Element.FIRE, Ability.FIRE_BALL, 2);
        f.press(air, Ability.TORNADO, false);
        List<TornadoEntity> tornadoes = helper.getLevel().getEntitiesOfClass(TornadoEntity.class, new AABB(air.blockPosition()).inflate(6));
        check(tornadoes.size() == 1, "the airbender's tornado (" + tornadoes.size() + ")");
        TornadoEntity t = tornadoes.get(0);
        int left = t.left();
        float damage = t.damage();
        f.lookAt(fire, t.position().add(0, t.size() * 1.2, 0).subtract(helper.absoluteVec(Vec3.ZERO)));
        Combos.cast(fire, fd, Ability.FIRE_BALL);
        check(t.kind() == TornadoEntity.Kind.FIRE, "the tornado catches fire (" + t.kind() + ")");
        check(t.left() < left * 0.6, "it lives a much shorter while (" + t.left() + " of " + left + ")");
        check(t.damage() >= damage * 2.5F, "and strikes far harder (" + t.damage() + " from " + damage + ")");
        check(Combos.discovered(air).contains("firestorm") && Combos.discovered(fire).contains("firestorm"), "both benders find it");
        check(!TeamCombos.ready(air, Combos.FIRESTORM) && !TeamCombos.ready(fire, Combos.FIRESTORM), "and both rest after it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aFireBallFlownIntoATornadoSetsItAlight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer air = f.player(4.5, FLOOR, 3.5);
        ServerPlayer fire = f.player(16.5, FLOOR, 3.5);
        f.bender(air, Element.AIR, Ability.TORNADO, 1);
        f.bender(fire, Element.FIRE, Ability.FIRE_BALL, 1);
        Vec3 at = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        TornadoEntity t = new TornadoEntity(helper.getLevel(), air, at, new Vec3(0, 0, 1), 2.0F, 200, 1.0F, false);
        helper.getLevel().addFreshEntity(t);
        BendingShotEntity own = new BendingShotEntity(helper.getLevel(), BendingShotEntity.Kind.FIRE_BALL, at.add(0, 1.5, 0), air, 0xFF6600);
        check(!TeamCombos.shotIntoTornado(helper.getLevel(), own, air), "an airbender's own shot does not join his tornado");
        BendingShotEntity shot = new BendingShotEntity(helper.getLevel(), BendingShotEntity.Kind.FIRE_BALL, at.add(0, 1.5, 0), fire, 0xFF6600);
        helper.getLevel().addFreshEntity(shot);
        check(TeamCombos.shotIntoTornado(helper.getLevel(), shot, fire), "the fire ball flies into the tornado");
        check(t.kind() == TornadoEntity.Kind.FIRE && shot.isRemoved(), "the tornado takes it and burns");
        check(fire.getUUID().equals(t.partner()), "the firebender fed it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void waterCastIntoATornadoMakesAHurricane(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer air = f.player(10.5, FLOOR, 3.5);
        ServerPlayer water = f.player(4.5, FLOOR, 5.5);
        f.bender(air, Element.AIR, Ability.TORNADO, 1);
        BendingData wd = f.bender(water, Element.WATER, Ability.WATER_VORTEX, 1);
        f.press(air, Ability.TORNADO, false);
        TornadoEntity t = helper.getLevel().getEntitiesOfClass(TornadoEntity.class, new AABB(air.blockPosition()).inflate(6)).get(0);
        float size = t.size();
        int left = t.left();
        f.lookAt(water, t.position().add(0, t.size() * 1.2, 0).subtract(helper.absoluteVec(Vec3.ZERO)));
        Combos.cast(water, wd, Ability.WATER_VORTEX);
        check(t.kind() == TornadoEntity.Kind.WATER, "a water tornado (" + t.kind() + ")");
        check(t.size() > size * 1.4F && t.left() > left, "bigger and longer-lived (" + t.size() + ", " + t.left() + ")");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void fireOnWaterHangsAScaldingSteam(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer water = f.player(4.5, FLOOR, 3.5);
        ServerPlayer fire = f.player(16.5, FLOOR, 3.5);
        BendingData wd = f.bender(water, Element.WATER, Ability.WATER_WHIP, 1);
        BendingData fd = f.bender(fire, Element.FIRE, Ability.FIRE_BALL, 1);
        Husk husk = husk(helper, 10, 12);
        f.lookAt(water, husk);
        f.lookAt(fire, husk);
        Combos.cast(water, wd, Ability.WATER_WHIP);
        Combos.cast(fire, fd, Ability.FIRE_BALL);
        check(Combos.discovered(fire).contains("scalding_steam"), "water met by fire: steam");
        helper.succeedWhen(() -> check(husk.getHealth() < husk.getMaxHealth() && husk.hasEffect(MobEffects.BLINDNESS),
                "the steam scalds and blinds whoever is in it"));
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void fireOnEarthErupts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer earth = f.player(4.5, FLOOR, 3.5);
        ServerPlayer fire = f.player(16.5, FLOOR, 3.5);
        BendingData ed = f.bender(earth, Element.EARTH, Ability.ROCK_THROW, 1);
        BendingData fd = f.bender(fire, Element.FIRE, Ability.FIRE_BALL, 1);
        Husk husk = husk(helper, 10, 12);
        f.lookAt(earth, husk);
        f.lookAt(fire, husk);
        float before = husk.getHealth();
        Combos.cast(earth, ed, Ability.ROCK_THROW);
        Combos.cast(fire, fd, Ability.FIRE_BALL);
        BlockPos under = helper.absolutePos(new BlockPos(10, 1, 12));
        check(helper.getLevel().getBlockState(under).is(Blocks.MAGMA_BLOCK), "the ground melts (" + helper.getLevel().getBlockState(under) + ")");
        check(husk.getHealth() < before && husk.isOnFire(), "the eruption burns and strikes");
        check(Combos.discovered(earth).contains("magma_eruption"), "found");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void earthChurnedIntoWaterFloodsWithMud(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer water = f.player(4.5, FLOOR, 3.5);
        ServerPlayer earth = f.player(16.5, FLOOR, 3.5);
        BendingData wd = f.bender(water, Element.WATER, Ability.WATER_WHIP, 1);
        BendingData ed = f.bender(earth, Element.EARTH, Ability.SHOCKWAVE, 1);
        Husk husk = husk(helper, 10, 12);
        f.lookAt(water, husk);
        f.lookAt(earth, husk);
        Combos.cast(water, wd, Ability.WATER_WHIP);
        Combos.cast(earth, ed, Ability.SHOCKWAVE);
        helper.succeedWhen(() -> check(husk.getHealth() < husk.getMaxHealth() && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "the mud strikes and bogs down"));
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void lightningIntoWaterAndMetal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer water = f.player(4.5, FLOOR, 3.5);
        ServerPlayer fire = f.player(16.5, FLOOR, 3.5);
        ServerPlayer metal = f.player(10.5, FLOOR, 2.5);
        BendingData wd = f.bender(water, Element.WATER, Ability.WATER_WHIP, 1);
        BendingData fd = f.bender(fire, Element.FIRE, Ability.LIGHTNING, 1);
        BendingData md = f.bender(metal, Element.EARTH, Ability.METAL_BLADES, 1);
        Husk wet = husk(helper, 6, 12);
        Husk far = husk(helper, 15, 12);
        f.lookAt(water, wet);
        f.lookAt(fire, wet);
        Combos.cast(water, wd, Ability.WATER_WHIP);
        Combos.cast(fire, fd, Ability.LIGHTNING);
        check(wet.getHealth() < wet.getMaxHealth() && wet.hasEffect(ModEffects.STUNNED), "the lightning runs through the water");
        check(Combos.discovered(fire).contains("storm_surge"), "storm surge found");
        // The lightning rod: a different combo, so the firebender may cast it at once.
        f.lookAt(metal, far);
        f.lookAt(fire, far);
        float before = far.getHealth();
        Combos.cast(metal, md, Ability.METAL_BLADES);
        Combos.cast(fire, fd, Ability.LIGHTNING);
        check(far.getHealth() < before, "the bolt leaps from the metal to the enemy");
        check(Combos.discovered(metal).contains("lightning_rod"), "lightning rod found");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aTeamComboIsNeverOneBenderAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // One Avatar alone casting both arts makes his own combo, never a team one.
        ServerPlayer avatar = f.player(10.5, FLOOR, 3.5);
        BendingData ad = f.bender(avatar, Element.AVATAR, Ability.WATER_WHIP, 1, Ability.FIRE_BALL, 1);
        Combos.cast(avatar, ad, Ability.WATER_WHIP);
        Combos.cast(avatar, ad, Ability.FIRE_BALL);
        check(!Combos.discovered(avatar).contains("scalding_steam"), "no team of one");
        check(Combos.discovered(avatar).contains("steam_cloud"), "but his own steam cloud");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aTeamComboTakesTwoBendersCloseTogether(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // Two benders aiming far apart do not join.
        ServerPlayer water = f.player(2.5, FLOOR, 3.5);
        ServerPlayer fire = f.player(18.5, FLOOR, 3.5);
        BendingData wd = f.bender(water, Element.WATER, Ability.WATER_WHIP, 1);
        BendingData fd = f.bender(fire, Element.FIRE, Ability.FIRE_BALL, 1);
        f.lookAt(water, new Vec3(2.5, 2.0, 18.5));
        f.lookAt(fire, new Vec3(18.5, 2.0, 18.5));
        Combos.cast(water, wd, Ability.WATER_WHIP);
        Combos.cast(fire, fd, Ability.FIRE_BALL);
        check(!Combos.discovered(fire).contains("scalding_steam"), "too far apart to meet");
        helper.succeed();
    }
}
