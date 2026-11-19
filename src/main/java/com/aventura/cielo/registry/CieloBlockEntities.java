package com.aventura.cielo.registry;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.block.NucleoDelJefeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CieloBlockEntities {
	public static final BlockEntityType<NucleoDelJefeBlockEntity> NUCLEO_DEL_JEFE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		CieloMod.id("nucleo_del_jefe"),
		FabricBlockEntityTypeBuilder.<NucleoDelJefeBlockEntity>create(NucleoDelJefeBlockEntity::new, CieloBlocks.NUCLEO_DEL_JEFE).build());

	private CieloBlockEntities() {
	}

	public static void init() {
		CieloMod.LOGGER.debug("Block entities del cielo registrados");
	}
}
