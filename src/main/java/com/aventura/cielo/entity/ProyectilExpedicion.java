package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Comida arrojada y orbe rastreador: colisionan con bloques, caducan y nunca destruyen terreno. */
public class ProyectilExpedicion extends ThrowableItemProjectile {
    private LivingEntity objetivo;
    public ProyectilExpedicion(EntityType<? extends ProyectilExpedicion> type,Level level) { super(type,level); }
    public ProyectilExpedicion(Level l,LivingEntity owner,LivingEntity target,boolean voidOrb) {
        super(voidOrb?CieloEntities.ORBE_HOSTIL:CieloEntities.COMIDA_ARROJADA,owner,l,new ItemStack(voidOrb?Items.ENDER_PEARL:Items.BAKED_POTATO));
        objetivo=target;
    }
    private boolean vacio() { return getType()==CieloEntities.ORBE_HOSTIL; }
    @Override protected Item getDefaultItem() { return vacio()?Items.ENDER_PEARL:Items.BAKED_POTATO; }
    @Override public void tick() {
        if(vacio()) {
            setNoGravity(true);
            if(objetivo!=null&&objetivo.isAlive()) {
                Vec3 d=objetivo.getEyePosition().subtract(position()).normalize().scale(0.075);
                setDeltaMovement(getDeltaMovement().scale(0.92).add(d).normalize().scale(0.55));
            }
        }
        super.tick();
        if(level() instanceof ServerLevel l) {
            l.sendParticles(vacio()?ParticleTypes.PORTAL:ParticleTypes.CRIT,getX(),getY(),getZ(),2,0.1,0.1,0.1,0);
            if(tickCount>100) discard();
        }
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if(level() instanceof ServerLevel l) {
            if(hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity t && t!=getOwner()) {
                t.hurtServer(l,damageSources().thrown(this,getOwner()),vacio()?7:4);
                t.addEffect(new MobEffectInstance(vacio()?MobEffects.LEVITATION:MobEffects.NAUSEA,vacio()?35:100,0));
                if(!vacio()) t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,70,1));
            }
            discard();
        }
    }
}
