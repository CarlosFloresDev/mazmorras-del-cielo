package com.aventura.cielo.world;

import com.aventura.cielo.block.NucleoDelJefeBlock;
import com.aventura.cielo.entity.MercaderDeLasNubes;
import com.aventura.cielo.registry.CieloBlocks;
import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.registry.CieloItems;
import com.aventura.cielo.registry.CieloWorld;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Ciudadela de las Nubes: mazmorra vertical inmensa generada al azar (pero igual para todos con la misma semilla).
 * <ul>
 *   <li>8 pisos de salas flotantes unidas por puentes (laberinto: arbol al azar + algunos atajos).</li>
 *   <li>Pisos 1-2: 3x3 salas; 3-5: 4x4; 6-8: 5x5. Mas arriba: mas monstruos, puentes rotos/angostos y sin barandas.</li>
 *   <li>La escalera al piso siguiente esta en la sala mas lejana y la cierra un Sello Celestial:
 *       hay que encontrar la Llave Celestial del piso (en otra sala lejana).</li>
 *   <li>Piso 4: el Capitan Barbanube guarda la escalera. Azotea: arena del Condor de Acero y el tesoro.</li>
 * </ul>
 * La entrada es una escalera de cuerda que baja hasta el suelo desde la primera sala.
 */
public class CiudadelaPiece extends StructurePiece {
	public static final int PISOS = 8;
	public static final int ALTO_PISO = 14;
	public static final int CELDA = 13;
	/** Media sala: las salas son de 9x9. */
	public static final int R = 4;
	/** Rejilla global de 5x5 celdas (los pisos chicos usan una ventana de 3x3 o 4x4). */
	public static final int REJILLA = 5;
	public static final int EXTENSION = 34;

	private static final Map<Long, Plan> PLANES = new ConcurrentHashMap<>();

	private final BlockPos origen;
	private final long semilla;

	public CiudadelaPiece(BlockPos origen, long semilla) {
		super(CieloWorld.PIEZA_CIUDADELA, 0, caja(origen));
		this.origen = origen;
		this.semilla = semilla;
	}

	public CiudadelaPiece(CompoundTag tag) {
		super(CieloWorld.PIEZA_CIUDADELA, tag);
		this.origen = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
		this.semilla = tag.getLongOr("Semilla", 0L);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("OX", this.origen.getX());
		tag.putInt("OY", this.origen.getY());
		tag.putInt("OZ", this.origen.getZ());
		tag.putLong("Semilla", this.semilla);
	}

	/** Toda la columna (hasta el fondo del mundo) es zona de la ciudadela: no se puede construir para saltarse pisos. */
	private static BoundingBox caja(BlockPos o) {
		return new BoundingBox(o.getX() - EXTENSION, -64, o.getZ() - EXTENSION, o.getX() + EXTENSION, o.getY() + PISOS * ALTO_PISO + 12,
			o.getZ() + EXTENSION);
	}

	// ================================================================== plan (se calcula una vez por semilla)

	/** Una parte de la construccion con su caja local, para dibujar solo lo que cae en cada chunk. */
	record Elemento(int x1, int y1, int z1, int x2, int y2, int z2, Consumer<Constructor> dibujar) {
	}

	record Celda(int i, int j) {
		int x() {
			return (this.i - REJILLA / 2) * CELDA;
		}

		int z() {
			return (this.j - REJILLA / 2) * CELDA;
		}
	}

	enum Puente { NORMAL, SIN_BARANDA, ANGOSTO, ROTO, CRISTAL }

	public enum TipoSala { MERCADER, FLECHAS, FOSO, PISTONES, BIBLIOTECA, JARDIN }
	/** Centro a la altura del suelo; vista inmutable para pruebas y herramientas. */
	public record SalaEspecial(int piso, BlockPos centro, TipoSala tipo) { }
	record SalaPlan(int piso, Celda celda, TipoSala tipo) { }

	static final class Plan {
		final List<Elemento> elementos = new ArrayList<>();
		final List<Elemento> escaleras = new ArrayList<>();
		final List<SalaPlan> salas = new ArrayList<>();
		/** Por piso: celda de llegada, de la escalera y de la llave. */
		final Celda[] llegada = new Celda[PISOS + 1];
		final Celda[] subida = new Celda[PISOS];
		final Celda[] llave = new Celda[PISOS];
	}

	Plan plan() {
		long clave = this.semilla ^ this.origen.asLong() * 31;
		if (PLANES.size() > 32) {
			PLANES.clear();
		}
		return PLANES.computeIfAbsent(clave, k -> generar(new Random(this.semilla), this.semilla));
	}

	private static int tamano(int piso) {
		return piso < 2 ? 3 : piso < 5 ? 4 : 5;
	}

	/** Tema por tercio de la torre: {plataforma, acento, baranda, puente, debajo, luz}. */
	private static Block[] tema(int piso) {
		if (piso < 3) {
			return new Block[] {Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICK_WALL, Blocks.OAK_PLANKS, Blocks.STONE,
				Blocks.LANTERN};
		}
		if (piso < 6) {
			return new Block[] {Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_WALL, Blocks.DARK_OAK_PLANKS,
				Blocks.COBBLED_DEEPSLATE, Blocks.SOUL_LANTERN};
		}
		return new Block[] {Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.END_STONE_BRICK_WALL, Blocks.END_STONE_BRICKS, Blocks.END_STONE,
			Blocks.END_ROD};
	}

	private static Plan generar(Random random, long semilla) {
		Plan plan = new Plan();
		// primera sala: borde de la ventana 3x3 central
		int[][] bordes = {{1, 2}, {3, 2}, {2, 1}, {2, 3}};
		int[] b = bordes[random.nextInt(bordes.length)];
		plan.llegada[0] = new Celda(b[0], b[1]);
		for (int piso = 0; piso < PISOS; piso++) {
			generarPiso(plan, piso, random, new Random(semilla ^ (0x4349454c4fL * (piso + 1))));
			plan.llegada[piso + 1] = plan.subida[piso];
		}
		azotea(plan, random);
		// La circulacion gana a plataformas, bases, decoracion y botin de todos los pisos.
		plan.elementos.addAll(plan.escaleras);
		// entrada: escalera de cuerda desde la primera sala hasta el suelo
		Celda e = plan.llegada[0];
		plan.elementos.add(new Elemento(e.x() + R - 1, -260, e.z(), e.x() + R, 2, e.z(), c -> c.cuerda(e.x() + R, 0, e.z())));
		return plan;
	}

	private static void generarPiso(Plan plan, int piso, Random random, Random salasRandom) {
		int g = tamano(piso);
		Celda llegada = plan.llegada[piso];
		// ventana de g x g dentro de la rejilla de 5x5 que contenga la celda de llegada
		List<int[]> ventanas = new ArrayList<>();
		for (int oi = 0; oi <= REJILLA - g; oi++) {
			for (int oj = 0; oj <= REJILLA - g; oj++) {
				if (llegada.i() >= oi && llegada.i() < oi + g && llegada.j() >= oj && llegada.j() < oj + g) {
					ventanas.add(new int[] {oi, oj});
				}
			}
		}
		int[] v = ventanas.get(random.nextInt(ventanas.size()));
		List<Celda> celdas = new ArrayList<>();
		for (int i = v[0]; i < v[0] + g; i++) {
			for (int j = v[1]; j < v[1] + g; j++) {
				celdas.add(new Celda(i, j));
			}
		}
		Set<Celda> dentro = new HashSet<>(celdas);

		// laberinto: DFS al azar desde la llegada + atajos
		Set<List<Celda>> uniones = new HashSet<>();
		Set<Celda> visitadas = new HashSet<>();
		Deque<Celda> pila = new ArrayDeque<>();
		pila.push(llegada);
		visitadas.add(llegada);
		while (!pila.isEmpty()) {
			Celda actual = pila.peek();
			List<Celda> vecinas = new ArrayList<>();
			for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				Celda n = new Celda(actual.i() + d[0], actual.j() + d[1]);
				if (dentro.contains(n) && !visitadas.contains(n)) {
					vecinas.add(n);
				}
			}
			if (vecinas.isEmpty()) {
				pila.pop();
				continue;
			}
			Celda n = vecinas.get(random.nextInt(vecinas.size()));
			uniones.add(par(actual, n));
			visitadas.add(n);
			pila.push(n);
		}
		for (Celda c : celdas) {
			for (int[] d : new int[][] {{1, 0}, {0, 1}}) {
				Celda n = new Celda(c.i() + d[0], c.j() + d[1]);
				if (dentro.contains(n) && random.nextFloat() < 0.18F) {
					uniones.add(par(c, n));
				}
			}
		}

		// distancias desde la llegada (BFS) -> escalera en la mas lejana, llave en otra lejana
		Map<Celda, Integer> dist = new HashMap<>();
		Deque<Celda> cola = new ArrayDeque<>();
		cola.add(llegada);
		dist.put(llegada, 0);
		while (!cola.isEmpty()) {
			Celda c = cola.poll();
			for (Celda n : celdas) {
				if (!dist.containsKey(n) && uniones.contains(par(c, n))) {
					dist.put(n, dist.get(c) + 1);
					cola.add(n);
				}
			}
		}
		List<Celda> orden = new ArrayList<>(celdas);
		Collections.shuffle(orden, random);
		orden.sort((a, c) -> dist.get(c) - dist.get(a));
		Celda subida = orden.get(0);
		Celda llave = orden.size() > 2 ? orden.get(1 + random.nextInt(Math.min(2, orden.size() - 2))) : orden.get(1);
		if (llave.equals(llegada)) {
			llave = orden.get(1);
		}
		plan.subida[piso] = subida;
		plan.llave[piso] = llave;
		// RNG independiente: las salas nuevas no cambian el laberinto ni sus escaleras.
		List<Celda> ordinarias = new ArrayList<>(celdas);
		ordinarias.removeIf(c -> c.equals(llegada) || c.equals(subida) || c.equals(plan.llave[piso]));
		Collections.shuffle(ordinarias, salasRandom);
		int cantidad = piso < 2 ? 1 : piso < 5 ? 2 : 3;
		Map<Celda, TipoSala> especiales = new HashMap<>();
		for (int n = 0; n < cantidad; n++) {
			TipoSala tipo;
			if (n == 0 && piso % 2 == 0) tipo = TipoSala.MERCADER;
			else if (n == cantidad - 1 && (cantidad > 1 || piso == 1))
				tipo = salasRandom.nextBoolean() ? TipoSala.BIBLIOTECA : TipoSala.JARDIN;
			else tipo = TipoSala.values()[1 + salasRandom.nextInt(piso < 3 ? 2 : 3)];
			Celda celda = ordinarias.get(n);
			especiales.put(celda, tipo);
			plan.salas.add(new SalaPlan(piso, celda, tipo));
		}

		int y = piso * ALTO_PISO;
		Block[] t = tema(piso);
		for (Celda c : celdas) {
			boolean esLlegada = c.equals(llegada);
			boolean esSubida = c.equals(subida);
			boolean esLlave = c.equals(llave);
			Set<Direction> salidas = new HashSet<>();
			for (Celda n : celdas) {
				if (uniones.contains(par(c, n))) {
					salidas.add(n.i() > c.i() ? Direction.EAST : n.i() < c.i() ? Direction.WEST : n.j() > c.j() ? Direction.SOUTH : Direction.NORTH);
				}
			}
			if (esSubida) {
				Direction puerta = salidas.isEmpty() ? Direction.NORTH : salidas.iterator().next();
				plan.escaleras.add(new Elemento(c.x() - 2, y, c.z() - 2, c.x() + 2, y + ALTO_PISO + 2, c.z() + 2,
					k -> escalera(k, c.x(), c.z(), y, t, puerta)));
			}
			long semillaSala = random.nextLong();
			int pisoFinal = piso;
			plan.elementos.add(new Elemento(c.x() - R - 1, y - 4, c.z() - R - 1, c.x() + R + 1, y + ALTO_PISO, c.z() + R + 1,
				k -> sala(k, pisoFinal, c, t, salidas, esLlegada && pisoFinal > 0, esSubida, esLlave, especiales.get(c), new Random(semillaSala))));
		}
		for (List<Celda> u : uniones) {
			Celda a = u.get(0);
			Celda c = u.get(1);
			Puente tipo = elegirPuente(piso, random);
			long semillaPuente = random.nextLong();
			boolean enX = a.j() == c.j();
			// el hueco entre salas va de inicio a inicio+3; la linea central es la del centro de las salas
			int inicio = enX ? Math.min(a.x(), c.x()) + R + 1 : Math.min(a.z(), c.z()) + R + 1;
			int centro = enX ? a.z() : a.x();
			int bx1 = enX ? inicio : centro - 2;
			int bx2 = enX ? inicio + 3 : centro + 2;
			int bz1 = enX ? centro - 2 : inicio;
			int bz2 = enX ? centro + 2 : inicio + 3;
			plan.elementos.add(new Elemento(bx1, y - 1, bz1, bx2, y + 2, bz2,
				k -> puente(k, y, inicio, centro, enX, tipo, t, new Random(semillaPuente))));
		}
	}

	private static List<Celda> par(Celda a, Celda b) {
		return a.i() < b.i() || a.i() == b.i() && a.j() < b.j() ? List.of(a, b) : List.of(b, a);
	}

	private static Puente elegirPuente(int piso, Random random) {
		float r = random.nextFloat();
		if (piso < 2) {
			return r < 0.8F ? Puente.NORMAL : Puente.SIN_BARANDA;
		}
		if (piso < 5) {
			return r < 0.35F ? Puente.NORMAL : r < 0.6F ? Puente.SIN_BARANDA : r < 0.8F ? Puente.ROTO : Puente.ANGOSTO;
		}
		return r < 0.15F ? Puente.CRISTAL : r < 0.4F ? Puente.SIN_BARANDA : r < 0.7F ? Puente.ROTO : Puente.ANGOSTO;
	}

	// ================================================================== dibujo

	/** Puente sobre el hueco de 4 bloques entre dos salas. {@code enX}: va a lo largo de x (si no, de z). */
	private static void puente(Constructor k, int y, int inicio, int centro, boolean enX, Puente tipo, Block[] t, Random random) {
		int ancho = tipo == Puente.ANGOSTO ? 0 : 1;
		int hueco = tipo == Puente.ROTO ? random.nextInt(4) : -1;
		Block piso = tipo == Puente.CRISTAL ? Blocks.GLASS : t[3];
		for (int s = 0; s < 4; s++) {
			int largo = inicio + s;
			if (s != hueco) {
				for (int w = -ancho; w <= ancho; w++) {
					k.set(enX ? largo : centro + w, y, enX ? centro + w : largo, piso);
				}
			}
			if (tipo == Puente.NORMAL) {
				for (int lado : new int[] {-2, 2}) {
					int x = enX ? largo : centro + lado;
					int z = enX ? centro + lado : largo;
					k.set(x, y, z, t[3]);
					k.set(x, y + 1, z, t[2]);
				}
			}
		}
	}

	private static void sala(Constructor k, int piso, Celda c, Block[] t, Set<Direction> salidas, boolean llegadaDesdeAbajo,
		boolean subida, boolean llave, TipoSala especial, Random random) {
		int y = piso * ALTO_PISO;
		int cx = c.x();
		int cz = c.z();
		// plataforma 9x9 con borde de acento y "isla" de piedra debajo
		for (int dx = -R; dx <= R; dx++) {
			for (int dz = -R; dz <= R; dz++) {
				boolean borde = Math.abs(dx) == R || Math.abs(dz) == R;
				k.set(cx + dx, y, cz + dz, borde || random.nextInt(9) == 0 ? t[1] : t[0]);
			}
		}
		for (int capa = 1; capa <= 3; capa++) {
			int r = R - capa * 1 - (capa == 3 ? 1 : 0);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (random.nextInt(6) != 0 || capa == 1) {
						k.set(cx + dx, y - capa, cz + dz, t[4]);
					}
				}
			}
		}
		// el hueco por donde se llega desde la escalera de abajo
		if (llegadaDesdeAbajo) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx != 0 || dz != 0) {
						for (int capa = 0; capa <= 3; capa++) {
							k.set(cx + dx, y - capa, cz + dz, Blocks.AIR);
						}
					}
				}
			}
		}
		// barandas en los bordes sin puente (los pisos altos tienen menos)
		float baranda = piso < 3 ? 1.0F : piso < 6 ? 0.6F : 0.25F;
		for (int s = -R; s <= R; s++) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				boolean abertura = salidas.contains(d) && Math.abs(s) <= 1;
				if (abertura || random.nextFloat() > baranda) {
					continue;
				}
				int x = cx + (d.getAxis() == Direction.Axis.X ? d.getStepX() * R : s);
				int z = cz + (d.getAxis() == Direction.Axis.Z ? d.getStepZ() * R : s);
				k.set(x, y + 1, z, t[2]);
			}
		}
		// columnas con faroles en las esquinas
		for (int[] e : new int[][] {{-R, -R}, {R, -R}, {-R, R}, {R, R}}) {
			int alto = 2 + random.nextInt(3);
			for (int h = 1; h <= alto; h++) {
				k.set(cx + e[0], y + h, cz + e[1], t[1]);
			}
			k.set(cx + e[0], y + alto + 1, cz + e[1], t[5]);
		}

		if (subida) {
			torre(k, piso, cx, cz, y, t, salidas, random);
			if (piso == 3) {
				k.nucleo(cx + 2, y + 1, cz - 3, NucleoDelJefeBlock.Jefe.CAPITAN);
			}
			return;
		}
		if (llave) {
			k.llave(cx, y + 1, cz, piso);
			k.spawner(cx - 2, y + 1, cz + 2, monstruo(piso, random));
			k.spawner(cx + 2, y + 1, cz - 2, monstruo(piso, random));
			return;
		}
		if (especial != null) {
			salaEspecial(k, piso, cx, cz, y, t, especial);
			return;
		}
		// sala comun: monstruos, spawners y a veces un cofre
		int spawners = piso < 2 ? random.nextInt(2) : 1 + random.nextInt(piso < 5 ? 2 : 3);
		if (llegadaDesdeAbajo) {
			spawners = Math.max(0, spawners - 1);
		}
		for (int n = 0; n < spawners; n++) {
			int dx = random.nextBoolean() ? -2 : 2;
			int dz = random.nextInt(5) - 2;
			k.spawner(cx + dx, y + 1, cz + dz, monstruo(piso, random));
		}
		int mobs = 1 + random.nextInt(2 + piso / 3);
		for (int n = 0; n < mobs; n++) {
			k.mob(monstruo(piso, random), cx + random.nextInt(5) - 2, y + 1, cz + random.nextInt(5) - 2);
		}
		if (random.nextFloat() < 0.3F + piso * 0.02F) {
			String botin = piso < 3 ? "ciudadela_1" : piso < 6 ? "ciudadela_2" : "ciudadela_3";
			k.cofre(cx + (random.nextBoolean() ? 3 : -3), y + 1, cz + (random.nextBoolean() ? 3 : -3), botin, Direction.NORTH);
		}
	}

	/** Los ejes x=0/z=0 quedan libres incluso con trampas activadas; entradas de tres bloques. */
	private static void salaEspecial(Constructor k, int piso, int x, int z, int y, Block[] t, TipoSala tipo) {
		switch (tipo) {
			case MERCADER -> {
				for (int dx = -R; dx <= R; dx++) {
					for (int dz = -R; dz <= R; dz++) k.set(x + dx, y + 5, z + dz, Blocks.SPRUCE_PLANKS);
				}
				for (int dx : new int[] {-3, 3}) for (int dz : new int[] {-3, 3}) {
					for (int h = 1; h <= 4; h++) k.set(x + dx, y + h, z + dz, Blocks.SPRUCE_LOG);
				}
				k.set(x + 2, y + 1, z + 3, Blocks.BARREL);
				k.set(x + 3, y + 1, z + 2, Blocks.BARREL);
				k.set(x + 2, y + 4, z + 2, Blocks.GLOWSTONE);
				k.mercader(x + 2, y + 1, z + 2);
			}
			case FLECHAS -> {
				for (int lado : piso < 3 ? new int[] {1} : new int[] {-1, 1}) {
					k.dispensador(x + 3, y + 1, z + lado * 2, Direction.WEST, 16 + piso * 8);
					k.set(x + 2, y + 1, z + lado * 2, Blocks.STONE_PRESSURE_PLATE);
					// Tope: las flechas no cruzan el pasillo central.
					k.set(x + 1, y + 1, z + lado * 2, t[1]);
				}
			}
			case FOSO -> {
				// Trampilla alimentada por el bloque adyacente bajo la placa. Fondo cerrado y escalones de salida.
				for (int dx = 1; dx <= 3; dx++) for (int dz = 1; dz <= 3; dz++) {
					k.set(x + dx, y - 3, z + dz, t[0]);
					for (int h = -2; h <= 0; h++) k.set(x + dx, y + h, z + dz, Blocks.AIR);
				}
				for (int h = -2; h <= 0; h++) {
					k.set(x + 3, y + h, z + 2, t[0]);
					for (int s = 0; s <= 4; s++) {
						k.set(x, y + h, z + s, t[0]);
						k.set(x + s, y + h, z, t[0]);
						k.set(x + s, y + h, z + 4, t[0]);
						k.set(x + 4, y + h, z + s, t[0]);
					}
				}
				k.set(x + 1, y - 2, z + 2, t[0]);
				k.set(x + 1, y - 1, z + 3, t[0]);
				k.set(x + 2, y, z + 3, t[0]);
				k.set(x + 2, y, z + 2, Blocks.OAK_TRAPDOOR.defaultBlockState()
					.setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.FACING, Direction.EAST));
				k.set(x + 3, y + 1, z + 2, Blocks.STONE_PRESSURE_PLATE);
			}
			case PISTONES -> {
				for (int lado : new int[] {-1, 1}) {
					k.set(x + 3, y + 1, z + lado * 2, Blocks.STICKY_PISTON.defaultBlockState()
						.setValue(PistonBaseBlock.FACING, Direction.WEST));
					k.set(x + 3, y + 1, z + lado * 3, Blocks.STONE_PRESSURE_PLATE);
					k.set(x + 2, y, z + lado * 2, Blocks.SLIME_BLOCK);
					k.set(x - 2, y + 1, z + lado * 2, Blocks.COBWEB);
				}
			}
			case BIBLIOTECA -> {
				for (int dx : new int[] {-3, -2, 2, 3}) {
					for (int h = 1; h <= 3; h++) k.set(x + dx, y + h, z - 3, Blocks.BOOKSHELF);
					k.set(x + dx, y + 4, z - 3, Blocks.SPRUCE_SLAB);
				}
				k.set(x - 2, y + 1, z + 2, Blocks.LECTERN);
				k.cofre(x + 2, y + 1, z + 2, piso < 3 ? "ciudadela_1" : piso < 6 ? "ciudadela_2" : "ciudadela_3", Direction.NORTH);
			}
			case JARDIN -> {
				for (int dx : new int[] {-3, 3}) for (int dz : new int[] {-3, 3}) {
					k.set(x + dx, y, z + dz, Blocks.MOSS_BLOCK);
					k.set(x + dx, y + 1, z + dz, Blocks.FLOWERING_AZALEA);
					k.set(x + dx, y - 1, z + dz, Blocks.MOSSY_STONE_BRICKS);
					k.set(x + dx, y - 2, z + dz, Blocks.IRON_CHAIN);
				}
				k.set(x - 2, y + 1, z + 2, Blocks.SEA_LANTERN);
				k.set(x + 2, y + 1, z - 2, Blocks.SEA_LANTERN);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static EntityType<? extends Mob> monstruo(int piso, Random random) {
		List<EntityType<? extends Mob>> opciones = new ArrayList<>(List.of(CieloEntities.PIRATA_DEL_CIELO));
		if (piso >= 1) {
			opciones.add(CieloEntities.ARCABUCERO_DEL_CIELO);
		}
		if (piso >= 3) {
			opciones.add(CieloEntities.MECANICO_DEL_DIRIGIBLE);
			opciones.add(CieloEntities.GUARDIAN_CELESTE);
		}
		if (piso >= 5) {
			opciones.add(CieloEntities.BRUJO_DEL_VIENTO);
			opciones.add(CieloEntities.GUARDIAN_CELESTE);
		}
		if (piso >= 6) {
			opciones.add(CieloEntities.BRUJO_DEL_VIENTO);
		}
		return opciones.get(random.nextInt(opciones.size()));
	}

	/** Torre con escalera de caracol hasta el piso de arriba, cerrada por el Sello Celestial. */
	private static void torre(Constructor k, int piso, int cx, int cz, int y, Block[] t, Set<Direction> salidas, Random random) {
		// puerta hacia alguno de los puentes (asi se ve desde donde llegas)
		Direction puerta = salidas.isEmpty() ? Direction.NORTH : salidas.iterator().next();
		for (int h = 1; h < ALTO_PISO; h++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean muro = Math.abs(dx) == 2 || Math.abs(dz) == 2;
					if (muro) {
						boolean hueco = h <= 2 && dx == puerta.getStepX() * 2 && dz == puerta.getStepZ() * 2;
						if (hueco) {
							k.set(cx + dx, y + h, cz + dz, CieloBlocks.SELLO_CELESTIAL.defaultBlockState());
						} else {
							k.set(cx + dx, y + h, cz + dz, h % 4 == 0 ? t[1] : t[0]);
						}
					} else if (dx == 0 && dz == 0) {
						k.set(cx, y + h, cz, t[1]);
					} else {
						k.set(cx + dx, y + h, cz + dz, Blocks.AIR);
					}
				}
			}
			if (h % 5 == 0) {
				k.set(cx, y + h, cz, t[5]);
			}
		}
	}

	/** Caracol continuo y desembarco: se dibuja despues de construir incluso el piso superior. */
	private static void escalera(Constructor k, int cx, int cz, int y, Block[] t, Direction puerta) {
		int[][] anillo = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
		int entrada = puerta == Direction.NORTH ? 1 : puerta == Direction.EAST ? 3 : puerta == Direction.SOUTH ? 5 : 7;
		for (int[] a : anillo) {
			k.set(cx + a[0], y, cz + a[1], t[0]);
			for (int h = 1; h <= ALTO_PISO + 2; h++) {
				k.set(cx + a[0], y + h, cz + a[1], Blocks.AIR);
			}
		}
		// La casilla interior de la puerta queda a nivel del suelo; el primer peldano va a su lado.
		for (int h = 1; h <= ALTO_PISO; h++) {
			int[] a = anillo[(entrada + h) % anillo.length];
			k.set(cx + a[0], y + h, cz + a[1], t[0]);
		}
		int[] fin = anillo[(entrada + ALTO_PISO) % anillo.length];
		int x = cx + (fin[0] == 0 ? 0 : fin[0] * 2);
		int z = cz + (fin[0] == 0 ? fin[1] * 2 : fin[1]);
		k.set(x, y + ALTO_PISO, z, t[0]);
		k.set(x, y + ALTO_PISO + 1, z, Blocks.AIR);
		k.set(x, y + ALTO_PISO + 2, z, Blocks.AIR);
	}

	/** Azotea: gran plaza sobre todo, arena del Condor y el tesoro final. */
	private static void azotea(Plan plan, Random random) {
		int y = PISOS * ALTO_PISO;
		Celda llegada = plan.llegada[PISOS];
		int radio = 32;
		plan.elementos.add(new Elemento(-radio, y - 3, -radio, radio, y + 10, radio, k -> {
			for (int dx = -radio; dx <= radio; dx++) {
				for (int dz = -radio; dz <= radio; dz++) {
					// plaza cuadrada con esquinas recortadas
					double d = Math.max(Math.abs(dx), Math.abs(dz));
					if (Math.abs(dx) + Math.abs(dz) > radio * 1.7) {
						continue;
					}
					boolean enHueco = Math.abs(dx - llegada.x()) <= 1 && Math.abs(dz - llegada.z()) <= 1 && (dx != llegada.x() || dz != llegada.z());
					if (!enHueco) {
						k.set(dx, y, dz, d > radio - 1.5 ? Blocks.GOLD_BLOCK : (dx + dz) % 6 == 0 ? Blocks.CHISELED_QUARTZ_BLOCK : Blocks.SMOOTH_QUARTZ);
						if (d < radio - 4 && (dx * 7 + dz * 13) % 5 != 0) {
							k.set(dx, y - 1, dz, Blocks.QUARTZ_BLOCK);
						}
					}
					if (d > radio - 1.5 && Math.floorMod(dx * 3 + dz, 5) != 0) {
						k.set(dx, y + 1, dz, Blocks.QUARTZ_PILLAR);
					}
				}
			}
			// nido del condor y tesoro
			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI * 2 / 16;
				k.set((int) Math.round(Math.cos(a) * 4), y + 1, (int) Math.round(Math.sin(a) * 4), i % 2 == 0 ? Blocks.HAY_BLOCK : Blocks.GOLD_BLOCK);
			}
			k.nucleo(0, y + 1, 0, NucleoDelJefeBlock.Jefe.CONDOR);
			k.cofre(0, y + 1, 6, "ciudadela_tesoro", Direction.NORTH);
			k.cofre(1, y + 1, 6, "ciudadela_tesoro", Direction.NORTH);
			for (int[] p : new int[][] {{14, 14}, {-14, 14}, {14, -14}, {-14, -14}}) {
				for (int h = 1; h <= 6; h++) {
					k.set(p[0], y + h, p[1], Blocks.QUARTZ_PILLAR);
				}
				k.set(p[0], y + 7, p[1], Blocks.SEA_LANTERN);
				k.mob(CieloEntities.ARCABUCERO_DEL_CIELO, p[0] + 1, y + 1, p[1]);
			}
		}));
	}

	// ================================================================== colocar en el mundo

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
		BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		Constructor k = new Constructor(level, chunkBB);
		for (Elemento e : this.plan().elementos) {
			int minX = this.origen.getX() + e.x1();
			int maxX = this.origen.getX() + e.x2();
			int minZ = this.origen.getZ() + e.z1();
			int maxZ = this.origen.getZ() + e.z2();
			if (maxX < chunkBB.minX() || minX > chunkBB.maxX() || maxZ < chunkBB.minZ() || minZ > chunkBB.maxZ()) {
				continue;
			}
			e.dibujar().accept(k);
		}
	}

	/** Coloca bloques en coordenadas locales (relativas al centro del piso 1), solo dentro del chunk actual. */
	final class Constructor {
		final WorldGenLevel level;
		final BoundingBox chunk;

		Constructor(WorldGenLevel level, BoundingBox chunk) {
			this.level = level;
			this.chunk = chunk;
		}

		BlockPos mundo(int x, int y, int z) {
			return CiudadelaPiece.this.origen.offset(x, y, z);
		}

		void set(int x, int y, int z, BlockState state) {
			BlockPos p = this.mundo(x, y, z);
			if (this.chunk.isInside(p)) {
				this.level.setBlock(p, state, Block.UPDATE_CLIENTS);
			}
		}

		void set(int x, int y, int z, Block block) {
			this.set(x, y, z, block.defaultBlockState());
		}

		void cofre(int x, int y, int z, String botin, Direction mira) {
			BlockPos p = this.mundo(x, y, z);
			if (this.chunk.isInside(p)) {
				this.set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, mira));
				if (this.level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
					chest.setLootTable(CieloPiece.botin(botin), CiudadelaPiece.this.semilla ^ p.asLong());
				}
			}
		}

		/** Cofre con la Llave Celestial del piso y algo de botin. */
		void llave(int x, int y, int z, int piso) {
			BlockPos p = this.mundo(x, y, z);
			if (this.chunk.isInside(p)) {
				this.set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
				if (this.level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
					chest.setItem(13, new ItemStack(CieloItems.LLAVE_CELESTIAL));
					chest.setItem(4, new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 1 + piso / 3));
					chest.setItem(22, new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 4 + piso * 2));
				}
				for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
					this.set(x + d[0], y, z + d[1], Blocks.GOLD_BLOCK);
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

		void dispensador(int x, int y, int z, Direction direccion, int flechas) {
			BlockPos p = this.mundo(x, y, z);
			if (!this.chunk.isInside(p)) return;
			this.set(x, y, z, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, direccion));
			if (this.level.getBlockEntity(p) instanceof DispenserBlockEntity dispenser) {
				dispenser.setItem(0, new ItemStack(net.minecraft.world.item.Items.ARROW, Math.min(64, flechas)));
			}
		}

		void mercader(int x, int y, int z) {
			BlockPos p = this.mundo(x, y, z);
			if (!this.chunk.isInside(p)) return;
			var mercader = MercaderDeLasNubes.crear(this.level.getLevel(), p);
			if (mercader != null) this.level.addFreshEntityWithPassengers(mercader);
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

		void cuerda(int x, int y, int z) {
			// Desembarco libre sobre la cuerda y sobre la primera baldosa interior.
			for (int dx = -1; dx <= 0; dx++) {
				this.set(x + dx, y + 1, z, Blocks.AIR);
				this.set(x + dx, y + 2, z, Blocks.AIR);
			}
			BlockPos top = this.mundo(x, y, z);
			if (top.getX() < this.chunk.minX() || top.getX() > this.chunk.maxX() || top.getZ() < this.chunk.minZ() || top.getZ() > this.chunk.maxZ()) {
				return;
			}
			// baja hasta tocar el suelo (no se usa el mapa de alturas: la azotea de la ciudadela esta encima)
			this.level.setBlock(top, CieloBlocks.ESCALERA_DE_CUERDA.defaultBlockState(), Block.UPDATE_CLIENTS);
			for (int wy = top.getY() - 1; wy > top.getY() - 260 && wy > this.level.getMinY(); wy--) {
				BlockPos p = new BlockPos(top.getX(), wy, top.getZ());
				if (!this.level.getBlockState(p).isAir()) {
					break;
				}
				this.level.setBlock(p, CieloBlocks.ESCALERA_DE_CUERDA.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** Para pruebas y para la brujula: celdas importantes. */
	public List<SalaEspecial> salasEspeciales() {
		return this.plan().salas.stream().map(s -> new SalaEspecial(s.piso(),
			this.origen.offset(s.celda().x(), s.piso() * ALTO_PISO, s.celda().z()), s.tipo())).toList();
	}

	public BlockPos subida(int piso) {
		Celda c = this.plan().subida[piso];
		return this.origen.offset(c.x(), piso * ALTO_PISO, c.z());
	}

	public BlockPos llave(int piso) {
		Celda c = this.plan().llave[piso];
		return this.origen.offset(c.x(), piso * ALTO_PISO, c.z());
	}

	public BlockPos entrada() {
		Celda c = this.plan().llegada[0];
		return this.origen.offset(c.x(), 0, c.z());
	}
}
