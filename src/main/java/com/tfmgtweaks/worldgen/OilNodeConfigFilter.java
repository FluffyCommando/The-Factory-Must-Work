package com.tfmgtweaks.worldgen;

import com.mojang.serialization.MapCodec;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Placement filter that disables TFMG's own oil nodes when Oil Rock replaces them (config). */
public class OilNodeConfigFilter extends PlacementFilter {
    public static final OilNodeConfigFilter INSTANCE = new OilNodeConfigFilter();
    public static final MapCodec<OilNodeConfigFilter> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !TFMGTweaksConfig.OIL_ROCK_REPLACES_OLD_OIL_NODES.get();
    }

    @Override
    public PlacementModifierType<?> type() {
        return TFMGTweaksPlacementModifiers.OIL_NODE_CONFIG_FILTER.get();
    }
}
