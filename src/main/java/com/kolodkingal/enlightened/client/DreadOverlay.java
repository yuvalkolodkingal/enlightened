package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.registry.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * The Anomaly's dread on the screen: the sight flashes between blood red and black, unevenly, hardest when the
 * lightning has just struck, the edges closing in dark; it fades as the dread wears off.
 */
public class DreadOverlay implements LayeredDraw.Layer {
    private static final int DURATION = com.kolodkingal.enlightened.effect.DreadEffect.DURATION;

    public static float strength(Minecraft mc) {
        if (mc.player == null) {
            return 0.0F;
        }
        MobEffectInstance dread = mc.player.getEffect(ModEffects.DREAD);
        if (dread == null) {
            return 0.0F;
        }
        int left = dread.getDuration();
        // Strongest just after the strike, fading over the last second and a half.
        float fresh = Mth.clamp((left - (DURATION - 25)) / 25.0F, 0.0F, 1.0F);
        float fade = Mth.clamp(left / 30.0F, 0.0F, 1.0F);
        return fade * (0.55F + 0.45F * fresh);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        float s = strength(mc);
        if (s <= 0.0F || mc.level == null) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        float t = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
        // Red and black in turns, the beat uneven: two quick flashes, then a longer dark.
        int beat = (int) t % 11;
        boolean red = beat == 0 || beat == 1 || beat == 4 || (beat == 7 && ((int) t / 11) % 3 == 0);
        float jitter = 0.85F + 0.15F * Mth.sin(t * 2.3F);
        int alpha = (int) (255 * s * jitter * (red ? 0.5F : 0.62F));
        int colour = red ? 0x9A0000 : 0x000000;
        g.fill(0, 0, w, h, (alpha << 24) | colour);
        // The edges close in, dark red to black.
        int edge = (int) (Math.min(w, h) * (0.18F + 0.1F * s));
        int dark = (int) (230 * s);
        g.fillGradient(0, 0, w, edge, (dark << 24) | 0x100000, 0);
        g.fillGradient(0, h - edge, w, h, 0, (dark << 24) | 0x100000);
        for (int i = 0; i < edge; i += 2) {
            int a = (int) (dark * (1.0F - (float) i / edge));
            g.fill(i, 0, i + 2, h, (a << 24) | 0x100000);
            g.fill(w - i - 2, 0, w - i, h, (a << 24) | 0x100000);
        }
    }
}
