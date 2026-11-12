package com.kolodkingal.enlightened.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets {@link com.kolodkingal.enlightened.client.anim.FirstPersonArms} draw the first-person hands itself, with the
 * extra movement of a gesture, by calling the two private methods that submit a hand with its item and an arm on its
 * own (26.3: the hands are drawn from the frame's player render state).
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("submitArmWithItem")
    void enlightened$renderArmWithItem(net.minecraft.client.renderer.state.level.PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float pitch,
                                       InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
                                       SubmitNodeCollector collector, int combinedLight);

    @Invoker("renderPlayerArm")
    void enlightened$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, float equippedProgress, float swingProgress,
                                     HumanoidArm side, net.minecraft.client.renderer.state.level.PlayerRenderState playerState);
}
