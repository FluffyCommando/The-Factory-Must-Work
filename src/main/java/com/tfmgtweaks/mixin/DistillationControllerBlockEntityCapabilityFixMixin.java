package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.controller.DistillationControllerBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities once, when fluid first appears, so connected pipes can drain it. */
@Mixin(DistillationControllerBlockEntity.class)
public abstract class DistillationControllerBlockEntityCapabilityFixMixin {
    @Unique
    private boolean tfmgtweaks$hasEverHadFluid = false;

    @Inject(method = "onFluidStackChanged", at = @At("HEAD"))
    private void tfmgtweaks$invalidateOnFluidChange(FluidStack newFluidStack, CallbackInfo ci) {
        if (tfmgtweaks$hasEverHadFluid || newFluidStack.getAmount() <= 0) {
            return;
        }
        tfmgtweaks$hasEverHadFluid = true;
        DistillationControllerBlockEntity self = (DistillationControllerBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide) {
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
