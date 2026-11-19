package com.aventura.cielo.world;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.block.NucleoDelJefeBlock;
import com.aventura.cielo.registry.CieloBlocks;
import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.registry.CieloWorld;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Construye las naves bloque a bloque (sin plantillas NBT): globo aerostatico, avioneta, dirigible pirata
 * y fortaleza flotante. Cada chunk solo coloca lo que cae dentro de el, de forma determinista (semilla fija).
 */
public class CieloPiece extends StructurePiece {
	private final CieloStructure.Tipo tipo;
	private final BlockPos origen;
	private final boolean girado;
	private final long semilla;

	public CieloPiece(CieloStructure.Tipo tipo, BlockPos origen, boolean girado, long semilla) {
		super(CieloWorld.PIEZA, 0, caja(tipo, origen, girado));
		this.tipo = tipo;
		this.origen = origen;
		this.girado = girado;
		this.semilla = semilla;
	}

	public CieloPiece(CompoundTag tag) {
		super(CieloWorld.PIEZA, tag);
		this.tipo = CieloStructure.Tipo.valueOf(tag.getStringOr("Tipo", "GLOBO"));
		this.origen = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
		this.girado = tag.getBooleanOr("Girado", false);
		this.semilla = tag.getLongOr("Semilla", 0L);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putString("Tipo", this.tipo.name());
		tag.putInt("OX", this.origen.getX());
		tag.putInt("OY", this.origen.getY());
		tag.putInt("OZ", this.origen.getZ());
		tag.putBoolean("Girado", this.girado);
		tag.putLong("Semilla", this.semilla);
	}

	/** Extension local de cada nave: {minX, minY, minZ, maxX, maxY, maxZ}. */
	private static int[] extension(CieloStructure.Tipo tipo) {
		return switch (tipo) {
			case GLOBO -> new int[] {-9, -2, -9, 9, 29, 9};
			case AVIONETA -> new int[] {-16, -3, -16, 16, 10, 17};
			case DIRIGIBLE -> new int[] {-17, -4, -32, 17, 27, 28};
			case FORTALEZA -> new int[] {-23, -25, -23, 23, 15, 23};
		};
	}

	private static BoundingBox caja(CieloStructure.Tipo tipo, BlockPos origen, boolean girado) {
		int[] e = extension(tipo);
		int minY = tipo == CieloStructure.Tipo.FORTALEZA ? origen.getY() + e[1] : Math.max(-64, origen.getY() - 220);
		if (girado) {
			return new BoundingBox(origen.getX() - e[5], minY, origen.getZ() + e[0], origen.getX() - e[2], origen.getY() + e[4], origen.getZ() + e[3]);
		}
		return new BoundingBox(origen.getX() + e[0], minY, origen.getZ() + e[2], origen.getX() + e[3], origen.getY() + e[4], origen.getZ() + e[5]);
	}

	public static ResourceKey<LootTable> botin(String nombre) {
		return ResourceKey.create(Registries.LOOT_TABLE, CieloMod.id("chests/" + nombre));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
		BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		Constructor c = new Constructor(level, chunkBB, new Random(this.semilla));
		switch (this.tipo) {
			case GLOBO -> this.globo(c);
			case AVIONETA -> this.avioneta(c);
			case DIRIGIBLE -> this.dirigible(c);
			case FORTALEZA -> this.fortaleza(c);
		}
	}

	// =========================================================================================== helpers
	private final class Constructor {
		final WorldGenLevel level;
		final BoundingBox chunk;
		final Random random;

		Constructor(WorldGenLevel level, BoundingBox chunk, Random random) {
			this.level = level;
			this.chunk = chunk;
			this.random = random;
		}

		BlockPos mundo(int x, int y, int z) {
			return CieloPiece.this.girado ? CieloPiece.this.origen.offset(-z, y, x) : CieloPiece.this.origen.offset(x, y, z);
		}

		void set(int x, int y, int z, BlockState state) {
			BlockPos p = this.mundo(x, y, z);
			if (this.chunk.isInside(p)) {
				this.level.setBlock(p, CieloPiece.this.girado ? state.rotate(Rotation.CLOCKWISE_90) : state, Block.UPDATE_CLIENTS);
			}
		}

		void set(int x, int y, int z, Block block) {
			this.set(x, y, z, block.defaultBlockState());
		}

		void caja(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
					for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
						this.set(x, y, z, block);
					}
				}
			}
		}

		void cofre(int x, int y, int z, String botin, Direction mira) {
			BlockPos p = this.mundo(x, y, z);
			if (this.chunk.isInside(p)) {
				this.set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, mira));
				if (this.level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
					chest.setLootTable(botin(botin), this.random.nextLong() ^ p.asLong());
				}
			}
		}

		void spawner(int x, int y, int z, EntityType<?> type) {
			BlockPos p = this.mundo(x, y, z);
			if (this.chunk.isInside(p)) {
				this.set(x, y, z, Blocks.SPAWNER);
				if (this.level.getBlockEntity(p) instanceof SpawnerBlockEntity spawner) {
					spawner.setEntityId(type, this.level.getRandom());
				}
			}
		}

		void mob(EntityType<? extends Mob> type, int x, int y, int z) {
			BlockPos p = this.mundo(x, y, z);
			if (!this.chunk.isInside(p)) {
				return;
			}
			Mob mob = type.create(this.level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (mob != null) {
				mob.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
				mob.setPersistenceRequired();
				mob.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(p), EntitySpawnReason.STRUCTURE, null);
				this.level.addFreshEntityWithPassengers(mob);
			}
		}

		void nucleo(int x, int y, int z, NucleoDelJefeBlock.Jefe jefe) {
			this.set(x, y, z, CieloBlocks.NUCLEO_DEL_JEFE.defaultBlockState().setValue(NucleoDelJefeBlock.JEFE, jefe));
		}

		/** Escalera de cuerda desde (x, y, z) hacia abajo hasta tocar el suelo. */
		void cuerda(int x, int y, int z) {
			BlockPos top = this.mundo(x, y, z);
			if (top.getX() < this.chunk.minX() || top.getX() > this.chunk.maxX() || top.getZ() < this.chunk.minZ() || top.getZ() > this.chunk.maxZ()) {
				return;
			}
			int suelo = this.level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, top.getX(), top.getZ());
			for (int wy = top.getY(); wy > suelo && wy > top.getY() - 220; wy--) {
				BlockPos p = new BlockPos(top.getX(), wy, top.getZ());
				if (this.chunk.isInside(p) && (this.level.getBlockState(p).isAir() || wy == top.getY())) {
					this.level.setBlock(p, CieloBlocks.ESCALERA_DE_CUERDA.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}

		/** Elipsoide hueco (cascara de 1 bloque). {@code pintor} decide el bloque segun la posicion. */
		void globo(int cx, int cy, int cz, int rx, int ry, int rz, Pintor pintor) {
			for (int x = -rx; x <= rx; x++) {
				for (int y = -ry; y <= ry; y++) {
					for (int z = -rz; z <= rz; z++) {
						double d = sq(x / (rx + 0.5)) + sq(y / (ry + 0.5)) + sq(z / (rz + 0.5));
						double dIn = sq(x / (rx - 0.5)) + sq(y / (ry - 0.5)) + sq(z / (rz - 0.5));
						if (d <= 1.0 && dIn > 1.0) {
							BlockState s = pintor.pintar(x, y, z);
							if (s != null) {
								this.set(cx + x, cy + y, cz + z, s);
							}
						}
					}
				}
			}
		}
	}

	@FunctionalInterface
	private interface Pintor {
		BlockState pintar(int x, int y, int z);
	}

	private static double sq(double v) {
		return v * v;
	}

	private static BlockState lana(DyeColor color) {
		return Blocks.WOOL.pick(color).defaultBlockState();
	}

	private static BlockState concreto(DyeColor color) {
		return Blocks.CONCRETE.pick(color).defaultBlockState();
	}

	// =========================================================================================== GLOBO (facil)
	private void globo(Constructor c) {
		// canasta de mimbre
		c.caja(-3, 0, -3, 3, 0, 3, Blocks.SPRUCE_PLANKS);
		for (int i = -3; i <= 3; i++) {
			c.set(i, 1, -3, Blocks.STRIPPED_BAMBOO_BLOCK);
			c.set(i, 1, 3, Blocks.STRIPPED_BAMBOO_BLOCK);
			c.set(-3, 1, i, Blocks.STRIPPED_BAMBOO_BLOCK);
			c.set(3, 1, i, Blocks.STRIPPED_BAMBOO_BLOCK);
		}
		// cuerdas hacia el globo
		for (int y = 2; y <= 10; y++) {
			c.set(-3, y, -3, Blocks.OAK_FENCE);
			c.set(3, y, -3, Blocks.OAK_FENCE);
			c.set(-3, y, 3, Blocks.OAK_FENCE);
			c.set(3, y, 3, Blocks.OAK_FENCE);
		}
		// quemador
		for (int y = 6; y <= 10; y++) {
			c.set(0, y, 0, Blocks.IRON_CHAIN);
		}
		c.set(0, 5, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		// globo con franjas (rojo, blanco, amarillo: colores andinos)
		DyeColor[] franjas = {DyeColor.RED, DyeColor.WHITE, DyeColor.YELLOW, DyeColor.WHITE};
		c.globo(0, 19, 0, 8, 9, 8, (x, y, z) -> {
			if (y < -6 && Math.abs(x) <= 2 && Math.abs(z) <= 2) {
				return null; // boca del globo
			}
			int sector = (int) Math.floor((Math.atan2(z, x) + Math.PI) / (Math.PI * 2) * 8);
			return lana(franjas[sector % franjas.length]);
		});
		// sacos de arena
		c.set(-4, 1, 0, Blocks.WOOL.pick(DyeColor.BROWN));
		c.set(4, 1, 0, Blocks.WOOL.pick(DyeColor.BROWN));
		// botin y tripulacion
		c.cofre(2, 1, -2, "globo", Direction.NORTH);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, -1, 1, 1);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, 1, 1, 0);
		if (c.random.nextFloat() < 0.4F) {
			c.mob(CieloEntities.ARCABUCERO_DEL_CIELO, 0, 1, -1);
		}
		// escalera de cuerda por un hueco en el piso
		c.cuerda(-2, 0, 2);
	}

	// =========================================================================================== AVIONETA (media)
	private void avioneta(Constructor c) {
		BlockState blanco = concreto(DyeColor.WHITE);
		BlockState rojo = concreto(DyeColor.RED);
		BlockState negro = concreto(DyeColor.BLACK);
		// fuselaje (largo en z, la nariz mira a +z)
		for (int z = -14; z <= 12; z++) {
			int ancho = z < -8 ? 1 : 2;
			for (int x = -ancho; x <= ancho; x++) {
				for (int y = 0; y <= 3; y++) {
					boolean borde = Math.abs(x) == ancho || y == 0 || y == 3;
					boolean cabina = z >= 3 && z <= 6 && y == 3 && Math.abs(x) < ancho;
					if (cabina) {
						continue;
					}
					if (borde) {
						c.set(x, y, z, y == 2 && Math.abs(x) == ancho ? rojo : blanco);
					} else {
						c.set(x, y, z, Blocks.AIR);
					}
				}
			}
			c.set(0, 0, z, Blocks.SPRUCE_PLANKS);
		}
		for (int x = -1; x <= 1; x++) {
			c.set(x, 4, 7, Blocks.GLASS_PANE);
			c.set(x, 3, 7, Blocks.GLASS_PANE);
		}
		// nariz y helice
		c.caja(-1, 0, 13, 1, 2, 14, Blocks.CONCRETE.pick(DyeColor.BLACK));
		for (int i = -3; i <= 3; i++) {
			c.set(0, 1 + i, 15, Blocks.POLISHED_BLACKSTONE);
			c.set(i, 1, 15, Blocks.POLISHED_BLACKSTONE);
		}
		c.set(0, 1, 16, Blocks.IRON_BLOCK);
		// alas (biplano)
		for (int y : new int[] {0, 6}) {
			for (int x = -14; x <= 14; x++) {
				for (int z = -1; z <= 4; z++) {
					if (Math.abs(x) <= 2 && y == 0) {
						continue;
					}
					c.set(x, y, z, Math.abs(x) >= 13 ? rojo : blanco);
				}
			}
		}
		for (int x : new int[] {-12, -6, 6, 12}) {
			for (int y = 1; y <= 5; y++) {
				c.set(x, y, 0, Blocks.OAK_FENCE);
				c.set(x, y, 3, Blocks.OAK_FENCE);
			}
		}
		// cola
		for (int z = -14; z <= -11; z++) {
			for (int y = 4; y <= 8; y++) {
				c.set(0, y, z, y == 8 ? rojo : blanco);
			}
			for (int x = -6; x <= 6; x++) {
				c.set(x, 3, z, Math.abs(x) >= 5 ? rojo : blanco);
			}
		}
		// tren de aterrizaje decorativo
		c.set(-2, -1, 8, negro);
		c.set(2, -1, 8, negro);
		c.set(-2, -2, 8, Blocks.WOOL.pick(DyeColor.BLACK));
		c.set(2, -2, 8, Blocks.WOOL.pick(DyeColor.BLACK));
		// botin, spawner y tripulacion
		c.cofre(0, 1, -3, "avioneta", Direction.SOUTH);
		c.cofre(0, 1, -6, "avioneta", Direction.SOUTH);
		c.spawner(0, 1, 0, CieloEntities.MECANICO_DEL_DIRIGIBLE);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, 8, 1, 1);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, -8, 1, 1);
		c.mob(CieloEntities.ARCABUCERO_DEL_CIELO, 9, 7, 1);
		c.mob(CieloEntities.ARCABUCERO_DEL_CIELO, -9, 7, 1);
		c.mob(CieloEntities.MECANICO_DEL_DIRIGIBLE, 0, 1, 4);
		c.cuerda(10, -1, 1);
	}

	// =========================================================================================== DIRIGIBLE (dificil + jefe)
	private void dirigible(Constructor c) {
		// gondola con forma de barco
		for (int z = -18; z <= 18; z++) {
			int ancho = (int) Math.round(6 - Math.max(0, Math.abs(z) - 12) / 1.4);
			for (int x = -ancho; x <= ancho; x++) {
				c.set(x, 0, z, Blocks.DARK_OAK_PLANKS);
				if (Math.abs(x) <= ancho - 1) {
					c.set(x, -1, z, Blocks.SPRUCE_PLANKS);
				}
				if (Math.abs(x) <= ancho - 3) {
					c.set(x, -2, z, Blocks.DARK_OAK_LOG);
				}
			}
			c.set(-ancho, 1, z, Blocks.DARK_OAK_FENCE);
			c.set(ancho, 1, z, Blocks.DARK_OAK_FENCE);
			c.set(-ancho - 1, 0, z, Blocks.SPRUCE_PLANKS);
			c.set(ancho + 1, 0, z, Blocks.SPRUCE_PLANKS);
		}
		// puente de mando (cabina) adelante
		for (int z = 8; z <= 14; z++) {
			for (int x = -4; x <= 4; x++) {
				for (int y = 1; y <= 4; y++) {
					boolean pared = Math.abs(x) == 4 || z == 8 || z == 14 || y == 4;
					if (pared) {
						boolean ventana = y == 2 && (Math.abs(x) == 4 && z % 2 == 0 || z == 14 && Math.abs(x) <= 2);
						c.set(x, y, z, ventana ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
					} else {
						c.set(x, y, z, Blocks.AIR);
					}
				}
			}
		}
		c.set(0, 1, 8, Blocks.AIR);
		c.set(0, 2, 8, Blocks.AIR);
		c.set(0, 1, 13, Blocks.LECTERN);
		c.cofre(-3, 1, 13, "dirigible", Direction.EAST);
		c.cofre(3, 1, 13, "dirigible", Direction.WEST);
		c.set(0, 3, 11, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		// cañones a los lados
		for (int z : new int[] {-12, -4, 4}) {
			c.set(-6, 1, z, Blocks.POLISHED_BLACKSTONE);
			c.set(-7, 1, z, Blocks.IRON_BARS);
			c.set(6, 1, z, Blocks.POLISHED_BLACKSTONE);
			c.set(7, 1, z, Blocks.IRON_BARS);
		}
		// tirantes hacia el globo
		for (int y = 1; y <= 8; y++) {
			for (int z : new int[] {-10, 0, 10}) {
				c.set(-4, y + (z == 10 ? 4 : 0), z, Blocks.IRON_CHAIN);
				c.set(4, y + (z == 10 ? 4 : 0), z, Blocks.IRON_CHAIN);
			}
		}
		// globo alargado con costillas
		c.globo(0, 18, 0, 10, 9, 26, (x, y, z) -> Math.floorMod(z, 6) == 0
			? Blocks.DARK_OAK_PLANKS.defaultBlockState()
			: (y > 4 ? lana(DyeColor.LIGHT_GRAY) : lana(DyeColor.GRAY)));
		// aletas de cola
		for (int z = -31; z <= -23; z++) {
			for (int y = 18; y <= 29 - (z + 31) / 2; y++) {
				c.set(0, y, z, Blocks.DARK_OAK_PLANKS);
			}
			for (int x = -15 + (z + 31) / 2; x <= 15 - (z + 31) / 2; x++) {
				if (Math.abs(x) > 4) {
					c.set(x, 18, z, lana(DyeColor.RED));
				}
			}
		}
		// motores con helices
		for (int x : new int[] {-12, 12}) {
			c.caja(x, 9, -17, x, 10, -14, Blocks.POLISHED_BLACKSTONE);
			for (int i = -2; i <= 2; i++) {
				c.set(x, 10 + i, -18, Blocks.IRON_BLOCK);
				c.set(x + i, 10, -18, Blocks.IRON_BLOCK);
			}
		}
		// spawners, tripulacion y el capitan en la cubierta de popa
		c.spawner(-4, 1, 4, CieloEntities.PIRATA_DEL_CIELO);
		c.spawner(4, 1, 4, CieloEntities.ARCABUCERO_DEL_CIELO);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, -2, 1, 2);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, 2, 1, -2);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, 0, 1, 6);
		c.mob(CieloEntities.ARCABUCERO_DEL_CIELO, -3, 5, 11);
		c.mob(CieloEntities.ARCABUCERO_DEL_CIELO, 3, 5, 11);
		c.mob(CieloEntities.MECANICO_DEL_DIRIGIBLE, 0, 1, -14);
		c.mob(CieloEntities.MECANICO_DEL_DIRIGIBLE, -3, 1, -6);
		for (int x = -2; x <= 2; x++) {
			for (int z = -11; z <= -7; z++) {
				c.set(x, 1, z, Blocks.CARPET.pick(DyeColor.RED));
			}
		}
		c.nucleo(0, 0, -9, NucleoDelJefeBlock.Jefe.CAPITAN);
		c.cuerda(0, -3, 14);
	}

	// =========================================================================================== FORTALEZA DEL CONDOR (jefe final)
	private void fortaleza(Constructor c) {
		// isla flotante (cono invertido irregular)
		for (int x = -22; x <= 22; x++) {
			for (int z = -22; z <= 22; z++) {
				double r = Math.sqrt(x * x + z * z);
				long h = (long) x * 341873128712L + (long) z * 132897987541L + CieloPiece.this.semilla;
				double ruido = ((h >>> 20) & 7) / 7.0 * 2.5;
				if (r > 21 - ruido * 0.5) {
					continue;
				}
				int fondo = (int) (-24 * Math.pow(1 - r / 21.5, 0.7) - ruido);
				for (int y = fondo; y <= 0; y++) {
					BlockState s;
					if (y == 0) {
						s = r <= 15 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
					} else if (y > -3) {
						s = Blocks.DIRT.defaultBlockState();
					} else {
						long v = h ^ (y * 2654435761L);
						int k = (int) ((v >>> 16) & 255);
						s = k < 2 ? Blocks.DIAMOND_ORE.defaultBlockState()
							: k < 8 ? Blocks.GOLD_ORE.defaultBlockState()
							: k < 60 ? Blocks.TUFF.defaultBlockState() : Blocks.STONE.defaultBlockState();
					}
					c.set(x, y, z, s);
				}
			}
		}
		// arena circular con muro bajo
		for (int x = -16; x <= 16; x++) {
			for (int z = -16; z <= 16; z++) {
				double r = Math.sqrt(x * x + z * z);
				if (r <= 3) {
					c.set(x, 0, z, Blocks.CHISELED_STONE_BRICKS);
				} else if (r <= 15 && (Math.abs(x) == Math.abs(z))) {
					c.set(x, 0, z, Blocks.STONE_BRICKS);
				}
				if (r > 15 && r <= 16 && Math.floorMod((int) Math.round(Math.atan2(z, x) * 10), 7) != 0) {
					c.set(x, 1, z, Blocks.ANDESITE_WALL);
				}
			}
		}
		// nido del condor
		for (int i = 0; i < 16; i++) {
			double a = i * Math.PI * 2 / 16;
			c.set((int) Math.round(Math.cos(a) * 3), 1, (int) Math.round(Math.sin(a) * 3), i % 2 == 0 ? Blocks.HAY_BLOCK : Blocks.DARK_OAK_LOG);
		}
		c.nucleo(0, 1, 0, NucleoDelJefeBlock.Jefe.CONDOR);
		// cuatro torres con arcabuceros
		int[][] torres = {{11, 11}, {-11, 11}, {11, -11}, {-11, -11}};
		for (int t = 0; t < torres.length; t++) {
			int tx = torres[t][0];
			int tz = torres[t][1];
			for (int y = 1; y <= 12; y++) {
				for (int x = -3; x <= 3; x++) {
					for (int z = -3; z <= 3; z++) {
						double r = Math.sqrt(x * x + z * z);
						if (y < 12 && r <= 2.6 && r > 1.6) {
							boolean puerta = y <= 2 && x == 0 && z == (tz > 0 ? -2 : 2);
							c.set(tx + x, y, tz + z, puerta ? Blocks.AIR.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
						} else if (y == 12 && r <= 3.2) {
							c.set(tx + x, y, tz + z, Blocks.STONE_BRICK_SLAB);
						} else if (y < 12 && r <= 1.6) {
							c.set(tx + x, y, tz + z, Blocks.AIR);
						}
					}
				}
				c.set(tx, y, tz + (tz > 0 ? 1 : -1), Blocks.LADDER.defaultBlockState()
					.setValue(LadderBlock.FACING, tz > 0 ? Direction.NORTH : Direction.SOUTH));
			}
			for (int i = 0; i < 8; i++) {
				double a = i * Math.PI / 4;
				c.set(tx + (int) Math.round(Math.cos(a) * 3), 13, tz + (int) Math.round(Math.sin(a) * 3), Blocks.STONE_BRICK_WALL);
			}
			c.set(tx, 13, tz, Blocks.SOUL_LANTERN);
			c.mob(CieloEntities.ARCABUCERO_DEL_CIELO, tx + 1, 13, tz + 1);
			c.cofre(tx + 1, 1, tz, "fortaleza", tz > 0 ? Direction.NORTH : Direction.SOUTH);
			if (t % 2 == 0) {
				c.spawner(tx - 1, 1, tz, CieloEntities.ARCABUCERO_DEL_CIELO);
			}
		}
		c.mob(CieloEntities.PIRATA_DEL_CIELO, 6, 1, 0);
		c.mob(CieloEntities.PIRATA_DEL_CIELO, -6, 1, 0);
		c.mob(CieloEntities.MECANICO_DEL_DIRIGIBLE, 0, 1, 7);
	}
}
