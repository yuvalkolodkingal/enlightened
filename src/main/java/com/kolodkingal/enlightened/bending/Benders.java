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

    /** What a player may be born to on the quest path: no bending, or one of the four (one in five each). */
    public static final Element[] BIRTHS = {Element.NONE, Element.FIRE, Element.AIR, Element.WATER, Element.EARTH};

    /** What a newborn is: an element, and (for the Avatar) whether the Dark Avatar. */
    public record Birth(Element element, boolean dark) {
    }

    /**
     * The fate of a newborn player. On the quest path one in five each: no bending, or one of the four (the Avatar is
     * Raava's to choose). On the fate path one in seven each: no bending, one of the four, the Avatar or the Dark
     * Avatar; an Avatar the world has no room for ({@code avatarsFull}, {@code darkFull}) is one of the four instead.
     */
    public static Birth birth(net.minecraft.util.RandomSource random, boolean quest, boolean avatarsFull, boolean darkFull) {
        if (quest) {
            return new Birth(BIRTHS[random.nextInt(BIRTHS.length)], false);
        }
        int roll = random.nextInt(7);
        if (roll < 5) {
            return new Birth(BIRTHS[roll], false);
        }
        boolean dark = roll == 6;
        if (dark ? darkFull : avatarsFull) {
            return new Birth(AvatarCycle.CYCLE.get(random.nextInt(AvatarCycle.CYCLE.size())), false);
        }
        return new Birth(Element.AVATAR, dark);
    }

    /**
     * The first time a player joins: his fate is cast, once (it is remembered through deaths). If he was born a bender
     * he awakens to it at once; if not, he is told so. A player who already bends keeps his element.
     */
    public static void castFate(ServerPlayer player) {
        if (player.getClass() != ServerPlayer.class) {
            // Fake players (machines, the GameTests' mock players) are not born.
            return;
        }
        net.minecraft.nbt.CompoundTag kept = player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if (kept.getBoolean("enlightened_fated")) {
            return;
        }
        kept.putBoolean("enlightened_fated", true);
        player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, kept);
        boolean on = !com.kolodkingal.enlightened.EnlightenedConfig.SPEC.isLoaded() || com.kolodkingal.enlightened.EnlightenedConfig.BORN_BENDING.get();
        if (!on || get(player).element() != Element.NONE) {
            return;
        }
        AvatarCycle cycle = AvatarCycle.get(player.getServer());
        Birth birth = birth(player.getRandom(), com.kolodkingal.enlightened.quest.AvatarQuest.questPath(), cycle.full(), cycle.hasDark());
        Element born = birth.element();
        if (born == Element.AVATAR && birth.dark()) {
            com.kolodkingal.enlightened.quest.AvatarQuest.becomeDark(player);
        } else if (born == Element.NONE) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.enlightened.born_none")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else if (born == Element.AVATAR) {
            AvatarCycle.becomeAvatar(player.getServer(), player);
        } else {
            awaken(player, born);
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
        // The Avatar Cycle keeps count of the Avatars and the element each was born to.
        if (player.getServer() != null && player.getClass() == ServerPlayer.class) {
            AvatarCycle cycle = AvatarCycle.get(player.getServer());
            if (element == Element.AVATAR) {
                Avatar.bear(data, cycle.register(player));
            } else if (old == Element.AVATAR) {
                cycle.unregister(player.getUUID());
                data.setDarkAvatar(false);
            }
        }
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
