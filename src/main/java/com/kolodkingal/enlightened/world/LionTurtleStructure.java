package com.kolodkingal.enlightened.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A lion turtle swimming in the deep ocean (the biome tag {@code has_structure/lion_turtle}): its island, and its head
 * at the island's front ({@link LionTurtlePiece}), where the sea is deep all round it.
 */
public class LionTurtleStructure extends Structure {
    public static final MapCodec<LionTurtleStructure> CODEC = simpleCodec(LionTurtleStructure::new);

    public LionTurtleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int sea = context.chunkGenerator().getSeaLevel() - 1;
        int r = LionTurtle.LENGTH + 4;
        for (int[] d : new int[][]{{0, 0}, {r, 0}, {-r, 0}, {0, r}, {0, -r}}) {
            int floor = context.chunkGenerator().getFirstOccupiedHeight(x + d[0], z + d[1], Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
            if (floor > sea - LionTurtle.DEPTH - 4) {
                return Optional.empty();
            }
        }
        BlockPos center = new BlockPos(x, sea, z);
        Direction heading = Direction.from2DDataValue(context.random().nextInt(4));
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new LionTurtlePiece(center, heading))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.LION_TURTLE.get();
    }
}
