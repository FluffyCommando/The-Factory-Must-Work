package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.BiConsumer;

/** Skips the hardcoded 4000 mB total fluid output check, which concrete's 32000 mB output can never pass. */
@Mixin(VatBlockEntity.class)
public abstract class VatBlockEntityOutputCapCheckFixMixinVanilla {
    @Redirect(method = "getMatchingRecipe", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private void tfmgtweaks$skipOutputCapCheck(Map instance, BiConsumer action) {
    }
}
