package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.particle.EnergyParticleOptions;
import com.kolodkingal.enlightened.registry.ModAttachments;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Helpers for reading and changing a player's bending. */
public final class Benders {

    public static BendingData get(Player player) {
        return player.getData(ModAttachments.BENDING);
    }

    public static Element element(Player player) {
        return get(player).element();
    }

    public static boolean is(Player player, Element element) {
        return element(player).commands(element);
    }

    /** Pushes the player's bending data to every client that can see them, if anything changed. */
    public static void syncIfDirty(ServerPlayer player) {
        BendingData data = get(player);
        if (data.isDirty()) {
            data.clearDirty();
            player.syncData(ModAttachments.BENDING);
        }
    }

    /**
     * Gives (or removes, with {@link Element#NONE}) an element. Cleans up whatever the old element was doing, and
     * gives the new element's starter ability and starting points and loadout.
     */
    public static void setElement(ServerPlayer player, Element element) {
        BendingData data = get(player);
        Element old = data.element();
        if (old == element) {
            return;
        }
        BendingTicker.endAll(player, data);
        data.setElementRaw(element);
        if (element.bends()) {
            Ability starter = Ability.starterOf(element);
            if (starter != null) {
                data.setLevel(starter, 1);
            }
            if (element == Element.AVATAR) {
                // The Avatar begins with the first art of each of the four.
                for (Element e : Element.FOUR) {
                    Ability first = Ability.starterOf(e);
                    if (first != null) {
                        data.setLevel(first, 1);
                    }
                }
            }
            data.setPoints(Mastery.startingPoints());
            Loadout.reset(data);
        }
        BendingTicker.applyPassives(player, data);
        syncIfDirty(player);
    }

    /**
     * The dramatic moment a player becomes a bender: a title on screen, a burst of the element's energy, a sound
     * everyone nearby hears and a message to the whole server.
     */
    public static void awaken(ServerPlayer player, Element element) {
        awaken(player, element, "", true);
    }

    /**
     * {@link #awaken(ServerPlayer, Element)} straight into a look; {@code ownWay} false when the element was given by
     * a command rather than found: the server is then told plainly, not the story of the scroll.
     */
    public static void awaken(ServerPlayer player, Element element, String variant, boolean ownWay) {
        setElement(player, element);
        setVariant(player, Variants.get(element, variant));
        ServerLevel level = player.serverLevel();

        Component title = element.awakeningTitle().copy().withStyle(s -> s.withColor(element.color).withBold(true));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("element.enlightened." + element.getSerializedName() + ".awaken").withStyle(ChatFormatting.GRAY)));

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        Variant look = variant(player);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), look.color(), 120, 1.6);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), look.glow(), 60, 0.8);

        String key = "element.enlightened." + element.getSerializedName() + (ownWay ? ".broadcast" : ".chosen");
        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable(key, player.getDisplayName()).withStyle(s -> s.withColor(element.color)), false);
        player.displayClientMessage(Component.translatable("message.enlightened.scroll_hint").withStyle(ChatFormatting.YELLOW), false);
    }

    public static Variant variant(Player player) {
        return Variants.current(get(player));
    }

    public static int color(Player player) {
        return variant(player).color();
    }

    public static int glow(Player player) {
        return variant(player).glow();
    }

    public static void setVariant(ServerPlayer player, Variant variant) {
        BendingData data = get(player);
        if (variant.element() != data.element()) {
            return;
        }
        data.setVariantRaw(variant.id());
        syncIfDirty(player);
    }

    /** The Change Look key: steps to the next (or previous) look. Returns false if the element has no other looks. */
    public static boolean cycleVariant(ServerPlayer player, int step) {
        BendingData data = get(player);
        if (!Variants.hasChoice(data.element())) {
            return false;
        }
        Variant next = Variants.step(data.element(), data.variant(), step);
        setVariant(player, next);
        ServerLevel level = player.serverLevel();
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), next.color(), 40, 0.5);
        burst(level, player.getX(), player.getY() + 1.0, player.getZ(), next.glow(), 20, 0.3);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AWAKEN.get(), SoundSource.PLAYERS, 0.35F, 1.6F);
        player.displayClientMessage(Component.translatable("message.enlightened.look", next.displayName())
                .withStyle(s -> s.withColor(next.color())), true);
        return true;
    }

    /** A sphere of energy particles, sent to everyone nearby. */
    public static void burst(ServerLevel level, double x, double y, double z, int color, int count, double spread) {
        level.sendParticles(new EnergyParticleOptions(color, 1.6F), x, y, z, count, spread, spread, spread, 0.15);
    }

    private Benders() {
    }
}
