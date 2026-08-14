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
 * Holds the pump jack's oil/waste/Steam state and exposes it as three
 * fully isolated IFluidHandler views (forOilOnly/forWasteOnly/
 * forSteamOnly), one per role a player can wrench a face to. Installed and
 * exposed directionally by PumpjackBaseBlockEntityFrackingMixin; faces are
 * reconfigured by PumpjackWrenchInteractionHandler.
 *
 * Every face starts unassigned; multiple faces can share the same role.
 */
public class PumpjackFrackingWrapper {

    /** Which function (if any) a given face is currently assigned to. */
    public enum FaceRole {
        NONE, OIL, WASTE, STEAM
    }

    private final PumpjackBaseBlockEntity pumpjack;
    private final IFluidHandler oilTank;

    public int wasteAmount = 0;
    public int steamAmount = 0;

    /** Per-face role assignment; absent means NONE. */
    private final Map<Direction, FaceRole> faceRoles = new EnumMap<>(Direction.class);

    public PumpjackFrackingWrapper(PumpjackBaseBlockEntity pumpjack, IFluidHandler oilTank) {
        this.pumpjack = pumpjack;
        this.oilTank = oilTank;
    }

    /** Sets a face's role during NBT load, without assignRole()'s sync/notify side effects. */
    public void loadRole(Direction face, FaceRole role) {
        if (role == FaceRole.NONE) {
            faceRoles.remove(face);
        } else {
            faceRoles.put(face, role);
        }
    }

    /** What role a specific face is currently playing, if any. */
    public FaceRole roleOf(Direction face) {
        return faceRoles.getOrDefault(face, FaceRole.NONE);
    }

    /**
     * Assigns a face to a new role. Saves, syncs to the client, invalidates
     * this position's capability, and notifies the neighboring pipe so it
     * re-checks its connection instead of staying stuck on stale state.
     */
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

    /** Forces the neighbor at this face to re-check its pipe connection. */
    private void notifyNeighbor(Direction face) {
        Level level = pumpjack.getLevel();
        if (level == null) {
            return;
        }
        BlockPos neighborPos = pumpjack.getBlockPos().relative(face);
        BlockState neighborState = level.getBlockState(neighborPos);

        // The actual fix: neighborChanged() below only triggers game LOGIC
        // (TFMG's own FluidPipeBlockMixin schedules a tick from it) -- it
        // never recomputes the pipe's own connection blockstate, which is
        // what its visual model is actually keyed on. Vanilla only ever
        // calls a neighbor's updateShape() automatically when the
        // originating block's own BLOCKSTATE changes -- but wrenching a
        // pump jack face only changes block-entity NBT, never the pump
        // jack's own blockstate, so that automatic propagation never
        // fires here at all. Calling updateShape() directly and applying
        // its result is the same thing vanilla does internally for a real
        // blockstate change; it recomputes and returns the neighbor's
        // correct connection state, which we then have to actually apply
        // ourselves via setBlock() since we're not going through the
        // normal setBlock()-triggers-neighbor-updates path.
        BlockState updatedNeighborState = neighborState.updateShape(
                face.getOpposite(), pumpjack.getBlockState(), level, neighborPos, pumpjack.getBlockPos());
        level.setBlock(neighborPos, updatedNeighborState, 3);

        level.neighborChanged(neighborState, neighborPos, pumpjack.getBlockState().getBlock(),
                pumpjack.getBlockPos(), false);
        FluidPropagator.propagateChangedPipe(level, neighborPos, updatedNeighborState);
    }

    /**
     * Notifies all 6 neighbors at once. Used when the client learns of a
     * role change via sync (see the mixin's read() injection), where only
     * the full resulting state is known, not which face(s) changed.
     */
    public void notifyAllNeighbors() {
        for (Direction direction : Direction.values()) {
            notifyNeighbor(direction);
        }
    }

    /**
     * Cycles a face through NONE -> OIL -> WASTE -> STEAM -> NONE. Returns
     * the role the face ends up with, for wrench feedback.
     */
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

    /** Exposed from whichever side is currently assigned to oil -- crude oil only, nothing else visible. */
    public IFluidHandler forOilOnly() {
        return new OilOnlyView();
    }

    /** Exposed from whichever side is currently assigned to waste -- waste only, nothing else visible. */
    public IFluidHandler forWasteOnly() {
        return new WasteOnlyView();
    }

    /** Exposed from whichever side is currently assigned to Steam -- Steam only, nothing else visible. */
    public IFluidHandler forSteamOnly() {
        return new SteamOnlyView();
    }

    /**
     * Exposed for null-context (non-side-specific) queries, e.g. TFMG's own
     * goggle tooltip. Shows all three tanks at once but is read-only, so it
     * can't be used to move fluid between them.
     */
    public IFluidHandler forDisplay() {
        return new DisplayOnlyView();
    }

    /**
     * Drains the Steam tank into fracking progress + waste over time,
     * rate-limited to a percentage of the Steam tank's own capacity per
     * tick rather than converting everything instantly. Waste is produced
     * at half the rate Steam is consumed; fracking progress tracks the
     * full amount of Steam consumed regardless.
     */
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
        wasteAmount += wasteProduced;
        steamAmount -= steamConsumed;
        pumpjack.setChanged();
        invalidate();
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

    /** Polluted Water if "Pollution of the Realms" is installed, otherwise plain Water. */
    private Fluid getWasteFluid() {
        Fluid pollutedWater = BuiltInRegistries.FLUID.get(
                ResourceLocation.fromNamespaceAndPath("adpother", "polluted_water_still"));
        return pollutedWater != Fluids.EMPTY ? pollutedWater : Fluids.WATER;
    }

    /** Crude oil (tank 0) only -- waste and Steam are entirely invisible through this view. */
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

    /** Waste byproduct (tank 0) only -- oil and Steam are entirely invisible through this view. */
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
                invalidate();
            }
            return new FluidStack(getWasteFluid(), amount);
        }
    }

    /** Steam (tank 0) only -- oil and waste are entirely invisible through this view. */
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

    /** Shows all three tanks together, purely for display -- fill()/drain() always reject. */
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
