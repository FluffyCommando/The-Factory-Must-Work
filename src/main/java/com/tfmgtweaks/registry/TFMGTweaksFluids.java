package com.tfmgtweaks.registry;

import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.content.fluid.BurningFuelBlock;
import com.tfmgtweaks.content.fluid.BurningFuelFlowingFluid;
import com.tfmgtweaks.content.fluid.BurningFuelFluidType;
import com.tfmgtweaks.content.fluid.SteamFluid;
import com.tfmgtweaks.content.fluid.SteamFluidType;
import com.tfmgtweaks.content.fluid.SulfurDioxideFluid;
import com.tfmgtweaks.content.fluid.SulfurDioxideFluidType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    /** Separate block register to avoid a circular reference with TFMGTweaksBlocks. */
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, TFMGTweaks.MOD_ID);

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

    /** Sulfur Dioxide, produced by the Air Intake. Pipeable only, never placeable. */
    public static final DeferredHolder<FluidType, FluidType> SULFUR_DIOXIDE_TYPE = FLUID_TYPES.register("sulfur_dioxide",
            () -> new SulfurDioxideFluidType(FluidType.Properties.create()
                    .descriptionId("fluid.tfmgtweaks.sulfur_dioxide")
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .canExtinguish(false)
                    .canConvertToSource(false)
                    .supportsBoating(false)
                    .density(-5)
                    .viscosity(200)
                    .lightLevel(0)
                    .temperature(300)));

    public static final DeferredHolder<Fluid, SulfurDioxideFluid> SULFUR_DIOXIDE_SOURCE =
            FLUIDS.register("sulfur_dioxide", () -> SulfurDioxideFluid.createSource(sulfurDioxideProperties()));

    public static final DeferredHolder<Fluid, SulfurDioxideFluid> SULFUR_DIOXIDE_FLOWING =
            FLUIDS.register("flowing_sulfur_dioxide", () -> SulfurDioxideFluid.createFlowing(sulfurDioxideProperties()));

    private static BaseFlowingFluid.Properties sulfurDioxideProperties() {
        return new BaseFlowingFluid.Properties(SULFUR_DIOXIDE_TYPE::get, SULFUR_DIOXIDE_SOURCE::get, SULFUR_DIOXIDE_FLOWING::get)
                .bucket(() -> TFMGTweaksItems.SULFUR_DIOXIDE_BUCKET.get());
    }

    /** Burning fuel is a real placeable fluid; its bucket picks up existing burning fuel like a lava bucket. */
    public static final DeferredHolder<FluidType, FluidType> BURNING_FUEL_TYPE = FLUID_TYPES.register("burning_fuel",
            () -> new BurningFuelFluidType(FluidType.Properties.create()
                    .descriptionId("fluid.tfmgtweaks.burning_fuel")
                    .canSwim(false)
                    .canDrown(false)
                    .lightLevel(15)
                    .density(3000)
                    .viscosity(3000)
                    .temperature(1500)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BURNING_FUEL_SOURCE =
            FLUIDS.register("burning_fuel", () -> new BaseFlowingFluid.Source(burningFuelProperties()));

    public static final DeferredHolder<Fluid, BurningFuelFlowingFluid> BURNING_FUEL_FLOWING =
            FLUIDS.register("flowing_burning_fuel", () -> new BurningFuelFlowingFluid(burningFuelProperties()));

    /** Liquid block for burning fuel, modeled on lava; light comes from BurningFuelFluidType. */
    public static final DeferredHolder<Block, BurningFuelBlock> BURNING_FUEL_BLOCK = BLOCKS.register("burning_fuel",
            () -> new BurningFuelBlock(BURNING_FUEL_SOURCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)));

    /** Matches TFMG's liquid fuels (slope find distance 5, explosion resistance 100). */
    private static BaseFlowingFluid.Properties burningFuelProperties() {
        return new BaseFlowingFluid.Properties(BURNING_FUEL_TYPE::get, BURNING_FUEL_SOURCE::get, BURNING_FUEL_FLOWING::get)
                .bucket(() -> TFMGTweaksItems.BURNING_FUEL_BUCKET.get())
                .block(BURNING_FUEL_BLOCK::get)
                .slopeFindDistance(5)
                .levelDecreasePerBlock(1)
                .explosionResistance(100f);
    }
}
