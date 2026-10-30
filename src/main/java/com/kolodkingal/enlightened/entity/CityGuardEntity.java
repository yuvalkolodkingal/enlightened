package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.bending.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A guard of one of the great cities: Omashu, Ba Sing Se or Ellora. He keeps to his post (a stretch of street round
 * where he was set), fights any monster that comes near and anyone who strikes him or another guard, and talks with
 * whoever speaks to him (right-click): a greeting of his own, then the talk of his city, a little of it each time. Each
 * city dresses its guards in its own colours; within a city they differ in face.
 */
public class CityGuardEntity extends PathfinderMob {
    /** How far from his post a guard walks. */
    public static final int POST = 20;
    /** How many things a guard of each city has to say after his greeting. */
    public static final int LINES = 8;
    /** How many greetings (and faces) the guards of a city have. */
    public static final int VOICES = 3;

    public enum City {
        OMASHU("omashu"), BA_SING_SE("ba_sing_se"), ELLORA("ellora");

        public final String id;

        City(String id) {
            this.id = id;
        }

        public static City of(int i) {
            City[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    private static final EntityDataAccessor<Integer> CITY = SynchedEntityData.defineId(CityGuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VOICE = SynchedEntityData.defineId(CityGuardEntity.class, EntityDataSerializers.INT);

    private int talks;
    /** Set when a guard is loaded without a post (summoned with data): he is stationed where he stands. */
    private boolean unposted;

    public CityGuardEntity(EntityType<? extends CityGuardEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 8.0).add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CITY, 0);
        builder.define(VOICE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, CityGuardEntity.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                e -> e instanceof Enemy && !(e instanceof Creeper)));
    }

    public City city() {
        return City.of(entityData.get(CITY));
    }

    public int voice() {
        return Math.floorMod(entityData.get(VOICE), VOICES);
    }

    /** Sets the guard's city, his face and greeting, his post, and arms him as his city arms its guards. */
    public void station(City city, int voice, BlockPos post) {
        entityData.set(CITY, city.ordinal());
        entityData.set(VOICE, Math.floorMod(voice, VOICES));
        restrictTo(post, POST);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (city) {
            case OMASHU -> Items.IRON_AXE;
            case BA_SING_SE -> Items.IRON_SWORD;
            case ELLORA -> Items.GOLDEN_SWORD;
        }));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (reason != MobSpawnType.STRUCTURE) {
            // From an egg or a command: a guard of a city chosen by chance, posted where he stands.
            station(City.of(random.nextInt(3)), random.nextInt(VOICES), blockPosition());
        }
        return result;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            talk(server);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** What the guard says to someone who speaks to him (the translation key of his line). */
    public String line(ServerPlayer player) {
        String city = "guard.enlightened." + city().id + ".";
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            return "guard.enlightened.busy";
        }
        int n = talks++;
        if (n == 0) {
            return city + "greet." + voice();
        }
        if (n == 1 && Benders.get(player).element() == Element.AVATAR) {
            return city + "avatar";
        }
        return city + "line." + Math.floorMod(voice() * 3 + n - 1, LINES);
    }

    public void talk(ServerPlayer player) {
        getLookControl().setLookAt(player);
        playSound(SoundEvents.VILLAGER_AMBIENT, 0.6F, 0.8F);
        player.sendSystemMessage(Component.translatable("guard.enlightened.says",
                Component.translatable("entity.enlightened.city_guard." + city().id), Component.translatable(line(player)))
                .withStyle(city() == City.ELLORA ? ChatFormatting.GOLD : ChatFormatting.GREEN));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // A guard never turns on another guard, nor on the people of his city.
        return !(target instanceof CityGuardEntity || target instanceof KingBumiEntity
                || target instanceof net.minecraft.world.entity.npc.AbstractVillager) && super.canAttack(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("City", entityData.get(CITY));
        tag.putInt("Voice", entityData.get(VOICE));
        tag.putInt("Talks", talks);
        if (hasRestriction()) {
            BlockPos p = getRestrictCenter();
            tag.putIntArray("Post", new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(CITY, tag.getInt("City"));
        entityData.set(VOICE, tag.getInt("Voice"));
        talks = tag.getInt("Talks");
        int[] p = tag.getIntArray("Post");
        if (p.length == 3) {
            restrictTo(new BlockPos(p[0], p[1], p[2]), POST);
        } else {
            unposted = true;
        }
    }

    @Override
    public void aiStep() {
        if (unposted && !level().isClientSide) {
            unposted = false;
            station(city(), voice(), blockPosition());
        }
        super.aiStep();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
