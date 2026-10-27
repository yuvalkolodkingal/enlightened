package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.entity.GuruPathikEntity;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Guru Pathik's lessons. Spoken to, he teaches a bender the way of the seven chakras (after which they open for him
 * by meditation); meditating before him, the bender opens them six times as fast and with no points asked, and as
 * each one opens the guru tells what blocked it: fear, guilt, shame, grief, lies, illusion, attachment.
 */
public final class Guru {
    /** How near the guru a meditation counts as his lesson. */
    public static final double REACH = 8.0;

    public static boolean near(ServerPlayer p) {
        return !p.serverLevel().getEntitiesOfClass(GuruPathikEntity.class, p.getBoundingBox().inflate(REACH), GuruPathikEntity::isAlive).isEmpty();
    }

    /** The bender speaks to the guru. */
    public static void talk(ServerPlayer p, GuruPathikEntity guru) {
        BendingData data = Benders.get(p);
        p.serverLevel().playSound(null, guru.getX(), guru.getY(), guru.getZ(), ModSounds.AWAKEN.get(), SoundSource.NEUTRAL, 0.5F, 1.6F);
        if (!data.element().bends()) {
            say(p, "guru.enlightened.no_bending");
            return;
        }
        if (!data.guruTaught()) {
            data.setGuruTaught(true);
            say(p, "guru.enlightened.welcome");
            say(p, "guru.enlightened.teach");
            say(p, data.element() == Element.AVATAR ? "guru.enlightened.teach_avatar" : "guru.enlightened.teach_bender");
            say(p, "guru.enlightened.sit");
            Benders.syncIfDirty(p);
            return;
        }
        int next = data.chakras() + 1;
        if (next <= Chakras.COUNT) {
            p.sendSystemMessage(Component.translatable("guru.enlightened.next", Chakras.name(next), Component.translatable("guru.enlightened.block." + next))
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        say(p, data.element() == Element.AVATAR ? "guru.enlightened.done_avatar" : "guru.enlightened.done");
    }

    /** The lesson of the chakra that just opened before him. */
    public static void lesson(ServerPlayer p, int chakra) {
        say(p, "guru.enlightened.lesson." + chakra);
    }

    private static void say(ServerPlayer p, String key) {
        p.sendSystemMessage(Component.translatable("guru.enlightened.says", Component.translatable(key)).withStyle(ChatFormatting.GOLD));
    }

    private Guru() {
    }
}
