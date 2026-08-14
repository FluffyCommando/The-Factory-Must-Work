package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.engines.types.AbstractSmallEngineBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Engines crash with an IllegalArgumentException while a
 * Create contraption carrying the engine is being assembled.
 *
 * Root cause: hasTwoShafts() calls getValue(ENGINE_STATE) for a
 * neighboring segment's position without checking it's still an engine
 * block -- during assembly that position can legitimately be air for a
 * tick, and Minecraft's getValue() throws for a missing property rather
 * than returning null.
 *
 * Fix: redirects getValue() calls in the method to check hasProperty()
 * first and return null instead of throwing, which correctly falls
 * through to "not a valid second shaft." Also guards a getControllerBE()
 * call that can be null for the same reason.
 */
@Mixin(AbstractSmallEngineBlockEntity.class)
public abstract class AbstractSmallEngineBlockEntityMixin {

    @Inject(method = "hasTwoShafts", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullController(CallbackInfoReturnable<Boolean> cir) {
        AbstractSmallEngineBlockEntity self = (AbstractSmallEngineBlockEntity) (Object) this;
        if (!self.isController() && self.getControllerBE() == null) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = "hasTwoShafts",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;"
                + "getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"))
    private Comparable<?> tfmgtweaks$safeGetEngineStateProperty(BlockState state, Property<?> property) {
        return state.hasProperty(property) ? state.getValue(property) : null;
    }
}
