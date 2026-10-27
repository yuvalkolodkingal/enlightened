package com.kolodkingal.enlightened.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The Southern Air Temple: built on a mountain top (the biome tag {@code has_structure/southern_air_temple}, mountain
 * biomes, and only where the ground stands high), as the Air Nomads built theirs. One code-built piece
 * ({@link AirTemplePiece}): a terrace on a stone plinth, a great tower and four lesser ones under blue cone roofs,
 * Guru Pathik meditating in the sanctuary, sky bison calves in their courtyard, flying lemurs, and chests with
 * gliders, saddles and food.
 */
public class AirTempleStructure extends Structure {
    public static final MapCodec<AirTempleStructure> CODEC = simpleCodec(AirTempleStructure::new);
    /** The lowest ground the temple stands on: a mountain, not a hill. */
    private static final int MIN_HEIGHT = 105;

    public AirTempleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = height(context, x, z);
        // The temple raises its own peak some 80 blocks: on ground too high it would reach past the top of the world.
        if (y < MIN_HEIGHT || y > 200) {
            return Optional.empty();
        }
        // On a peak, not a needle: the ground round it may fall away, but not into a gorge.
        int low = y;
        for (int[] d : new int[][]{{16, 0}, {-16, 0}, {0, 16}, {0, -16}}) {
            low = Math.min(low, height(context, x + d[0], z + d[1]));
        }
        if (y - low > 36) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new AirTemplePiece(origin))));
    }

    private static int height(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.AIR_TEMPLE.get();
    }
}
