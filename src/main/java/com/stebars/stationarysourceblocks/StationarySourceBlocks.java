package com.stebars.stationarysourceblocks;

import com.stebars.stationarysourceblocks.dispenser.EmptyBucketDispenseBehavior;
import com.stebars.stationarysourceblocks.dispenser.FishBucketDispenseBehavior;
import com.stebars.stationarysourceblocks.dispenser.LavaBucketDispenseBehavior;
import com.stebars.stationarysourceblocks.dispenser.WaterBucketDispenseBehavior;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.*;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.jline.utils.Log;

import java.util.Objects;

@Mod(StationarySourceBlocks.MODID)
@Mod.EventBusSubscriber()
public class StationarySourceBlocks {
	final static String MODID = "stationarysourceblocks";

	public StationarySourceBlocks() {
		MinecraftForge.EVENT_BUS.register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OptionsHolder.COMMON_SPEC);
		if (OptionsHolder.Common.fixDispensers.get()) registerDispenserBehaviors();
	}

	public void registerDispenserBehaviors() {
		DispenserBlock.registerBehavior(Items.BUCKET, new EmptyBucketDispenseBehavior());
		DispenserBlock.registerBehavior(Items.WATER_BUCKET, new WaterBucketDispenseBehavior());
		DispenserBlock.registerBehavior(Items.LAVA_BUCKET, new LavaBucketDispenseBehavior());
		for (String name: OptionsHolder.Common.dispenserFishBucketItems.get()) {
			Item item;
			try {
				item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
			} catch (Exception e) {
				Log.error("Item not found, ignoring: " + name);
				continue;
			}
			if (item != null) {
				DispenserBlock.registerBehavior(item, new FishBucketDispenseBehavior());
			}
		}

		for (String name: OptionsHolder.Common.dispenserDefaultItems.get()) {
			Item item;
			try {
				item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
			} catch (Exception e) {
				Log.error("Item not found, ignoring: " + name);
				continue;
			}
			if (item != null) {
				DispenserBlock.registerBehavior(item, new DefaultDispenseItemBehavior());
			}
		}
	}

	@SubscribeEvent
	public void bucketUsed(final FillBucketEvent event) {
		PlayerEntity player = event.getPlayer();
		if (player.isCreative()) return;
		Item heldItem = event.getEmptyBucket().getItem();
		World world = event.getWorld();
		RayTraceResult target = getPlayerPOVHitResult(world, player);
		RayTraceResult.Type targetType = target.getType();
		if (heldItem instanceof FishBucketItem) useFishBucket(event, world, player, target, targetType, (FishBucketItem) heldItem);
		else if (heldItem == Items.WATER_BUCKET) useWaterBucket(event, world, player, target, targetType);
		else if (heldItem == Items.LAVA_BUCKET) useLavaBucket(event, world, player, target, targetType);
		else if (heldItem == Items.BUCKET) useEmptyBucket(event, world, target, targetType);
	}

	private void hydrateFarmland(World world, BlockPos pos) {
		for(BlockPos targetPos : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
			BlockState targetState = world.getBlockState(targetPos);
			if (targetState.getBlock().equals(Blocks.FARMLAND)) world.setBlock(targetPos, targetState.setValue(FarmlandBlock.MOISTURE, 7), Constants.BlockFlags.DEFAULT_AND_RERENDER);
		}
	}

	private void useWaterBucket(FillBucketEvent event, World world, PlayerEntity player, RayTraceResult target, RayTraceResult.Type targetType) {		
		boolean soundPlayed = false;
		if (player.isOnFire()) {
			player.clearFire();
			world.playSound(player, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			soundPlayed = true;
		}

		if (!targetType.equals(RayTraceResult.Type.BLOCK)) return;

		BlockPos pos = ((BlockRayTraceResult) target).getBlockPos();

		if (world.getFluidState(pos).is(FluidTags.WATER)) {
			if (!soundPlayed) world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else if (world.getFluidState(pos).is(FluidTags.LAVA)) {
			if (!soundPlayed) world.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
		} else if (world.getBlockState(pos).getBlock().equals(Blocks.FIRE)) {
			if (!soundPlayed) world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		} else {
			hydrateFarmland(world, pos);
			if (!soundPlayed) world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}

		// Clear bucket
		event.setFilledBucket(new ItemStack(Items.BUCKET));

		// Return allow to signal that we've processed it
		event.setResult(Result.ALLOW);
	}

	private void useFishBucket(FillBucketEvent event, World world, PlayerEntity player, RayTraceResult target, RayTraceResult.Type targetType, FishBucketItem heldItem) {
		if (!(world instanceof ServerWorld) || !targetType.equals(RayTraceResult.Type.BLOCK)) return;
		EntityType<?> type = heldItem.type;
		BlockRayTraceResult blockResult = (BlockRayTraceResult) target;
		BlockPos pos = blockResult.getBlockPos();
		Entity entity = type.spawn((ServerWorld) world, event.getEmptyBucket(), null, pos, SpawnReason.BUCKET, true, false);
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
		if (!targetType.equals(RayTraceResult.Type.BLOCK)) return;

		BlockRayTraceResult blockResult = (BlockRayTraceResult) target;
		BlockPos pos = blockResult.getBlockPos();
		BlockState state = world.getBlockState(pos);

		if (world.getFluidState(pos).is(FluidTags.WATER)) {
			// poured lava onto water -- create cobblestone
			world.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
		} else if (world.getFluidState(pos).is(FluidTags.LAVA)) {
			// poured lava onto lava -- ignore
			world.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else if (state.getBlock().equals(Blocks.CAULDRON)) {
			// CauldronBlock doesn't have a clause for lava buckets, only water buckets and empty buckets - ???
			// for now, just make it empty
			world.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else {
			// poured lava onto some other block -- try to start a fire
			boolean couldSetOnFire = setOnFire(pos, world, player, state, blockResult, event.getEmptyBucket());
			if (!couldSetOnFire)
				world.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}

		// Clear bucket, return allow to signal we processed the event
		event.setFilledBucket(new ItemStack(Items.BUCKET));
		event.setResult(Result.ALLOW);
	}

	private void useEmptyBucket(FillBucketEvent event, World world, RayTraceResult target, RayTraceResult.Type targetType) {
		if (!targetType.equals(RayTraceResult.Type.BLOCK)) {
			// Can be RayTraceResult.Type.ENTITY, e.g. fish, so deny to cause default fish-catching behavior
			event.setResult(Result.DENY);
			return;
		}

		BlockPos pos = ((BlockRayTraceResult) target).getBlockPos();
		BlockState state = world.getBlockState(pos);

		if (state.getBlock().equals(Blocks.WATER)) {
			event.setFilledBucket(new ItemStack(Items.WATER_BUCKET));
			event.setResult(Result.ALLOW);
			world.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
		} else if (state.getBlock().equals(Blocks.LAVA)) {
			event.setFilledBucket(new ItemStack(Items.LAVA_BUCKET));
			event.setResult(Result.ALLOW);
			world.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
	}

	// Copied from class Item, because this is a protected function there
	private BlockRayTraceResult getPlayerPOVHitResult(World world, PlayerEntity player) {
		float f = player.xRot;
		float f1 = player.yRot;
		Vector3d vector3d = player.getEyePosition(1.0F);
		float f4 = -MathHelper.cos(-f * ((float)Math.PI / 180F));
		double d0 = Objects.requireNonNull(player.getAttribute(ForgeMod.REACH_DISTANCE.get())).getValue();
		Vector3d vector3d1 = vector3d.add((double)((MathHelper.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI)) * f4) * d0, (double)(MathHelper.sin(-f * ((float)Math.PI / 180F))) * d0, (double)((MathHelper.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI)) * f4) * d0);
		return world.clip(new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.SOURCE_ONLY, player));
	}

	// Mostly taken from FlintAndSteelItem
	// Returns true iff it could start fire
	private boolean setOnFire(BlockPos pos, World world, PlayerEntity player, BlockState state, BlockRayTraceResult target, ItemStack itemStack) {
		if (CampfireBlock.canLight(state)) {
			world.playSound(player, pos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.setBlock(pos, state.setValue(BlockStateProperties.LIT, Boolean.TRUE), 11);
			return true;
		} else {
			BlockPos blockPos = pos.relative(target.getDirection());
			if (AbstractFireBlock.canBePlacedAt(world, blockPos, player.getDirection())) {
				world.playSound(player, blockPos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
				world.setBlock(blockPos, AbstractFireBlock.getState(world, blockPos), 11);
				if (player instanceof ServerPlayerEntity) CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity)player, blockPos, itemStack);
				return true;
			} else {
				return false;
			}
		}
	}
}
