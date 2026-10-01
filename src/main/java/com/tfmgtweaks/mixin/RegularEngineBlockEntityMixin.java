package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.engines.types.regular_engine.RegularEngineBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Shows "Cylinders Missing" instead of "Pistons Missing" while assembling a regular engine. */
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
