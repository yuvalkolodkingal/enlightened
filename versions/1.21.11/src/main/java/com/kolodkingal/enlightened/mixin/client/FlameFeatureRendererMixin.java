package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.fx.BlueBurning;
import com.kolodkingal.enlightened.compat.RenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A creature set alight by Azula's fire burns with blue flames: the game's flame overlay is drawn with the soul fire
 * sprites instead. Client only. (1.21.11: the flames are drawn by the flame feature renderer, which takes the sprites
 * from the atlas manager; EntityRenderDispatcherMixin is not applied there.)
 */
@Mixin(FlameFeatureRenderer.class)
public abstract class FlameFeatureRendererMixin {
    private static final Material ENLIGHTENED$SOUL_0 = new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/soul_fire_0"));
    private static final Material ENLIGHTENED$SOUL_1 = new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/soul_fire_1"));

    @Redirect(method = "renderFlame", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/AtlasManager;get(Lnet/minecraft/client/resources/model/Material;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private TextureAtlasSprite enlightened$blueFlame(AtlasManager atlas, Material material, PoseStack.Pose pose, MultiBufferSource buffers,
                                                     EntityRenderState state, Quaternionf rotation, AtlasManager atlasArg) {
        Entity entity = RenderBridge.entity(state);
        if (entity != null && BlueBurning.isBlue(entity)) {
            return atlas.get(material.equals(ModelBakery.FIRE_1) ? ENLIGHTENED$SOUL_1 : ENLIGHTENED$SOUL_0);
        }
        return atlas.get(material);
    }
}
