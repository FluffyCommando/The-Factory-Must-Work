package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.base.ElectricalNetwork;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/** Null-safe network lookups, fixing crashes when placing or removing electric blocks. */
@Mixin(IElectric.class)
public interface IElectricNetworksNullSafetyMixin {
    @Redirect(
        method = {"getOrCreateElectricNetwork", "onRemoved", "setNetwork"},
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    default Object tfmgtweaks$safeNetworksGet(Map<LevelAccessor, Map<Long, ElectricalNetwork>> map, Object key) {
        LevelAccessor level = (LevelAccessor) key;
        return map.computeIfAbsent(level, $ -> new HashMap<>());
    }
}
