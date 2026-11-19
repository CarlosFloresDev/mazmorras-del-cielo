package com.aventura.cielo;

import com.aventura.cielo.registry.CieloBlockEntities;
import com.aventura.cielo.registry.CieloBlocks;
import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.registry.CieloItems;
import com.aventura.cielo.registry.CieloWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CieloMod implements ModInitializer {
	public static final String MOD_ID = "cielo";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		CieloBlocks.init();
		CieloEntities.init();
		CieloItems.init();
		CieloBlockEntities.init();
		CieloWorld.init();
		com.aventura.cielo.world.ZonaCiudadela.registrar();
		this.botinDeOtrosMods();
		LOGGER.info("Mazmorras del Cielo cargado: mira hacia arriba...");
	}

	/** Si estan Arequipa o Tecsup, sus objetos tambien aparecen en los cofres del cielo. */
	private void botinDeOtrosMods() {
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			String id = key.identifier().toString();
			if (!id.startsWith("cielo:chests/")) {
				return;
			}
			addIf(table, "arequipa", "arequipa:ficha", 0.6F);
			addIf(table, "arequipa", "arequipa:pisco_sour", 0.3F);
			addIf(table, "tecsup", "tecsup:plc", id.endsWith("globo") ? 0.0F : 0.3F);
			addIf(table, "tecsup", "tecsup:lingote_de_acero_industrial", id.endsWith("dirigible") || id.endsWith("fortaleza") ? 0.5F : 0.0F);
		});
	}

	private static void addIf(net.minecraft.world.level.storage.loot.LootTable.Builder table, String mod, String item, float chance) {
		if (chance <= 0 || !FabricLoader.getInstance().isModLoaded(mod)) {
			return;
		}
		BuiltInRegistries.ITEM.getOptional(Identifier.parse(item)).ifPresent(it ->
			table.withPool(LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(chance)).add(LootItem.lootTableItem(it))));
	}
}
