package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.effect.BloodbentEffect;
import com.kolodkingal.enlightened.effect.FrozenEffect;
import com.kolodkingal.enlightened.effect.QuicksandEffect;
import com.kolodkingal.enlightened.effect.StunnedEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Enlightened.MODID);

    /** Held by a bloodbender: cannot move, jump or bend. */
    public static final DeferredHolder<MobEffect, BloodbentEffect> BLOODBENT = EFFECTS.register("bloodbent", BloodbentEffect::new);
    /** Sinking in quicksand: slow, cannot jump, pulled down. */
    public static final DeferredHolder<MobEffect, QuicksandEffect> QUICKSAND = EFFECTS.register("quicksand", QuicksandEffect::new);
    /** Frozen by a waterbender: slow and freezing. */
    public static final DeferredHolder<MobEffect, FrozenEffect> FROZEN = EFFECTS.register("frozen", FrozenEffect::new);
    public static final DeferredHolder<MobEffect, com.kolodkingal.enlightened.effect.PacifiedEffect> PACIFIED = EFFECTS.register("pacified", com.kolodkingal.enlightened.effect.PacifiedEffect::new);
    /** Struck by lightning: cannot move for a moment. */
    public static final DeferredHolder<MobEffect, StunnedEffect> STUNNED = EFFECTS.register("stunned", StunnedEffect::new);

    /** Struck by the Anomaly's red lightning: the sight flashes red and black, the world shakes, the heart pounds. */
    public static final DeferredHolder<MobEffect, com.kolodkingal.enlightened.effect.DreadEffect> DREAD = EFFECTS.register("dread", com.kolodkingal.enlightened.effect.DreadEffect::new);

    private ModEffects() {
    }
}
