package com.tfmgtweaks.mixin;

import com.tfmgtweaks.compat.TFMGTagKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops flammable fluid replacing water source blocks, so oil floats. */
@Mixin(WaterFluid.class)
public abstract class WaterFluidMixin {
    @Inject(method = "canBeReplacedWith", at = @At("HEAD"), cancellable = true, require = 0)
    private void tfmgtweaks$keepFlammableFluidOffFullWater(FluidState fluidState, BlockGetter blockGetter,
            BlockPos pos, Fluid fluid, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (fluidState.isSource() && fluid.is(TFMGTagKeys.FLAMMABLE_FLUID)) {
            cir.setReturnValue(false);
        }
    }
}
