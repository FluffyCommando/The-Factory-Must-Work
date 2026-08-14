package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.recipes.jei.TFMGJei;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * JEI logs an IllegalArgumentException on startup
 * ("An interpreter is already registered for this: create:potion").
 *
 * Root cause: TFMG registers its own subtype interpreter for Create's
 * own potion fluids, but Create's JEI plugin already registers one
 * first, and JEI throws on a second registration for the same pair.
 *
 * Fix: skip this registration entirely -- Create's own interpreter
 * already handles it correctly.
 */
@Mixin(TFMGJei.class)
public abstract class TFMGJeiMixin {

    @Inject(method = "registerFluidSubtypes", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$skipRedundantPotionSubtypeRegistration(CallbackInfo ci) {
        ci.cancel();
    }
}
