package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Ataques con aviso de 30 ticks. Los temporizadores usan ticks de entidad, nunca la hora del mundo. */
public abstract class JefeExpedicion extends Monster {
    private static final EntityDataAccessor<Integer> AVISO = SynchedEntityData.defineId(JefeExpedicion.class,EntityDataSerializers.INT);
    private final ServerBossEvent barra = new ServerBossEvent(Mth.createInsecureUUID(random),getType().getDescription(),
        BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.NOTCHED_10);
    private int espera=70, ataque, carga, saltoTicks;
    private boolean refuerzos;
    private boolean eco;
    private Vec3 destino=Vec3.ZERO;
    private Vec3 direccion=Vec3.ZERO;
    private final List<Zona> zonas=new ArrayList<>();
    private static final class Zona {
        final Vec3 centro; int ticks; final boolean fuego;
        Zona(Vec3 c,int t,boolean f) { centro=c; ticks=t; fuego=f; }
    }
    protected JefeExpedicion(EntityType<? extends JefeExpedicion> type,Level level) {
        super(type,level); xpReward=300; setPersistenceRequired();
    }
    protected abstract int estilo(); // Jean, Ceniza, Vacio
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,420).add(Attributes.MOVEMENT_SPEED,0.26)
            .add(Attributes.ATTACK_DAMAGE,10).add(Attributes.ARMOR,8).add(Attributes.KNOCKBACK_RESISTANCE,0.95)
            .add(Attributes.FOLLOW_RANGE,40);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(AVISO,0); }
    public int aviso() { return entityData.get(AVISO); }
    public int fase() { return getHealth()>getMaxHealth()*0.66F?1:getHealth()>getMaxHealth()*0.33F?2:3; }
    public ServerBossEvent barra() { return barra; }
    public void convertirEnEco() {
        eco=true; refuerzos=true; xpReward=0;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(6); setHealth(6);
        setCustomName(Component.translatable("entity.cielo.eco_del_vacio"));
    }
    public boolean escudoActivo() { return estilo()==1 && fase()==3 && tickCount%120<75; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,true) {
            @Override public boolean canUse() { return aviso()==0 && carga==0 && saltoTicks==0 && super.canUse(); }
            @Override public boolean canContinueToUse() { return aviso()==0 && carga==0 && saltoTicks==0 && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,32));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,false));
    }
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if(eco) {
            if(tickCount>300) { discard(); return; }
            if(getTarget()!=null && tickCount%70==0) proyectil(level,getTarget(),true);
            return;
        }
        barra.setProgress(getHealth()/getMaxHealth());
        barra.setColor(escudoActivo()?BossEvent.BossBarColor.RED:BossEvent.BossBarColor.PURPLE);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(fase()==3?0.34:0.26);
        LivingEntity target=getTarget();
        if(target!=null && fase()>=(estilo()==1?2:3) && !refuerzos) { refuerzos=true; invocar(level,target); }
        if(escudoActivo()) level.sendParticles(ParticleTypes.FLAME,getX(),getY()+1.5,getZ(),6,1.4,1.2,1.4,0.02);
        if(estilo()==1 && fase()==3 && tickCount%120==75) {
            playSound(SoundEvents.BEACON_DEACTIVATE,1.5F,1.2F);
            anillo(level,position(),ParticleTypes.SMOKE,2.5);
        }
        if(estilo()==1 && target!=null && tickCount%20==0) zonas.add(new Zona(position(),65,true));
        for(var it=zonas.iterator();it.hasNext();) {
            Zona z=it.next();
            if(--z.ticks<=0) { it.remove(); continue; }
            if(z.ticks%5==0) {
                anillo(level,z.centro,z.fuego?ParticleTypes.FLAME:ParticleTypes.PORTAL,2.5);
                for(Player p:level.getEntitiesOfClass(Player.class,new AABB(z.centro,z.centro).inflate(2.5,2,2.5))) {
                    if(p.isCreative()||p.isSpectator()) continue;
                    if(z.fuego) { p.igniteForSeconds(3); p.hurtServer(level,damageSources().mobAttack(this),3); }
                    else p.addEffect(new MobEffectInstance(MobEffects.LEVITATION,30,0));
                }
            }
        }
        if(carga>0) {
            carga--; getNavigation().stop(); setDeltaMovement(direccion.x*1.3,getDeltaMovement().y,direccion.z*1.3);
            golpear(level,2.3F,9,true,false); return;
        }
        if(saltoTicks>0) {
            saltoTicks--;
            if(saltoTicks==0 || (saltoTicks<17 && onGround())) {
                saltoTicks=0; golpear(level,7,14,false,true);
                anillo(level,position(),ParticleTypes.EXPLOSION,7);
                playSound(SoundEvents.GENERIC_EXPLODE.value(),1.4F,0.6F);
            }
            return;
        }
        if(aviso()>0) {
            getNavigation().stop(); setDeltaMovement(0,getDeltaMovement().y,0);
            entityData.set(AVISO,aviso()-1);
            anillo(level,destino,estilo()==0?ParticleTypes.CRIT:estilo()==1?ParticleTypes.FLAME:ParticleTypes.PORTAL,3);
            if(aviso()==0 && target!=null && target.isAlive()) ejecutar(level,target);
            return;
        }
        if(target==null || !target.isAlive()) return;
        if(--espera<=0) {
            destino=target.position(); entityData.set(AVISO,30); ataque++;
            espera=fase()==3?60:95;
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK,1.4F,estilo()==0?0.6F:1.1F);
        }
    }
    private void ejecutar(ServerLevel level,LivingEntity target) {
        if(estilo()==0) {
            if(fase()>=2 && ataque%3==0) proyectil(level,target,false);
            else if(ataque%2==0) { setDeltaMovement(0,0.8,0); saltoTicks=22; }
            else { direccion=destino.subtract(position()).multiply(1,0,1).normalize(); carga=14; }
        } else if(estilo()==1) {
            zonas.add(new Zona(destino,85,true));
            level.sendParticles(ParticleTypes.LAVA,destino.x,destino.y,destino.z,30,2,0.5,2,0.1);
        } else {
            proyectil(level,target,true);
            zonas.add(new Zona(destino,100,false));
            // randomTeleport verifica suelo y colisiones; no atraviesa paredes de la arena.
            for(int i=0;i<6;i++) if(randomTeleport(getX()+random.nextInt(15)-7,getY()+1,getZ()+random.nextInt(15)-7,true, state -> state.isSolid())) break;
            playSound(SoundEvents.ENDERMAN_TELEPORT,1,0.8F);
        }
    }
    private void proyectil(ServerLevel level,LivingEntity target,boolean vacio) {
        ProyectilExpedicion p=new ProyectilExpedicion(level,this,target,vacio);
        Vec3 d=target.getEyePosition().subtract(p.position()); p.shoot(d.x,d.y,d.z,vacio?0.55F:0.85F,2);
        level.addFreshEntity(p); playSound(SoundEvents.SNOWBALL_THROW,1,0.7F);
    }
    private void golpear(ServerLevel level,float r,float damage,boolean empuje,boolean suelo) {
        for(Player p:level.getEntitiesOfClass(Player.class,getBoundingBox().inflate(r,2,r))) {
            if(p.isCreative()||p.isSpectator()||distanceTo(p)>r+1 || (suelo&&!p.onGround())) continue;
            if(p.hurtServer(level,damageSources().mobAttack(this),damage)) {
                Vec3 d=p.position().subtract(position()).multiply(1,0,1).normalize();
                p.push(d.x*(empuje?2.8:0.6),empuje?0.5:0.3,d.z*(empuje?2.8:0.6)); p.needsSync=true;
                if(suelo) p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,70,1));
            }
        }
    }
    private void invocar(ServerLevel level,LivingEntity target) {
        for(int i=0;i<3;i++) {
            Mob m=(estilo()==0?CieloEntities.AYUDANTE_COCINA:estilo()==1?net.minecraft.world.entity.EntityTypes.BLAZE:CieloEntities.HERALDO_DEL_VACIO)
                .create(level,EntitySpawnReason.REINFORCEMENT);
            if(m==null) continue;
            m.snapTo(getX()+Math.cos(i*2.1)*4,getY()+0.5,getZ()+Math.sin(i*2.1)*4);
            if(!level.noCollision(m)) continue;
            m.finalizeSpawn(level,level.getCurrentDifficultyAt(blockPosition()),EntitySpawnReason.REINFORCEMENT,null);
            if(m instanceof JefeExpedicion j) j.convertirEnEco();
            m.setTarget(target); level.addFreshEntity(m);
        }
    }
    static void anillo(ServerLevel l,Vec3 p,net.minecraft.core.particles.SimpleParticleType particle,double r) {
        for(int i=0;i<18;i++) { double a=i*Math.PI/9; l.sendParticles(particle,p.x+Math.cos(a)*r,p.y+0.15,p.z+Math.sin(a)*r,1,0,0,0,0); }
    }
    @Override public boolean hurtServer(ServerLevel l,DamageSource s,float d) {
        if(s.is(DamageTypeTags.IS_FALL)||s.is(DamageTypes.IN_WALL)) return false;
        if(escudoActivo()&&!s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { playSound(SoundEvents.SHIELD_BLOCK.value(),1,0.6F); return false; }
        return super.hurtServer(l,s,d);
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); if(!eco) barra.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); barra.removePlayer(p); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDropLoot(ServerLevel l) { return !eco && super.shouldDropLoot(l); }
    @Override public void remove(Entity.RemovalReason reason) { barra.removeAllPlayers(); super.remove(reason); }
    @Override protected void addAdditionalSaveData(ValueOutput o) {
        super.addAdditionalSaveData(o); o.putBoolean("Refuerzos",refuerzos); o.putBoolean("Eco",eco);
    }
    @Override protected void readAdditionalSaveData(ValueInput i) {
        super.readAdditionalSaveData(i); refuerzos=i.getBooleanOr("Refuerzos",false); eco=i.getBooleanOr("Eco",false); if(eco) xpReward=0;
    }
}
