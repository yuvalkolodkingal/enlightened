package com.kolodkingal.enlightened.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 1.21.11: an item drawn by the mod (1.21.1's {@code ItemRenderer.renderStatic}) is resolved and submitted. */
public final class ItemSubmit {
    private ItemSubmit() {
    }

    /** An item a living entity carries. */
    public static void living(LivingEntity holder, ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector,
                              int light, int overlay) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(state, stack, context, holder);
        state.submit(pose, collector, light, overlay, 0);
    }

    /** An item shown by another entity (a thrown shot). */
    public static void entity(Entity holder, ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector,
                              int light, int overlay) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForNonLiving(state, stack, context, holder);
        state.submit(pose, collector, light, overlay, 0);
    }
}
