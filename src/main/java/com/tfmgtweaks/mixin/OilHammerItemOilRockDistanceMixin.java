package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.OilHammerItem;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.registry.TFMGTweaksBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports how close the nearest Oil Rock is (exact coordinates in debug mode) when using the Oil Hammer. */
@Mixin(OilHammerItem.class)
public abstract class OilHammerItemOilRockDistanceMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void tfmgtweaks$reportNearestOilRock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide() || player == null) {
            return;
        }

        BlockPos clicked = context.getClickedPos();
        BlockPos nearest = tfmgtweaks$findNearestOilRock(level, clicked);
        if (nearest == null) {
            player.displayClientMessage(
                    Component.translatable("tfmgtweaks.oil_hammer.no_oil_rock")
                            .withStyle(ChatFormatting.GRAY),
                    true);
            return;
        }

        if (TFMGTweaksConfig.OIL_HAMMER_DEBUG_MODE.get()) {
            player.displayClientMessage(
                    Component.translatable("tfmgtweaks.oil_hammer.debug_coords",
                                    nearest.getX(), nearest.getY(), nearest.getZ())
                            .withStyle(ChatFormatting.AQUA),
                    true);
            return;
        }

        long distance = Math.round(Math.sqrt(nearest.distSqr(clicked)));
        player.displayClientMessage(tfmgtweaks$traceMessage(distance), true);
    }

    private static Component tfmgtweaks$traceMessage(long distance) {
        if (distance <= 20) {
            return Component.translatable("tfmgtweaks.oil_hammer.trace.strong").withStyle(ChatFormatting.GOLD);
        } else if (distance <= 50) {
            return Component.translatable("tfmgtweaks.oil_hammer.trace.moderate").withStyle(ChatFormatting.YELLOW);
        } else if (distance <= 100) {
            return Component.translatable("tfmgtweaks.oil_hammer.trace.faint").withStyle(ChatFormatting.GRAY);
        } else {
            return Component.translatable("tfmgtweaks.oil_hammer.trace.very_faint").withStyle(ChatFormatting.DARK_GRAY);
        }
    }

    /** Nearest Oil Rock in the clicked chunk within the configured height range. */
    private static BlockPos tfmgtweaks$findNearestOilRock(Level level, BlockPos clicked) {
        ChunkPos chunkPos = new ChunkPos(clicked);
        int minY = Math.min(TFMGTweaksConfig.OIL_ROCK_MIN_HEIGHT.get(), TFMGTweaksConfig.OIL_ROCK_MAX_HEIGHT.get());
        int maxY = Math.max(TFMGTweaksConfig.OIL_ROCK_MIN_HEIGHT.get(), TFMGTweaksConfig.OIL_ROCK_MAX_HEIGHT.get());
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;

        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).is(TFMGTweaksBlocks.OIL_ROCK.get())) {
                        continue;
                    }
                    double distSqr = cursor.distSqr(clicked);
                    if (distSqr < nearestDistSqr) {
                        nearestDistSqr = distSqr;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return nearest;
    }
}
