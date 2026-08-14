package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.engines.types.regular_engine.RegularEngineBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * While assembling a regular/radial engine, the goggle
 * tooltip for the final missing component says "Pistons Missing" -- but
 * the item it's waiting for is an Engine Cylinder, not any piston.
 *
 * Root cause: the lang key is built from "pistons" for the non-turbine
 * branch, which is just the wrong string -- it should be "cylinders",
 * and that lang key is shipped alongside this fix.
 *
 * Intercepts the argument right before the call so only "pistons" is
 * changed; "turbines" passes through untouched.
 */
@Mixin(RegularEngineBlockEntity.class)
public abstract class RegularEngineBlockEntityMixin {

    @ModifyArg(
        method = "addToGoggleTooltip",
        at = @At(value = "INVOKE",
            target = "Lcom/drmangotea/tfmg/base/lang/TFMGTexts$Engine;"
                + "lastRequirement(Ljava/lang/String;)Lnet/createmod/catnip/lang/LangBuilder;"))
    private String tfmgtweaks$fixCylinderMissingMessage(String type) {
        return "pistons".equals(type) ? "cylinders" : type;
    }
}
