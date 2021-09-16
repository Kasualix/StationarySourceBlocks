package com.stebars.stationarysourceblocks.dispenser;

import net.minecraft.block.Blocks;
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

public class LavaBucketDispenseBehavior extends OptionalDispenseBehavior {
   protected ItemStack execute(IBlockSource source, ItemStack bucket) {
	   // takes in item stack, returns new item stack (placed back in dispenser), optionally spawns other items
	   
       Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
       BlockPos dispenseBlockPos = source.getPos().relative(direction);
       World world = source.getLevel();
       IPosition dispensePosition = DispenserBlock.getDispensePosition(source);

       // try to create cobblestone
       if (world.getFluidState(dispenseBlockPos).is(FluidTags.WATER)) {
    	   world.playSound((PlayerEntity)null, dispenseBlockPos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
    	   world.setBlock(dispenseBlockPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
    	   
    	   bucket.shrink(1);
    	   if (bucket.isEmpty())
    		   return new ItemStack(Items.BUCKET);
    	   spawnItem(world, new ItemStack(Items.BUCKET), 6, direction, dispensePosition);
    	   return bucket;
       }

       // if we couldn't create cobblestone, just toss the bucket
       ItemStack oneBucket = bucket.split(1);
       spawnItem(world, oneBucket, 6, direction, dispensePosition);
       return bucket;
   }
}