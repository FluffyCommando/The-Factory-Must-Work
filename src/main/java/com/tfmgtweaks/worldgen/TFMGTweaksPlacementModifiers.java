package com.tfmgtweaks.worldgen;

import com.tfmgtweaks.TFMGTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TFMGTweaksPlacementModifiers {

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, TFMGTweaks.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<OilNodeConfigFilter>> OIL_NODE_CONFIG_FILTER =
            PLACEMENT_MODIFIERS.register("oil_node_config_filter", () -> () -> OilNodeConfigFilter.CODEC);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<OilRockRarityFilter>> OIL_ROCK_RARITY_FILTER =
            PLACEMENT_MODIFIERS.register("oil_rock_rarity_filter", () -> () -> OilRockRarityFilter.CODEC);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<OilRockHeightRangePlacement>> OIL_ROCK_HEIGHT_RANGE =
            PLACEMENT_MODIFIERS.register("oil_rock_height_range", () -> () -> OilRockHeightRangePlacement.CODEC);
}
