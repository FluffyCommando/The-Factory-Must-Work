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

    public static final DeferredItem<BlockItem> BOILER = ITEMS.registerSimpleBlockItem(
            "boiler", TFMGTweaksBlocks.BOILER);

    /**
     * Steam stays non-placeable -- this bucket exists so players have a
     * real item to put into a fluid filter and request Steam extraction
     * with (Items.AIR, the original placeholder, can't be selected).
     * Matches TFMG's own convention for gas fluids exactly. Also needs a
     * Spout recipe to actually produce one -- see
     * data/tfmgtweaks/recipe/filling/steam_bucket.json.
     */
    public static final DeferredItem<BucketItem> STEAM_BUCKET = ITEMS.register("steam_bucket",
            () -> new BucketItem(TFMGTweaksFluids.STEAM_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
}
