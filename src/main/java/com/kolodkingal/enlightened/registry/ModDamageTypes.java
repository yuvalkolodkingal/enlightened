package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Damage types are data-driven: see data/enlightened/damage_type. All of them are in the tag
 * {@code enlightened:bending} ({@link ModTags#BENDING}), which is how a kill is known to be the bender's own.
 */
public final class ModDamageTypes {
    /** Any bending blow with no type of its own. */
    public static final ResourceKey<DamageType> BENDING = key("bending");
    /** A rock, sand or metal blade thrown by an earthbender, or the quicksand. */
    public static final ResourceKey<DamageType> EARTH = key("earth");
    /** Lava bending: a shuriken or a path. */
    public static final ResourceKey<DamageType> LAVA = key("lava");
    /** An air blast, blade or tornado. */
    public static final ResourceKey<DamageType> AIR = key("air");
    /** The burst of a breaking air shield. */
    public static final ResourceKey<DamageType> AIR_BURST = key("air_burst");
    /** The oxygen a tetherless airbender takes. */
    public static final ResourceKey<DamageType> SUFFOCATE = key("suffocate");
    /** A water whip, the arms, the spout, the tentacles. */
    public static final ResourceKey<DamageType> WATER = key("water");
    /** Ice knives, a freeze, snow. */
    public static final ResourceKey<DamageType> ICE = key("ice");
    /** Bloodbending: the grip and the throw. */
    public static final ResourceKey<DamageType> BLOOD = key("blood");
    /** Fire bending: ball, fist, whip, ring. Burns, and fire protection softens it. */
    public static final ResourceKey<DamageType> FIRE = key("fire");
    /** A continuous flame (no damage cooldown). */
    public static final ResourceKey<DamageType> FLAME = key("flame");
    /** A firebender's lightning. */
    public static final ResourceKey<DamageType> LIGHTNING = key("lightning");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Enlightened.id(name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity cause) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, cause);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity cause) {
        return source(level, type, cause, cause);
    }

    private ModDamageTypes() {
    }
}
