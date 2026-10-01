package com.tfmgtweaks.content.fluid;

import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

public class BurningFuelBlock extends LiquidBlock {
    public BurningFuelBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(24) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
        }

        if (TFMGTweaksConfig.FLUID_IGNITION_PARTICLES_PER_TICK.get() <= 0) {
            return;
        }
        FluidState fluidState = level.getFluidState(pos);
        // Uses the fluid's real surface height for Flowing Fluids compatibility.
        double surfaceHeight = fluidState.getHeight(level, pos);
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
        double y = pos.getY() + surfaceHeight;
        double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;

        level.addAlwaysVisibleParticle(ParticleTypes.FLAME, true, x, y, z, 0.0, 0.01, 0.0);
        if (random.nextInt(4) == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, x, y, z, 0.0, 0.01, 0.0);
        }
    }
}
