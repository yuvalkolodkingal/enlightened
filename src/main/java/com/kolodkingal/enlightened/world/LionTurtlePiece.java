package com.kolodkingal.enlightened.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** The mark of a lion turtle: when the chunk at its heart is made, the island is queued to rise ({@link LionTurtles#queue}). */
public class LionTurtlePiece extends StructurePiece {
    private final BlockPos center;
    private final Direction heading;
    private final int scale;

    public LionTurtlePiece(BlockPos center, Direction heading, int scale) {
        super(ModStructures.LION_TURTLE_PIECE.get(), 0, new BoundingBox(center.getX(), center.getY(), center.getZ(), center.getX(), center.getY(), center.getZ()));
        this.center = center;
        this.heading = heading;
        this.scale = scale;
    }

    public LionTurtlePiece(CompoundTag tag) {
        super(ModStructures.LION_TURTLE_PIECE.get(), tag);
        this.center = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.heading = Direction.from2DDataValue(tag.getInt("Heading"));
        this.scale = tag.contains("Scale") ? tag.getInt("Scale") : 1;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", center.getX());
        tag.putInt("CY", center.getY());
        tag.putInt("CZ", center.getZ());
        tag.putInt("Heading", heading.get2DDataValue());
        tag.putInt("Scale", scale);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        if (box.isInside(center)) {
            LionTurtles.queue(level.getLevel().dimension(), center, heading, scale);
        }
    }
}
