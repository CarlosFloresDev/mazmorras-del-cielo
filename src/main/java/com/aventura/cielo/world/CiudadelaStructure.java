package com.aventura.cielo.world;

import com.aventura.cielo.registry.CieloWorld;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Ciudadela de las Nubes: la mazmorra mas grande (8 pisos + azotea). Muy rara; desde 1200 bloques del spawn
 * (a veces desde 700). Su base flota unos 40 bloques sobre el terreno.
 */
public class CiudadelaStructure extends Structure {
	public static final MapCodec<CiudadelaStructure> CODEC = simpleCodec(CiudadelaStructure::new);

	public CiudadelaStructure(Structure.StructureSettings settings) {
		super(settings);
	}

	public static boolean permitido(double distancia, float azar) {
		return distancia >= 1200 || distancia >= 700 && azar < 0.25F;
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		WorldgenRandom random = context.random();
		if (!permitido(Math.sqrt((double) x * x + (double) z * z), random.nextFloat())) {
			return Optional.empty();
		}
		int suelo = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
			context.randomState());
		int techo = context.heightAccessor().getMaxY() - CiudadelaPiece.PISOS * CiudadelaPiece.ALTO_PISO - 14;
		int y = Mth.clamp(suelo + 40, 110, techo);
		BlockPos origen = new BlockPos(x, y, z);
		long semilla = random.nextLong();
		return Optional.of(new Structure.GenerationStub(origen, builder -> builder.addPiece(new CiudadelaPiece(origen, semilla))));
	}

	@Override
	public StructureType<?> type() {
		return CieloWorld.CIUDADELA;
	}
}
