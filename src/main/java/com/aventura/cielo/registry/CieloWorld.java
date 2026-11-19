package com.aventura.cielo.registry;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.world.CieloPiece;
import com.aventura.cielo.world.CieloStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class CieloWorld {
	public static final StructureType<CieloStructure> CIELO = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, CieloMod.id("nave_del_cielo"),
		() -> CieloStructure.CODEC);
	public static final StructurePieceType PIEZA = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, CieloMod.id("pieza_del_cielo"),
		(StructurePieceType.ContextlessType) CieloPiece::new);

	public static final StructureType<com.aventura.cielo.world.CiudadelaStructure> CIUDADELA = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
		CieloMod.id("ciudadela"), () -> com.aventura.cielo.world.CiudadelaStructure.CODEC);
	public static final StructurePieceType PIEZA_CIUDADELA = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, CieloMod.id("pieza_ciudadela"),
		(StructurePieceType.ContextlessType) com.aventura.cielo.world.CiudadelaPiece::new);

    public static final StructureType<com.aventura.cielo.world.MazmorraStructure> MAZMORRA = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
        CieloMod.id("mazmorra"), () -> com.aventura.cielo.world.MazmorraStructure.CODEC);
    public static final StructurePieceType PIEZA_MAZMORRA = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, CieloMod.id("pieza_mazmorra"),
        (StructurePieceType.ContextlessType) com.aventura.cielo.world.MazmorraPiece::new);

	private CieloWorld() {
	}

	public static void init() {
		CieloMod.LOGGER.debug("Estructuras del cielo registradas");
	}
}
