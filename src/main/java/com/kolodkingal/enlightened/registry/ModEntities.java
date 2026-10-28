package com.kolodkingal.enlightened.registry;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.BendingShotEntity;
import com.kolodkingal.enlightened.entity.FlyingLemurEntity;
import com.kolodkingal.enlightened.entity.MeditatingBodyEntity;
import com.kolodkingal.enlightened.entity.TornadoEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Enlightened.MODID);

    /** A rock, a cloud of sand, a metal blade, an ice knife, a snowball, a lava shuriken, an air blade or a fire ball a bender shot or holds. */
    public static final DeferredHolder<EntityType<?>, EntityType<BendingShotEntity>> SHOT = ENTITIES.register("shot",
            () -> EntityType.Builder.<BendingShotEntity>of(BendingShotEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(2).build("shot"));

    /** A small tornado an airbender sent at his enemies. */
    public static final DeferredHolder<EntityType<?>, EntityType<TornadoEntity>> TORNADO = ENTITIES.register("tornado",
            () -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC)
                    .sized(1.5F, 3.0F).clientTrackingRange(10).updateInterval(2).build("tornado"));

    /** The body an airbender left behind in meditation while his spirit looks about. */
    public static final DeferredHolder<EntityType<?>, EntityType<MeditatingBodyEntity>> MEDITATING_BODY = ENTITIES.register("meditating_body",
            () -> EntityType.Builder.<MeditatingBodyEntity>of(MeditatingBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.5F).clientTrackingRange(10).updateInterval(4).build("meditating_body"));

    /** The flying lemur of the Air Nomads: tamed with fruit, it follows its owner and flits from tree to tree. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.kolodkingal.enlightened.entity.FireWallEntity>> FIRE_WALL = ENTITIES.register("fire_wall",
            () -> EntityType.Builder.<com.kolodkingal.enlightened.entity.FireWallEntity>of(com.kolodkingal.enlightened.entity.FireWallEntity::new, MobCategory.MISC)
                    .sized(0.5F, 3.0F).clientTrackingRange(10).updateInterval(10).noSummon().build("fire_wall"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingLemurEntity>> LEMUR = ENTITIES.register("flying_lemur",
            () -> EntityType.Builder.of(FlyingLemurEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F).clientTrackingRange(8).build("flying_lemur"));

    /** Guru Pathik, who sits in the lotus at the Southern Air Temple and teaches the seven chakras. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.kolodkingal.enlightened.entity.GuruPathikEntity>> GURU_PATHIK = ENTITIES.register("guru_pathik",
            () -> EntityType.Builder.<com.kolodkingal.enlightened.entity.GuruPathikEntity>of(com.kolodkingal.enlightened.entity.GuruPathikEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.2F).clientTrackingRange(10).build("guru_pathik"));

    /** The sky bison of the Air Nomads: a calf at the Southern Air Temple, tamed with wheat, saddled and flown with up to six riders. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.kolodkingal.enlightened.entity.SkyBisonEntity>> SKY_BISON = ENTITIES.register("sky_bison",
            () -> EntityType.Builder.of(com.kolodkingal.enlightened.entity.SkyBisonEntity::new, MobCategory.CREATURE)
                    .sized(3.0F, 2.5F).eyeHeight(2.0F).clientTrackingRange(12).build("sky_bison"));

    /** A lion turtle's head, at the front of the island of its shell, which swims and carries all on it. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.kolodkingal.enlightened.entity.LionTurtleEntity>> LION_TURTLE = ENTITIES.register("lion_turtle",
            () -> EntityType.Builder.<com.kolodkingal.enlightened.entity.LionTurtleEntity>of(com.kolodkingal.enlightened.entity.LionTurtleEntity::new, MobCategory.MISC)
                    .sized(8.0F, 9.0F).eyeHeight(7.0F).clientTrackingRange(16).updateInterval(20).fireImmune().build("lion_turtle"));

    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(SKY_BISON.get(), com.kolodkingal.enlightened.entity.SkyBisonEntity.createAttributes().build());
        event.put(LION_TURTLE.get(), com.kolodkingal.enlightened.entity.LionTurtleEntity.createAttributes().build());
        event.put(GURU_PATHIK.get(), com.kolodkingal.enlightened.entity.GuruPathikEntity.createAttributes().build());
        event.put(MEDITATING_BODY.get(), MeditatingBodyEntity.createAttributes().build());
        event.put(LEMUR.get(), FlyingLemurEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(LEMUR.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FlyingLemurEntity::checkLemurSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEntities() {
    }
}
