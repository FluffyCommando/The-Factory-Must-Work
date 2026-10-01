package com.tfmgtweaks.airintake;

import com.drmangotea.tfmg.registry.TFMGFluids;
import com.tfmgtweaks.registry.TFMGTweaksFluids;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

/** Gas an Air Intake produces, cycled with a wrench. */
public enum AirIntakeGasMode {
    AIR,
    CARBON_DIOXIDE,
    SULFUR_DIOXIDE;

    public AirIntakeGasMode next() {
        AirIntakeGasMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** The still fluid this mode produces. */
    public Fluid fluid() {
        return switch (this) {
            case AIR -> FluidHelper.convertToStill(TFMGFluids.AIR.get());
            case CARBON_DIOXIDE -> FluidHelper.convertToStill(TFMGFluids.CARBON_DIOXIDE.get());
            case SULFUR_DIOXIDE -> TFMGTweaksFluids.SULFUR_DIOXIDE_SOURCE.get();
        };
    }

    public Component displayName() {
        return switch (this) {
            case AIR -> Component.translatable("tfmgtweaks.air_intake.mode.air");
            case CARBON_DIOXIDE -> Component.translatable("tfmgtweaks.air_intake.mode.carbon_dioxide");
            case SULFUR_DIOXIDE -> Component.translatable("tfmgtweaks.air_intake.mode.sulfur_dioxide");
        };
    }
}
