package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.client.anim.Animator;
import com.kolodkingal.enlightened.client.anim.BodyLean;
import com.kolodkingal.enlightened.client.fx.WorldFx;
import com.kolodkingal.enlightened.client.fx.WorldFxRenderer;
import com.kolodkingal.enlightened.network.AbilityKeyPayload;
import com.kolodkingal.enlightened.network.ChangeLookPayload;
import com.kolodkingal.enlightened.network.MeditatePayload;
import com.kolodkingal.enlightened.network.ModNetwork;
import com.kolodkingal.enlightened.network.StepPayload;
import com.kolodkingal.enlightened.bending.Steps;
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
    private static boolean meditateSent;
    /** The last tick each movement key went down (forward, back, left, right, jump), for the double tap of a step. */
    private static final long[] TAPPED = new long[5];
    private static final boolean[] WAS_DOWN = new boolean[5];
    private static long stepTick = Long.MIN_VALUE;

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

    /** The local player burning blue sees blue flames over the screen instead of orange. */
    /** The back slot is drawn as a slot (the inventory's own texture has none there). */
    @SubscribeEvent
    public static void onContainerBackground(net.neoforged.neoforge.client.event.ContainerScreenEvent.Render.Background event) {
        var screen = event.getContainerScreen();
        for (net.minecraft.world.inventory.Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof com.kolodkingal.enlightened.item.BackSlot.Back && slot.isActive()) {
                int x = screen.getGuiLeft() + slot.x - 1;
                int y = screen.getGuiTop() + slot.y - 1;
                var g = event.getGuiGraphics();
                g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
                g.fill(x, y, x + 17, y + 1, 0xFF373737);
                g.fill(x, y, x + 1, y + 17, 0xFF373737);
                g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
                g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
            }
        }
    }

    /** The empty back slot names itself. */
    @SubscribeEvent
    public static void onScreenRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen
                && screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().container instanceof com.kolodkingal.enlightened.item.BackSlot.Back
                && !screen.getSlotUnderMouse().hasItem() && screen.getMenu().getCarried().isEmpty()) {
            event.getGuiGraphics().renderTooltip(net.minecraft.client.Minecraft.getInstance().font,
                    net.minecraft.network.chat.Component.translatable("slot.enlightened.back"), event.getMouseX(), event.getMouseY());
        }
    }

    /** The brigade trim's template says which material makes which brigade, and where it is found. */
    @SubscribeEvent
    public static void onTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (event.getItemStack().is(com.kolodkingal.enlightened.registry.ModItems.BRIGADE_TRIM_TEMPLATE.get())) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("item.enlightened.brigade_armor_trim_smithing_template.desc")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public static void onScreenFire(net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() != net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent.OverlayType.FIRE
                || !com.kolodkingal.enlightened.client.fx.BlueBurning.isBlue(event.getPlayer())) {
            return;
        }
        event.setCanceled(true);
        com.kolodkingal.enlightened.client.fx.BlueScreenFire.render(event.getPoseStack());
    }

    /** The Anomaly's dread shakes the world in his eyes. */
    @SubscribeEvent
    public static void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        float s = DreadOverlay.strength(Minecraft.getInstance());
        if (s <= 0.0F) {
            return;
        }
        double t = event.getPartialTick() + (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime());
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 1.9) * 2.5 * s));
        event.setYaw(event.getYaw() + (float) (Math.sin(t * 3.7) * 0.8 * s));
        event.setPitch(event.getPitch() + (float) (Math.cos(t * 4.3) * 0.6 * s));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Under the Anomaly's dread, a heart pounds, faster the fresher it is.
        float dread = DreadOverlay.strength(mc);
        if (dread > 0.0F && player.tickCount % (dread > 0.8F ? 12 : 18) == 0) {
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, 0.8F, 0.4F + 0.6F * dread));
        }
        handleKeys(mc, player);
        handleMeditate(mc, player);
        handleSteps(mc, player);
        BodyLean.tick(mc.level);
        WorldFx.tick(mc.level);
        if (mc.player != null) {
            com.kolodkingal.enlightened.bending.BendingData data = com.kolodkingal.enlightened.bending.Benders.get(mc.player);
            if (data.has(com.kolodkingal.enlightened.bending.Ability.AIR_SCOOTER) && data.on(com.kolodkingal.enlightened.bending.Ability.AIR_SCOOTER)) {
                com.kolodkingal.enlightened.air.AirBending.skimWater(mc.player);
            }
            driveSpout(mc.player, data);
        }
    }

    /** The spouts are ridden on this client: up the column and where the movement keys point, fast. */
    private static void driveSpout(LocalPlayer player, BendingData data) {
        com.kolodkingal.enlightened.bending.Ability spout = data.has(com.kolodkingal.enlightened.bending.Ability.WATER_SPOUT) && data.on(com.kolodkingal.enlightened.bending.Ability.WATER_SPOUT)
                ? com.kolodkingal.enlightened.bending.Ability.WATER_SPOUT
                : data.has(com.kolodkingal.enlightened.bending.Ability.AIR_SPOUT) && data.on(com.kolodkingal.enlightened.bending.Ability.AIR_SPOUT)
                ? com.kolodkingal.enlightened.bending.Ability.AIR_SPOUT : null;
        if (spout == null || player.getAbilities().flying || player.isPassenger()) {
            return;
        }
        boolean water = spout == com.kolodkingal.enlightened.bending.Ability.WATER_SPOUT;
        int lvl = data.level(spout);
        int height = com.kolodkingal.enlightened.bending.Spouts.height(water, lvl);
        double base = com.kolodkingal.enlightened.bending.Spouts.base(player.level(), player, height + 2, water);
        if (Double.isNaN(base)) {
            return;
        }
        com.kolodkingal.enlightened.bending.Spouts.drive(player, player.input.forwardImpulse, player.input.leftImpulse, base, height,
                com.kolodkingal.enlightened.bending.Spouts.speed(lvl));
    }

    private static void handleKeys(Minecraft mc, LocalPlayer player) {
        BendingData data = Benders.get(player);
        boolean bends = data.canBend();
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
                com.kolodkingal.enlightened.bending.Ability pressed = data.slot(BendingData.slotIndex(i, sneak));
                if (pressed != null && data.has(pressed)) {
                    Animator.predict(player, pressed);
                }
            }
            if (!down && SENT_DOWN[i]) {
                ModNetwork.sendToServer(new AbilityKeyPayload(i, false, mc.options.keyShift.isDown()));
                SENT_DOWN[i] = false;
            }
        }
    }

    /** The Meditate key: down and up go to the server (it decides whether he may). */
    private static void handleMeditate(Minecraft mc, LocalPlayer player) {
        BendingData data = Benders.get(player);
        boolean down = data.element().bends() && mc.screen == null && KeyBindings.MEDITATE.isDown() && !player.isSpectator();
        if (down != meditateSent) {
            meditateSent = down;
            ModNetwork.sendToServer(new MeditatePayload(down));
        }
    }

    /** A double tap of a direction (or of jump) within seven ticks: the element step. */
    private static void handleSteps(Minecraft mc, LocalPlayer player) {
        BendingData data = Benders.get(player);
        if (!data.element().bends() || mc.screen != null || Steps.stepOf(data.element()) == null || !data.has(Steps.stepOf(data.element()))) {
            return;
        }
        KeyMapping[] keys = {mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump};
        long now = mc.level.getGameTime();
        for (int i = 0; i < keys.length; i++) {
            boolean isDown = keys[i].isDown();
            if (isDown && !WAS_DOWN[i]) {
                if (now - TAPPED[i] <= 7 && now - stepTick > 10) {
                    stepTick = now;
                    TAPPED[i] = Long.MIN_VALUE / 2;
                    float yaw = (float) Math.toRadians(player.getYRot());
                    double fx = -Math.sin(yaw);
                    double fz = Math.cos(yaw);
                    double rx = -fz;
                    double rz = fx;
                    double[] dir = switch (i) {
                        case 0 -> new double[]{fx, 0, fz};
                        case 1 -> new double[]{-fx, 0, -fz};
                        case 2 -> new double[]{-rx, 0, -rz};
                        case 3 -> new double[]{rx, 0, rz};
                        default -> new double[]{0, 1, 0};
                    };
                    ModNetwork.sendToServer(new StepPayload((float) dir[0], (float) dir[1], (float) dir[2]));
                    com.kolodkingal.enlightened.client.anim.Animator.playLocal(player, com.kolodkingal.enlightened.client.anim.Gesture.STEP);
                } else {
                    TAPPED[i] = now;
                }
            }
            WAS_DOWN[i] = isDown;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            WorldFxRenderer.render(event);
            com.kolodkingal.enlightened.client.cosmic.CosmicRenderer.render(event);
            com.kolodkingal.enlightened.client.render.AnomalyRenderer.drawAll(event);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SeismicGlow.clear();
        BodyLean.clear();
        WorldFx.clear();
        com.kolodkingal.enlightened.client.fx.BlueBurning.clear();
        java.util.Arrays.fill(SENT_DOWN, false);
    }

    private ClientEvents() {
    }
}
