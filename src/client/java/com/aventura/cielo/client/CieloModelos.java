package com.aventura.cielo.client;

import com.aventura.cielo.CieloMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Capas de modelo del mod: tripulantes humanoides, el Capitan (con tricornio y barba) y el Condor de Acero. */
public final class CieloModelos {
	public static final ModelLayerLocation TRIPULANTE = new ModelLayerLocation(CieloMod.id("tripulante"), "main");
	public static final ModelLayerLocation CAPITAN = new ModelLayerLocation(CieloMod.id("capitan_barbanube"), "main");
	public static final ModelLayerLocation CONDOR = new ModelLayerLocation(CieloMod.id("condor_de_acero"), "main");

	private CieloModelos() {
	}

	public static LayerDefinition tripulante() {
		return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
	}

	public static LayerDefinition capitan() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition head = mesh.getRoot().getChild("head");
		// tricornio: ala ancha + copa + punta delantera
		head.addOrReplaceChild("ala_sombrero", CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, -9.0F, -6.0F, 12.0F, 1.0F, 12.0F),
			PartPose.ZERO);
		head.addOrReplaceChild("copa_sombrero", CubeListBuilder.create().texOffs(0, 45).addBox(-4.5F, -12.0F, -4.5F, 9.0F, 3.0F, 9.0F),
			PartPose.ZERO);
		head.addOrReplaceChild("punta_sombrero", CubeListBuilder.create().texOffs(48, 32).addBox(-2.0F, -10.5F, -7.5F, 4.0F, 2.0F, 2.0F),
			PartPose.ZERO);
		// barba gris
		head.addOrReplaceChild("barba", CubeListBuilder.create().texOffs(36, 45).addBox(-4.0F, -2.0F, -4.6F, 8.0F, 5.0F, 1.0F), PartPose.ZERO);
		// faldones del abrigo
		mesh.getRoot().getChild("body").addOrReplaceChild("abrigo",
			CubeListBuilder.create().texOffs(36, 52).addBox(-4.0F, 12.0F, 1.5F, 8.0F, 6.0F, 1.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}
}
