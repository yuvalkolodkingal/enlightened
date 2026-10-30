package com.kolodkingal.enlightened.bending;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-player bending state. Attached to every player, saved with the player, kept on death and synced to every
 * client that can see the player (other clients draw suits, armour shells, shields and the HUD from it).
 *
 * <p>Saved: the element, the look, the level of every ability, the points and experience, the kills, the loadout
 * (which ability sits on which of the ten key slots), the toggles that are on, whether the airbender let go of his
 * tethers, and the water the bender carries. Runtime (synced, not saved): which abilities are active right now (a key
 * held, an effect running), the cooldowns, the entity a bloodbender holds, and the bending that is suppressed.
 */
public final class BendingData {
    /** Five keys, each with a plain and a Sneak variant. */
    public static final int KEYS = 5;
    public static final int SLOTS = KEYS * 2;
    private static final int COUNT = Ability.values().length;

    // ---- persisted ----
    private Element element = Element.NONE;
    private String variant = "";
    private final byte[] levels = new byte[COUNT];
    private int points;
    private int xp;
    private int kills;
    /** The ability on each slot (an ordinal), or -1. Slot {@code (key - 1) * 2} is the plain key, {@code + 1} its Sneak. */
    private final int[] slots = new int[SLOTS];
    /** The toggles that are on, one bit per ability (two words: there are more than 64 abilities). */
    private final long[] toggles = new long[2];
    private boolean tetherless;
    private float water;
    /** The chakras open (0..7), the meditation toward the next (ticks), and when the Avatar State may come again. */
    private int chakras;
    private int meditation;
    private long avatarCooldownUntil;
    /** The waterbender keeps his mastered octopus form to the arms alone, not wrapping his whole body (saved, synced). */
    private boolean octopusArms;
    /** Guru Pathik has taught him to open his chakras (saved, synced). */
    private boolean guruTaught;

    // ---- runtime (synced, not saved) ----
    private final long[] active = new long[2];
    private final long[] cooldownUntil = new long[COUNT];
    private int heldEntityId = -1;
    private long suppressedUntil;
    private boolean flying;
    private boolean meditating;
    /** Whether the airbender's glider staff is open (runtime, synced). */
    private boolean gliderOpen;
    private long meditateSince;
    private long avatarUntil;

    private boolean dirty;

    public BendingData() {
        Arrays.fill(slots, -1);
    }

    public static final Codec<BendingData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Element.CODEC.optionalFieldOf("element", Element.NONE).forGetter(d -> d.element),
            Codec.STRING.optionalFieldOf("variant", "").forGetter(d -> d.variant),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(BendingData::levelMap),
            Codec.INT.optionalFieldOf("points", 0).forGetter(d -> d.points),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(d -> d.xp),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(d -> d.kills),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(BendingData::slotList),
            Codec.LONG.optionalFieldOf("toggles", 0L).forGetter(d -> d.toggles[0]),
            Codec.LONG.optionalFieldOf("toggles2", 0L).forGetter(d -> d.toggles[1]),
            Codec.BOOL.optionalFieldOf("tetherless", false).forGetter(d -> d.tetherless),
            Codec.FLOAT.optionalFieldOf("water", 0F).forGetter(d -> d.water),
            Codec.INT.optionalFieldOf("chakras", 0).forGetter(d -> d.chakras),
            Codec.INT.optionalFieldOf("meditation", 0).forGetter(d -> d.meditation),
            Codec.LONG.optionalFieldOf("avatarCooldownUntil", 0L).forGetter(d -> d.avatarCooldownUntil),
            Codec.BOOL.optionalFieldOf("octopusArms", false).forGetter(d -> d.octopusArms),
            Codec.BOOL.optionalFieldOf("guruTaught", false).forGetter(d -> d.guruTaught)
    ).apply(i, (element, variant, levels, points, xp, kills, slots, toggles, toggles2, tetherless, water, chakras, meditation, avatarCooldownUntil, octopusArms, guruTaught) -> {
        BendingData d = new BendingData();
        d.element = element;
        d.variant = variant;
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            Ability a = Ability.byId(e.getKey());
            if (a != null) {
                d.levels[a.ordinal()] = (byte) Math.max(0, Math.min(Ability.MAX_LEVEL, e.getValue()));
            }
        }
        d.points = points;
        d.xp = xp;
        d.kills = kills;
        for (int s = 0; s < Math.min(SLOTS, slots.size()); s++) {
            Ability a = Ability.byId(slots.get(s));
            d.slots[s] = a == null ? -1 : a.ordinal();
        }
        d.toggles[0] = toggles;
        d.toggles[1] = toggles2;
        d.tetherless = tetherless;
        d.water = water;
        d.chakras = Math.max(0, Math.min(7, chakras));
        d.meditation = meditation;
        d.avatarCooldownUntil = avatarCooldownUntil;
        d.octopusArms = octopusArms;
        d.guruTaught = guruTaught;
        return d;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, BendingData> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> d.write(buf), BendingData::read);

    private Map<String, Integer> levelMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Ability a : Ability.values()) {
            if (levels[a.ordinal()] > 0) {
                map.put(a.id(), (int) levels[a.ordinal()]);
            }
        }
        return map;
    }

    private List<String> slotList() {
        List<String> list = new ArrayList<>(SLOTS);
        for (int s = 0; s < SLOTS; s++) {
            list.add(slots[s] < 0 ? "" : Ability.values()[slots[s]].id());
        }
        return list;
    }

    private void write(ByteBuf buf) {
        ByteBufCodecs.VAR_INT.encode(buf, element.ordinal());
        ByteBufCodecs.stringUtf8(64).encode(buf, variant);
        for (int k = 0; k < COUNT; k++) {
            buf.writeByte(levels[k]);
        }
        ByteBufCodecs.VAR_INT.encode(buf, points);
        ByteBufCodecs.VAR_INT.encode(buf, xp);
        ByteBufCodecs.VAR_INT.encode(buf, kills);
        for (int s = 0; s < SLOTS; s++) {
            ByteBufCodecs.VAR_INT.encode(buf, slots[s] + 1);
        }
        ByteBufCodecs.VAR_LONG.encode(buf, toggles[0]);
        ByteBufCodecs.VAR_LONG.encode(buf, toggles[1]);
        ByteBufCodecs.BOOL.encode(buf, tetherless);
        ByteBufCodecs.FLOAT.encode(buf, water);
        ByteBufCodecs.VAR_LONG.encode(buf, active[0]);
        ByteBufCodecs.VAR_LONG.encode(buf, active[1]);
        for (int k = 0; k < COUNT; k++) {
            ByteBufCodecs.VAR_LONG.encode(buf, cooldownUntil[k]);
        }
        ByteBufCodecs.VAR_INT.encode(buf, heldEntityId + 1);
        ByteBufCodecs.VAR_LONG.encode(buf, suppressedUntil);
        ByteBufCodecs.BOOL.encode(buf, flying);
        ByteBufCodecs.VAR_INT.encode(buf, chakras);
        ByteBufCodecs.VAR_INT.encode(buf, meditation);
        ByteBufCodecs.VAR_LONG.encode(buf, avatarCooldownUntil);
        ByteBufCodecs.BOOL.encode(buf, meditating);
        ByteBufCodecs.VAR_LONG.encode(buf, meditateSince);
        ByteBufCodecs.VAR_LONG.encode(buf, avatarUntil);
        ByteBufCodecs.BOOL.encode(buf, gliderOpen);
        ByteBufCodecs.BOOL.encode(buf, octopusArms);
        ByteBufCodecs.BOOL.encode(buf, guruTaught);
    }

    private static BendingData read(ByteBuf buf) {
        BendingData d = new BendingData();
        int ordinal = ByteBufCodecs.VAR_INT.decode(buf);
        Element[] elements = Element.values();
        d.element = ordinal >= 0 && ordinal < elements.length ? elements[ordinal] : Element.NONE;
        d.variant = ByteBufCodecs.stringUtf8(64).decode(buf);
        for (int k = 0; k < COUNT; k++) {
            d.levels[k] = buf.readByte();
        }
        d.points = ByteBufCodecs.VAR_INT.decode(buf);
        d.xp = ByteBufCodecs.VAR_INT.decode(buf);
        d.kills = ByteBufCodecs.VAR_INT.decode(buf);
        for (int s = 0; s < SLOTS; s++) {
            int v = ByteBufCodecs.VAR_INT.decode(buf) - 1;
            d.slots[s] = v >= 0 && v < COUNT ? v : -1;
        }
        d.toggles[0] = ByteBufCodecs.VAR_LONG.decode(buf);
        d.toggles[1] = ByteBufCodecs.VAR_LONG.decode(buf);
        d.tetherless = ByteBufCodecs.BOOL.decode(buf);
        d.water = ByteBufCodecs.FLOAT.decode(buf);
        d.active[0] = ByteBufCodecs.VAR_LONG.decode(buf);
        d.active[1] = ByteBufCodecs.VAR_LONG.decode(buf);
        for (int k = 0; k < COUNT; k++) {
            d.cooldownUntil[k] = ByteBufCodecs.VAR_LONG.decode(buf);
        }
        d.heldEntityId = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        d.suppressedUntil = ByteBufCodecs.VAR_LONG.decode(buf);
        d.flying = ByteBufCodecs.BOOL.decode(buf);
        d.chakras = ByteBufCodecs.VAR_INT.decode(buf);
        d.meditation = ByteBufCodecs.VAR_INT.decode(buf);
        d.avatarCooldownUntil = ByteBufCodecs.VAR_LONG.decode(buf);
        d.meditating = ByteBufCodecs.BOOL.decode(buf);
        d.meditateSince = ByteBufCodecs.VAR_LONG.decode(buf);
        d.avatarUntil = ByteBufCodecs.VAR_LONG.decode(buf);
        d.gliderOpen = ByteBufCodecs.BOOL.decode(buf);
        d.octopusArms = ByteBufCodecs.BOOL.decode(buf);
        d.guruTaught = ByteBufCodecs.BOOL.decode(buf);
        return d;
    }

    // ---- element and look ----

    public Element element() {
        return element;
    }

    /** Sets the element and resets everything that belonged to the old one. Use {@link Benders#setElement} from game code. */
    void setElementRaw(Element element) {
        this.element = element;
        this.variant = "";
        Arrays.fill(levels, (byte) 0);
        Arrays.fill(slots, -1);
        this.points = 0;
        this.xp = 0;
        this.kills = 0;
        java.util.Arrays.fill(this.toggles, 0L);
        this.tetherless = false;
        this.water = 0;
        java.util.Arrays.fill(this.active, 0L);
        Arrays.fill(cooldownUntil, 0L);
        this.heldEntityId = -1;
        this.suppressedUntil = 0;
        dirty = true;
    }

    public String variant() {
        return variant;
    }

    void setVariantRaw(String v) {
        if (!variant.equals(v)) {
            variant = v;
            dirty = true;
        }
    }

    // ---- levels and points ----

    /** The level the player has of an ability: 0 (not learned) to 3 (master). */
    public int level(Ability a) {
        return levels[a.ordinal()];
    }

    public boolean has(Ability a) {
        return (element.commands(a.element) || a.gift()) && levels[a.ordinal()] > 0;
    }

    /** Whether he bends at all: an element, or the lion turtle's gift of energybending. */
    public boolean canBend() {
        return element.bends() || levels[Ability.ENERGYBENDING.ordinal()] > 0 || levels[Ability.ENERGY_RESTORE.ordinal()] > 0;
    }

    public void setLevel(Ability a, int level) {
        byte v = (byte) Math.max(0, Math.min(Ability.MAX_LEVEL, level));
        if (levels[a.ordinal()] != v) {
            levels[a.ordinal()] = v;
            dirty = true;
        }
    }

    /** The points spent on the element so far: the sum of every level (what a sub-bending gate counts). */
    /** The points spent on this one element's abilities (the Avatar's sub-arts open by the points of their element). */
    public int spent(Element of) {
        int sum = 0;
        for (Ability a : Ability.values()) {
            if (a.element == of) {
                for (int l = 1; l <= levels[a.ordinal()]; l++) {
                    sum += Ability.cost(l);
                }
            }
        }
        return sum;
    }

    public int spent() {
        int sum = 0;
        for (Ability a : Ability.values()) {
            if (element.commands(a.element)) {
                for (int l = 1; l <= levels[a.ordinal()]; l++) {
                    sum += Ability.cost(l);
                }
            }
        }
        return sum;
    }

    public int points() {
        return points;
    }

    public void setPoints(int v) {
        if (points != v) {
            points = v;
            dirty = true;
        }
    }

    public int xp() {
        return xp;
    }

    public void setXp(int v) {
        if (xp != v) {
            xp = v;
            dirty = true;
        }
    }

    public int kills() {
        return kills;
    }

    public void setKills(int v) {
        if (kills != v) {
            kills = v;
            dirty = true;
        }
    }

    // ---- loadout ----

    /** The ability on a slot (0 to 9), or null. */
    public Ability slot(int slot) {
        int v = slots[slot];
        return v < 0 ? null : Ability.values()[v];
    }

    public void setSlot(int slot, Ability ability) {
        int v = ability == null ? -1 : ability.ordinal();
        if (slots[slot] != v) {
            slots[slot] = v;
            dirty = true;
        }
    }

    /** The slot an ability sits on, or -1. */
    public int slotOf(Ability a) {
        for (int s = 0; s < SLOTS; s++) {
            if (slots[s] == a.ordinal()) {
                return s;
            }
        }
        return -1;
    }

    public static int slotIndex(int key, boolean sneak) {
        return (key - 1) * 2 + (sneak ? 1 : 0);
    }

    // ---- toggles, active states, cooldowns ----

    private static boolean bit(long[] bits, Ability a) {
        int o = a.ordinal();
        return (bits[o >> 6] & (1L << (o & 63))) != 0;
    }

    /** Sets one ability's bit; returns whether it changed. */
    private static boolean setBit(long[] bits, Ability a, boolean on) {
        int o = a.ordinal();
        long mask = 1L << (o & 63);
        long old = bits[o >> 6];
        long updated = on ? old | mask : old & ~mask;
        bits[o >> 6] = updated;
        return updated != old;
    }

    public boolean on(Ability a) {
        return bit(toggles, a);
    }

    public void setOn(Ability a, boolean on) {
        if (setBit(toggles, a, on)) {
            dirty = true;
        }
    }

    /**
     * The fire colour the bender chose: 0 for his look's, else 1 + {@link Variants#fireColour} (normal, blue, rainbow).
     * Kept in the two top bits of the second toggle word, which no ability reaches, so it is saved and synced with them.
     */
    public int fireChoice() {
        return (int) (toggles[1] >>> 62);
    }

    public void setFireChoice(int choice) {
        long bits = (toggles[1] & ~(3L << 62)) | ((long) (choice & 3) << 62);
        if (bits != toggles[1]) {
            toggles[1] = bits;
            dirty = true;
        }
    }

    /** A copy of the toggle bits (bit {@code ordinal & 63} of word {@code ordinal >> 6}). */
    public long[] toggleBits() {
        return toggles.clone();
    }

    /** Turns every toggle off (the chosen fire colour, kept in the same word, stays). */
    /** The element an Avatar was born to (bits 59 to 61 of the second toggle word, kept with the fire colour), or NONE. */
    public Element avatarBase() {
        int o = (int) ((toggles[1] >>> 59) & 7L);
        return o > 0 && o < Element.values().length ? Element.values()[o] : Element.NONE;
    }

    public void setAvatarBase(Element base) {
        long bits = (toggles[1] & ~(7L << 59)) | ((long) (base.ordinal() & 7) << 59);
        if (bits != toggles[1]) {
            toggles[1] = bits;
            dirty = true;
        }
    }

    /** Whether this is an Avatar born to the element of {@code ability}: its arts are stronger for him. */
    public boolean bornTo(Ability ability) {
        return element == Element.AVATAR && ability.element != Element.AVATAR && ability.element == avatarBase();
    }

    public void clearToggles() {
        long keep = toggles[1] & ((3L << 62) | (7L << 59));
        if (toggles[0] != 0 || toggles[1] != keep) {
            toggles[0] = 0L;
            toggles[1] = keep;
            dirty = true;
        }
    }

    /** Whether an ability is running right now (its key is held, or its effect is on). */
    public boolean active(Ability a) {
        return bit(active, a);
    }

    public void setActive(Ability a, boolean on) {
        if (setBit(active, a, on)) {
            dirty = true;
        }
    }

    /** A copy of the active bits (laid out as {@link #toggleBits}). */
    public long[] activeBits() {
        return active.clone();
    }

    public void clearActive() {
        if (active[0] != 0 || active[1] != 0) {
            java.util.Arrays.fill(active, 0L);
            dirty = true;
        }
    }

    public long cooldownUntil(Ability a) {
        return cooldownUntil[a.ordinal()];
    }

    public boolean isReady(Ability a, long gameTime) {
        return gameTime >= cooldownUntil[a.ordinal()];
    }

    public void startCooldown(Ability a, long gameTime, int ticks) {
        cooldownUntil[a.ordinal()] = gameTime + ticks;
        dirty = true;
    }

    public void clearCooldown(Ability a) {
        if (cooldownUntil[a.ordinal()] != 0) {
            cooldownUntil[a.ordinal()] = 0;
            dirty = true;
        }
    }

    // ---- the rest ----

    public boolean tetherless() {
        return tetherless;
    }

    public void setTetherless(boolean v) {
        if (tetherless != v) {
            tetherless = v;
            dirty = true;
        }
    }

    /** The water a waterbender carries with him, bent from a source, in units (one whip is one unit). */
    public float water() {
        return water;
    }

    public void setWater(float v) {
        v = Math.max(0, v);
        if (Math.abs(water - v) > 1.0E-4F) {
            water = v;
            dirty = true;
        }
    }

    public int heldEntityId() {
        return heldEntityId;
    }

    public void setHeldEntityId(int v) {
        if (heldEntityId != v) {
            heldEntityId = v;
            dirty = true;
        }
    }

    /** Game time until which a bloodbender has taken this player's bending away (0 when it is not). */
    public long suppressedUntil() {
        return suppressedUntil;
    }

    public void setSuppressedUntil(long v) {
        if (suppressedUntil != v) {
            suppressedUntil = v;
            dirty = true;
        }
    }

    public boolean suppressed(long gameTime) {
        return suppressedUntil > gameTime;
    }

    // ---- chakras, meditation, the Avatar State ----

    public int chakras() {
        return chakras;
    }

    public void setChakras(int v) {
        v = Math.max(0, Math.min(7, v));
        if (v != chakras) {
            chakras = v;
            dirty = true;
        }
    }

    public int meditation() {
        return meditation;
    }

    public void setMeditation(int v) {
        if (v != meditation) {
            meditation = v;
            dirty = true;
        }
    }

    public boolean meditating() {
        return meditating;
    }

    public void setMeditating(boolean v) {
        if (v != meditating) {
            meditating = v;
            dirty = true;
        }
    }

    public boolean gliderOpen() {
        return gliderOpen;
    }

    public void setGliderOpen(boolean v) {
        if (v != gliderOpen) {
            gliderOpen = v;
            dirty = true;
        }
    }

    public boolean octopusArms() {
        return octopusArms;
    }

    public void setOctopusArms(boolean v) {
        if (v != octopusArms) {
            octopusArms = v;
            dirty = true;
        }
    }

    public boolean guruTaught() {
        return guruTaught;
    }

    public void setGuruTaught(boolean v) {
        if (v != guruTaught) {
            guruTaught = v;
            dirty = true;
        }
    }

    public long meditateSince() {
        return meditateSince;
    }

    public void setMeditateSince(long v) {
        meditateSince = v;
    }

    public long avatarUntil() {
        return avatarUntil;
    }

    public void setAvatarUntil(long v) {
        if (v != avatarUntil) {
            avatarUntil = v;
            dirty = true;
        }
    }

    /** Whether the Avatar State holds him now. */
    public boolean avatar(long gameTime) {
        return avatarUntil > gameTime;
    }

    public long avatarCooldownUntil() {
        return avatarCooldownUntil;
    }

    public void setAvatarCooldownUntil(long v) {
        if (v != avatarCooldownUntil) {
            avatarCooldownUntil = v;
            dirty = true;
        }
    }

    public void clearCooldowns() {
        java.util.Arrays.fill(cooldownUntil, 0L);
        dirty = true;
    }

    public boolean flying() {
        return flying;
    }

    public void setFlying(boolean v) {
        if (flying != v) {
            flying = v;
            dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }
}
