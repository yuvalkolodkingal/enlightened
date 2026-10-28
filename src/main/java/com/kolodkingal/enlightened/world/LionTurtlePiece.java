package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.entity.LionTurtleEntity;
import com.kolodkingal.enlightened.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A lion turtle's island, laid chunk by chunk, and its head, put in the sea with the chunk that holds it. */
public class LionTurtlePiece extends StructurePiece {
    private final BlockPos center;
    private final Direction heading;

    public LionTurtlePiece(BlockPos center, Direction heading) {
        super(ModStructures.LION_TURTLE_PIECE.get(), 0, LionTurtle.box(center));
        this.center = center;
        this.heading = heading;
    }

    public LionTurtlePiece(CompoundTag tag) {
        super(ModStructures.LION_TURTLE_PIECE.get(), tag);
        this.center = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.heading = Direction.from2DDataValue(tag.getInt("Heading"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", center.getX());
        tag.putInt("CY", center.getY());
        tag.putInt("CZ", center.getZ());
        tag.putInt("Heading", heading.get2DDataValue());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        LionTurtle.build(level, center, heading, box);
        BlockPos head = LionTurtle.headPos(center, heading);
        if (box.isInside(head)) {
            LionTurtleEntity turtle = ModEntities.LION_TURTLE.get().create(level.getLevel());
            if (turtle != null) {
                turtle.setIsland(center, heading);
                turtle.finalizeSpawn(level, level.getCurrentDifficultyAt(head), MobSpawnType.STRUCTURE, null);
                turtle.setPersistenceRequired();
                level.addFreshEntityWithPassengers(turtle);
            }
        }
    }
}
