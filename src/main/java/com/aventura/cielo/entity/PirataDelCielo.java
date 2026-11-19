package com.aventura.cielo.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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

/**
 * Pirata del Cielo: espadachin de las naves voladoras. Usa paracaidas (no recibe dano por caida),
 * avisa a sus companeros cuando lo atacan y es mas fuerte que un zombi.
 */
public class PirataDelCielo extends HostilCielo {
	public PirataDelCielo(EntityType<? extends PirataDelCielo> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 44)
			.add(Attributes.MOVEMENT_SPEED, 0.31)
			.add(Attributes.ATTACK_DAMAGE, 9)
			.add(Attributes.ARMOR, 6)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new CombateCieloGoal(this, 1.2, false));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextFloat() < 0.2F ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
	}

	@Override
	public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty,
		net.minecraft.world.entity.EntitySpawnReason reason, net.minecraft.world.entity.SpawnGroupData groupData) {
		groupData = super.finalizeSpawn(level, difficulty, reason, groupData);
		this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
		return groupData;
	}

	/** Los piratas del cielo llevan paracaidas. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}
}
