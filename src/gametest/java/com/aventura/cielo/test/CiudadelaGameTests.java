package com.aventura.cielo.test;

import com.aventura.cielo.block.SelloCelestialBlock;
import com.aventura.cielo.entity.BrujoDelViento;
import com.aventura.cielo.entity.MercaderDeLasNubes;
import com.aventura.cielo.registry.CieloBlocks;
import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.registry.CieloItems;
import com.aventura.cielo.world.CiudadelaPiece;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class CiudadelaGameTests {
	@GameTest
	public void ciudadelaDistribuyeSalasEspeciales(GameTestHelper helper) {
		Set<CiudadelaPiece.TipoSala> tipos = new HashSet<>();
		for (long semilla : new long[] {0L, 1L, 2L, 42L, 987654321L, -1L, Long.MAX_VALUE}) {
			BlockPos origen = new BlockPos(12000, 120, 12000);
			CiudadelaPiece pieza = new CiudadelaPiece(origen, semilla);
			var salas = pieza.salasEspeciales();
			helper.assertTrue(salas.equals(new CiudadelaPiece(origen, semilla).salasEspeciales()), "Plan no determinista");
			var trasladadas = new CiudadelaPiece(origen.offset(160, 0, 0), semilla).salasEspeciales();
			for (int i = 0; i < salas.size(); i++) {
				helper.assertTrue(salas.get(i).tipo() == trasladadas.get(i).tipo()
					&& salas.get(i).centro().offset(160, 0, 0).equals(trasladadas.get(i).centro()), "Las salas dependen del origen");
			}
			int ultimoMercader = -2;
			int mercaderes = 0;
			for (int piso = 0; piso < CiudadelaPiece.PISOS; piso++) {
				int actual = piso;
				var delPiso = salas.stream().filter(s -> s.piso() == actual).toList();
				helper.assertTrue(delPiso.size() >= 1 && delPiso.size() <= 3, "Debe haber 1-3 salas por piso");
				Set<BlockPos> centros = new HashSet<>();
				for (var sala : delPiso) {
					tipos.add(sala.tipo());
					BlockPos llegada = piso == 0 ? pieza.entrada() : pieza.subida(piso - 1).above(CiudadelaPiece.ALTO_PISO);
					helper.assertTrue(centros.add(sala.centro()) && !sala.centro().equals(llegada)
						&& !sala.centro().equals(pieza.subida(piso)) && !sala.centro().equals(pieza.llave(piso)), "Sala sobre celda reservada o repetida");
					if (sala.tipo() == CiudadelaPiece.TipoSala.MERCADER) {
						helper.assertTrue(piso - ultimoMercader >= 2, "Mercaderes demasiado juntos");
						ultimoMercader = piso;
						mercaderes++;
					}
				}
			}
			helper.assertTrue(mercaderes > 0, "Falta un mercader en la ciudadela");
		}
		helper.assertTrue(tipos.size() == CiudadelaPiece.TipoSala.values().length, "Las semillas no cubren todas las variantes");
		helper.succeed();
	}

	/** Generacion recortada por chunks; sin depender de ticks, amaneceres ni otras plantillas. */
	@GameTest(maxTicks = 1200)
	public void ciudadelaGeneraMercaderConOfertas(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int caso = 0; caso < 3; caso++) {
			cargar(level, new BlockPos(14000 + caso * 160, 120, 14000), true);
		}
		// La carga forzada se aplica en un tick posterior: antes, las entidades nuevas de esos chunks no se ven.
		helper.runAfterDelay(20, () -> {
		for (int caso = 0; caso < 3; caso++) {
			BlockPos origen = new BlockPos(14000 + caso * 160, 120, 14000);
			try {
				for (BlockPos p : BlockPos.betweenClosed(origen.offset(-34, -4, -34), origen.offset(34, 124, 34))) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				}
				CiudadelaPiece pieza = new CiudadelaPiece(origen, caso);
				for (int x = origen.getX() + 34 >> 4; x >= (origen.getX() - 34 >> 4); x--) {
					for (int z = origen.getZ() + 34 >> 4; z >= (origen.getZ() - 34 >> 4); z--) {
						pieza.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
							new BoundingBox(x * 16, level.getMinY(), z * 16, x * 16 + 15, level.getMaxY(), z * 16 + 15), new ChunkPos(x, z), origen);
					}
				}
				int esperados = 0;
				for (var sala : pieza.salasEspeciales()) {
					BlockPos centro = sala.centro();
					if (sala.tipo() == CiudadelaPiece.TipoSala.FOSO) {
						BlockPos trampilla = centro.offset(2, 0, 2);
						helper.assertTrue(level.getBlockState(trampilla).is(Blocks.OAK_TRAPDOOR), "Falta la trampilla del foso");
						// Apertura directa: el reloj congelado no debe decidir el resultado de la prueba.
						level.setBlock(trampilla, level.getBlockState(trampilla)
							.setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true), 2);
						BlockPos fondo = centro.offset(2, -2, 2);
						helper.assertTrue(apoyado(level, fondo), "El foso debe tener fondo seguro");
						helper.assertTrue(caminar3D(level, fondo, origen).contains(centro.above()), "No se puede salir del foso caminando");
					}
					if (sala.tipo() == CiudadelaPiece.TipoSala.FLECHAS) {
						helper.assertTrue(level.getBlockEntity(centro.offset(3, 1, 2))
							instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity d
							&& d.countItem(net.minecraft.world.item.Items.ARROW) > 0, "Trampa sin flechas");
					}
					// Paso central hasta el umbral interior de cada puente, con dos bloques de altura.
					for (Direction d : Direction.Plane.HORIZONTAL) for (int paso = 0; paso < CiudadelaPiece.R; paso++) {
						BlockPos pies = centro.relative(d, paso).above();
						helper.assertTrue(cuerpoLibre(level, pies) && apoyado(level, pies), "Sala bloquea el paso: " + sala);
					}
					if (sala.tipo() != CiudadelaPiece.TipoSala.MERCADER) continue;
					esperados++;
					var aldeanos = level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,
						new AABB(centro.offset(2, 1, 2)).inflate(0.1));
					helper.assertTrue(aldeanos.size() == 1, "Debe generarse exactamente un mercader por sala");
					var mercader = aldeanos.getFirst();
					helper.assertTrue(mercader.isPersistenceRequired() && mercader.isPermanentlyInvulnerable() && mercader.isNoAi(),
						"El mercader debe ser persistente, invulnerable e inmovil");
					var ofertas = mercader.getOffers();
					helper.assertTrue(ofertas.size() == 5, "Faltan ofertas del mercader");
					var productos = Set.of(net.minecraft.world.item.Items.BREAD, net.minecraft.world.item.Items.ARROW,
						net.minecraft.world.item.Items.POTION, CieloItems.BRUJULA_DEL_CIELO, CieloItems.ESCALERA_DE_CUERDA);
					Set<net.minecraft.world.item.Item> encontrados = new HashSet<>();
					for (var oferta : ofertas) {
						encontrados.add(oferta.getResult().getItem());
						helper.assertTrue(oferta.getCostA().is(MercaderDeLasNubes.moneda()) && !oferta.isOutOfStock(), "Moneda u oferta incorrecta");
						if (oferta.getResult().is(net.minecraft.world.item.Items.POTION)) {
							var pocion = oferta.getResult().get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
							helper.assertTrue(pocion != null && pocion.is(net.minecraft.world.item.alchemy.Potions.HEALING), "La pocion debe curar");
						}
					}
					helper.assertTrue(encontrados.equals(productos), "Productos incorrectos");
				}
				helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,
					new AABB(origen).inflate(60, 160, 60)).size() == esperados, "Mercaderes duplicados entre chunks");
			} finally {
				level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(origen).inflate(60, 160, 60))
					.forEach(net.minecraft.world.entity.Entity::discard);
				cargar(level, origen, false);
			}
		}
		helper.succeed();
		});
	}

	/** Se puede pararse en (x, y, z): bloque solido debajo y dos de aire (o cuerda) arriba. */
	private static boolean pisable(ServerLevel level, int x, int y, int z) {
		var suelo = level.getBlockState(new BlockPos(x, y, z));
		var pies = level.getBlockState(new BlockPos(x, y + 1, z));
		var cabeza = level.getBlockState(new BlockPos(x, y + 2, z));
		return suelo.isSolid() && !pies.isSolid() && !cabeza.isSolid();
	}

	/**
	 * Recorre caminando el piso (a la altura y), permitiendo subir/bajar 1 bloque y saltar huecos de 1.
	 * Devuelve las posiciones (x, z) alcanzadas.
	 */
	private static Set<Long> caminar(ServerLevel level, BlockPos inicio, int y) {
		Set<Long> vistos = new HashSet<>();
		Deque<int[]> cola = new ArrayDeque<>();
		cola.add(new int[] {inicio.getX(), y, inicio.getZ()});
		while (!cola.isEmpty() && vistos.size() < 20000) {
			int[] p = cola.poll();
			long clave = BlockPos.asLong(p[0], p[1], p[2]);
			if (!vistos.add(clave)) {
				continue;
			}
			for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				for (int salto = 1; salto <= 2; salto++) {
					int nx = p[0] + d[0] * salto;
					int nz = p[2] + d[1] * salto;
					boolean encontrado = false;
					for (int dy = -1; dy <= 1; dy++) {
						int ny = p[1] + dy;
						if (Math.abs(ny - y) <= 3 && pisable(level, nx, ny, nz)) {
							cola.add(new int[] {nx, ny, nz});
							encontrado = true;
						}
					}
					if (encontrado) {
						break; // si se puede ir al lado, no hace falta saltar
					}
				}
			}
		}
		return vistos;
	}

	private static boolean llega(Set<Long> camino, BlockPos destino, int radio) {
		for (long c : camino) {
			BlockPos p = BlockPos.of(c);
			if (Math.abs(p.getX() - destino.getX()) <= radio && Math.abs(p.getZ() - destino.getZ()) <= radio && Math.abs(p.getY() - destino.getY()) <= 2) {
				return true;
			}
		}
		return false;
	}

	/** Construye una ciudadela entera y comprueba que cada piso se puede recorrer: llegada -> llave y llegada -> escalera. */
	@GameTest(maxTicks = 400)
	public void ciudadelaSeRecorre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Fija y lejos de las demas: una posicion relativa a la plantilla podia caer sobre otra prueba y borrar sus entidades.
		BlockPos origen = new BlockPos(10000, 120, 10000);
		cargar(level, origen, true);
		CiudadelaPiece pieza = new CiudadelaPiece(origen, 987654321L);
		pieza.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), BoundingBox.infinite(),
			ChunkPos.containing(origen), origen);

		for (int piso = 0; piso < CiudadelaPiece.PISOS; piso++) {
			int y = origen.getY() + piso * CiudadelaPiece.ALTO_PISO;
			BlockPos llegada = piso == 0 ? pieza.entrada() : pieza.subida(piso - 1).atY(y);
			// en los pisos de arriba se llega por el caracol: empezar al borde del hueco
			BlockPos inicio = piso == 0 ? llegada : llegada.offset(2, 0, 2);
			Set<Long> camino = caminar(level, inicio, y);
			helper.assertTrue(camino.size() > 50, "Piso " + (piso + 1) + ": casi no se puede caminar (" + camino.size() + ")");
			helper.assertTrue(llega(camino, pieza.llave(piso), 2), "Piso " + (piso + 1) + ": no se llega a la llave");
			helper.assertTrue(llega(camino, pieza.subida(piso), 3), "Piso " + (piso + 1) + ": no se llega a la escalera");

			// la llave esta en su cofre
			BlockPos cofre = pieza.llave(piso).above();
			helper.assertTrue(level.getBlockEntity(cofre) instanceof ChestBlockEntity c && c.countItem(CieloItems.LLAVE_CELESTIAL) == 1,
				"Piso " + (piso + 1) + ": falta la Llave Celestial");
			// y la escalera esta sellada
			int sellos = 0;
			BlockPos sub = pieza.subida(piso);
			for (BlockPos p : BlockPos.betweenClosed(sub.offset(-2, 1, -2), sub.offset(2, 2, 2))) {
				if (level.getBlockState(p).is(CieloBlocks.SELLO_CELESTIAL)) {
					sellos++;
				}
			}
			helper.assertTrue(sellos == 2, "Piso " + (piso + 1) + ": la escalera debe tener un sello de 2 bloques (" + sellos + ")");
		}
		// azotea con el nucleo del Condor
		BlockPos azotea = origen.above(CiudadelaPiece.PISOS * CiudadelaPiece.ALTO_PISO + 1);
		helper.assertTrue(level.getBlockState(azotea).is(CieloBlocks.NUCLEO_DEL_JEFE), "Falta el núcleo del Cóndor en la azotea");
		// la entrada tiene cuerda hasta abajo
		helper.assertTrue(level.getBlockState(pieza.entrada().offset(CiudadelaPiece.R, -5, 0)).is(CieloBlocks.ESCALERA_DE_CUERDA),
			"La entrada debe tener escalera de cuerda");
		// quitar a los monstruos de la ciudadela para no molestar a las otras pruebas
		level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(origen).inflate(60, 160, 60)).forEach(net.minecraft.world.entity.Entity::discard);
		cargar(level, origen, false);
		helper.succeed();
	}

	/** Prueba desde el pie de la cuerda, sin teletransportarse entre pisos ni atravesar sellos. */
	@GameTest(maxTicks = 1200)
	public void ciudadelaSeAsciendeEn3D(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long[] semillas = {0L, 1L, 2L, 987654321L};
		for (int caso = 0; caso < semillas.length; caso++) {
			// Areas propias, fuera de las plantillas y de la prueba horizontal. No depende del tiempo del mundo.
			BlockPos origen = new BlockPos(8000 + caso * 160, 120, 8000);
			cargar(level, origen, true);
			try {
				for (BlockPos p : BlockPos.betweenClosed(origen.offset(-34, -21, -34), origen.offset(34, 124, 34))) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				}
				CiudadelaPiece pieza = new CiudadelaPiece(origen, semillas[caso]);
				BlockPos pie = pieza.entrada().offset(CiudadelaPiece.R, -20, 0);
				level.setBlock(pie.below(), Blocks.STONE.defaultBlockState(), 2);
				// Reproduce el recorte por chunks de worldgen, en orden inverso en casos alternos.
				for (int n = 0; n < 36; n++) {
					int indice = caso % 2 == 0 ? n : 35 - n;
					int cx = (origen.getX() - 34 >> 4) + indice / 6;
					int cz = (origen.getZ() - 34 >> 4) + indice % 6;
					pieza.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
						new BoundingBox(cx * 16, level.getMinY(), cz * 16, cx * 16 + 15, level.getMaxY(), cz * 16 + 15),
						new ChunkPos(cx, cz), origen);
				}
				helper.assertTrue(level.getBlockState(pie).is(CieloBlocks.ESCALERA_DE_CUERDA), "Falta el pie de la cuerda");
				String contexto = "Semilla " + semillas[caso] + ": ";
				for (int piso = 0; piso < CiudadelaPiece.PISOS; piso++) {
					Set<BlockPos> camino = caminar3D(level, pie, origen);
					BlockPos llave = pieza.llave(piso).above();
					helper.assertTrue(adyacente(camino, llave), contexto + "no se alcanza la llave del piso " + (piso + 1));
					helper.assertTrue(level.getBlockEntity(llave) instanceof ChestBlockEntity c && c.countItem(CieloItems.LLAVE_CELESTIAL) == 1,
						contexto + "falta la llave del piso " + (piso + 1));
					BlockPos sub = pieza.subida(piso);
					BlockPos sello = null;
					for (BlockPos p : BlockPos.betweenClosed(sub.offset(-2, 1, -2), sub.offset(2, 1, 2))) {
						if (level.getBlockState(p).is(CieloBlocks.SELLO_CELESTIAL)) sello = p.immutable();
					}
					helper.assertTrue(sello != null && adyacente(camino, sello), contexto + "no se alcanza el sello del piso " + (piso + 1));
					SelloCelestialBlock.abrir(level, sello);
				}
				Set<BlockPos> camino = caminar3D(level, pie, origen);
				helper.assertTrue(adyacente(camino, origen.offset(0, 113, 0)), contexto + "no se alcanza la arena superior");
				helper.assertTrue(adyacente(camino, origen.offset(0, 113, 6)), contexto + "no se alcanza el tesoro");
			} finally {
				level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(origen).inflate(60, 160, 60))
					.forEach(net.minecraft.world.entity.Entity::discard);
				cargar(level, origen, false);
			}
		}
		helper.succeed();
	}

	private static void cargar(ServerLevel level, BlockPos origen, boolean cargar) {
		for (int x = origen.getX() - 34 >> 4; x <= (origen.getX() + 34 >> 4); x++) {
			for (int z = origen.getZ() - 34 >> 4; z <= (origen.getZ() + 34 >> 4); z++) {
				level.setChunkForced(x, z, cargar);
				if (cargar) level.getChunk(x, z);
			}
		}
	}

	private static boolean adyacente(Set<BlockPos> camino, BlockPos bloque) {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (camino.contains(bloque.relative(d)) || camino.contains(bloque.relative(d).above())) return true;
		}
		return false;
	}

	private static boolean libre(ServerLevel level, BlockPos p) {
		var state = level.getBlockState(p);
		return state.is(BlockTags.CLIMBABLE) || state.getCollisionShape(level, p).isEmpty();
	}

	private static boolean cuerpoLibre(ServerLevel level, BlockPos pies) {
		return libre(level, pies) && libre(level, pies.above());
	}

	private static boolean trepable(ServerLevel level, BlockPos p) {
		return level.getBlockState(p).is(BlockTags.CLIMBABLE);
	}

	private static boolean apoyado(ServerLevel level, BlockPos p) {
		var suelo = level.getBlockState(p.below());
		return suelo.isFaceSturdy(level, p.below(), Direction.UP)
			&& suelo.getCollisionShape(level, p.below()).max(Direction.Axis.Y) <= 1;
	}

	/** Nodos = pies de un jugador 1x2. Comprueba tambien el volumen barrido al subir/saltar. */
	private static Set<BlockPos> caminar3D(ServerLevel level, BlockPos inicio, BlockPos origen) {
		Set<BlockPos> vistos = new HashSet<>();
		Deque<BlockPos> cola = new ArrayDeque<>();
		cola.add(inicio);
		while (!cola.isEmpty()) {
			BlockPos p = cola.removeFirst();
			if (Math.abs(p.getX() - origen.getX()) > 34 || Math.abs(p.getZ() - origen.getZ()) > 34
				|| p.getY() < inicio.getY() || p.getY() > origen.getY() + 122 || !cuerpoLibre(level, p) || !vistos.add(p)) continue;
			boolean escalando = trepable(level, p);
			if (!apoyado(level, p) && !escalando) {
				cola.add(p.below()); // gravedad: no permite caminar por el aire
				continue;
			}
			if (escalando) {
				cola.add(p.above());
				cola.add(p.below());
			}
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos vecino = p.relative(d);
				if (cuerpoLibre(level, vecino)) cola.add(vecino);
				if (libre(level, p.above(2)) && cuerpoLibre(level, vecino.above()) && apoyado(level, vecino.above())) {
					cola.add(vecino.above());
				}
				// Los puentes ROTOS requieren saltar un hueco de un bloque, ya previsto por la prueba horizontal.
				BlockPos otro = vecino.relative(d);
				if (!apoyado(level, vecino) && cuerpoLibre(level, vecino) && cuerpoLibre(level, p.above())
					&& cuerpoLibre(level, vecino.above()) && cuerpoLibre(level, otro.above())
					&& cuerpoLibre(level, otro) && apoyado(level, otro)) cola.add(otro);
			}
		}
		return vistos;
	}

	/** El sello se abre con la llave (y desaparece entero). */
	@GameTest
	public void selloSeAbreConLlave(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), CieloBlocks.SELLO_CELESTIAL);
		helper.setBlock(new BlockPos(1, 2, 1), CieloBlocks.SELLO_CELESTIAL);
		SelloCelestialBlock.abrir(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 1));
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 2, 1));
		helper.succeed();
	}

	/** El Brujo del Viento dispara cargas de viento a su objetivo. */
	@GameTest(maxTicks = 200)
	public void brujoDisparaViento(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var objetivo = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
		BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
		BrujoDelViento brujo = CieloEntities.BRUJO_DEL_VIENTO.create(level, EntitySpawnReason.COMMAND);
		brujo.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
		level.addFreshEntity(brujo);
		var zombi = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		zombi.snapTo(base.getX() + 6.5, base.getY(), base.getZ() + 0.5);
		zombi.setNoAi(true);
		level.addFreshEntity(zombi);
		brujo.setTarget(zombi);
		helper.succeedWhen(() -> {
			brujo.setTarget(zombi);
			helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge.class,
				new AABB(base).inflate(12)).isEmpty() || zombi.position().distanceTo(brujo.position()) > 7.5, "Debe disparar viento");
		});
		if (objetivo != null) {
			objetivo.discard();
		}
	}
}
