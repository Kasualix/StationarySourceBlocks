package com.stebars.stationarysourceblocks;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class OptionsHolder {
	public static class Common {	

	    public ConfigValue<Boolean> fixIce;

		public Common(ForgeConfigSpec.Builder builder) {
	        fixIce = builder.comment("If true, replace ice blocks with new ice blocks that don't turn into water in light or when mined.")
	        		.define("fixIce", true);
	        /* TODO: allowInCreative, make dispensers also not affect source blocks */
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