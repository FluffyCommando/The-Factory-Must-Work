package com.tfmgtweaks.content.fluid;

import com.tfmgtweaks.registry.TFMGTweaksItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Sulfur Dioxide: pipeable only, never placeable. */
public class SulfurDioxideFluid extends BaseFlowingFluid {
    public static SulfurDioxideFluid createSource(Properties properties) {
        return new SulfurDioxideFluid(properties, true);
    }

    public static SulfurDioxideFluid createFlowing(Properties properties) {
        return new SulfurDioxideFluid(properties, false);
    }

    private final boolean source;

    public SulfurDioxideFluid(Properties properties, boolean source) {
        super(properties);
        this.source = source;
    }

    @Override
    public Fluid getSource() {
        if (source) {
            return this;
        }
        return super.getSource();
    }

    @Override
    public Fluid getFlowing() {
        if (source) {
            return super.getFlowing();
        }
        return this;
    }

    @Override
    public Item getBucket() {
        return TFMGTweaksItems.SULFUR_DIOXIDE_BUCKET.get();
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return source;
    }

    @Override
    public int getAmount(FluidState state) {
        return 0;
    }
}
