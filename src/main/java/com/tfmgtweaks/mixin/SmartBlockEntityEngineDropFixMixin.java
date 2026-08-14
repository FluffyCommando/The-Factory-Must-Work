package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.engines.types.regular_engine.RegularEngineBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Components placed into a regular or turbine engine
 * (TurbineEngineBlockEntity extends RegularEngineBlockEntity) are lost
 * entirely when the engine is broken or wrenched up.
 *
 * Root cause: neither class overrides destroy() -- pistonInventory is a
 * plain field, never registered as a Create Behaviour, and
 * SmartBlockEntity#destroy() only drops contents for behaviours it knows
 * about. Compare WindingMachineBlockEntity, which correctly overrides
 * destroy() itself.
 *
 * Since RegularEngineBlockEntity doesn't declare destroy() itself, this
 * mixins into SmartBlockEntity#destroy() directly and scopes the added
 * behavior with an instanceof check.
 */
@Mixin(SmartBlockEntity.class)
public abstract class SmartBlockEntityEngineDropFixMixin {

    @Inject(method = "destroy", at = @At("TAIL"))
    private void tfmgtweaks$dropEngineComponents(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof RegularEngineBlockEntity engine)) {
            return;
        }
        Level level = engine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (engine.pistonInventory == null) {
            return;
        }
        ItemHelper.dropContents(level, engine.getBlockPos(), engine.pistonInventory);
    }
}
