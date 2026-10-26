package com.kolodkingal.enlightened.gametest;

import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingHandler;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Loadout;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.bending.Abilities;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Shared helpers for the Enlightened GameTests. Every test runs in the {@code enlightened:arena} template: a 21x21
 * smooth stone floor at relative y = 1, so creatures stand at relative y = 2, with 9 blocks of air above.
 *
 * <p>A test's players are real {@link ServerPlayer}s on a fake connection, placed on the server; the server does not
 * tick their connection, so the fixture ticks them itself ({@link ServerPlayer#doTick()}, what a connected client's
 * tick does), which is what runs the bending's per-tick logic. Everything the fixture made is undone when the test
 * ends, pass or fail.
 */
public final class TestKit {
    public static final String ARENA = "arena";
    /** Relative height of the top of the arena floor. */
    public static final double FLOOR = 2.0;

    private static int playerCounter;

    public static Fixture fixture(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        helper.testInfo.addListener(fixture);
        helper.onEachTick(fixture::tickPlayers);
        return fixture;
    }

    public static final class Fixture implements GameTestListener {
        private final GameTestHelper helper;
        private final List<ServerPlayer> players = new ArrayList<>();
        private final List<Runnable> undo = new ArrayList<>();

        Fixture(GameTestHelper helper) {
            this.helper = helper;
        }

        /** A Survival player standing at the relative position, facing +z. */
        public ServerPlayer player(double x, double y, double z) {
            return player(x, y, z, GameType.SURVIVAL);
        }

        public ServerPlayer player(double x, double y, double z, GameType gameType) {
            ServerLevel level = helper.getLevel();
            MinecraftServer server = level.getServer();
            GameProfile profile = new GameProfile(UUID.randomUUID(), "enl-test-" + (++playerCounter));
            CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
            ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            // The fake client "supports" every channel, so synced attachments can be sent to it.
            connection.channel().attr(ChannelAttributes.ADHOC_CHANNELS).set(new AcceptAll());
            ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(gameType);
            clearSpawnInvulnerability(player);
            Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
            player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            player.setYHeadRot(0.0F);
            player.getFoodData().setFoodLevel(20);
            players.add(player);
            return player;
        }

        /** Makes the player a bender of the element at the given ability levels (and lays out the keys). */
        public BendingData bender(ServerPlayer player, Element element, Object... levels) {
            Benders.setElement(player, element);
            BendingData data = Benders.get(player);
            for (int i = 0; i + 1 < levels.length; i += 2) {
                Ability a = (Ability) levels[i];
                data.setLevel(a, (Integer) levels[i + 1]);
                if (a.kind == Ability.Kind.PASSIVE) {
                    data.setOn(a, true);
                }
            }
            Loadout.reset(data);
            BendingTicker.applyPassives(player, data);
            Benders.syncIfDirty(player);
            return data;
        }

        /** Presses (and releases at once, unless {@code hold}) the key an ability sits on. */
        public void press(ServerPlayer player, Ability ability, boolean hold) {
            BendingData data = Benders.get(player);
            int slot = data.slotOf(ability);
            if (slot < 0) {
                throw new GameTestAssertException(ability + " is on no key");
            }
            int key = slot / 2 + 1;
            boolean sneak = slot % 2 == 1;
            BendingHandler.onKey(player, key, true, sneak);
            if (!hold) {
                BendingHandler.onKey(player, key, false, sneak);
            }
        }

        public void release(ServerPlayer player, Ability ability) {
            BendingData data = Benders.get(player);
            int slot = data.slotOf(ability);
            BendingHandler.onKey(player, slot / 2 + 1, false, slot % 2 == 1);
        }

        public void lookAt(ServerPlayer player, Entity target) {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, target.position().add(0, target.getBbHeight() * 0.5, 0));
        }

        public void lookAt(ServerPlayer player, Vec3 relative) {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(relative));
        }

        /** A zombie without AI standing at the relative position. */
        public Mob zombie(double x, double y, double z) {
            Mob mob = helper.spawn(EntityType.ZOMBIE, new Vec3(x, y, z));
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            return mob;
        }

        /** PvP is off on the GameTest server; tests where one bender attacks another switch it on. */
        public void pvp(boolean allowed) {
            MinecraftServer server = helper.getLevel().getServer();
            boolean before = server.isPvpAllowed();
            undo.add(() -> server.setPvpAllowed(before));
            server.setPvpAllowed(allowed);
        }

        /** Sets the world's time of day (the moon) and puts it back when the test ends. */
        public void dayTime(long time) {
            ServerLevel level = helper.getLevel();
            long before = level.getDayTime();
            undo.add(() -> level.setDayTime(before));
            level.setDayTime(time);
        }

        public void onClose(Runnable action) {
            undo.add(action);
        }

        void tickPlayers() {
            for (ServerPlayer player : players) {
                if (player.isAlive() && !player.isRemoved()) {
                    player.doTick();
                }
            }
        }

        private void close() {
            Collections.reverse(undo);
            for (Runnable action : undo) {
                try {
                    action.run();
                } catch (RuntimeException ignored) {
                }
            }
            undo.clear();
            MinecraftServer server = helper.getLevel().getServer();
            TempBlocks.restoreEverything(server);
            Iterator<ServerPlayer> it = players.iterator();
            while (it.hasNext()) {
                ServerPlayer player = it.next();
                it.remove();
                try {
                    Benders.setElement(player, Element.NONE);
                    Abilities.forgetAll(player);
                    server.getPlayerList().remove(player);
                } catch (RuntimeException ignored) {
                }
            }
        }

        @Override
        public void testStructureLoaded(GameTestInfo info) {
        }

        @Override
        public void testPassed(GameTestInfo info, GameTestRunner runner) {
            close();
        }

        @Override
        public void testFailed(GameTestInfo info, GameTestRunner runner) {
            close();
        }

        @Override
        public void testAddedForRerun(GameTestInfo oldInfo, GameTestInfo newInfo, GameTestRunner runner) {
        }
    }

    /** A channel set that contains every channel, so the fake client accepts every payload. */
    private static final class AcceptAll extends AbstractSet<ResourceLocation> {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /** A player just placed on the server cannot be hurt for three seconds; a test wants him hurt now. */
    private static void clearSpawnInvulnerability(ServerPlayer player) {
        try {
            java.lang.reflect.Field field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("spawnInvulnerableTime moved", e);
        }
        player.invulnerableTime = 0;
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    public static BlockPos floor(int x, int z) {
        return new BlockPos(x, (int) FLOOR, z);
    }

    private TestKit() {
    }
}
