package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.hammer.PumpjackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A pump jack's multiblock invalidates and has
 * to be manually rebuilt if any nearby chunk unloads, even briefly.
 *
 * Root cause: tick() re-derives "base"/"crank" from getBlockEntity()
 * every tick with no cooldown, and disassembles immediately the moment
 * either is null -- including for a single-tick chunk unload blip during
 * normal play.
 *
 * Fix: redirect the isComplete() call feeding disassemble() to track
 * consecutive "incomplete" ticks, only reporting false once that streak
 * exceeds a grace period. A momentary hiccup self-resolves within the
 * window; a genuine break still disassembles, just slightly later.
 */
@Mixin(PumpjackBlockEntity.class)
public abstract class PumpjackBlockEntityMixin {

    // ~2 seconds at 20 TPS. Raise if chunk-load hiccups on your setup last
    // longer than this; lower if you want disassembly to react faster to
    // genuine breaks.
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
        // report "still complete" (suppressing disassemble) until the
        // incomplete streak persists past the grace period
        return tfmgtweaks$incompleteTickStreak < TFMGTWEAKS$DISASSEMBLE_GRACE_TICKS;
    }
}
