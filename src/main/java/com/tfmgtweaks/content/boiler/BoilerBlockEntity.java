package com.tfmgtweaks.content.boiler;

import com.drmangotea.tfmg.base.lang.TFMGTexts;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.registry.TFMGTweaksBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Water in (any side, any segment), Steam out (top face of the topmost
 * segment only, matching how gases settle above liquids in TFMG's own
 * fluid system), heated from below the bottom segment via Create's own
 * BoilerHeater.findHeat().
 *
 * Stacks vertically into one multiblock, matching TFMG's own Steel Tank
 * convention -- the bottom block of a contiguous vertical run becomes the
 * controller and holds all real state (water/steam amounts); every other
 * block is a member that delegates to it. Both tank capacity and Steam
 * production rate scale linearly with height, capped at
 * TFMGTweaksConfig.BOILER_MAX_HEIGHT.
 *
 * Structure detection is both event-driven (immediate, via
 * BoilerBlock.setPlacedBy/onRemove for responsiveness) and periodically
 * self-healing (every 20 ticks from tick(), so a stale controller pointer
 * after a world/chunk reload -- the exact same class of bug fixed
 * elsewhere in this mod this session -- corrects itself rather than
 * needing the block broken and replaced).
 */
public class BoilerBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    private static final int RESCAN_INTERVAL_TICKS = 20;

    /** Null if this block IS the controller; otherwise the position of whichever block holds the real state. */
    @Nullable
    private BlockPos controller;

    /** Only meaningful on the controller: how many blocks tall the current stack is, capped at the configured max. */
    private int height = 1;

    private int water = 0;
    private int steam = 0;

    private int tfmgtweaks$rescanTimer = 0;

    private final IFluidHandler topFluidCapability = new BoilerFluidHandler(true);
    private final IFluidHandler sideFluidCapability = new BoilerFluidHandler(false);

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(TFMGTweaksBlockEntities.BOILER.get(), pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                TFMGTweaksBlockEntities.BOILER.get(),
                (be, context) -> {
                    BoilerBlockEntity controllerBE = be.getControllerBE();
                    if (controllerBE == null) {
                        return null;
                    }
                    return context == Direction.UP && be.isTopmostMember()
                            ? controllerBE.topFluidCapability : controllerBE.sideFluidCapability;
                }
        );
    }

    private static Fluid steamFluid() {
        return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfmgtweaks", "steam"));
    }

    /** Resolves to the block holding this stack's shared state; self-promotes if the stored controller is gone. */
    @Nullable
    private BoilerBlockEntity getControllerBE() {
        if (controller == null) {
            return this;
        }
        if (level == null) {
            return null;
        }
        if (level.getBlockEntity(controller) instanceof BoilerBlockEntity controllerBE) {
            return controllerBE;
        }
        // Stored controller is gone -- self-promote rather than staying broken.
        controller = null;
        setChanged();
        return this;
    }

    /** True if there's no other Boiler block directly above this one within the counted stack height. */
    private boolean isTopmostMember() {
        BoilerBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null || level == null) {
            return true;
        }
        BlockPos controllerPos = controllerBE.getBlockPos();
        int myOffset = getBlockPos().getY() - controllerPos.getY();
        return myOffset == controllerBE.height - 1;
    }

    private boolean isBoilerAt(BlockPos pos) {
        return level != null && level.getBlockState(pos).getBlock() instanceof BoilerBlock;
    }

    /**
     * Scans downward to find the bottom of the contiguous vertical run
     * (the controller), updates this block's own controller pointer, and
     * -- since a newly placed or newly re-scanned block can extend an
     * existing stack's height -- also asks the controller to re-count.
     */
    public void scanAndUpdateStructure() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos bottomPos = getBlockPos();
        while (isBoilerAt(bottomPos.below())) {
            bottomPos = bottomPos.below();
        }

        BlockPos newController = bottomPos.equals(getBlockPos()) ? null : bottomPos;
        if (!Objects.equals(controller, newController)) {
            controller = newController;
            setChanged();
        }

        if (newController == null) {
            recountOwnHeight();
        } else if (level.getBlockEntity(bottomPos) instanceof BoilerBlockEntity controllerBE) {
            controllerBE.recountOwnHeight();
        }
    }

    /** Only meaningful when called on the controller itself. */
    private void recountOwnHeight() {
        if (level == null) {
            return;
        }
        int maxHeight = TFMGTweaksConfig.BOILER_MAX_HEIGHT.get();
        int newHeight = 1;
        BlockPos scanPos = getBlockPos().above();
        while (newHeight < maxHeight && isBoilerAt(scanPos)) {
            newHeight++;
            scanPos = scanPos.above();
        }
        if (height != newHeight) {
            height = newHeight;
            setChanged();
        }
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        tfmgtweaks$rescanTimer++;
        if (tfmgtweaks$rescanTimer >= RESCAN_INTERVAL_TICKS) {
            tfmgtweaks$rescanTimer = 0;
            scanAndUpdateStructure();
        }

        if (controller != null) {
            // Only the controller processes; members hold no real state.
            return;
        }

        if (water <= 0) {
            return;
        }
        BlockPos belowPos = getBlockPos().below();
        BlockState belowState = level.getBlockState(belowPos);
        float heat = BoilerHeater.findHeat(level, belowPos, belowState);
        int heatLevel = (int) heat;
        if (heatLevel <= 0) {
            return;
        }

        int capacity = TFMGTweaksConfig.BOILER_TANK_CAPACITY.get() * height;
        // Steam is always exactly double the water consumed; water stays
        // the driving variable to avoid rounding mismatches.
        int steamRate = heatLevel * TFMGTweaksConfig.BOILER_STEAM_PER_HEAT_PER_TICK.get() * height;
        int maxWaterByRate = steamRate / 2;
        int maxWaterByRoom = (capacity - steam) / 2;
        int waterConsumed = Math.min(water, Math.min(maxWaterByRate, maxWaterByRoom));
        if (waterConsumed <= 0) {
            return;
        }
        water -= waterConsumed;
        steam += waterConsumed * 2;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Water", water);
        tag.putInt("Steam", steam);
        tag.putInt("Height", height);
        if (controller != null) {
            tag.putLong("Controller", controller.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = tag.getInt("Water");
        steam = tag.getInt("Steam");
        height = Math.max(1, tag.getInt("Height"));
        controller = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BoilerBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null) {
            return false;
        }
        int capacity = TFMGTweaksConfig.BOILER_TANK_CAPACITY.get() * controllerBE.height;
        TFMGTexts.header("boiler").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (controllerBE.height > 1) {
            tooltip.add(Component.literal("Height: " + controllerBE.height).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("Water: " + controllerBE.water + " / " + capacity + " mB").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Steam: " + controllerBE.steam + " / " + capacity + " mB (extract from top)")
                .withStyle(ChatFormatting.GRAY));
        if (level != null) {
            BlockPos belowPos = controllerBE.getBlockPos().below();
            float heat = BoilerHeater.findHeat(level, belowPos, level.getBlockState(belowPos));
            tooltip.add(Component.literal(heat > 0 ? "Heated" : "Not Heated")
                    .withStyle(heat > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        return true;
    }

    /** `exposeSteam` controls whether this instance exposes tank 1 (steam) at all -- only true for topmost UP queries. */
    private class BoilerFluidHandler implements IFluidHandler {

        private final boolean exposeSteam;

        private BoilerFluidHandler(boolean exposeSteam) {
            this.exposeSteam = exposeSteam;
        }

        @Override
        public int getTanks() {
            return exposeSteam ? 2 : 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
            }
            if (tank == 1 && exposeSteam) {
                return steam > 0 ? new FluidStack(steamFluid(), steam) : FluidStack.EMPTY;
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return TFMGTweaksConfig.BOILER_TANK_CAPACITY.get() * height;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank == 0) {
                return stack.getFluid().isSame(Fluids.WATER);
            }
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!resource.getFluid().isSame(Fluids.WATER)) {
                return 0;
            }
            int capacity = TFMGTweaksConfig.BOILER_TANK_CAPACITY.get() * height;
            int amount = Math.min(resource.getAmount(), capacity - water);
            if (amount <= 0) {
                return 0;
            }
            if (action.execute()) {
                water += amount;
                setChanged();
            }
            return amount;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!exposeSteam || !resource.getFluid().isSame(steamFluid())) {
                return FluidStack.EMPTY;
            }
            return drainSteam(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!exposeSteam) {
                return FluidStack.EMPTY;
            }
            return drainSteam(maxDrain, action);
        }

        private FluidStack drainSteam(int maxDrain, FluidAction action) {
            int amount = Math.min(maxDrain, steam);
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                steam -= amount;
                setChanged();
            }
            return new FluidStack(steamFluid(), amount);
        }
    }
}
