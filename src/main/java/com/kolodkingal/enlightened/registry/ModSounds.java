package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own sound effects (all original, synthesised: see tools/sounds.py). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Enlightened.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKEN = register("awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEARN = register("learn");
    public static final DeferredHolder<SoundEvent, SoundEvent> POINT = register("point");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCROLL = register("scroll");

    // Earth
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCK_THROW = register("rock_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCK_HIT = register("rock_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> EARTH_RISE = register("earth_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> EARTH_ARMOR = register("earth_armor");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAND_HISS = register("sand_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAVA_FLOW = register("lava_flow");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL_CLANG = register("metal_clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL_WRECK = register("metal_wreck");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEISMIC = register("seismic");

    // Air
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_BLAST = register("air_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_SHIELD = register("air_shield");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_SHIELD_BREAK = register("air_shield_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_BLADE = register("air_blade");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_JUMP = register("air_jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_SCOOTER = register("air_scooter");
    public static final DeferredHolder<SoundEvent, SoundEvent> TORNADO = register("tornado");
    public static final DeferredHolder<SoundEvent, SoundEvent> PROJECTION = register("projection");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUFFOCATE = register("suffocate");

    // Water
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_WHIP = register("water_whip");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_GATHER = register("water_gather");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAL = register("heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> OCTOPUS = register("octopus");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPOUT = register("spout");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_CRACK = register("ice_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_KNIFE = register("ice_knife");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOD_GRIP = register("blood_grip");

    // Fire
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_BLAST = register("fire_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_STREAM = register("fire_stream");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_WHIP = register("fire_whip");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_RING = register("fire_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_CHARGE = register("lightning_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_STRIKE = register("lightning_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTNING_REDIRECT = register("lightning_redirect");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Enlightened.id(name)));
    }

    private ModSounds() {
    }
}
