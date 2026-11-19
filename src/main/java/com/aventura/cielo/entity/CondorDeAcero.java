package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Jefe final: el Condor de Acero vuela en circulos sobre su fortaleza y alterna tres ataques:
 * <ul>
 *   <li>Picada: chilla y apunta (aviso), luego se lanza en picada. Si choca queda ATURDIDO 3 s en el piso
 *       y recibe el doble de dano: esa es la ventana para pegarle cuerpo a cuerpo.</li>
 *   <li>Lluvia de plumas: abanico de 7 plumas de acero (flechas); en fase 2 dispara dos rafagas.</li>
 *   <li>Rafaga de viento: aviso con nubes y luego empuja a todos lejos... cuidado con caerse de la isla.</li>
 * </ul>
 * Mientras vuela las flechas le hacen la mitad de dano. Bajo 50% de vida llama arcabuceros y ataca mas seguido.
 */
public class CondorDeAcero extends HostilCielo {
	public static final int ORBITAR = 0;
	public static final int PREPARAR_PICADA = 1;
	public static final int PICADA = 2;
	public static final int ATURDIDO = 3;
	public static final int PLUMAS = 4;
	public static final int VIENTO = 5;
	private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(CondorDeAcero.class, EntityDataSerializers.INT);

	private final ServerBossEvent barra = new ServerBossEvent(Mth.createInsecureUUID(this.random),
		Component.literal("Cóndor de Acero").withStyle(ChatFormatting.AQUA), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_12);
	private BlockPos ancla;
	private float angulo;
	private int estadoTicks;
	private int espera = 60;
	private Vec3 objetivo = Vec3.ZERO;
	private boolean refuerzos;
	/** Cuantos ataques especiales lleva (lo usan las pruebas). */
	public int ataquesRealizados;

	public CondorDeAcero(EntityType<? extends CondorDeAcero> type, Level level) {
		super(type, level);
		this.xpReward = 400;
		this.setNoGravity(true);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 620)
			.add(Attributes.MOVEMENT_SPEED, 0.33)
			.add(Attributes.ATTACK_DAMAGE, 18)
			.add(Attributes.ARMOR, 16)
			.add(Attributes.ARMOR_TOUGHNESS, 6.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ESTADO, ORBITAR);
	}

	public int estado() {
		return this.entityData.get(ESTADO);
	}

	private void setEstado(int estado, int ticks) {
		this.entityData.set(ESTADO, estado);
		this.estadoTicks = ticks;
	}

	public void setAncla(BlockPos ancla) {
		this.ancla = ancla;
	}

	@Override
	protected void registerGoals() {
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	public int fase() {
		return this.getHealth() / this.getMaxHealth() > 0.5F ? 1 : 2;
	}

	/** Vuela hacia un punto con velocidad maxima {@code vel}. */
	private void volar(Vec3 punto, double vel) {
		Vec3 d = punto.subtract(this.position());
		double dist = d.length();
		if (dist > 0.01) {
			Vec3 v = d.scale(Math.min(vel, dist * 0.2) / dist);
			this.setDeltaMovement(v);
			this.setYRot((float) (Mth.atan2(v.z, v.x) * (180 / Math.PI)) - 90.0F);
			this.yBodyRot = this.getYRot();
		}
	}

	private void mirar(LivingEntity target) {
		double dx = target.getX() - this.getX();
		double dz = target.getZ() - this.getZ();
		this.setYRot((float) (Mth.atan2(dz, dx) * (180 / Math.PI)) - 90.0F);
		this.yBodyRot = this.getYRot();
		this.yHeadRot = this.getYRot();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.ancla == null) {
			this.ancla = this.blockPosition();
		}
		this.barra.setProgress(this.getHealth() / this.getMaxHealth());
		LivingEntity target = this.getTarget();
		int fase = this.fase();
		Vec3 centro = Vec3.atBottomCenterOf(this.ancla);

		// si lo tiraron fuera de la isla, vuelve
		if (this.getY() < this.ancla.getY() - 25) {
			this.snapTo(centro.x, centro.y + 10, centro.z);
			this.setNoGravity(true);
			this.setEstado(ORBITAR, 0);
		}
		if (fase == 2 && !this.refuerzos) {
			this.refuerzos = true;
			this.anunciar(level, "¡KRRAAAA! (el cóndor llama a los arcabuceros)");
			for (int i = 0; i < 2; i++) {
				ArcabuceroDelCielo a = CieloEntities.ARCABUCERO_DEL_CIELO.create(level, EntitySpawnReason.REINFORCEMENT);
				if (a != null) {
					a.snapTo(centro.x + (i == 0 ? 6 : -6), centro.y + 1, centro.z);
					a.finalizeSpawn(level, level.getCurrentDifficultyAt(this.ancla), EntitySpawnReason.REINFORCEMENT, null);
					level.addFreshEntity(a);
				}
			}
		}

		this.estadoTicks--;
		switch (this.estado()) {
			case ORBITAR -> {
				this.angulo += fase == 2 ? 0.042F : 0.03F;
				Vec3 punto = centro.add(Math.cos(this.angulo) * 13, 10 + Math.sin(this.tickCount * 0.05) * 1.5, Math.sin(this.angulo) * 13);
				this.volar(punto, 0.55);
				if (--this.espera <= 0 && target != null && target.distanceToSqr(centro) < 40 * 40) {
					this.ataquesRealizados++;
					float r = this.random.nextFloat();
					if (r < 0.45F) {
						this.setEstado(PREPARAR_PICADA, 30);
						this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.6F);
					} else if (r < 0.75F) {
						this.setEstado(PLUMAS, 22);
					} else {
						this.setEstado(VIENTO, 35);
						this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 3.0F, 0.6F);
					}
					this.espera = fase == 2 ? 30 : 55;
				}
			}
			case PREPARAR_PICADA -> {
				this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
				if (target != null) {
					this.mirar(target);
					if (this.estadoTicks > 8) {
						this.objetivo = target.position();
					}
				}
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1, this.getZ(), 4, 1.5, 0.5, 1.5, 0.05);
				if (this.estadoTicks <= 0) {
					this.setEstado(PICADA, 45);
					this.playSound(SoundEvents.PHANTOM_SWOOP, 2.0F, 0.6F);
				}
			}
			case PICADA -> {
				Vec3 d = this.objetivo.subtract(this.position());
				this.setDeltaMovement(d.normalize().scale(1.3));
				for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.5))) {
					p.hurtServer(level, this.damageSources().mobAttack(this), 14.0F);
				}
				if (d.length() < 1.6 || this.horizontalCollision || this.verticalCollision || this.estadoTicks <= 0) {
					this.impacto(level);
				}
			}
			case ATURDIDO -> {
				this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 1.0, 0.5));
				if (this.tickCount % 5 == 0) {
					level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.2, this.getZ(), 6, 0.8, 0.2, 0.8, 0.1);
				}
				if (this.estadoTicks <= 0) {
					this.setNoGravity(true);
					this.setDeltaMovement(0, 0.8, 0);
					this.setEstado(ORBITAR, 0);
					this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 3.0F, 0.8F);
				}
			}
			case PLUMAS -> {
				this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
				if (target != null) {
					this.mirar(target);
					if (this.estadoTicks == 12 || (fase == 2 && this.estadoTicks == 4)) {
						this.plumas(level, target);
					}
				}
				if (this.estadoTicks <= 0) {
					this.setEstado(ORBITAR, 0);
				}
			}
			case VIENTO -> {
				this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
				if (this.estadoTicks > 10) {
					level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 10, 4, 1, 4, 0.05);
				} else if (this.estadoTicks == 10) {
					for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(13.0))) {
						Vec3 away = p.position().subtract(this.position()).multiply(1, 0, 1).normalize();
						p.push(away.x * 2.0, 0.45, away.z * 2.0);
						p.needsSync = true;
						p.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
					}
					level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
					this.playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 2.5F, 0.6F);
				} else if (this.estadoTicks <= 0) {
					this.setEstado(ORBITAR, 0);
				}
			}
			default -> this.setEstado(ORBITAR, 0);
		}
	}

	private void impacto(ServerLevel level) {
		for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3.5))) {
			p.hurtServer(level, this.damageSources().mobAttack(this), 12.0F);
			Vec3 away = p.position().subtract(this.position()).normalize();
			p.push(away.x * 1.2, 0.5, away.z * 1.2);
			p.needsSync = true;
		}
		level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 6, 1.5, 0.3, 1.5, 0);
		this.playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.6F);
		this.setNoGravity(false);
		this.setEstado(ATURDIDO, 60);
	}

	private void plumas(ServerLevel level, LivingEntity target) {
		Vec3 base = target.getEyePosition().subtract(this.getEyePosition());
		for (int i = -3; i <= 3; i++) {
			double ang = Math.toRadians(i * 9);
			Vec3 dir = new Vec3(base.x * Math.cos(ang) - base.z * Math.sin(ang), base.y, base.x * Math.sin(ang) + base.z * Math.cos(ang));
			Arrow pluma = new Arrow(level, this, new ItemStack(Items.ARROW), null);
			pluma.pickup = AbstractArrow.Pickup.DISALLOWED;
			pluma.setBaseDamage(4.0);
			pluma.shoot(dir.x, dir.y, dir.z, 1.6F, 2.0F);
			level.addFreshEntity(pluma);
		}
		this.playSound(SoundEvents.ARROW_SHOOT, 2.0F, 0.7F);
	}

	private void anunciar(ServerLevel level, String texto) {
		for (ServerPlayer p : level.players()) {
			if (p.distanceTo(this) < 64) {
				p.sendSystemMessage(Component.literal(texto).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_DROWNING)) {
			return false;
		}
		if (this.estado() == ATURDIDO) {
			damage *= 2.0F;
		} else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
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
		if (this.ancla != null) {
			output.putInt("AnclaX", this.ancla.getX());
			output.putInt("AnclaY", this.ancla.getY());
			output.putInt("AnclaZ", this.ancla.getZ());
		}
		output.putBoolean("Refuerzos", this.refuerzos);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		if (input.getInt("AnclaY").isPresent()) {
			this.ancla = new BlockPos(input.getIntOr("AnclaX", 0), input.getIntOr("AnclaY", 0), input.getIntOr("AnclaZ", 0));
		}
		this.refuerzos = input.getBooleanOr("Refuerzos", false);
	}
}
