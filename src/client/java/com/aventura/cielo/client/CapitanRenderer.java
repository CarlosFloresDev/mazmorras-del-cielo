package com.aventura.cielo.client;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.entity.CapitanBarbanube;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Capitan Barbanube: levanta el sable al preparar el salto y abre los brazos girando en el torbellino. */
public class CapitanRenderer extends HumanoidMobRenderer<CapitanBarbanube, CapitanRenderer.Estado, CapitanRenderer.Modelo> {
	private static final Identifier TEXTURA = CieloMod.id("textures/entity/capitan_barbanube.png");

	public static class Estado extends HumanoidRenderState {
		int estado;
	}

	public static class Modelo extends HumanoidModel<Estado> {
		public Modelo(net.minecraft.client.model.geom.ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(Estado state) {
			super.setupAnim(state);
			if (state.estado == CapitanBarbanube.PREPARANDO_SALTO) {
				float temblor = Mth.sin(state.ageInTicks * 1.6F) * 0.08F;
				this.rightArm.xRot = (float) -Math.PI + temblor;
				this.leftArm.xRot = (float) -Math.PI * 0.85F - temblor;
				this.rightArm.zRot = 0.1F;
				this.leftArm.zRot = -0.1F;
				this.body.xRot = -0.15F;
			} else if (state.estado == CapitanBarbanube.TORBELLINO) {
				this.rightArm.xRot = -0.2F;
				this.leftArm.xRot = -0.2F;
				this.rightArm.zRot = 1.45F;
				this.leftArm.zRot = -1.45F;
			}
		}
	}

	public CapitanRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelo(context.bakeLayer(CieloModelos.CAPITAN)), 0.9F);
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(CapitanBarbanube entity, Estado state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.estado = entity.estado();
	}

	@Override
	public Identifier getTextureLocation(Estado state) {
		return TEXTURA;
	}
}
