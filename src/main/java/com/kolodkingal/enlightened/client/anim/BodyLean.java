package com.kolodkingal.enlightened.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Moves a bender's whole body on the render pose stack (so armour, held items and every overlay layer follow, and
 * the name tag stays where it is): the lean and bank of the flight and the glider, the hover bob, the dip of a stomp
 * or a landing, the turn of a spin. The motion itself is worked out by {@link Animator}; this is the small, stable
 * door the renderers and effects use.
 */
public final class BodyLean {

    /** Updates this player's animation for the frame being drawn and applies the body's motion to the pose stack. */
    public static void apply(AbstractClientPlayer player, PoseStack poseStack, float yBodyRot, float partialTick) {
        AnimState state = Animator.frame(player, yBodyRot, partialTick);
        if (state != null && state.wVanilla <= 0.0F) {
            Animator.applyBody(poseStack, state, player);
        }
    }

    /** Moves a point of the upright body (a world position, like the eye's) to where the body's motion has taken it. */
    public static Vec3 follow(Player player, Vec3 point, float partialTick) {
        return Animator.follow(player, point, partialTick);
    }

    /** The forward pitch (radians) this player's body is drawn with, or 0. */
    public static float pitch(Player player) {
        AnimState state = Animator.peek(player);
        return state != null && state.wVanilla <= 0.0F ? state.renderPitch * Mth.DEG_TO_RAD : 0.0F;
    }

    /** The bank (degrees, right side down positive) this player's body is drawn with, or 0. */
    public static float roll(Player player) {
        AnimState state = Animator.peek(player);
        return state != null && state.wVanilla <= 0.0F ? state.renderRoll : 0.0F;
    }

    public static void tick(ClientLevel level) {
        Animator.tick(level);
    }

    public static void clear() {
        Animator.clear();
    }

    private BodyLean() {
    }
}
