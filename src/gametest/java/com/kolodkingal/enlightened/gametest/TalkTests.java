package com.kolodkingal.enlightened.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.command.PresetCommand;
import com.kolodkingal.enlightened.entity.IrohEntity;
import com.kolodkingal.enlightened.entity.SeatEntity;
import com.kolodkingal.enlightened.entity.SunWarriorEntity;
import com.kolodkingal.enlightened.item.EternalFlameItem;
import com.kolodkingal.enlightened.registry.ModBlocks;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.registry.ModItems;
import com.kolodkingal.enlightened.talk.Dialogue;
import com.kolodkingal.enlightened.talk.Speaker;
import com.kolodkingal.enlightened.talk.Talk;
import com.kolodkingal.enlightened.world.BaSingSePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enlightened.gametest.TestKit.ARENA;
import static com.kolodkingal.enlightened.gametest.TestKit.check;

/** 1.0.23: the talk screen's dialogues, Iroh and his tea house, the tsungi horn, the stools, the scroll's presets. */
@GameTestHolder(Enlightened.MODID)
@PrefixGameTestTemplate(false)
public class TalkTests {
    private static JsonObject lang(String name) {
        try (var in = TalkTests.class.getResourceAsStream("/assets/enlightened/lang/" + name + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void everyLineOfEveryTalkIsInBothLanguages(GameTestHelper helper) {
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> missing = new ArrayList<>();
        for (String key : Dialogue.langKeys()) {
            if (!en.has(key) || !he.has(key)) {
                missing.add(key);
            }
        }
        check(missing.isEmpty(), "missing lines: " + missing);
        for (Speaker s : Speaker.values()) {
            check(!Dialogue.nodes(s).isEmpty(), s + " has something to say");
        }
        helper.succeed();
    }

    private static IrohEntity iroh(GameTestHelper helper, int x, int z) {
        IrohEntity iroh = ModEntities.IROH.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        iroh.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(iroh);
        return iroh;
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void irohTalksOfDragonsAndSellsHisHorn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        IrohEntity iroh = iroh(helper, 3, 3);
        check(Talk.begin(p, iroh, Speaker.IROH), "he talks");
        check("greet".equals(Talk.currentNode(p)), "he greets: " + Talk.currentNode(p));
        check(Talk.offered(p).containsAll(List.of("buy_tea", "ask_dragons", "ask_home", "ask_advice")), "tea, dragons, home, advice: " + Talk.offered(p));
        check(Talk.tryChoose(p, iroh.getId(), "ask_dragons") && "dragons".equals(Talk.currentNode(p)), "he killed the last dragon");
        check(Talk.tryChoose(p, iroh.getId(), "buy_tea_after") && "menu".equals(Talk.currentNode(p)), "back to tea");
        check(Talk.tryChoose(p, iroh.getId(), "ask_home") && Talk.offered(p).contains("no_gold") && !Talk.offered(p).contains("pay_gold"), "no gold, no horn");
        check(!Talk.tryChoose(p, iroh.getId(), "pay_gold"), "an answer not offered is refused");
        p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 32));
        p.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 2));
        Talk.end(p);
        Talk.begin(p, iroh, Speaker.IROH);
        Talk.tryChoose(p, iroh.getId(), "ask_home");
        check(Talk.offered(p).contains("pay_gold"), "with 50 gold he offers the horn");
        check(Talk.tryChoose(p, iroh.getId(), "pay_gold") && "home_deal".equals(Talk.currentNode(p)), "the deal is made");
        check(count(p, ModItems.TSUNGI_HORN.get()) == 1, "the tsungi horn is his");
        check(IrohEntity.gold(p) == 32 + 18 - IrohEntity.HORN_PRICE, "fifty gold paid, change given (" + IrohEntity.gold(p) + " left)");
        check(Talk.tryChoose(p, iroh.getId(), "thank_you") && Talk.currentNode(p) == null, "the talk ends");
        check(!iroh.getOffers().isEmpty() && iroh.getOffers().get(0).getResult().is(ModItems.JASMINE_TEA.get()), "he sells jasmine tea");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void theHornCallsIrohHomeOnJetsOfFire(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(2.5, TestKit.FLOOR, 2.5);
        IrohEntity first = IrohEntity.arrive(p);
        check(first.ownedBy(p) && first.atHome() && first.arriving() > 0, "he comes, flying");
        check(first.getY() > p.getY() + 10, "from high above");
        IrohEntity second = IrohEntity.arrive(p);
        check(first.isRemoved() && second.isAlive(), "one Iroh per player: the newest");
        helper.succeedWhen(() -> {
            check(second.arriving() == 0, "he lands");
            check(Math.abs(second.getY() - p.getY()) < 1.5, "beside his host");
            Talk.begin(p, second, Speaker.IROH);
            check("home_greet".equals(Talk.currentNode(p)), "at home, he greets him as a friend");
            check(!Talk.offered(p).contains("ask_home"), "no horn to sell at home");
            Talk.end(p);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void theChiefsTestIsAnAnswerNow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        f.bender(p, Element.FIRE);
        SunWarriorEntity chief = ModEntities.SUN_WARRIOR.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
        chief.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        chief.station(SunWarriorEntity.Role.CHIEF, 0, at);
        helper.getLevel().addFreshEntity(chief);
        check(Talk.begin(p, chief, chief.speaker()) && "offer".equals(Talk.currentNode(p)), "he offers the test");
        check(Talk.tryChoose(p, chief.getId(), "take_test") && EternalFlameItem.carries(p), "accepted, the flame is his");
        check("given".equals(Talk.currentNode(p)), "and he says so");
        Talk.end(p);
        check(Talk.begin(p, chief, chief.speaker()) && "hurry".equals(Talk.currentNode(p)), "with the flame: hurry");
        helper.succeed();
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
    public static void baSingSeHasTheJasmineDragon(GameTestHelper helper) {
        BaSingSePiece city = new BaSingSePiece(BlockPos.ZERO);
        int stools = 0;
        for (int[] s : BaSingSePiece.STOOLS) {
            if (at(city, BaSingSePiece.TEA_X0 + s[0], 1, BaSingSePiece.TEA_Z0 + s[1]).is(ModBlocks.TEA_STOOL.get())) {
                stools++;
            }
        }
        check(stools == BaSingSePiece.STOOLS.length, "stools to sit at (" + stools + ")");
        check(at(city, BaSingSePiece.TEA_X0 + 5, 1, BaSingSePiece.TEA_Z0).isAir(), "an open door to the street");
        check(at(city, BaSingSePiece.TEA_X0 + 5, 8, BaSingSePiece.TEA_Z0 + 4).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK), "a golden ridge");
        BlockPos iroh = BaSingSePiece.irohSpot();
        check(at(city, iroh.getX(), 1, iroh.getZ()).isAir() && at(city, iroh.getX(), 2, iroh.getZ()).isAir(), "room for Iroh behind the counter");
        check(at(city, iroh.getX(), 1, iroh.getZ() - 1).is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS), "the counter in front of him");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void aStoolIsSatOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        BlockPos stool = helper.absolutePos(new BlockPos(2, 2, 1));
        helper.getLevel().setBlockAndUpdate(stool, ModBlocks.TEA_STOOL.get().defaultBlockState());
        helper.getLevel().getBlockState(stool).useWithoutItem(helper.getLevel(), p,
                new BlockHitResult(Vec3.atCenterOf(stool), net.minecraft.core.Direction.UP, stool, false));
        check(p.getVehicle() instanceof SeatEntity, "he sits");
        helper.getLevel().removeBlock(stool, false);
        helper.succeedWhen(() -> check(!p.isPassenger(), "the stool gone, he stands"));
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void presetsAreListedForTheScroll(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer p = f.player(1.5, TestKit.FLOOR, 1.5);
        f.bender(p, Element.WATER);
        check(PresetCommand.save(p, "fight"), "saved");
        var listing = PresetCommand.listing(p);
        check(listing.names().equals(List.of("fight")) && listing.slots().get(0).split(",", -1).length == 10, "one preset of ten keys: " + listing);
        check(PresetCommand.delete(p, "fight") && PresetCommand.listing(p).names().isEmpty(), "deleted");
        check(com.kolodkingal.enlightened.network.PresetPayload.validName("שלום_1") && !com.kolodkingal.enlightened.network.PresetPayload.validName("a b"),
                "names of letters (any alphabet), digits, - and _");
        helper.succeed();
    }
}
