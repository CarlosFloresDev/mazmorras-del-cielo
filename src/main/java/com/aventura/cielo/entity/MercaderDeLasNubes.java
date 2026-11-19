package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Aldeano vanilla: guarda nombre, ofertas y persistencia sin registro ni textura adicionales. */
public final class MercaderDeLasNubes {
	private MercaderDeLasNubes() { }

	public static Item moneda() {
		return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("arequipa", "ficha"))
			.filter(item -> item != Items.AIR).orElse(Items.EMERALD);
	}

	public static Villager crear(ServerLevel level, BlockPos pos) {
		Villager mercader = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
		if (mercader == null) return null;
		mercader.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		mercader.setVillagerData(mercader.getVillagerData()
			.withProfession(level.registryAccess(), VillagerProfession.CARTOGRAPHER).withLevel(5));
		mercader.setPersistenceRequired();
		mercader.setPermanentlyInvulnerable(true);
		mercader.setNoAi(true);
		mercader.setCustomName(Component.translatable("entity.cielo.mercader_de_las_nubes"));
		mercader.setCustomNameVisible(true);
		MerchantOffers ofertas = new MerchantOffers();
		Item moneda = moneda();
		ofertas.add(oferta(moneda, 2, new ItemStack(Items.BREAD, 8)));
		ofertas.add(oferta(moneda, 3, new ItemStack(Items.ARROW, 16)));
		ofertas.add(oferta(moneda, 5, PotionContents.createItemStack(Items.POTION, Potions.HEALING)));
		ofertas.add(oferta(moneda, 12, new ItemStack(CieloItems.BRUJULA_DEL_CIELO)));
		ofertas.add(oferta(moneda, 4, new ItemStack(CieloItems.ESCALERA_DE_CUERDA, 16)));
		mercader.setOffers(ofertas);
		return mercader;
	}

	private static MerchantOffer oferta(Item moneda, int precio, ItemStack producto) {
		// Sin IA ni reloj diurno para reabastecer: existencias amplias y precio fijo.
		return new MerchantOffer(new ItemCost(moneda, precio), producto, Integer.MAX_VALUE, 0, 0.0F);
	}
}
