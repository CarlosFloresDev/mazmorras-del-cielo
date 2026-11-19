package com.aventura.cielo.entity;

import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Melee normal de Mob (armadura, escudos y encantamientos incluidos), con reloj de entidad. */
public class CombateCieloGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private int nextAttack;
    public CombateCieloGoal(PathfinderMob mob,double speed,boolean follow) {
        this.mob=mob; this.speed=speed;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean canUse() {
        var t=mob.getTarget();
        return t!=null && t.isAlive() && !(t instanceof Player p && (p.isCreative()||p.isSpectator()));
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { mob.setAggressive(true); }
    @Override public void stop() { mob.getNavigation().stop(); mob.setAggressive(false); }
    @Override public void tick() {
        var t=mob.getTarget(); if(t==null) return;
        mob.getLookControl().setLookAt(t,30,30);
        if(mob.tickCount%5==0) {
            // Alterna el lado para rodear al jugador; el navegador conserva colisiones y suelo.
            double side=(mob.tickCount/40%2==0?1:-1)*1.1;
            var d=t.position().subtract(mob.position()).multiply(1,0,1).normalize();
            if(mob.distanceTo(t)<3 && mob.tickCount%40<12)
                mob.getNavigation().moveTo(t.getX()-d.z*side,t.getY(),t.getZ()+d.x*side,speed);
            else mob.getNavigation().moveTo(t,speed);
        }
        if(mob.tickCount>=nextAttack && mob.isWithinMeleeAttackRange(t) && mob.getSensing().hasLineOfSight(t)) {
            nextAttack=mob.tickCount+14;
            mob.swingForAttack(InteractionHand.MAIN_HAND);
            mob.doHurtTarget(getServerLevel(mob),t);
        }
    }
}
