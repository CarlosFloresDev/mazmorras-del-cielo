package com.aventura.cielo.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/** Arcabucero del Cielo: tirador con arco que cubre a los piratas desde las alas y las torres. */
public class ArcabuceroDelCielo extends HostilCielo implements RangedAttackMob {
	public ArcabuceroDelCielo(EntityType<? extends ArcabuceroDelCielo> type, Level level) {
		super(type, level);
		this.xpReward = 8;
		// Tambien al cargar NBT configurado: BaseSpawner puede omitir finalizeSpawn.
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 36)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ARMOR, 5)
			.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(3, new DistanciaCieloGoal(this,26));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
		@Nullable SpawnGroupData groupData) {
		groupData = super.finalizeSpawn(level, difficulty, reason, groupData);
		this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
		return groupData;
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		ItemStack bow = this.getMainHandItem().isEmpty() ? new ItemStack(Items.BOW) : this.getMainHandItem();
		ItemStack projectile = this.getProjectile(bow);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectile, power, bow);
		arrow.setBaseDamage(4.0);
		double xd = target.getX() - this.getX();
		double yd = target.getY(1.0 / 3.0) - arrow.getY();
		double zd = target.getZ() - this.getZ();
		double distance = Math.sqrt(xd * xd + zd * zd);
		if (this.level() instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileUsingShoot(arrow, serverLevel, projectile, xd, yd + distance * 0.2, zd, 1.7F, 8.0F);
		}
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}
}
