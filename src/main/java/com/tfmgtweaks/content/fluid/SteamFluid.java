package com.tfmgtweaks.content.fluid;

import com.tfmgtweaks.registry.TFMGTweaksItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Steam -- the fracking input, replacing Hot Air/Water. Structured like
 * Create's own VirtualFluid (which is what TFMG's Hot Air actually is):
 * pipeable and pumpable, but no in-world placeable block, since it's only
 * ever meant to travel through pipes into a pump jack.
 *
 * Has a real, functional bucket item despite staying non-placeable --
 * without one, there was no way to configure a fluid filter to request
 * Steam. Placing the bucket still does nothing visually.
 */
public class SteamFluid extends BaseFlowingFluid {

    public static SteamFluid createSource(Properties properties) {
        return new SteamFluid(properties, true);
    }

    public static SteamFluid createFlowing(Properties properties) {
        return new SteamFluid(properties, false);
    }

    private final boolean source;

    public SteamFluid(Properties properties, boolean source) {
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
        return TFMGTweaksItems.STEAM_BUCKET.get();
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
