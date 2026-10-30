package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.registry.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * The Avatar's element of birth (see {@link AvatarCycle}): every Avatar is born to one of the four, and is strongest
 * in it. He begins with all its first-circle arts learned (not its sub-bendings), they work a level higher (to a
 * master's), cool down faster, and its kind of harm strikes harder.
 */
public final class Avatar {
    /** The factor on the damage of the Avatar's element of birth. */
    public static final float BORN_DAMAGE = 1.25F;
    /** The factor on the cooldowns of its arts. */
    public static final float BORN_COOLDOWN = 0.8F;

    /** The element a kind of bending harm belongs to (NONE for bending in general). */
    public static Element elementOf(ResourceKey<DamageType> type) {
        if (type == ModDamageTypes.FIRE || type == ModDamageTypes.FLAME || type == ModDamageTypes.LIGHTNING) {
            return Element.FIRE;
        }
        if (type == ModDamageTypes.WATER || type == ModDamageTypes.ICE || type == ModDamageTypes.BLOOD) {
            return Element.WATER;
        }
        if (type == ModDamageTypes.EARTH || type == ModDamageTypes.LAVA) {
            return Element.EARTH;
        }
        if (type == ModDamageTypes.AIR || type == ModDamageTypes.AIR_BURST || type == ModDamageTypes.SUFFOCATE) {
            return Element.AIR;
        }
        return Element.NONE;
    }

    /** Gives the Avatar the element he was born to, and all its first-circle arts (at least level 1). */
    public static void bear(BendingData data, Element base) {
        data.setAvatarBase(base);
        for (Ability a : Ability.values()) {
            if (a.element == base && a.gate == Gate.NONE && !a.gift() && data.level(a) < 1) {
                data.setLevel(a, 1);
            }
        }
    }

    private Avatar() {
    }
}
