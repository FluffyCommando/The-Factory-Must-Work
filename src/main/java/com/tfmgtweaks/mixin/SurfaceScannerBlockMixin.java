package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.surface_scanner.SurfaceScannerBlock;
import com.tfmgtweaks.api.ITFMGTweaksSurfaceScannerSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The surface scanner previously had no redstone output at all. Adds a
 * directional signal: strength encodes distance to the nearest detected
 * deposit (closer = stronger), and which of the four horizontal faces
 * outputs encodes rough direction -- a deposit in the scanner's own
 * chunk outputs on every side at full strength.
 *
 * Actual scan data and signal computation live on
 * SurfaceScannerBlockEntityRescanThrottleMixin; this is just the
 * standard block-side plumbing.
 */
@Mixin(SurfaceScannerBlock.class)
public abstract class SurfaceScannerBlockMixin {

    public boolean isSignalSource(BlockState state) {
        return true;
    }

    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        if (side.getAxis().isVertical()) {
            return 0;
        }
        if (blockAccess.getBlockEntity(pos) instanceof ITFMGTweaksSurfaceScannerSignal signalProvider) {
            return signalProvider.tfmgtweaks$getSignalForDirection(side);
        }
        return 0;
    }

    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return getSignal(blockState, blockAccess, pos, side);
    }
}
