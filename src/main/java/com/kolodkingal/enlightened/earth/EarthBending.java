package com.kolodkingal.enlightened.earth;

import com.kolodkingal.enlightened.fx.Fx;
import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.BlockRights;
import com.kolodkingal.enlightened.bending.Cast;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.Raycasts;
import com.kolodkingal.enlightened.bending.Shots;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.network.ModNetwork;
import com.kolodkingal.enlightened.network.SeismicPayload;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Earthbending: rock thrown from the ground and rock lifted and held, earth armour, a pillar under the bender's feet,
 * a wall in front of him, the seismic sense; and the sub-bending arts: sand (a wall, sand in the air, quicksand),
 * lava (a path, a shuriken, a lavabender's skin) and metal (a wall, blades, full armour, the pull, wrecking devices).
 */
public final class EarthBending {
    private static final ResourceLocation ARMOR_ID = Enlightened.id("earth_armor");
    private static final ResourceLocation TOUGHNESS_ID = Enlightened.id("earth_armor_toughness");
    private static final ResourceLocation KNOCKBACK_ID = Enlightened.id("earth_armor_knockback");
    private static final ResourceLocation SLOW_ID = Enlightened.id("earth_armor_slow");

    /** The rocks each bender holds in the air, in the order he lifted them. */
    private static final Map<UUID, List<Integer>> HELD = new HashMap<>();

    public static void register(Map<Ability, AbilityLogic> logic) {
        logic.put(Ability.ROCK_THROW, new RockThrow());
        logic.put(Ability.ROCK_LIFT, new RockLift());
        logic.put(Ability.EARTH_ARMOR, new Armor(false));
        logic.put(Ability.METAL_ARMOR, new Armor(true));
        logic.put(Ability.EARTH_PILLAR, new Pillar());
        logic.put(Ability.EARTH_WALL, new Wall(Wall.Material.EARTH));
        logic.put(Ability.SAND_WALL, new Wall(Wall.Material.SAND));
        logic.put(Ability.METAL_WALL, new Wall(Wall.Material.METAL));
        logic.put(Ability.SEISMIC_SENSE, new Seismic());
        logic.put(Ability.SAND_LIFT, new SandLift());
        logic.put(Ability.QUICKSAND, new Quicksand());
        logic.put(Ability.LAVA_PATH, new LavaPath());
        logic.put(Ability.LAVA_SHURIKEN, new LavaShuriken());
        logic.put(Ability.LAVA_SKIN, new AbilityLogic() {
            @Override
            public void key(Cast c, boolean down) {
            }
        });
        logic.put(Ability.METAL_BLADES, new MetalBlades());
        logic.put(Ability.METAL_PULL, new MetalPull());
        logic.put(Ability.WRECK, new Wreck());
        EarthMore.register(logic);
    }

    // ---- the body: earth and metal armour ----

    public static void applyBody(ServerPlayer player, BendingData data) {
        boolean metal = data.has(Ability.METAL_ARMOR) && data.on(Ability.METAL_ARMOR);
        boolean earth = !metal && data.has(Ability.EARTH_ARMOR) && data.on(Ability.EARTH_ARMOR);
        boolean any = metal || earth;
        int level = metal ? data.level(Ability.METAL_ARMOR) : data.level(Ability.EARTH_ARMOR);
        double armor = metal ? 12 + level * 4 : 2 + level * 4;           // earth: 6/10/14, metal: 16/20/24
        double toughness = metal ? 4 + level * 2 : level * 2;            // earth: 2/4/6, metal: 6/8/10
        double slowBase = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.EARTH_ARMOR_SLOW.get() : 0.3;
        double slow = -(metal ? slowBase + 0.1 : slowBase) + (level - 1) * 0.05; // less slow as he masters it
        BendingTicker.setModifier(player, Attributes.ARMOR, ARMOR_ID, armor, any);
        BendingTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, toughness, any);
        BendingTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, metal ? 0.6 : 0.3, any);
        BendingTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SLOW_ID, slow, any, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Whether the player wears the earth or the metal armour (the client draws the shell). */
    public static boolean armored(BendingData data) {
        return data.has(Ability.EARTH_ARMOR) && data.on(Ability.EARTH_ARMOR) || data.has(Ability.METAL_ARMOR) && data.on(Ability.METAL_ARMOR);
    }

    /** How much of the fire damage a lavabender's skin takes away (0 to 0.75). */
    public static float lavaSkin(BendingData data) {
        if (!data.has(Ability.LAVA_SKIN)) {
            return 0F;
        }
        return 0.25F * data.level(Ability.LAVA_SKIN);
    }

    // ---- ground ----

    /** The earth block under or just ahead of the bender's feet (within 3 blocks down), or null if there is none. */
    @Nullable
    static BlockPos groundNear(ServerLevel level, Vec3 at) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(at).mutable();
        for (int i = 0; i < 4; i++) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModTags.EARTH)) {
                return pos.immutable();
            }
            pos.move(0, -1, 0);
        }
        return null;
    }

    /** The top earth cell at this column, from {@code y + 2} down to {@code y - 3}, or null. */
    @Nullable
    static BlockPos topEarth(ServerLevel level, int x, int y, int z) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = new BlockPos(x, y + dy, z);
            if (level.getBlockState(pos).is(ModTags.EARTH) && !level.getBlockState(pos.above()).isSolid()) {
                return pos;
            }
        }
        return null;
    }

    /** Whether sand is close (any sand block within {@code radius}). */
    static boolean sandNear(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (level.getBlockState(pos).is(ModTags.SAND)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the bender has metal to bend: a metal block within 8, or metal in his inventory. */
    static boolean metalNear(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModTags.METAL_ITEMS)) {
                return true;
            }
        }
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            if (level.getBlockState(pos).is(ModTags.METAL)) {
                return true;
            }
        }
        return false;
    }

    static boolean requireMetal(Cast c) {
        if (metalNear(c.player)) {
            return true;
        }
        c.refuse("message.enlightened.earth.no_metal");
        return false;
    }

    // ---- shots ----

    static BendingShotEntity shoot(Cast c, BendingShotEntity.Kind kind, Vec3 from, Vec3 to, float damage, int touch, double speed) {
        BendingShotEntity shot = new BendingShotEntity(c.level(), kind, from, c.player, c.color());
        shot.setDamage(damage);
        shot.setTouch(touch);
        shot.launch(to.subtract(from), speed);
        c.level().addFreshEntity(shot);
        return shot;
    }

    static double rockRange() {
        return EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.ROCK_RANGE.get() : 24;
    }

    // ---- Rock Throw: rocks torn from the ground in front and thrown; more of them as he masters it ----

    static final class RockThrow implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            Vec3 facing = Shots.facing(p);
            List<BlockPos> grounds = new ArrayList<>();
            int count = c.level;
            for (int i = 0; i < count; i++) {
                double side = (i - (count - 1) / 2.0) * 1.2;
                Vec3 at = p.position().add(facing.scale(1.5)).add(-facing.z * side, 0, facing.x * side);
                BlockPos ground = groundNear(level, at);
                if (ground != null) {
                    grounds.add(ground);
                }
            }
            if (grounds.isEmpty()) {
                c.refuse("message.enlightened.earth.no_earth");
                return;
            }
            Shots.Aim aim = Shots.aim(p, rockRange());
            float damage = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.ROCK_DAMAGE.get() : 4.0) + c.level;
            for (BlockPos ground : grounds) {
                BlockState state = level.getBlockState(ground);
                Vec3 from = Vec3.atCenterOf(ground).add(0, 1.0, 0);
                BendingShotEntity shot = shoot(c, BendingShotEntity.Kind.ROCK, from, aim.end(), damage, 0, 1.3);
                shot.setBlockState(state);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), from.x, from.y, from.z, 12, 0.3, 0.2, 0.3, 0.1);
            }
            c.sound(ModSounds.ROCK_THROW.get(), 1.0F, 0.9F + c.level * 0.05F);
            c.exhaust(0.2F);
            c.cooldown(EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.ROCK_COOLDOWN.get() : 20);
        }
    }

    // ---- Rock Lift: a block torn out of the ground and held; up to three at once; thrown together ----

    static List<Integer> held(ServerPlayer p) {
        return HELD.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
    }

    /** The shots the bender still holds (dead ones are forgotten). */
    static List<BendingShotEntity> heldShots(ServerPlayer p) {
        List<Integer> ids = held(p);
        List<BendingShotEntity> shots = new ArrayList<>();
        ids.removeIf(id -> {
            Entity e = p.serverLevel().getEntity(id);
            if (e instanceof BendingShotEntity shot && shot.isAlive() && shot.held()) {
                shots.add(shot);
                return false;
            }
            return true;
        });
        return shots;
    }

    static final class RockLift implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            List<BendingShotEntity> shots = heldShots(p);
            BlockHitResult hit = Raycasts.block(p, rockRange(), 1.0F);
            boolean atEarth = hit.getType() == HitResult.Type.BLOCK && level.getBlockState(hit.getBlockPos()).is(ModTags.EARTH);
            if (!shots.isEmpty() && (!atEarth || shots.size() >= c.level)) {
                launchAll(c, shots);
                return;
            }
            if (!atEarth) {
                c.refuse("message.enlightened.earth.no_earth_there");
                return;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            boolean take = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.ROCK_LIFT_TAKES_BLOCKS.get();
            if (take && !BlockRights.mayBreak(p, level, pos, state)) {
                c.refuse("message.enlightened.protected");
                return;
            }
            if (take) {
                level.destroyBlock(pos, false, p);
            }
            BendingShotEntity shot = new BendingShotEntity(level, BendingShotEntity.Kind.ROCK, Vec3.atCenterOf(pos), p, c.color());
            shot.setBlockState(state);
            shot.setDropsItem(take);
            shot.setDamage((float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.ROCK_DAMAGE.get() : 4.0) + 2 + c.level);
            shot.hold(shots.size());
            level.addFreshEntity(shot);
            held(p).add(shot.getId());
            c.data.setHeldEntityId(shot.getId());
            c.setActive(true);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.1);
            c.sound(ModSounds.EARTH_RISE.get(), 0.8F, 1.2F);
            c.exhaust(0.1F);
        }

        private void launchAll(Cast c, List<BendingShotEntity> shots) {
            Shots.Aim aim = Shots.aim(c.player, rockRange());
            for (BendingShotEntity shot : shots) {
                shot.launch(aim.end().subtract(shot.position()), 1.4);
            }
            held(c.player).clear();
            c.data.setHeldEntityId(-1);
            c.setActive(false);
            c.sound(ModSounds.ROCK_THROW.get(), 1.0F, 0.8F);
            c.exhaust(0.2F);
        }

        @Override
        public void tick(Cast c) {
            if (c.active() && heldShots(c.player).isEmpty()) {
                c.setActive(false);
                c.data.setHeldEntityId(-1);
            }
        }

        @Override
        public void end(Cast c) {
            for (BendingShotEntity shot : heldShots(c.player)) {
                shot.drop();
            }
            held(c.player).clear();
            c.data.setHeldEntityId(-1);
        }

        @Override
        public void forget(ServerPlayer player) {
            HELD.remove(player.getUUID());
        }

        @Override
        public void clear() {
            HELD.clear();
        }
    }

    // ---- Sand Lift: sand drawn into the air round him and thrown as a blinding cloud ----

    static final class SandLift implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (!sandNear(c.level(), p.blockPosition(), 4)) {
                c.refuse("message.enlightened.earth.no_sand");
                return;
            }
            Shots.Aim aim = Shots.aim(p, rockRange());
            for (int i = 0; i < c.level; i++) {
                Vec3 from = p.position().add((p.getRandom().nextDouble() - 0.5) * 2, 0.3, (p.getRandom().nextDouble() - 0.5) * 2);
                BendingShotEntity shot = shoot(c, BendingShotEntity.Kind.SAND, from, aim.end().add(0, i * 0.3, 0), 2.0F + c.level, 3, 1.2);
                shot.setBlockState(Blocks.SAND.defaultBlockState());
            }
            c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), p.getX(), p.getY() + 0.5, p.getZ(), 30, 1.0, 0.5, 1.0, 0.1);
            c.sound(ModSounds.SAND_HISS.get(), 1.0F, 1.0F);
            c.exhaust(0.15F);
            c.cooldownSeconds(1.5);
        }
    }

    // ---- Earth and Metal Armour ----

    static final class Armor implements AbilityLogic {
        private final boolean metal;

        Armor(boolean metal) {
            this.metal = metal;
        }

        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            boolean on = !c.on();
            if (on && metal && !requireMetal(c)) {
                return;
            }
            if (on && !metal && groundNear(c.level(), c.player.position()) == null) {
                c.refuse("message.enlightened.earth.no_earth");
                return;
            }
            c.setOn(on);
            if (on) {
                // One armour at a time: the metal one replaces the earth one and the other way round.
                c.data.setOn(metal ? Ability.EARTH_ARMOR : Ability.METAL_ARMOR, false);
            }
            BendingTicker.applyPassives(c.player, c.data);
            BlockState shell = metal ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
            c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, shell), c.player.getX(), c.player.getY() + 1, c.player.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
            c.sound(metal ? ModSounds.METAL_CLANG.get() : ModSounds.EARTH_ARMOR.get(), 1.0F, on ? 1.0F : 0.7F);
            c.say(on ? (metal ? "message.enlightened.earth.metal_armor_on" : "message.enlightened.earth.armor_on")
                    : "message.enlightened.earth.armor_off");
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
        }
    }

    // ---- Earth Pillar: the ground rises under his feet ----

    static final class Pillar implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            BlockPos ground = groundNear(level, p.position().add(0, -0.1, 0));
            if (ground == null || !p.onGround()) {
                c.refuse("message.enlightened.earth.no_earth");
                return;
            }
            BlockState state = level.getBlockState(ground);
            int height = c.by(3, 5, 8);
            int seconds = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.RAISED_BLOCK_SECONDS.get() : 10;
            BlockPos foot = p.blockPosition();
            int placed = 0;
            for (int i = 0; i < height; i++) {
                BlockPos pos = foot.above(i);
                if (!level.getBlockState(pos.above()).isAir() && i > 0 && level.getBlockState(pos.above()).isSolid()) {
                    break; // a ceiling
                }
                if (!TempBlocks.place(p, level, pos, state, seconds * 20 + i * 2)) {
                    break;
                }
                placed++;
            }
            if (placed == 0) {
                c.refuse("message.enlightened.protected");
                return;
            }
            p.teleportTo(p.getX(), foot.getY() + placed, p.getZ());
            p.setDeltaMovement(0, 0.3, 0);
            p.hurtMarked = true;
            p.fallDistance = 0;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX(), foot.getY() + placed * 0.5, p.getZ(), 30, 0.5, placed * 0.5, 0.5, 0.1);
            Fx.at(level, Fx.EARTH_RISE, Vec3.atBottomCenterOf(foot), c.color(), 2.0F);
            c.sound(ModSounds.EARTH_RISE.get(), 1.2F, 0.8F);
            c.exhaust(0.2F);
            c.cooldownSeconds(6);
        }
    }

    // ---- Walls of earth, sand and metal ----

    static final class Wall implements AbilityLogic {
        enum Material { EARTH, SAND, METAL }

        private final Material material;

        Wall(Material material) {
            this.material = material;
        }

        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            if (material == Material.METAL && !requireMetal(c)) {
                return;
            }
            if (material == Material.SAND && !sandNear(level, p.blockPosition(), 6)) {
                c.refuse("message.enlightened.earth.no_sand");
                return;
            }
            Vec3 facing = Shots.facing(p);
            Vec3 side = new Vec3(-facing.z, 0, facing.x);
            int width = c.by(3, 5, 7);
            int height = 3;
            int seconds = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.WALL_SECONDS.get() : 10;
            Vec3 center = p.position().add(facing.scale(2.5));
            int placed = 0;
            BlockState last = Blocks.STONE.defaultBlockState();
            for (int w = 0; w < width; w++) {
                double offset = w - (width - 1) / 2.0;
                Vec3 column = center.add(side.scale(offset));
                BlockPos foot = BlockPos.containing(column);
                BlockPos ground = topEarth(level, foot.getX(), foot.getY() - 1, foot.getZ());
                if (ground == null) {
                    if (material != Material.EARTH) {
                        ground = foot.below();
                        if (!level.getBlockState(ground).isSolid()) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                BlockState state = switch (material) {
                    case EARTH -> level.getBlockState(ground).is(ModTags.EARTH) ? level.getBlockState(ground) : Blocks.STONE.defaultBlockState();
                    case SAND -> Blocks.SAND.defaultBlockState();
                    case METAL -> Blocks.IRON_BLOCK.defaultBlockState();
                };
                last = state;
                for (int h = 1; h <= height; h++) {
                    BlockPos pos = ground.above(h);
                    if (p.getBoundingBox().intersects(new AABB(pos))) {
                        continue;
                    }
                    if (TempBlocks.place(p, level, pos, state, seconds * 20 + w * 2)) {
                        placed++;
                    } else {
                        break;
                    }
                }
            }
            if (placed == 0) {
                c.refuse(material == Material.EARTH ? "message.enlightened.earth.no_earth" : "message.enlightened.protected");
                return;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, last), center.x, center.y + 1, center.z, 40, width * 0.4, 1.0, width * 0.4, 0.1);
            Fx.at(level, Fx.EARTH_RISE, center, c.color(), width * 0.6F);
            c.sound(material == Material.METAL ? ModSounds.METAL_CLANG.get() : material == Material.SAND ? ModSounds.SAND_HISS.get()
                    : ModSounds.EARTH_RISE.get(), 1.2F, 1.0F);
            c.exhaust(0.3F);
            c.cooldown(EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.WALL_COOLDOWN.get() : 200);
        }
    }

    // ---- Seismic Sense: on the ground, every step tells him where everyone stands ----

    static final class Seismic implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down) {
                return;
            }
            c.setOn(!c.on());
            c.sound(ModSounds.SEISMIC.get(), 0.8F, c.on() ? 1.0F : 0.7F);
            c.say(c.on() ? "message.enlightened.earth.seismic_on" : "message.enlightened.earth.seismic_off");
        }

        @Override
        public void tick(Cast c) {
            ServerPlayer p = c.player;
            if (!c.on() || p.tickCount % 10 != 0) {
                return;
            }
            boolean grounded = p.onGround() && !p.getAbilities().flying;
            c.setActive(grounded);
            if (!grounded) {
                return;
            }
            double range = c.by(12, 20, 32);
            List<Integer> ids = new ArrayList<>();
            for (Entity e : c.level().getEntities(p, p.getBoundingBox().inflate(range), e -> e instanceof LivingEntity && e.isAlive() && e != p)) {
                if (e.onGround() || e.isInWater() || e.getY() - p.getY() < 2) {
                    ids.add(e.getId());
                    if (ids.size() >= 48) {
                        break;
                    }
                }
            }
            ModNetwork.sendTo(p, new SeismicPayload(ids, 12));
            if (p.tickCount % 40 == 0) {
                Fx.at(c.level(), Fx.SEISMIC, p.position(), c.color(), 8.0F);
            }
        }

        @Override
        public void end(Cast c) {
            c.setOn(false);
            c.setActive(false);
        }
    }

    // ---- Quicksand: the ground under an enemy swallows him (unless he is the stronger earthbender) ----

    static final class Quicksand implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, 16);
            LivingEntity target = aim.target();
            if (target == null) {
                c.refuse("message.enlightened.no_target");
                return;
            }
            if (!com.kolodkingal.enlightened.bending.AreaTargets.mayHurt(p, target)) {
                c.refuse("message.enlightened.not_allowed");
                return;
            }
            ServerLevel level = c.level();
            BlockPos under = target.blockPosition().below();
            BlockState ground = level.getBlockState(under);
            boolean sand = ground.is(ModTags.SAND);
            if (!sand && !(c.level >= 2 && ground.is(ModTags.EARTH))) {
                c.refuse("message.enlightened.earth.no_sand_under");
                return;
            }
            if (target instanceof ServerPlayer other && com.kolodkingal.enlightened.bending.Benders.is(other, com.kolodkingal.enlightened.bending.Element.EARTH)
                    && Mastery.strength(other) >= Mastery.strength(p)) {
                c.refuse("message.enlightened.earth.quicksand_stronger", other.getDisplayName());
                c.cooldownSeconds(3);
                return;
            }
            if (!sand) {
                // An adept turns the earth under him to sand for the while.
                TempBlocks.place(p, level, under, Blocks.SAND.defaultBlockState(), c.by(4, 6, 8) * 20 + 20);
            }
            target.addEffect(new MobEffectInstance(ModEffects.QUICKSAND, c.by(4, 6, 8) * 20, c.level - 1));
            Shots.hurt(p, target, 1.0F, ModDamageTypes.EARTH);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), target.getX(), target.getY() + 0.2, target.getZ(), 30, 0.6, 0.2, 0.6, 0.1);
            Fx.around(level, Fx.QUICKSAND, target, c.color(), 1.0F);
            c.sound(ModSounds.SAND_HISS.get(), 1.0F, 0.7F);
            c.exhaust(0.2F);
            c.cooldownSeconds(10);
        }
    }

    // ---- Lava Path: the ground ahead turns to lava for a while ----

    static final class LavaPath implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            Vec3 facing = Shots.facing(p);
            int length = c.by(4, 7, 10);
            int seconds = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.LAVA_SECONDS.get() : 8;
            int placed = 0;
            BlockPos last = null;
            for (int d = 2; d < 2 + length; d++) {
                Vec3 at = p.position().add(facing.scale(d));
                BlockPos foot = BlockPos.containing(at);
                BlockPos ground = topEarth(level, foot.getX(), foot.getY() - 1, foot.getZ());
                if (ground == null) {
                    continue;
                }
                if (!BlockRights.mayBreak(p, level, ground, level.getBlockState(ground))) {
                    continue;
                }
                if (TempBlocks.place(p, level, ground, Blocks.LAVA.defaultBlockState(), seconds * 20 + (d * 3))) {
                    placed++;
                    last = ground;
                    level.sendParticles(ParticleTypes.LAVA, ground.getX() + 0.5, ground.getY() + 1.1, ground.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.0);
                }
            }
            if (placed == 0) {
                c.refuse("message.enlightened.earth.no_earth_there");
                return;
            }
            // The lavabender does not burn in the lava he makes.
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, seconds * 20 + 60, 0, false, false, true));
            c.sound(ModSounds.LAVA_FLOW.get(), 1.2F, 1.0F);
            c.exhaust(0.4F);
            c.cooldownSeconds(15);
        }
    }

    // ---- Lava Shuriken ----

    static final class LavaShuriken implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, rockRange());
            BendingShotEntity shot = shoot(c, BendingShotEntity.Kind.LAVA, Shots.hand(p), aim.end(), 5.0F + c.level, c.by(3, 4, 6), 1.3);
            shot.setBlockState(Blocks.MAGMA_BLOCK.defaultBlockState());
            c.level().sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            c.sound(ModSounds.LAVA_FLOW.get(), 0.8F, 1.4F);
            c.exhaust(0.15F);
            c.cooldownSeconds(c.by(2.5, 2.0, 1.5));
        }
    }

    // ---- Metal Blades ----

    static final class MetalBlades implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready() || !requireMetal(c)) {
                return;
            }
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, rockRange());
            float damage = (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.METAL_BLADE_DAMAGE.get() : 6.0) + c.level;
            for (int i = 0; i < c.level; i++) {
                Vec3 from = Shots.hand(p, i % 2 == 1).add(0, (i / 2) * 0.3, 0);
                shoot(c, BendingShotEntity.Kind.METAL_BLADE, from, aim.end(), damage, 0, 1.6);
            }
            c.sound(ModSounds.METAL_CLANG.get(), 1.0F, 1.5F);
            c.exhaust(0.15F);
            c.cooldownSeconds(1.5);
        }
    }

    // ---- Metal Pull: the metal in the ground and on it comes to him ----

    static final class MetalPull implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            int base = EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.METAL_PULL_RADIUS.get() : 6;
            int radius = base + (c.level - 1) * 3;
            int max = c.by(8, 16, 32);
            int taken = 0;
            BlockPos center = p.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
                if (taken >= max) {
                    break;
                }
                BlockState state = level.getBlockState(pos);
                if (!state.is(ModTags.METAL_ORES) || !pos.closerThan(center, radius)) {
                    continue;
                }
                if (!BlockRights.mayBreak(p, level, pos.immutable(), state)) {
                    continue;
                }
                ItemStack metal = oreGives(state, p);
                BlockState left = oreLeaves(state);
                level.setBlock(pos, left, 3);
                give(p, metal);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                taken++;
            }
            // Loose metal on the ground comes too.
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(radius), i -> i.getItem().is(ModTags.METAL_ITEMS))) {
                if (taken >= max) {
                    break;
                }
                give(p, item.getItem().copy());
                item.discard();
                taken++;
            }
            if (taken == 0) {
                c.refuse("message.enlightened.earth.no_metal_around");
                c.cooldownSeconds(1);
                return;
            }
            Fx.at(level, Fx.METAL_PULL, p.position().add(0, 0.2, 0), c.color(), (float) radius);
            c.sound(ModSounds.METAL_CLANG.get(), 1.0F, 0.6F);
            c.say("message.enlightened.earth.metal_pulled", taken);
            c.exhaust(0.5F);
            c.cooldownSeconds(8);
        }

        private static ItemStack oreGives(BlockState ore, ServerPlayer p) {
            int roll = p.getRandom().nextInt(4);
            if (ore.is(Blocks.IRON_ORE) || ore.is(Blocks.DEEPSLATE_IRON_ORE)) {
                return new ItemStack(Items.RAW_IRON);
            }
            if (ore.is(Blocks.GOLD_ORE) || ore.is(Blocks.DEEPSLATE_GOLD_ORE)) {
                return new ItemStack(Items.RAW_GOLD);
            }
            if (ore.is(Blocks.COPPER_ORE) || ore.is(Blocks.DEEPSLATE_COPPER_ORE)) {
                return new ItemStack(Items.RAW_COPPER, 2 + roll);
            }
            if (ore.is(Blocks.NETHER_GOLD_ORE)) {
                return new ItemStack(Items.GOLD_NUGGET, 2 + roll);
            }
            if (ore.is(Blocks.ANCIENT_DEBRIS)) {
                return new ItemStack(Items.NETHERITE_SCRAP);
            }
            return new ItemStack(Items.IRON_NUGGET);
        }

        private static BlockState oreLeaves(BlockState ore) {
            if (ore.is(Blocks.DEEPSLATE_IRON_ORE) || ore.is(Blocks.DEEPSLATE_GOLD_ORE) || ore.is(Blocks.DEEPSLATE_COPPER_ORE)) {
                return Blocks.DEEPSLATE.defaultBlockState();
            }
            if (ore.is(Blocks.NETHER_GOLD_ORE) || ore.is(Blocks.ANCIENT_DEBRIS)) {
                return Blocks.NETHERRACK.defaultBlockState();
            }
            return Blocks.STONE.defaultBlockState();
        }

        private static void give(ServerPlayer p, ItemStack stack) {
            if (!p.getInventory().add(stack)) {
                p.drop(stack, false);
            }
        }
    }

    // ---- Wreck: metal devices (furnaces, pistons, rails, hoppers...) torn apart; golems and minecarts too ----

    static final class Wreck implements AbilityLogic {
        @Override
        public void key(Cast c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            Shots.Aim aim = Shots.aim(p, 12);
            if (aim.target() instanceof IronGolem golem) {
                Shots.hurt(p, golem, 6.0F + c.level * 2, ModDamageTypes.EARTH);
                level.sendParticles(ParticleTypes.CRIT, golem.getX(), golem.getY() + 1, golem.getZ(), 20, 0.5, 0.8, 0.5, 0.1);
                c.sound(ModSounds.METAL_WRECK.get(), 1.0F, 0.8F);
                c.cooldownSeconds(5);
                return;
            }
            BlockHitResult hit = Raycasts.block(p, 12, 1.0F);
            int max = c.by(1, 3, 6);
            int wrecked = 0;
            boolean drops = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.WRECK_DROPS.get();
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos center = hit.getBlockPos();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                    if (wrecked >= max) {
                        break;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(ModTags.METAL_DEVICES) || !BlockRights.mayBreak(p, level, pos.immutable(), state)) {
                        continue;
                    }
                    level.destroyBlock(pos, drops, p);
                    level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.1);
                    wrecked++;
                }
            }
            for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, p.getBoundingBox().inflate(12))) {
                if (wrecked >= max) {
                    break;
                }
                if (cart.position().subtract(p.getEyePosition()).normalize().dot(p.getLookAngle()) > 0.9) {
                    cart.hurt(ModDamageTypes.source(level, ModDamageTypes.EARTH, p), 50.0F);
                    wrecked++;
                }
            }
            if (wrecked == 0) {
                c.refuse("message.enlightened.earth.nothing_to_wreck");
                return;
            }
            c.sound(ModSounds.METAL_WRECK.get(), 1.0F, 1.0F);
            c.exhaust(0.2F);
            c.cooldownSeconds(5);
        }
    }

    /** Whether the bender holds rocks in the air right now. */
    public static int heldCount(Player player) {
        List<Integer> ids = HELD.get(player.getUUID());
        return ids == null ? 0 : ids.size();
    }

    private EarthBending() {
    }
}
