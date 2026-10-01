package com.tfmgtweaks.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

/** Fluid type for burning fuel, which replaces ignited flammable fluid. */
public class BurningFuelFluidType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("tfmgtweaks", "block/burning_fuel_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("tfmgtweaks", "block/burning_fuel_flow");

    private static final int FIRE_TICKS_ON_CONTACT = 60;

    public BurningFuelFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return -1;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return -1;
            }
        });
    }

    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), FIRE_TICKS_ON_CONTACT));
        return false;
    }

    @Override
    public boolean canExtinguish(Entity entity) {
        return false;
    }
}
