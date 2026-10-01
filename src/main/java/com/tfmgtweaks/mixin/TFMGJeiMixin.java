package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.recipes.jei.TFMGJei;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips TFMG's duplicate potion fluid subtype registration, which Create's JEI plugin already handles. */
@Mixin(TFMGJei.class)
public abstract class TFMGJeiMixin {
    @Inject(method = "registerFluidSubtypes", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$skipRedundantPotionSubtypeRegistration(CallbackInfo ci) {
        ci.cancel();
    }
}
