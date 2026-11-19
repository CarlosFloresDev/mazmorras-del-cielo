package com.aventura.cielo.client;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.entity.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;

/** Seis siluetas propias, UV 128x128, articulaciones y accesorios animados. */
public class ExpedicionRenderer<T extends Monster> extends MobRenderer<T,ExpedicionRenderer.Estado,ExpedicionRenderer.Modelo> {
    private final Identifier textura;
    private final float escala;
    public static class Estado extends LivingEntityRenderState { int aviso; }
    public ExpedicionRenderer(EntityRendererProvider.Context context,String id,int estilo) {
        super(context,new Modelo(Modelo.crear(estilo).bakeRoot(),estilo),estilo<3?1.1F:0.5F);
        textura=CieloMod.id("textures/entity/"+id+".png"); escala=estilo<3?1.6F:1;
    }
    @Override public Estado createRenderState() { return new Estado(); }
    @Override public void extractRenderState(T e,Estado s,float t) {
        super.extractRenderState(e,s,t);
        s.aviso=e instanceof JefeExpedicion j?j.aviso():e instanceof EnemigoExpedicion m?m.aviso():0;
    }
    @Override protected void scale(Estado s,PoseStack p) { p.scale(escala,escala,escala); }
    @Override public Identifier getTextureLocation(Estado s) { return textura; }
    public static class Modelo extends EntityModel<Estado> {
        private final ModelPart cuerpo,cabeza,brazoI,brazoD,piernaI,piernaD,adorno;
        private final int estilo;
        Modelo(ModelPart root,int estilo) {
            super(root); this.estilo=estilo; cuerpo=root.getChild("cuerpo"); cabeza=cuerpo.getChild("cabeza");
            brazoI=cuerpo.getChild("brazo_i"); brazoD=cuerpo.getChild("brazo_d");
            piernaI=root.getChild("pierna_i"); piernaD=root.getChild("pierna_d"); adorno=cuerpo.getChild("adorno");
        }
        private static PartDefinition pieza(PartDefinition p,String n,int u,int v,float x,float y,float z,float w,float h,float d,PartPose pose) {
            return p.addOrReplaceChild(n,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d),pose);
        }
        public static LayerDefinition crear(int estilo) {
            MeshDefinition mesh=new MeshDefinition(); PartDefinition root=mesh.getRoot();
            boolean jean=estilo==0, vacio=estilo==2||estilo==5, chef=estilo==3;
            float ancho=jean?16:estilo==1?11:8, fondo=jean?10:6;
            PartDefinition cuerpo=pieza(root,"cuerpo",0,32,-ancho/2,-11,-fondo/2,ancho,13,fondo,PartPose.offset(0,16,0));
            PartDefinition cabeza=pieza(cuerpo,"cabeza",0,0,-4,-8,-4,8,8,8,PartPose.offset(0,-11,0));
            PartDefinition bi=pieza(cuerpo,"brazo_i",64,32,-2,-1,-2,jean?5:4,jean?8:11,4,PartPose.offset(ancho/2+2,-10,0));
            PartDefinition bd=pieza(cuerpo,"brazo_d",64,32,-2,-1,-2,jean?5:4,jean?8:11,4,PartPose.offset(-ancho/2-2,-10,0));
            pieza(root,"pierna_i",96,32,-2,0,-2,4,7,5,PartPose.offset(jean?5:2.5F,17,0));
            pieza(root,"pierna_d",96,32,-2,0,-2,4,7,5,PartPose.offset(jean?-5:-2.5F,17,0));
            PartDefinition adorno=cuerpo.addOrReplaceChild("adorno",CubeListBuilder.create(),PartPose.ZERO);
            if(jean) {
                // Tres volúmenes escalonados redondean barriga y costados; cabeza calva y bigote pequeño.
                pieza(cuerpo,"barriga",0,64,-7,-8,-8,14,11,6,PartPose.ZERO);
                pieza(cuerpo,"barriga_frente",48,64,-5,-6,-9,10,7,2,PartPose.ZERO);
                pieza(cuerpo,"delantal",0,90,-5,-8,-9.3F,10,12,1,PartPose.ZERO);
                pieza(cabeza,"nariz",40,0,-1,-4,-5.5F,2,2,2,PartPose.ZERO);
                pieza(cabeza,"bigote",52,0,-2.5F,-2.5F,-4.5F,5,1,1,PartPose.ZERO);
                pieza(cabeza,"oreja_i",40,8,4,-5,-1,2,3,2,PartPose.ZERO);
                pieza(cabeza,"oreja_d",40,8,-6,-5,-1,2,3,2,PartPose.ZERO);
                pieza(adorno,"cucharon",72,70,-1,0,-1,2,11,2,PartPose.offset(-12,-7,0));
                pieza(adorno,"cuchara",80,70,-3,10,-2,6,4,3,PartPose.offset(-12,-7,0));
            } else if(estilo==1) {
                pieza(cabeza,"visera",32,0,-5,-6,-5,10,2,2,PartPose.ZERO);
                for(int x:new int[]{-3,3}) pieza(cabeza,"cuerno"+x,56,0,-1,-6,-1,2,7,2,PartPose.offset(x,-7,0));
                pieza(bi,"hombrera",32,16,-3,-3,-3,7,5,7,PartPose.ZERO);
                pieza(bd,"hombrera",32,16,-4,-3,-3,7,5,7,PartPose.ZERO);
                pieza(bd,"hoja",72,80,-1,-1,-1,2,17,3,PartPose.offset(0,8,-3));
                pieza(cuerpo,"faldon",0,90,-6,1,-4,12,5,8,PartPose.ZERO);
                for(int i=0;i<4;i++) pieza(adorno,"brasa"+i,100,0,-1,-1,-1,2,4,2,PartPose.offset((i-1.5F)*4,-15,3));
            } else if(vacio) {
                pieza(cabeza,"capucha",32,0,-5,-9,-3,10,10,8,PartPose.ZERO);
                pieza(cuerpo,"tunica",0,90,-6,1,-4,12,7,8,PartPose.ZERO);
                pieza(bi,"manga",48,90,-3,5,-3,6,6,6,PartPose.ZERO);
                pieza(bd,"manga",48,90,-3,5,-3,6,6,6,PartPose.ZERO);
                for(int i=0;i<4;i++) {
                    float a=i*Mth.PI/2;
                    pieza(adorno,"cristal"+i,100,0,-1,-3,-1,2,6,2,PartPose.offset(Mth.cos(a)*12,-6,Mth.sin(a)*12));
                }
            } else if(chef) {
                pieza(cabeza,"gorro",32,0,-5,-11,-5,10,4,10,PartPose.ZERO);
                pieza(cuerpo,"delantal",0,90,-4,-9,-3.5F,8,12,1,PartPose.ZERO);
                pieza(bi,"sarten",72,70,-4,-4,-1,8,9,2,PartPose.offset(0,8,-3));
                pieza(bd,"cuchillo",96,70,-1,0,-1,2,7,1,PartPose.offset(0,8,0));
            } else {
                pieza(cuerpo,"barril",0,64,-6,-10,3,12,14,7,PartPose.ZERO);
                pieza(cabeza,"gafas",32,0,-4,-5,-5,8,2,2,PartPose.ZERO);
                pieza(adorno,"mecha",100,0,-1,-5,-1,2,6,2,PartPose.offset(0,-11,7));
                pieza(bi,"bomba",72,70,-3,6,-3,6,6,6,PartPose.ZERO);
            }
            return LayerDefinition.create(mesh,128,128);
        }
        @Override public void setupAnim(Estado s) {
            super.setupAnim(s);
            float walk=Mth.sin(s.walkAnimationPos*0.65F)*s.walkAnimationSpeed;
            piernaI.xRot=walk; piernaD.xRot=-walk; brazoI.xRot=-walk*0.7F; brazoD.xRot=walk*0.7F;
            cabeza.yRot=s.yRot*Mth.DEG_TO_RAD; cabeza.xRot=s.xRot*Mth.DEG_TO_RAD;
            cuerpo.y=16+Mth.sin(s.ageInTicks*0.1F)*(estilo==2||estilo==5?1.5F:0.25F);
            adorno.yRot=(estilo==2||estilo==5)?s.ageInTicks*0.035F:0;
            if(s.aviso>0) {
                brazoI.xRot=-2.1F; brazoD.xRot=-2.1F;
                cuerpo.xRot=0.18F+Mth.sin(s.ageInTicks*0.8F)*0.035F;
            }
        }
    }
}
