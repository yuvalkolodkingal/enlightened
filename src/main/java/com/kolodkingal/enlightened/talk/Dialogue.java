package com.kolodkingal.enlightened.talk;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * What the people of the world say (1.0.23, after the Asgardian talk of Enhanced): a table of dialogue nodes per
 * {@link Speaker}, in Java. Right-clicking a speaker picks the node whose {@code when} holds with the highest priority
 * ({@link #pick}); its options are the answers offered (at most {@link #MAX_OPTIONS}), and choosing one runs its action
 * ({@link Talk}): another node, a gift, a trade, a lesson...
 *
 * <p><b>Lang keys.</b> A node's line is {@code npc.enlightened.<speaker>.<node>}, unless the node names its own line
 * ({@link Builder#line}: an older key, or one worked out in the talk, as a guard's turn of lines). A line with variants
 * is said as {@code <line>.<n>}, n from 1, one at random. An option is {@code npc.enlightened.option.<key>}. Arguments
 * fill the line's {@code %s}; an argument that starts with {@code @} is a lang key the client translates.
 */
public final class Dialogue {
    /** The most answers a node offers (the talk screen's buttons). */
    public static final int MAX_OPTIONS = 5;
    public static final Predicate<Ctx> NEVER = ctx -> false;
    public static final Predicate<Ctx> ALWAYS = ctx -> true;

    /** Who talks to whom. */
    public record Ctx(ServerPlayer player, Entity speaker, Speaker who) {
        public BendingData data() {
            return Benders.get(player);
        }

        public boolean avatar() {
            return data().element() == Element.AVATAR;
        }

        public boolean bender() {
            return data().element().bends();
        }

        @Nullable
        public <T> T speakerAs(Class<T> type) {
            return type.isInstance(speaker) ? type.cast(speaker) : null;
        }
    }

    @FunctionalInterface
    public interface Action {
        void run(Ctx ctx);
    }

    /** An answer the player may choose: shown when {@code when} holds. */
    public record Option(String key, Predicate<Ctx> when, Action action) {
        public static Option to(String key, String node) {
            return new Option(key, ALWAYS, ctx -> Talk.show(ctx, node));
        }

        public static Option to(String key, Predicate<Ctx> when, String node) {
            return new Option(key, when, ctx -> Talk.show(ctx, node));
        }

        public static Option close(String key) {
            return new Option(key, ALWAYS, ctx -> {
            });
        }
    }

    /** A line as it is said: its lang key and its arguments. */
    public record Said(String line, List<String> args) {
    }

    public record Node(String key, Predicate<Ctx> when, int priority, List<Option> options) {
    }

    private static final Map<Speaker, List<Node>> NODES = new EnumMap<>(Speaker.class);
    private static final Map<String, Integer> VARIANTS = new HashMap<>();
    private static final Map<String, Function<Ctx, List<String>>> ARGS = new HashMap<>();
    private static final Map<String, Function<Ctx, String>> LINE = new HashMap<>();
    private static final Map<String, Action> ON_SHOW = new HashMap<>();
    /** Lang keys said that the table does not name by itself (worked-out lines): checked by the tests. */
    private static final Set<String> LINES = new LinkedHashSet<>();
    private static boolean loaded;

    /** Loads every speaker's lines (once). */
    public static synchronized void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            Lines.registerAll();
        }
    }

    public static synchronized void register(Speaker who, Node node) {
        NODES.computeIfAbsent(who, r -> new ArrayList<>()).add(node);
    }

    public static synchronized List<Node> nodes(Speaker who) {
        ensureLoaded();
        return Collections.unmodifiableList(new ArrayList<>(NODES.getOrDefault(who, List.of())));
    }

    @Nullable
    public static synchronized Node find(Speaker who, String key) {
        ensureLoaded();
        for (Node node : NODES.getOrDefault(who, List.of())) {
            if (node.key().equals(key)) {
                return node;
            }
        }
        return null;
    }

    public static synchronized void variants(String lineKey, int count) {
        VARIANTS.put(lineKey, count);
    }

    public static synchronized int variantCount(String lineKey) {
        return VARIANTS.getOrDefault(lineKey, 1);
    }

    public static synchronized void lines(String... keys) {
        Collections.addAll(LINES, keys);
    }

    /** Every lang key the table names: lines (with variants), options, the noted extra lines. */
    public static synchronized Set<String> langKeys() {
        ensureLoaded();
        Set<String> keys = new LinkedHashSet<>();
        for (Map.Entry<Speaker, List<Node>> entry : NODES.entrySet()) {
            keys.add(entry.getKey().nameKey);
            for (Node node : entry.getValue()) {
                if (!LINE.containsKey(entry.getKey().name() + "/" + node.key())) {
                    keys.addAll(lineKeys(lineKey(entry.getKey(), node.key())));
                }
                for (Option option : node.options()) {
                    keys.add(optionKey(option.key()));
                }
            }
        }
        for (String line : LINES) {
            keys.addAll(lineKeys(line));
        }
        keys.add("npc.enlightened.screen.farewell");
        return keys;
    }

    private static List<String> lineKeys(String base) {
        int n = variantCount(base);
        if (n <= 1) {
            return List.of(base);
        }
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            keys.add(base + "." + i);
        }
        return keys;
    }

    public static String lineKey(Speaker who, String node) {
        return "npc.enlightened." + who.id() + "." + node;
    }

    public static String optionKey(String option) {
        return "npc.enlightened.option." + option;
    }

    public static String variant(String base, RandomSource random) {
        int n = variantCount(base);
        return n <= 1 ? base : base + "." + (1 + random.nextInt(n));
    }

    /** The line a node says in this talk (its own key, a variant of it, or the one it works out), with its arguments. */
    public static synchronized Said said(Ctx ctx, Node node) {
        String id = ctx.who().name() + "/" + node.key();
        Function<Ctx, String> line = LINE.get(id);
        String key = line != null ? line.apply(ctx) : variant(lineKey(ctx.who(), node.key()), ctx.player().getRandom());
        Function<Ctx, List<String>> args = ARGS.get(id);
        return new Said(key, args == null ? List.of() : args.apply(ctx));
    }

    @Nullable
    public static synchronized Action onShowOf(Speaker who, String node) {
        return ON_SHOW.get(who.name() + "/" + node);
    }

    /** The node to say first: of those whose {@code when} holds, the one of the highest priority (the first). */
    @Nullable
    public static synchronized Node pick(Ctx ctx) {
        ensureLoaded();
        Node best = null;
        for (Node node : NODES.getOrDefault(ctx.who(), List.of())) {
            if ((best == null || node.priority() > best.priority()) && node.when().test(ctx)) {
                best = node;
            }
        }
        return best;
    }

    public static List<Option> offered(Ctx ctx, Node node) {
        List<Option> offered = new ArrayList<>();
        for (Option option : node.options()) {
            if (offered.size() < MAX_OPTIONS && option.when().test(ctx)) {
                offered.add(option);
            }
        }
        return offered;
    }

    public static Builder node(Speaker who, String key) {
        return new Builder(who, key);
    }

    public static final class Builder {
        private final Speaker who;
        private final String key;
        private Predicate<Ctx> when = NEVER;
        private int priority;
        private final List<Option> options = new ArrayList<>();

        private Builder(Speaker who, String key) {
            this.who = who;
            this.key = key;
        }

        /** When the node is picked by itself (without: only shown by an option). */
        public Builder when(Predicate<Ctx> when) {
            this.when = when;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder variants(int count) {
            Dialogue.variants(lineKey(who, key), count);
            return this;
        }

        /** The line the node says, worked out in the talk (a full lang key). */
        public Builder line(Function<Ctx, String> line) {
            synchronized (Dialogue.class) {
                LINE.put(who.name() + "/" + key, line);
            }
            return this;
        }

        public Builder args(Function<Ctx, List<String>> args) {
            synchronized (Dialogue.class) {
                ARGS.put(who.name() + "/" + key, args);
            }
            return this;
        }

        /** What happens each time the node is shown (before its line is worked out). */
        public Builder onShow(Action action) {
            synchronized (Dialogue.class) {
                ON_SHOW.put(who.name() + "/" + key, action);
            }
            return this;
        }

        public Builder option(Option option) {
            options.add(option);
            return this;
        }

        public Builder option(String key, Predicate<Ctx> when, Action action) {
            return option(new Option(key, when, action));
        }

        public Builder options(List<Option> more) {
            options.addAll(more);
            return this;
        }

        public Node register() {
            Node node = new Node(key, when, priority, List.copyOf(options));
            Dialogue.register(who, node);
            return node;
        }
    }

    private Dialogue() {
    }
}
