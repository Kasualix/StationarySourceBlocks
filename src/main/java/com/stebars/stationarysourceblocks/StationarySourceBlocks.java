package com.stebars.stationarysourceblocks;

import java.lang.reflect.Field;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod("stationarysourceblocks")
public class StationarySourceBlocks {
    public StationarySourceBlocks() {
        MinecraftForge.EVENT_BUS.register(this);
    }

	@SubscribeEvent
	public void bucketUsed(final FillBucketEvent event)
	{		
		// The name `FillBucketEvent` and javadoc seem wrong.
		// This event is fired when a bucket is filled or emptied.
		// event.getEmptyBucket() returns the bucket you're holding, full or empty.
		// event.getFilledBucket() returns null unless you set it.
		// You set with event.setFilledBucket() to determine resulting item.
		// event.getResult() is always "default", unless you change it to something else with .setResult().
		// event's result is whether bucket is allowed to do what this function tries to do.
		// So setting result to "deny" makes it do whatever it would by default. Setting to "allow" cancels default.
		
		Item heldItem = event.getEmptyBucket().getItem();
		World world = event.getWorld();
		PlayerEntity player = event.getPlayer();

		// redo raytrace, the one in event.getTarget() doesn't seem to detect ray colliding with fluid source blocks
        RayTraceResult target = getPlayerPOVHitResult(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);
		RayTraceResult.Type targetType = target.getType();
		
		if (heldItem instanceof FishBucketItem)
			useFishBucket(event, world, player, target, targetType, (FishBucketItem) heldItem);
		else if (heldItem == Items.WATER_BUCKET)
			useWaterBucket(event, world, player, target, targetType);
		else if (heldItem == Items.LAVA_BUCKET)
			useLavaBucket(event, world, player, target, targetType);
		else if (heldItem == Items.BUCKET)
			useEmptyBucket(event, world, player, target, targetType);
	}
	
	private void useWaterBucket(FillBucketEvent event, World world, PlayerEntity player, RayTraceResult target, RayTraceResult.Type targetType) {		
		boolean soundPlayed = false;
		if (player.isOnFire()) {
			player.clearFire();
			world.playSound(player, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			soundPlayed = true;
		}
		
		if (!targetType.equals(RayTraceResult.Type.BLOCK))
			return;
		
		BlockRayTraceResult blockResult = (BlockRayTraceResult) target;
		BlockPos pos = blockResult.getBlockPos();
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();		

		if (world.getFluidState(pos).is(FluidTags.WATER)) {
			// poured water onto water -- just empty bucket
			if (!soundPlayed)
				world.playSound((PlayerEntity)null, pos, SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else if (world.getFluidState(pos).is(FluidTags.LAVA)) {
			// poured water onto lava -- create obsidian
			if (!soundPlayed)
				world.playSound((PlayerEntity)null, pos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
		} else if (block.equals(Blocks.FIRE)) {
			// poured water onto fire -- extinguish the fire
			if (!soundPlayed)
				world.playSound((PlayerEntity)null, pos, SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		} else {
			// tried to pour water on a block, not lava, so empty and possibly hydrate nearby farmland
			hydrateFarmland(world, pos, state);
			if (!soundPlayed)
				world.playSound((PlayerEntity)null, pos, SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
		
		// Clear bucket
		event.setFilledBucket(new ItemStack(Items.BUCKET));
		
		// Return allow to signal that we've processed it
		event.setResult(Result.ALLOW);
	}
	
	private void hydrateFarmland(World world, BlockPos pos, BlockState state) {
		for(BlockPos targetPos : BlockPos.betweenClosed(
				pos.offset(-1, 0, -1),
				pos.offset(1, 0, 1))) {
			BlockState targetState = world.getBlockState(targetPos);
			Block targetBlock = targetState.getBlock();
			if (targetBlock.equals(Blocks.FARMLAND)) {
				world.setBlock(targetPos,
						targetState.setValue(FarmlandBlock.MOISTURE, 7),
						Constants.BlockFlags.DEFAULT_AND_RERENDER);
			}
		}
	}
	
	private void useFishBucket(FillBucketEvent event, World world, PlayerEntity player, RayTraceResult target, RayTraceResult.Type targetType,
			FishBucketItem heldItem) {
		if (!(world instanceof ServerWorld))
			return;
		if (!targetType.equals(RayTraceResult.Type.BLOCK))
			return;
		
		Field fishBucketType;
		EntityType<?> type;
		try {
			fishBucketType = FishBucketItem.class.getDeclaredField("type");
			type = (EntityType<?>) fishBucketType.get(heldItem);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		fishBucketType.setAccessible(true);
		
		BlockRayTraceResult blockResult = (BlockRayTraceResult) target;
		BlockPos pos = blockResult.getBlockPos();	

		Entity entity = type.spawn((ServerWorld) world, event.getEmptyBucket(), (PlayerEntity)null, pos, SpawnReason.BUCKET, true, false);
		if (entity != null) {
			((AbstractFishEntity)entity).setFromBucket(true);
		}
		world.playSound(player, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
		
		// Finish with a bucket containing only water
		event.setFilledBucket(new ItemStack(Items.WATER_BUCKET));
		
		// Return allow to signal that we've processed it
		event.setResult(Result.ALLOW);
	}

	private void useLavaBucket(FillBucketEvent event, World world, PlayerEntity player, RayTraceResult target, RayTraceResult.Type targetType) {
		if (!targetType.equals(RayTraceResult.Type.BLOCK))
			return;

		BlockRayTraceResult blockResult = (BlockRayTraceResult) target;
		BlockPos pos = blockResult.getBlockPos();
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		if (world.getFluidState(pos).is(FluidTags.WATER)) {
			// poured lava onto water -- create cobblestone
			world.playSound((PlayerEntity)null, pos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
		} else if (world.getFluidState(pos).is(FluidTags.LAVA)) {
			// poured lava onto lava -- ignore
			world.playSound((PlayerEntity)null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else if (block.equals(Blocks.CAULDRON)) {
			// CauldronBlock doesn't have a clause for lava buckets, only water buckets and empty buckets - ???
			// for now, just make it empty
			//CauldronBlock cauldronBlock = (CauldronBlock) block;
			//cauldronBlock.use(state, world, pos, player, Hand.MAIN_HAND, blockResult);
			world.playSound((PlayerEntity)null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else {
			// poured lava onto some other block -- try to start a fire
			boolean couldSetOnFire = setOnFire(event, pos, world, player, state, blockResult, event.getEmptyBucket());
			if (!couldSetOnFire)
				world.playSound((PlayerEntity)null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}

		// Clear bucket, return allow to signal we processed the event
		event.setFilledBucket(new ItemStack(Items.BUCKET));
		event.setResult(Result.ALLOW);
	}
	
	private void useEmptyBucket(FillBucketEvent event, World world, PlayerEntity player, RayTraceResult target, RayTraceResult.Type targetType) {
		if (!targetType.equals(RayTraceResult.Type.BLOCK)) {
			// Can be RayTraceResult.Type.ENTITY, e.g. fish, so deny to cause default fish-catching behavior
			event.setResult(Result.DENY);
			return;
		}
		
		BlockRayTraceResult blockResult = (BlockRayTraceResult) target;
		BlockPos pos = blockResult.getBlockPos();
		BlockState state = world.getBlockState(pos);
		
		if (state.getBlock().equals(Blocks.WATER)) {
			event.setFilledBucket(new ItemStack(Items.WATER_BUCKET));
			event.setResult(Result.ALLOW);
            world.playSound((PlayerEntity)null, pos, SoundEvents.BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else if (state.getBlock().equals(Blocks.LAVA)) {
			event.setFilledBucket(new ItemStack(Items.LAVA_BUCKET));
			event.setResult(Result.ALLOW);
			world.playSound((PlayerEntity)null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
	}
	
	// Copied from class Item, because this is a protected function there
	private BlockRayTraceResult getPlayerPOVHitResult(World p_219968_0_, PlayerEntity p_219968_1_, RayTraceContext.FluidMode p_219968_2_) {
	      float f = p_219968_1_.xRot;
	      float f1 = p_219968_1_.yRot;
	      Vector3d vector3d = p_219968_1_.getEyePosition(1.0F);
	      float f2 = MathHelper.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
	      float f3 = MathHelper.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
	      float f4 = -MathHelper.cos(-f * ((float)Math.PI / 180F));
	      float f5 = MathHelper.sin(-f * ((float)Math.PI / 180F));
	      float f6 = f3 * f4;
	      float f7 = f2 * f4;
	      double d0 = p_219968_1_.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();;
	      Vector3d vector3d1 = vector3d.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
	      return p_219968_0_.clip(new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, p_219968_2_, p_219968_1_));
	}
	
	// Mostly taken from FlintAndSteelItem
	// Returns true iff it could start fire
	private boolean setOnFire(FillBucketEvent event, BlockPos pos, World world, PlayerEntity player, BlockState state, BlockRayTraceResult target, ItemStack itemStack) {		
		if (CampfireBlock.canLight(state)) {
			world.playSound(player, pos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
			return true;
		} else {
			BlockPos blockpos1 = pos.relative(target.getDirection());
			if (AbstractFireBlock.canBePlacedAt(world, blockpos1, player.getDirection())) {
				world.playSound(player, blockpos1, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
				BlockState blockstate1 = AbstractFireBlock.getState(world, blockpos1);
				world.setBlock(blockpos1, blockstate1, 11);
				if (player instanceof ServerPlayerEntity) {
					CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity)player, blockpos1, itemStack);
				}
				return true;
			} else {
				return false;
			}
		}
	}
}
