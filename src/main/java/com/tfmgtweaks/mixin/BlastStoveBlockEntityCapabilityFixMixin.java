package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_stove.BlastStoveBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities once, when an output tank first fills, so connected pipes can drain it. */
@Mixin(BlastStoveBlockEntity.class)
public abstract class BlastStoveBlockEntityCapabilityFixMixin {
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
        if (tfmgtweaks$hasEverHadOutput) {
            return;
        }
        BlastStoveBlockEntity self = (BlastStoveBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        if (!self.primaryOutputInventory.getFluid().isEmpty() || !self.secondaryOutputInventory.getFluid().isEmpty()) {
            tfmgtweaks$hasEverHadOutput = true;
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
