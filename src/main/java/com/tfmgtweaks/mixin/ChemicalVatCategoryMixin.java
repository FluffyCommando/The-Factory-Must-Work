package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.recipes.VatMachineRecipe;
import com.drmangotea.tfmg.recipes.jei.ChemicalVatCategory;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vat recipes with a minimum vat size (minSize) never
 * show that requirement in JEI -- draw() already renders heat level the
 * same way, just never does the same for minSize.
 *
 * Adds that missing text, position/color matching the reporter's own
 * tested fix from the issue.
 */
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
        // Position/color match the reporter's own tested fix.
        graphics.drawString(Minecraft.getInstance().font, "Min. Size: " + recipe.minSize, 106, 9, 16579836);
    }
}
