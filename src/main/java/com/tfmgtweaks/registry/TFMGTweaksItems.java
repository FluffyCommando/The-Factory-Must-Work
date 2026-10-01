package com.tfmgtweaks.registry;

import com.tfmgtweaks.TFMGTweaks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TFMGTweaksItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TFMGTweaks.MOD_ID);

    public static final DeferredItem<BlockItem> OIL_ROCK = ITEMS.registerSimpleBlockItem(
            "oil_rock", TFMGTweaksBlocks.OIL_ROCK);

    /** Steam can't be placed; the bucket exists so Steam can be set in fluid filters. */
    public static final DeferredItem<BucketItem> STEAM_BUCKET = ITEMS.register("steam_bucket",
            () -> new BucketItem(TFMGTweaksFluids.STEAM_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /** Same purpose as STEAM_BUCKET. */
    public static final DeferredItem<BucketItem> SULFUR_DIOXIDE_BUCKET = ITEMS.register("sulfur_dioxide_bucket",
            () -> new BucketItem(TFMGTweaksFluids.SULFUR_DIOXIDE_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /** Regular bucket for burning fuel, which is a placeable fluid. */
    public static final DeferredItem<BucketItem> BURNING_FUEL_BUCKET = ITEMS.register("burning_fuel_bucket",
            () -> new BucketItem(TFMGTweaksFluids.BURNING_FUEL_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
}
