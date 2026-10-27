package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Element;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;

/**
 * Everything remembered about one bender between frames: the motion measured from their position, the smoothed
 * weights of every layer of the pose, the springs of the body's lean, the stances and gestures, and what was seen of
 * their synced data last tick (to find the edges). {@link Animator} advances it once per frame (and once per tick for
 * what needs the tick); nothing here allocates per frame.
 */
public final class AnimState {
    /** One playing gesture. */
    public static final class Slot {
        public Gesture id;
        /** Game age (ticks plus partial tick) the gesture started at. */
        public float start;
        /** When it was cut short: the weight it was fading from and the age the fade started; NaN while it plays on. */
        public float releaseFrom;
        public float releaseStart = Float.NaN;
        /** Set by the frame: the weight and the time into the gesture. */
        public float weight;
        public float t;
        /** The local player's own prediction, not yet confirmed by the synced data. */
        public boolean predicted;
        public boolean active;
    }

    public static final int SLOTS = 3;
    private static final int ABILITIES = Ability.values().length;

    // ---- identity ----
    public final UUID uuid;
    public int entityId;
    public Element element = Element.NONE;
    public String look = "";
    /** The first frame snaps every weight and spring to its target. */
    public boolean fresh = true;
    /** The age (ticks plus partial tick) of the last frame update; -1 before the first. */
    public double time = -1.0;
    /** Whether the tick has looked at this bender yet (the first tick only remembers). */
    public boolean seeded;
    /** Whether the model being drawn right now was posed from this state (not the inventory's preview). */
    public boolean posed;

    // ---- tick samples ----
    public int airTicks;
    public int groundTicks;
    public boolean onGround = true;
    public boolean wasOnGround = true;
    public boolean sprinting;
    public boolean crouching;
    /** Flying by the airbender's own power, gliding on the glider, riding the scooter, on the spout. */
    public boolean flyingEff;
    public boolean gliding;
    public boolean scooter;
    public boolean spout;

    // ---- measured motion (frame, smoothed) ----
    /** Blocks per tick forward, to the right, and up; the body's turn in degrees per tick (right positive). */
    public float fwd;
    public float side;
    public float climb;
    public float turn;
    public float speed;
    /** The speed normalised to the flight: 1 is cruise, 2 a sprint; {@link #u} the climb, -1..1. */
    public float s;
    public float sH;
    public float u;
    public float limbSwing;
    public float limbAmount;
    public float bodyYaw;
    public float pivot = 0.4F;

    // ---- weights ----
    public float wAir;
    public float wGlide;
    public float wScooter;
    public float wSprint;
    /** 1 while vanilla owns the body (swimming, gliding with an elytra, sleeping, riding): nothing here is applied. */
    public float wVanilla;
    public final float[] stance = new float[Stance.COUNT];
    public final boolean[] stanceOn = new boolean[Stance.COUNT];
    public float landWeight;

    // ---- renderer channels ----
    public final AnimMath.Spring pitch = new AnimMath.Spring();
    public float roll;
    public float addPitch;
    public float addRoll;
    public float renderPitch;
    public float renderRoll;
    public float yawAdd;
    public float gestureYaw;
    public float bob;
    public final AnimMath.Spring drop = new AnimMath.Spring();
    public float addDrop;
    public float jx;
    public float jz;

    // ---- gestures ----
    public final Slot[] slots = new Slot[SLOTS];

    // ---- edge memory ----
    public final long[] cooldowns = new long[ABILITIES];
    public long[] toggles = new long[2];
    public long[] active = new long[2];
    /** The tick each ability last went active (for how long a charge was held). */
    public final int[] activeSince = new int[ABILITIES];
    public int lastHeld = -1;
    public boolean avatar;
    public int chakras = -1;
    public boolean meditating;
    /** The age the stance now held (a charge, a grip) began at, for the rows that grow with it. */
    public float chargeAge;

    /** The age the current charge began at. */
    public float activeSinceAge() {
        return chargeAge;
    }
    /** The air counter as it stood before this tick (how long the last fall was, when the ground comes). */
    public int prevAir;

    // ---- world references ----
    @Nullable
    public Entity heldEntity;

    // ---- first person ----
    public double fpAge = Double.NEGATIVE_INFINITY;

    public AnimState(UUID uuid) {
        this.uuid = uuid;
        for (int i = 0; i < SLOTS; i++) {
            slots[i] = new Slot();
        }
    }

    public float stance(Stance stance) {
        return this.stance[stance.ordinal()];
    }

    /** A gesture of this id that plays now, or null. */
    @Nullable
    public Slot playing(Gesture id) {
        for (Slot slot : slots) {
            if (slot.active && slot.id == id) {
                return slot;
            }
        }
        return null;
    }

    /** Clears everything tied to the element (a new element, or a new world). */
    public void reseed(Element element) {
        this.element = element;
        for (Slot slot : slots) {
            slot.active = false;
            slot.id = null;
        }
        Arrays.fill(stance, 0.0F);
        Arrays.fill(stanceOn, false);
        Arrays.fill(cooldowns, 0L);
        Arrays.fill(activeSince, 0);
        toggles = new long[2];
        active = new long[2];
        heldEntity = null;
        fresh = true;
        seeded = false;
        time = -1.0;
    }
}
