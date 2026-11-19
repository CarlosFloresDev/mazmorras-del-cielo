package com.aventura.cielo.client;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.entity.CondorDeAcero;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Condor de Acero: modelo propio (cuerpo, gorguera blanca, cabeza con pico ganchudo, alas de dos segmentos, cola y garras).
 * Animaciones: aleteo al volar, alas plegadas en la picada, alas caidas y cabeza baja cuando esta aturdido, y pico abierto al chillar.
 */
public class CondorRenderer extends MobRenderer<CondorDeAcero, CondorRenderer.Estado, CondorRenderer.Modelo> {
	private static final Identifier TEXTURA = CieloMod.id("textures/entity/condor_de_acero.png");

	public static class Estado extends LivingEntityRenderState {
		int estado;
	}

	public static class Modelo extends EntityModel<Estado> {
		private final ModelPart cuerpo;
		private final ModelPart cuello;
		private final ModelPart cabeza;
		private final ModelPart pico;
		private final ModelPart alaIzq;
		private final ModelPart puntaIzq;
		private final ModelPart alaDer;
		private final ModelPart puntaDer;
		private final ModelPart cola;

		public Modelo(ModelPart root) {
			super(root);
			this.cuerpo = root.getChild("cuerpo");
			this.cuello = this.cuerpo.getChild("cuello");
			this.cabeza = this.cuello.getChild("cabeza");
			this.pico = this.cabeza.getChild("pico");
			this.alaIzq = this.cuerpo.getChild("ala_izq");
			this.puntaIzq = this.alaIzq.getChild("punta_izq");
			this.alaDer = this.cuerpo.getChild("ala_der");
			this.puntaDer = this.alaDer.getChild("punta_der");
			this.cola = this.cuerpo.getChild("cola");
		}

		public static LayerDefinition crear() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			PartDefinition cuerpo = root.addOrReplaceChild("cuerpo",
				CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -9.0F, 10.0F, 8.0F, 18.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
			PartDefinition cuello = cuerpo.addOrReplaceChild("cuello",
				CubeListBuilder.create().texOffs(0, 26).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 4.0F, 5.0F)
					.texOffs(56, 0).addBox(-3.5F, -3.0F, -1.5F, 7.0F, 6.0F, 2.0F),
				PartPose.offset(0.0F, -2.0F, -9.0F));
			PartDefinition cabeza = cuello.addOrReplaceChild("cabeza",
				CubeListBuilder.create().texOffs(18, 26).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 0.0F, -5.0F));
			cabeza.addOrReplaceChild("pico",
				CubeListBuilder.create().texOffs(40, 26).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F)
					.texOffs(52, 26).addBox(-1.0F, 1.0F, -4.0F, 2.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, 0.0F, -5.0F));
			PartDefinition alaIzq = cuerpo.addOrReplaceChild("ala_izq",
				CubeListBuilder.create().texOffs(0, 40).addBox(0.0F, -1.0F, 0.0F, 14.0F, 2.0F, 12.0F), PartPose.offset(5.0F, -3.0F, -6.0F));
			alaIzq.addOrReplaceChild("punta_izq",
				CubeListBuilder.create().texOffs(52, 40).addBox(0.0F, -0.5F, 0.0F, 18.0F, 1.0F, 12.0F), PartPose.offset(14.0F, 0.0F, 0.0F));
			PartDefinition alaDer = cuerpo.addOrReplaceChild("ala_der",
				CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-14.0F, -1.0F, 0.0F, 14.0F, 2.0F, 12.0F), PartPose.offset(-5.0F, -3.0F, -6.0F));
			alaDer.addOrReplaceChild("punta_der",
				CubeListBuilder.create().texOffs(52, 40).mirror().addBox(-18.0F, -0.5F, 0.0F, 18.0F, 1.0F, 12.0F), PartPose.offset(-14.0F, 0.0F, 0.0F));
			cuerpo.addOrReplaceChild("cola",
				CubeListBuilder.create().texOffs(72, 0).addBox(-5.0F, -1.0F, 0.0F, 10.0F, 2.0F, 9.0F), PartPose.offset(0.0F, -2.0F, 9.0F));
			cuerpo.addOrReplaceChild("garra_izq",
				CubeListBuilder.create().texOffs(90, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(3.0F, 4.0F, 3.0F));
			cuerpo.addOrReplaceChild("garra_der",
				CubeListBuilder.create().texOffs(90, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(-3.0F, 4.0F, 3.0F));
			return LayerDefinition.create(mesh, 128, 64);
		}

		@Override
		public void setupAnim(Estado s) {
			super.setupAnim(s);
			float t = s.ageInTicks;
			float aleteo = Mth.sin(t * 0.32F);
			float ala = aleteo * 0.55F;
			float punta = Mth.sin(t * 0.32F - 0.6F) * 0.45F;
			this.cuerpo.xRot = 0.0F;
			this.cuello.xRot = 0.15F;
			this.cabeza.xRot = -0.1F;
			this.pico.xRot = 0.0F;
			this.cola.xRot = Mth.sin(t * 0.16F) * 0.08F;
			this.alaIzq.yRot = 0.0F;
			this.alaDer.yRot = 0.0F;
			switch (s.estado) {
				case CondorDeAcero.PICADA -> {
					// alas plegadas hacia atras
					this.alaIzq.zRot = 0.25F;
					this.alaDer.zRot = -0.25F;
					this.alaIzq.yRot = -0.9F;
					this.alaDer.yRot = 0.9F;
					this.puntaIzq.zRot = 0.2F;
					this.puntaDer.zRot = -0.2F;
					this.cuello.xRot = 0.35F;
				}
				case CondorDeAcero.ATURDIDO -> {
					// alas caidas, cabeza al piso
					this.alaIzq.zRot = 0.75F;
					this.alaDer.zRot = -0.75F;
					this.puntaIzq.zRot = 0.4F;
					this.puntaDer.zRot = -0.4F;
					this.cuello.xRot = 0.8F + Mth.sin(t * 0.5F) * 0.05F;
					this.cabeza.zRot = Mth.sin(t * 0.3F) * 0.2F;
				}
				case CondorDeAcero.PREPARAR_PICADA, CondorDeAcero.VIENTO -> {
					// alas abiertas al maximo, pico abierto (chillido)
					this.alaIzq.zRot = -0.35F + ala * 0.3F;
					this.alaDer.zRot = 0.35F - ala * 0.3F;
					this.puntaIzq.zRot = -0.15F;
					this.puntaDer.zRot = 0.15F;
					this.cuello.xRot = -0.35F;
					this.pico.xRot = 0.35F + Mth.sin(t * 1.2F) * 0.1F;
				}
				default -> {
					this.alaIzq.zRot = ala;
					this.alaDer.zRot = -ala;
					this.puntaIzq.zRot = punta;
					this.puntaDer.zRot = -punta;
					this.cabeza.zRot = 0.0F;
					if (s.estado == CondorDeAcero.PLUMAS) {
						this.pico.xRot = 0.25F;
					}
				}
			}
		}
	}

	public CondorRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelo(context.bakeLayer(CieloModelos.CONDOR)), 2.0F);
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(CondorDeAcero entity, Estado state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.estado = entity.estado();
	}

	@Override
	protected void scale(Estado state, PoseStack poseStack) {
		poseStack.scale(2.4F, 2.4F, 2.4F);
	}

	@Override
	protected void setupRotations(Estado state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		if (state.estado == CondorDeAcero.PICADA) {
			poseStack.rotateDegrees(Axis.XP, 35.0F);
		}
	}

	@Override
	public Identifier getTextureLocation(Estado state) {
		return TEXTURA;
	}
}
