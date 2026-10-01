package com.tfmgtweaks.content.fluid;

import com.tfmgtweaks.compat.TFMGTagKeys;
import com.tfmgtweaks.explosion.FluidIgnition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Flowing burning fuel; see FluidIgnition for how it is placed and removed. */
public class BurningFuelFlowingFluid extends BaseFlowingFluid.Flowing {
    public BurningFuelFlowingFluid(BaseFlowingFluid.Properties properties) {
        super(properties);
    }

    /**
     * Holds its level while unlit flammable fluid is adjacent, then uses normal fluid physics.
     * Returns empty while FluidIgnition is actively clearing this position.
     */
    @Override
    protected FluidState getNewLiquid(Level level, BlockPos pos, BlockState blockState) {
        if (level instanceof ServerLevel serverLevel && FluidIgnition.isActivelyClearing(serverLevel, pos)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        FluidState current = level.getFluidState(pos);
        if (current.getType() == this && hasUnlitFlammableNeighbor(level, pos)) {
            return current;
        }
        return super.getNewLiquid(level, pos, blockState);
    }

    /** Whether any adjacent fluid is flammable but not yet burning. */
    private boolean hasUnlitFlammableNeighbor(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            FluidState neighbor = level.getFluidState(pos.relative(direction));
            if (!neighbor.isEmpty() && !neighbor.getType().isSame(this)
                    && neighbor.getType().is(TFMGTagKeys.FLAMMABLE_FLUID)) {
                return true;
            }
        }
        return false;
    }

    /** Refuses replacement by flammable fluid so fresh oil can't overwrite a just-ignited position. */
    @Override
    public boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockGetter, BlockPos pos, Fluid fluid,
                                      Direction direction) {
        if (fluid.is(TFMGTagKeys.FLAMMABLE_FLUID)) {
            return false;
        }
        return super.canBeReplacedWith(fluidState, blockGetter, pos, fluid, direction);
    }
}
