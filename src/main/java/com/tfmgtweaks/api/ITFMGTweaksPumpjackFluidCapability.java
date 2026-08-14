package com.tfmgtweaks.api;

import com.tfmgtweaks.pumpjack.PumpjackFrackingWrapper;

/**
 * Implemented by PumpjackBaseBlockEntityFrackingMixin. The capability
 * registration lambda needs to call into the shared PumpjackFrackingWrapper
 * core from an arbitrary PumpjackBaseBlockEntity instance, which a plain
 * field access or mixin-merged method can't do at compile time -- routing
 * through a real interface sidesteps that.
 */
public interface ITFMGTweaksPumpjackFluidCapability {

    PumpjackFrackingWrapper tfmgtweaks$getFrackingCore();
}
