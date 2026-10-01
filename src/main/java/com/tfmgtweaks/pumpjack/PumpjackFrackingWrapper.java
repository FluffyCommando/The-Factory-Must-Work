package com.tfmgtweaks.pumpjack;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.base.PumpjackBaseBlockEntity;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.content.oilrock.OilRockBlockEntity;
import com.tfmgtweaks.registry.TFMGTweaksFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the pump jack's oil, waste, and Steam state and exposes separate fluid handlers for each role.
 * Faces start unassigned, and multiple faces can share the same role.
 */
public class PumpjackFrackingWrapper {
    /** The fluid role currently assigned to a face. */
    public enum FaceRole {
        NONE, OIL, WASTE, STEAM
    }

    private final PumpjackBaseBlockEntity pumpjack;
    private final IFluidHandler oilTank;

    public int wasteAmount = 0;
    public int steamAmount = 0;

    /** Tracks whether waste has ever been produced. */
    private boolean hasEverHadWaste = false;

    /** Per-face fluid role assignments; absent means NONE. */
    private final Map<Direction, FaceRole> faceRoles = new EnumMap<>(Direction.class);

    public PumpjackFrackingWrapper(PumpjackBaseBlockEntity pumpjack, IFluidHandler oilTank) {
        this.pumpjack = pumpjack;
        this.oilTank = oilTank;
    }

    /** Loads a face role without triggering sync or neighbor updates. */
    public void loadRole(Direction face, FaceRole role) {
        if (role == FaceRole.NONE) {
            faceRoles.remove(face);
        } else {
            faceRoles.put(face, role);
        }
    }

    public FaceRole roleOf(Direction face) {
        return faceRoles.getOrDefault(face, FaceRole.NONE);
    }

    /** Assigns a role to a face, then syncs, invalidates capabilities and refreshes the neighboring pipe. */
    public void assignRole(Direction face, FaceRole role) {
        if (role == FaceRole.NONE) {
            faceRoles.remove(face);
        } else {
            faceRoles.put(face, role);
        }
        pumpjack.setChanged();
        pumpjack.sendData();
        invalidate();
        notifyNeighbor(face);
    }

    /** Recomputes and refreshes the neighboring pipe connection. */
    private void notifyNeighbor(Direction face) {
        Level level = pumpjack.getLevel();
        if (level == null) {
            return;
        }
        BlockPos neighborPos = pumpjack.getBlockPos().relative(face);
        BlockState neighborState = level.getBlockState(neighborPos);
        BlockState updatedNeighborState = neighborState.updateShape(
                face.getOpposite(), pumpjack.getBlockState(), level, neighborPos, pumpjack.getBlockPos());
        level.setBlock(neighborPos, updatedNeighborState, 3);

        level.neighborChanged(neighborState, neighborPos, pumpjack.getBlockState().getBlock(),
                pumpjack.getBlockPos(), false);
        FluidPropagator.propagateChangedPipe(level, neighborPos, updatedNeighborState);
    }

    /** Notifies all 6 neighbors, used when the client receives a role change. */
    public void notifyAllNeighbors() {
        for (Direction direction : Direction.values()) {
            notifyNeighbor(direction);
        }
    }

    /** Cycles a face through NONE -> OIL -> WASTE -> STEAM and returns the new role. */
    public FaceRole cycleRole(Direction face) {
        FaceRole current = roleOf(face);
        FaceRole next = switch (current) {
            case NONE -> FaceRole.OIL;
            case OIL -> FaceRole.WASTE;
            case WASTE -> FaceRole.STEAM;
            case STEAM -> FaceRole.NONE;
        };
        assignRole(face, next);
        return next;
    }

    /** Handler for faces assigned to oil. */
    public IFluidHandler forOilOnly() {
        return new OilOnlyView();
    }

    /** Handler for faces assigned to waste. */
    public IFluidHandler forWasteOnly() {
        return new WasteOnlyView();
    }

    /** Handler for faces assigned to Steam. */
    public IFluidHandler forSteamOnly() {
        return new SteamOnlyView();
    }

    /** Read-only view of all three tanks for side-less queries such as goggle tooltips. */
    public IFluidHandler forDisplay() {
        return new DisplayOnlyView();
    }

    /** Converts Steam into fracking progress and half as much waste, rate-limited per tick. */
    public void tickProcessing() {
        if (steamAmount <= 0 || !pumpjack.isRunning) {
            return;
        }
        OilRockBlockEntity oilRock = getConnectedOilRock();
        if (oilRock == null) {
            return;
        }
        int wasteCapacity = TFMGTweaksConfig.PUMPJACK_WASTE_WATER_CAPACITY.get();
        int roomForWaste = wasteCapacity - wasteAmount;
        int steamCapacity = TFMGTweaksConfig.PUMPJACK_STEAM_TANK_CAPACITY.get();
        int ratePercent = TFMGTweaksConfig.PUMPJACK_STEAM_PROCESSING_RATE_PERCENT.get();
        int perTickCap = Math.max(1, steamCapacity * ratePercent / 100);
        int maxSteamByWasteRoom = roomForWaste * 2;
        int steamConsumed = Math.min(Math.min(steamAmount, maxSteamByWasteRoom), perTickCap);
        if (steamConsumed <= 0) {
            return;
        }
        int wasteProduced = steamConsumed / 2;
        oilRock.addFrackingProgress(steamConsumed);
        boolean wasteWasEmpty = wasteAmount <= 0;
        wasteAmount += wasteProduced;
        steamAmount -= steamConsumed;
        pumpjack.setChanged();
        if (!hasEverHadWaste && wasteWasEmpty && wasteAmount > 0) {
            hasEverHadWaste = true;
            invalidate();
        }
    }

    private void invalidate() {
        Level level = pumpjack.getLevel();
        if (level != null) {
            level.invalidateCapabilities(pumpjack.getBlockPos());
        }
    }

    @Nullable
    private OilRockBlockEntity getConnectedOilRock() {
        if (pumpjack.deposit == null) {
            return null;
        }
        Level level = pumpjack.getLevel();
        if (level == null) {
            return null;
        }
        return level.getBlockEntity(pumpjack.deposit) instanceof OilRockBlockEntity be ? be : null;
    }

    private boolean isSteam(FluidStack stack) {
        return stack.getFluid().isSame(TFMGTweaksFluids.STEAM_SOURCE.get())
                || stack.getFluid().isSame(TFMGTweaksFluids.STEAM_FLOWING.get());
    }

    private Fluid steamFluid() {
        return TFMGTweaksFluids.STEAM_SOURCE.get();
    }

    /** Returns polluted water when available, otherwise regular water. */
    private Fluid getWasteFluid() {
        Fluid pollutedWater = BuiltInRegistries.FLUID.get(
                ResourceLocation.fromNamespaceAndPath("adpother", "polluted_water_still"));
        return pollutedWater != Fluids.EMPTY ? pollutedWater : Fluids.WATER;
    }

    /** Exposes only the crude oil tank. */
    private class OilOnlyView implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return oilTank.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return oilTank.getTankCapacity(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return oilTank.isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (isSteam(resource)) {
                return 0;
            }
            return oilTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return oilTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return oilTank.drain(maxDrain, action);
        }
    }

    /** Exposes only the waste tank. */
    private class WasteOnlyView implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return wasteAmount > 0 ? new FluidStack(getWasteFluid(), wasteAmount) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return TFMGTweaksConfig.PUMPJACK_WASTE_WATER_CAPACITY.get();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!resource.getFluid().isSame(getWasteFluid())) {
                return FluidStack.EMPTY;
            }
            return drainWaste(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return drainWaste(maxDrain, action);
        }

        private FluidStack drainWaste(int maxDrain, FluidAction action) {
            int amount = Math.min(maxDrain, wasteAmount);
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                wasteAmount -= amount;
                pumpjack.setChanged();
            }
            return new FluidStack(getWasteFluid(), amount);
        }
    }

    /** Exposes only the Steam tank. */
    private class SteamOnlyView implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return steamAmount > 0 ? new FluidStack(steamFluid(), steamAmount) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return TFMGTweaksConfig.PUMPJACK_STEAM_TANK_CAPACITY.get();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return isSteam(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isSteam(resource)) {
                return 0;
            }
            int capacity = TFMGTweaksConfig.PUMPJACK_STEAM_TANK_CAPACITY.get();
            int room = capacity - steamAmount;
            int amount = Math.min(resource.getAmount(), room);
            if (amount <= 0) {
                return 0;
            }
            if (action.execute()) {
                steamAmount += amount;
                pumpjack.setChanged();
            }
            return amount;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    /** Shows all three tanks for display; fluid transfer is disabled. */
    private class DisplayOnlyView implements IFluidHandler {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return oilTank.getFluidInTank(0);
            }
            if (tank == 1) {
                return wasteAmount > 0 ? new FluidStack(getWasteFluid(), wasteAmount) : FluidStack.EMPTY;
            }
            return steamAmount > 0 ? new FluidStack(steamFluid(), steamAmount) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return oilTank.getTankCapacity(0);
            }
            if (tank == 1) {
                return TFMGTweaksConfig.PUMPJACK_WASTE_WATER_CAPACITY.get();
            }
            return TFMGTweaksConfig.PUMPJACK_STEAM_TANK_CAPACITY.get();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
