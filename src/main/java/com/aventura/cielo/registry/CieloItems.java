package com.aventura.cielo.registry;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.item.BombaItem;
import com.aventura.cielo.item.BrujulaDelCieloItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

public final class CieloItems {
	private static final List<Item> TAB = new ArrayList<>();

	public static final ArmorMaterial SOMBRERO = new ArmorMaterial(35, Map.of(ArmorType.HELMET, 4, ArmorType.BOOTS, 1,
		ArmorType.LEGGINGS, 1, ArmorType.CHESTPLATE, 1, ArmorType.BODY, 1), 18, SoundEvents.ARMOR_EQUIP_LEATHER, 2.5F, 0.1F,
		ItemTags.REPAIRS_LEATHER_ARMOR, ResourceKey.create(EquipmentAssets.ROOT_ID, CieloMod.id("sombrero_del_capitan")));

	public static final Item BRUJULA_DEL_CIELO = register("brujula_del_cielo", BrujulaDelCieloItem::new,
		new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	public static final Item ESCALERA_DE_CUERDA = block("escalera_de_cuerda", CieloBlocks.ESCALERA_DE_CUERDA);
	public static final Item BOMBA = register("bomba", BombaItem::new, new Item.Properties().stacksTo(16));
	public static final Item PLUMA_DE_ACERO = register("pluma_de_acero", Item::new, new Item.Properties().rarity(Rarity.RARE));
	public static final Item SOMBRERO_DEL_CAPITAN = register("sombrero_del_capitan", Item::new,
		new Item.Properties().humanoidArmor(SOMBRERO, ArmorType.HELMET).rarity(Rarity.EPIC));
	public static final Item ALAS_DEL_CONDOR = register("alas_del_condor", Item::new, new Item.Properties()
		.durability(1200).rarity(Rarity.EPIC)
		.component(DataComponents.GLIDER, Unit.INSTANCE)
		.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
			.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, CieloMod.id("alas_del_condor"))).setDamageOnHurt(false).build())
		.repairable(PLUMA_DE_ACERO));
	public static final Item NUCLEO_DEL_JEFE = block("nucleo_del_jefe", CieloBlocks.NUCLEO_DEL_JEFE);

	public static final Item HUEVO_PIRATA = egg("huevo_pirata_del_cielo", CieloEntities.PIRATA_DEL_CIELO);
	public static final Item HUEVO_ARCABUCERO = egg("huevo_arcabucero_del_cielo", CieloEntities.ARCABUCERO_DEL_CIELO);
	public static final Item HUEVO_MECANICO = egg("huevo_mecanico_del_dirigible", CieloEntities.MECANICO_DEL_DIRIGIBLE);
	public static final Item HUEVO_CAPITAN = egg("huevo_capitan_barbanube", CieloEntities.CAPITAN_BARBANUBE);
	public static final Item HUEVO_CONDOR = egg("huevo_condor_de_acero", CieloEntities.CONDOR_DE_ACERO);
	public static final Item HUEVO_GUARDIAN = egg("huevo_guardian_celeste", CieloEntities.GUARDIAN_CELESTE);
	public static final Item HUEVO_BRUJO = egg("huevo_brujo_del_viento", CieloEntities.BRUJO_DEL_VIENTO);
	public static final Item LLAVE_CELESTIAL = register("llave_celestial", Item::new, new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)
		.component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
	public static final Item SELLO_CELESTIAL = block("sello_celestial", CieloBlocks.SELLO_CELESTIAL);

    public static final Item CINTURON_JEAN_BECK = register("cinturon_jean_beck", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
        .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, new net.minecraft.world.entity.ai.attributes.AttributeModifier(CieloMod.id("cinturon_vida"), 8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.OFFHAND)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(CieloMod.id("cinturon_firmeza"), 0.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.OFFHAND).build()));
    public static final Item HOJA_DE_CENIZA = register("hoja_de_ceniza", com.aventura.cielo.item.HojaDeCenizaItem::new,
        new Item.Properties().sword(net.minecraft.world.item.ToolMaterial.NETHERITE,3,-2.4F).rarity(Rarity.EPIC).fireResistant());
    public static final Item FRAGMENTO_DEL_VACIO = register("fragmento_del_vacio", Item::new,new Item.Properties().rarity(Rarity.RARE));
    public static final Item ORBE_DEL_VACIO = register("orbe_del_vacio", com.aventura.cielo.item.OrbeDelVacioItem::new,new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final Item RACION_DE_COCINA = register("racion_de_cocina", Item::new,new Item.Properties().food(net.minecraft.world.food.Foods.COOKED_BEEF));
    public static final Item BRASA_INESTABLE = register("brasa_inestable",Item::new,new Item.Properties());
    public static final Item HUEVO_JEAN_BECK = egg("huevo_jean_beck", CieloEntities.JEAN_BECK);
    public static final Item HUEVO_SENOR_DE_LA_CENIZA = egg("huevo_senor_de_la_ceniza", CieloEntities.SENOR_DE_LA_CENIZA);
    public static final Item HUEVO_HERALDO_DEL_VACIO = egg("huevo_heraldo_del_vacio", CieloEntities.HERALDO_DEL_VACIO);
    public static final Item HUEVO_AYUDANTE_COCINA = egg("huevo_ayudante_cocina", CieloEntities.AYUDANTE_COCINA);
    public static final Item HUEVO_BOMBARDERO_CENIZA = egg("huevo_bombardero_ceniza", CieloEntities.BOMBARDERO_CENIZA);
    public static final Item HUEVO_FUEGO_FATUO = egg("huevo_fuego_fatuo", CieloEntities.FUEGO_FATUO);

    public static final Item HUEVO_DUELISTA_COCINA = egg("huevo_duelista_cocina", CieloEntities.DUELISTA_COCINA);
    public static final Item HUEVO_MAESTRO_RACIONES = egg("huevo_maestro_raciones", CieloEntities.MAESTRO_RACIONES);
    public static final Item HUEVO_GUARDIA_ESCORIA = egg("huevo_guardia_escoria", CieloEntities.GUARDIA_ESCORIA);
    public static final Item HUEVO_CANTOR_BRASAS = egg("huevo_cantor_brasas", CieloEntities.CANTOR_BRASAS);
    public static final Item HUEVO_ACECHADOR_VACIO = egg("huevo_acechador_vacio", CieloEntities.ACECHADOR_VACIO);
    public static final Item HUEVO_TEJEDOR_VACIO = egg("huevo_tejedor_vacio", CieloEntities.TEJEDOR_VACIO);

	private CieloItems() {
	}

	private static Item egg(String name, EntityType<?> type) {
		return register(name, net.minecraft.world.item.SpawnEggItem::new, new Item.Properties().spawnEgg(type));
	}

	private static Item block(String name, net.minecraft.world.level.block.Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CieloMod.id(name));
		BlockItem item = new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
		item.registerBlocks(Item.BY_BLOCK, item);
		TAB.add(item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CieloMod.id(name));
		Item item = factory.apply(properties.setId(key));
		TAB.add(item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, CieloMod.id("cielo")),
			FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.cielo.cielo"))
				.icon(() -> new ItemStack(BRUJULA_DEL_CIELO))
				.displayItems((params, output) -> TAB.forEach(output::accept))
				.build());
	}
}
