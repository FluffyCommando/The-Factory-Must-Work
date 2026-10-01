package com.tfmgtweaks.integration.pollution;

import com.endertech.minecraft.forge.world.BiomeId;
import com.endertech.minecraft.mods.adpother.blocks.Pollutant;
import com.endertech.minecraft.mods.adpother.init.Pollutants;
import com.endertech.minecraft.mods.adpother.pollution.ChunkPollution;
import com.endertech.minecraft.mods.adpother.pollution.PollutionInfo;
import com.endertech.minecraft.mods.adpother.pollution.WorldData;
import com.tfmgtweaks.airintake.AirIntakeGasMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

/** Direct Pollution of the Realms calls; only reached through PollutionCompat. */
public final class PollutionIntegration {
    /** How far above and below settleY to scan for gas blocks. */
    private static final int SETTLE_LAYER_HALF_THICKNESS = 4;

    /** Below this quantity gas blocks are sparse, so the scan checks every block instead of striding. */
    private static final int SPARSE_QUANTITY_THRESHOLD = 50;

    /** X/Z stride above SPARSE_QUANTITY_THRESHOLD. */
    private static final int SCAN_STRIDE_DENSE = 2;

    /** X/Z stride at or below SPARSE_QUANTITY_THRESHOLD. */
    private static final int SCAN_STRIDE_SPARSE = 1;

    private PollutionIntegration() {
    }

    /** Reduces every pollutant in pos's chunk by up to amount, never below 0. */
    public static void reducePollutionNear(ServerLevel level, BlockPos pos, int amount) {
        if (amount <= 0) {
            return;
        }
        WorldData worldData = WorldData.getData(level);
        ChunkPollution chunkPollution = worldData.getChunkPollution(level, pos);
        chunkPollution.getInfos().forEach(info -> {
            int reduced = Math.max(0, info.getQuantity() - amount);
            if (reduced != info.getQuantity()) {
                info.setQuantity(reduced);
                info.markDirty();
            }
        });
    }

    /**
     * Spends up to desiredAmount of the pollutant matching mode from real gas blocks near settleY in chunkPos.
     * Returns how much was spent.
     */
    public static int spendPollutionInChunk(ServerLevel level, ChunkPos chunkPos, int settleY,
                                             AirIntakeGasMode mode, int desiredAmount) {
        if (desiredAmount <= 0 || !level.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }
        Pollutant pollutant = pollutantFor(mode);

        WorldData worldData = WorldData.getData(level);
        ChunkPollution chunkPollution = worldData.getChunkPollution(level, chunkPos.getMiddleBlockPosition(settleY));
        Optional<PollutionInfo> infoOpt = chunkPollution.getInfoFor(pollutant);
        if (infoOpt.isEmpty()) {
            return 0;
        }
        int trackedQuantity = infoOpt.get().getQuantity();
        if (trackedQuantity <= 0) {
            return 0;
        }
        int stride = trackedQuantity <= SPARSE_QUANTITY_THRESHOLD ? SCAN_STRIDE_SPARSE : SCAN_STRIDE_DENSE;

        int minY = settleY - SETTLE_LAYER_HALF_THICKNESS;
        int maxY = settleY + SETTLE_LAYER_HALF_THICKNESS;
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int remaining = desiredAmount;
        int consumed = 0;
        for (int y = minY; y <= maxY && remaining > 0; y++) {
            for (int x = minX; x <= minX + 15 && remaining > 0; x += stride) {
                for (int z = minZ; z <= minZ + 15 && remaining > 0; z += stride) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).is(pollutant)) {
                        continue;
                    }
                    int spent = pollutant.spend(level, cursor, remaining);
                    consumed += spent;
                    remaining -= spent;
                }
            }
        }
        return consumed;
    }

    /** Pollution of the Realms' configured settle height for this gas at pos's biome. */
    public static int getGasSettleHeight(ServerLevel level, BlockPos pos, AirIntakeGasMode mode) {
        Pollutant pollutant = pollutantFor(mode);
        BiomeId biomeId = BiomeId.from(level, pos);
        return pollutant.getConcentrationAltitudeIn(biomeId);
    }

    /** AIR has no pollutant; only called in Carbon Dioxide or Sulfur Dioxide mode. */
    private static Pollutant pollutantFor(AirIntakeGasMode mode) {
        Pollutants.BuiltIn builtIn = switch (mode) {
            case CARBON_DIOXIDE -> Pollutants.BuiltIn.CARBON;
            case SULFUR_DIOXIDE -> Pollutants.BuiltIn.SULFUR;
            case AIR -> throw new IllegalArgumentException("AIR has no corresponding Pollutant");
        };
        return builtIn.get();
    }
}

