package com.aventura.cielo.test;

import com.aventura.cielo.item.BrujulaDelCieloItem;
import com.aventura.cielo.world.CiudadelaPiece;
import com.aventura.cielo.world.ZonaCiudadela;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Mundo normal (no plano): busca una Ciudadela de las Nubes generada de verdad, la fotografia y comprueba
 * que dentro de su zona no se pueden poner bloques.
 */
public class CiudadelaClientTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			BlockPos ciudadela = world.getServer().computeOnServer(server -> server.overworld()
				.findNearestMapStructure(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, com.aventura.cielo.CieloMod.id("solo_ciudadela")), BlockPos.ZERO, 120, false));
			if (ciudadela == null) {
				throw new AssertionError("No se encontró ninguna Ciudadela de las Nubes");
			}
			// vista de lejos (en espectador, volando)
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("tp @a " + (ciudadela.getX() - 75) + " 230 " + (ciudadela.getZ() - 75) + " -45 25");
			world.getConnection().waitForChunksRender();
			context.waitTicks(80);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("ciudadela_lejos");
			world.getServer().runCommand("tp @a " + (ciudadela.getX() - 20) + " 160 " + (ciudadela.getZ() - 45) + " -20 10");
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("ciudadela_puentes");

			// Ciudadela de prueba en un sitio conocido: la llegada de la cuerda y el caracol entre pisos
			BlockPos origen = new BlockPos(ciudadela.getX() + 400, 150, ciudadela.getZ());
			java.util.List<CiudadelaPiece.TipoSala> salas = new java.util.ArrayList<>();
			BlockPos[] vistas = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				int r = CiudadelaPiece.EXTENSION;
				for (int x = origen.getX() - r >> 4; x <= (origen.getX() + r >> 4); x++) {
					for (int z = origen.getZ() - r >> 4; z <= (origen.getZ() + r >> 4); z++) {
						level.setChunkForced(x, z, true);
						level.getChunk(x, z);
					}
				}
				for (BlockPos p : BlockPos.betweenClosed(origen.offset(-r, -21, -r), origen.offset(r, 124, r))) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				}
				CiudadelaPiece pieza = new CiudadelaPiece(origen, 1L);
				for (int x = origen.getX() - r >> 4; x <= (origen.getX() + r >> 4); x++) {
					for (int z = origen.getZ() - r >> 4; z <= (origen.getZ() + r >> 4); z++) {
						pieza.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
							new BoundingBox(x * 16, level.getMinY(), z * 16, x * 16 + 15, level.getMaxY(), z * 16 + 15),
							new ChunkPos(x, z), origen);
					}
				}
				java.util.List<BlockPos> v = new java.util.ArrayList<>(java.util.List.of(pieza.entrada().offset(CiudadelaPiece.R, 1, 0), pieza.subida(0)));
				java.util.Set<CiudadelaPiece.TipoSala> vistos = java.util.EnumSet.noneOf(CiudadelaPiece.TipoSala.class);
				for (var sala : pieza.salasEspeciales()) {
					if (vistos.add(sala.tipo())) {
						v.add(sala.centro());
						salas.add(sala.tipo());
					}
				}
				return v.toArray(BlockPos[]::new);
			});
			world.getServer().runCommand("gamemode spectator @a");
			BlockPos cuerda = vistas[0];
			BlockPos sub = vistas[1];
			// encima de la cuerda, mirando hacia el piso 1
			world.getServer().runCommand("tp @a " + (cuerda.getX() + 0.5) + " " + cuerda.getY() + " " + (cuerda.getZ() + 0.5) + " 90 15");
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("ciudadela_llegada_cuerda");
			// desde fuera: la cuerda y la entrada sin baranda
			world.getServer().runCommand("tp @a " + (cuerda.getX() + 7.5) + " " + (cuerda.getY() + 3) + " " + (cuerda.getZ() + 7.5) + " 135 20");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("ciudadela_entrada_fuera");
			// dentro de la torre, mirando el caracol hacia arriba
			world.getServer().runCommand("tp @a " + (sub.getX() + 1.5) + " " + (sub.getY() + 1) + " " + (sub.getZ() + 1.5) + " 135 -50");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("ciudadela_caracol");
			// una foto de cada tipo de sala especial
			for (int i = 0; i < salas.size(); i++) {
				BlockPos c = vistas[i + 2];
				// de lado (sin la columna de la esquina delante); bajo el techo para ver al mercader
				boolean techada = salas.get(i) == CiudadelaPiece.TipoSala.MERCADER;
				world.getServer().runCommand("tp @a " + (c.getX() - (techada ? 6.5 : 7.5)) + " " + (c.getY() + (techada ? 2 : 7)) + " "
					+ (c.getZ() + 0.5) + " -90 " + (techada ? 10 : 45));
				context.waitTicks(20);
				world.getConnection().waitForChunksRender();
				context.takeScreenshot("ciudadela_sala_" + salas.get(i).name().toLowerCase(java.util.Locale.ROOT));
			}

			// dentro de la zona: no se pueden poner bloques
			String resultado = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				// buscar un bloque solido de la ciudadela para apoyar el intento
				BlockPos apoyo = null;
				// la ciudadela flota: se busca de arriba abajo una baldosa suya con aire encima, no el terreno de y=0
				for (BlockPos p : BlockPos.betweenClosed(new BlockPos(ciudadela.getX() - 20, 300, ciudadela.getZ() - 20), new BlockPos(ciudadela.getX() + 20, 80, ciudadela.getZ() + 20))) {
					if (level.getBlockState(p).isSolid() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir()
						&& ZonaCiudadela.enZona(level, p.above())) {
						apoyo = p.immutable();
						break;
					}
				}
				if (apoyo == null) {
					return "sin apoyo";
				}
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(apoyo.getX() + 0.5, apoyo.getY() + 1, apoyo.getZ() + 0.5);
				if (!ZonaCiudadela.enZona(level, apoyo.above())) {
					return "la zona no se detecta en " + apoyo;
				}
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 16));
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(apoyo).add(0, 0.5, 0), Direction.UP, apoyo, false);
				player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
				if (!level.getBlockState(apoyo.above()).isAir()) {
					return "se pudo poner un bloque dentro de la ciudadela";
				}
				// y fuera de la zona (lejos) si se puede: la regla es solo de la ciudadela
				return ZonaCiudadela.enZona(level, ciudadela.offset(200, 0, 200)) ? "la zona es demasiado grande" : "ok";
			});
			if (!resultado.equals("ok")) {
				throw new AssertionError(resultado);
			}
			context.waitTicks(20);
			context.takeScreenshot("ciudadela_dentro");

            // Vitrinas aisladas en el Overworld: las paletas de Nether y End se fotografian aqui tambien.
            world.getServer().runCommand("gamemode spectator @a");
            for (var tema : com.aventura.cielo.world.MazmorraStructure.Tema.values()) {
                BlockPos base = new BlockPos(38000 + tema.ordinal() * 384, 170, 38000);
                world.getServer().runCommand("tp @a " + (base.getX() + 48) + " 224 " + (base.getZ() - 115) + " 0 20");
                world.getServer().runOnServer(server -> {
                    ExpansionGameTests.cargar(server.overworld(), base, true);
                    ExpansionGameTests.construir(server.overworld(), base, 404L, tema);
                });
                context.waitTicks(40);
                world.getConnection().waitForChunksRender();
                context.takeScreenshot("dungeon_" + tema.getSerializedName());
                world.getServer().runOnServer(server -> {
                    var level = server.overworld();
                    var type = switch (tema) {
                        case FORTIN -> com.aventura.cielo.registry.CieloEntities.JEAN_BECK;
                        case CENIZA -> com.aventura.cielo.registry.CieloEntities.SENOR_DE_LA_CENIZA;
                        case VACIO -> com.aventura.cielo.registry.CieloEntities.HERALDO_DEL_VACIO;
                    };
                    var boss = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    boss.snapTo(base.getX() + 96.5, base.getY() + 1, base.getZ() + 0.5);
                    boss.setNoAi(true);
                    level.addFreshEntity(boss);
                });
                world.getServer().runCommand("tp @a " + (base.getX() + 96.5) + " " + (base.getY() + 3) + " " + (base.getZ() + 10.5) + " 180 0");
                context.waitTicks(40);
                world.getConnection().waitForChunksRender();
                context.takeScreenshot("jefe_" + tema.getSerializedName());
                world.getServer().runOnServer(server -> ExpansionGameTests.cargar(server.overworld(), base, false));
            }


		}
	}
}
