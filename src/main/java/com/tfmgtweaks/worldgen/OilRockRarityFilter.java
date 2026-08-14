package com.tfmgtweaks.worldgen;

import com.mojang.serialization.MapCodec;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Same idea as vanilla's minecraft:rarity_filter, but reads the chance
 * from TFMGTweaksConfig.OIL_ROCK_SPAWN_CHANCE at runtime instead of a
 * fixed JSON value, so the user can adjust spawn rate via config without
 * needing a resource pack.
 */
public class OilRockRarityFilter extends PlacementFilter {

    public static final OilRockRarityFilter INSTANCE = new OilRockRarityFilter();
    public static final MapCodec<OilRockRarityFilter> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        int chance = TFMGTweaksConfig.OIL_ROCK_SPAWN_CHANCE.get();
        return chance <= 1 || random.nextInt(chance) == 0;
    }

    @Override
    public PlacementModifierType<?> type() {
        return TFMGTweaksPlacementModifiers.OIL_ROCK_RARITY_FILTER.get();
    }
}
