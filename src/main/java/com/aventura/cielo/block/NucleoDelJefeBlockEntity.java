package com.aventura.cielo.block;

import com.aventura.cielo.entity.CondorDeAcero;
import com.aventura.cielo.registry.CieloBlockEntities;
import com.aventura.cielo.registry.CieloEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NucleoDelJefeBlockEntity extends BlockEntity {
	public static final double RADIO = 14.0;

	public NucleoDelJefeBlockEntity(BlockPos pos, BlockState state) {
		super(CieloBlockEntities.NUCLEO_DEL_JEFE, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.getValue(NucleoDelJefeBlock.ACTIVO)) {
			return;
		}
		ServerPlayer retador = null;
		for (ServerPlayer p : level.players()) {
			if (!p.isSpectator() && !p.isCreative() && p.position().closerThan(net.minecraft.world.phys.Vec3.atCenterOf(pos), RADIO)
                && level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(), net.minecraft.world.phys.Vec3.atCenterOf(pos.above(2)),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p)).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
				retador = p;
				break;
			}
		}
		if (retador == null) {
			if (level.getGameTime() % 40 == 0) {
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
			}
			return;
		}
		NucleoDelJefeBlock.Jefe jefe = state.getValue(NucleoDelJefeBlock.JEFE);
        Mob mob = switch (jefe) {
            case CAPITAN -> CieloEntities.CAPITAN_BARBANUBE.create(level, EntitySpawnReason.STRUCTURE);
            case CONDOR -> CieloEntities.CONDOR_DE_ACERO.create(level, EntitySpawnReason.STRUCTURE);
            case JEAN -> CieloEntities.JEAN_BECK.create(level, EntitySpawnReason.STRUCTURE);
            case CENIZA -> CieloEntities.SENOR_DE_LA_CENIZA.create(level, EntitySpawnReason.STRUCTURE);
            case VACIO -> CieloEntities.HERALDO_DEL_VACIO.create(level, EntitySpawnReason.STRUCTURE);
        };
		if (mob == null) {
			return;
		}
		mob.snapTo(pos.getX() + 0.5, pos.getY() + (jefe == NucleoDelJefeBlock.Jefe.CONDOR ? 8 : 1), pos.getZ() + 0.5);
		if (mob instanceof CondorDeAcero condor) {
			condor.setAncla(pos.above());
		}
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		mob.setTarget(retador);
		if (!level.addFreshEntity(mob)) return;
		level.setBlock(pos, state.setValue(NucleoDelJefeBlock.ACTIVO, false), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, jefe == NucleoDelJefeBlock.Jefe.CONDOR ? 1.3F : 0.9F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        Component aviso = Component.translatable("message.cielo.jefe_despierta", mob.getDisplayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		for (ServerPlayer p : level.players()) {
			if (p.position().closerThan(net.minecraft.world.phys.Vec3.atCenterOf(pos), 64)) {
				p.sendSystemMessage(aviso);
			}
		}
	}
}
