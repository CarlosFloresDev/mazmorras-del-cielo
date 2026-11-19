package com.aventura.cielo.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Guardian Celeste: caballero con escudo de la Ciudadela. Lento y muy resistente; las flechas que vienen de frente
 * rebotan en su escudo (25 % del dano) y sus golpes empujan fuerte: cuidado en los puentes.
 */
public class GuardianCeleste extends HostilCielo {
	public GuardianCeleste(EntityType<? extends GuardianCeleste> type, Level level) {
		super(type, level);
		this.xpReward = 15;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 72)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ATTACK_DAMAGE, 12)
			.add(Attributes.ATTACK_KNOCKBACK, 1.6)
			.add(Attributes.ARMOR, 16)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new CombateCieloGoal(this, 1.1, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.04F);
		this.setDropChance(EquipmentSlot.OFFHAND, 0.08F);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
		groupData = super.finalizeSpawn(level, difficulty, reason, groupData);
		this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
		return groupData;
	}

	/** El escudo para lo que viene de frente (flechas, bolas de fuego, tridentes...). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity && tickCount % 80 < 50 && source.getSourcePosition() != null) {
			Vec3 hacia = source.getSourcePosition().subtract(this.position()).normalize();
			Vec3 mira = Vec3.directionFromRotation(0, this.yBodyRot);
			if (hacia.x * mira.x + hacia.z * mira.z > 0.3) {
				level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
				damage *= 0.25F;
			}
		}
		return super.hurtServer(level, source, damage);
	}
}
