package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.winding_machine.WindingMachineBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Treats a missing spool amount as 0 instead of null, fixing a server crash loop. */
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
