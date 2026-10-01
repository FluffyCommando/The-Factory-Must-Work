package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.base.fluid.ForceableFluidTank;
import com.drmangotea.tfmg.content.machinery.misc.firebox.FireboxBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities once, when the exhaust tank first fills, so connected pipes can drain it. */
@Mixin(FireboxBlockEntity.class)
public abstract class FireboxBlockEntityCapabilityFixMixin {
    @Shadow
    protected ForceableFluidTank exhuastTank;

    @Unique
    private boolean tfmgtweaks$hasEverHadOutput = false;

    @Inject(method = "onFluidStackChanged", at = @At("TAIL"))
    private void tfmgtweaks$invalidateOnFluidChange(FluidStack newFluidStack, CallbackInfo ci) {
        if (tfmgtweaks$hasEverHadOutput) {
            return;
        }
        FireboxBlockEntity self = (FireboxBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide || exhuastTank == null) {
            return;
        }
        if (!exhuastTank.getFluid().isEmpty()) {
            tfmgtweaks$hasEverHadOutput = true;
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
