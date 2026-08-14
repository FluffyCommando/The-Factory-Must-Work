package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.network.large_switch.LargeSwitchVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import dev.engine_room.flywheel.api.instance.Instance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Placing a large switch spams the log with a Flywheel
 * NullPointerException every time you enter/exit render distance.
 *
 * Root cause: LargeSwitchVisual's constructor leaves `shaft` null for
 * any block that isn't the main part, but _delete(), update(), and
 * collectCrumblingInstances() all use it unconditionally. The crash is
 * specifically in _delete() (torn down on every visual, hence frequent),
 * but the others share the same unguarded pattern.
 */
@Mixin(LargeSwitchVisual.class)
public abstract class LargeSwitchVisualMixin {

    @Shadow
    protected final RotatingInstance shaft = null;

    @Inject(method = {"update", "updateLight"}, at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullShaft(float pt, CallbackInfo ci) {
        if (shaft == null) {
            ci.cancel();
        }
    }

    @Inject(method = "_delete", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullShaftOnDelete(CallbackInfo ci) {
        if (shaft == null) {
            ci.cancel();
        }
    }

    @Inject(method = "collectCrumblingInstances", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullShaftOnCrumbling(Consumer<Instance> consumer, CallbackInfo ci) {
        if (shaft == null) {
            ci.cancel();
        }
    }
}
