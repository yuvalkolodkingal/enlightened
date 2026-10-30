package com.kolodkingal.enlightened.event;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.EnlightenedConfig;
import com.kolodkingal.enlightened.air.AirBending;
import com.kolodkingal.enlightened.air.Projection;
import com.kolodkingal.enlightened.bending.Abilities;
import com.kolodkingal.enlightened.bending.Ability;
import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Chakras;
import com.kolodkingal.enlightened.bending.SoftLanding;
import com.kolodkingal.enlightened.bending.BendingData;
import com.kolodkingal.enlightened.bending.BendingTicker;
import com.kolodkingal.enlightened.bending.Element;
import com.kolodkingal.enlightened.bending.Mastery;
import com.kolodkingal.enlightened.bending.TempBlocks;
import com.kolodkingal.enlightened.command.EnlightenedCommand;
import com.kolodkingal.enlightened.earth.EarthBending;
import com.kolodkingal.enlightened.fire.FireBending;
import com.kolodkingal.enlightened.registry.ModDamageTypes;
import com.kolodkingal.enlightened.registry.ModEffects;
import com.kolodkingal.enlightened.registry.ModSounds;
import com.kolodkingal.enlightened.registry.ModTags;
import com.kolodkingal.enlightened.water.WaterBending;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** The game events every element listens to: ticks, deaths (experience, the pet), damage (skins, shields, lightning), sessions. */
@EventBusSubscriber(modid = Enlightened.MODID)
public final class CommonEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BendingTicker.tick(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TempBlocks.tick(event.getServer());
        com.kolodkingal.enlightened.world.LionTurtles.tick(event.getServer());
        com.kolodkingal.enlightened.bending.AvatarCycle.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        EnlightenedCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    // ---- the lion turtle's shell: only a Hero of the Village builds on it ----

    @SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!onShell(event.getPlayer(), event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && !onShell(player, event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Whether the player may change the block: anywhere off a lion turtle, and on one only if it lets him. */
    private static boolean onShell(Player player, net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        if (player == null || !(level instanceof net.minecraft.server.level.ServerLevel server)
                || com.kolodkingal.enlightened.world.LionTurtles.islandAt(server, pos) == null
                || com.kolodkingal.enlightened.world.LionTurtle.mayBuild(player)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.enlightened.lion_turtle.no_building").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    // ---- deaths: experience for a kill with bending; an airbender's pet ----

    /**
     * Ba Sing Se keeps its peace: no monster comes into the city (from a spawner, an egg, a raid, anything), and no
     * wither or other boss can be raised inside its walls. Omashu keeps monsters out too.
     */
    @SubscribeEvent
    public static void onJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        net.minecraft.world.entity.Entity e = event.getEntity();
        boolean boss = e instanceof net.minecraft.world.entity.boss.wither.WitherBoss || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || e instanceof net.minecraft.world.entity.monster.warden.Warden || e instanceof net.minecraft.world.entity.monster.ElderGuardian;
        if (!(event.getLevel() instanceof ServerLevel level) || !(boss || e instanceof net.minecraft.world.entity.monster.Enemy)) {
            return;
        }
        if (inPeacefulCity(level, e.blockPosition(), boss)) {
            event.setCanceled(true);
            if (boss) {
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, e.getBoundingBox().inflate(32))) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.enlightened.ba_sing_se.forbidden")
                            .withStyle(net.minecraft.ChatFormatting.GREEN), false);
                }
            }
        }
    }

    /** Whether a place is inside Ba Sing Se (or, for monsters, Omashu). */
    public static boolean inPeacefulCity(ServerLevel level, net.minecraft.core.BlockPos pos, boolean bossOnlyBaSingSe) {
        var structures = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        String[] cities = bossOnlyBaSingSe ? new String[]{"ba_sing_se"} : new String[]{"ba_sing_se", "omashu"};
        for (String id : cities) {
            net.minecraft.world.level.levelgen.structure.Structure s = structures.get(com.kolodkingal.enlightened.Enlightened.id(id));
            if (s != null && level.structureManager().getStructureAt(pos, s).isValid()) {
                return true;
            }
        }
        return false;
    }

    /** What he carried on his back falls with the rest of his things, unless the world keeps inventories. */
    @SubscribeEvent
    public static void onDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        net.minecraft.world.item.ItemStack back = com.kolodkingal.enlightened.item.BackSlot.get(p);
        if (!back.isEmpty()) {
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX(), p.getY() + 1, p.getZ(), back.copy()));
            com.kolodkingal.enlightened.item.BackSlot.set(p, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        // In a hardcore world an Avatar who dies is gone for good: the Avatar is reborn in another player.
        if (killed instanceof ServerPlayer dead && dead.getServer() != null && dead.serverLevel().getLevelData().isHardcore()) {
            com.kolodkingal.enlightened.bending.AvatarCycle cycle = com.kolodkingal.enlightened.bending.AvatarCycle.get(dead.getServer());
            if (cycle.isAvatar(dead.getUUID())) {
                cycle.passOn(dead.getServer(), dead.getUUID(), false);
            }
        }
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer killer && killer != killed) {
            BendingData data = Benders.get(killer);
            if (data.element().bends() && source.is(ModTags.BENDING)) {
                Mastery.onKill(killer, killed);
            }
            boolean severs = !EnlightenedConfig.SPEC.isLoaded() || EnlightenedConfig.PET_KILL_SEVERS_TETHERS.get();
            if (severs && data.element().commands(Element.AIR) && !data.tetherless() && killed instanceof TamableAnimal pet && pet.isOwnedBy(killer)) {
                data.setTetherless(true);
                killer.level().playSound(null, killer.getX(), killer.getY(), killer.getZ(), ModSounds.PROJECTION.get(), SoundSource.PLAYERS, 1.5F, 0.5F);
                Benders.burst(killer.serverLevel(), killer.getX(), killer.getY() + 1, killer.getZ(), 0xE6E6E6, 80, 1.2);
                killer.displayClientMessage(Component.translatable("message.enlightened.air.tetherless").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                Benders.syncIfDirty(killer);
            }
        }
        if (killed instanceof ServerPlayer player) {
            BendingData data = Benders.get(player);
            if (data.element().bends()) {
                BendingTicker.stopHolds(player, data);
                if (Projection.isProjecting(player)) {
                    Projection.end(player, null);
                }
                Benders.syncIfDirty(player);
            }
        }
    }

    // ---- damage: a firebender's skin, a lavabender's, the air shield, the octopus form, lightning redirection ----

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        BendingData data = Benders.get(victim);
        if (!data.element().bends()) {
            return;
        }
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        long now = victim.level().getGameTime();
        if (data.avatar(now) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // The Avatar State is not invincible: it takes most of every blow away, and he can still be beaten.
            amount *= avatarGuard();
            event.setAmount(amount);
        }
        if (amount >= victim.getHealth() - 0.5F && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && Chakras.dire(victim, data)) {
            event.setCanceled(true);
            return;
        }
        boolean lightning = source.is(ModDamageTypes.LIGHTNING) || source.is(DamageTypes.LIGHTNING_BOLT);
        if (lightning && data.element().commands(Element.FIRE) && FireBending.redirect(victim, source, amount)) {
            event.setCanceled(true);
            return;
        }
        boolean fire = source.is(DamageTypeTags.IS_FIRE) || source.is(ModTags.FIRE_BENDING);
        if (fire) {
            float guard = Math.max(data.element().commands(Element.FIRE) ? FireBending.fireSkin(data) : 0F,
                    data.element().commands(Element.EARTH) ? EarthBending.lavaSkin(data) : 0F);
            if (guard >= 1.0F) {
                event.setCanceled(true);
                return;
            }
            amount *= 1.0F - guard;
        }
        if (data.element().commands(Element.WATER)) {
            amount *= 1.0F - WaterBending.octopusGuard(data);
            if (!source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                amount = com.kolodkingal.enlightened.water.WaterMore.shieldAbsorb(victim, data, amount);
            }
        }
        if (data.element().commands(Element.FIRE) && !source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            boolean deflected = source.is(DamageTypeTags.IS_PROJECTILE) || source.is(ModTags.BENDING);
            amount = com.kolodkingal.enlightened.fire.FireMore.shieldAbsorb(victim, data, amount, deflected);
        }
        if (amount <= 0) {
            event.setCanceled(true);
            return;
        }
        if (data.element().commands(Element.AIR) && AirBending.shieldUp(victim, data)
                && !source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount = AirBending.shieldAbsorb(victim, data, amount);
            if (amount <= 0) {
                event.setCanceled(true);
                return;
            }
        }
        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }

    /** A firebender who masters his skin is not struck by the sky's lightning either. */
    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Benders.is(player, Element.FIRE)
                && Benders.get(player).level(Ability.FIRE_SKIN) >= 2) {
            event.setCanceled(true);
        }
    }

    /** The octopus form strikes back round whoever its bender hits. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target
                && Benders.is(player, Element.WATER)) {
            WaterBending.octopusStrike(player, target);
        }
    }

    /** A creature calmed by spiritbending takes no prey while the calm lasts. */
    @SubscribeEvent
    public static void onChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && event.getEntity().hasEffect(com.kolodkingal.enlightened.registry.ModEffects.PACIFIED)) {
            event.setCanceled(true);
        }
    }

    /** Held, sinking, frozen or stunned: no jumping. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity e = event.getEntity();
        if (e.hasEffect(ModEffects.BLOODBENT) || e.hasEffect(ModEffects.QUICKSAND) || e.hasEffect(ModEffects.FROZEN) || e.hasEffect(ModEffects.STUNNED)) {
            e.setDeltaMovement(e.getDeltaMovement().x, 0, e.getDeltaMovement().z);
        }
    }

    /** An airbender on his scooter or his glider lands softly; so does anyone a step, a jet or a catapult carried. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SoftLanding.active(player)) {
            event.setDamageMultiplier(0.0F);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && Benders.is(player, Element.AIR)) {
            // An airbender never falls hard: the air cushions every landing.
            event.setDamageMultiplier(0.0F);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && Benders.is(player, Element.EARTH)) {
            com.kolodkingal.enlightened.earth.EarthMore.onFall(player, Benders.get(player), event);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && Benders.get(player).avatar(player.level().getGameTime())) {
            event.setDamageMultiplier(0.0F);
            return;
        }
    }

    /** The share of a blow that reaches the Avatar in the Avatar State (config {@code avatarStateDamageTaken}). */
    private static float avatarGuard() {
        return (float) (EnlightenedConfig.SPEC.isLoaded() ? EnlightenedConfig.AVATAR_STATE_DAMAGE_TAKEN.get() : 0.3);
    }

    // ---- sessions ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Benders.castFate(player);
            if (player.getClass() == ServerPlayer.class) {
                com.kolodkingal.enlightened.bending.AvatarCycle cycle = com.kolodkingal.enlightened.bending.AvatarCycle.get(player.getServer());
                // An Avatar from before the cycle was kept is counted now.
                BendingData born = Benders.get(player);
                if (born.element() == Element.AVATAR && (!cycle.isAvatar(player.getUUID()) || born.avatarBase() == Element.NONE)) {
                    com.kolodkingal.enlightened.bending.Avatar.bear(born, cycle.register(player));
                    born.markDirty();
                    Benders.syncIfDirty(player);
                }
                cycle.onLogin(player);
            }
            BendingData data = Benders.get(player);
            data.clearActive();
            BendingTicker.applyPassives(player, data);
            data.markDirty();
            Benders.syncIfDirty(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer leaving) {
            SoftLanding.forget(leaving);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            BendingData data = Benders.get(player);
            if (data.element().bends()) {
                Projection.onLogout(player);
                BendingTicker.stopHolds(player, data);
            }
            Abilities.forgetAll(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BendingData data = Benders.get(player);
            data.clearActive();
            BendingTicker.applyPassives(player, data);
            Benders.syncIfDirty(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BendingData data = Benders.get(player);
            if (data.element().bends()) {
                if (Projection.isProjecting(player)) {
                    Projection.end(player, null);
                }
                BendingTicker.stopHolds(player, data);
                Benders.syncIfDirty(player);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Projection.returnAll(event.getServer());
        TempBlocks.restoreEverything(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SoftLanding.clear();
        Abilities.clearAll();
        Projection.clear();
        TempBlocks.clear();
    }

    private CommonEvents() {
    }
}
