package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.entity.IrohEntity;
import com.kolodkingal.enlightened.entity.NationNpcEntity;
import com.kolodkingal.enlightened.entity.OzaiEntity;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.talk.Speaker;
import com.kolodkingal.enlightened.talk.Talk;
import com.kolodkingal.enlightened.world.Comet;
import com.kolodkingal.enlightened.world.WulongForestPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** 1.0.27: Fire Lord Ozai, Sozin's Comet, Wulong Forest. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public class OzaiTests {
    private static OzaiEntity ozai(GameTestHelper helper, int x, int z) {
        OzaiEntity o = ModEntities.OZAI.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        // He waits where he stands and fights there; his cell is in the corner.
        o.place(at, at.below(), helper.absolutePos(new BlockPos(0, 2, 0)));
        o.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(o);
        return o;
    }

    private static DamageSource source(GameTestHelper helper, ResourceKey<DamageType> type, Entity by) {
        return new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), by);
    }

    @GameTest(template = ARENA, timeoutTicks = 60, batch = "ozai")
    public static void waterHurtsOzaiFireHardly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        f.bender(p, Element.FIRE, Ability.FIRE_BALL, 1);
        OzaiEntity o = ozai(helper, 4, 4);
        check(!o.hurt(source(helper, ModDamageTypes.WATER, p), 10), "waiting on his ship, nothing harms him");
        check(o.challenge(p), "he answers the challenge");
        check(Comet.active(helper.getLevel()), "Sozin's Comet comes");
        helper.succeedWhen(() -> {
            check(o.state() == OzaiEntity.State.FIGHTING, "he lands and fights");
            check(o.share(source(helper, ModDamageTypes.WATER, p)) == 1.0F, "water strikes him fully");
            check(o.share(source(helper, ModDamageTypes.FIRE, p)) < 0.3F, "an unmastered firebender's fire hardly touches him");
            check(o.share(source(helper, ModDamageTypes.EARTH, p)) < 0.5F, "other blows little");
            for (Ability a : Ability.values()) {
                if (a.element == Element.FIRE) {
                    com.kolodkingal.enlightened.bending.Benders.get(p).setLevel(a, Ability.MAX_LEVEL);
                }
            }
            check(OzaiEntity.masteredFire(p), "every art of fire mastered");
            check(o.share(source(helper, ModDamageTypes.FIRE, p)) == 1.0F, "a master of fire under the comet strikes him fully");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60, batch = "ozai")
    public static void energybendingTakesHisBendingAndHeGoesToPrison(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        OzaiEntity o = ozai(helper, 4, 4);
        o.challenge(p);
        helper.runAfterDelay(10, () -> {
            check(o.state() == OzaiEntity.State.FIGHTING, "fighting");
            check(o.takeBending(p), "his firebending is taken");
            check(o.prisoner(), "a prisoner");
            check(o.blockPosition().equals(o.cell()), "in his cell (" + o.blockPosition() + " / " + o.cell() + ")");
            check(!o.hurt(source(helper, ModDamageTypes.WATER, p), 50), "and nobody harms him there");
            check(Talk.begin(p, o, Speaker.OZAI) && "chained".equals(Talk.currentNode(p)), "he still talks, in chains");
            Talk.end(p);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60, batch = "ozai")
    public static void beatenByWaterHeIsTakenNotKilled(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        OzaiEntity o = ozai(helper, 4, 4);
        o.challenge(p);
        helper.runAfterDelay(10, () -> {
            o.setHealth(5.0F);
            o.invulnerableTime = 0;
            o.hurt(source(helper, ModDamageTypes.WATER, p), 20);
            check(o.isAlive() && o.prisoner(), "beaten, he lives on, a prisoner");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "comet")
    public static void theCometDoublesFirebending(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        f.bender(p, Element.FIRE, Ability.FIRE_BALL, 1);
        Husk a = helper.spawn(EntityType.HUSK, new BlockPos(3, 2, 3));
        Husk b = helper.spawn(EntityType.HUSK, new BlockPos(5, 2, 5));
        a.setNoAi(true);
        b.setNoAi(true);
        // (Wait for any comet of another test to pass.)
        helper.succeedWhen(() -> {
            check(!Comet.active(helper.getLevel()), "a clear sky first");
            Shots.hurt(p, a, 4.0F, ModDamageTypes.FIRE);
            float plain = a.getMaxHealth() - a.getHealth();
            Comet.keep(helper.getLevel());
            Shots.hurt(p, b, 4.0F, ModDamageTypes.FIRE);
            float comet = b.getMaxHealth() - b.getHealth();
            check(plain > 0 && Math.abs(comet - plain * 2) < 0.6F, "under the comet fire strikes twice as hard (" + plain + " / " + comet + ")");
        });
    }

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
    public static void wulongForestHasItsSpiresShipsPalaceAndPrison(GameTestHelper helper) {
        WulongForestPiece w = new WulongForestPiece(BlockPos.ZERO);
        check(at(w, 0, 3, 0).isAir() && at(w, 10, 3, 10).isAir(), "an open field");
        int rock = 0;
        for (int x = -70; x <= 70; x += 2) {
            for (int z = -40; z <= 70; z += 2) {
                var s = at(w, x, 6, z);
                if (s != null && (s.is(Blocks.STONE) || s.is(Blocks.ANDESITE) || s.is(Blocks.TUFF) || s.is(Blocks.COBBLESTONE))) {
                    rock++;
                }
            }
        }
        check(rock > 40, "a forest of rock spires (" + rock + ")");
        BlockPos deck = WulongForestPiece.ozaiDeck();
        check(at(w, deck.getX(), deck.getY() - 1, deck.getZ()).is(Blocks.DARK_OAK_PLANKS), "the flagship's deck under Ozai");
        check(at(w, deck.getX(), WulongForestPiece.SHIP_Y + WulongForestPiece.SHIP_R, deck.getZ()) != null, "its hull above");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theFirePalaceHasItsHallThroneAndPrison(GameTestHelper helper) {
        com.kolodkingal.enlightened.world.FirePalacePiece p = new com.kolodkingal.enlightened.world.FirePalacePiece(BlockPos.ZERO);
        BlockPos cell = com.kolodkingal.enlightened.world.FirePalacePiece.OZAI_CELL;
        check(at(p, cell.getX(), cell.getY(), cell.getZ()).isAir() && at(p, cell.getX(), cell.getY() + 1, cell.getZ()).isAir(), "room in Ozai's cell");
        check(at(p, cell.getX(), cell.getY(), -9).is(Blocks.IRON_BARS), "behind bars");
        BlockPos throne = com.kolodkingal.enlightened.world.FirePalacePiece.THRONE;
        check(at(p, throne.getX(), throne.getY(), throne.getZ()).is(Blocks.RED_NETHER_BRICK_STAIRS), "the throne");
        check(at(p, 3, throne.getY() - 1, com.kolodkingal.enlightened.world.FirePalacePiece.FIRE_Z).is(Blocks.SOUL_CAMPFIRE), "blue fire behind it");
        check(at(p, 8, 9, com.kolodkingal.enlightened.world.FirePalacePiece.HALL_Z0).is(Blocks.RED_TERRACOTTA)
                || at(p, 8, 9, com.kolodkingal.enlightened.world.FirePalacePiece.HALL_Z0).is(Blocks.NETHER_BRICKS), "red walls");
        check(at(p, 0, 3, 30).isAir(), "an open gate");
        check(at(p, 15, 0, -7).is(Blocks.LADDER), "a ladder down to the prison");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20, batch = "herald")
    public static void zukoTakesTheThroneOnce(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2 - com.kolodkingal.enlightened.world.FirePalacePiece.THRONE.getY(), 8));
        com.kolodkingal.enlightened.world.FirePalacePiece.crown(helper.getLevel(), origin);
        com.kolodkingal.enlightened.world.FirePalacePiece.crown(helper.getLevel(), origin);
        var all = helper.getLevel().getEntitiesOfClass(NationNpcEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40));
        long zukos = all.stream().filter(n -> n.kind() == NationNpcEntity.Kind.ZUKO).count();
        long guards = all.stream().filter(n -> n.kind() == NationNpcEntity.Kind.GUARD).count();
        check(zukos == 1 && guards == 4, "Zuko on the throne, four guards behind him (" + zukos + ", " + guards + ")");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20, batch = "herald")
    public static void zukoAndTheHeraldTalk(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        NationNpcEntity zuko = ModEntities.NATION_NPC.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
        zuko.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        zuko.kind(NationNpcEntity.Kind.ZUKO);
        helper.getLevel().addFreshEntity(zuko);
        check(Talk.begin(p, zuko, Speaker.ZUKO) && "throne".equals(Talk.currentNode(p)), "Fire Lord Zuko greets him");
        check(Talk.tryChoose(p, zuko.getId(), "what_now") && "peace".equals(Talk.currentNode(p)), "and speaks of peace");
        Talk.end(p);
        IrohEntity iroh = ModEntities.IROH.get().create(helper.getLevel());
        iroh.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 2.5, 0, 0);
        helper.getLevel().addFreshEntity(iroh);
        check(Talk.begin(p, iroh, Speaker.IROH) && Talk.offered(p).contains("ask_ozai"), "Iroh knows where his brother is");
        check(Talk.tryChoose(p, iroh.getId(), "ask_ozai") && Talk.tryChoose(p, iroh.getId(), "ask_map"), "and marks it");
        check("ozai_map".equals(Talk.currentNode(p)) || "ozai_no_map".equals(Talk.currentNode(p)), "a map, or none near: " + Talk.currentNode(p));
        Talk.end(p);
        NationNpcEntity herald = ModEntities.NATION_NPC.get().create(helper.getLevel());
        herald.moveTo(at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0, 0);
        herald.kind(NationNpcEntity.Kind.HERALD);
        helper.getLevel().addFreshEntity(herald);
        check(Talk.begin(p, herald, Speaker.FIRE_HERALD) && Talk.offered(p).contains("yes_fight"), "the herald asks");
        Talk.end(p);
        helper.succeed();
    }
}
