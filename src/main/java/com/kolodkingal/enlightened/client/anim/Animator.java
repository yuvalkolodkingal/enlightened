package com.kolodkingal.enlightened.client.anim;

import com.kolodkingal.enlightened.air.AirBending;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.client.EnlightenedClientConfig;
import com.kolodkingal.enlightened.earth.EarthBending;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enlightened.client.anim.AnimMath.ss;

/**
 * The animator of the benders: once a tick it samples every bender in view, finds the edges in their synced data (a
 * cooldown that starts, a hold that begins or ends, a toggle that flips, a landing) and starts the gestures they call
 * for; once a frame it advances the smoothed weights, the measured motion, the lean springs and the playing gestures,
 * so the poses ({@link BenderPoses}) and the body transform ({@link BodyLean}) have everything they need.
 *
 * <p>The local player's own casts are predicted the moment the key goes down ({@link #predict}); the edge the server
 * sends a few ticks later confirms them rather than starting them twice.
 */
public final class Animator {
    private static final float TAU_MOTION = 2.5F;
    private static final float TELEPORT = 6.0F;
    private static final float EPSILON = 0.02F;
    /** The reference speeds (blocks per tick) of flight: cruise, and the climb that counts as a full one. */
    private static final float V_REF = 0.55F;
    private static final float VY_REF = 0.28F;
    /** Ticks a predicted gesture waits for the synced edge that confirms it. */
    private static final float PREDICT_WINDOW = 8.0F;
    /** Abilities whose gestures come from their active flag, not from their cooldown. */
    private static final Set<Ability> ACTIVE_DRIVEN = EnumSet.of(Ability.ROCK_LIFT, Ability.BLOODBENDING, Ability.SUFFOCATE,
            Ability.FIRE_FIST, Ability.FIRE_WHIP, Ability.SPIRIT_PROJECTION, Ability.AIR_JUMP, Ability.FIRE_BALL, Ability.LIGHTNING,
            Ability.SEISMIC_SENSE, Ability.EARTH_TUNNEL, Ability.WATER_SHIELD, Ability.FROST_BREATH, Ability.FIRE_JET, Ability.FIRE_SHIELD,
            Ability.COMBUSTION);

    private static final Map<UUID, AnimState> STATES = new HashMap<>();

    /** The state of a bender, or null when they are not animated (no element, or the layer is off). */
    @Nullable
    public static AnimState peek(Player player) {
        return STATES.get(player.getUUID());
    }

    public static void clear() {
        STATES.clear();
        HandTracker.clear();
        GestureEffects.clear();
    }

    // ====================================================================================================================
    // Tick
    // ====================================================================================================================

    /** Samples every bender in view, finds the edges in their data and sets their stances. */
    public static void tick(ClientLevel level) {
        if (!EnlightenedClientConfig.animations()) {
            STATES.clear();
            HandTracker.clear();
            return;
        }
        STATES.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        HandTracker.prune(level);
        long now = level.getGameTime();
        for (AbstractClientPlayer player : level.players()) {
            BendingData data = Benders.get(player);
            AnimState s = STATES.get(player.getUUID());
            if (!data.element().bends()) {
                if (s != null) {
                    STATES.remove(player.getUUID());
                }
                continue;
            }
            if (s == null) {
                s = new AnimState(player.getUUID());
                STATES.put(player.getUUID(), s);
                s.reseed(data.element());
            } else if (s.element != data.element()) {
                s.reseed(data.element());
            }
            s.entityId = player.getId();
            boolean remember = !s.seeded;
            sample(s, player, data);
            s.heldEntity = data.heldEntityId() < 0 ? null : level.getEntity(data.heldEntityId());
            if (!remember) {
                edges(s, player, data, now);
            }
            stanceTargets(s, player, data);
            rememberData(s, player, data);
            s.seeded = true;
            if (s.time < player.tickCount - 1.0) {
                // A bender who is not being drawn (the local player in first person) still moves on, once per tick.
                frame(player, player.yBodyRot, 0.0F);
            }
        }
        GestureEffects.tick(level);
    }

    private static void sample(AnimState s, AbstractClientPlayer p, BendingData data) {
        s.wasOnGround = s.onGround;
        s.onGround = p.onGround();
        if (s.onGround) {
            s.groundTicks = Math.min(100, s.groundTicks + 1);
            s.airTicks = 0;
        } else {
            s.airTicks = Math.min(100, s.airTicks + 1);
            s.groundTicks = 0;
        }
        s.sprinting = p.isSprinting();
        s.crouching = p.isCrouching();
        boolean flying = p instanceof LocalPlayer ? p.getAbilities().flying : data.flying();
        s.flyingEff = flying && (data.element().commands(Element.AIR) || data.has(Ability.ELEMENTAL_SPHERE) && data.on(Ability.ELEMENTAL_SPHERE)) && !p.isCreative() && !p.isSpectator();
        s.gliding = AirBending.gliding(data) && !s.onGround;
        s.scooter = data.has(Ability.AIR_SCOOTER) && data.on(Ability.AIR_SCOOTER) && !p.isPassenger();
        s.spout = data.has(Ability.WATER_SPOUT) && data.on(Ability.WATER_SPOUT) || data.active(Ability.WATER_VORTEX);
    }

    /** The edges in the synced data since last tick, and what they start. */
    private static void edges(AnimState s, AbstractClientPlayer p, BendingData data, long now) {
        float age = p.tickCount;
        long toggles = data.toggleBits();
        long active = data.activeBits();
        for (Ability a : Ability.values()) {
            int i = a.ordinal();
            long bit = 1L << i;
            long until = data.cooldownUntil(a);
            boolean cooled = until > s.cooldowns[i] && until > now;
            boolean turnedOn = (toggles & bit) != 0 && (s.toggles & bit) == 0;
            boolean turnedOff = (toggles & bit) == 0 && (s.toggles & bit) != 0;
            boolean rose = (active & bit) != 0 && (s.active & bit) == 0;
            boolean fell = (active & bit) == 0 && (s.active & bit) != 0;
            if (rose) {
                s.activeSince[i] = p.tickCount;
                s.chargeAge = age;
                Gesture g = activeStart(a);
                if (g != null) {
                    start(s, p, g, age, false);
                }
            }
            if (fell) {
                int held = p.tickCount - s.activeSince[i];
                Gesture g = activeEnd(a, held, s.crouching);
                if (g != null) {
                    start(s, p, g, age, false);
                }
            }
            if (turnedOn || turnedOff) {
                Gesture g = toggle(a, turnedOn);
                if (g != null) {
                    start(s, p, g, age, false);
                }
            }
            if (cooled && !ACTIVE_DRIVEN.contains(a)) {
                Gesture g = cast(a);
                if (g != null) {
                    start(s, p, g, age, false);
                }
            }
            if (cooled && a == Ability.LIGHTNING && data.on(Ability.LIGHTNING_REDIRECT) && (s.active & bit) == 0 && (active & bit) == 0) {
                // Lightning left him without a charge: it was someone else's, redirected.
                start(s, p, Gesture.FIRE_REDIRECTED, age, false);
            }
        }
        if (!s.look.equals(data.variant()) && !s.look.isEmpty()) {
            start(s, p, Gesture.LOOK_CHANGE, age, false);
        }
        boolean avatar = data.avatar(now);
        if (avatar && !s.avatar) {
            start(s, p, Gesture.AVATAR_ENTER, age, false);
        } else if (!avatar && s.avatar) {
            start(s, p, Gesture.AVATAR_LEAVE, age, false);
        }
        if (data.chakras() > s.chakras && s.chakras >= 0) {
            start(s, p, Gesture.CHAKRA_OPEN, age, false);
        }
        if (data.meditating() && !s.meditating) {
            start(s, p, Gesture.AIR_PROJECTION, age, false);
        }
        if (s.onGround && !s.wasOnGround && !s.scooter) {
            if (s.element == Element.AIR && s.airTicks >= 0 && lastAir(s) >= 8) {
                start(s, p, Gesture.AIR_LAND, age, false);
            } else if (s.element == Element.EARTH && lastAir(s) >= 10) {
                start(s, p, Gesture.EARTH_LAND, age, false);
            }
        }
    }

    /** How long the last flight through the air was (the air counter before this tick's ground). */
    private static int lastAir(AnimState s) {
        return s.prevAir;
    }

    private static void rememberData(AnimState s, AbstractClientPlayer p, BendingData data) {
        for (Ability a : Ability.values()) {
            s.cooldowns[a.ordinal()] = data.cooldownUntil(a);
        }
        s.toggles = data.toggleBits();
        s.active = data.activeBits();
        s.look = data.variant();
        s.lastHeld = data.heldEntityId();
        s.avatar = data.avatar(p.level().getGameTime());
        s.chakras = data.chakras();
        s.meditating = data.meditating();
        s.prevAir = s.onGround ? 0 : s.airTicks;
    }

    /** The stances the data asks for this tick. */
    private static void stanceTargets(AnimState s, AbstractClientPlayer p, BendingData data) {
        boolean[] on = s.stanceOn;
        boolean fist = data.active(Ability.FIRE_FIST);
        boolean breath = fist && data.level(Ability.FIRE_FIST) >= 3 && p.isShiftKeyDown();
        on[Stance.ARMORED.ordinal()] = EarthBending.armored(data);
        on[Stance.SEISMIC.ordinal()] = data.has(Ability.SEISMIC_SENSE) && data.on(Ability.SEISMIC_SENSE) && !s.flyingEff;
        on[Stance.AIR_SHIELD.ordinal()] = data.has(Ability.AIR_SHIELD) && data.on(Ability.AIR_SHIELD);
        on[Stance.SCOOTER.ordinal()] = s.scooter;
        on[Stance.GLIDE.ordinal()] = data.gliderOpen();
        on[Stance.FLIGHT.ordinal()] = s.flyingEff && !s.onGround;
        on[Stance.OCTOPUS.ordinal()] = data.has(Ability.OCTOPUS_FORM) && data.on(Ability.OCTOPUS_FORM);
        on[Stance.SPOUT.ordinal()] = s.spout;
        on[Stance.WATER_ARMS.ordinal()] = data.has(Ability.WATER_ARMS) && data.on(Ability.WATER_ARMS);
        on[Stance.REDIRECT.ordinal()] = data.has(Ability.LIGHTNING_REDIRECT) && data.on(Ability.LIGHTNING_REDIRECT) && !data.active(Ability.LIGHTNING);
        on[Stance.ROCK_LIFT.ordinal()] = data.active(Ability.ROCK_LIFT);
        on[Stance.BLOODBENDING.ordinal()] = data.active(Ability.BLOODBENDING);
        on[Stance.SUFFOCATE.ordinal()] = data.active(Ability.SUFFOCATE);
        on[Stance.FIRE_FIST.ordinal()] = fist && !breath;
        on[Stance.FIRE_BREATH.ordinal()] = breath;
        on[Stance.FIRE_WHIP.ordinal()] = data.active(Ability.FIRE_WHIP);
        on[Stance.FIRE_BALL_CHARGE.ordinal()] = data.active(Ability.FIRE_BALL);
        on[Stance.LIGHTNING_CHARGE.ordinal()] = data.active(Ability.LIGHTNING);
        on[Stance.EARTH_TUNNEL.ordinal()] = data.active(Ability.EARTH_TUNNEL);
        on[Stance.WATER_SHIELD.ordinal()] = data.active(Ability.WATER_SHIELD);
        on[Stance.FROST_BREATH.ordinal()] = data.active(Ability.FROST_BREATH);
        on[Stance.FIRE_JET.ordinal()] = data.active(Ability.FIRE_JET);
        on[Stance.FIRE_SHIELD.ordinal()] = data.active(Ability.FIRE_SHIELD);
        on[Stance.AIR_SPOUT.ordinal()] = data.has(Ability.AIR_SPOUT) && data.on(Ability.AIR_SPOUT);
        on[Stance.MEDITATE.ordinal()] = data.meditating();
        on[Stance.AVATAR.ordinal()] = data.avatar(p.level().getGameTime());
        on[Stance.COMBUSTION.ordinal()] = data.active(Ability.COMBUSTION);
        on[Stance.PALM_FORGE.ordinal()] = data.active(Ability.PALM_FORGE);
    }

    // ---- the routes from the data to the gestures ----

    /** The gesture of a press (a cooldown that starts). */
    @Nullable
    static Gesture cast(Ability a) {
        return switch (a) {
            case ROCK_THROW -> Gesture.EARTH_ROCK_THROW;
            case EARTH_PILLAR -> Gesture.EARTH_RISE;
            case EARTH_WALL, METAL_WALL -> Gesture.EARTH_WALL;
            case SAND_WALL -> Gesture.EARTH_SAND_WAVE;
            case SAND_LIFT -> Gesture.EARTH_SAND_LIFT;
            case QUICKSAND -> Gesture.EARTH_QUICKSAND;
            case LAVA_PATH -> Gesture.EARTH_LAVA_PATH;
            case LAVA_SHURIKEN -> Gesture.EARTH_LAVA_FLICK;
            case METAL_BLADES -> Gesture.EARTH_METAL_BLADES;
            case METAL_PULL -> Gesture.EARTH_METAL_PULL;
            case WRECK -> Gesture.EARTH_WRECK;
            case AIR_BLAST -> Gesture.AIR_BLAST;
            case AIR_BLADES -> Gesture.AIR_BLADES;
            case TORNADO -> Gesture.AIR_TORNADO;
            case WATER_WHIP -> Gesture.WATER_WHIP;
            case WATER_HEALING -> Gesture.WATER_HEAL;
            case ICE_KNIVES -> Gesture.WATER_ICE_KNIVES;
            case FREEZE -> Gesture.WATER_FREEZE;
            case SNOW_THROW -> Gesture.WATER_SNOW;
            case ICE_WALL -> Gesture.WATER_ICE_WALL;
            case BLOOD_SUPPRESS -> Gesture.WATER_SUPPRESS;
            case FIRE_RING -> Gesture.FIRE_RING;
            case FIRE_HEALING -> Gesture.FIRE_HEAL;
            case SHOCKWAVE -> Gesture.EARTH_SHOCKWAVE;
            case CATAPULT -> Gesture.EARTH_CATAPULT;
            case EARTH_GRAB -> Gesture.EARTH_GRAB;
            case EXTRACTION -> Gesture.EARTH_EXTRACT;
            case AIR_SUCTION -> Gesture.AIR_SUCTION;
            case AIR_BURST -> Gesture.AIR_BURST;
            case PHASE_CHANGE -> Gesture.WATER_PHASE;
            case PLANT_TETHER -> Gesture.WATER_TETHER;
            case WALL_OF_FIRE -> Gesture.FIRE_WALL;
            case BLAZE -> Gesture.FIRE_BLAZE;
            default -> null;
        };
    }

    /** The gesture of a toggle flipping. */
    @Nullable
    static Gesture toggle(Ability a, boolean on) {
        return switch (a) {
            case EARTH_ARMOR, METAL_ARMOR -> on ? Gesture.EARTH_ARMOR_ON : Gesture.EARTH_ARMOR_OFF;
            case SEISMIC_SENSE -> on ? Gesture.EARTH_SEISMIC : null;
            case AIR_SHIELD -> on ? Gesture.AIR_SHIELD_ON : Gesture.AIR_SHIELD_OFF;
            case AIR_SCOOTER -> on ? Gesture.AIR_SCOOTER_ON : Gesture.AIR_SCOOTER_OFF;
            case FLIGHT -> on ? Gesture.AIR_FLIGHT_ON : Gesture.AIR_FLIGHT_OFF;
            case ELEMENTAL_SPHERE -> on ? Gesture.AVATAR_ENTER : Gesture.AVATAR_LEAVE;
            case OCTOPUS_FORM -> on ? Gesture.WATER_OCTOPUS_ON : Gesture.WATER_OCTOPUS_OFF;
            case WATER_SPOUT -> on ? Gesture.WATER_SPOUT_ON : Gesture.WATER_SPOUT_OFF;
            case WATER_ARMS -> on ? Gesture.WATER_ARMS_ON : Gesture.WATER_ARMS_OFF;
            case LIGHTNING_REDIRECT -> on ? Gesture.FIRE_REDIRECT_ON : Gesture.FIRE_REDIRECT_OFF;
            case AIR_SPOUT -> on ? Gesture.AIR_SPOUT_ON : Gesture.AIR_SPOUT_OFF;
            case WATER_BUBBLE -> on ? Gesture.WATER_BUBBLE_ON : Gesture.WATER_BUBBLE_OFF;
            case HEAT_CONTROL -> on ? Gesture.FIRE_HEAT_ON : Gesture.FIRE_HEAT_OFF;
            case ILLUMINATION -> on ? Gesture.FIRE_LIGHT_ON : Gesture.FIRE_LIGHT_OFF;
            default -> null;
        };
    }

    /** The gesture of a hold that begins. */
    @Nullable
    static Gesture activeStart(Ability a) {
        return switch (a) {
            case ROCK_LIFT -> Gesture.EARTH_ROCK_GRAB;
            case BLOODBENDING -> Gesture.WATER_BLOOD_GRAB;
            case SUFFOCATE -> Gesture.AIR_SUFFOCATE;
            case FIRE_FIST -> Gesture.FIRE_FIST_START;
            case FIRE_WHIP -> Gesture.FIRE_WHIP_START;
            case SPIRIT_PROJECTION -> Gesture.AIR_PROJECTION;
            case AIR_JUMP -> Gesture.AIR_JUMP;
            case EARTH_TUNNEL -> Gesture.EARTH_TUNNEL_START;
            case WATER_SHIELD -> Gesture.WATER_SHIELD_START;
            case FROST_BREATH -> Gesture.WATER_FROST_START;
            case FIRE_JET -> Gesture.FIRE_JET;
            case FIRE_SHIELD -> Gesture.FIRE_SHIELD_START;
            case COMBUSTION -> Gesture.FIRE_COMBUSTION;
            case AVATAR_STATE -> Gesture.AVATAR_ENTER;
            case ENERGYBENDING -> Gesture.WATER_SUPPRESS;
            case SPIRITBENDING -> Gesture.WATER_HEAL;
            default -> null;
        };
    }

    /** The gesture of a hold that ends after {@code held} ticks. */
    @Nullable
    static Gesture activeEnd(Ability a, int held, boolean crouching) {
        return switch (a) {
            case ROCK_LIFT -> Gesture.EARTH_ROCK_FLING;
            case BLOODBENDING -> Gesture.WATER_BLOOD_RELEASE;
            case FIRE_BALL -> held >= 12 ? Gesture.FIRE_BALL_BIG : Gesture.FIRE_BALL;
            case LIGHTNING -> crouching || held >= 40 ? Gesture.FIRE_LIGHTNING_BOTH : Gesture.FIRE_LIGHTNING;
            default -> null;
        };
    }

    /** The local player pressed an ability's key: their gesture starts now, before the server answers. */
    public static void predict(LocalPlayer player, Ability a) {
        AnimState s = STATES.get(player.getUUID());
        if (s == null) {
            return;
        }
        BendingData data = Benders.get(player);
        Gesture g = switch (a.kind) {
            case PRESS -> ACTIVE_DRIVEN.contains(a) ? (data.active(a) ? null : activeStart(a)) : cast(a);
            case HOLD -> data.active(a) ? null : activeStart(a);
            case TOGGLE -> toggle(a, !data.on(a));
            case PASSIVE -> null;
        };
        if (g != null) {
            start(s, player, g, player.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), true);
        }
    }

    /** The local player does something the data does not show yet (a step): its gesture starts now. */
    public static void playLocal(LocalPlayer player, Gesture id) {
        AnimState s = STATES.get(player.getUUID());
        if (s != null) {
            start(s, player, id, player.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), true);
        }
    }

    /** Plays a gesture on a player (the renderers and tests). */
    public static void play(Player player, Gesture id) {
        AnimState s = STATES.get(player.getUUID());
        if (s != null && player instanceof AbstractClientPlayer p) {
            start(s, p, id, player.tickCount, false);
        }
    }

    /**
     * Starts a gesture: the others of its group fade out, and a free (or the weakest) slot plays it. A predicted
     * gesture of the same id that plays already is confirmed instead of restarted.
     */
    static void start(AnimState s, AbstractClientPlayer p, Gesture id, float age, boolean predicted) {
        Gestures.Def def = Gestures.get(id);
        if (def == null) {
            return;
        }
        for (AnimState.Slot slot : s.slots) {
            if (slot.active && slot.id == id && slot.predicted && age - slot.start <= PREDICT_WINDOW) {
                slot.predicted = predicted;
                return;
            }
        }
        for (AnimState.Slot slot : s.slots) {
            if (slot.active && slot.id.group == id.group && Float.isNaN(slot.releaseStart) && !Gestures.get(slot.id).hasYaw()) {
                slot.releaseFrom = slot.weight;
                slot.releaseStart = age;
            }
        }
        AnimState.Slot target = null;
        for (AnimState.Slot slot : s.slots) {
            if (!slot.active) {
                target = slot;
                break;
            }
        }
        if (target == null) {
            for (AnimState.Slot slot : s.slots) {
                if (!Gestures.get(slot.id).hasYaw() && (target == null || slot.weight < target.weight)) {
                    target = slot;
                }
            }
        }
        if (target == null) {
            return;
        }
        target.id = id;
        target.start = age;
        target.releaseStart = Float.NaN;
        target.releaseFrom = 0.0F;
        target.weight = 0.0F;
        target.t = 0.0F;
        target.predicted = predicted;
        target.active = true;
        GestureEffects.started(p, s, id);
    }

    /** Lets a playing gesture go early: it fades out over 3 ticks instead of running to its end. */
    static void release(AnimState s, Gesture id, float age) {
        for (AnimState.Slot slot : s.slots) {
            if (slot.active && slot.id == id && Float.isNaN(slot.releaseStart)) {
                slot.releaseFrom = slot.weight;
                slot.releaseStart = age;
            }
        }
    }

    // ====================================================================================================================
    // Frame
    // ====================================================================================================================

    /** Advances the state to the frame being drawn and returns it, or null when the player is not animated. */
    @Nullable
    public static AnimState frame(AbstractClientPlayer player, float yBodyRot, float pt) {
        AnimState s = STATES.get(player.getUUID());
        if (s == null || pt >= 1.0F || !EnlightenedClientConfig.animations()) {
            return null;
        }
        double time = player.tickCount + pt;
        boolean first = s.fresh || time < s.time - 1.5;
        float dt = first ? 0.0F : (float) Mth.clamp(time - s.time, 0.0, 4.0);
        if (!first && dt <= 0.0F) {
            return s;
        }
        s.time = time;
        s.fresh = false;
        s.bodyYaw = yBodyRot;
        float age = (float) time;

        if (vanillaOwnsBody(player, pt)) {
            s.wVanilla = 1.0F;
            s.wAir = 0.0F;
            s.wGlide = 0.0F;
            s.wScooter = 0.0F;
            s.wSprint = 0.0F;
            java.util.Arrays.fill(s.stance, 0.0F);
            s.pitch.snap(0.0F);
            s.roll = 0.0F;
            s.addPitch = 0.0F;
            s.addRoll = 0.0F;
            s.renderPitch = 0.0F;
            s.renderRoll = 0.0F;
            s.yawAdd = 0.0F;
            s.bob = 0.0F;
            s.drop.snap(0.0F);
            s.addDrop = 0.0F;
            s.landWeight = 0.0F;
            for (AnimState.Slot slot : s.slots) {
                slot.active = false;
            }
            return s;
        }
        s.wVanilla = 0.0F;
        boolean snap = first;

        measure(s, player, yBodyRot, dt);

        // ---- weights ----
        boolean air = s.flyingEff && !s.onGround;
        boolean glide = s.gliding && !air;
        boolean inWater = player.isInWater();
        boolean sprint = s.sprinting && s.onGround && !s.crouching && !inWater && !s.scooter;
        s.wAir = snap ? (air ? 1.0F : 0.0F) : AnimMath.ease(s.wAir, air ? 1.0F : 0.0F, dt, 2.0F, s.onGround ? 1.5F : 3.0F);
        s.wGlide = snap ? (glide ? 1.0F : 0.0F) : AnimMath.ease(s.wGlide, glide ? 1.0F : 0.0F, dt, 2.0F, 2.5F);
        s.wScooter = snap ? (s.scooter ? 1.0F : 0.0F) : AnimMath.ease(s.wScooter, s.scooter ? 1.0F : 0.0F, dt, 2.0F, 3.0F);
        s.wSprint = snap ? (sprint ? 1.0F : 0.0F) : AnimMath.ease(s.wSprint, sprint ? 1.0F : 0.0F, dt, 3.0F, 4.0F);
        for (Stance stance : Stance.VALUES) {
            int i = stance.ordinal();
            float target = s.stanceOn[i] ? 1.0F : 0.0F;
            s.stance[i] = snap ? target : AnimMath.ease(s.stance[i], target, dt, stance.tauIn, stance.tauOut);
        }

        // ---- gestures ----
        sampleSlots(s, age);

        // ---- the lean of the body ----
        float calm = EnlightenedClientConfig.reducedMotion() ? 0.3F : 1.0F;
        RunCharacter ch = RunCharacter.of(s.element);
        float groundPitch = ch.lean * s.wSprint * ss(0.05F, 0.30F, s.speed) + 14.0F * s.wScooter * ss(0.04F, 0.30F, s.speed);
        float flightPitch = 50.0F * ss(0.15F, 1.0F, s.s) + 22.0F * ss(1.0F, 2.0F, s.s) - 20.0F * s.u * ss(0.2F, 0.8F, s.sH);
        float glidePitch = 68.0F - 22.0F * Math.max(0.0F, s.u) + 10.0F * Math.max(0.0F, -s.u);
        float pitchTarget = AnimMath.lerp(s.wAir, AnimMath.lerp(s.wGlide, groundPitch, glidePitch), flightPitch);
        if (snap) {
            s.pitch.snap(pitchTarget);
        } else if (s.wAir > 0.01F || s.wGlide > 0.01F) {
            s.pitch.damped(pitchTarget, 0.45F, 0.78F, dt);
        } else {
            float next = AnimMath.ease(s.pitch.x, pitchTarget, dt, 4.5F, 4.0F);
            s.pitch.v = (next - s.pitch.x) / Math.max(dt, 1.0E-3F);
            s.pitch.x = next;
        }
        float hover = (1.0F - ss(0.2F, 0.8F, s.sH)) * s.wAir;
        float flightRoll = Mth.clamp(1.8F * s.turn, -35.0F, 35.0F) * ss(0.2F, 0.8F, s.sH) + 4.0F * calm * hover * Mth.sin(0.04F * age);
        float glideRoll = Mth.clamp(2.2F * s.turn, -40.0F, 40.0F);
        float scooterRoll = Mth.clamp(1.2F * s.turn, -18.0F, 18.0F) * s.wScooter;
        float rollTarget = AnimMath.lerp(s.wAir, AnimMath.lerp(s.wGlide, scooterRoll, glideRoll), flightRoll);
        s.roll = snap ? rollTarget : AnimMath.ease(s.roll, rollTarget, dt, 4.0F);

        // ---- the body's other channels ----
        float bob = 0.035F * calm * Mth.sin(Mth.TWO_PI * age / 44.0F) * hover
                + 0.015F * calm * Mth.sin(0.15F * age) * s.wGlide
                + 0.02F * calm * Math.abs(Mth.sin(0.5F * age)) * ss(0.04F, 0.3F, s.speed) * s.wScooter
                + 0.012F * calm * Mth.sin(0.2F * age) * s.stance(Stance.SPOUT);
        s.bob = bob + RunCharacter.bob(s, ch) * calm;
        if (snap) {
            s.drop.snap(0.0F);
        } else {
            s.drop.damped(0.0F, 0.8F, 0.45F, dt);
        }
        compose(s);
        return s;
    }

    private static void compose(AnimState s) {
        if (!Float.isFinite(s.pitch.x) || !Float.isFinite(s.pitch.v) || !Float.isFinite(s.roll) || !Float.isFinite(s.drop.x)
                || !Float.isFinite(s.drop.v) || !Float.isFinite(s.bob) || !Float.isFinite(s.addPitch) || !Float.isFinite(s.addDrop)) {
            s.pitch.snap(0.0F);
            s.roll = 0.0F;
            s.drop.snap(0.0F);
            s.bob = 0.0F;
            s.addPitch = 0.0F;
            s.addDrop = 0.0F;
        }
        s.yawAdd = s.gestureYaw;
        s.addDrop += Stances.drop(s);
        s.renderPitch = s.pitch.x + s.addPitch + Stances.lean(s);
        s.renderRoll = s.roll + s.addRoll;
        float lifted = Math.max(s.wAir, s.wGlide);
        s.pivot = AnimMath.lerp(s.landWeight, AnimMath.lerp(lifted, 0.40F, 0.90F), 0.40F);
    }

    /** Vanilla already turns the body of a dying, sleeping, elytra-gliding, swimming or riding player. */
    private static boolean vanillaOwnsBody(AbstractClientPlayer player, float pt) {
        return player.deathTime > 0 || player.isSleeping() || player.isFallFlying() || player.isAutoSpinAttack()
                || player.isPassenger() || player.getSwimAmount(pt) > 0.0F || player.isSpectator();
    }

    private static void measure(AnimState s, AbstractClientPlayer p, float yBodyRot, float dt) {
        double dx = p.getX() - p.xOld;
        double dy = p.getY() - p.yOld;
        double dz = p.getZ() - p.zOld;
        if (dx * dx + dy * dy + dz * dz > TELEPORT * TELEPORT) {
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
        }
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        float forward = (float) (-dx * Mth.sin(yaw) + dz * Mth.cos(yaw));
        float sideways = (float) (-dx * Mth.cos(yaw) - dz * Mth.sin(yaw));
        s.fwd = smooth(s.fwd, forward, dt);
        s.side = smooth(s.side, sideways, dt);
        s.climb = smooth(s.climb, (float) dy, dt);
        s.turn = smooth(s.turn, Mth.wrapDegrees(p.yBodyRot - p.yBodyRotO), dt);
        s.speed = Mth.sqrt(s.fwd * s.fwd + s.side * s.side);
        s.s = s.fwd / V_REF;
        s.sH = s.speed / V_REF;
        s.u = AnimMath.clamp(s.climb / VY_REF, -1.0F, 1.0F);
        float pt = s.fresh ? 0.0F : (float) (s.time - p.tickCount);
        s.limbSwing = p.walkAnimation.position(pt);
        s.limbAmount = Math.min(1.0F, p.walkAnimation.speed(pt));
    }

    private static float smooth(float current, float measured, float dt) {
        return dt <= 0.0F ? measured : AnimMath.ease(current, measured, dt, TAU_MOTION);
    }

    /** Plays the slots for this frame: weights, and what they add to the lean, the roll, the dip and the turn. */
    private static void sampleSlots(AnimState s, float age) {
        s.jx = 0.0F;
        s.jz = 0.0F;
        s.addPitch = 0.0F;
        s.addRoll = 0.0F;
        s.addDrop = 0.0F;
        s.gestureYaw = 0.0F;
        s.landWeight = 0.0F;
        for (AnimState.Slot slot : s.slots) {
            if (!slot.active) {
                continue;
            }
            Gesture id = slot.id;
            Gestures.Def def = Gestures.get(id);
            float t = age - slot.start;
            float weight;
            if (!Float.isNaN(slot.releaseStart)) {
                float u = (age - slot.releaseStart) / 3.0F;
                weight = u >= 1.0F ? 0.0F : slot.releaseFrom * (1.0F - AnimMath.easeInOutSine(Math.max(u, 0.0F)));
                if (u >= 1.0F) {
                    slot.active = false;
                    continue;
                }
            } else {
                if (t >= id.dur) {
                    slot.active = false;
                    continue;
                }
                weight = AnimMath.envelope(Math.max(t, 0.0F), id.attack, id.dur, id.release);
            }
            slot.weight = weight;
            slot.t = t;
            s.addPitch += def.lean.scalar(t, 0.0F) * weight;
            s.addRoll += def.roll.scalar(t, 0.0F) * weight;
            s.addDrop += def.drop.scalar(t, 0.0F) * weight;
            if (def.hasYaw()) {
                s.gestureYaw += def.yaw.scalar(t, 0.0F);
            }
            if (id.group == Gesture.Group.LAND) {
                s.landWeight = Math.max(s.landWeight, weight);
            }
        }
    }

    // ====================================================================================================================
    // Body transform
    // ====================================================================================================================

    /**
     * Moves the pose stack (in the entity's frame: x right, y up, -z forward, already turned to its body yaw) to where
     * the bender's body is this frame: lifted by the bob, sunk by a landing, turned about its vertical, and leaned about
     * the pivot: the feet on the ground, the waist in the air.
     */
    public static void applyBody(PoseStack poseStack, AnimState s, Player player) {
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float crouchFix = player.isCrouching() ? 0.125F * s.wAir : 0.0F;
        float dy = s.bob - s.drop.x - s.addDrop + crouchFix;
        if (Math.abs(dy) > 1.0E-4F || s.jx != 0.0F || s.jz != 0.0F) {
            poseStack.translate(s.jx, dy, s.jz);
        }
        if (Math.abs(s.yawAdd) > 1.0E-3F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-s.yawAdd * side));
        }
        if (Math.abs(s.renderPitch) < EPSILON && Math.abs(s.renderRoll) < EPSILON) {
            return;
        }
        poseStack.translate(0.0F, s.pivot, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-s.renderRoll));
        poseStack.mulPose(Axis.XP.rotationDegrees(-s.renderPitch));
        poseStack.translate(0.0F, -s.pivot, 0.0F);
    }

    /**
     * Moves a point of the upright body (a world position) to where this frame's body has put it, so that energy
     * leaves the head or hand that is drawn. A body that has not been drawn lately leaves it be.
     */
    public static Vec3 follow(Player player, Vec3 point, float partialTick) {
        AnimState s = peek(player);
        if (s == null || s.wVanilla > 0.0F || Math.abs(player.tickCount + partialTick - s.time) > 2.0F) {
            return point;
        }
        if (Math.abs(s.renderPitch) < EPSILON && Math.abs(s.renderRoll) < EPSILON && Math.abs(s.bob) < 1.0E-4F
                && Math.abs(s.drop.x + s.addDrop) < 1.0E-4F && Math.abs(s.yawAdd) < 1.0E-3F) {
            return point;
        }
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        Vec3 feet = player.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        Quaternionf yaw = Axis.YP.rotationDegrees(180.0F - bodyYaw);
        Quaternionf inverse = yaw.conjugate(new Quaternionf());
        Vector3f v = new Vector3f((float) (point.x - feet.x), (float) (point.y - feet.y), (float) (point.z - feet.z));
        inverse.transform(v);
        v.y -= s.pivot;
        v.rotateX(-s.renderPitch * Mth.DEG_TO_RAD).rotateZ(-s.renderRoll * Mth.DEG_TO_RAD);
        v.y += s.pivot;
        v.rotateY(-s.yawAdd * side * Mth.DEG_TO_RAD);
        v.add(s.jx, s.bob - s.drop.x - s.addDrop, s.jz);
        yaw.transform(v);
        return feet.add(v.x, v.y, v.z);
    }

    private Animator() {
    }
}
