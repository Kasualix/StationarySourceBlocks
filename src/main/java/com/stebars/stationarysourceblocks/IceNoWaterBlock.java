package com.stebars.stationarysourceblocks;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IceBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

// This code mostly from IceBlock class, modified so it doesn't melt in light or turn into water when broken

public class IceNoWaterBlock extends IceBlock {

	public IceNoWaterBlock() {
		super(Block.Properties.copy(Blocks.ICE));
	}

	@Override
	public void playerDestroy(World p_180657_1_, PlayerEntity p_180657_2_, BlockPos p_180657_3_, BlockState p_180657_4_, @Nullable TileEntity p_180657_5_, ItemStack p_180657_6_) {
		// We can't call super.super.playerDestroy or even ((BreakableBlock) this).playerDestroy, because it bReAkS eNcApSuLaTiOn, so instead just copying Block's playerDestroy code here 
		p_180657_2_.awardStat(Stats.BLOCK_MINED.get(this));
		p_180657_2_.causeFoodExhaustion(0.005F);
		dropResources(p_180657_4_, p_180657_1_, p_180657_3_, p_180657_5_, p_180657_2_, p_180657_6_);

		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, p_180657_6_) == 0) {
			if (p_180657_1_.dimensionType().ultraWarm()) {
				p_180657_1_.removeBlock(p_180657_3_, false);
				return;
			}
		}
	}

	@Override
	public void randomTick(BlockState p_225542_1_, ServerWorld p_225542_2_, BlockPos p_225542_3_, Random p_225542_4_) {
		if (p_225542_2_.getBrightness(LightType.BLOCK, p_225542_3_) > 11 - p_225542_1_.getLightBlock(p_225542_2_, p_225542_3_)) {
			this.melt(p_225542_1_, p_225542_2_, p_225542_3_);
		}
	}

	@Override
	protected void melt(BlockState p_196454_1_, World p_196454_2_, BlockPos p_196454_3_) {
		p_196454_2_.removeBlock(p_196454_3_, false);
	}
}

