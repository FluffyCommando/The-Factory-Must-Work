package com.tfmgtweaks.mixin;

import com.simibubi.create.foundation.utility.DynamicComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Returns "" instead of throwing when Create's DynamicComponent has no parsed text. */
@Mixin(DynamicComponent.class)
public abstract class DynamicComponentResolveNullFixMixin {
    @Shadow
    private Component parsedCustomText;

    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true, require = 0)
    private void tfmgtweaks$preventNullResolveCrash(CallbackInfoReturnable<String> cir) {
        if (parsedCustomText == null) {
            cir.setReturnValue("");
        }
    }
}
