package com.kolodkingal.enlightened.world;

import com.kolodkingal.enlightened.Enlightened;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The temple's and the lion turtle's structure types and piece types (the structure, its set and its biomes are data: worldgen/). */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Enlightened.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Enlightened.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<AirTempleStructure>> AIR_TEMPLE = TYPES.register("air_temple",
            () -> () -> AirTempleStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> AIR_TEMPLE_PIECE = PIECES.register("air_temple",
            () -> (StructurePieceType.ContextlessType) AirTemplePiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<LionTurtleStructure>> LION_TURTLE = TYPES.register("lion_turtle",
            () -> () -> LionTurtleStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LION_TURTLE_PIECE = PIECES.register("lion_turtle",
            () -> (StructurePieceType.ContextlessType) LionTurtlePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<ElloraStructure>> ELLORA = TYPES.register("ellora",
            () -> () -> ElloraStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ELLORA_PIECE = PIECES.register("ellora",
            () -> (StructurePieceType.ContextlessType) ElloraPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<SiteStructure>> SITE = TYPES.register("site",
            () -> () -> SiteStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BA_SING_SE_PIECE = PIECES.register("ba_sing_se",
            () -> (StructurePieceType.ContextlessType) BaSingSePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OMASHU_PIECE = PIECES.register("omashu",
            () -> (StructurePieceType.ContextlessType) OmashuPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SPIRIT_PORTAL_PIECE = PIECES.register("spirit_portal",
            () -> (StructurePieceType.ContextlessType) SpiritPortalPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TREE_OF_TIME_PIECE = PIECES.register("tree_of_time",
            () -> (StructurePieceType.ContextlessType) TreeOfTimePiece::new);

    private ModStructures() {
    }
}
