package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.coke_oven.CokeOvenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reported bug: placing a second Coke Oven facing an existing one shuts
 * the first one off, every time, unless they're at least
 * cokeOvenMaxSize blocks apart (the config default) -- but only on
 * initial placement; both work correctly again after a reload.
 *
 * Root cause: CokeOvenBlockEntity.createMultiblock() consistently
 * extends the structure upward and in the *opposite* of the oven's own
 * facing direction (`.relative(facing.getOpposite(), i)`, used three
 * times within that same method) -- but updateOvenBlocks(), which
 * re-evaluates every potentially-affected oven whenever one is placed
 * or removed, scans downward and in the oven's own facing direction
 * instead (`.relative(facing, maxSize)`, no getOpposite()) -- the one
 * inconsistent usage in this class. That scan range, up to
 * cokeOvenMaxSize blocks in the wrong direction, is exactly wide enough
 * to reach a second oven facing back toward the first (since two ovens
 * "facing each other" sit along the first oven's own facing direction),
 * pulling in an otherwise-unrelated oven and forcing its own
 * createMultiblock() to needlessly re-run -- which resets its door
 * animation state (forceOpen, doorAngle) even though the oven wasn't
 * actually part of any structure that changed.
 *
 * Fix: redirect this one BlockPos.relative(Direction, int) call within
 * updateOvenBlocks() to use facing.getOpposite(), matching every other
 * use of this pattern in the same class -- so the scan only reaches
 * positions createMultiblock() could actually incorporate into a
 * structure, not an oven facing back the other way.
 */
@Mixin(CokeOvenBlockEntity.class)
public abstract class CokeOvenBlockEntityScanDirectionFixMixin {

    @Redirect(method = "updateOvenBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;I)Lnet/minecraft/core/BlockPos;"))
    private BlockPos tfmgtweaks$scanOppositeFacingNotFacing(BlockPos pos, Direction facing, int steps) {
        return pos.relative(facing.getOpposite(), steps);
    }
}
