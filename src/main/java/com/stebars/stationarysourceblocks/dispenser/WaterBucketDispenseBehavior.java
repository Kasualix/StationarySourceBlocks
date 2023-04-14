package com.stebars.stationarysourceblocks.dispenser;

import net.minecraft.block.*;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.dispenser.OptionalDispenseBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

public class WaterBucketDispenseBehavior extends OptionalDispenseBehavior {
	protected ItemStack execute(IBlockSource source, ItemStack bucket) {
		// takes in item stack, returns new item stack (placed back in dispenser), optionally spawns other items

		Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
		BlockPos dispenseBlockPos = source.getPos().relative(direction);
		World world = source.getLevel();
		IPosition dispensePosition = DispenserBlock.getDispensePosition(source);

		// try to create cobblestone
		if (world.getFluidState(dispenseBlockPos).is(FluidTags.LAVA)) {
			world.playSound(null, dispenseBlockPos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(dispenseBlockPos, Blocks.COBBLESTONE.defaultBlockState(), 3);

			bucket.shrink(1);
			if (bucket.isEmpty())
				return new ItemStack(Items.BUCKET);
			spawnItem(world, new ItemStack(Items.BUCKET), 6, direction, dispensePosition);
			return bucket;
		}

		// try to hydrate blocks
		boolean anyBlocksHydrated = false;
		BlockPos hydrateCenter = dispenseBlockPos.relative(direction);
		for(BlockPos targetPos : BlockPos.betweenClosed(
				hydrateCenter.offset(-1, 0, -1),
				hydrateCenter.offset(1, -1, 1))) {
			BlockState targetState = world.getBlockState(targetPos);
			Block targetBlock = targetState.getBlock();
			if (targetBlock.equals(Blocks.FARMLAND)) {
				anyBlocksHydrated = true;
				world.setBlock(targetPos,
						targetState.setValue(FarmlandBlock.MOISTURE, 7),
						Constants.BlockFlags.DEFAULT_AND_RERENDER);
			}
		}
		// if we could hydrate blocks, toss out the empty bucket
		if (anyBlocksHydrated) {
			world.playSound((PlayerEntity)null, hydrateCenter, SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
			return new ItemStack(Items.BUCKET);
		}

		// if no farmland was hydrated, just toss the bucket (e.g. down a mineshaft to a cobblestone generator)
		ItemStack oneBucket = bucket.split(1);
		spawnItem(world, oneBucket, 6, direction, dispensePosition);
		return bucket;
	}
}