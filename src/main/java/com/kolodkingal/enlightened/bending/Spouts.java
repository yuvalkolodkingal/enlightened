package com.kolodkingal.enlightened.bending;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The spouts of air and water, shared by the server and the client: what carries each one (solid ground for air;
 * water, or ice, for water), and the ride itself. The ride is driven on the rider's own client, as every player's
 * movement is: the server would zero his speed each tick and leave him stuck on the column. On the column he races
 * where the movement keys point, forward, back and to either side, far faster than he runs.
 */
public final class Spouts {

    /**
     * The height of the top of what carries the spout under {@code p} (within {@code depth} blocks), or NaN if
     * nothing does: for air the first solid block; for water the first water or ice, and a solid block above it
     * stops the search.
     */
    public static double base(Level level, Player p, int depth, boolean water) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(p.getX(), p.getY() + 0.1, p.getZ()).mutable();
        for (int i = 0; i <= depth; i++) {
            BlockState state = level.getBlockState(pos);
            if (water) {
                if (level.getFluidState(pos).is(FluidTags.WATER) || state.is(BlockTags.ICE)) {
                    return pos.getY() + 1.0;
                }
                if (state.isSolid()) {
                    return Double.NaN;
                }
            } else if (state.isSolid()) {
                return pos.getY() + 1.0;
            }
            pos.move(0, -1, 0);
        }
        return Double.NaN;
    }

    /** The column's height by level: air 6 / 10 / 15, water 6 / 12 / 20. */
    public static int height(boolean water, int level) {
        return water ? (level >= 3 ? 20 : level == 2 ? 12 : 6) : (level >= 3 ? 15 : level == 2 ? 10 : 6);
    }

    /** The speed on the column, blocks a tick: 0.55 / 0.7 / 0.85 (a sprint is 0.28). */
    public static double speed(int level) {
        return 0.4 + 0.15 * Math.max(1, Math.min(3, level));
    }

    /**
     * One tick of the ride: up to the top of the column (Sneak sinks him toward its foot), and across the ground where
     * the movement keys point ({@code forward} and {@code strafe} as the player's input gives them, -1..1).
     */
    public static void drive(Player p, float forward, float strafe, double base, int height, double speed) {
        double top = base + height;
        double y = p.getY();
        double vy = p.isShiftKeyDown() ? Math.max(-0.45, (base - y) * 0.15) : (top - y) * 0.15;
        vy = Mth.clamp(vy, -0.5, 0.6);
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        // The player's left: forward turned a quarter.
        double lx = fz;
        double lz = -fx;
        double dx = fx * forward + lx * strafe;
        double dz = fz * forward + lz * strafe;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 1.0) {
            dx /= len;
            dz /= len;
        }
        Vec3 v = p.getDeltaMovement();
        // A quick but smooth answer to the keys: he leans into a turn and glides to a stop.
        double nx = v.x + (dx * speed - v.x) * 0.3;
        double nz = v.z + (dz * speed - v.z) * 0.3;
        p.setDeltaMovement(nx, vy, nz);
        p.fallDistance = 0;
    }

    private Spouts() {
    }
}
