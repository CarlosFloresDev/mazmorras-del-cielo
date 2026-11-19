package com.aventura.cielo.block;

import com.aventura.cielo.registry.CieloBlocks;
import com.aventura.cielo.registry.CieloItems;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sello Celestial: cierra la escalera al piso siguiente de la Ciudadela. Indestructible; se abre usando una
 * Llave Celestial (se gasta). Al abrirse desaparece todo el sello conectado.
 */
public class SelloCelestialBlock extends Block {
	public SelloCelestialBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hitResult) {
		if (!itemStack.is(CieloItems.LLAVE_CELESTIAL)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level instanceof ServerLevel server) {
			abrir(server, pos);
			itemStack.consume(1, player);
			player.sendOverlayMessage(Component.literal("¡El sello se rompe! Avanza a la siguiente sección").withStyle(ChatFormatting.AQUA));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.literal("Sellado. Busca la Llave Celestial en esta sección")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return InteractionResult.SUCCESS;
	}

	/** Quita el sello conectado (hasta 12 bloques). */
	public static void abrir(ServerLevel level, BlockPos inicio) {
		Set<BlockPos> vistos = new HashSet<>();
		Deque<BlockPos> cola = new ArrayDeque<>();
		cola.add(inicio);
        int abiertos = 0;
		while (!cola.isEmpty() && abiertos < 12) {
			BlockPos p = cola.poll();
			if (!vistos.add(p) || !level.getBlockState(p).is(CieloBlocks.SELLO_CELESTIAL)) {
				continue;
			}
            abiertos++;
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
			for (Direction d : Direction.values()) {
				cola.add(p.relative(d));
			}
		}
		level.playSound(null, inicio, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.4F);
	}
}
