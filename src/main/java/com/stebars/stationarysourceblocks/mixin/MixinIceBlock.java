package com.stebars.stationarysourceblocks.mixin;

import com.stebars.stationarysourceblocks.OptionsHolder;
import net.minecraft.block.BlockState;
import net.minecraft.block.BreakableBlock;
import net.minecraft.block.IceBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public class MixinIceBlock extends BreakableBlock {
    public MixinIceBlock(Properties properties) {
        super(properties);
    }

    @Inject(method = "playerDestroy", at = @At("HEAD"), cancellable = true)
    private void onPlayerDestroy(World world, PlayerEntity player, BlockPos pos, BlockState state, TileEntity entity, ItemStack stack, CallbackInfo ci) {
        if (!OptionsHolder.Common.fixIce.get()) return;
        player.awardStat(Stats.BLOCK_MINED.get((IceBlock)((Object)this)));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, world, pos, entity, player, stack);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack) == 0 && world.dimensionType().ultraWarm()) world.removeBlock(pos, false);
        ci.cancel();
    }
    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    private void onMelt(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (!OptionsHolder.Common.fixIce.get()) return;
        world.removeBlock(pos, false);
        ci.cancel();
    }
}
