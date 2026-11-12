package com.kolodkingal.enlightened.mixin.client;

import com.kolodkingal.enlightened.client.fx.BlueBurning;
import com.kolodkingal.enlightened.compat.RenderBridge;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A creature set alight by Azula's fire burns with blue flames: the game's flame overlay is drawn with the soul fire
 * sprites instead. Client only. (26.3: the flame feature renderer is given its two sprites for each flame.)
 */
@Mixin(FlameFeatureRenderer.class)
public abstract class FlameFeatureRendererMixin {
    private static final SpriteId ENLIGHTENED$SOUL_0 = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/soul_fire_0"));
    private static final SpriteId ENLIGHTENED$SOUL_1 = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/soul_fire_1"));

    @ModifyVariable(method = "prepare", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private TextureAtlasSprite enlightened$blueFlame0(TextureAtlasSprite sprite, FlameFeatureRenderer.Submit submit, VertexConsumer buffer,
                                                      TextureAtlasSprite fire1, TextureAtlasSprite fire2) {
        return enlightened$blue(submit) ? Minecraft.getInstance().getAtlasManager().get(ENLIGHTENED$SOUL_0) : sprite;
    }

    @ModifyVariable(method = "prepare", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private TextureAtlasSprite enlightened$blueFlame1(TextureAtlasSprite sprite, FlameFeatureRenderer.Submit submit, VertexConsumer buffer,
                                                      TextureAtlasSprite fire1, TextureAtlasSprite fire2) {
        return enlightened$blue(submit) ? Minecraft.getInstance().getAtlasManager().get(ENLIGHTENED$SOUL_1) : sprite;
    }

    private static boolean enlightened$blue(FlameFeatureRenderer.Submit submit) {
        Entity entity = RenderBridge.entity(submit.entityRenderState());
        return entity != null && BlueBurning.isBlue(entity);
    }
}
