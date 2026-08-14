package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A distillation tower stops detecting heat after a world/chunk reload
 * (and visually reverts to a plain tank), staying broken until the tank
 * block is replaced.
 *
 * Root cause: lazyTick only calls updateTemperature() when
 * isDistillationTower is true, but that flag is only recomputed by
 * updateBoilerState(), which is only called from block-place /
 * neighbor-changed handlers -- never fired by a plain world load.
 *
 * Fix: call updateBoilerState() from lazyTick() too (already safe to
 * call on every segment), so state self-corrects periodically instead
 * of depending entirely on events that don't fire on load.
 */
@Mixin(SteelTankBlockEntity.class)
public abstract class SteelTankBlockEntityLazyTickMixin {

    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$reevaluateTowerStateOnLazyTick(CallbackInfo ci) {
        SteelTankBlockEntity self = (SteelTankBlockEntity) (Object) this;
        self.updateBoilerState();
    }
}
