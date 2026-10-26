package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.EnlightenedConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Who an attack that hits everything in an area may hurt: a fire ring, an air blast, a tornado, a water spout.
 * Without a rule they would kill the villagers, golems, farm animals and pets round a base, so each of them asks
 * {@link #mayHurt} for every creature in reach.
 *
 * <p>The rule: never the caster, a Creative or Spectator player or an armour stand; a player only if PvP is on and
 * the caster may harm them. With {@code general.hurtEverything} (on by default) every other creature is fair game:
 * the bending goes where it is sent, through pets, teammates, villagers and golems alike, as in the show. With it off,
 * the old mercy: a teammate or the caster's own pet is spared, hostile creatures and anything going after the caster or
 * their team are hit, and every other creature (villagers, golems, animals, others' pets) only if
 * {@code general.hurtPassives} says so.
 */
public final class AreaTargets {

    public static boolean mayHurt(LivingEntity owner, LivingEntity target) {
        if (target == owner || target instanceof ArmorStand || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            if (owner instanceof Player attacker) {
                MinecraftServer server = player.getServer();
                return server != null && server.isPvpAllowed() && attacker.canHarmPlayer(player);
            }
            return true;
        }
        boolean loaded = EnlightenedConfig.SPEC.isLoaded();
        if (!loaded || EnlightenedConfig.HURT_EVERYTHING.get()) {
            return true;
        }
        if (owner.isAlliedTo(target) || (target instanceof TamableAnimal pet && pet.isOwnedBy(owner))) {
            return false;
        }
        if (target instanceof Enemy) {
            return true;
        }
        if (target instanceof Mob mob) {
            LivingEntity goal = mob.getTarget();
            if (goal != null && (goal == owner || owner.isAlliedTo(goal))) {
                return true;
            }
        }
        return EnlightenedConfig.HURT_PASSIVES.get();
    }

    private AreaTargets() {
    }
}
