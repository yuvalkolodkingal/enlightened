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
 * <p>The rule: never the caster, a Creative or Spectator player, an armour stand, a teammate or the caster's own pets.
 * A player only if PvP is on and the caster may harm them. Hostile creatures always, and anything that is going after
 * the caster or their team. Every other creature (villagers, golems, animals, others' pets) only if the server option
 * {@code general.hurtPassives} says so (off by default).
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
        return !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.HURT_PASSIVES.get();
    }

    private AreaTargets() {
    }
}
