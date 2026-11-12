package com.kolodkingal.enlightened.client.particle;

import com.kolodkingal.enlightened.particle.EnergyParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/**
 * A glowing mote that fades and shrinks. Always full-bright, so it glows in the dark.
 * 1.21.11: a {@link SingleQuadParticle} (1.21.9 replaced the texture sheet particles), the same numbers.
 */
public class EnergyParticle extends SingleQuadParticle {
    private final float baseSize;

    protected EnergyParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                             EnergyParticleOptions options, SpriteSet sprites, RandomSource rand) {
        super(level, x, y, z, sprites.get(rand));
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        int color = options.color() == com.kolodkingal.enlightened.bending.Variants.RAINBOW_MARK
                ? com.kolodkingal.enlightened.bending.Variants.rainbow(level.random.nextFloat()) : options.color();
        float shade = 0.85F + random.nextFloat() * 0.15F;
        this.rCol = ((color >> 16) & 0xFF) / 255F * shade;
        this.gCol = ((color >> 8) & 0xFF) / 255F * shade;
        this.bCol = (color & 0xFF) / 255F * shade;
        this.baseSize = 0.11F * options.scale() * (0.75F + random.nextFloat() * 0.5F);
        this.quadSize = baseSize;
        this.lifetime = 10 + random.nextInt(12);
        this.friction = 0.9F;
        this.gravity = 0F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        this.alpha = 1F - life * life;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (age + partialTick) / lifetime;
        return baseSize * (1F - life * 0.6F);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<EnergyParticleOptions> {
        @Override
        public Particle createParticle(EnergyParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return new EnergyParticle(level, x, y, z, xd, yd, zd, options, sprites, random);
        }
    }
}
