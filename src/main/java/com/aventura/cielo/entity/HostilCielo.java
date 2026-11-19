package com.aventura.cielo.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Alerta entre clases del mod, sin cambiar equipos ni el sistema vanilla de dano. */
public abstract class HostilCielo extends Monster {
    protected HostilCielo(EntityType<? extends Monster> type,Level level) { super(type,level); }
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        var target=getTarget();
        if(target!=null&&target.isAlive()&&tickCount%40==0) {
            for(HostilCielo ally:level.getEntitiesOfClass(HostilCielo.class,getBoundingBox().inflate(14)))
                if(ally.getTarget()==null&&ally.canAttack(target)&&ally.getSensing().hasLineOfSight(target)) ally.setTarget(target);
        }
        if(target!=null && ((this instanceof PirataDelCielo && tickCount%80<16) || (this instanceof GuardianCeleste && tickCount%80<50)))
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,getX(),getY()+1,getZ(),2,0.4,0.4,0.4,0);
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
        if(this instanceof PirataDelCielo && getTarget()!=null && tickCount%80<16
            && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && source.getSourcePosition()!=null
            && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            var direction=source.getSourcePosition().subtract(position()).multiply(1,0,1).normalize();
            if(getLookAngle().dot(direction)>0.4) { amount*=0.4F; playSound(SoundEvents.SHIELD_BLOCK.value(),1,1.3F); }
        }
        return super.hurtServer(level,source,amount);
    }
}
