package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.engines.types.regular_engine.RegularEngineBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops an engine's installed components when it's broken. */
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
