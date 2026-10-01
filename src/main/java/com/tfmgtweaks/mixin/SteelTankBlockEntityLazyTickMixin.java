package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recomputes the distillation tower state every lazyTick, so heat is detected after a reload. */
@Mixin(SteelTankBlockEntity.class)
public abstract class SteelTankBlockEntityLazyTickMixin {
    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$reevaluateTowerStateOnLazyTick(CallbackInfo ci) {
        SteelTankBlockEntity self = (SteelTankBlockEntity) (Object) this;
        self.updateBoilerState();
    }
}
