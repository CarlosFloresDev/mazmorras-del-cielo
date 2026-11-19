package com.aventura.cielo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Escalera de cuerda colgante: se trepa como una enredadera (tag minecraft:climbable). Cuelga de las naves hasta el suelo. */
public class EscaleraDeCuerdaBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

	public EscaleraDeCuerdaBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
