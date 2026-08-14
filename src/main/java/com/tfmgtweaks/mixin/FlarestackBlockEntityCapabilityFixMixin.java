package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.flarestack.FlarestackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same confirmed bug pattern as CastingBasinBlockEntityCapabilityFixMixin
 * -- see that class's doc for the fuller background on this deep search.
 *
 * Confirmed via source: FlarestackBlockEntity.tick() calls
 * tankInventory.drain(...) every tick to burn off gas piped into it, but
 * never calls invalidateCapabilities() anywhere in the class. A pipe that
 * cached "tank is full, can't fill more" the moment the flarestack's
 * buffer filled up has no signal telling it room opened back up after
 * some was drained/burned off, and may simply stop feeding it -- the
 * flarestack is meant to act as a relief valve for excess gas elsewhere in
 * the system, so this can manifest as gas backing up and production
 * stalling upstream, not just at the flarestack itself.
 */
@Mixin(FlarestackBlockEntity.class)
public abstract class FlarestackBlockEntityCapabilityFixMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnTick(CallbackInfo ci) {
        FlarestackBlockEntity self = (FlarestackBlockEntity) (Object) this;
        if (self.getLevel() != null) {
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
