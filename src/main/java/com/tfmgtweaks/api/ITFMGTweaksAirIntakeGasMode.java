package com.tfmgtweaks.api;

import com.tfmgtweaks.airintake.AirIntakeGasMode;

/** Gives access to an Air Intake's gas mode. */
public interface ITFMGTweaksAirIntakeGasMode {
    AirIntakeGasMode tfmgtweaks$getGasMode();

    void tfmgtweaks$setGasMode(AirIntakeGasMode mode);
}
