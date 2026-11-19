package com.aventura.cielo.client;

import com.aventura.cielo.CieloMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;

/** Renderizador de los tripulantes humanoides (pirata, arcabucero y mecanico) con su propia textura. */
public class CieloHumanoRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier textura;

	public CieloHumanoRenderer(EntityRendererProvider.Context context, String textura) {
		super(context, new HumanoidModel<>(context.bakeLayer(CieloModelos.TRIPULANTE)), 0.5F);
		this.textura = CieloMod.id("textures/entity/" + textura + ".png");
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return this.textura;
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(T mob, HumanoidArm arm) {
		return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)
			? HumanoidModel.ArmPose.BOW_AND_ARROW
			: super.getArmPose(mob, arm);
	}
}
