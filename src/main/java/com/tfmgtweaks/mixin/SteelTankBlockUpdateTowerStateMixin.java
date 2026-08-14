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

/**
 * SteelTankBlock.updateTowerState() calls
 * getControllerBE() up to five times without checking for null, which it
 * legitimately can be -- e.g. while a Create contraption carrying a
 * distillation tower is being assembled/disassembled. Both reported
 * crash variants are different lines hitting this same unguarded
 * pattern.
 *
 * Fix: early-cancel guard so none of the five unsafe call sites run when
 * the controller is unavailable, rather than patching each individually.
 * updateTowerState is static, so its tankBE lookup is recomputed from
 * the method's own parameters rather than needing local capture.
 */
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
