package com.kolodkingal.enlightened.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Ellora, the golden city of Seven Havens: built in the desert and the badlands (the biome tag
 * {@code has_structure/ellora}) where the ground is level enough for its terraces. One code-built piece
 * ({@link ElloraPiece}).
 */
public class ElloraStructure extends Structure {
    public static final MapCodec<ElloraStructure> CODEC = simpleCodec(ElloraStructure::new);

    public ElloraStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = height(context, x, z);
        if (y < 58 || y > 130) {
            return Optional.empty();
        }
        // Level ground: the city's arms reach 50 blocks out and stand on the desert, not over a canyon.
        for (int[] d : new int[][]{{36, 36}, {-36, 36}, {36, -36}, {-36, -36}, {30, 0}, {-30, 0}, {0, 30}, {0, -30}}) {
            if (Math.abs(height(context, x + d[0], z + d[1]) - y) > 14) {
                return Optional.empty();
            }
        }
        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ElloraPiece(origin))));
    }

    private static int height(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ELLORA.get();
    }
}
