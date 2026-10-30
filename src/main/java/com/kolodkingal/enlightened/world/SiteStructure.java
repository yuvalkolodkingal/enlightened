package com.kolodkingal.enlightened.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.function.Function;

/**
 * The code-built sites of 1.0 (Ba Sing Se, Omashu, a spirit portal, the Tree of Time): one structure type whose
 * {@code kind} picks the piece. Each wants ground in a height range, level enough round it.
 */
public class SiteStructure extends Structure {
    public enum Kind implements StringRepresentable {
        BA_SING_SE("ba_sing_se", 80, 16, 58, 140, BaSingSePiece::new),
        OMASHU("omashu", 60, 14, 58, 130, OmashuPiece::new),
        SPIRIT_PORTAL("spirit_portal", 8, 4, 50, 200, SpiritPortalPiece::new),
        TREE_OF_TIME("tree_of_time", 12, 8, 40, 200, TreeOfTimePiece::new);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        final String name;
        final int reach;
        final int level;
        final int minY;
        final int maxY;
        final Function<BlockPos, DesignPiece> piece;

        Kind(String name, int reach, int level, int minY, int maxY, Function<BlockPos, DesignPiece> piece) {
            this.name = name;
            this.reach = reach;
            this.level = level;
            this.minY = minY;
            this.maxY = maxY;
            this.piece = piece;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<SiteStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
            Kind.CODEC.fieldOf("kind").forGetter(s -> s.kind)).apply(i, SiteStructure::new));
    final Kind kind;

    public SiteStructure(StructureSettings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = height(context, x, z);
        if (y < kind.minY || y > kind.maxY) {
            return Optional.empty();
        }
        int r = kind.reach;
        for (int[] d : new int[][]{{r, 0}, {-r, 0}, {0, r}, {0, -r}, {r * 7 / 10, r * 7 / 10}, {-r * 7 / 10, r * 7 / 10},
                {r * 7 / 10, -r * 7 / 10}, {-r * 7 / 10, -r * 7 / 10}}) {
            if (Math.abs(height(context, x + d[0], z + d[1]) - y) > kind.level) {
                return Optional.empty();
            }
        }
        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(kind.piece.apply(origin))));
    }

    private static int height(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SITE.get();
    }
}
