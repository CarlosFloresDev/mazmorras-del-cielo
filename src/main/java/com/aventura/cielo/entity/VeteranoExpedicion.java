package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Seis tipos publicos con roles distintos. No depende del orden de inicializacion del registro. */
public class VeteranoExpedicion extends HostilCielo implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> AVISO=SynchedEntityData.defineId(VeteranoExpedicion.class,EntityDataSerializers.INT);
    private int cooldown=55;
    private Vec3 destino=Vec3.ZERO;
    public VeteranoExpedicion(EntityType<? extends VeteranoExpedicion> type,Level level) { super(type,level); xpReward=22; }
    public int rol() {
        if(getType()==CieloEntities.DUELISTA_COCINA) return 0;
        if(getType()==CieloEntities.MAESTRO_RACIONES) return 1;
        if(getType()==CieloEntities.GUARDIA_ESCORIA) return 2;
        if(getType()==CieloEntities.CANTOR_BRASAS) return 3;
        if(getType()==CieloEntities.ACECHADOR_VACIO) return 4;
        return 5;
    }
    public static AttributeSupplier.Builder atributos(double health,double damage,double armor,double speed) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.ATTACK_DAMAGE,damage)
            .add(Attributes.ARMOR,armor).add(Attributes.MOVEMENT_SPEED,speed).add(Attributes.FOLLOW_RANGE,36)
            .add(Attributes.KNOCKBACK_RESISTANCE,0.35);
    }
    public int aviso() { return entityData.get(AVISO); }
    public boolean escudoActivo() { return rol()==2&&getTarget()!=null&&tickCount%80<50; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(AVISO,0); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        // rol() se consulta al ejecutar, no durante el constructor de Mob.
        goalSelector.addGoal(2,new DistanciaCieloGoal(this,34) {
            @Override public boolean canUse() { return rol()%2==1&&aviso()==0&&super.canUse(); }
            @Override public boolean canContinueToUse() { return canUse(); }
        });
        goalSelector.addGoal(2,new CombateCieloGoal(this,1.15,true) {
            @Override public boolean canUse() { return rol()%2==0&&aviso()==0&&super.canUse(); }
        });
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,0.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,20));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        var target=getTarget();
        if(target==null||!target.isAlive()) { entityData.set(AVISO,0); return; }
        if(escudoActivo()) level.sendParticles(ParticleTypes.SMOKE,getX(),getY()+1.3,getZ(),2,0.45,0.4,0.45,0);
        if(aviso()>0) {
            getNavigation().stop();
            JefeExpedicion.anillo(level,destino,rol()<2?ParticleTypes.CRIT:rol()<4?ParticleTypes.FLAME:ParticleTypes.PORTAL,2.5);
            entityData.set(AVISO,aviso()-1);
            if(aviso()==0) especial(level,target);
        } else if(--cooldown<=0 && getSensing().hasLineOfSight(target)) {
            cooldown=rol()==1?150:110;
            destino=rol()==1||rol()==3?position():target.position();
            entityData.set(AVISO,24);
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK,1,rol()<2?1.4F:0.8F);
        }
    }
    private void especial(ServerLevel level,LivingEntity target) {
        switch(rol()) {
            case 0 -> { // Salto al punto anunciado: se puede esquivar moviendose de ese punto.
                if(distanceTo(target)<8&&onGround()) {
                    Vec3 d=destino.subtract(position()).multiply(1,0,1).normalize();
                    setDeltaMovement(d.x*0.85,0.48,d.z*0.85);
                }
            }
            case 1 -> {
                for(HostilCielo ally:level.getEntitiesOfClass(HostilCielo.class,getBoundingBox().inflate(7))) {
                    if(ally==this||!getSensing().hasLineOfSight(ally)) continue;
                    ally.heal(8);
                    level.sendParticles(ParticleTypes.HEART,ally.getX(),ally.getEyeY()+0.3,ally.getZ(),4,0.3,0.2,0.3,0);
                }
            }
            case 2 -> {
                if(isWithinMeleeAttackRange(target)&&getSensing().hasLineOfSight(target)) {
                    if(doHurtTarget(level,target)) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40,0));
                }
            }
            case 3 -> {
                for(HostilCielo ally:level.getEntitiesOfClass(HostilCielo.class,getBoundingBox().inflate(7))) {
                    if(!getSensing().hasLineOfSight(ally)) continue;
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH,100,0));
                    ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,120,0));
                    level.sendParticles(ParticleTypes.FLAME,ally.getX(),ally.getY()+1,ally.getZ(),8,0.5,0.5,0.5,0);
                }
            }
            case 4 -> {
                if(distanceTo(target)<12) {
                    var back=target.getLookAngle().multiply(1,0,1).normalize().scale(-2);
                    var p=destino.add(back);
                    // Solo se salta a una posicion visible para no atravesar sellos/paredes.
                    if(level.clip(new net.minecraft.world.level.ClipContext(getEyePosition(),p.add(0,1,0),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this)).getType()==net.minecraft.world.phys.HitResult.Type.MISS)
                        randomTeleport(p.x,p.y,p.z,true,state -> state.isSolid());
                    playSound(SoundEvents.ENDERMAN_TELEPORT,1,1.2F);
                }
            }
            case 5 -> {
                for(Player p:level.getEntitiesOfClass(Player.class,new net.minecraft.world.phys.AABB(destino,destino).inflate(2.5))) {
                    if(!p.isCreative()&&!p.isSpectator()&&getSensing().hasLineOfSight(p)
                        &&p.hurtServer(level,damageSources().mobAttack(this),6))
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,1));
                }
            }
            default -> throw new IllegalStateException();
        }
    }
    @Override public void performRangedAttack(LivingEntity target,float power) {
        if(!(level() instanceof ServerLevel level)) return;
        var p=new ProyectilExpedicion(level,this,target,rol()==5);
        Vec3 d=target.getBoundingBox().getCenter().subtract(p.position());
        p.shoot(d.x,d.y+d.horizontalDistance()*0.1,d.z,rol()==5?0.55F:1.1F,1);
        level.addFreshEntity(p); playSound(SoundEvents.SNOWBALL_THROW,1,0.8F);
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage) {
        if(escudoActivo()&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&source.getEntity() instanceof LivingEntity
            &&source.getSourcePosition()!=null) {
            Vec3 d=source.getSourcePosition().subtract(position()).multiply(1,0,1).normalize();
            if(getLookAngle().dot(d)>0.25) { playSound(SoundEvents.SHIELD_BLOCK.value(),1,0.7F); damage*=0.15F; }
        }
        return super.hurtServer(level,source,damage);
    }
}
