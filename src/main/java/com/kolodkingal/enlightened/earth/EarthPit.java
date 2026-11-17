package com.kolodkingal.enlightened.earth;

import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.AreaTargets;
import com.kolodkingal.enlightened.bending.BlockRights;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Raycasts;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Hidden Pit (1.1.0): the earthbender hollows out the earth under a patch of ground and leaves its top as it was, so
 * the pit looks like any other ground. The first creature that steps on it (never the bender, his teammates or his
 * pets) drops in as the top caves; a moment later the ground closes over its head and holds it, and then the earth
 * spits it out and the ground is whole again.
 *
 * <ul>
 * <li>Novice: one block wide, two deep; one pit at a time.</li>
 * <li>Adept: three blocks wide, three deep; two pits.</li>
 * <li>Master: three wide, three deep, held longer and harder; three pits.</li>
 * </ul>
 * Only earth and sand are dug, never a block with something in it, never next to water or lava, never under anything
 * that would fall, and only where the bender may break blocks ({@link BlockRights}). Every dug block is raised air
 * ({@link TempBlocks}): it goes back when the pit closes, when it waits too long, when the bender leaves, and when the
 * server stops; whoever is inside is lifted out first, so nobody is ever shut in the earth.
 */
public final class EarthPit implements AbilityLogic {
    /** Ticks a victim falls before the earth strikes him and closes. */
    public static final int FALL = 8;

    enum Stage { ARMED, SPRUNG, CLOSED, OPEN }

    /** One pit: its surface (the disguise), the hollow under it, its owner and where it is in its life. */
    public static final class Pit {
        final ServerLevel level;
        final UUID owner;
        final List<BlockPos> tops = new ArrayList<>();
        final List<BlockState> topStates = new ArrayList<>();
        final List<BlockPos> holes = new ArrayList<>();
        /** The height of the surface blocks. */
        final int surface;
        final int depth;
        final float damage;
        final int hold;
        final long armedUntil;
        Stage stage = Stage.ARMED;
        long since;
        AABB footprint;

        Pit(ServerLevel level, UUID owner, int surface, int depth, float damage, int hold, long armedUntil) {
            this.level = level;
            this.owner = owner;
            this.surface = surface;
            this.depth = depth;
            this.damage = damage;
            this.hold = hold;
            this.armedUntil = armedUntil;
        }

        /** The hollow and the surface over it, as a box. */
        AABB inside() {
            return new AABB(footprint.minX, surface - depth, footprint.minZ, footprint.maxX, surface + 1, footprint.maxZ);
        }

        public boolean sprung() {
            return stage != Stage.ARMED;
        }

        public boolean closed() {
            return stage == Stage.CLOSED;
        }

        public List<BlockPos> holes() {
            return holes;
        }

        public List<BlockPos> tops() {
            return tops;
        }
    }

    private static final List<Pit> PITS = new ArrayList<>();

    static int range() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.PIT_RANGE.get() : 16;
    }

    /** Whether the pit may be dug through this block: earth or sand, whole, nothing in it, nothing that falls. */
    static boolean diggable(ServerPlayer p, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.is(ModTags.EARTH) || state.is(ModTags.SAND)) || state.hasBlockEntity() || state.getBlock() instanceof FallingBlock
                || TempBlocks.isRaised(level, pos) || !BlockRights.mayBreak(p, level, pos, state)) {
            return false;
        }
        for (Direction d : Direction.values()) {
            if (!level.getFluidState(pos.relative(d)).isEmpty()) {
                return false; // water or lava would pour in
            }
        }
        return true;
    }

    @Override
    public void key(Cast c, boolean down) {
        if (!down || !c.ready()) {
            return;
        }
        ServerPlayer p = c.player;
        ServerLevel level = c.level();
        BlockHitResult hit = Raycasts.block(p, range(), 1.0F);
        if (hit.getType() != HitResult.Type.BLOCK) {
            c.refuse("message.enlightened.earth.pit_no_earth");
            return;
        }
        BlockPos center = EarthBending.topEarth(level, hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
        if (center == null) {
            c.refuse("message.enlightened.earth.pit_no_earth");
            return;
        }
        int r = c.level >= 2 ? 1 : 0;
        int depth = c.by(2, 3, 3);
        int holdSeconds = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.PIT_HOLD_SECONDS.get() : 3;
        int hold = (holdSeconds + c.level - 1) * 20;
        int wait = 20 * (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.PIT_WAIT_SECONDS.get() : 60);
        float base = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.PIT_DAMAGE.get().floatValue() : 4.0F;
        Pit pit = new Pit(level, p.getUUID(), center.getY(), depth, base * c.byF(1.0, 1.4, 1.8), hold, c.now() + wait);
        int safety = wait + hold + 20 * 30;
        // Every column of the patch at the same height, with air over it and earth all the way down, is dug.
        List<BlockPos> columns = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos top = center.offset(dx, 0, dz);
                if (!level.getBlockState(top.above()).isAir() || !diggable(p, level, top)) {
                    continue;
                }
                boolean ok = true;
                for (int d = 1; d <= depth && ok; d++) {
                    ok = diggable(p, level, top.below(d));
                }
                if (ok) {
                    columns.add(top);
                }
            }
        }
        if (columns.isEmpty() || !columns.contains(center) && r == 0) {
            c.refuse("message.enlightened.earth.pit_no_earth");
            return;
        }
        // The bender's own pits: the oldest fills in if he has dug as many as his level allows.
        List<Pit> mine = new ArrayList<>();
        for (Pit other : PITS) {
            if (other.owner.equals(p.getUUID())) {
                mine.add(other);
            }
        }
        if (mine.size() >= c.level) {
            release(mine.get(0));
            PITS.remove(mine.get(0));
        }
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (BlockPos top : columns) {
            for (int d = 1; d <= depth; d++) {
                BlockPos hole = top.below(d).immutable();
                if (TempBlocks.replace(p, level, hole, Blocks.AIR.defaultBlockState(), safety)) {
                    pit.holes.add(hole);
                }
            }
            pit.tops.add(top.immutable());
            pit.topStates.add(level.getBlockState(top));
            minX = Math.min(minX, top.getX());
            minZ = Math.min(minZ, top.getZ());
            maxX = Math.max(maxX, top.getX() + 1);
            maxZ = Math.max(maxZ, top.getZ() + 1);
        }
        if (pit.holes.isEmpty()) {
            c.refuse("message.enlightened.protected");
            return;
        }
        pit.footprint = new AABB(minX, center.getY() + 0.9, minZ, maxX, center.getY() + 1.6, maxZ);
        PITS.add(pit);
        // Only a whisper of dust at the bender's own feet: the pit itself must not show.
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(center)),
                p.getX(), p.getY() + 0.1, p.getZ(), 10, 0.3, 0.02, 0.3, 0.02);
        c.sound(SoundEvents.GRAVEL_BREAK, 0.6F, 0.5F);
        c.say("message.enlightened.earth.pit_ready");
        c.exhaust(0.3F);
        c.cooldown(EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.PIT_COOLDOWN.get() : 200);
    }

    /** Whether the pit is free to swallow this creature: never its bender, his teammates, his pets, or whom he may not hurt. */
    static boolean prey(ServerPlayer owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e.isSpectator() || owner.isAlliedTo(e)) {
            return false;
        }
        if (e instanceof TamableAnimal pet && pet.isOwnedBy(owner)) {
            return false;
        }
        return AreaTargets.mayHurt(owner, e);
    }

    @Override
    public void tick(Cast c) {
        if (PITS.isEmpty()) {
            return;
        }
        ServerPlayer owner = c.player;
        long now = c.now();
        Iterator<Pit> it = PITS.iterator();
        while (it.hasNext()) {
            Pit pit = it.next();
            if (!pit.owner.equals(owner.getUUID())) {
                continue;
            }
            ServerLevel level = pit.level;
            switch (pit.stage) {
                case ARMED -> {
                    if (now > pit.armedUntil) {
                        release(pit);
                        it.remove();
                        continue;
                    }
                    List<LivingEntity> prey = level.getEntitiesOfClass(LivingEntity.class, pit.footprint, e -> prey(owner, e));
                    if (!prey.isEmpty()) {
                        spring(owner, pit, prey, now);
                    }
                }
                case SPRUNG -> {
                    // It strikes once whoever fell has come down (or soon after, whether or not).
                    if (now - pit.since >= FALL && (settled(pit) || now - pit.since >= FALL * 3)) {
                        strike(owner, pit, now);
                    }
                }
                case CLOSED, OPEN -> {
                    if (now - pit.since >= pit.hold) {
                        release(pit);
                        it.remove();
                        continue;
                    }
                    if (pit.stage == Stage.CLOSED && (now - pit.since) % 10 == 0 && !pit.tops.isEmpty()) {
                        // Muffled thumps from under the ground.
                        BlockPos top = pit.tops.get(level.random.nextInt(pit.tops.size()));
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(top)),
                                top.getX() + 0.5, top.getY() + 1.05, top.getZ() + 0.5, 4, 0.3, 0.02, 0.3, 0.02);
                        level.playSound(null, top, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.4F, 0.5F);
                    }
                }
            }
        }
    }

    /** The top caves in under the prey. */
    private static void spring(ServerPlayer owner, Pit pit, List<LivingEntity> prey, long now) {
        ServerLevel level = pit.level;
        int safety = pit.hold + 20 * 30;
        for (int i = 0; i < pit.tops.size(); i++) {
            BlockPos top = pit.tops.get(i);
            BlockState was = level.getBlockState(top);
            if (TempBlocks.replace(owner, level, top, Blocks.AIR.defaultBlockState(), safety)) {
                pit.topStates.set(i, was);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, was), top.getX() + 0.5, top.getY() + 0.6, top.getZ() + 0.5,
                        14, 0.35, 0.3, 0.35, 0.1);
            }
        }
        for (LivingEntity e : prey) {
            e.setDeltaMovement(e.getDeltaMovement().x * 0.2, -0.5, e.getDeltaMovement().z * 0.2);
            e.hurtMarked = true;
            if (e instanceof ServerPlayer victim) {
                victim.displayClientMessage(Component.translatable("message.enlightened.earth.pit_fell").withStyle(ChatFormatting.GOLD), true);
            }
        }
        Vec3 mid = pit.footprint.getCenter();
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.4F, 0.6F);
        level.playSound(null, mid.x, mid.y, mid.z, ModSounds.EARTH_RISE.get(), SoundSource.PLAYERS, 1.2F, 0.5F);
        owner.displayClientMessage(Component.translatable("message.enlightened.earth.pit_sprung", prey.get(0).getDisplayName())
                .withStyle(ChatFormatting.GREEN), true);
        pit.stage = Stage.SPRUNG;
        pit.since = now;
    }

    /** Whether everyone in the pit is down under its lid. */
    private static boolean settled(Pit pit) {
        for (LivingEntity e : pit.level.getEntitiesOfClass(LivingEntity.class, pit.inside(), LivingEntity::isAlive)) {
            if (e.getBoundingBox().maxY > pit.surface - 0.02) {
                return false;
            }
        }
        return true;
    }

    /** The earth strikes what fell in, and closes over its head if it can without shutting anyone in. */
    private static void strike(ServerPlayer owner, Pit pit, long now) {
        ServerLevel level = pit.level;
        boolean fits = pit.depth >= 2;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pit.inside(), LivingEntity::isAlive)) {
            if (prey(owner, e)) {
                Shots.hurt(owner, e, pit.damage, ModDamageTypes.EARTH);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, pit.hold, 3));
            }
            // A head above the lid, or anyone still in the top layer: the lid stays open.
            if (e.getBoundingBox().maxY > pit.surface - 0.02) {
                fits = false;
            }
        }
        if (fits) {
            for (int i = 0; i < pit.tops.size(); i++) {
                TempBlocks.hold(level, pit.tops.get(i), pit.topStates.get(i), pit.hold + 20 * 30);
                BlockPos top = pit.tops.get(i);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, pit.topStates.get(i)), top.getX() + 0.5, top.getY() + 0.9,
                        top.getZ() + 0.5, 10, 0.4, 0.2, 0.4, 0.05);
            }
            for (LivingEntity e : level.getEntitiesOfClass(ServerPlayer.class, pit.inside(), LivingEntity::isAlive)) {
                ((ServerPlayer) e).displayClientMessage(Component.translatable("message.enlightened.earth.pit_closed").withStyle(ChatFormatting.RED), true);
            }
        }
        Vec3 mid = pit.footprint.getCenter();
        level.playSound(null, mid.x, mid.y, mid.z, ModSounds.EARTH_RISE.get(), SoundSource.PLAYERS, 1.3F, fits ? 0.6F : 0.9F);
        level.playSound(null, mid.x, mid.y, mid.z, ModSounds.ROCK_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        pit.stage = fits ? Stage.CLOSED : Stage.OPEN;
        pit.since = now;
    }

    /** Lifts whoever is inside out onto the ground, and puts the earth back. */
    static void release(Pit pit) {
        ServerLevel level = pit.level;
        if (pit.footprint != null && pit.sprung()) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pit.inside().inflate(0.2, 0, 0.2), LivingEntity::isAlive)) {
                lift(pit, e);
            }
            Vec3 mid = pit.footprint.getCenter();
            level.playSound(null, mid.x, mid.y, mid.z, ModSounds.EARTH_RISE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
        }
        for (BlockPos hole : pit.holes) {
            TempBlocks.releaseAt(level, hole);
        }
        for (BlockPos top : pit.tops) {
            TempBlocks.releaseAt(level, top);
        }
    }

    /** The earth spits a creature out onto the ground over the pit. */
    private static void lift(Pit pit, LivingEntity e) {
        double y = pit.surface + 1.05;
        if (e.getY() >= y) {
            return;
        }
        if (e instanceof Player player) {
            player.teleportTo(e.getX(), y, e.getZ());
        } else {
            e.setPos(e.getX(), y, e.getZ());
        }
        e.setDeltaMovement(0, 0.35, 0);
        e.hurtMarked = true;
        e.fallDistance = 0;
        pit.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), e.getX(), y, e.getZ(),
                12, 0.3, 0.1, 0.3, 0.1);
    }

    /** The pits a bender has dug (a GameTest reads them). */
    public static List<Pit> of(UUID owner) {
        List<Pit> out = new ArrayList<>();
        for (Pit pit : PITS) {
            if (pit.owner.equals(owner)) {
                out.add(pit);
            }
        }
        return out;
    }

    /** Whether a block is the closed lid of a pit (it cannot be broken while it holds someone). */
    public static boolean isLid(Level level, BlockPos pos) {
        for (Pit pit : PITS) {
            if (pit.stage == Stage.CLOSED && pit.level == level && pit.tops.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    /** A player leaves the game: if he is in a pit, he is set on the ground first, so he never comes back inside earth. */
    public static void leave(ServerPlayer player) {
        for (Pit pit : PITS) {
            if (pit.footprint != null && pit.level == player.level() && pit.sprung() && pit.inside().contains(player.position())) {
                lift(pit, player);
            }
        }
    }

    /** The server stops: every pit gives back whom it holds and fills in, before the raised blocks are put back. */
    public static void releaseAll(MinecraftServer server) {
        for (Pit pit : new ArrayList<>(PITS)) {
            release(pit);
        }
        PITS.clear();
    }

    private static void releaseOwned(UUID owner) {
        Iterator<Pit> it = PITS.iterator();
        while (it.hasNext()) {
            Pit pit = it.next();
            if (pit.owner.equals(owner)) {
                release(pit);
                it.remove();
            }
        }
    }

    @Override
    public void end(Cast c) {
        releaseOwned(c.player.getUUID());
    }

    @Override
    public void forget(ServerPlayer player) {
        releaseOwned(player.getUUID());
    }

    @Override
    public void clear() {
        PITS.clear();
    }
}
