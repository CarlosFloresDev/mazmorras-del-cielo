package com.aventura.cielo.registry;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.block.EscaleraDeCuerdaBlock;
import com.aventura.cielo.block.NucleoDelJefeBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class CieloBlocks {
	public static final Block ESCALERA_DE_CUERDA = Blocks.register(ResourceKey.create(Registries.BLOCK, CieloMod.id("escalera_de_cuerda")),
		EscaleraDeCuerdaBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOL).strength(0.4F)
			.noCollision().noOcclusion().pushReaction(PushReaction.POPPED));
	public static final Block NUCLEO_DEL_JEFE = Blocks.register(ResourceKey.create(Registries.BLOCK, CieloMod.id("nucleo_del_jefe")),
		NucleoDelJefeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.LODESTONE)
			.strength(-1.0F, 3600000.0F).lightLevel(s -> 10).pushReaction(PushReaction.IMMOVEABLE).noLootTable());

	public static final Block SELLO_CELESTIAL = Blocks.register(ResourceKey.create(Registries.BLOCK, CieloMod.id("sello_celestial")),
		com.aventura.cielo.block.SelloCelestialBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
			.sound(SoundType.AMETHYST).strength(-1.0F, 3600000.0F).lightLevel(s -> 7).pushReaction(PushReaction.IMMOVEABLE).noLootTable());

	private CieloBlocks() {
	}

	public static void init() {
		CieloMod.LOGGER.debug("Bloques del cielo registrados");
	}
}
