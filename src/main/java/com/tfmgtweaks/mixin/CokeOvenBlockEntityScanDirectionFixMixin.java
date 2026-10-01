package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.coke_oven.CokeOvenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Scans in the direction the oven multiblock actually extends, so a facing oven doesn't shut it off. */
@Mixin(CokeOvenBlockEntity.class)
public abstract class CokeOvenBlockEntityScanDirectionFixMixin {
    @Redirect(method = "updateOvenBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;I)Lnet/minecraft/core/BlockPos;"))
    private BlockPos tfmgtweaks$scanOppositeFacingNotFacing(BlockPos pos, Direction facing, int steps) {
        return pos.relative(facing.getOpposite(), steps);
    }
}
