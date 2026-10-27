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
        CompoundTag back = new CompoundTag();
        back.putString("dim", p.level().dimension().location().toString());
        back.putDouble("x", p.getX());
        back.putDouble("y", p.getY());
        back.putDouble("z", p.getZ());
        back.putFloat("yaw", p.getYRot());
        p.getPersistentData().put(RETURN, back);
        build(cosmic);
        p.fallDistance = 0;
        p.teleportTo(cosmic, 0.5, Y + 1, 0.5, 0.0F, 0.0F);
        cosmic.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        p.sendSystemMessage(Component.translatable("message.enlightened.cosmic.enter").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Every tick of a bender on the plane: the end of the path opens the chakra; a fall puts him back on it. */
    public static void tick(ServerPlayer p, BendingData data) {
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

    /** Lays the path and its two platforms, once. */
    static void build(ServerLevel level) {
        BlockPos endPos = BlockPos.containing(END.x, Y, END.z);
        if (built && level.getBlockState(endPos).is(Blocks.AMETHYST_BLOCK)) {
            return;
        }
        BlockState glass = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        BlockState glow = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState stone = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        // The platform where he arrives, and the one at the end.
        disc(level, 0, 0, 3, stone, glass);
        disc(level, endPos.getX(), endPos.getZ(), 4, stone, glass);
        for (int z = 3; z <= 63; z++) {
            int cx = pathX(z);
            for (int dx = -1; dx <= 1; dx++) {
                at.set(cx + dx, Y, z);
                level.setBlock(at, glass, 2);
                at.set(cx + dx, Y - 1, z);
                level.setBlock(at, glow, 2);
            }
            // The path's edges shine.
            if (z % 4 == 0) {
                at.set(cx - 2, Y, z);
                level.setBlock(at, Blocks.END_ROD.defaultBlockState(), 2);
                at.set(cx + 2, Y, z);
                level.setBlock(at, Blocks.END_ROD.defaultBlockState(), 2);
            }
        }
        built = true;
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
                level.setBlock(at, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
            }
        }
    }

    private Cosmic() {
    }
}
