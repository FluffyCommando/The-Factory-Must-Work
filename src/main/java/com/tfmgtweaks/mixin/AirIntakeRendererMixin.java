package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.air_intake.AirIntakeRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Raises the Air Intake's render distance to 128 so large intakes don't vanish at 64 blocks. */
@Mixin(AirIntakeRenderer.class)
public abstract class AirIntakeRendererMixin {
    public int getViewDistance() {
        return 128;
    }
}
