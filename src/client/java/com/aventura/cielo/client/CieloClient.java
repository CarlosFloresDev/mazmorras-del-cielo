package com.aventura.cielo.client;

import com.aventura.cielo.registry.CieloEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class CieloClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        EntityRendererRegistry.register(CieloEntities.DUELISTA_COCINA, ctx -> new ExpedicionRenderer<>(ctx, "duelista_cocina", 6));
        EntityRendererRegistry.register(CieloEntities.MAESTRO_RACIONES, ctx -> new ExpedicionRenderer<>(ctx, "maestro_raciones", 7));
        EntityRendererRegistry.register(CieloEntities.GUARDIA_ESCORIA, ctx -> new ExpedicionRenderer<>(ctx, "guardia_escoria", 8));
        EntityRendererRegistry.register(CieloEntities.CANTOR_BRASAS, ctx -> new ExpedicionRenderer<>(ctx, "cantor_brasas", 9));
        EntityRendererRegistry.register(CieloEntities.ACECHADOR_VACIO, ctx -> new ExpedicionRenderer<>(ctx, "acechador_vacio", 10));
        EntityRendererRegistry.register(CieloEntities.TEJEDOR_VACIO, ctx -> new ExpedicionRenderer<>(ctx, "tejedor_vacio", 11));
        EntityRendererRegistry.register(CieloEntities.JEAN_BECK, ctx -> new ExpedicionRenderer<>(ctx, "jean_beck", 0));
        EntityRendererRegistry.register(CieloEntities.SENOR_DE_LA_CENIZA, ctx -> new ExpedicionRenderer<>(ctx, "senor_de_la_ceniza", 1));
        EntityRendererRegistry.register(CieloEntities.HERALDO_DEL_VACIO, ctx -> new ExpedicionRenderer<>(ctx, "heraldo_del_vacio", 2));
        EntityRendererRegistry.register(CieloEntities.AYUDANTE_COCINA, ctx -> new ExpedicionRenderer<>(ctx, "ayudante_cocina", 3));
        EntityRendererRegistry.register(CieloEntities.BOMBARDERO_CENIZA, ctx -> new ExpedicionRenderer<>(ctx, "bombardero_ceniza", 4));
        EntityRendererRegistry.register(CieloEntities.FUEGO_FATUO, ctx -> new ExpedicionRenderer<>(ctx, "fuego_fatuo", 5));
        EntityRendererRegistry.register(CieloEntities.COMIDA_ARROJADA, ThrownItemRenderer::new);
        EntityRendererRegistry.register(CieloEntities.ORBE_HOSTIL, ThrownItemRenderer::new);

		ModelLayerRegistry.registerModelLayer(CieloModelos.TRIPULANTE, CieloModelos::tripulante);
		ModelLayerRegistry.registerModelLayer(CieloModelos.CAPITAN, CieloModelos::capitan);
		ModelLayerRegistry.registerModelLayer(CieloModelos.CONDOR, CondorRenderer.Modelo::crear);
		EntityRendererRegistry.register(CieloEntities.PIRATA_DEL_CIELO, ctx -> new CieloHumanoRenderer<>(ctx, "pirata_del_cielo"));
		EntityRendererRegistry.register(CieloEntities.ARCABUCERO_DEL_CIELO, ctx -> new CieloHumanoRenderer<>(ctx, "arcabucero_del_cielo"));
		EntityRendererRegistry.register(CieloEntities.MECANICO_DEL_DIRIGIBLE, ctx -> new CieloHumanoRenderer<>(ctx, "mecanico_del_dirigible"));
		EntityRendererRegistry.register(CieloEntities.CAPITAN_BARBANUBE, CapitanRenderer::new);
		EntityRendererRegistry.register(CieloEntities.CONDOR_DE_ACERO, CondorRenderer::new);
		EntityRendererRegistry.register(CieloEntities.BOMBA, ThrownItemRenderer::new);
		EntityRendererRegistry.register(CieloEntities.GUARDIAN_CELESTE, ctx -> new CieloHumanoRenderer<>(ctx, "guardian_celeste"));
		EntityRendererRegistry.register(CieloEntities.BRUJO_DEL_VIENTO, ctx -> new CieloHumanoRenderer<>(ctx, "brujo_del_viento"));
	}
}
