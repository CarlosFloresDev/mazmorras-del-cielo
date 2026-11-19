package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ayudante: escudo frontal. Bombardero: mecha visible. Fatuo: parpadeo evasivo. */
public class EnemigoExpedicion extends Monster {
    private static final EntityDataAccessor<Integer> AVISO=SynchedEntityData.defineId(EnemigoExpedicion.class,EntityDataSerializers.INT);
    private int recarga=80;
    public EnemigoExpedicion(EntityType<? extends EnemigoExpedicion> t,Level l) { super(t,l); xpReward=12; }
    public int estilo() { return getType()==CieloEntities.AYUDANTE_COCINA?0:getType()==CieloEntities.BOMBARDERO_CENIZA?1:2; }
    public int aviso() { return entityData.get(AVISO); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(AVISO,0); }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,0.27)
            .add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,28).add(Attributes.ARMOR,3);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this)); goalSelector.addGoal(2,new MeleeAttackGoal(this,1,true));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,0.7)); goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,20));
        targetSelector.addGoal(1,new HurtByTargetGoal(this)); targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,false));
    }
    @Override protected void customServerAiStep(ServerLevel l) {
        super.customServerAiStep(l);
        if(aviso()>0) {
            entityData.set(AVISO,aviso()-1); getNavigation().stop();
            JefeExpedicion.anillo(l,position(),ParticleTypes.FLAME,4);
            if(aviso()==0) {
                for(Player p:l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(4))) {
                    p.hurtServer(l,damageSources().mobAttack(this),9); p.igniteForSeconds(4);
                }
                l.sendParticles(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),6,2,1,2,0);
                playSound(SoundEvents.GENERIC_EXPLODE.value(),1,1);
                // Muere con botin normal; no usa una explosion que destruya la mazmorra.
                hurtServer(l,damageSources().generic(),1000);
            }
        } else if(getTarget()!=null) {
            if(estilo()==1 && distanceTo(getTarget())<4) { entityData.set(AVISO,35); playSound(SoundEvents.CREEPER_PRIMED,1,1); }
            if(estilo()==2 && --recarga<=0) {
                recarga=85; l.sendParticles(ParticleTypes.PORTAL,getX(),getY()+1,getZ(),25,0.5,1,0.5,0.05);
                randomTeleport(getX()+random.nextInt(9)-4,getY()+1,getZ()+random.nextInt(9)-4,true, state -> state.isSolid());
            }
        }
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource s,float d) {
        if(estilo()==0 && s.is(DamageTypeTags.IS_PROJECTILE) && s.getDirectEntity()!=null) {
            Vec3 v=s.getDirectEntity().position().subtract(position()).multiply(1,0,1).normalize();
            if(getLookAngle().dot(v)>0.25) { playSound(SoundEvents.SHIELD_BLOCK.value(),1,1); return false; }
        }
        return super.hurtServer(l,s,d);
    }
}
