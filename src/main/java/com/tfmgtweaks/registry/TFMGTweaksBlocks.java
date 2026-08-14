package com.tfmgtweaks.registry;

import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.content.boiler.BoilerBlock;
import com.tfmgtweaks.content.oilrock.OilRockBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TFMGTweaksBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TFMGTweaks.MOD_ID);

    public static final DeferredBlock<OilRockBlock> OIL_ROCK = BLOCKS.register("oil_rock",
            () -> new OilRockBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(2.5f, 6.0f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<BoilerBlock> BOILER = BLOCKS.register("boiler",
            () -> new BoilerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(4.0f, 8.0f)
                    .requiresCorrectToolForDrops()));
}
