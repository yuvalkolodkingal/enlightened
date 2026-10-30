package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.Chakras;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The cosmic plane of the seventh chakra, as Aang saw it: when the Thought chakra is about to open, the bender's spirit
 * leaves for a sea of stars in purple, where a path of light runs out ahead of him and his own self sits at its end,
 * vast and glowing, in the lotus. He must walk the path to its end; there the chakra opens and he comes back where he
 * sat. A step off the path, and it catches him again at its start.
 */
public final class Cosmic {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Enlightened.id("cosmic"));
    public static final int Y = 100;
    /** Where the path ends (the platform before the giant). */
    public static final Vec3 END = new Vec3(pathX(66) + 0.5, Y + 1, 66.5);
    /** Where the giant self sits, behind the end of the path, facing back along it. */
    public static final Vec3 GIANT = new Vec3(pathX(66) + 0.5, Y - 4, 104.5);
    private static final String RETURN = "enlightened_cosmic_return";
    /** Set while he walks the Anomaly's path (to the seven circles), not the Thought chakra's (to his giant self). */
    private static final String ANOMALY = "enlightened_cosmic_anomaly";
    /** Where the Anomaly's path runs, far from the giant: straight out along z from x = {@code ANOMALY_X}. */
    public static final int ANOMALY_X = 2000;
    /** The seven circles at the end of the Anomaly's path, one for each chakra, the first nearest (and lowest). */
    public static final int[] CIRCLE_Z = {44, 52, 60, 68, 76, 84, 92};
    public static final int CIRCLE_R = 3;
    private static boolean builtAnomaly;
    private static boolean built;

    /** The path's centre line, swaying gently from side to side as it runs out over the stars. */
    public static int pathX(int z) {
        return Math.round(6.0F * Mth.sin(z / 11.0F));
    }

    public static boolean in(ServerPlayer p) {
        return p.level().dimension().equals(LEVEL);
    }

    /** Sends his spirit to the plane, remembering where he sat. */
    public static boolean send(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel cosmic = server == null ? null : server.getLevel(LEVEL);
        if (cosmic == null) {
            return false;
        }
        remember(p);
        p.getPersistentData().remove(ANOMALY);
        build(cosmic);
        p.fallDistance = 0;
        p.teleportTo(cosmic, 0.5, Y + 1, 0.5, 0.0F, 0.0F);
        cosmic.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        p.sendSystemMessage(Component.translatable("message.enlightened.cosmic.enter").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /**
     * After the Anomaly is beaten: his spirit goes to the plane, onto a path that ends not at his giant self but at seven
     * circles, one in each chakra's colour; the first, red, is open. Walking into it opens his first chakra and sends him
     * back.
     */
    public static boolean sendForAnomaly(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel cosmic = server == null ? null : server.getLevel(LEVEL);
        if (cosmic == null) {
            return false;
        }
        if (!in(p)) {
            remember(p);
        }
        p.getPersistentData().putBoolean(ANOMALY, true);
        buildAnomaly(cosmic);
        p.fallDistance = 0;
        p.teleportTo(cosmic, ANOMALY_X + 0.5, Y + 1, 0.5, 0.0F, 0.0F);
        cosmic.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        p.sendSystemMessage(Component.translatable("message.enlightened.cosmic.anomaly").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    public static boolean onAnomalyPath(ServerPlayer p) {
        return p.getPersistentData().getBoolean(ANOMALY);
    }

    private static void remember(ServerPlayer p) {
        CompoundTag back = new CompoundTag();
        back.putString("dim", p.level().dimension().location().toString());
        back.putDouble("x", p.getX());
        back.putDouble("y", p.getY());
        back.putDouble("z", p.getZ());
        back.putFloat("yaw", p.getYRot());
        p.getPersistentData().put(RETURN, back);
    }

    /** Every tick on the Anomaly's path: a fall puts him back at its start; the red circle opens the first chakra. */
    private static void anomalyTick(ServerPlayer p, BendingData data) {
        ServerLevel level = p.serverLevel();
        buildAnomaly(level);
        p.fallDistance = 0;
        if (p.getY() < Y - 15) {
            p.teleportTo(level, ANOMALY_X + 0.5, Y + 1, 0.5, 0.0F, 0.0F);
            p.displayClientMessage(Component.translatable("message.enlightened.cosmic.fell").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        if (p.tickCount % 4 == 0) {
            // The red circle is open: its light swirls inside it.
            for (int i = 0; i < 6; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = level.random.nextDouble() * (CIRCLE_R - 0.6);
                level.sendParticles(new com.kolodkingal.enlightened.particle.EnergyParticleOptions(Chakras.colour(1), 0.8F),
                        ANOMALY_X + 0.5 + Math.cos(a) * r, circleCentre(0).y + Math.sin(a) * r, CIRCLE_Z[0] + 0.5, 1, 0, 0, 0, 0.0);
            }
        }
        if (inCircle(p.position(), 0)) {
            if (data.chakras() < 1) {
                Chakras.open(p, data, 1, false);
            } else {
                p.sendSystemMessage(Component.translatable("message.enlightened.cosmic.anomaly_open").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            bringBack(p);
        }
    }

    /**
     * The centre of the n-th circle (0 the first): the first stands on the path, the others rise one above another
     * beyond it, a ladder of the chakras up into the stars, as the show draws them.
     */
    public static Vec3 circleCentre(int n) {
        return new Vec3(ANOMALY_X + 0.5, Y + CIRCLE_R + 0.5 + n * (2 * CIRCLE_R + 1.5), CIRCLE_Z[n] + 0.5);
    }

    /** Whether a place is inside the n-th circle (0 the first), where it stands across the path. */
    public static boolean inCircle(Vec3 at, int n) {
        double cy = circleCentre(n).y;
        double dx = at.x - (ANOMALY_X + 0.5);
        double dyFeet = at.y - cy;
        double dyHead = at.y + 1.6 - cy;
        boolean within = dx * dx + Math.min(dyFeet * dyFeet, dyHead * dyHead) < (CIRCLE_R - 0.4) * (CIRCLE_R - 0.4);
        return within && Math.abs(at.z - (CIRCLE_Z[n] + 0.5)) < 0.8;
    }

    /** Every tick of a bender on the plane: the end of the path opens the chakra; a fall puts him back on it. */
    public static void tick(ServerPlayer p, BendingData data) {
        if (onAnomalyPath(p)) {
            anomalyTick(p, data);
            return;
        }
        ServerLevel level = p.serverLevel();
        build(level);
        p.fallDistance = 0;
        if (p.getY() < Y - 15) {
            p.teleportTo(level, 0.5, Y + 1, 0.5, 0.0F, 0.0F);
            p.displayClientMessage(Component.translatable("message.enlightened.cosmic.fell").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        double dx = p.getX() - END.x;
        double dz = p.getZ() - END.z;
        boolean arrived = dx * dx + dz * dz < 12.0 && Math.abs(p.getY() - END.y) < 3.0;
        if (data.chakras() >= Chakras.COUNT || arrived) {
            if (data.chakras() < Chakras.COUNT) {
                Chakras.open(p, data, Chakras.COUNT, false);
                p.sendSystemMessage(Component.translatable("message.enlightened.cosmic.done").withStyle(ChatFormatting.GOLD));
            }
            bringBack(p);
        }
    }

    /** Back to where he sat (or the world's spawn, if that is lost). */
    public static void bringBack(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) {
            return;
        }
        CompoundTag back = p.getPersistentData().getCompound(RETURN);
        p.getPersistentData().remove(RETURN);
        p.getPersistentData().remove(ANOMALY);
        ServerLevel to = null;
        if (back.contains("dim")) {
            ResourceLocation id = ResourceLocation.tryParse(back.getString("dim"));
            to = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        }
        p.fallDistance = 0;
        if (to == null || to.dimension().equals(LEVEL)) {
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            p.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0F, 0.0F);
        } else {
            p.teleportTo(to, back.getDouble("x"), back.getDouble("y"), back.getDouble("z"), back.getFloat("yaw"), 0.0F);
        }
    }

    /**
     * Lays the path and its two platforms, once: an unseen floor (barrier blocks) under the path of energy the client
     * draws over it (CosmicRenderer). Whatever an older build laid (glass, crying obsidian, end rods) is cleared.
     */
    static void build(ServerLevel level) {
        BlockPos endPos = BlockPos.containing(END.x, Y, END.z);
        if (built && level.getBlockState(endPos).is(Blocks.BARRIER)) {
            return;
        }
        BlockState floor = Blocks.BARRIER.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        disc(level, 0, 0, 3, floor, floor);
        disc(level, endPos.getX(), endPos.getZ(), 4, floor, floor);
        for (int z = 3; z <= 63; z++) {
            int cx = pathX(z);
            for (int dx = -2; dx <= 2; dx++) {
                level.setBlock(at.set(cx + dx, Y, z), Math.abs(dx) <= 1 ? floor : Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(at.set(cx + dx, Y - 1, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        built = true;
    }

    /**
     * Lays the Anomaly's path, straight out, once: an unseen floor under the energy the client draws, with its seven
     * circles (drawn by the client too) standing across its end. Older builds' blocks are cleared.
     */
    static void buildAnomaly(ServerLevel level) {
        BlockPos check = new BlockPos(ANOMALY_X, Y, CIRCLE_Z[6] + 4);
        if (builtAnomaly && level.getBlockState(check).is(Blocks.BARRIER)) {
            return;
        }
        BlockState floor = Blocks.BARRIER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        disc(level, ANOMALY_X, 0, 3, floor, floor);
        disc(level, ANOMALY_X, CIRCLE_Z[6] + 4, 3, floor, floor);
        for (int z = 3; z <= CIRCLE_Z[6] + 1; z++) {
            for (int dx = -2; dx <= 2; dx++) {
                level.setBlock(at.set(ANOMALY_X + dx, Y, z), Math.abs(dx) <= 1 ? floor : air, 2);
                level.setBlock(at.set(ANOMALY_X + dx, Y - 1, z), air, 2);
            }
        }
        // Where an older build stood the circles in concrete, clear them.
        for (int n = 0; n < CIRCLE_Z.length; n++) {
            for (int dx = -CIRCLE_R - 1; dx <= CIRCLE_R + 1; dx++) {
                for (int dy = 1; dy <= 2 * CIRCLE_R + 2; dy++) {
                    level.setBlock(at.set(ANOMALY_X + dx, Y + dy, CIRCLE_Z[n]), air, 2);
                }
            }
        }
        builtAnomaly = true;
    }

    private static void disc(ServerLevel level, int cx, int cz, int r, BlockState core, BlockState rim) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r * r + r) {
                    continue;
                }
                at.set(cx + dx, Y, cz + dz);
                level.setBlock(at, d2 >= (r - 1) * (r - 1) + r - 1 ? rim : core, 2);
                at.set(cx + dx, Y - 1, cz + dz);
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private Cosmic() {
    }
}
