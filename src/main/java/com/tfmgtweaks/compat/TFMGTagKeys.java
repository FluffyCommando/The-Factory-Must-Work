package com.tfmgtweaks.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class TFMGTagKeys {
    private TFMGTagKeys() {
    }

    /** tfmg:surface_scanner_findable */
    public static final TagKey<Block> SURFACE_SCANNER_FINDABLE = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("tfmg", "surface_scanner_findable"));

    /** tfmg:flammable */
    public static final TagKey<Fluid> FLAMMABLE_FLUID = TagKey.create(
            Registries.FLUID, ResourceLocation.fromNamespaceAndPath("tfmg", "flammable"));
}
