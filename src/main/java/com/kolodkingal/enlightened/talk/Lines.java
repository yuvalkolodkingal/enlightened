package com.kolodkingal.enlightened.talk;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Chakras;
import com.kolodkingal.enlightened.entity.CityGuardEntity;
import com.kolodkingal.enlightened.entity.GreatSpiritEntity;
import com.kolodkingal.enlightened.entity.IrohEntity;
import com.kolodkingal.enlightened.entity.KingBumiEntity;
import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import com.kolodkingal.enlightened.entity.SunWarriorEntity;
import com.kolodkingal.enlightened.item.EternalFlameItem;
import com.kolodkingal.enlightened.quest.AvatarQuest;
import com.kolodkingal.enlightened.talk.Dialogue.Option;
import com.kolodkingal.enlightened.world.LionTurtle;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.kolodkingal.enlightened.talk.Dialogue.ALWAYS;
import static com.kolodkingal.enlightened.talk.Dialogue.node;

/**
 * Every speaker's dialogue (1.0.23). The older lines (the guards' turns, the king's riddles, the guru's lessons, the
 * chief's offer, Raava's plea) are said as they were, by the same rules, but now with answers to choose: what used to
 * need a second right-click (taking up the Sun Warriors' test, letting Raava in, the lion turtle's gift) is an answer.
 */
final class Lines {
    static void registerAll() {
        pathik();
        bumi();
        for (Speaker guard : List.of(Speaker.GUARD_OMASHU, Speaker.GUARD_BA_SING_SE, Speaker.GUARD_ELLORA)) {
            guard(guard);
        }
        sunWarriors();
        lionTurtle();
        raava();
        iroh();
    }

    // ---- Guru Pathik ----

    private static void pathik() {
        Speaker g = Speaker.PATHIK;
        node(g, "no_bending").when(ctx -> !ctx.bender()).priority(30).line(ctx -> "guru.enlightened.no_bending")
                .option(Option.to("what_chakras", "chakras")).register();
        node(g, "welcome").when(ctx -> ctx.bender() && !ctx.data().guruTaught()).priority(20).line(ctx -> "guru.enlightened.welcome")
                .option(Option.to("teach_me", "teach"))
                .option(Option.to("what_chakras", "chakras"))
                .option(Option.close("not_now")).register();
        node(g, "teach").line(ctx -> "guru.enlightened.teach").onShow(ctx -> {
                    ctx.data().setGuruTaught(true);
                    Benders.syncIfDirty(ctx.player());
                })
                .option(Option.to("go_on", "teach_more")).register();
        node(g, "teach_more").line(ctx -> ctx.avatar() ? "guru.enlightened.teach_avatar" : "guru.enlightened.teach_bender")
                .option(Option.to("how_begin", "sit")).register();
        node(g, "sit").line(ctx -> "guru.enlightened.sit").option(Option.close("i_will_meditate")).register();
        node(g, "next").when(ctx -> ctx.bender() && ctx.data().guruTaught() && ctx.data().chakras() < Chakras.COUNT).priority(10)
                .line(ctx -> "guru.enlightened.next")
                .args(ctx -> {
                    int next = ctx.data().chakras() + 1;
                    return List.of("@chakra.enlightened." + next, "@guru.enlightened.block." + next);
                })
                .option(Option.to("what_chakras", "chakras"))
                .option(Option.close("i_will_meditate")).register();
        node(g, "done").when(ctx -> ctx.bender() && ctx.data().chakras() >= Chakras.COUNT).priority(10)
                .line(ctx -> ctx.avatar() ? "guru.enlightened.done_avatar" : "guru.enlightened.done")
                .option(Option.to("what_chakras", "chakras"))
                .option(Option.close("thank_you")).register();
        node(g, "chakras").option(Option.close("thank_you")).register();
        Dialogue.lines("guru.enlightened.no_bending", "guru.enlightened.welcome", "guru.enlightened.teach", "guru.enlightened.teach_avatar",
                "guru.enlightened.teach_bender", "guru.enlightened.sit", "guru.enlightened.next", "guru.enlightened.done", "guru.enlightened.done_avatar");
    }

    // ---- King Bumi ----

    private static void bumi() {
        Speaker b = Speaker.BUMI;
        node(b, "greet").when(ALWAYS).line(ctx -> {
                    KingBumiEntity king = ctx.speakerAs(KingBumiEntity.class);
                    return king != null ? king.line(ctx.player()) : "bumi.enlightened.line.0";
                })
                .option(Option.to("riddle", "riddle"))
                .option(Option.to("about_omashu", "omashu"))
                .option(Option.to("about_mail", "mail"))
                .option(Option.close("farewell_king")).register();
        node(b, "riddle").variants(3).option(Option.to("another", "riddle")).option(Option.to("about_omashu", "omashu")).register();
        node(b, "omashu").option(Option.to("about_mail", "mail")).option(Option.to("riddle", "riddle")).register();
        node(b, "mail").option(Option.to("riddle", "riddle")).option(Option.close("farewell_king")).register();
    }

    // ---- the city guards ----

    private static void guard(Speaker g) {
        node(g, "busy").when(ctx -> {
            CityGuardEntity guard = ctx.speakerAs(CityGuardEntity.class);
            LivingEntity target = guard == null ? null : guard.getTarget();
            return target != null && target.isAlive();
        }).priority(50).line(ctx -> "guard.enlightened.busy").register();
        node(g, "greet").when(ALWAYS).line(Lines::guardLine)
                .option(Option.to("anything_else", "more"))
                .option(Option.to("seen_avatar", "avatar"))
                .option(Option.to("where_tea", ctx -> ctx.who() == Speaker.GUARD_BA_SING_SE, "tea"))
                .option(Option.close("carry_on")).register();
        node(g, "more").line(Lines::guardLine)
                .option(Option.to("anything_else", "more"))
                .option(Option.to("where_tea", ctx -> ctx.who() == Speaker.GUARD_BA_SING_SE, "tea"))
                .option(Option.close("carry_on")).register();
        node(g, "avatar").line(ctx -> ctx.avatar() ? "npc.enlightened.guard.you_are_avatar" : "npc.enlightened.guard.avatar_rumor")
                .option(Option.to("anything_else", "more")).option(Option.close("carry_on")).register();
        node(g, "tea").option(Option.close("thanks")).register();
        Dialogue.lines("guard.enlightened.busy", "npc.enlightened.guard.you_are_avatar", "npc.enlightened.guard.avatar_rumor");
    }

    private static String guardLine(Dialogue.Ctx ctx) {
        CityGuardEntity guard = ctx.speakerAs(CityGuardEntity.class);
        return guard != null ? guard.line(ctx.player()) : "guard.enlightened.busy";
    }

    // ---- the Sun Warriors ----

    private static void sunWarriors() {
        Speaker chief = Speaker.SUN_CHIEF;
        node(chief, "blessed").when(ctx -> ctx.data().dragonBlessed()).priority(30).line(ctx -> "sun.enlightened.chief.blessed")
                .option(Option.to("about_dragons", "dragons")).option(Option.close("thank_you")).register();
        node(chief, "hurry").when(ctx -> EternalFlameItem.carries(ctx.player())).priority(20).line(ctx -> "sun.enlightened.chief.hurry")
                .option(Option.to("where_masters", "masters")).option(Option.close("on_my_way")).register();
        node(chief, "offer").when(ALWAYS).line(ctx -> "sun.enlightened.chief.offer")
                .option("take_test", ALWAYS, ctx -> {
                    SunWarriorEntity w = ctx.speakerAs(SunWarriorEntity.class);
                    if (w != null) {
                        w.giveFlame(ctx.player());
                    }
                    Talk.show(ctx, "given");
                })
                .option(Option.to("about_dragons", "dragons"))
                .option(Option.close("not_yet")).register();
        node(chief, "given").line(ctx -> "sun.enlightened.chief.given")
                .option(Option.to("where_masters", "masters")).option(Option.close("on_my_way")).register();
        node(chief, "dragons").option(Option.to("take_test", ctx -> !ctx.data().dragonBlessed() && !EternalFlameItem.carries(ctx.player()), "offer"))
                .option(Option.close("thank_you")).register();
        node(chief, "masters").option(Option.close("on_my_way")).register();
        sunBusy(chief);
        Speaker dancer = Speaker.SUN_DANCER;
        sunBusy(dancer);
        node(dancer, "dance").when(ALWAYS).line(Lines::sunLine)
                .option(Option.to("join_dance", "dance_on")).option(Option.close("farewell")).register();
        node(dancer, "dance_on").line(Lines::sunLine).option(Option.to("join_dance", "dance_on")).option(Option.close("farewell")).register();
        Speaker warrior = Speaker.SUN_WARRIOR;
        sunBusy(warrior);
        node(warrior, "greet").when(ALWAYS).line(Lines::sunLine)
                .option(Option.to("anything_else", "more")).option(Option.to("about_flame", "flame")).option(Option.close("farewell")).register();
        node(warrior, "more").line(Lines::sunLine).option(Option.to("anything_else", "more")).option(Option.close("farewell")).register();
        node(warrior, "flame").option(Option.close("thank_you")).register();
        Dialogue.lines("sun.enlightened.busy", "sun.enlightened.chief.blessed", "sun.enlightened.chief.hurry", "sun.enlightened.chief.offer",
                "sun.enlightened.chief.given");
    }

    private static void sunBusy(Speaker who) {
        node(who, "busy").when(ctx -> {
            SunWarriorEntity w = ctx.speakerAs(SunWarriorEntity.class);
            LivingEntity target = w == null ? null : w.getTarget();
            return target != null && target.isAlive();
        }).priority(50).line(ctx -> "sun.enlightened.busy").register();
    }

    private static String sunLine(Dialogue.Ctx ctx) {
        SunWarriorEntity w = ctx.speakerAs(SunWarriorEntity.class);
        return w != null ? w.line(ctx.player()) : "sun.enlightened.busy";
    }

    // ---- the lion turtle ----

    private static void lionTurtle() {
        Speaker t = Speaker.LION_TURTLE;
        node(t, "silent").when(ctx -> !LionTurtle.hero(ctx.player())).priority(30).line(ctx -> "lion_turtle.enlightened.silent")
                .option(Option.close("leave_quietly")).register();
        node(t, "hero").when(ctx -> LionTurtle.hero(ctx.player()) && !LionTurtle.gifted(ctx.player())).priority(20)
                .line(ctx -> "lion_turtle.enlightened.hero")
                .option("ready_gift", ALWAYS, ctx -> {
                    LionTurtleEntity turtle = ctx.speakerAs(LionTurtleEntity.class);
                    if (turtle != null) {
                        LionTurtle.gift(ctx.player(), turtle);
                    }
                    Talk.show(ctx, "gift");
                })
                .option(Option.to("who_are_you", "lore"))
                .option(Option.close("not_ready")).register();
        node(t, "gift").line(ctx -> "lion_turtle.enlightened.gift").option(Option.to("go_on", "home")).register();
        node(t, "again").when(ctx -> LionTurtle.hero(ctx.player()) && LionTurtle.gifted(ctx.player())).priority(20)
                .line(ctx -> "lion_turtle.enlightened.again")
                .option(Option.to("what_energy", "energy")).option(Option.to("who_are_you", "lore")).option(Option.to("go_on", "home")).register();
        node(t, "home").line(ctx -> "lion_turtle.enlightened.home").option(Option.close("thank_you")).register();
        node(t, "lore").option(Option.to("what_energy", "energy")).option(Option.close("thank_you")).register();
        node(t, "energy").option(Option.close("thank_you")).register();
        Dialogue.lines("lion_turtle.enlightened.silent", "lion_turtle.enlightened.hero", "lion_turtle.enlightened.gift",
                "lion_turtle.enlightened.again", "lion_turtle.enlightened.home");
    }

    // ---- Raava ----

    private static void raava() {
        Speaker r = Speaker.RAAVA;
        node(r, "taken").when(ctx -> !AvatarQuest.raavaFree(ctx.player().getServer())).priority(30).line(ctx -> "raava.enlightened.taken")
                .option(Option.to("about_vaatu", "vaatu")).register();
        node(r, "already").when(ctx -> ctx.avatar()).priority(20).line(ctx -> "raava.enlightened.already")
                .option(Option.to("about_vaatu", "vaatu")).option(Option.close("thank_you")).register();
        node(r, "ask").when(ALWAYS).line(ctx -> "raava.enlightened.ask")
                .option("help_raava", ALWAYS, ctx -> {
                    GreatSpiritEntity raava = ctx.speakerAs(GreatSpiritEntity.class);
                    boolean merged = raava != null && AvatarQuest.merge(ctx.player(), raava);
                    Talk.show(ctx, merged ? "merge" : "taken");
                })
                .option(Option.to("who_are_you", "lore"))
                .option(Option.to("about_vaatu", "vaatu"))
                .option(Option.close("not_ready")).register();
        node(r, "merge").line(ctx -> "raava.enlightened.merge").register();
        node(r, "lore").option(Option.to("help_raava_ask", "ask")).option(Option.close("not_ready")).register();
        node(r, "vaatu").option(Option.close("thank_you")).register();
        Dialogue.lines("raava.enlightened.taken", "raava.enlightened.already", "raava.enlightened.ask", "raava.enlightened.merge");
    }

    // ---- Iroh ----

    private static void iroh() {
        Speaker i = Speaker.IROH;
        List<Option> menu = List.of(
                new Option("buy_tea", ALWAYS, ctx -> {
                    IrohEntity iroh = ctx.speakerAs(IrohEntity.class);
                    if (iroh != null) {
                        iroh.openTrades(ctx.player());
                    }
                }),
                Option.to("ask_dragons", "dragons"),
                Option.to("ask_home", ctx -> !atHome(ctx), "home_offer"),
                Option.to("ask_advice", "advice"));
        node(i, "greet").when(ctx -> !atHome(ctx)).variants(3).options(menu).register();
        node(i, "home_greet").when(ctx -> atHome(ctx)).priority(10).variants(3).options(menu).register();
        node(i, "dragons").option(Option.to("really", "dragons_really")).option(Option.to("buy_tea_after", "menu")).register();
        node(i, "dragons_really").option(Option.to("buy_tea_after", "menu")).option(Option.close("farewell")).register();
        node(i, "home_offer").args(ctx -> List.of(String.valueOf(IrohEntity.HORN_PRICE)))
                .option("pay_gold", ctx -> IrohEntity.gold(ctx.player()) >= IrohEntity.HORN_PRICE || ctx.player().getAbilities().instabuild, ctx -> {
                    IrohEntity iroh = ctx.speakerAs(IrohEntity.class);
                    Talk.show(ctx, iroh != null && iroh.sellHorn(ctx.player()) ? "home_deal" : "come_back");
                })
                .option(Option.to("no_gold", ctx -> IrohEntity.gold(ctx.player()) < IrohEntity.HORN_PRICE && !ctx.player().getAbilities().instabuild, "come_back"))
                .option(Option.to("buy_tea_after", "menu")).register();
        node(i, "home_deal").option(Option.close("thank_you")).register();
        node(i, "come_back").args(ctx -> List.of(String.valueOf(IrohEntity.HORN_PRICE))).option(Option.to("buy_tea_after", "menu")).register();
        node(i, "advice").variants(6).option(Option.to("more_advice", "advice")).option(Option.to("buy_tea_after", "menu")).register();
        node(i, "menu").options(menu).register();
    }

    private static boolean atHome(Dialogue.Ctx ctx) {
        IrohEntity iroh = ctx.speakerAs(IrohEntity.class);
        return iroh != null && iroh.atHome();
    }

    private Lines() {
    }
}
