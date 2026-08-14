package com.tfmgtweaks.content.boiler;

import com.mojang.serialization.MapCodec;
import com.tfmgtweaks.registry.TFMGTweaksBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * A dedicated boiler: fed water from any side, converts it to Steam over
 * time when placed above a heat source (detected the same way TFMG's own
 * Vat detects heat). Steam is drained from the top.
 *
 * Stacks vertically into one multiblock, matching TFMG's own Steel Tank
 * convention -- see BoilerBlockEntity for the controller/member design.
 * setPlacedBy triggers an immediate scan on a newly placed block;
 * neighborChanged triggers a scan on any existing block whenever its
 * up/down neighbor changes (covers both a new block joining and an
 * existing one being removed, since vanilla guarantees this fires after
 * the neighbor's actual state has changed). BoilerBlockEntity also
 * periodically self-heals in case a neighbor's chunk wasn't loaded yet
 * at the exact moment one of these fired.
 *
 * Built as a dedicated machine rather than a mixer recipe, since Steam
 * is a non-placeable, no-bucket fluid that doesn't play well with
 * certain recipe-output paths or filter-based pipe extraction.
 */
public class BoilerBlock extends BaseEntityBlock {

    public static final MapCodec<BoilerBlock> CODEC = simpleCodec(BoilerBlock::new);

    public BoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof BoilerBlockEntity be) {
            be.scanAndUpdateStructure();
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        if (neighborPos.equals(pos.above()) || neighborPos.equals(pos.below())) {
            if (level.getBlockEntity(pos) instanceof BoilerBlockEntity be) {
                be.scanAndUpdateStructure();
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTicker(level, type);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level, BlockEntityType<T> type) {
        if (type != TFMGTweaksBlockEntities.BOILER.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<BoilerBlockEntity>)
                (lvl, pos, blockState, blockEntity) -> blockEntity.tick();
    }
}
