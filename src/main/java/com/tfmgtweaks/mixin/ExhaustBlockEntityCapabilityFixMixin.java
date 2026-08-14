package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.exhaust.ExhaustBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same confirmed bug pattern as CastingBasinBlockEntityCapabilityFixMixin
 * -- see that class's doc for the fuller background on this deep search.
 *
 * Confirmed via source: ExhaustBlockEntity.tick() calls
 * tankInventory.drain(...) every tick to vent gas piped into it, but never
 * calls invalidateCapabilities() anywhere in the class. Same relief-valve
 * shape as the Flarestack fix -- a pipe that cached "tank is full" before
 * the exhaust vented some of it has no signal telling it room opened back
 * up, and may simply stop feeding it.
 */
@Mixin(ExhaustBlockEntity.class)
public abstract class ExhaustBlockEntityCapabilityFixMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnTick(CallbackInfo ci) {
        ExhaustBlockEntity self = (ExhaustBlockEntity) (Object) this;
        if (self.getLevel() != null) {
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
