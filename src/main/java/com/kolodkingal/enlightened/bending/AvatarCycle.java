package com.kolodkingal.enlightened.bending;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Avatar Cycle of a world: who the Avatars are (by default there is one at a time; an operator changes it with
 * {@code /enlightened avatars max}), and the element each was born to, which goes round the cycle of the show: Fire,
 * Air, Water, Earth, each new Avatar born to the element after the last one's. When an Avatar is banned, or dies in a
 * hardcore world, the Avatar is reborn in another player, at random among those online (or the next to join).
 * A banned Avatar who comes back is a bender of the element he was born to, no more.
 */
public final class AvatarCycle extends SavedData {
    /** The cycle of the show: after a firebender Avatar an airbender, a waterbender, an earthbender, and round again. */
    public static final List<Element> CYCLE = List.of(Element.FIRE, Element.AIR, Element.WATER, Element.EARTH);
    private static final String NAME = "enlightened_avatar_cycle";

    private int max = 1;
    /** The Avatars by player, with the element each was born to. */
    private final Map<UUID, Element> avatars = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();
    /** Former Avatars who lost the Avatar while away (banned, or the cycle moved on), and the element to give them back. */
    private final Map<UUID, Element> revoked = new LinkedHashMap<>();
    /** The element the next Avatar is born to; null before the first. */
    private Element next;
    /** Avatars owed to the world: lost while nobody else was online, given to the next player who comes. */
    private int owed;

    public static AvatarCycle get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AvatarCycle::new, AvatarCycle::load, null), NAME);
    }

    public int max() {
        return max;
    }

    public void setMax(int max) {
        this.max = Math.max(0, max);
        setDirty();
    }

    public boolean full() {
        return avatars.size() >= max;
    }

    public Map<UUID, Element> avatars() {
        return avatars;
    }

    public String name(UUID id) {
        return names.getOrDefault(id, id.toString());
    }

    public Element base(UUID id) {
        return avatars.get(id);
    }

    public boolean isAvatar(UUID id) {
        return avatars.containsKey(id);
    }

    public int owed() {
        return owed;
    }

    /** The element the next Avatar will be born to: the one after the last Avatar's, or a random one for the first. */
    public Element peekNext(RandomSource random) {
        if (next == null) {
            next = CYCLE.get(random.nextInt(CYCLE.size()));
            setDirty();
        }
        return next;
    }

    /** Records a new Avatar, born to the next element of the cycle; the cycle moves on. Returns that element. */
    public Element register(ServerPlayer player) {
        Element known = avatars.get(player.getUUID());
        if (known != null) {
            return known;
        }
        Element base = peekNext(player.getRandom());
        avatars.put(player.getUUID(), base);
        names.put(player.getUUID(), player.getGameProfile().getName());
        next = CYCLE.get((CYCLE.indexOf(base) + 1) % CYCLE.size());
        setDirty();
        return base;
    }

    public void unregister(UUID id) {
        if (avatars.remove(id) != null) {
            names.remove(id);
            setDirty();
        }
    }

    // ---- the cycle turning ----

    /** The Avatar is lost by {@code former} (banned, or dead for good); he is reborn in another player. */
    public void passOn(MinecraftServer server, UUID former, boolean revoke) {
        Element base = avatars.get(former);
        if (base == null) {
            return;
        }
        unregister(former);
        if (revoke) {
            revoked.put(former, base);
        }
        ServerPlayer formerOnline = server.getPlayerList().getPlayer(former);
        if (formerOnline != null && revoke) {
            Benders.setElement(formerOnline, base);
            revoked.remove(former);
        }
        if (!rebirth(server, former)) {
            owed++;
        }
        setDirty();
    }

    /** Makes a random online player (not an Avatar, not {@code except}) the Avatar. Returns false if there is none. */
    public boolean rebirth(MinecraftServer server, UUID except) {
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.getUUID().equals(except) && !isAvatar(p.getUUID()) && !p.isSpectator() && p.getClass() == ServerPlayer.class
                    && !server.getPlayerList().getBans().isBanned(p.getGameProfile())) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        ServerPlayer chosen = candidates.get(server.overworld().getRandom().nextInt(candidates.size()));
        becomeAvatar(server, chosen);
        return true;
    }

    /** Makes a player the Avatar (born to the next element of the cycle), and tells the world. */
    public static void becomeAvatar(MinecraftServer server, ServerPlayer player) {
        Benders.awaken(player, Element.AVATAR);
        Element base = get(server).base(player.getUUID());
        if (base != null) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("message.enlightened.avatar.reborn",
                    player.getDisplayName(), base.displayName()).withStyle(ChatFormatting.AQUA), false);
        }
    }

    /** A player joins: a revoked Avatar is given back his first element; an owed Avatar may be born in him. */
    public void onLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        Element back = revoked.remove(player.getUUID());
        if (back != null) {
            setDirty();
            if (Benders.get(player).element() == Element.AVATAR) {
                Benders.setElement(player, back);
                player.sendSystemMessage(Component.translatable("message.enlightened.avatar.lost", back.displayName()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (server != null && owed > 0 && !isAvatar(player.getUUID()) && player.getClass() == ServerPlayer.class && !full()) {
            owed--;
            setDirty();
            becomeAvatar(server, player);
        }
    }

    /** Every few seconds: a banned Avatar passes the Avatar on. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        AvatarCycle cycle = get(server);
        if (cycle.avatars.isEmpty()) {
            return;
        }
        for (UUID id : new ArrayList<>(cycle.avatars.keySet())) {
            var profile = server.getProfileCache() == null ? null : server.getProfileCache().get(id).orElse(null);
            if (profile != null && server.getPlayerList().getBans().isBanned(profile)) {
                cycle.passOn(server, id, true);
            }
        }
    }

    // ---- saving ----

    private static AvatarCycle load(CompoundTag tag, HolderLookup.Provider provider) {
        AvatarCycle c = new AvatarCycle();
        c.max = tag.contains("Max") ? tag.getInt("Max") : 1;
        c.owed = tag.getInt("Owed");
        c.next = tag.contains("Next") ? Element.byName(tag.getString("Next")) : null;
        for (Tag t : tag.getList("Avatars", Tag.TAG_COMPOUND)) {
            CompoundTag a = (CompoundTag) t;
            UUID id = a.getUUID("Id");
            c.avatars.put(id, Element.byName(a.getString("Base")));
            c.names.put(id, a.getString("Name"));
        }
        for (Tag t : tag.getList("Revoked", Tag.TAG_COMPOUND)) {
            CompoundTag a = (CompoundTag) t;
            c.revoked.put(a.getUUID("Id"), Element.byName(a.getString("Base")));
        }
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("Max", max);
        tag.putInt("Owed", owed);
        if (next != null) {
            tag.putString("Next", next.getSerializedName());
        }
        ListTag list = new ListTag();
        avatars.forEach((id, base) -> {
            CompoundTag a = new CompoundTag();
            a.putUUID("Id", id);
            a.putString("Base", base.getSerializedName());
            a.putString("Name", names.getOrDefault(id, ""));
            list.add(a);
        });
        tag.put("Avatars", list);
        ListTag rev = new ListTag();
        revoked.forEach((id, base) -> {
            CompoundTag a = new CompoundTag();
            a.putUUID("Id", id);
            a.putString("Base", base.getSerializedName());
            rev.add(a);
        });
        tag.put("Revoked", rev);
        return tag;
    }

    /** All the Avatars' ids, for the command. */
    public Set<UUID> ids() {
        return new HashSet<>(avatars.keySet());
    }
}
