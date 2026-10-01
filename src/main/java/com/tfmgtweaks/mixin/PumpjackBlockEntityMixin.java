package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.hammer.PumpjackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only disassembles the pump jack after it has been incomplete for a grace period, so brief chunk unloads don't break it. */
@Mixin(PumpjackBlockEntity.class)
public abstract class PumpjackBlockEntityMixin {
    // About 2 seconds.
    private static final int TFMGTWEAKS$DISASSEMBLE_GRACE_TICKS = 40;

    @Unique
    private int tfmgtweaks$incompleteTickStreak = 0;

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE",
            target = "Lcom/drmangotea/tfmg/content/machinery/oil_processing/pumpjack/hammer/PumpjackBlockEntity;isComplete()Z",
            ordinal = 1))
    private boolean tfmgtweaks$debounceDisassembly(PumpjackBlockEntity self) {
        boolean actuallyComplete = self.isComplete();
        if (actuallyComplete) {
            tfmgtweaks$incompleteTickStreak = 0;
            return true;
        }
        tfmgtweaks$incompleteTickStreak++;
        return tfmgtweaks$incompleteTickStreak < TFMGTWEAKS$DISASSEMBLE_GRACE_TICKS;
    }
}
