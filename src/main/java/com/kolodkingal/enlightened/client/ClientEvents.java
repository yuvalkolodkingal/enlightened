package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.client.render.AirShieldRenderer;
import com.kolodkingal.enlightened.network.AbilityKeyPayload;
import com.kolodkingal.enlightened.network.ChangeLookPayload;
import com.kolodkingal.enlightened.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** The client's key handling (ability keys with their Sneak, the look, the scroll) and the world effects. */
@EventBusSubscriber(modid = Enlightened.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Whether a "down" was sent for each ability key, so its "up" is sent once and never twice. */
    private static final boolean[] SENT_DOWN = new boolean[BendingData.KEYS + 1];
    /**
     * The Sneak held at the moment each key was pressed (0: not pressed this tick, 1: without Sneak, 2: with), read on
     * the key event itself: by the time the tick handles the click the player may have let go of Sneak.
     */
    private static final byte[] PRESS_SNEAK = new byte[BendingData.KEYS + 1];

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == InputConstants.PRESS) {
            remember(InputConstants.getKey(event.getKey(), event.getScanCode()));
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Post event) {
        if (event.getAction() == InputConstants.PRESS) {
            remember(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    private static void remember(InputConstants.Key input) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 1; i <= BendingData.KEYS; i++) {
            if (PRESS_SNEAK[i] == 0 && KeyBindings.ability(i).isActiveAndMatches(input)) {
                PRESS_SNEAK[i] = (byte) (mc.options.keyShift.isDown() ? 2 : 1);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        handleKeys(mc, player);
    }

    private static void handleKeys(Minecraft mc, LocalPlayer player) {
        BendingData data = Benders.get(player);
        boolean bends = data.element().bends();
        boolean lookClicked = false;
        while (KeyBindings.CHANGE_LOOK.consumeClick()) {
            lookClicked = true;
        }
        if (lookClicked && bends && mc.screen == null && !player.isSpectator()) {
            ModNetwork.sendToServer(new ChangeLookPayload(player.isShiftKeyDown() ? -1 : 1));
        }
        boolean scrollClicked = false;
        while (KeyBindings.SCROLL.consumeClick()) {
            scrollClicked = true;
        }
        if (scrollClicked && bends && mc.screen == null) {
            ClientScroll.open();
        }
        for (int i = 1; i <= BendingData.KEYS; i++) {
            KeyMapping key = KeyBindings.ability(i);
            boolean clicked = false;
            while (key.consumeClick()) {
                clicked = true;
            }
            boolean usable = bends && mc.screen == null && (!player.isSpectator() || data.active(com.kolodkingal.enlightened.bending.Ability.SPIRIT_PROJECTION));
            boolean down = usable && key.isDown();
            boolean sneak = PRESS_SNEAK[i] != 0 ? PRESS_SNEAK[i] == 2 : mc.options.keyShift.isDown();
            PRESS_SNEAK[i] = 0;
            if (usable && clicked && !SENT_DOWN[i]) {
                ModNetwork.sendToServer(new AbilityKeyPayload(i, true, sneak));
                SENT_DOWN[i] = true;
            }
            if (!down && SENT_DOWN[i]) {
                ModNetwork.sendToServer(new AbilityKeyPayload(i, false, mc.options.keyShift.isDown()));
                SENT_DOWN[i] = false;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            AirShieldRenderer.render(event);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SeismicGlow.clear();
        java.util.Arrays.fill(SENT_DOWN, false);
    }

    private ClientEvents() {
    }
}
