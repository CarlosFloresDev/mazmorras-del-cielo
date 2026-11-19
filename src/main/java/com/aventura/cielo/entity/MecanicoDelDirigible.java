package com.aventura.cielo.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.Level;

/** Mecanico del Dirigible: pequeno y rapido, lanza bombas caseras que explotan sin romper bloques. */
public class MecanicoDelDirigible extends HostilCielo implements RangedAttackMob {
	public MecanicoDelDirigible(EntityType<? extends MecanicoDelDirigible> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 34)
			.add(Attributes.MOVEMENT_SPEED, 0.34)
			.add(Attributes.ARMOR, 5)
			.add(Attributes.FOLLOW_RANGE, 28.0)
			.add(Attributes.SCALE, 0.75);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(3, new DistanciaCieloGoal(this,38));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		Bomba bomba = new Bomba(level, this, 1.4F);
		double xd = target.getX() - this.getX();
		double yd = target.getEyeY() - 1.1 - bomba.getY();
		double zd = target.getZ() - this.getZ();
		double distance = Math.sqrt(xd * xd + zd * zd);
		bomba.shoot(xd, yd + distance * 0.25, zd, 1.0F, 6.0F);
		level.addFreshEntity(bomba);
		this.playSound(SoundEvents.TNT_PRIMED, 0.6F, 1.6F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_EXPLOSION)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}
}
