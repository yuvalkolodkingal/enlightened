package com.kolodkingal.enlightened.talk;

import com.kolodkingal.enlightened.network.ModNetwork;
import com.kolodkingal.enlightened.network.TalkPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of a talk (1.0.23). A right-click {@link #begin}s it: the node is picked by {@link Dialogue}'s rules
 * and sent to the player's talk screen ({@link TalkPayload.Open}); the speaker stops and turns to him while they talk
 * ({@link #tick}). The answer he chooses ({@link TalkPayload.Choose}) comes to {@link #choose}: it must be one the last
 * node offered him, from no farther than {@link #DISTANCE}; then its action runs. An empty answer is the screen
 * closing: the talk ends. An action that shows no other node ends the talk too.
 */
public final class Talk {
    /** How far a player may stand from the one he talks to. */
    public static final double DISTANCE = 8.0;

    private record Session(int entityId, Speaker who, String node, List<Dialogue.Option> offered) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static Dialogue.Ctx context(ServerPlayer player, Entity speaker, Speaker who) {
        return new Dialogue.Ctx(player, speaker, who);
    }

    /** The player right-clicked a speaker: opens the talk (false if there is nothing to say, or he is too far). */
    public static boolean begin(ServerPlayer player, Entity speaker, Speaker who) {
        if (player.isSpectator() || !speaker.isAlive() || player.distanceTo(speaker) > DISTANCE + 0.5 + speaker.getBbWidth() / 2) {
            return false;
        }
        Dialogue.Ctx ctx = context(player, speaker, who);
        Dialogue.Node node = Dialogue.pick(ctx);
        if (node == null) {
            return false;
        }
        show(ctx, node);
        return true;
    }

    /** Shows the node {@code key} of the speaker (an option's next node); nothing if there is none. */
    public static void show(Dialogue.Ctx ctx, String key) {
        Dialogue.Node node = Dialogue.find(ctx.who(), key);
        if (node != null) {
            show(ctx, node);
        }
    }

    public static void show(Dialogue.Ctx ctx, Dialogue.Node node) {
        ServerPlayer player = ctx.player();
        Dialogue.Action onShow = Dialogue.onShowOf(ctx.who(), node.key());
        if (onShow != null) {
            onShow.run(ctx);
        }
        Dialogue.Said said = Dialogue.said(ctx, node);
        List<Dialogue.Option> offered = Dialogue.offered(ctx, node);
        List<String> keys = new ArrayList<>();
        for (Dialogue.Option option : offered) {
            keys.add(option.key());
        }
        SESSIONS.put(player.getUUID(), new Session(ctx.speaker().getId(), ctx.who(), node.key(), offered));
        List<String> args = said.args().size() > TalkPayload.MAX_ARGS ? said.args().subList(0, TalkPayload.MAX_ARGS) : said.args();
        ModNetwork.sendTo(player, new TalkPayload.Open(ctx.speaker().getId(), ctx.who().ordinal(), said.line(), List.copyOf(args), List.copyOf(keys)));
        if (ctx.speaker() instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(player);
            mob.playSound(SoundEvents.VILLAGER_AMBIENT, 0.5F, ctx.who() == Speaker.LION_TURTLE ? 0.4F : 0.9F);
        }
    }

    public static void choose(ServerPlayer player, int entityId, String option) {
        tryChoose(player, entityId, option);
    }

    /** Whether the answer was taken (one offered, near enough); the empty answer ends the talk. */
    public static boolean tryChoose(ServerPlayer player, int entityId, String option) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId() != entityId) {
            return false;
        }
        if (option.isEmpty()) {
            end(player);
            return true;
        }
        Entity speaker = player.level().getEntity(entityId);
        Dialogue.Option chosen = null;
        for (Dialogue.Option offered : session.offered()) {
            if (offered.key().equals(option)) {
                chosen = offered;
            }
        }
        if (chosen == null || speaker == null || !speaker.isAlive() || player.distanceTo(speaker) > DISTANCE + 2 + speaker.getBbWidth() / 2) {
            return false;
        }
        chosen.action().run(context(player, speaker, session.who()));
        if (SESSIONS.get(player.getUUID()) == session) {
            end(player);
        }
        return true;
    }

    public static void end(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    /** Whether a speaker is in a talk with anyone. */
    public static boolean talking(Entity speaker) {
        for (Session s : SESSIONS.values()) {
            if (s.entityId() == speaker.getId()) {
                return true;
            }
        }
        return false;
    }

    /** Every tick: the speakers stand still and look at whom they talk to; a talk ends when he walks away. */
    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            Entity speaker = player == null ? null : player.level().getEntity(e.getValue().entityId());
            if (player == null || speaker == null || !speaker.isAlive() || player.distanceTo(speaker) > DISTANCE + 4 + speaker.getBbWidth() / 2) {
                it.remove();
                continue;
            }
            if (speaker instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
            }
        }
    }

    @Nullable
    public static String currentNode(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? null : session.node();
    }

    public static List<String> offered(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        List<String> keys = new ArrayList<>();
        if (session != null) {
            for (Dialogue.Option option : session.offered()) {
                keys.add(option.key());
            }
        }
        return keys;
    }

    public static void clear() {
        SESSIONS.clear();
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }

    private Talk() {
    }
}
