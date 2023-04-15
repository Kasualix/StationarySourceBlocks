package com.stebars.stationarysourceblocks.dispenser;

import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.dispenser.OptionalDispenseBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.lang.reflect.Field;

public class FishBucketDispenseBehavior extends OptionalDispenseBehavior {
	protected ItemStack execute(IBlockSource source, ItemStack bucket) {
		// takes in item stack, returns new item stack (placed back in dispenser), optionally spawns other items

		Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
		BlockPos targetPos = source.getPos().relative(direction);
		ServerWorld world = source.getLevel();
		IPosition dispensePosition = DispenserBlock.getDispensePosition(source);

		Field fishBucketType;
		EntityType<?> type;
		try {
			fishBucketType = FishBucketItem.class.getDeclaredField("type");
			fishBucketType.setAccessible(true);
			type = (EntityType<?>) fishBucketType.get(bucket.getItem());
		} catch (Exception e) {
			e.printStackTrace();
			return bucket;
		}

		// spawn fish
		Entity entity = type.spawn(world, bucket, null, targetPos, SpawnReason.BUCKET, true, false);
		if (entity != null) ((AbstractFishEntity)entity).setFromBucket(true);

		world.playSound(null, targetPos, SoundEvents.BUCKET_EMPTY_FISH, SoundCategory.NEUTRAL, 1.0F, 1.0F);

		// throw out water bucket, return the rest of the buckets
		bucket.shrink(1);
		spawnItem(world, new ItemStack(Items.WATER_BUCKET), 6, direction, dispensePosition);
		return bucket;
	}
}