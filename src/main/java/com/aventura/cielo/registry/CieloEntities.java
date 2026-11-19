package com.aventura.cielo.registry;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.entity.*;
import com.aventura.cielo.entity.ArcabuceroDelCielo;
import com.aventura.cielo.entity.Bomba;
import com.aventura.cielo.entity.CapitanBarbanube;
import com.aventura.cielo.entity.CondorDeAcero;
import com.aventura.cielo.entity.MecanicoDelDirigible;
import com.aventura.cielo.entity.PirataDelCielo;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class CieloEntities {
	public static final EntityType<PirataDelCielo> PIRATA_DEL_CIELO = register("pirata_del_cielo",
		EntityType.Builder.of(PirataDelCielo::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10));
	public static final EntityType<ArcabuceroDelCielo> ARCABUCERO_DEL_CIELO = register("arcabucero_del_cielo",
		EntityType.Builder.of(ArcabuceroDelCielo::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10));
	public static final EntityType<MecanicoDelDirigible> MECANICO_DEL_DIRIGIBLE = register("mecanico_del_dirigible",
		EntityType.Builder.of(MecanicoDelDirigible::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10));
	public static final EntityType<CapitanBarbanube> CAPITAN_BARBANUBE = register("capitan_barbanube",
		EntityType.Builder.of(CapitanBarbanube::new, MobCategory.MONSTER).sized(0.6F, 1.95F).fireImmune().clientTrackingRange(12));
	public static final EntityType<CondorDeAcero> CONDOR_DE_ACERO = register("condor_de_acero",
		EntityType.Builder.of(CondorDeAcero::new, MobCategory.MONSTER).sized(3.2F, 1.8F).fireImmune().clientTrackingRange(16));
	public static final EntityType<com.aventura.cielo.entity.GuardianCeleste> GUARDIAN_CELESTE = register("guardian_celeste",
		EntityType.Builder.of(com.aventura.cielo.entity.GuardianCeleste::new, MobCategory.MONSTER).sized(0.7F, 2.1F).clientTrackingRange(10));
	public static final EntityType<com.aventura.cielo.entity.BrujoDelViento> BRUJO_DEL_VIENTO = register("brujo_del_viento",
		EntityType.Builder.of(com.aventura.cielo.entity.BrujoDelViento::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10));
	public static final EntityType<Bomba> BOMBA = register("bomba",
		EntityType.Builder.<Bomba>of(Bomba::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(10));

    public static final EntityType<JeanBeck> JEAN_BECK = register("jean_beck", EntityType.Builder.<JeanBeck>of(JeanBeck::new, MobCategory.MONSTER).sized(1.8F, 3.3F).clientTrackingRange(14));
    public static final EntityType<SenorDeLaCeniza> SENOR_DE_LA_CENIZA = register("senor_de_la_ceniza", EntityType.Builder.<SenorDeLaCeniza>of(SenorDeLaCeniza::new, MobCategory.MONSTER).sized(1.1F, 3.2F).fireImmune().clientTrackingRange(14));
    public static final EntityType<HeraldoDelVacio> HERALDO_DEL_VACIO = register("heraldo_del_vacio", EntityType.Builder.<HeraldoDelVacio>of(HeraldoDelVacio::new, MobCategory.MONSTER).sized(1.0F, 3.0F).fireImmune().clientTrackingRange(14));
    public static final EntityType<EnemigoExpedicion> AYUDANTE_COCINA = register("ayudante_cocina", EntityType.Builder.<EnemigoExpedicion>of(EnemigoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 1.9F).clientTrackingRange(14));
    public static final EntityType<EnemigoExpedicion> BOMBARDERO_CENIZA = register("bombardero_ceniza", EntityType.Builder.<EnemigoExpedicion>of(EnemigoExpedicion::new, MobCategory.MONSTER).sized(0.8F, 1.8F).fireImmune().clientTrackingRange(14));
    public static final EntityType<EnemigoExpedicion> FUEGO_FATUO = register("fuego_fatuo", EntityType.Builder.<EnemigoExpedicion>of(EnemigoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 1.6F).clientTrackingRange(14));
    public static final EntityType<ProyectilExpedicion> COMIDA_ARROJADA = register("comida_arrojada", EntityType.Builder.<ProyectilExpedicion>of(ProyectilExpedicion::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(14));
    public static final EntityType<ProyectilExpedicion> ORBE_HOSTIL = register("orbe_hostil", EntityType.Builder.<ProyectilExpedicion>of(ProyectilExpedicion::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(14));

    public static final EntityType<VeteranoExpedicion> DUELISTA_COCINA = register("duelista_cocina", EntityType.Builder.<VeteranoExpedicion>of(VeteranoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 2.0F).clientTrackingRange(14));
    public static final EntityType<VeteranoExpedicion> MAESTRO_RACIONES = register("maestro_raciones", EntityType.Builder.<VeteranoExpedicion>of(VeteranoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 2.0F).clientTrackingRange(14));
    public static final EntityType<VeteranoExpedicion> GUARDIA_ESCORIA = register("guardia_escoria", EntityType.Builder.<VeteranoExpedicion>of(VeteranoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 2.0F).fireImmune().clientTrackingRange(14));
    public static final EntityType<VeteranoExpedicion> CANTOR_BRASAS = register("cantor_brasas", EntityType.Builder.<VeteranoExpedicion>of(VeteranoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 2.0F).fireImmune().clientTrackingRange(14));
    public static final EntityType<VeteranoExpedicion> ACECHADOR_VACIO = register("acechador_vacio", EntityType.Builder.<VeteranoExpedicion>of(VeteranoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 2.0F).clientTrackingRange(14));
    public static final EntityType<VeteranoExpedicion> TEJEDOR_VACIO = register("tejedor_vacio", EntityType.Builder.<VeteranoExpedicion>of(VeteranoExpedicion::new, MobCategory.MONSTER).sized(0.7F, 2.0F).clientTrackingRange(14));

	private CieloEntities() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, CieloMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
        FabricDefaultAttributeRegistry.register(DUELISTA_COCINA, VeteranoExpedicion.atributos(56,11,8,0.33));
        FabricDefaultAttributeRegistry.register(MAESTRO_RACIONES, VeteranoExpedicion.atributos(46,7,6,0.29));
        FabricDefaultAttributeRegistry.register(GUARDIA_ESCORIA, VeteranoExpedicion.atributos(84,13,18,0.26));
        FabricDefaultAttributeRegistry.register(CANTOR_BRASAS, VeteranoExpedicion.atributos(50,8,8,0.3));
        FabricDefaultAttributeRegistry.register(ACECHADOR_VACIO, VeteranoExpedicion.atributos(48,12,6,0.35));
        FabricDefaultAttributeRegistry.register(TEJEDOR_VACIO, VeteranoExpedicion.atributos(52,9,7,0.3));
        FabricDefaultAttributeRegistry.register(FUEGO_FATUO, EnemigoExpedicion.createAttributes());
        FabricDefaultAttributeRegistry.register(BOMBARDERO_CENIZA, EnemigoExpedicion.createAttributes());
        FabricDefaultAttributeRegistry.register(AYUDANTE_COCINA, EnemigoExpedicion.createAttributes());
        FabricDefaultAttributeRegistry.register(HERALDO_DEL_VACIO, HeraldoDelVacio.createAttributes());
        FabricDefaultAttributeRegistry.register(SENOR_DE_LA_CENIZA, SenorDeLaCeniza.createAttributes());
        FabricDefaultAttributeRegistry.register(JEAN_BECK, JeanBeck.createAttributes());
		FabricDefaultAttributeRegistry.register(PIRATA_DEL_CIELO, PirataDelCielo.createAttributes());
		FabricDefaultAttributeRegistry.register(ARCABUCERO_DEL_CIELO, ArcabuceroDelCielo.createAttributes());
		FabricDefaultAttributeRegistry.register(MECANICO_DEL_DIRIGIBLE, MecanicoDelDirigible.createAttributes());
		FabricDefaultAttributeRegistry.register(CAPITAN_BARBANUBE, CapitanBarbanube.createAttributes());
		FabricDefaultAttributeRegistry.register(CONDOR_DE_ACERO, CondorDeAcero.createAttributes());
		FabricDefaultAttributeRegistry.register(GUARDIAN_CELESTE, com.aventura.cielo.entity.GuardianCeleste.createAttributes());
		FabricDefaultAttributeRegistry.register(BRUJO_DEL_VIENTO, com.aventura.cielo.entity.BrujoDelViento.createAttributes());
	}
}
