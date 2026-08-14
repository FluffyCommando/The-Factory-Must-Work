package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.base.ElectricalNetwork;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/**
 * IElectric is the shared base for essentially every electric block.
 * Three of its default methods -- getOrCreateElectricNetwork(),
 * onRemoved(), setNetwork() -- all call
 * ElectricNetworkManager.networks.get(getLevelAccessor()) with no null
 * check, even though ElectricNetworkManager's own
 * getOrCreateNetworkFor() uses computeIfAbsent() for this identical map,
 * proving it needs the guard. A null here (any timing edge case around
 * level load/unload) NPE-crashes electric block placement/removal.
 *
 * Fix: redirect these Map.get() calls to the same null-safe
 * computeIfAbsent pattern used correctly elsewhere.
 */
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
