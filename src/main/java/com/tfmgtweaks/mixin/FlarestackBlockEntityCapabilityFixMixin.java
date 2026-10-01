package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.flarestack.FlarestackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities every tick so pipes see room open up as the flarestack vents. */
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
