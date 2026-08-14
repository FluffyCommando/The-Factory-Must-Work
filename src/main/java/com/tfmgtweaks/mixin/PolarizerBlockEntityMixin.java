package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.utilities.polarizer.PolarizerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reported bug: the Polarizer only reliably finishes a recipe if there's a
 * lamp (or other steady load) also connected to the same power grid.
 * Without one, the progress bar completes and the item briefly shows as
 * the crafted result, but reverts back to the input ingot -- adjusting
 * grid voltage while it's running is also reported to make it complete
 * normally.
 *
 * Root cause: PolarizerBlockEntity.tick() gates BOTH charging AND the
 * final recipe-completion trigger behind the exact same instantaneous
 * check, getPowerUsage() >= 1000:
 *
 *   if (getPowerUsage() >= 1000) {
 *       if (chargeCapacitors) {
 *           if (capacitorPercentage < 200) capacitorPercentage++;
 *           else onInventoryChanged(...);   // completes the recipe
 *       }
 *   }
 *
 * Reaching 100% charge already proves sustained power was available to
 * get there -- but completion still needs one more tick where
 * getPowerUsage() happens to read >= 1000 again. On a network with only
 * the Polarizer as a consumer, voltage has less to stabilize against than
 * with a second steady load present, so that one extra tick isn't
 * guaranteed to land -- matching both reported workarounds.
 *
 * Fix: a plain @Inject at TAIL of tick(), after the original logic has
 * already run for that tick. If capacitorPercentage is still >= 200 at
 * that point, the original check must not have fired this tick (it would
 * have reset capacitorPercentage to 0 if it had) -- so this calls
 * onInventoryChanged() directly to force completion regardless. Safe to
 * call unconditionally: it re-checks the recipe itself and just sets
 * chargeCapacitors = false with no other effect if the item is no longer
 * valid.
 *
 * An earlier version of this fix used @Redirect on the getPowerUsage()
 * call itself, qualified against IElectric (the interface that declares
 * it) -- that crashed startup with zero targets found. Confirmed via the
 * actual crash log that the call, made from within PolarizerBlockEntity's
 * own tick(), compiles qualified against the concrete class instead, not
 * the interface -- an incorrect generalization from a different mixin
 * (IElectricConnectionEndpointFixMixin) that safely targets IElectric
 * directly because those calls are inside IElectric's own default method
 * bodies, not a concrete subclass's. This TAIL-injection rewrite avoids
 * that class of guess entirely -- tick() is declared directly on
 * PolarizerBlockEntity, and every field/method used below is public.
 */
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
