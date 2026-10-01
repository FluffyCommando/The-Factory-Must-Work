package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.base.fluid.ForceableFluidTank;
import com.drmangotea.tfmg.content.machinery.metallurgy.blast_stove.BlastStoveBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities once, when an output tank first fills, so connected pipes can drain it. */
@Mixin(BlastStoveBlockEntity.class)
public abstract class BlastStoveBlockEntityCapabilityFixMixin {
    @Shadow
    protected ForceableFluidTank primaryOutputTank;

    @Shadow
    protected ForceableFluidTank exhaustOutputTank;

    @Unique
    private boolean tfmgtweaks$hasEverHadOutput = false;

    @Inject(method = "onFluidStackChanged", at = @At("TAIL"))
    private void tfmgtweaks$invalidateOnFluidChange(FluidStack newFluidStack, CallbackInfo ci) {
        if (tfmgtweaks$hasEverHadOutput) {
            return;
        }
        BlastStoveBlockEntity self = (BlastStoveBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        boolean primaryHasFluid = primaryOutputTank != null && !primaryOutputTank.getFluid().isEmpty();
        boolean secondaryHasFluid = exhaustOutputTank != null && !exhaustOutputTank.getFluid().isEmpty();
        if (primaryHasFluid || secondaryHasFluid) {
            tfmgtweaks$hasEverHadOutput = true;
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
