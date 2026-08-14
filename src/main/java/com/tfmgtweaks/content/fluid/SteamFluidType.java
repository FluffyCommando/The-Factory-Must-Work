package com.tfmgtweaks.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

/**
 * Steam needed a dedicated FluidType subclass to override
 * initializeClient() -- the actual hook for a fluid's client-side
 * rendering (texture, tint, fog). Plain `new FluidType(...)` has no
 * client rendering setup at all. Overrides match TFMG's own GasFluidType.
 *
 * Reuses vanilla's still/flowing water textures rather than a new
 * animated one -- a light, pale tint is what actually differentiates
 * Steam visually.
 */
public class SteamFluidType extends FluidType {

    private static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final int TINT_COLOR = 0xB0E8E8E8;

    public SteamFluidType(Properties properties) {
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
                return TINT_COLOR;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return TINT_COLOR;
            }
        });
    }
}
