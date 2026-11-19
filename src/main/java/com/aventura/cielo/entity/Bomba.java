package com.aventura.cielo.entity;

import com.aventura.cielo.registry.CieloEntities;
import com.aventura.cielo.registry.CieloItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Bomba de los mecanicos (y del cañon del capitan): explota al impactar sin romper bloques. */
public class Bomba extends ThrowableItemProjectile {
	private float potencia = 1.5F;

	public Bomba(EntityType<? extends Bomba> type, Level level) {
		super(type, level);
	}

	public Bomba(Level level, LivingEntity owner, float potencia) {
		super(CieloEntities.BOMBA, owner, level, new ItemStack(CieloItems.BOMBA));
		this.potencia = potencia;
	}

	@Override
	protected Item getDefaultItem() {
		return CieloItems.BOMBA;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.2, this.getZ(), 0, 0.02, 0);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide()) {
			this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.potencia, Level.ExplosionInteraction.NONE);
			this.discard();
		}
	}
}
