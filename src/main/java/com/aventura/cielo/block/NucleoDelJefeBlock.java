package com.aventura.cielo.block;

import com.aventura.cielo.registry.CieloBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * Nucleo del jefe: va en el centro de la arena. Cuando un jugador en supervivencia se acerca, invoca al jefe
 * (una sola vez) y queda apagado. Es indestructible mientras esta activo.
 */
public class NucleoDelJefeBlock extends Block implements EntityBlock {
	public enum Jefe implements StringRepresentable {
		CAPITAN("capitan"), CONDOR("condor"), JEAN("jean"), CENIZA("ceniza"), VACIO("vacio");

		private final String id;

		Jefe(String id) {
			this.id = id;
		}

		@Override
		public String getSerializedName() {
			return this.id;
		}
	}

	public static final EnumProperty<Jefe> JEFE = EnumProperty.create("jefe", Jefe.class);
	public static final BooleanProperty ACTIVO = BooleanProperty.create("activo");

	public NucleoDelJefeBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(JEFE, Jefe.CAPITAN).setValue(ACTIVO, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(JEFE, ACTIVO);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new NucleoDelJefeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != CieloBlockEntities.NUCLEO_DEL_JEFE || !state.getValue(ACTIVO) ? null
			: (lvl, pos, st, be) -> NucleoDelJefeBlockEntity.serverTick((ServerLevel) lvl, pos, st);
	}
}
