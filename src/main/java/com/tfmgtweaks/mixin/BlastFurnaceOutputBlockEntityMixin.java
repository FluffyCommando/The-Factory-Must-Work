package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_furnace.BlastFurnaceOutputBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A fully reinforced blast furnace reverts to "regular"
 * the moment an item is pushed into it, staying that way until a wall
 * block is broken and replaced.
 *
 * Root cause: isReinforced is only recomputed inside getSize(), which
 * tick() only calls once the input is non-empty -- so on a fresh
 * furnace, if any wall position's chunk hasn't finished loading at that
 * exact moment (real on servers), it reads as air and undercounts,
 * incorrectly computing isReinforced = false permanently.
 *
 * Fix: same self-healing pattern as SteelTankBlockEntityLazyTickMixin --
 * call getSize() (read-only) from lazyTick() too, so a bad first read
 * gets corrected on a later pass.
 */
@Mixin(BlastFurnaceOutputBlockEntity.class)
public abstract class BlastFurnaceOutputBlockEntityMixin {

    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$reevaluateReinforcementOnLazyTick(CallbackInfo ci) {
        BlastFurnaceOutputBlockEntity self = (BlastFurnaceOutputBlockEntity) (Object) this;
        self.getSize();
    }
}
