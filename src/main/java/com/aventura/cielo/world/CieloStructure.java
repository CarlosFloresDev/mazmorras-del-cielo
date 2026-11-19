package com.aventura.cielo.world;

import com.aventura.cielo.registry.CieloWorld;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Mazmorra del cielo. Flota muy por encima del terreno, asi que no altera la generacion del mundo.
 * La dificultad depende de la distancia al centro del mundo (donde aparecen los jugadores):
 * <ul>
 *   <li>Globo aerostatico: desde 300 bloques (sobre todo entre 300 y 1600).</li>
 *   <li>Avioneta: desde 900 bloques (a veces desde 450).</li>
 *   <li>Dirigible pirata (jefe: Capitan Barbanube): desde 1700 (a veces desde 900).</li>
 *   <li>Fortaleza del Condor (jefe final): desde 3000 (a veces desde 2000).</li>
 * </ul>
 */
public class CieloStructure extends Structure {
	public enum Tipo implements StringRepresentable {
		GLOBO(45, 115), AVIONETA(55, 125), DIRIGIBLE(55, 135), FORTALEZA(70, 165);

		public static final Codec<Tipo> CODEC = StringRepresentable.fromEnum(Tipo::values);
		/** Altura sobre el terreno y altura minima absoluta. */
		final int sobreTerreno;
		final int minimo;

		Tipo(int sobreTerreno, int minimo) {
			this.sobreTerreno = sobreTerreno;
			this.minimo = minimo;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase();
		}
	}

	public static final MapCodec<CieloStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		settingsCodec(i),
		Tipo.CODEC.fieldOf("tipo").forGetter(s -> s.tipo)
	).apply(i, CieloStructure::new));

	private final Tipo tipo;

	public CieloStructure(Structure.StructureSettings settings, Tipo tipo) {
		super(settings);
		this.tipo = tipo;
	}

	/** ¿Puede aparecer este tipo a esta distancia del centro? */
	public static boolean permitido(Tipo tipo, double distancia, float azar) {
		return switch (tipo) {
			case GLOBO -> distancia >= 300 && (distancia <= 1600 || azar < 0.35F);
			case AVIONETA -> distancia >= 900 || (distancia >= 450 && azar < 0.2F);
			case DIRIGIBLE -> distancia >= 1700 || (distancia >= 900 && azar < 0.12F);
			case FORTALEZA -> distancia >= 3000 || (distancia >= 2000 && azar < 0.1F);
		};
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		WorldgenRandom random = context.random();
		if (!permitido(this.tipo, Math.sqrt((double) x * x + (double) z * z), random.nextFloat())) {
			return Optional.empty();
		}
		int suelo = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
			context.randomState());
		int techo = context.heightAccessor().getMaxY() - 40;
		int y = Mth.clamp(Math.max(suelo + this.tipo.sobreTerreno, this.tipo.minimo), this.tipo.minimo, techo);
		BlockPos origen = new BlockPos(x, y, z);
		boolean girado = random.nextBoolean();
		long semilla = random.nextLong();
		return Optional.of(new Structure.GenerationStub(origen, builder -> builder.addPiece(new CieloPiece(this.tipo, origen, girado, semilla))));
	}

	@Override
	public StructureType<?> type() {
		return CieloWorld.CIELO;
	}
}
