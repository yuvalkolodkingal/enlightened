package com.kolodkingal.enlightened.water;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.AbilityLogic;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Cast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Plant Giant, the swamp's waterbending: the bender draws the water through every plant round him and wraps
 * himself in them, a giant of vines and leaves. Bigger, stronger and harder to hurt (more so as he masters it), but
 * only where plants grow thick ({@link #needed} within {@link #RADIUS} blocks to begin); the plants take the blows
 * (its {@link #vigor}), and fire, which they fear, hurts the giant more and tears it apart three times as fast. When its
 * vigour is gone the plants fall away. Among plants it grows back slowly.
 */
public final class PlantGiant implements AbilityLogic {
    public static final int RADIUS = 8;
    private static final ResourceLocation SCALE_ID = Enlightened.id("plant_giant_scale");
    private static final ResourceLocation DAMAGE_ID = Enlightened.id("plant_giant_damage");
    private static final ResourceLocation ARMOR_ID = Enlightened.id("plant_giant_armor");
    private static final ResourceLocation KNOCKBACK_ID = Enlightened.id("plant_giant_knockback");
    private static final ResourceLocation SPEED_ID = Enlightened.id("plant_giant_speed");
    private static final ResourceLocation REACH_ID = Enlightened.id("plant_giant_reach");

    /** How much of the giant is left (its plants), per bender. */
    private static final Map<UUID, Float> VIGOR = new HashMap<>();

    /** The plants needed nearby to raise the giant, by level. */
    public static int needed(int level) {
        return switch (level) {
            case 1 -> 40;
            case 2 -> 32;
            default -> 24;
        };
    }

    public static float maxVigor(int level) {
        return 30.0F + 15.0F * level;
    }

    /** Whether a block is a plant the giant can be made of. */
    public static boolean plant(BlockState s) {
        return s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS)
                || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
                || s.is(Blocks.VINE) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.LILY_PAD)
                || s.is(Blocks.SEAGRASS) || s.is(Blocks.TALL_SEAGRASS) || s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT)
                || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.BAMBOO) || s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)
                || s.is(Blocks.GLOW_LICHEN) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.MANGROVE_ROOTS) || s.is(Blocks.HANGING_ROOTS)
                || s.is(Blocks.AZALEA) || s.is(Blocks.FLOWERING_AZALEA) || s.is(Blocks.DEAD_BUSH);
    }

    /** The plants round a place, counted up to {@code cap}. */
    public static int plantsNear(ServerLevel level, BlockPos center, int cap) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -3, -RADIUS), center.offset(RADIUS, 5, RADIUS))) {
            if (plant(level.getBlockState(pos)) && ++n >= cap) {
                return n;
            }
        }
        return n;
    }

    public static boolean wearing(BendingData data) {
        return data.has(Ability.PLANT_GIANT) && data.on(Ability.PLANT_GIANT);
    }

    public static float vigor(ServerPlayer p) {
        return VIGOR.getOrDefault(p.getUUID(), 0.0F);
    }

    @Override
    public void key(Cast c, boolean down) {
        if (!down) {
            return;
        }
        ServerPlayer p = c.player;
        if (c.on()) {
            fall(c, false);
            return;
        }
        if (!c.ready()) {
            return;
        }
        int need = needed(c.level);
        int have = plantsNear(c.level(), p.blockPosition(), need);
        if (have < need) {
            c.refuse("message.enlightened.plant_giant.few", have, need);
            return;
        }
        c.setOn(true);
        VIGOR.put(p.getUUID(), maxVigor(c.level));
        apply(p, c.data);
        burst(c.level(), p, 60);
        c.sound(SoundEvents.AZALEA_LEAVES_PLACE, 1.4F, 0.6F);
        c.say("message.enlightened.plant_giant.on");
    }

    @Override
    public void tick(Cast c) {
        ServerPlayer p = c.player;
        if (!c.on()) {
            return;
        }
        c.setActive(true);
        VIGOR.putIfAbsent(p.getUUID(), maxVigor(c.level));
        if (p.tickCount % 40 == 0) {
            // Among plants it grows back; it costs the bender his strength to hold it.
            if (plantsNear(c.level(), p.blockPosition(), 12) >= 12) {
                VIGOR.put(p.getUUID(), Math.min(maxVigor(c.level), vigor(p) + 2.0F));
            }
            c.exhaust(0.15F);
        }
        if (p.tickCount % 10 == 0) {
            c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                    p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ(), 3, 0.5, p.getBbHeight() * 0.3, 0.5, 0.02);
        }
    }

    /**
     * The giant takes a blow: what is left of the blow, and its vigour spent (fire burns through it three times as
     * fast and hurts half again as much). At no vigour the plants fall away.
     */
    public static float absorb(ServerPlayer p, BendingData data, float amount, boolean fire) {
        if (!wearing(data)) {
            return amount;
        }
        float hit = fire ? amount * 1.5F : amount;
        float left = vigor(p) - (fire ? hit * 3.0F : hit);
        VIGOR.put(p.getUUID(), left);
        if (fire) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.FIRE_AMBIENT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if (left <= 0) {
            Cast c = new Cast(p, data, Ability.PLANT_GIANT);
            fall(c, true);
            return hit;
        }
        // The plants take most of a blow, but not the burning.
        return fire ? hit : hit * (0.55F - 0.05F * data.level(Ability.PLANT_GIANT));
    }

    /** The plants fall away (torn apart, or let go). */
    static void fall(Cast c, boolean torn) {
        ServerPlayer p = c.player;
        c.setOn(false);
        c.setActive(false);
        VIGOR.remove(p.getUUID());
        apply(p, c.data);
        burst(c.level(), p, 40);
        c.sound(SoundEvents.AZALEA_LEAVES_BREAK, 1.4F, 0.7F);
        if (torn) {
            c.say("message.enlightened.plant_giant.torn");
        }
        c.cooldown(c.by(300, 240, 200));
    }

    /** What the giant gives while it stands: size, blows, armour, weight; a little slower. */
    public static void apply(ServerPlayer p, BendingData data) {
        boolean on = wearing(data);
        int lvl = Math.max(1, data.level(Ability.PLANT_GIANT));
        BendingTicker.setModifier(p, Attributes.SCALE, SCALE_ID, 0.5 + 0.25 * lvl, on, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        BendingTicker.setModifier(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, 2.0 + 2.0 * lvl, on);
        BendingTicker.setModifier(p, Attributes.ARMOR, ARMOR_ID, 4.0 + 2.0 * lvl, on);
        BendingTicker.setModifier(p, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.6, on);
        BendingTicker.setModifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, -0.1, on, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        BendingTicker.setModifier(p, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, 1.0 + lvl * 0.5, on);
    }

    private static void burst(ServerLevel level, ServerPlayer p, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                p.getX(), p.getY() + 1.2, p.getZ(), count, 0.8, 1.2, 0.8, 0.1);
        level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1.2, p.getZ(), count / 2, 0.8, 1.0, 0.8, 0.1);
    }

    @Override
    public void end(Cast c) {
        c.setOn(false);
        c.setActive(false);
        VIGOR.remove(c.player.getUUID());
        apply(c.player, c.data);
    }

    @Override
    public void forget(ServerPlayer player) {
        VIGOR.remove(player.getUUID());
    }

    @Override
    public void clear() {
        VIGOR.clear();
    }
}
