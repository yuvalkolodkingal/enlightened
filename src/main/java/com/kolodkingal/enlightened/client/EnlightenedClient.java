package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.client.anim.HandTrackerLayer;
import com.kolodkingal.enlightened.client.particle.EnergyParticle;
import com.kolodkingal.enlightened.client.render.MeditatingBodyRenderer;
import com.kolodkingal.enlightened.client.render.ShellLayer;
import com.kolodkingal.enlightened.client.render.ShotRenderer;
import com.kolodkingal.enlightened.item.GliderItem;
import com.kolodkingal.enlightened.registry.ModEntities;
import com.kolodkingal.enlightened.registry.ModItems;
import com.kolodkingal.enlightened.registry.ModParticles;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** The client half of the mod: keys, the HUD, renderers, layers, particles, the glider's colour and the config screen. */
@Mod(value = Enlightened.MODID, dist = Dist.CLIENT)
public final class EnlightenedClient {
    /** A player model inflated past the skin's outer layer, for the earth and metal armour shells. */
    public static final ModelLayerLocation SHELL_WIDE = new ModelLayerLocation(Enlightened.id("player_shell"), "wide");
    public static final ModelLayerLocation SHELL_SLIM = new ModelLayerLocation(Enlightened.id("player_shell"), "slim");

    public EnlightenedClient(ModContainer container, IEventBus modBus) {
        modBus.addListener(EnlightenedClient::onKeys);
        modBus.addListener(EnlightenedClient::onGuiLayers);
        modBus.addListener(EnlightenedClient::onParticles);
        modBus.addListener(EnlightenedClient::onRenderers);
        modBus.addListener(EnlightenedClient::onLayerDefinitions);
        modBus.addListener(EnlightenedClient::onAddLayers);
        modBus.addListener(EnlightenedClient::onItemColors);
        container.registerConfig(ModConfig.Type.CLIENT, EnlightenedClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void onKeys(RegisterKeyMappingsEvent event) {
        for (var key : KeyBindings.ABILITIES) {
            event.register(key);
        }
        event.register(KeyBindings.CHANGE_LOOK);
        event.register(KeyBindings.SCROLL);
    }

    private static void onGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Enlightened.id("bending"), new BendingHud());
    }

    private static void onParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ENERGY.get(), EnergyParticle.Provider::new);
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHOT.get(), ShotRenderer::new);
        event.registerEntityRenderer(ModEntities.TORNADO.get(), com.kolodkingal.enlightened.client.render.TornadoRenderer::new);
        event.registerEntityRenderer(ModEntities.MEDITATING_BODY.get(), MeditatingBodyRenderer::new);
        event.registerEntityRenderer(ModEntities.LEMUR.get(), com.kolodkingal.enlightened.client.render.LemurRenderer::new);
    }

    private static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.LemurRenderer.LAYER,
                com.kolodkingal.enlightened.client.render.LemurModel::createBodyLayer);
        event.registerLayerDefinition(SHELL_WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.6F), false), 64, 64));
        event.registerLayerDefinition(SHELL_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.6F), true), 64, 64));
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                renderer.addLayer(new ShellLayer(renderer, event.getEntityModels().bakeLayer(slim ? SHELL_SLIM : SHELL_WIDE), slim));
                renderer.addLayer(new HandTrackerLayer(renderer));
            }
        }
    }

    private static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 1 ? 0xFF000000 | GliderItem.color(stack) : 0xFFFFFFFF, ModItems.AIR_GLIDER.get());
    }
}
