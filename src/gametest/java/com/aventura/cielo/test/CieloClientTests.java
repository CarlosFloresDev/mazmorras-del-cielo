package com.aventura.cielo.test;

import com.aventura.cielo.entity.CondorDeAcero;
import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.world.CieloPiece;
import com.aventura.cielo.world.CieloStructure;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Construye las naves en el cielo y pone a los mobs y jefes en fila para sacar capturas. */
public class CieloClientTests implements FabricClientGameTest {
	private int y;

	private void nave(ServerLevel level, CieloStructure.Tipo tipo, BlockPos origen) {
		new CieloPiece(tipo, origen, false, 777L).postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
			level.getRandom(), BoundingBox.infinite(), ChunkPos.containing(origen), origen);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("difficulty peaceful");
			this.y = world.getServer().computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));

			// --- mobs y jefes en fila (pacifico: no atacan, pero los jefes no se borran)
			world.getServer().runCommand("difficulty easy");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				EntityType<?>[] tipos = {CieloEntities.PIRATA_DEL_CIELO, CieloEntities.ARCABUCERO_DEL_CIELO, CieloEntities.MECANICO_DEL_DIRIGIBLE,
					CieloEntities.CAPITAN_BARBANUBE};
				for (int i = 0; i < tipos.length; i++) {
					Mob mob = (Mob) tipos[i].create(level, EntitySpawnReason.COMMAND);
					mob.snapTo(-4.5 + i * 3, this.y, 5.5);
					mob.setNoAi(true);
					mob.setYRot(180);
					mob.setYHeadRot(180);
					mob.yBodyRot = 180;
					mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);
					level.addFreshEntity(mob);
				}
				CondorDeAcero condor = CieloEntities.CONDOR_DE_ACERO.create(level, EntitySpawnReason.COMMAND);
				condor.snapTo(0.5, this.y + 5, 14.5);
				condor.setNoAi(true);
				condor.setYRot(180);
				condor.yBodyRot = 180;
				level.addFreshEntity(condor);
			});
			world.getServer().runCommand("tp @a 0.5 " + (this.y + 1.5) + " -3 0 8");
			world.getConnection().waitForChunksRender();
			context.waitTicks(30);
			context.takeScreenshot("cielo_mobs");

			// --- las 4 naves en el cielo
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				this.nave(level, CieloStructure.Tipo.GLOBO, new BlockPos(-40, this.y + 40, 60));
				this.nave(level, CieloStructure.Tipo.AVIONETA, new BlockPos(5, this.y + 45, 70));
				this.nave(level, CieloStructure.Tipo.DIRIGIBLE, new BlockPos(60, this.y + 50, 80));
				this.nave(level, CieloStructure.Tipo.FORTALEZA, new BlockPos(0, this.y + 70, 150));
			});
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("tp @a 0.5 " + (this.y + 30) + " -30 0 -8");
			world.getConnection().waitForChunksRender();
			context.waitTicks(60);
			context.takeScreenshot("cielo_naves");
			world.getServer().runCommand("tp @a -40 " + (this.y + 38) + " 45 0 -25");
			context.waitTicks(40);
			context.takeScreenshot("cielo_globo");
			world.getServer().runCommand("tp @a 60 " + (this.y + 52) + " 45 0 10");
			context.waitTicks(40);
			context.takeScreenshot("cielo_dirigible");
			world.getServer().runCommand("tp @a 0 " + (this.y + 82) + " 118 0 25");
			context.waitTicks(40);
			context.takeScreenshot("cielo_fortaleza");
		}
	}
}
