package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.drmangotea.tfmg.mixin.accessor.TankSegmentAccessor;
import com.drmangotea.tfmg.recipes.VatMachineRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.tfmgtweaks.vat.VatInputOnlyFluidWrapper;
import com.tfmgtweaks.vat.VatInputOnlyItemWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Restricts item and fluid insertion to the vat's input side and invalidates capabilities on first output.
 * Prefers the most specific matching recipe and re-checks it every lazyTick.
 */
@Mixin(VatBlockEntity.class)
public abstract class VatBlockEntityCapabilityFixMixin {
    @Unique
    private boolean tfmgtweaks$hasEverHadOutput = false;

    @Inject(method = "getNewItemCapability", at = @At("RETURN"), cancellable = true)
    private void tfmgtweaks$restrictItemInsertToInput(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        if (self.isController()) {
            cir.setReturnValue(new VatInputOnlyItemWrapper(self.inputInventory, self.outputInventory));
        }
    }

    @Inject(method = "getNewFluidCapability", at = @At("RETURN"), cancellable = true)
    private void tfmgtweaks$restrictFluidFillToInput(CallbackInfoReturnable<IFluidHandler> cir) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        if (!self.isController()) {
            return;
        }
        IFluidHandler inputHandler = self.inputTank.getCapability();
        IFluidHandler outputHandler = self.outputTank.getCapability();
        if (inputHandler == null || outputHandler == null) {
            return;
        }
        cir.setReturnValue(new VatInputOnlyFluidWrapper(inputHandler, outputHandler));
    }

    @Inject(method = "handleRecipe", at = @At("TAIL"))
    private void tfmgtweaks$invalidateOnRecipeOutput(CallbackInfo ci) {
        tfmgtweaks$maybeInvalidate();
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void tfmgtweaks$invalidateAfterLoad(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        tfmgtweaks$maybeInvalidate();
    }

    private void tfmgtweaks$maybeInvalidate() {
        if (tfmgtweaks$hasEverHadOutput) {
            return;
        }
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        if (!self.isController() || self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        for (SmartFluidTankBehaviour.TankSegment segment : self.outputTank.getTanks()) {
            SmartFluidTank tank = ((TankSegmentAccessor) segment).tfmg$tank();
            if (!tank.getFluid().isEmpty()) {
                tfmgtweaks$hasEverHadOutput = true;
                self.getLevel().invalidateCapabilities(self.getBlockPos());
                return;
            }
        }
    }

    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$reevaluateRecipeEveryLazyTick(CallbackInfo ci) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        if (self.isController() && self.getLevel() != null && !self.getLevel().isClientSide) {
            self.recipe = self.getMatchingRecipe();
        }
    }

    @Redirect(method = "getMatchingRecipe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/recipe/RecipeFinder;get(Ljava/lang/Object;Lnet/minecraft/world/level/Level;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<RecipeHolder<? extends Recipe<?>>> tfmgtweaks$sortBySpecificity(
            Object cacheKey, net.minecraft.world.level.Level level, java.util.function.Predicate<RecipeHolder<? extends Recipe<?>>> conditions) {
        List<RecipeHolder<? extends Recipe<?>>> list = new ArrayList<>(
                com.simibubi.create.foundation.recipe.RecipeFinder.get(cacheKey, level, conditions));
        list.sort(Comparator.comparingInt(
                (RecipeHolder<? extends Recipe<?>> holder) -> ((VatMachineRecipe) holder.value()).getIngredients().size()
        ).reversed());
        return list;
    }
}
