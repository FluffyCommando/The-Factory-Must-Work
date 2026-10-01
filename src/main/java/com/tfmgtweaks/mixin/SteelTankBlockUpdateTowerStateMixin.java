package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlock;
import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cancels updateTowerState() when the controller is null, e.g. during contraption assembly. */
@Mixin(SteelTankBlock.class)
public abstract class SteelTankBlockUpdateTowerStateMixin {
    @Inject(method = "updateTowerState", at = @At("HEAD"), cancellable = true)
    private static void tfmgtweaks$guardNullController(Level pLevel, BlockPos tankPos, boolean assemble,
                                                         boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity be = pLevel.getBlockEntity(tankPos);
        if (!(be instanceof SteelTankBlockEntity tankBE)) {
            return;
        }
        if (tankBE.getControllerBE() == null) {
            cir.setReturnValue(false);
        }
    }
}
