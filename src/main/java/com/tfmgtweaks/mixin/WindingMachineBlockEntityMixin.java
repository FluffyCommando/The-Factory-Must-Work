package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.winding_machine.WindingMachineBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dedicated server crash loop caused by
 * tfmg:winding_machine.
 *
 * Root cause: performRecipe uses getOrDefault(SPOOL_AMOUNT, default)
 * everywhere except two spots that call the raw get() instead. If the
 * spool item doesn't carry that component, get() returns null, and the
 * surrounding arithmetic auto-unboxes it -- NPE every tick.
 *
 * Fix: redirect every get(DataComponentType) call in performRecipe to
 * fall back to 0 instead of null, matching the pattern used elsewhere.
 */
@Mixin(WindingMachineBlockEntity.class)
public abstract class WindingMachineBlockEntityMixin {

    @Redirect(
        method = "performRecipe",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object tfmgtweaks$safeGetSpoolAmount(ItemStack stack, DataComponentType<?> component) {
        Object value = stack.get(component);
        return value != null ? value : Integer.valueOf(0);
    }
}
