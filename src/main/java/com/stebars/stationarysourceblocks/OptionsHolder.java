package com.stebars.stationarysourceblocks;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class OptionsHolder {
	public static class Common {	

		public ConfigValue<Boolean> fixIce;
		public ConfigValue<Boolean> fixDispensers;
		public ConfigValue<List<? extends String>> dispenserFishBucketItems;
		public ConfigValue<List<? extends String>> dispenserDefaultItems;

		public Common(ForgeConfigSpec.Builder builder) {
			fixIce = builder.comment("If true, replace ice blocks with new ice blocks that don't turn into water in light or when mined.")
					.define("fixIce", true);
			fixDispensers = builder.comment("If true, dispensers can't place or remove source blocks.")
					.define("fixDispensers", true);
			dispenserFishBucketItems = builder.comment("List of fish buckets. Dispensers will toss out the fish and the water bucket separately.")
					.defineList("dispenserFishBucketItems", Lists.newArrayList("minecraft:cod_bucket",
							"minecraft:salmon_bucket", "minecraft:pufferfish_bucket", "minecraft:tropical_fish_bucket"),
							e -> e instanceof String);
			dispenserDefaultItems = builder.comment("List of items that can place/consume water when a dispenser uses them, that should be overwritten to just throw the item instead. "
					+ "Except don't add buckets, water buckets, lava buckets, or fish buckets, because this mod gives them their own behavior.")
					.defineList("dispenserDefault", Lists.newArrayList(),
							e -> e instanceof String);
		}
	}

	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;

	static { //constructor
		Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON = commonSpecPair.getLeft();
		COMMON_SPEC = commonSpecPair.getRight();
	}
}