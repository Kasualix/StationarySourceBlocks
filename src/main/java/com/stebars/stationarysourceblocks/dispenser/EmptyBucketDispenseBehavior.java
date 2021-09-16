package com.stebars.stationarysourceblocks.dispenser;

import net.minecraft.block.DispenserBlock;
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

public class EmptyBucketDispenseBehavior extends OptionalDispenseBehavior {
	protected ItemStack execute(IBlockSource source, ItemStack bucket) {
		// takes in item stack, returns new item stack (placed back in dispenser), optionally spawns other items

		Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
		BlockPos targetPos = source.getPos().relative(direction);
		World world = source.getLevel();
		IPosition dispensePosition = DispenserBlock.getDispensePosition(source);

		if (world.getFluidState(targetPos).is(FluidTags.LAVA) // will throw bucket into lava, where it will be destroyed
				|| world.getFluidState(targetPos.below()).is(FluidTags.LAVA)
				|| world.getFluidState(source.getPos().below()).is(FluidTags.LAVA)) {
			bucket.shrink(1);
			spawnItem(world, new ItemStack(Items.LAVA_BUCKET), 6, direction, dispensePosition);
			world.playSound((PlayerEntity)null, source.getPos(), SoundEvents.BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
			return bucket;
		}

		if (world.getFluidState(targetPos).is(FluidTags.WATER)
				|| world.getFluidState(targetPos.below()).is(FluidTags.WATER)
				|| world.getFluidState(source.getPos().below()).is(FluidTags.WATER)) {
			bucket.shrink(1);
			spawnItem(world, new ItemStack(Items.WATER_BUCKET), 6, direction, dispensePosition);
			world.playSound((PlayerEntity)null, source.getPos(), SoundEvents.BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
			return bucket;
		}

		// else no liquid to take, just toss out the bucket
		ItemStack oneBucket = bucket.split(1);
		spawnItem(world, oneBucket, 6, direction, dispensePosition);
		return bucket;
	}
}