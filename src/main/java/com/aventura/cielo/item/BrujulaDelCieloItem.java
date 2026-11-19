package com.aventura.cielo.item;

import com.aventura.cielo.CieloMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Brujula del Cielo: indica distancia y direccion a la mazmorra del cielo mas cercana (y a la mas cercana con jefe). */
public class BrujulaDelCieloItem extends Item {
	public static final TagKey<Structure> MAZMORRAS = TagKey.create(Registries.STRUCTURE, CieloMod.id("mazmorras"));
	public static final TagKey<Structure> CON_JEFE = TagKey.create(Registries.STRUCTURE, CieloMod.id("con_jefe"));
	public static final TagKey<Structure> CIUDADELAS = TagKey.create(Registries.STRUCTURE, CieloMod.id("ciudadelas"));

	public BrujulaDelCieloItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel) {
			player.getCooldowns().addCooldown(player.getItemInHand(hand), 60);
			BlockPos origen = player.blockPosition();
			BlockPos cercana = serverLevel.findNearestMapStructure(MAZMORRAS, origen, 80, false);
			BlockPos jefe = serverLevel.findNearestMapStructure(CON_JEFE, origen, 120, false);
			BlockPos ciudadela = serverLevel.findNearestMapStructure(CIUDADELAS, origen, 150, false);
			level.playSound(null, origen, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
			player.sendSystemMessage(Component.literal("◆ Brújula del Cielo").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
			player.sendSystemMessage(describir("  Mazmorra más cercana: ", origen, cercana, player));
			player.sendSystemMessage(describir("  Mazmorra con jefe: ", origen, jefe, player));
			player.sendSystemMessage(describir("  Gran mazmorra: ", origen, ciudadela, player));
		}
		return InteractionResult.SUCCESS;
	}

	private static Component describir(String titulo, BlockPos origen, BlockPos destino, Player player) {
		if (destino == null) {
			return Component.literal(titulo).withStyle(ChatFormatting.GRAY).append(Component.literal("ninguna cerca").withStyle(ChatFormatting.RED));
		}
		double dx = destino.getX() - player.getX();
		double dz = destino.getZ() - player.getZ();
		String[] rosa = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
		double ang = (Math.toDegrees(Math.atan2(-dx, dz)) + 360 + 22.5) % 360;
		long dist = Math.round(Math.sqrt(dx * dx + dz * dz));
		return Component.literal(titulo).withStyle(ChatFormatting.GRAY)
			.append(Component.literal(dist + " bloques al " + rosa[(int) (ang / 45) % 8]).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("  (X " + destino.getX() + ", Z " + destino.getZ() + ")").withStyle(ChatFormatting.DARK_GRAY));
	}
}
