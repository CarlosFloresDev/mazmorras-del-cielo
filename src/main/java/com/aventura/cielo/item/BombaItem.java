package com.aventura.cielo.item;

import com.aventura.cielo.entity.Bomba;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bomba arrojadiza: explota al impactar (no rompe bloques). La sueltan los mecanicos. */
public class BombaItem extends Item {
	public BombaItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.5F, 1.6F);
		if (level instanceof ServerLevel serverLevel) {
			Bomba bomba = new Bomba(serverLevel, player, 1.6F);
			bomba.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.2F, 1.0F);
			serverLevel.addFreshEntity(bomba);
			player.getCooldowns().addCooldown(stack, 15);
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
