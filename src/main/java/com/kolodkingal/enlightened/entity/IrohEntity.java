package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.registry.ModItems;
import com.kolodkingal.enlightened.talk.Speaker;
import com.kolodkingal.enlightened.talk.Talk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Uncle Iroh (1.0.23), in the green and cream of a tea server, behind the counter of the Jasmine Dragon in Ba Sing Se's
 * Upper Ring. Spoken to (the talk screen), he sells tea (the vanilla trading screen, for gold nuggets), tells how he
 * "killed the last dragon", gives advice, and for 50 gold gives a tsungi horn: blown at your home, he comes flying on
 * jets of fire and sells his tea there ({@link #arrive}). A home Iroh has an owner and keeps near his home; there is one
 * per player (the newest: an older one that loads again leaves). Nothing harms him.
 */
public class IrohEntity extends PathfinderMob implements Merchant {
    /** The gold (ingots; a gold block counts nine) the horn costs. */
    public static final int HORN_PRICE = 50;
    /** How long he flies down to a horn's call. */
    public static final int ARRIVAL_TICKS = 60;
    private static final EntityDataAccessor<Integer> ARRIVING = SynchedEntityData.defineId(IrohEntity.class, EntityDataSerializers.INT);
    /** The player's latest home Iroh (in his persisted data). */
    public static final String HOME_KEY = "enlightened_iroh";

    @Nullable
    private UUID owner;
    @Nullable
    private BlockPos home;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;

    public IrohEntity(EntityType<? extends IrohEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARRIVING, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5) {
            @Override
            public boolean canUse() {
                return home() != null && arriving() == 0 && !Talk.talking(IrohEntity.this) && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ---- who he is ----

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    /** Whether this is a player's own Iroh, at his home. */
    public boolean atHome() {
        return owner != null;
    }

    public boolean ownedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    public int arriving() {
        return entityData.get(ARRIVING);
    }

    // ---- the horn's call ----

    /**
     * Iroh answers a player's horn: any Iroh of his that is about leaves, and a new one comes flying down on jets of
     * fire to land beside him, where he will sell his tea from now on.
     */
    public static IrohEntity arrive(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (ServerLevel l : player.server.getAllLevels()) {
            for (Entity e : l.getAllEntities()) {
                if (e instanceof IrohEntity old && old.ownedBy(player)) {
                    old.discard();
                }
            }
        }
        IrohEntity iroh = com.kolodkingal.enlightened.registry.ModEntities.IROH.get().create(level);
        Vec3 side = player.getLookAngle().multiply(1, 0, 1);
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        BlockPos land = BlockPos.containing(player.position().add(side.scale(2.5)));
        iroh.owner = player.getUUID();
        iroh.home = land;
        iroh.restrictTo(land, 8);
        iroh.moveTo(land.getX() + 0.5, land.getY() + 24, land.getZ() + 0.5, player.getYRot() + 180, 0);
        iroh.entityData.set(ARRIVING, ARRIVAL_TICKS);
        iroh.setNoGravity(true);
        level.addFreshEntity(iroh);
        CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        kept.putUUID(HOME_KEY, iroh.getUUID());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        level.playSound(null, land, SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 0.6F);
        return iroh;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        int left = arriving();
        if (left > 0) {
            // Down on two jets of fire from his feet, slowing as he comes.
            BlockPos at = home != null ? home : blockPosition();
            double target = at.getY();
            double y = getY();
            double step = Math.max(0.08, (y - target) / Math.max(1, left) * 1.2);
            setDeltaMovement(0, 0, 0);
            setPos(at.getX() + 0.5, Math.max(target, y - step), at.getZ() + 0.5);
            ServerLevel level = (ServerLevel) level();
            Vec3 right = Vec3.directionFromRotation(0, yBodyRot + 90).scale(0.2);
            for (int s = -1; s <= 1; s += 2) {
                Vec3 foot = position().add(right.scale(s));
                level.sendParticles(ParticleTypes.FLAME, foot.x, foot.y, foot.z, 6, 0.06, 0.05, 0.06, 0.02);
                level.sendParticles(ParticleTypes.SMALL_FLAME, foot.x, foot.y - 0.4, foot.z, 4, 0.05, 0.3, 0.05, 0.01);
            }
            if (left % 10 == 0) {
                playSound(SoundEvents.FIRECHARGE_USE, 0.7F, 0.6F + random.nextFloat() * 0.2F);
            }
            left--;
            if (getY() <= target + 0.01) {
                left = 0;
            }
            entityData.set(ARRIVING, left);
            if (left == 0) {
                setNoGravity(false);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.1, getZ(), 20, 0.4, 0.1, 0.4, 0.02);
                playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.8F, 1.0F);
            }
        }
        if (owner != null && tickCount % 100 == 0) {
            // One Iroh per player: an older one that loads again leaves.
            ServerPlayer p = level().getServer().getPlayerList().getPlayer(owner);
            if (p != null) {
                CompoundTag kept = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
                if (kept.hasUUID(HOME_KEY) && !kept.getUUID(HOME_KEY).equals(getUUID())) {
                    discard();
                }
            }
        }
        if (tradingPlayer != null && (!tradingPlayer.isAlive() || tradingPlayer.distanceTo(this) > 12)) {
            setTradingPlayer(null);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || arriving() > 0) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            Talk.begin(server, this, Speaker.IROH);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.getEntity() instanceof Player p && p.isCreative()) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ---- gold ----

    /** The gold a player carries, in ingots (a block is nine). */
    public static int gold(Player player) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(Items.GOLD_INGOT)) {
                n += s.getCount();
            } else if (s.is(Items.GOLD_BLOCK)) {
                n += s.getCount() * 9;
            }
        }
        return n;
    }

    /** Takes {@code price} gold (ingots first, then blocks, with change in ingots); false if he has not enough. */
    public static boolean pay(Player player, int price) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (gold(player) < price) {
            return false;
        }
        int left = price;
        for (ItemStack s : player.getInventory().items) {
            if (left > 0 && s.is(Items.GOLD_INGOT)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        for (ItemStack s : player.getInventory().items) {
            while (left > 0 && s.is(Items.GOLD_BLOCK) && !s.isEmpty()) {
                s.shrink(1);
                int change = 9 - Math.min(9, left);
                left -= Math.min(9, left);
                if (change > 0 && !player.getInventory().add(new ItemStack(Items.GOLD_INGOT, change))) {
                    player.drop(new ItemStack(Items.GOLD_INGOT, change), false);
                }
            }
        }
        return true;
    }

    /** Sells a player the tsungi horn that calls his own Iroh home. */
    public boolean sellHorn(ServerPlayer player) {
        if (!pay(player, HORN_PRICE)) {
            return false;
        }
        ItemStack horn = new ItemStack(ModItems.TSUNGI_HORN.get());
        if (!player.getInventory().add(horn)) {
            player.drop(horn, false);
        }
        playSound(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.0F, 0.8F);
        return true;
    }

    // ---- the tea (the vanilla trading screen) ----

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = new MerchantOffers();
            offers.add(offer(Items.GOLD_NUGGET, 3, new ItemStack(ModItems.JASMINE_TEA.get())));
            offers.add(offer(Items.GOLD_NUGGET, 4, new ItemStack(ModItems.GINSENG_TEA.get())));
            offers.add(offer(Items.GOLD_NUGGET, 6, new ItemStack(ModItems.DRAGON_TEA.get())));
            offers.add(offer(Items.GOLD_NUGGET, 2, new ItemStack(Items.COOKIE, 4)));
            offers.add(offer(Items.GOLD_NUGGET, 4, new ItemStack(Items.PUMPKIN_PIE, 2)));
            offers.add(offer(Items.GOLD_INGOT, 1, new ItemStack(Items.CAKE)));
            offers.add(offer(Items.GOLD_INGOT, 2, new ItemStack(ModBlocksHolder.stool(), 2)));
        }
        return offers;
    }

    private static MerchantOffer offer(net.minecraft.world.item.Item cost, int count, ItemStack result) {
        return new MerchantOffer(new ItemCost(cost, count), result, Integer.MAX_VALUE, 0, 0.0F);
    }

    /** Opens his trades. */
    public void openTrades(ServerPlayer player) {
        setTradingPlayer(player);
        openTradingScreen(player, Component.translatable(Speaker.IROH.nameKey), 1);
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        playSound(SoundEvents.VILLAGER_YES, 0.6F, 0.8F);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    // ---- saving ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        tag.putInt("Arriving", arriving());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        if (home != null) {
            restrictTo(home, 8);
        }
        entityData.set(ARRIVING, tag.getInt("Arriving"));
        setNoGravity(arriving() > 0);
    }

    /** The stool, without loading the blocks class early. */
    private static final class ModBlocksHolder {
        static net.minecraft.world.level.ItemLike stool() {
            return com.kolodkingal.enlightened.registry.ModBlocks.TEA_STOOL.get();
        }
    }
}
