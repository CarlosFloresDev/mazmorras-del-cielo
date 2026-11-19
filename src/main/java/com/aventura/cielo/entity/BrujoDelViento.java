package com.aventura.cielo.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Brujo del Viento: lanza cargas de viento que empujan (y te pueden tirar de los puentes). Fragil pero escurridizo:
 * no recibe dano por caida y se aleja si te acercas.
 */
public class BrujoDelViento extends HostilCielo implements RangedAttackMob {
	public BrujoDelViento(EntityType<? extends BrujoDelViento> type, Level level) {
		super(type, level);
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 40)
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			.add(Attributes.ARMOR, 5)
			.add(Attributes.FOLLOW_RANGE, 36.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new DistanciaCieloGoal(this,34));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel level)) return;
        // WindCharge vanilla solo hace 1 de impacto y su explosion no dana entidades.
        ProyectilExpedicion carga = new ProyectilExpedicion(level,this,target,true);
        Vec3 hacia=target.getBoundingBox().getCenter().subtract(carga.position());
        carga.shoot(hacia.x,hacia.y,hacia.z,0.65F,1);
        level.addFreshEntity(carga);
		this.level().playSound(null, this.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}
}
