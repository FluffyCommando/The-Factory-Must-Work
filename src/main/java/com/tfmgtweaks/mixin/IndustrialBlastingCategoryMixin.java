package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.datagen.recipes.TFMGRecipeProvider;
import com.drmangotea.tfmg.recipes.IndustrialBlastingRecipe;
import com.drmangotea.tfmg.recipes.jei.IndustrialBlastingCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows the hot air requirement of industrial blasting recipes in JEI. */
@Mixin(IndustrialBlastingCategory.class)
public abstract class IndustrialBlastingCategoryMixin {
    @Inject(
        method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lcom/drmangotea/tfmg/recipes/IndustrialBlastingRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",
        at = @At("TAIL"))
    private void tfmgtweaks$showHotAirRequirement(IRecipeLayoutBuilder builder, IndustrialBlastingRecipe recipe,
                                                   IFocusGroup focuses, CallbackInfo ci) {
        if (recipe.hotAirUsage > 0) {
            CreateRecipeCategory.addFluidSlot(builder, 90, 13,
                    new FluidStack(TFMGRecipeProvider.F.hotAir(), recipe.hotAirUsage));
        }
    }
}
