package com.aventura.cielo.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;

/** Retirada con suelo seguro, tiro con linea de vision y aviso de ocho ticks. */
public class DistanciaCieloGoal extends Goal {
    private final PathfinderMob mob;
    private final int interval;
    private int cooldown=12;
    public DistanciaCieloGoal(PathfinderMob mob,int interval) {
        this.mob=mob; this.interval=interval;
        if(!(mob instanceof RangedAttackMob)) throw new IllegalArgumentException("Falta ataque a distancia");
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean canUse() {
        var t=mob.getTarget();
        return t!=null&&t.isAlive()&&!(t instanceof Player p&&(p.isCreative()||p.isSpectator()));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void stop() { mob.getNavigation().stop(); }
    @Override public void tick() {
        var t=mob.getTarget(); if(t==null) return;
        mob.getLookControl().setLookAt(t,30,30);
        double distance=mob.distanceTo(t);
        if(mob.tickCount%8==0) {
            if(distance<5) {
                var away=mob.position().subtract(t.position()).multiply(1,0,1).normalize().scale(4);
                var pos=mob.position().add(away);
                if(mob.level().getBlockState(BlockPos.containing(pos).below()).isSolid())
                    mob.getNavigation().moveTo(pos.x,pos.y,pos.z,1.25);
            } else if(distance>13||!mob.getSensing().hasLineOfSight(t)) mob.getNavigation().moveTo(t,1.1);
            else mob.getNavigation().stop();
        }
        if(--cooldown<=8 && cooldown>0) getServerLevel(mob).sendParticles(ParticleTypes.CRIT,mob.getX(),mob.getEyeY(),mob.getZ(),1,0.15,0.15,0.15,0);
        if(cooldown<=0 && distance<22 && mob.getSensing().hasLineOfSight(t)) {
            ((RangedAttackMob)mob).performRangedAttack(t,1);
            cooldown=interval;
        }
    }
}
