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

    private ModStructures() {
    }
}
