package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Jefe del Dirigible Pirata. Tres fases:
 * <ol>
 *   <li>Espadazos y "Salto Aplastante": levanta el sable (aviso de 1 s), salta hacia ti y al caer lanza una onda de choque.</li>
 *   <li>Bajo 66%: llama refuerzos una vez y dispara cañonazos (bombas) a los que se alejan.</li>
 *   <li>Bajo 33%: furia. Mas rapido, "Torbellino" giratorio que daña alrededor y las flechas le hacen la mitad.</li>
 * </ol>
 */
public class CapitanBarbanube extends HostilCielo {
	public static final int NORMAL = 0;
	public static final int PREPARANDO_SALTO = 1;
	public static final int TORBELLINO = 2;
	private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(CapitanBarbanube.class, EntityDataSerializers.INT);

	private final ServerBossEvent barra = new ServerBossEvent(Mth.createInsecureUUID(this.random),
		Component.literal("Capitán Barbanube").withStyle(ChatFormatting.GOLD), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	private int estadoTicks;
	private int esperaSalto = 60;
	private int esperaCanon = 45;
	private int esperaGiro = 75;
	private boolean saltando;
	private boolean refuerzos;
	private boolean furia;

	public CapitanBarbanube(EntityType<? extends CapitanBarbanube> type, Level level) {
		super(type, level);
		this.xpReward = 200;
		this.setPersistenceRequired();
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 480)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 15)
			.add(Attributes.ARMOR, 14)
			.add(Attributes.ARMOR_TOUGHNESS, 4.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.SCALE, 1.5);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ESTADO, NORMAL);
	}

	public int estado() {
		return this.entityData.get(ESTADO);
	}

	private void setEstado(int estado, int ticks) {
		this.entityData.set(ESTADO, estado);
		this.estadoTicks = ticks;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new CombateCieloGoal(this, 1.1, true));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	/** 1, 2 o 3 segun la vida restante. */
	public int fase() {
		float r = this.getHealth() / this.getMaxHealth();
		return r > 0.66F ? 1 : r > 0.33F ? 2 : 3;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.barra.setProgress(this.getHealth() / this.getMaxHealth());
		LivingEntity target = this.getTarget();
		int fase = this.fase();

		if (fase == 3 && !this.furia) {
			this.furia = true;
			this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
			this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20.0);
			this.anunciar(level, "¡¡NADIE ABORDA MI DIRIGIBLE!! (el capitán entra en furia)");
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 3, this.getZ(), 12, 1, 0.5, 1, 0);
		}
		if (fase >= 2 && !this.refuerzos) {
			this.refuerzos = true;
			this.anunciar(level, "¡A mí, tripulación!");
			for (int i = 0; i < 3; i++) {
				PirataDelCielo pirata = CieloEntities.PIRATA_DEL_CIELO.create(level, EntitySpawnReason.REINFORCEMENT);
				if (pirata != null) {
					double a = i * Math.PI * 2 / 3;
					pirata.snapTo(this.getX() + Math.cos(a) * 3, this.getY() + 0.5, this.getZ() + Math.sin(a) * 3);
					pirata.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), EntitySpawnReason.REINFORCEMENT, null);
					pirata.setTarget(target);
					level.addFreshEntity(pirata);
				}
			}
		}

		// ---------------------------------------------------- estados en curso
		if (this.estado() == PREPARANDO_SALTO) {
			this.getNavigation().stop();
			if (--this.estadoTicks <= 0) {
				this.setEstado(NORMAL, 0);
				if (target != null) {
					Vec3 dir = target.position().subtract(this.position()).normalize();
					this.setDeltaMovement(dir.x * 1.3, 0.95, dir.z * 1.3);
					this.saltando = true;
					this.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.2F);
				}
			}
		} else if (this.estado() == TORBELLINO) {
			this.getNavigation().stop();
			this.setYRot(this.getYRot() + 40);
			if (this.estadoTicks % 6 == 0) {
				for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3.5))) {
					p.hurtServer(level, this.damageSources().mobAttack(this), 5.0F);
					p.push((p.getX() - this.getX()) * 0.15, 0.1, (p.getZ() - this.getZ()) * 0.15);
					p.needsSync = true;
				}
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.5, this.getZ(), 6, 2, 0.3, 2, 0);
				this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
			}
			if (--this.estadoTicks <= 0) {
				this.setEstado(NORMAL, 0);
			}
		}
		if (this.saltando && this.onGround() && this.getDeltaMovement().y <= 0.01) {
			this.saltando = false;
			this.ondaDeChoque(level, fase);
		}

		// ---------------------------------------------------- elegir ataques
		this.esperaSalto--;
		this.esperaCanon--;
		this.esperaGiro--;
		if (target == null || this.estado() != NORMAL || this.saltando) {
			return;
		}
		double dist = this.distanceTo(target);
		if (fase == 3 && this.esperaGiro <= 0 && dist < 5) {
			this.setEstado(TORBELLINO, 40);
			this.esperaGiro = 120;
			this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.5F, 0.7F);
		} else if (this.esperaSalto <= 0 && dist > 3.5 && dist < 18) {
			this.setEstado(PREPARANDO_SALTO, 20);
			this.esperaSalto = 170 - fase * 35;
			this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.6F);
		} else if (fase >= 2 && this.esperaCanon <= 0 && dist > 6) {
			this.esperaCanon = fase == 3 ? 50 : 80;
			for (int i = 0; i < (fase == 3 ? 3 : 2); i++) {
				Bomba bomba = new Bomba(level, this, 2.0F);
				double xd = target.getX() - this.getX();
				double yd = target.getEyeY() - 1.0 - bomba.getY();
				double zd = target.getZ() - this.getZ();
				bomba.shoot(xd, yd + Math.sqrt(xd * xd + zd * zd) * 0.25, zd, 1.1F, 10.0F);
				level.addFreshEntity(bomba);
			}
			this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.8F);
		}
	}

	private void ondaDeChoque(ServerLevel level, int fase) {
		float dano = 8.0F + fase * 2.0F;
		for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(5.0, 2.0, 5.0))) {
			p.hurtServer(level, this.damageSources().mobAttack(this), dano);
			Vec3 away = p.position().subtract(this.position()).normalize();
			p.push(away.x * 1.4, 0.6, away.z * 1.4);
			p.needsSync = true;
		}
		level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2, this.getZ(), 8, 2.5, 0.1, 2.5, 0);
		this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.7F);
	}

	private void anunciar(ServerLevel level, String texto) {
		for (ServerPlayer p : level.players()) {
			if (p.distanceTo(this) < 48) {
				p.sendSystemMessage(Component.literal("Capitán Barbanube: ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
					.append(Component.literal(texto).withStyle(ChatFormatting.YELLOW)));
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_EXPLOSION)) {
			return false;
		}
		if (this.fase() == 3 && source.is(DamageTypeTags.IS_PROJECTILE)) {
			damage *= 0.5F;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.barra.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.barra.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Refuerzos", this.refuerzos);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.refuerzos = input.getBooleanOr("Refuerzos", false);
	}
}
