package com.tfmgtweaks.registry;

import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.content.fluid.SteamFluid;
import com.tfmgtweaks.content.fluid.SteamFluidType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class TFMGTweaksFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TFMGTweaks.MOD_ID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, TFMGTweaks.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
            () -> new SteamFluidType(FluidType.Properties.create()
                    .descriptionId("fluid.tfmgtweaks.steam")
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .canExtinguish(false)
                    .canConvertToSource(false)
                    .supportsBoating(false)
                    .density(-10)
                    .viscosity(200)
                    .lightLevel(0)
                    .temperature(400)));

    public static final DeferredHolder<Fluid, SteamFluid> STEAM_SOURCE =
            FLUIDS.register("steam", () -> SteamFluid.createSource(steamProperties()));

    public static final DeferredHolder<Fluid, SteamFluid> STEAM_FLOWING =
            FLUIDS.register("flowing_steam", () -> SteamFluid.createFlowing(steamProperties()));

    private static BaseFlowingFluid.Properties steamProperties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE::get, STEAM_SOURCE::get, STEAM_FLOWING::get)
                .bucket(() -> TFMGTweaksItems.STEAM_BUCKET.get());
    }
}
