package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * How a bender grows. Every monster killed with the bender's own bending earns experience; every
 * {@code general.xpPerPoint} experience is an ability point; a point buys the next level of an ability (level 1
 * costs 1 point, level 2 two, level 3 three). Sub-bending (sand, lava, metal, ice, blood, lightning) opens once
 * enough points are spent on the element.
 *
 * <p>The bender's <em>strength</em> is the points spent on his element: it decides who is the stronger bender when
 * bending meets bending (quicksand and bloodbending fail against a stronger or equal bender).
 */
public final class Mastery {

    public static int startingPoints() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.STARTING_POINTS.get() : 1;
    }

    public static int xpPerPoint() {
        return EnlightenedConfig.SPEC.isLoaded() ? Math.max(1, EnlightenedConfig.XP_PER_POINT.get()) : 3;
    }

    /** The points spent on the element: the bender's strength. */
    public static int strength(Player player) {
        return Benders.get(player).spent();
    }

    /** The points a gate asks for (0 when it is open to all; -1 when no number of points opens it). */
    public static int gateCost(Gate gate) {
        boolean loaded = EnlightenedConfig.SPEC.isLoaded();
        return switch (gate) {
            case NONE -> 0;
            case SAND -> loaded ? EnlightenedConfig.SAND_GATE.get() : 3;
            case LAVA -> loaded ? EnlightenedConfig.LAVA_GATE.get() : 6;
            case METAL -> loaded ? EnlightenedConfig.METAL_GATE.get() : 9;
            case ICE -> loaded ? EnlightenedConfig.ICE_GATE.get() : 3;
            case BLOOD -> loaded ? EnlightenedConfig.BLOOD_GATE.get() : 9;
            case LIGHTNING -> loaded ? EnlightenedConfig.LIGHTNING_GATE.get() : 6;
            case COMBUSTION -> loaded ? EnlightenedConfig.COMBUSTION_GATE.get() : 9;
            case TETHERLESS -> -1;
        };
    }

    /** Whether the gate of an ability is open for this bender. */
    public static boolean gateOpen(BendingData data, Ability a) {
        if (a.gate == Gate.TETHERLESS) {
            return data.tetherless();
        }
        return data.spent() >= gateCost(a.gate);
    }

    /** Why the next level of an ability cannot be learned, or null if it can. */
    public static String refusal(BendingData data, Ability a) {
        if (data.element() != a.element) {
            return "message.enlightened.learn.wrong_element";
        }
        if (data.level(a) >= Ability.MAX_LEVEL) {
            return "message.enlightened.learn.mastered";
        }
        if (!gateOpen(data, a)) {
            return a.gate == Gate.TETHERLESS ? "message.enlightened.learn.tethered" : "message.enlightened.learn.gate";
        }
        if (data.points() < Ability.cost(data.level(a) + 1)) {
            return "message.enlightened.learn.points";
        }
        return null;
    }

    public static boolean canLearn(BendingData data, Ability a) {
        return refusal(data, a) == null;
    }

    /** Spends points on the next level of an ability. Returns false (and tells the player why) if it cannot. */
    public static boolean learn(ServerPlayer player, Ability a) {
        BendingData data = Benders.get(player);
        String refused = refusal(data, a);
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused, a.displayName(), gateCost(a.gate)), true);
            return false;
        }
        int level = data.level(a) + 1;
        data.setPoints(data.points() - Ability.cost(level));
        data.setLevel(a, level);
        if (a.bindable() && data.slotOf(a) < 0) {
            Loadout.placeFree(data, a);
        }
        if (level == 1 && a.kind == Ability.Kind.PASSIVE) {
            data.setOn(a, true);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEARN.get(), SoundSource.PLAYERS, 1.0F, 0.9F + level * 0.1F);
        Benders.burst(player.serverLevel(), player.getX(), player.getY() + 1, player.getZ(), Benders.color(player), 30, 0.6);
        player.displayClientMessage(Component.translatable("message.enlightened.learned", a.displayName(), level)
                .withStyle(ChatFormatting.GREEN), true);
        BendingTicker.applyPassives(player, data);
        Benders.syncIfDirty(player);
        return true;
    }

    /** Gives points directly (a command). */
    public static void addPoints(ServerPlayer player, int points) {
        BendingData data = Benders.get(player);
        data.setPoints(Math.max(0, data.points() + points));
        Benders.syncIfDirty(player);
    }

    /** Takes back every level of every ability and refunds the points (a command, or the scroll's reset). */
    public static void reset(ServerPlayer player) {
        BendingData data = Benders.get(player);
        int refund = data.spent();
        for (Ability a : Ability.values()) {
            data.setLevel(a, 0);
            data.setOn(a, false);
        }
        Ability starter = Ability.starterOf(data.element());
        if (starter != null) {
            data.setLevel(starter, 1);
            refund -= 1;
        }
        data.setPoints(data.points() + Math.max(0, refund));
        Loadout.reset(data);
        BendingTicker.applyPassives(player, data);
        Benders.syncIfDirty(player);
    }

    /** The experience a kill of this creature earns a bender (0 for creatures that give none: animals, villagers). */
    public static int xpFor(LivingEntity killed) {
        boolean loaded = EnlightenedConfig.SPEC.isLoaded();
        if (killed instanceof EnderDragon || killed instanceof WitherBoss || killed instanceof Warden || killed instanceof ElderGuardian) {
            return loaded ? EnlightenedConfig.BOSS_XP.get() : 10;
        }
        if (killed instanceof Player) {
            return loaded ? EnlightenedConfig.PLAYER_XP.get() : 2;
        }
        if (killed instanceof Enemy) {
            return 1;
        }
        return 0;
    }

    /** Experience from anywhere (meditation): a point whenever enough has gathered. */
    public static void earn(ServerPlayer player, int xp) {
        BendingData data = Benders.get(player);
        int total = data.xp() + xp;
        int per = xpPerPoint();
        int gained = total / per;
        data.setXp(total % per);
        if (gained > 0) {
            data.setPoints(data.points() + gained);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POINT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable(gained == 1 ? "message.enlightened.point" : "message.enlightened.points", gained, data.points())
                    .withStyle(ChatFormatting.GOLD), true);
        }
        Benders.syncIfDirty(player);
    }

    /** A bender killed a creature with his bending: experience, and a point whenever enough has gathered. */
    public static void onKill(ServerPlayer player, LivingEntity killed) {
        BendingData data = Benders.get(player);
        if (!data.element().bends()) {
            return;
        }
        int xp = xpFor(killed);
        if (xp <= 0) {
            return;
        }
        data.setKills(data.kills() + 1);
        int total = data.xp() + xp;
        int per = xpPerPoint();
        int gained = total / per;
        data.setXp(total % per);
        if (gained > 0) {
            data.setPoints(data.points() + gained);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POINT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable(gained == 1 ? "message.enlightened.point" : "message.enlightened.points", gained, data.points())
                    .withStyle(ChatFormatting.GOLD), true);
        }
        Benders.syncIfDirty(player);
    }

    private Mastery() {
    }
}
