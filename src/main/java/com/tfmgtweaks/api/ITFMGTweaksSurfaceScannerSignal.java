package com.tfmgtweaks.api;

import net.minecraft.core.Direction;

/**
 * Implemented by SurfaceScannerBlockEntityRescanThrottleMixin (which
 * maintains the server-side scan data), read by SurfaceScannerBlockMixin
 * for getSignal()/getDirectSignal().
 *
 * Deliberately lives outside com.tfmgtweaks.mixin -- Mixin forbids
 * anything in the designated mixin package from being referenced
 * directly by normal code, even a plain interface (confirmed via an
 * actual IllegalClassLoadError when this lived there originally).
 */
public interface ITFMGTweaksSurfaceScannerSignal {

    /**
     * Signal strength (0-15) this scanner should emit toward `direction`,
     * based on the nearest detected deposit in that direction. 0 if
     * nothing detected there.
     */
    int tfmgtweaks$getSignalForDirection(Direction direction);
}
