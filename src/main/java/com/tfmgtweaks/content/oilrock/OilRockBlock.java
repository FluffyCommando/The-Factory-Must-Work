package com.tfmgtweaks.content.oilrock;

import com.mojang.serialization.MapCodec;
import com.tfmgtweaks.registry.TFMGTweaksBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.Nullable;

/** Oil Rock: an underground oil deposit that can be cracked with Steam for faster extraction. */
public class OilRockBlock extends BaseEntityBlock {
    public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
    public static final MapCodec<OilRockBlock> CODEC = simpleCodec(OilRockBlock::new);

    public OilRockBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(CRACKED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(CRACKED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OilRockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTicker(level, type);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level, BlockEntityType<T> type) {
        if (type != TFMGTweaksBlockEntities.OIL_ROCK.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<OilRockBlockEntity>)
                (lvl, pos, blockState, blockEntity) -> blockEntity.tick();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean genuinelyMined = !state.is(newState.getBlock()) && newState.isAir();
        super.onRemove(state, level, pos, newState, isMoving);
        if (genuinelyMined && !level.isClientSide) {
            Fluid crudeOil = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfmg", "crude_oil"));
            Fluid crudeOilSource = crudeOil instanceof FlowingFluid flowingFluid ? flowingFluid.getSource() : crudeOil;
            if (crudeOilSource != null) {
                level.setBlock(pos, crudeOilSource.defaultFluidState().createLegacyBlock(), 3);
            }
        }
    }
}

