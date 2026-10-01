package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.utilities.polarizer.PolarizerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Completes the recipe once charge is full, so the polarizer doesn't need a steady extra load to finish. */
@Mixin(PolarizerBlockEntity.class)
public abstract class PolarizerBlockEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$completeOnceFullyChargedRegardlessOfThisTicksPower(CallbackInfo ci) {
        PolarizerBlockEntity self = (PolarizerBlockEntity) (Object) this;
        if (self.chargeCapacitors && self.capacitorPercentage >= 200) {
            self.onInventoryChanged(self.inventory.getStackInSlot(0).getCount());
        }
    }
}
