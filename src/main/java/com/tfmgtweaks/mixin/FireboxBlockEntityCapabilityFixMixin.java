package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.firebox.FireboxBlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
    protected FluidTank exhuastTank;

    @Unique
    private boolean tfmgtweaks$hasEverHadOutput = false;

    @Inject(method = "onFluidStackChanged", at = @At("TAIL"))
    private void tfmgtweaks$invalidateOnFluidChange(FluidStack newFluidStack, CallbackInfo ci) {
        tfmgtweaks$maybeInvalidate();
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void tfmgtweaks$invalidateAfterLoad(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        tfmgtweaks$maybeInvalidate();
    }

    private void tfmgtweaks$maybeInvalidate() {
        if (tfmgtweaks$hasEverHadOutput || exhuastTank == null) {
            return;
        }
        FireboxBlockEntity self = (FireboxBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        if (!exhuastTank.getFluid().isEmpty()) {
            tfmgtweaks$hasEverHadOutput = true;
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
