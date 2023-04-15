package com.stebars.stationarysourceblocks;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.List;

public class OptionsHolder {
	public static ForgeConfigSpec config;
	public static ForgeConfigSpec.BooleanValue fixIce;
	public static ForgeConfigSpec.BooleanValue fixDispensers;
	public static ConfigValue<List<? extends String>> dispenserFishBucketItems;
	public static ConfigValue<List<? extends String>> dispenserDefaultItems;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("StationarySourceBlocks").push("General");
		fixIce = builder
				.comment("If true, replace ice blocks with new ice blocks that don't turn into water in light or when mined.")
				.define("fixIce", true);
		fixDispensers = builder
				.comment("If true, dispensers can't place or remove source blocks.")
				.define("fixDispensers", true);
		dispenserFishBucketItems = builder
				.comment("List of fish buckets. Dispensers will toss out the fish and the water bucket separately.")
				.defineList("dispenserFishBucketItems", Lists.newArrayList("minecraft:cod_bucket", "minecraft:salmon_bucket", "minecraft:pufferfish_bucket", "minecraft:tropical_fish_bucket"), e -> e instanceof String);
		dispenserDefaultItems = builder
				.comment("List of items that can place/consume water when a dispenser uses them, that should be overwritten to just throw the item instead. ",
						"Except don't add buckets, water buckets, lava buckets, or fish buckets, because this mod gives them their own behavior.")
				.defineList("dispenserDefault", Lists.newArrayList(), e -> e instanceof String);
		builder.pop();
		config = builder.build();
	}
}