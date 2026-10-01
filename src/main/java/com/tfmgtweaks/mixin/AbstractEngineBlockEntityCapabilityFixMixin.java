package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.engines.base.AbstractEngineBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities once, when the exhaust tank first fills, so connected pipes can drain it. */
@Mixin(AbstractEngineBlockEntity.class)
public abstract class AbstractEngineBlockEntityCapabilityFixMixin {
    @Unique
    private boolean tfmgtweaks$hasEverHadExhaust = false;

    @Inject(method = "tankUpdated", at = @At("TAIL"))
    private void tfmgtweaks$invalidateOnFluidChange(FluidStack stack, boolean fuelTank, CallbackInfo ci) {
        tfmgtweaks$maybeInvalidate();
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void tfmgtweaks$invalidateAfterLoad(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        tfmgtweaks$maybeInvalidate();
    }

    private void tfmgtweaks$maybeInvalidate() {
        if (tfmgtweaks$hasEverHadExhaust) {
            return;
        }
        AbstractEngineBlockEntity self = (AbstractEngineBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        if (!self.exhaustTank.getFluid().isEmpty()) {
            tfmgtweaks$hasEverHadExhaust = true;
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
