package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.recipes.VatMachineRecipe;
import com.drmangotea.tfmg.recipes.jei.ChemicalVatCategory;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows the vat recipe's minimum vat size in JEI. */
@Mixin(ChemicalVatCategory.class)
public abstract class ChemicalVatCategoryMixin {
    @Inject(
        method = "draw(Lcom/drmangotea/tfmg/recipes/VatMachineRecipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V",
        at = @At("TAIL"))
    private void tfmgtweaks$drawMinimumVatSize(VatMachineRecipe recipe, IRecipeSlotsView iRecipeSlotsView,
                                                GuiGraphics graphics, double mouseX, double mouseY, CallbackInfo ci) {
        if (recipe.minSize <= 0) {
            return;
        }
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("tfmgtweaks.jei.vat.min_size", recipe.minSize), 106, 9, 16579836);
    }
}
