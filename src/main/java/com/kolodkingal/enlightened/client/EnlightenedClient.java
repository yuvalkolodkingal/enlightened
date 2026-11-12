package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.client.anim.HandTrackerLayer;
import com.kolodkingal.enlightened.client.particle.EnergyParticle;
import com.kolodkingal.enlightened.client.render.MeditatingBodyRenderer;
import com.kolodkingal.enlightened.client.render.AvatarGlowLayer;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** The client half of the mod: keys, the HUD, renderers, layers, particles, the glider's colour and the config screen. */
/* Started by boot.EnlightenedBoot on a client. */
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
        //#if <1.21.11
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent e) ->
                e.register(Enlightened.id("cosmic"), new com.kolodkingal.enlightened.client.cosmic.CosmicEffects()));
        //#endif
        // (1.21.11: the cosmic plane's sky is its dimension type's attributes, in the 1.21.11 data overlay)
        //#if >=1.21.4
        //$$ // (the glider's dye is its item model's tint from 1.21.4: assets/enlightened/items/air_glider.json)
        //$$ modBus.addListener(com.kolodkingal.enlightened.compat.RenderBridge::register);
        //#else
        modBus.addListener(EnlightenedClient::onItemColors);
        //#endif
        container.registerConfig(ModConfig.Type.CLIENT, EnlightenedClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // (registered here, not found by scanning: see Enlightened)
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ClientEvents.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.kolodkingal.enlightened.client.anim.FirstPersonArms.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.kolodkingal.enlightened.client.dev.Tour.class);
    }

    private static void onKeys(RegisterKeyMappingsEvent event) {
        //#if >=1.21.11
        //$$ event.registerCategory(KeyBindings.CATEGORY);
        //#endif
        for (var key : KeyBindings.ABILITIES) {
            event.register(key);
        }
        event.register(KeyBindings.CHANGE_LOOK);
        event.register(KeyBindings.SCROLL);
        event.register(KeyBindings.MEDITATE);
        event.register(KeyBindings.HUD_STYLE);
        event.register(KeyBindings.WHEEL);
    }

    private static void onGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Enlightened.id("bending"), new BendingHud());
        event.registerAboveAll(Enlightened.id("dread"), new DreadOverlay());
    }

    private static void onParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ENERGY.get(), EnergyParticle.Provider::new);
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHOT.get(), ShotRenderer::new);
        event.registerEntityRenderer(ModEntities.TORNADO.get(), com.kolodkingal.enlightened.client.render.TornadoRenderer::new);
        event.registerEntityRenderer(ModEntities.MEDITATING_BODY.get(), MeditatingBodyRenderer::new);
        event.registerEntityRenderer(ModEntities.LEMUR.get(), com.kolodkingal.enlightened.client.render.LemurRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_WALL.get(), com.kolodkingal.enlightened.client.render.FireWallRenderer::new);
        event.registerEntityRenderer(ModEntities.GURU_PATHIK.get(), com.kolodkingal.enlightened.client.render.GuruPathikRenderer::new);
        event.registerEntityRenderer(ModEntities.SKY_BISON.get(), com.kolodkingal.enlightened.client.render.SkyBisonRenderer::new);
        event.registerEntityRenderer(ModEntities.LION_TURTLE.get(), com.kolodkingal.enlightened.client.render.LionTurtleRenderer::new);
        event.registerEntityRenderer(ModEntities.CITY_GUARD.get(), com.kolodkingal.enlightened.client.render.CityGuardRenderer::new);
        event.registerEntityRenderer(ModEntities.OZAI.get(), com.kolodkingal.enlightened.client.render.NationRenderer::ozai);
        event.registerEntityRenderer(ModEntities.NATION_NPC.get(), com.kolodkingal.enlightened.client.render.NationRenderer::npc);
        event.registerEntityRenderer(ModEntities.IROH.get(), com.kolodkingal.enlightened.client.render.IrohRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.KING_BUMI.get(), com.kolodkingal.enlightened.client.render.KingBumiRenderer::new);
        event.registerEntityRenderer(ModEntities.ANOMALY.get(), com.kolodkingal.enlightened.client.render.AnomalyRenderer::new);
        event.registerEntityRenderer(ModEntities.SUN_WARRIOR.get(), com.kolodkingal.enlightened.client.render.SunWarriorRenderer::new);
        event.registerEntityRenderer(ModEntities.DRAGON.get(), com.kolodkingal.enlightened.client.render.DragonRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_SLAB.get(), com.kolodkingal.enlightened.client.render.FlyingSlabRenderer::new);
        event.registerEntityRenderer(ModEntities.GREAT_SPIRIT.get(), com.kolodkingal.enlightened.client.render.GreatSpiritRenderer::new);
        event.registerEntityRenderer(ModEntities.DARK_SPIRIT.get(), com.kolodkingal.enlightened.client.render.DarkSpiritRenderer::new);
        event.registerBlockEntityRenderer(com.kolodkingal.enlightened.registry.ModBlocks.ANOMALY_CUBE.get(),
                com.kolodkingal.enlightened.client.render.AnomalyCubeRenderer::new);
    }

    private static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.LemurRenderer.LAYER,
                com.kolodkingal.enlightened.client.render.LemurModel::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.SkyBisonRenderer.LAYER,
                com.kolodkingal.enlightened.client.render.SkyBisonModel::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.LionTurtleRenderer.LAYER,
                com.kolodkingal.enlightened.client.render.LionTurtleModel::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.DarkSpiritRenderer.LAYER,
                com.kolodkingal.enlightened.client.render.DarkSpiritRenderer::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.DragonRenderer.LAYER,
                com.kolodkingal.enlightened.client.render.DragonRenderer::createBodyLayer);
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.CityGuardRenderer.HELMET,
                com.kolodkingal.enlightened.client.render.CityGuardRenderer::createHelmetLayer);
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.BrigadeLayer.WIDE,
                () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.04F), false), 64, 64));
        event.registerLayerDefinition(com.kolodkingal.enlightened.client.render.BrigadeLayer.SLIM,
                () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.04F), true), 64, 64));
        event.registerLayerDefinition(SHELL_WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.6F), false), 64, 64));
        event.registerLayerDefinition(SHELL_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.6F), true), 64, 64));
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                renderer.addLayer(new ShellLayer(renderer, event.getEntityModels().bakeLayer(slim ? SHELL_SLIM : SHELL_WIDE), slim));
                renderer.addLayer(new com.kolodkingal.enlightened.client.render.BrigadeLayer(renderer, event.getEntityModels().bakeLayer(slim
                        ? com.kolodkingal.enlightened.client.render.BrigadeLayer.SLIM : com.kolodkingal.enlightened.client.render.BrigadeLayer.WIDE), slim));
                renderer.addLayer(new AvatarGlowLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enlightened.client.render.ChakraAuraLayer(renderer));
                renderer.addLayer(new HandTrackerLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enlightened.client.render.BackItemLayer(renderer));
                renderer.addLayer(new com.kolodkingal.enlightened.client.render.LemurShoulderLayer(renderer,
                        new com.kolodkingal.enlightened.client.render.LemurModel(event.getEntityModels().bakeLayer(com.kolodkingal.enlightened.client.render.LemurRenderer.LAYER))));
            }
        }
    }

    //#if <1.21.4
    private static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 1 ? 0xFF000000 | GliderItem.color(stack) : 0xFFFFFFFF, ModItems.AIR_GLIDER.get());
    }
    //#endif
}
