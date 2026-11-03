package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AvatarCycle;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.entity.DarkSpiritEntity;
import com.kolodkingal.enlightened.entity.GreatSpiritEntity;
import com.kolodkingal.enlightened.quest.AvatarQuest;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.world.RaavaGladePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** The GameTests of Raava and Vaatu: the glade, the merging, the dark spirits, Vaatu's offer, the Dark Avatar. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public final class QuestTests {

    /** Room in the world for Avatars during a test (other tests make Avatars of their own). */
    private static int roomy(GameTestHelper helper) {
        AvatarCycle cycle = AvatarCycle.get(helper.getLevel().getServer());
        int max = cycle.max();
        cycle.setMax(1000);
        return max;
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void raavasGladeHasAPoolAndPillars(GameTestHelper helper) throws ReflectiveOperationException {
        RaavaGladePiece glade = new RaavaGladePiece(BlockPos.ZERO);
        Method m = RaavaGladePiece.class.getDeclaredMethod("design", int.class, int.class, int.class);
        m.setAccessible(true);
        check(((BlockState) m.invoke(glade, 0, 0, 0)).is(Blocks.WATER), "a pool in the middle");
        check(((BlockState) m.invoke(glade, 7, 2, 0)).is(Blocks.QUARTZ_PILLAR) && ((BlockState) m.invoke(glade, 7, 4, 0)).is(Blocks.SOUL_LANTERN),
                "white pillars crowned with lanterns round it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void raavaMakesHerHelperTheAvatar(GameTestHelper helper) {
        int max = roomy(helper);
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, TestKit.FLOOR, 5.5);
        f.bender(p, Element.WATER);
        GreatSpiritEntity raava = GreatSpiritEntity.raavaAt(helper.getLevel(), helper.absoluteVec(new Vec3(8, 4, 8)));
        helper.getLevel().addFreshEntity(raava);
        check(raava.talk(p).equals("raava.enlightened.ask"), "she asks for help");
        check(raava.talk(p).equals("raava.enlightened.merge"), "asked again, she enters him");
        check(Benders.get(p).element() == Element.AVATAR && !Benders.get(p).darkAvatar(), "he is the Avatar");
        check(AvatarQuest.stage(p) == AvatarQuest.Stage.SPIRITS && !raava.isAlive(), "and the dark spirits will come");
        AvatarCycle.get(helper.getLevel().getServer()).setMax(0);
        GreatSpiritEntity other = GreatSpiritEntity.raavaAt(helper.getLevel(), helper.absoluteVec(new Vec3(12, 4, 12)));
        helper.getLevel().addFreshEntity(other);
        ServerPlayer q = f.player(12.5, TestKit.FLOOR, 10.5);
        f.bender(q, Element.FIRE);
        check(other.talk(q).equals("raava.enlightened.taken"), "where the world has its Avatar, she chooses nobody");
        AvatarCycle.get(helper.getLevel().getServer()).setMax(max);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void beatingTheDarkSpiritsSendsHimToVaatu(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, TestKit.FLOOR, 5.5);
        f.bender(p, Element.AVATAR);
        AvatarQuest.setStage(p, AvatarQuest.Stage.SPIRITS);
        DarkSpiritEntity s = ModEntities.DARK_SPIRIT.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(8, 2, 8));
        s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        s.hunt(p);
        helper.getLevel().addFreshEntity(s);
        s.hurt(helper.getLevel().damageSources().playerAttack(p), 1000.0F);
        check(AvatarQuest.kills(p) == 1, "a dark spirit that hunted him is counted when beaten");
        for (int i = 1; i < AvatarQuest.SPIRITS; i++) {
            AvatarQuest.spiritBeaten(p);
        }
        check(AvatarQuest.stage(p) == AvatarQuest.Stage.SEEK_VAATU, "ten beaten, Vaatu waits in the Spirit World");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void joiningVaatuMakesTheDarkAvatar(GameTestHelper helper) {
        int max = roomy(helper);
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, TestKit.FLOOR, 5.5);
        BendingData data = f.bender(p, Element.AVATAR);
        check(AvatarQuest.answer(p, true).equals("message.enlightened.quest.not_asked"), "no answer before he is asked");
        AvatarQuest.setStage(p, AvatarQuest.Stage.OFFERED);
        AvatarQuest.answer(p, true);
        check(data.darkAvatar() && data.tetherless(), "he is the Dark Avatar, free of every tether (he may fly)");
        check(AvatarQuest.stage(p) == AvatarQuest.Stage.FIGHT_RAAVA, "and must fight Raava");
        var raava = helper.getLevel().getEntitiesOfClass(GreatSpiritEntity.class, new AABB(p.blockPosition()).inflate(32),
                g -> g.kind() == GreatSpiritEntity.Kind.RAAVA && g.hostile());
        check(raava.size() == 1, "Raava comes against him");
        check(Mastery.gateOpen(data, Ability.BLOODBENDING), "bloodbending is open to him at once");
        ServerPlayer light = f.player(9.5, TestKit.FLOOR, 9.5);
        BendingData his = f.bender(light, Element.AVATAR);
        check(Mastery.energyStrength(p) < Mastery.energyStrength(light), "energy is weaker in the Dark Avatar");
        check(!raava.get(0).hurt(helper.getLevel().damageSources().playerAttack(light), 50.0F), "only the one she was set against may harm her");
        raava.get(0).hurt(helper.getLevel().damageSources().playerAttack(p), 1000.0F);
        check(AvatarQuest.stage(p) == AvatarQuest.Stage.DONE_DARK, "beating her, the darkness is his");
        AvatarQuest.setStage(light, AvatarQuest.Stage.OFFERED);
        AvatarQuest.answer(light, false);
        check(AvatarQuest.stage(light) == AvatarQuest.Stage.FIGHT_VAATU && !his.darkAvatar(), "refusing, he must fight Vaatu, and stays in the light");
        AvatarCycle.get(helper.getLevel().getServer()).setMax(max);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void anOperatorMakesTheDarkAvatar(GameTestHelper helper) throws Exception {
        int max = roomy(helper);
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, TestKit.FLOOR, 5.5);
        f.bender(p, Element.FIRE);
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        server.getCommands().getDispatcher().execute("enlightened avatar " + p.getGameProfile().getName() + " dark", source);
        BendingData data = Benders.get(p);
        check(data.element() == Element.AVATAR && data.darkAvatar() && data.tetherless(), "the command makes him the Dark Avatar");
        server.getCommands().getDispatcher().execute("enlightened avatar " + p.getGameProfile().getName() + " light", source);
        check(data.element() == Element.AVATAR && !data.darkAvatar(), "and brings him back to the light");
        AvatarCycle.get(server).setMax(max);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void presetsKeepTheKeys(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, TestKit.FLOOR, 5.5);
        BendingData data = f.bender(p, Element.EARTH, Ability.ROCK_THROW, 1, Ability.EARTH_WALL, 1, Ability.EARTH_PILLAR, 1);
        data.setSlot(0, Ability.EARTH_WALL);
        data.setSlot(1, Ability.ROCK_THROW);
        check(com.kolodkingal.enlightened.command.PresetCommand.save(p, "walls"), "saved");
        data.setSlot(0, Ability.EARTH_PILLAR);
        data.setSlot(1, null);
        check(com.kolodkingal.enlightened.command.PresetCommand.load(p, "walls") >= 2, "loaded");
        check(data.slot(0) == Ability.EARTH_WALL && data.slot(1) == Ability.ROCK_THROW, "the keys are as they were saved");
        check(com.kolodkingal.enlightened.command.PresetCommand.load(p, "nothing") < 0, "no such preset");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aNewbornBenderGetsAStarterKit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, TestKit.FLOOR, 5.5);
        BendingData data = f.bender(p, Element.FIRE);
        for (Element e : Element.FOUR) {
            for (var kit : com.kolodkingal.enlightened.bending.StarterKits.kits(e)) {
                for (Ability a : kit) {
                    check(a.element == e && a.gate == com.kolodkingal.enlightened.bending.Gate.NONE, a + " is open to every " + e + " bender");
                }
            }
        }
        com.kolodkingal.enlightened.bending.StarterKits.give(p);
        long learned = Ability.of(Element.FIRE).stream().filter(a -> data.level(a) > 0).count();
        check(learned >= 3, "he begins with a set of fire arts (" + learned + ")");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void aPresetLearnsItsAbilities(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(5.5, TestKit.FLOOR, 5.5);
        BendingData data = f.bender(p, Element.EARTH, Ability.ROCK_THROW, 1, Ability.EARTH_WALL, 2, Ability.SHOCKWAVE, 1);
        data.setSlot(0, Ability.EARTH_WALL);
        data.setSlot(1, Ability.SHOCKWAVE);
        com.kolodkingal.enlightened.command.PresetCommand.save(p, "wall");
        com.kolodkingal.enlightened.bending.Benders.setElement(p, Element.NONE);
        f.bender(p, Element.EARTH, Ability.ROCK_THROW, 1);
        BendingData fresh = Benders.get(p);
        fresh.setPoints(10);
        com.kolodkingal.enlightened.command.PresetCommand.load(p, "wall");
        check(fresh.level(Ability.EARTH_WALL) == 2 && fresh.level(Ability.SHOCKWAVE) == 1, "it learns them again, to their levels");
        check(fresh.points() == 10 - 1 - 2 - 1, "with his points (" + fresh.points() + ")");
        check(fresh.slot(0) == Ability.EARTH_WALL && fresh.slot(1) == Ability.SHOCKWAVE, "and puts them on his keys");
        helper.succeed();
    }
}
