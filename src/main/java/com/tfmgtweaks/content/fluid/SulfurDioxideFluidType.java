package com.tfmgtweaks.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

/** Fluid type for Sulfur Dioxide, reusing TFMG's carbon dioxide texture with its own tint. */
public class SulfurDioxideFluidType extends FluidType {
    private static final ResourceLocation GAS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("tfmg", "fluid/carbon_dioxide");
    private static final int TINT_COLOR = 0xB0C8D23C;

    public SulfurDioxideFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return GAS_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return GAS_TEXTURE;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return TINT_COLOR;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return TINT_COLOR;
            }
        });
    }
}
