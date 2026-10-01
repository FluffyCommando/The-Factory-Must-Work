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
 * Fixes a crash when an engine is assembled onto a contraption: hasTwoShafts() read ENGINE_STATE
 * from a neighbor that may not have it.
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
