package com.kolodkingal.enlightened.entity;

import com.kolodkingal.enlightened.bending.Benders;
import com.kolodkingal.enlightened.item.EternalFlameItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A Sun Warrior of the ancient city, the firebenders who kept the dragons' secret: they live among the ruins, fight
 * any monster that comes (their blows set it alight), and talk with whoever speaks to them.
 *
 * <ul>
 * <li>The <b>chief</b> waits at the foot of the mountain's stairway. Spoken to, he asks whether you would be tested by
 * Ran and Shaw; speak to him again and he gives you the Eternal Flame (see {@link EternalFlameItem}), which goes out
 * in seven minutes: carry it up to the masters before it does.</li>
 * <li>The <b>dancers</b> go round the fire on the terrace below the summit, in the ceremony that comes before the
 * last stair, flames rising from their hands.</li>
 * <li>The <b>warriors</b> keep the city.</li>
 * </ul>
 */
public class SunWarriorEntity extends PathfinderMob {
    public static final int POST = 16;
    public static final int FACES = 3;
    /** How many things a warrior has to say after his greeting, and a dancer's chants. */
    public static final int LINES = 6;
    public static final int CHANTS = 3;
    /** How long (ticks) the chief waits for an answer to his offer. */
    public static final int ANSWER = 1200;
    /** The dancers' circle round the fire. */
    public static final double DANCE_RADIUS = 4.0;

    public enum Role {
        WARRIOR("warrior"), CHIEF("chief"), DANCER("dancer");

        public final String id;

        Role(String id) {
            this.id = id;
        }

        public static Role of(int i) {
            Role[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(SunWarriorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(SunWarriorEntity.class, EntityDataSerializers.INT);

    private int talks;
    private boolean unposted;
    /** Whom the chief made his offer to, and when. */
    private UUID asked;
    private long askedAt;

    public SunWarriorEntity(EntityType<? extends SunWarriorEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, 0);
        builder.define(FACE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(2, new DanceGoal());
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5) {
            @Override
            public boolean canUse() {
                return role() == Role.WARRIOR && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, SunWarriorEntity.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                e -> e instanceof Enemy && !(e instanceof Creeper)));
    }

    public Role role() {
        return Role.of(entityData.get(ROLE));
    }

    public int face() {
        return Math.floorMod(entityData.get(FACE), FACES);
    }

    /** Sets his role, his face and his post (for a dancer, the fire he dances round). */
    public void station(Role role, int face, BlockPos post) {
        entityData.set(ROLE, role.ordinal());
        entityData.set(FACE, Math.floorMod(face, FACES));
        restrictTo(post, role == Role.DANCER ? 7 : role == Role.CHIEF ? 4 : POST);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (reason != MobSpawnType.STRUCTURE) {
            station(Role.WARRIOR, random.nextInt(FACES), blockPosition());
        }
        return result;
    }

    // ---- talk, and the chief's offer ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server && !com.kolodkingal.enlightened.talk.Talk.begin(server, this, speaker())) {
            talk(server);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** What he says to someone who speaks to him (the translation key of his line), doing what it says. */
    public String line(ServerPlayer player) {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            return "sun.enlightened.busy";
        }
        return switch (role()) {
            case CHIEF -> chief(player);
            case DANCER -> "sun.enlightened.dancer." + Math.floorMod(talks++, CHANTS);
            case WARRIOR -> {
                int n = talks++;
                yield n == 0 ? "sun.enlightened.greet." + face() : "sun.enlightened.line." + Math.floorMod(face() * 2 + n - 1, LINES);
            }
        };
    }

    private String chief(ServerPlayer player) {
        long now = level().getGameTime();
        if (Benders.get(player).dragonBlessed()) {
            return "sun.enlightened.chief.blessed";
        }
        if (EternalFlameItem.carries(player)) {
            return "sun.enlightened.chief.hurry";
        }
        if (player.getUUID().equals(asked) && now - askedAt <= ANSWER) {
            asked = null;
            giveFlame(player);
            return "sun.enlightened.chief.given";
        }
        asked = player.getUUID();
        askedAt = now;
        return "sun.enlightened.chief.offer";
    }

    /** The chief hands the Eternal Flame to a player who takes up the test. */
    public void giveFlame(ServerPlayer player) {
        ItemStack flame = EternalFlameItem.lit(level().getGameTime());
        if (!player.getInventory().add(flame)) {
            player.drop(flame, false);
        }
        playSound(SoundEvents.FIRECHARGE_USE, 0.8F, 1.0F);
    }

    /** The speaker of the talk screen this warrior is. */
    public com.kolodkingal.enlightened.talk.Speaker speaker() {
        return switch (role()) {
            case CHIEF -> com.kolodkingal.enlightened.talk.Speaker.SUN_CHIEF;
            case DANCER -> com.kolodkingal.enlightened.talk.Speaker.SUN_DANCER;
            case WARRIOR -> com.kolodkingal.enlightened.talk.Speaker.SUN_WARRIOR;
        };
    }

    public void talk(ServerPlayer player) {
        getLookControl().setLookAt(player);
        playSound(SoundEvents.VILLAGER_AMBIENT, 0.6F, 0.9F);
        player.sendSystemMessage(Component.translatable("sun.enlightened.says",
                Component.translatable("entity.enlightened.sun_warrior." + role().id), Component.translatable(line(player)))
                .withStyle(ChatFormatting.GOLD));
    }

    // ---- fighting ----

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            // A firebender's blow.
            target.igniteForSeconds(3.0F);
        }
        return hit;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof SunWarriorEntity || target instanceof DragonEntity
                || target instanceof net.minecraft.world.entity.npc.AbstractVillager) && super.canAttack(target);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    // ---- the ceremony ----

    /** A dancer goes round the fire, now and then leaping and throwing up his hands. */
    final class DanceGoal extends Goal {
        DanceGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return role() == Role.DANCER && hasRestriction() && getTarget() == null;
        }

        @Override
        public void tick() {
            BlockPos c = getRestrictCenter();
            double phase = Math.atan2(getZ() - (c.getZ() + 0.5), getX() - (c.getX() + 0.5));
            double a = phase + 0.35;
            if (tickCount % 8 == 0) {
                getNavigation().moveTo(c.getX() + 0.5 + Math.cos(a) * DANCE_RADIUS, c.getY(), c.getZ() + 0.5 + Math.sin(a) * DANCE_RADIUS, 0.6);
            }
            getLookControl().setLookAt(c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5);
            if (random.nextInt(40) == 0 && onGround()) {
                getJumpControl().jump();
                swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    /** Whether he is dancing just now (for the renderer: arms raised). */
    public boolean dancing() {
        return role() == Role.DANCER && getTarget() == null;
    }

    @Override
    public void aiStep() {
        if (unposted && !level().isClientSide) {
            unposted = false;
            station(role(), face(), blockPosition());
        }
        super.aiStep();
        if (level().isClientSide && dancing() && tickCount % 3 == 0) {
            // Flames rising from his hands.
            float yaw = yBodyRot * Mth.DEG_TO_RAD;
            for (int side = -1; side <= 1; side += 2) {
                double hx = getX() - Mth.cos(yaw) * 0.4 * side;
                double hz = getZ() - Mth.sin(yaw) * 0.4 * side;
                level().addParticle(ParticleTypes.FLAME, hx, getY() + 2.1, hz, 0, 0.04, 0);
            }
        }
    }

    // ---- saving ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", entityData.get(ROLE));
        tag.putInt("Face", entityData.get(FACE));
        tag.putInt("Talks", talks);
        if (hasRestriction()) {
            BlockPos p = getRestrictCenter();
            tag.putIntArray("Post", new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, tag.getInt("Role"));
        entityData.set(FACE, tag.getInt("Face"));
        talks = tag.getInt("Talks");
        int[] p = tag.getIntArray("Post");
        if (p.length == 3) {
            restrictTo(new BlockPos(p[0], p[1], p[2]), role() == Role.DANCER ? 7 : role() == Role.CHIEF ? 4 : POST);
        } else {
            unposted = true;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
