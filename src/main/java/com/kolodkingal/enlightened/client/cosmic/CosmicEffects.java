package com.kolodkingal.enlightened.client.cosmic;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** The cosmic plane's sky: night for ever (so every star shows), a deep purple haze, no clouds, no ground. */
public class CosmicEffects extends DimensionSpecialEffects {
    public CosmicEffects() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.13, 0.05, 0.24);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }
}
