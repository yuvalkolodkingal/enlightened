package com.kolodkingal.enlightened.client;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Variant;
import com.kolodkingal.enlightened.bending.Variants;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;

import java.util.HashMap;
import java.util.Map;

/**
 * The looks' skins. A bender always wears the skin of his current look (unless the look is "your own skin"). Hooked
 * into {@link AbstractClientPlayer#getSkin()} by a mixin, so it applies everywhere the skin is drawn: the body, the
 * first-person arms, the inventory preview.
 */
public final class SuitSkins {
    private static final Map<Variant, PlayerSkin> CACHE = new HashMap<>();

    public static PlayerSkin skinFor(AbstractClientPlayer player, PlayerSkin own) {
        BendingData data = Benders.get(player);
        if (!data.element().bends()) {
            return own;
        }
        Variant look = Variants.current(data);
        if (look.ownSkin()) {
            return own;
        }
        return CACHE.computeIfAbsent(look, v -> new PlayerSkin(v.skin(), null, null, null,
                v.slim() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, true));
    }

    private SuitSkins() {
    }
}
