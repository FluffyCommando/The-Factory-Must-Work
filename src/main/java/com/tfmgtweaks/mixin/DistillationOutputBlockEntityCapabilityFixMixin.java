package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.output.DistillationOutputBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities once when fluid first appears, and again after loading, so pipes can drain it. */
@Mixin(DistillationOutputBlockEntity.class)
public abstract class DistillationOutputBlockEntityCapabilityFixMixin {
    @Unique
    private boolean tfmgtweaks$hasEverHadFluid = false;

    @Inject(method = "onFluidStackChanged", at = @At("HEAD"))
    private void tfmgtweaks$invalidateOnFluidChange(FluidStack newFluidStack, CallbackInfo ci) {
        tfmgtweaks$maybeInvalidate(newFluidStack.getAmount());
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void tfmgtweaks$invalidateAfterLoad(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        DistillationOutputBlockEntity self = (DistillationOutputBlockEntity) (Object) this;
        tfmgtweaks$maybeInvalidate(self.tank.getFluidAmount());
    }

    private void tfmgtweaks$maybeInvalidate(int amount) {
        if (tfmgtweaks$hasEverHadFluid || amount <= 0) {
            return;
        }
        tfmgtweaks$hasEverHadFluid = true;
        DistillationOutputBlockEntity self = (DistillationOutputBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide) {
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
