package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_furnace.BlastFurnaceOutputBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recomputes the furnace size every lazyTick, so a reinforced furnace doesn't revert to regular
 * when a wall chunk wasn't loaded at the time.
 */
@Mixin(BlastFurnaceOutputBlockEntity.class)
public abstract class BlastFurnaceOutputBlockEntityMixin {
    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$reevaluateReinforcementOnLazyTick(CallbackInfo ci) {
        BlastFurnaceOutputBlockEntity self = (BlastFurnaceOutputBlockEntity) (Object) this;
        self.getSize();
    }
}
