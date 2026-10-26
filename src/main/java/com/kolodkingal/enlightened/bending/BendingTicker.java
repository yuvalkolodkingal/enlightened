package com.kolodkingal.enlightened.bending;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.air.AirBending;
import com.kolodkingal.enlightened.earth.EarthBending;
import com.kolodkingal.enlightened.water.WaterBending;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Runs every ability's per-tick logic and keeps the passive bonuses (flight, armour, speed, reach) in sync. */
public final class BendingTicker {
    private static final ResourceLocation FLIGHT_ID = Enlightened.id("bending_flight");

    /** Called at the end of every server player tick. */
    public static void tick(ServerPlayer player) {
        BendingData data = Benders.get(player);
        if (!data.element().bends()) {
            return;
        }
        if (!player.isAlive()) {
            // A dead body must not keep whipping or burning while its player sits on the death screen.
            stopHolds(player, data);
            Benders.syncIfDirty(player);
            return;
        }
        applyPassives(player, data);
        for (Ability a : Ability.values()) {
            if (data.has(a)) {
                AbilityLogic logic = com.kolodkingal.enlightened.bending.Abilities.of(a);
                if (logic != null) {
                    logic.tick(new Cast(player, data, a));
                }
            }
        }
        data.setFlying(player.getAbilities().flying);
        Benders.syncIfDirty(player);
    }

    /** Flight permission and speed, and the body modifiers of armour, arms and scooter. */
    public static void applyPassives(ServerPlayer player, BendingData data) {
        boolean canFly = data.element() == Element.AIR && data.has(Ability.FLIGHT) && data.on(Ability.FLIGHT)
                && !data.suppressed(player.level().getGameTime());
        setModifier(player, NeoForgeMod.CREATIVE_FLIGHT, FLIGHT_ID, 1.0, canFly);

        Abilities abilities = player.getAbilities();
        boolean survivalLike = !player.isCreative() && !player.isSpectator();
        float speed = !survivalLike || !canFly ? 0.05F : 0.08F;
        boolean changed = false;
        if (Math.abs(abilities.getFlyingSpeed() - speed) > 1.0E-4F) {
            abilities.setFlyingSpeed(speed);
            changed = true;
        }
        if (abilities.flying && !player.mayFly()) {
            abilities.flying = false;
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
        EarthBending.applyBody(player, data);
        WaterBending.applyBody(player, data);
        AirBending.applyBody(player, data);
    }

    /** Everything the bender's abilities left in the world ends (the element is lost). */
    public static void endAll(ServerPlayer player, BendingData data) {
        for (Ability a : Ability.values()) {
            AbilityLogic logic = com.kolodkingal.enlightened.bending.Abilities.of(a);
            if (logic != null) {
                logic.end(new Cast(player, data, a));
            }
        }
        data.clearActive();
        data.clearToggles();
        data.setHeldEntityId(-1);
        applyPassives(player, data);
    }

    /** Releases every hold ability and the momentary states (the player died, or logged out). */
    public static void stopHolds(ServerPlayer player, BendingData data) {
        BendingHandler.releaseHolds(player, data);
        for (Ability a : Ability.values()) {
            if (a.kind != Ability.Kind.TOGGLE && a.kind != Ability.Kind.PASSIVE && data.active(a)) {
                AbilityLogic logic = com.kolodkingal.enlightened.bending.Abilities.of(a);
                if (logic != null) {
                    logic.end(new Cast(player, data, a));
                }
                data.setActive(a, false);
            }
        }
    }

    /** The flight key of a tetherless airbender: take off or land, the same as double-tapping Space. */
    public static void toggleFlight(ServerPlayer player) {
        if (!player.mayFly()) {
            return;
        }
        Abilities abilities = player.getAbilities();
        abilities.flying = !abilities.flying;
        if (abilities.flying && player.onGround()) {
            player.setDeltaMovement(new Vec3(0, 0.45, 0));
            player.hurtMarked = true;
        }
        player.onUpdateAbilities();
        player.displayClientMessage(Component.translatable(abilities.flying
                ? "message.enlightened.flight.on" : "message.enlightened.flight.off"), true);
    }

    /**
     * Adds or removes a permanent attribute modifier. Permanent (saved) modifiers are used so that flight and armour
     * are already correct when the player's abilities are loaded on login.
     */
    public static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present) {
        setModifier(player, attribute, id, amount, present, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present,
                                   AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount || existing.operation() != operation) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    private BendingTicker() {
    }
}
