package com.kolodkingal.enlightened.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Where the parts of a bender really are this frame: the hands, the feet and the forehead, as world positions.
 * {@link HandTrackerLayer} (the last layer drawn) records them from the pose stack the body is drawn with, so every
 * flame, mote and wisp starts where the model is drawn, whatever the lean or the pose.
 *
 * <p>A point that was not drawn lately (the bender is out of view, or is the local player in first person) is null:
 * callers keep their own approximation then.
 */
public final class HandTracker {
    private static final float FRESH_TICKS = 2.0F;

    private static final class Points {
        double age = Double.NEGATIVE_INFINITY;
        Vec3 handRight;
        Vec3 handLeft;
        Vec3 footRight;
        Vec3 footLeft;
        Vec3 forehead;
    }

    private static final Map<Integer, Points> POINTS = new HashMap<>();

    /** The middle of the fist of this hand. */
    @Nullable
    public static Vec3 hand(Player player, HumanoidArm arm, float partialTick) {
        Points p = fresh(player, partialTick);
        return p == null ? null : arm == HumanoidArm.RIGHT ? p.handRight : p.handLeft;
    }

    /** The sole of this foot. */
    @Nullable
    public static Vec3 foot(Player player, HumanoidArm side, float partialTick) {
        Points p = fresh(player, partialTick);
        return p == null ? null : side == HumanoidArm.RIGHT ? p.footRight : p.footLeft;
    }

    @Nullable
    public static Vec3 forehead(Player player, float partialTick) {
        Points p = fresh(player, partialTick);
        return p == null ? null : p.forehead;
    }

    /** The hand, or a guess from the player's position when the body was not drawn lately. */
    public static Vec3 handOrGuess(Player player, HumanoidArm arm, float partialTick) {
        Vec3 hand = hand(player, arm, partialTick);
        if (hand != null) {
            return hand;
        }
        boolean right = arm == HumanoidArm.RIGHT;
        Vec3 look = player.getViewVector(partialTick);
        Vec3 sideways = new Vec3(-look.z, 0.0, look.x).normalize().scale(right ? 0.35 : -0.35);
        return player.getEyePosition(partialTick).add(look.scale(0.4)).add(sideways).add(0.0, -0.45, 0.0);
    }

    @Nullable
    private static Points fresh(Player player, float partialTick) {
        Points p = POINTS.get(player.getId());
        if (p == null || Math.abs(player.tickCount + partialTick - p.age) > FRESH_TICKS) {
            return null;
        }
        return p;
    }

    static void record(Player player, double age, Vec3 handRight, Vec3 handLeft, Vec3 footRight, Vec3 footLeft, Vec3 forehead) {
        Points p = POINTS.computeIfAbsent(player.getId(), id -> new Points());
        p.age = age;
        p.handRight = handRight;
        p.handLeft = handLeft;
        p.footRight = footRight;
        p.footLeft = footLeft;
        p.forehead = forehead;
    }

    /** A point of the pose stack (entity-render space, relative to the camera) as a world position. */
    static Vec3 world(PoseStack ps, float x, float y, float z) {
        Vector3f v = new Vector3f(x, y, z);
        ps.last().pose().transformPosition(v);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return new Vec3(camera.x + v.x, camera.y + v.y, camera.z + v.z);
    }

    static void prune(ClientLevel level) {
        POINTS.keySet().removeIf(id -> level.getEntity(id) == null);
    }

    static void clear() {
        POINTS.clear();
    }

    private HandTracker() {
    }
}
