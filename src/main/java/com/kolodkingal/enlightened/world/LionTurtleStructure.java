package com.kolodkingal.enlightened.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Where a lion turtle swims in the deep ocean (the biome tag {@code has_structure/lion_turtle}): a spot where the
 * floor lies deep under the whole of its island. Its island is far too great for one structure (the generator places
 * pieces no more than eight chunks from their start), so the structure only marks the spot ({@link LionTurtlePiece});
 * the island itself is raised by {@link LionTurtles}, a few rows a tick.
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
        int scale = LionTurtles.configuredScale();
        LionTurtle.Shape s = new LionTurtle.Shape(scale);
        int r = s.length() + 8;
        for (double ang = 0; ang < Math.PI * 2; ang += Math.PI / 4) {
            for (double f : new double[]{0.0, 0.5, 1.0}) {
                int px = x + (int) Math.round(Math.cos(ang) * r * f);
                int pz = z + (int) Math.round(Math.sin(ang) * r * f);
                int floor = context.chunkGenerator().getFirstOccupiedHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG,
                        context.heightAccessor(), context.randomState());
                if (floor > sea - s.depth() - 4) {
                    return Optional.empty();
                }
                if (f == 0.0) {
                    break;
                }
            }
        }
        BlockPos center = new BlockPos(x, sea, z);
        Direction heading = Direction.from2DDataValue(context.random().nextInt(4));
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new LionTurtlePiece(center, heading, scale))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.LION_TURTLE.get();
    }
}
