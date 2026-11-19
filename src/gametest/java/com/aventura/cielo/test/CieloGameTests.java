package com.aventura.cielo.test;

import com.aventura.cielo.block.NucleoDelJefeBlock;
import com.aventura.cielo.entity.CapitanBarbanube;
import com.aventura.cielo.entity.CondorDeAcero;
import com.aventura.cielo.entity.PirataDelCielo;
import com.aventura.cielo.registry.CieloBlocks;
import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.world.CieloPiece;
import com.aventura.cielo.world.CieloStructure;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class CieloGameTests {
	/** Jugador simulado en SUPERVIVENCIA (el de GameTestHelper siempre es creativo y los jefes lo ignoran). */
	static ServerPlayer jugadorSupervivencia(GameTestHelper helper) { return jugadorSupervivencia(helper,true); }
    static ServerPlayer jugadorSupervivencia(GameTestHelper helper,boolean protegido) {
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
			new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "retador"), false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        player.getAbilities().invulnerable=false;
        if (!protegido) return player;
        // Solo pruebas de estructura/fases; NO usar para probar perdida de salud.
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 20 * 60, 4));
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 20 * 60, 4));
		return player;
	}

	@GameTest
	public void distanciasDeAparicion(GameTestHelper helper) {
		helper.assertTrue(!CieloStructure.permitido(CieloStructure.Tipo.GLOBO, 150, 0.0F), "Nada a menos de 300 bloques del spawn");
		helper.assertTrue(CieloStructure.permitido(CieloStructure.Tipo.GLOBO, 400, 0.9F), "Globos entre 300 y 500");
		helper.assertTrue(!CieloStructure.permitido(CieloStructure.Tipo.DIRIGIBLE, 400, 0.0F), "Sin jefes cerca del spawn");
		helper.assertTrue(CieloStructure.permitido(CieloStructure.Tipo.DIRIGIBLE, 2000, 0.9F), "Dirigibles lejos");
		helper.assertTrue(!CieloStructure.permitido(CieloStructure.Tipo.FORTALEZA, 1500, 0.0F), "La fortaleza final esta muy lejos");
		helper.assertTrue(CieloStructure.permitido(CieloStructure.Tipo.FORTALEZA, 3500, 0.9F), "Fortaleza a mas de 3000");
		helper.succeed();
	}

	private static void construir(GameTestHelper helper, CieloStructure.Tipo tipo, BlockPos origen) {
		ServerLevel level = helper.getLevel();
		CieloPiece piece = new CieloPiece(tipo, origen, false, 12345L);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
			BoundingBox.infinite(), ChunkPos.containing(origen), origen);
	}

	/** Construye las 4 naves en el aire y revisa que tengan cofres, tripulacion y (las grandes) nucleo de jefe. */
	@GameTest(maxTicks = 40)
	public void navesSeConstruyen(GameTestHelper helper) {
		BlockPos base = helper.absolutePos(new BlockPos(0, 0, 0)).above(70);
		CieloStructure.Tipo[] tipos = CieloStructure.Tipo.values();
		// cargar los chunks de las 4 naves (si no, los tripulantes quedan "pendientes" y no se cuentan)
		for (int i = 0; i < tipos.length; i++) {
			BlockPos o = base.offset(i * 70, 0, 0);
			for (int cx = -3; cx <= 3; cx++) {
				for (int cz = -3; cz <= 3; cz++) {
					helper.getLevel().setChunkForced((o.getX() >> 4) + cx, (o.getZ() >> 4) + cz, true);
				}
			}
		}
		// la carga forzada se aplica en un tick posterior; en el tick 0 algunos tripulantes aun no se ven
		helper.runAfterDelay(10, () -> {
		for (int i = 0; i < tipos.length; i++) {
			BlockPos origen = base.offset(i * 70, 0, 0);
			construir(helper, tipos[i], origen);
			int cofres = 0;
			int nucleos = 0;
			for (BlockPos p : BlockPos.betweenClosed(origen.offset(-25, -25, -35), origen.offset(25, 20, 30))) {
				var s = helper.getLevel().getBlockState(p);
				if (s.is(Blocks.CHEST)) {
					cofres++;
				}
				if (s.is(CieloBlocks.NUCLEO_DEL_JEFE)) {
					nucleos++;
				}
			}
			int tripulacion = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
				new AABB(origen).inflate(30)).size();
			helper.assertTrue(cofres >= 1, tipos[i] + " sin cofres");
			helper.assertTrue(tripulacion >= 2, tipos[i] + " sin tripulacion (" + tripulacion + ")");
			boolean conJefe = tipos[i] == CieloStructure.Tipo.DIRIGIBLE || tipos[i] == CieloStructure.Tipo.FORTALEZA;
			helper.assertTrue(conJefe == (nucleos == 1), tipos[i] + " nucleos de jefe: " + nucleos);
		}
		helper.succeed();
		});
	}

	/** El nucleo invoca al jefe cuando llega un jugador en supervivencia, una sola vez. */
	@GameTest(maxTicks = 100)
	public void nucleoInvocaJefe(GameTestHelper helper) {
		BlockPos rel = new BlockPos(2, 1, 2);
		helper.setBlock(rel, CieloBlocks.NUCLEO_DEL_JEFE.defaultBlockState().setValue(NucleoDelJefeBlock.JEFE, NucleoDelJefeBlock.Jefe.CAPITAN));
		ServerPlayer player = jugadorSupervivencia(helper);
		BlockPos abs = helper.absolutePos(rel);
		player.teleportTo(abs.getX() + 3, abs.getY(), abs.getZ());
		helper.succeedWhen(() -> {
			List<CapitanBarbanube> jefes = helper.getLevel().getEntitiesOfClass(CapitanBarbanube.class, new AABB(abs).inflate(10));
			helper.assertTrue(jefes.size() == 1, "Debe aparecer exactamente un capitan, hay " + jefes.size());
			helper.assertTrue(!helper.getLevel().getBlockState(abs).getValue(NucleoDelJefeBlock.ACTIVO), "El nucleo debe apagarse");
		});
	}

	/** Los jefes pelean varios segundos sin errores y usan sus ataques especiales. */
	@GameTest(maxTicks = 600)
	public void jefesPelean(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = jugadorSupervivencia(helper);
		BlockPos centro = helper.absolutePos(new BlockPos(4, 1, 4));
		// el condor vuela en circulos de 13 bloques: cargar los chunks de alrededor para que no se "congele"
		for (int cx = -2; cx <= 2; cx++) {
			for (int cz = -2; cz <= 2; cz++) {
				level.setChunkForced((centro.getX() >> 4) + cx, (centro.getZ() >> 4) + cz, true);
			}
		}
		player.teleportTo(centro.getX() + 6, centro.getY(), centro.getZ());
		CapitanBarbanube capitan = CieloEntities.CAPITAN_BARBANUBE.create(level, EntitySpawnReason.COMMAND);
		capitan.snapTo(centro.getX(), centro.getY(), centro.getZ());
		capitan.setTarget(player);
		level.addFreshEntity(capitan);
		CondorDeAcero condor = CieloEntities.CONDOR_DE_ACERO.create(level, EntitySpawnReason.COMMAND);
		condor.snapTo(centro.getX(), centro.getY() + 8, centro.getZ());
		condor.setAncla(centro);
		condor.setTarget(player);
		level.addFreshEntity(condor);
		// bajar la vida del capitan para que entre en fase 2 (llama refuerzos)
		capitan.setHealth(capitan.getMaxHealth() * 0.5F);
		helper.succeedWhen(() -> {
			helper.assertTrue(capitan.isAlive() && condor.isAlive(), "Los jefes deben seguir vivos");
			int piratas = level.getEntitiesOfClass(PirataDelCielo.class, new AABB(centro).inflate(15)).size();
			helper.assertTrue(piratas >= 3, "En fase 2 el capitan llama refuerzos (piratas: " + piratas + ")");
			int ataques = condor.ataquesRealizados;
			helper.assertTrue(ataques > 0, "El condor debe usar algun ataque especial. target=" + condor.getTarget()
				+ " pos=" + condor.position() + " centro=" + centro
				+ " id=" + condor.getId() + " ticks=" + condor.tickCount + " capTicks=" + capitan.tickCount + " noAi=" + condor.isNoAi() + " vida=" + condor.getHealth());
		});
	}
}
