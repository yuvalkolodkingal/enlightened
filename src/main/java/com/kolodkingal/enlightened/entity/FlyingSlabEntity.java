package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.BlockRights;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A body of blocks moved by an earthbender, flying as one: a wall he struck (at Earth Wall or Metal Wall's third
 * level), or a great slab of land he lifted (the Great Lift). While <b>held</b> it hangs where its bender looks;
 * <b>thrown</b>, it flies on until it meets a block or a creature, hurting everything it hits, and then breaks apart
 * (a slab of land breaks into the land where it falls). A held slab can also be <b>set down</b> whole, somewhere else.
 *
 * <p>Each block is kept relative to the entity's position: block {@code rel} fills {@code pos + rel} shifted half a
 * block (the position is the middle of the bottom of the block at {@code rel} 0,0,0).
 */
public class FlyingSlabEntity extends Entity implements IEntityWithComplexSpawn {
    public record Piece(BlockPos rel, BlockState state) {
    }

    public enum Mode { HELD, THROWN }

    private final List<Piece> pieces = new ArrayList<>();
    private AABB extent = new AABB(-0.5, 0, -0.5, 0.5, 1, 0.5);
    @Nullable
    private UUID owner;
    private Mode mode = Mode.THROWN;
    private float damage = 8.0F;
    /** Whether it breaks into nothing when it lands (a wall), or falls apart into the land there (a slab). */
    private boolean shatters = true;
    /** How far in front of its bender a held slab hangs. */
    private double holdDistance = 8.0;
    private int life;
    private final Set<Integer> struck = new HashSet<>();

    public FlyingSlabEntity(EntityType<? extends FlyingSlabEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A body of blocks, from blocks keyed by where they were in the world; its middle at the bottom is the position. */
    public static FlyingSlabEntity of(ServerLevel level, Map<BlockPos, BlockState> blocks, ServerPlayer owner) {
        FlyingSlabEntity slab = ModEntities.FLYING_SLAB.get().create(level);
        int minY = Integer.MAX_VALUE;
        double sx = 0;
        double sz = 0;
        for (BlockPos p : blocks.keySet()) {
            minY = Math.min(minY, p.getY());
            sx += p.getX();
            sz += p.getZ();
        }
        BlockPos pivot = new BlockPos((int) Math.floor(sx / blocks.size() + 0.5), minY, (int) Math.floor(sz / blocks.size() + 0.5));
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            slab.pieces.add(new Piece(e.getKey().subtract(pivot), e.getValue()));
        }
        slab.computeExtent();
        slab.owner = owner.getUUID();
        slab.moveTo(pivot.getX() + 0.5, pivot.getY(), pivot.getZ() + 0.5, 0.0F, 0.0F);
        return slab;
    }

    private void computeExtent() {
        double x0 = 0, y0 = 0, z0 = 0, x1 = 0, y1 = 0, z1 = 0;
        boolean first = true;
        for (Piece p : pieces) {
            BlockPos r = p.rel;
            if (first) {
                x0 = r.getX(); y0 = r.getY(); z0 = r.getZ(); x1 = r.getX(); y1 = r.getY(); z1 = r.getZ();
                first = false;
            }
            x0 = Math.min(x0, r.getX()); y0 = Math.min(y0, r.getY()); z0 = Math.min(z0, r.getZ());
            x1 = Math.max(x1, r.getX()); y1 = Math.max(y1, r.getY()); z1 = Math.max(z1, r.getZ());
        }
        extent = new AABB(x0 - 0.5, y0, z0 - 0.5, x1 + 0.5, y1 + 1, z1 + 0.5);
    }

    public List<Piece> pieces() {
        return pieces;
    }

    public Mode mode() {
        return mode;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setShatters(boolean shatters) {
        this.shatters = shatters;
    }

    public void hold(double distance) {
        mode = Mode.HELD;
        holdDistance = distance;
        setDeltaMovement(Vec3.ZERO);
    }

    public void throwAlong(Vec3 direction, double speed) {
        mode = Mode.THROWN;
        struck.clear();
        setDeltaMovement(direction.normalize().scale(speed));
    }

    public boolean ownedBy(Entity e) {
        return owner != null && owner.equals(e.getUUID());
    }

    /** The space it fills, where it is now. */
    public AABB space() {
        return extent.move(position());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return space().inflate(1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            // Moved by the server; drawn where it is told.
            return;
        }
        life++;
        ServerPlayer bender = owner == null ? null : (ServerPlayer) level.getPlayerByUUID(owner);
        if (mode == Mode.HELD) {
            if (bender == null || !bender.isAlive() || bender.level() != level || life > 20 * 60) {
                // Let go: it settles where it hangs.
                setDown(level, null);
                return;
            }
            Vec3 want = bender.getEyePosition().add(bender.getLookAngle().scale(holdDistance)).subtract(0, (extent.maxY - extent.minY) / 2, 0);
            Vec3 v = want.subtract(position()).scale(0.25);
            setDeltaMovement(v);
            setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
            if (life % 4 == 0) {
                dust(level, 3);
            }
            return;
        }
        Vec3 v = getDeltaMovement().add(0, -0.025, 0);
        setDeltaMovement(v);
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        // What it strikes on the way.
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, space().inflate(0.3), e -> e.isAlive() && !ownedBy(e))) {
            if (struck.add(e.getId())) {
                boolean hurt = bender != null ? AreaTargets.mayHurt(bender, e) && Shots.hurt(bender, e, damage, ModDamageTypes.EARTH)
                        : e.hurt(damageSources().generic(), damage);
                if (hurt || bender == null) {
                    Shots.shove(e, v, 1.2, 0.4);
                }
            }
        }
        if (hitsBlock(level) || life > 200 || getY() < level.getMinBuildHeight()) {
            land(level);
        }
    }

    private boolean hitsBlock(ServerLevel level) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        Vec3 p = position();
        for (Piece piece : pieces) {
            at.set(Math.floor(p.x + piece.rel.getX()), Math.floor(p.y + piece.rel.getY() + 0.05), Math.floor(p.z + piece.rel.getZ()));
            BlockState s = level.getBlockState(at);
            if (!s.isAir() && !s.canBeReplaced() && !s.getCollisionShape(level, at).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** It meets the ground or a wall: a wall breaks apart; a slab of land falls into the land there. */
    private void land(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
        if (shatters) {
            dust(level, Math.min(60, pieces.size() * 3));
            discard();
            return;
        }
        // Everything round where it lands is crushed under it.
        ServerPlayer bender = owner == null ? null : (ServerPlayer) level.getPlayerByUUID(owner);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, space().inflate(2.0), e -> e.isAlive() && !ownedBy(e))) {
            if (struck.add(e.getId())) {
                if (bender != null ? AreaTargets.mayHurt(bender, e) && Shots.hurt(bender, e, damage * 0.75F, ModDamageTypes.EARTH)
                        : e.hurt(damageSources().generic(), damage * 0.75F)) {
                    Shots.push(e, position(), 1.0, 0.5);
                }
            }
        }
        dust(level, Math.min(80, pieces.size()));
        // Back off the step that took it into the block, and settle there.
        setPos(getX() - getDeltaMovement().x, getY() - getDeltaMovement().y, getZ() - getDeltaMovement().z);
        setDown(level, null);
    }

    /**
     * Sets every block down where it is (or at {@code at}, if given: its new middle at the bottom), into air,
     * water or plants, where its bender may build; what cannot be set down breaks.
     */
    public void setDown(ServerLevel level, @Nullable Vec3 at) {
        Vec3 base = at != null ? at : position();
        BlockPos pivot = BlockPos.containing(base.x, base.y + 0.3, base.z);
        ServerPlayer bender = owner == null ? null : (ServerPlayer) level.getPlayerByUUID(owner);
        // Lowest first, so what stood on the slab has its ground.
        List<Piece> order = new ArrayList<>(pieces);
        order.sort((a, b) -> Integer.compare(a.rel.getY(), b.rel.getY()));
        int broken = 0;
        for (Piece piece : order) {
            BlockPos to = pivot.offset(piece.rel);
            BlockState there = level.getBlockState(to);
            boolean free = there.isAir() || there.canBeReplaced();
            if (free && !level.isOutsideBuildHeight(to) && (bender == null || BlockRights.mayPlace(bender, level, to))
                    && piece.state.canSurvive(level, to)) {
                level.setBlock(to, piece.state, Block.UPDATE_ALL);
            } else if (broken++ < 40) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, piece.state), to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.05);
            }
        }
        level.playSound(null, pivot, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.5F);
        discard();
    }

    private void dust(ServerLevel level, int count) {
        if (pieces.isEmpty()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            Piece piece = pieces.get(random.nextInt(pieces.size()));
            Vec3 c = position().add(piece.rel.getX(), piece.rel.getY() + 0.5, piece.rel.getZ());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, piece.state), c.x, c.y, c.z, 2, 0.35, 0.35, 0.35, 0.05);
        }
    }

    // ---- network and saving ----

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(pieces.size());
        for (Piece p : pieces) {
            buf.writeVarInt(p.rel.getX());
            buf.writeVarInt(p.rel.getY());
            buf.writeVarInt(p.rel.getZ());
            buf.writeVarInt(Block.getId(p.state));
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        pieces.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            BlockPos rel = new BlockPos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            pieces.add(new Piece(rel, Block.stateById(buf.readVarInt())));
        }
        computeExtent();
    }

    @Override
    public boolean shouldBeSaved() {
        // A body of blocks in flight lasts seconds; it is never saved (a held one is set down when its bender goes).
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
