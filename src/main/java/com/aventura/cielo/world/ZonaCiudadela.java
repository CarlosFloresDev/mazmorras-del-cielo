package com.aventura.cielo.world;

import com.aventura.cielo.CieloMod;
import com.aventura.cielo.block.SelloCelestialBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Reglas de las estructuras etiquetadas cielo:protegidas (toda la columna de su pieza):
 * no se pueden poner ni romper bloques (salvo los spawners), ni usar perlas de ender, frutas chorus o cubetas,
 * ni planear con elitros. En creativo no aplica.
 */
public final class ZonaCiudadela {
	public static final ResourceKey<Structure> CLAVE = ResourceKey.create(Registries.STRUCTURE, CieloMod.id("ciudadela"));
	public static final net.minecraft.tags.TagKey<Structure> PROTEGIDAS = net.minecraft.tags.TagKey.create(Registries.STRUCTURE, CieloMod.id("protegidas"));
	private static final Map<UUID, Long> ULTIMO_AVISO = new HashMap<>();

	private ZonaCiudadela() {
	}

	public static boolean enZona(Level level, BlockPos pos) {
		return level instanceof ServerLevel server
			&& server.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(PROTEGIDAS)).isValid();
	}

	private static boolean aplica(Player player) {
		return !player.isCreative() && !player.isSpectator();
	}

	private static void avisar(Player player, String texto) {
		long ahora = player.level().getGameTime();
		if (ahora - ULTIMO_AVISO.getOrDefault(player.getUUID(), -100L) > 40) {
			ULTIMO_AVISO.put(player.getUUID(), ahora);
			player.sendOverlayMessage(Component.literal("☁ " + texto).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	/** Bloques con los que si se puede interactuar (cofres, sellos, puertas, palancas...). */
	private static boolean interactuable(BlockState state, Level level, BlockPos pos) {
		return state.getMenuProvider(level, pos) != null || state.is(Blocks.CHEST) || state.getBlock() instanceof SelloCelestialBlock
			|| state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof LeverBlock
			|| state.getBlock() instanceof ButtonBlock;
	}

	private static boolean prohibido(ItemStack stack) {
		return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE) || stack.is(Items.ENDER_PEARL) || stack.is(Items.CHORUS_FRUIT) || stack.is(com.aventura.cielo.registry.CieloItems.ORBE_DEL_VACIO) || stack.getItem() instanceof BucketItem;
	}

	public static void registrar() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() || !aplica(player)) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			boolean pondria = stack.getItem() instanceof BlockItem || prohibido(stack);
			if (pondria && (enZona(level, hit.getBlockPos()) || enZona(level, hit.getBlockPos().relative(hit.getDirection())))) {
				BlockState objetivo = level.getBlockState(hit.getBlockPos());
				if (!prohibido(stack) && interactuable(objetivo, level, hit.getBlockPos()) && !player.isSecondaryUseActive()) {
					return InteractionResult.PASS;
				}
				avisar(player, "La magia de la mazmorra no deja construir aquí");
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (level.isClientSide() || !aplica(player) || !prohibido(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			if (enZona(level, player.blockPosition())) {
				avisar(player, "La mazmorra bloquea los atajos: abre los sellos para avanzar");
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (level.isClientSide() || !aplica(player) || state.is(Blocks.SPAWNER) || !enZona(level, pos)) {
				return true;
			}
			avisar(player, "Los muros de la mazmorra no se pueden romper (solo los spawners)");
			return false;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 5 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isFallFlying() && aplica(player) && enZona(player.level(), player.blockPosition())) {
					player.stopFallFlying();
					avisar(player, "Los vientos de la mazmorra no dejan planear");
				}
			}
		});
	}
}
