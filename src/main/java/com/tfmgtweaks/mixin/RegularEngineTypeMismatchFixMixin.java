package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.engines.types.AbstractSmallEngineBlockEntity;
import com.drmangotea.tfmg.content.engines.types.regular_engine.RegularEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Same bug class as the vat/tank mixed-type merge, in a different system:
 * TFMG's own AbstractSmallEngineBlockEntity.connect() -- the method that
 * chains adjacent engine blocks into one multi-engine structure -- walks
 * outward from the controller and absorbs every block that's an
 * `instanceof AbstractSmallEngineBlockEntity` facing the same way, with
 * no check that the engine TYPE matches. RegularEngineBlockEntity's own
 * EngineType (I, V, W, U, BOXER, RADIAL, TURBINE -- different piston
 * arrangements and different speed/torque/efficiency modifiers) is
 * stored as a `type` field on the shared RegularEngineBlockEntity class,
 * not encoded as separate blocks for every variant, so nothing besides
 * this check would ever catch two different engine types being chained
 * into the same structure.
 *
 * TFMG's own source already contains the fix for this, complete and
 * correctly designed, just commented out and never re-enabled:
 *
 *   //if (be instanceof RegularEngineBlockEntity be1 && this instanceof RegularEngineBlockEntity be2 && be1.type != be2.type) {
 *   //    setBlockStates(this, getBlockPos().relative(updateDirection, i - 1));
 *   //    TFMG.LOGGER.debug("set blockstates");
 *   //    return;
 *   //}
 *
 * Confirmed identical in Community Edition via bytecode disassembly:
 * same two Level.getBlockEntity(BlockPos) call sites, same single
 * detashEngines() call, same missing check.
 *
 * Rather than trying to re-insert this exact check mid-loop (which
 * would need local variable capture -- the loop index, the candidate
 * block entity, and the scan direction are all local variables inside
 * connect(), not fields, and getting their exact bytecode slot order
 * right without introducing a startup-crashing mixin failure isn't
 * something to get low-confidence about), this instead redirects the
 * SECOND of connect()'s two Level.getBlockEntity(BlockPos) calls -- the
 * one inside the main scanning loop, which is what actually produces
 * the unchecked candidate. If the found block entity is a
 * RegularEngineBlockEntity whose type doesn't match the scan's own
 * origin, this returns null instead of the real result. Since
 * `instanceof AbstractSmallEngineBlockEntity` on null is simply false,
 * this makes the original method's own, already-correct "else" branch
 * -- which does exactly the same stop-and-setBlockStates cleanup as the
 * commented-out fix -- run naturally, without needing to duplicate any
 * of that logic here.
 *
 * Also applies to connect()'s first getBlockEntity() call (the
 * redirect-to-controller check), but harmlessly: that check already
 * requires the exact same Block, which a type mismatch would already
 * fail on its own, so this check can never actually change that
 * outcome -- it's just never reached there in practice.
 */
@Mixin(AbstractSmallEngineBlockEntity.class)
public abstract class RegularEngineTypeMismatchFixMixin {

    @Redirect(method = "connect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity tfmgtweaks$hideTypeMismatchedEngine(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RegularEngineBlockEntity candidate
                && (Object) this instanceof RegularEngineBlockEntity self
                && candidate.type != self.type) {
            return null;
        }
        return be;
    }
}
