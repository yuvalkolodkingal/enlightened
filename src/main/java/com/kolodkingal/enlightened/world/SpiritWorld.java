package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.block.SpiritPortalBlock;
import com.kolodkingal.enlightened.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The Spirit World: a dimension of its own (data: dimension/spirit_world, a lavender sky over groves of cherry and
 * azalea, giant mushrooms, ferns and flowers, the Tree of Time). One crosses to it through a spirit portal (a gate of
 * old stone in the forests, {@link SpiritPortalPiece}), or as the Avatar, meditating on a Meditation Point; and comes
 * back the same ways. Crossing builds a gate where one arrives, if none is near, so the way back is always at hand.
 */
public final class SpiritWorld {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Enlightened.id("spirit_world"));
    private static final String RETURN = "enlightened_spirit_return";
    /** How long the Avatar meditates on a Meditation Point to cross (ticks). */
    public static final int AVATAR_CROSSING = 200;

    public static boolean in(ServerPlayer p) {
        return p.level().dimension().equals(LEVEL);
    }

    /** Crosses over, from the world to the Spirit World or back; {@code gate} is the portal he stepped into, if any. */
    public static void cross(ServerPlayer p, BlockPos gate) {
        if (in(p)) {
            leave(p);
        } else {
            enter(p);
        }
    }

    /** To the Spirit World, at the same place as he stands, remembering where he left from. */
    public static boolean enter(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel spirit = server == null ? null : server.getLevel(LEVEL);
        if (spirit == null) {
            return false;
        }
        CompoundTag back = new CompoundTag();
        back.putString("dim", p.level().dimension().location().toString());
        back.putDouble("x", p.getX());
        back.putDouble("y", p.getY());
        back.putDouble("z", p.getZ());
        back.putFloat("yaw", p.getYRot());
        p.getPersistentData().put(RETURN, back);
        BlockPos at = arrival(spirit, p.getBlockX(), p.getBlockZ());
        p.fallDistance = 0;
        p.setPortalCooldown(80);
        p.teleportTo(spirit, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0.0F);
        spirit.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.7F);
        p.sendSystemMessage(Component.translatable("message.enlightened.spirit.enter").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Back to where he left from (or, if that is lost, to the same place in the world). */
    public static void leave(ServerPlayer p) {
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
        p.setPortalCooldown(80);
        if (to == null || to.dimension().equals(LEVEL)) {
            ServerLevel overworld = server.overworld();
            BlockPos at = arrival(overworld, p.getBlockX(), p.getBlockZ());
            p.teleportTo(overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0.0F);
        } else {
            // A step out of the gate he went in by, so he does not walk straight back through it.
            p.teleportTo(to, back.getDouble("x") + 1.5, back.getDouble("y"), back.getDouble("z") + 1.5, back.getFloat("yaw"), 0.0F);
        }
        p.sendSystemMessage(Component.translatable("message.enlightened.spirit.leave").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Where one arrives in a level at a column: on its surface, beside a gate (built if none stands near). */
    static BlockPos arrival(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        y = Math.max(y, level.getMinBuildHeight() + 2);
        BlockPos stand = new BlockPos(x, y, z);
        if (level.getBlockState(stand.below()).getFluidState().isSource() || level.getBlockState(stand.below()).isAir()) {
            level.setBlock(stand.below(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
        }
        if (level.dimension().equals(LEVEL) && !gateNear(level, stand)) {
            gate(level, stand.offset(0, 0, -3), Direction.Axis.X, true);
        }
        return stand;
    }

    private static boolean gateNear(ServerLevel level, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-8, -4, -8), at.offset(8, 6, 8))) {
            if (level.getBlockState(p).is(ModBlocks.SPIRIT_PORTAL.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A spirit gate standing at {@code base} (the middle of its threshold), its opening along {@code axis}: two pillars
     * of mossy stone, a lintel, the portal's light between them (3 wide, 5 high) on a floor of old stone.
     */
    public static void gate(LevelAccessor level, BlockPos base, Direction.Axis axis, boolean clear) {
        BlockState pillar = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState light = ModBlocks.SPIRIT_PORTAL.get().defaultBlockState().setValue(SpiritPortalBlock.AXIS, axis);
        for (int a = -3; a <= 3; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int y = 0; y <= 7; y++) {
                    BlockPos p = at(base, axis, a, y, b);
                    BlockState s = null;
                    if (y == 0) {
                        s = Math.abs(a) == 3 ? stone : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                    } else if (Math.abs(a) == 2 && b == 0 && y <= 5) {
                        s = y == 3 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : pillar;
                    } else if (y == 6 && b == 0) {
                        s = Math.abs(a) <= 2 ? pillar : Blocks.STONE_BRICK_WALL.defaultBlockState();
                    } else if (y == 7 && b == 0 && Math.abs(a) <= 1) {
                        s = a == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICK_SLAB.defaultBlockState();
                    } else if (Math.abs(a) <= 1 && b == 0 && y <= 5) {
                        s = light;
                    } else if (clear && y >= 1) {
                        s = Blocks.AIR.defaultBlockState();
                    }
                    if (s != null) {
                        level.setBlock(p, s, 2);
                    }
                }
            }
        }
    }

    private static BlockPos at(BlockPos base, Direction.Axis axis, int a, int y, int b) {
        return axis == Direction.Axis.X ? base.offset(a, y, b) : base.offset(b, y, a);
    }

    private SpiritWorld() {
    }
}
