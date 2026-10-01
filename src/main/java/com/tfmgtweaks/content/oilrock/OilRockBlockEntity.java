package com.tfmgtweaks.content.oilrock;

import com.drmangotea.tfmg.base.lang.TFMGTexts;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.tfmgtweaks.advancement.TFMGTweaksTriggers;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.registry.TFMGTweaksBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Oil Rock blocks form a deposit sharing one pool of reserves and cracking progress through a controller. */
public class OilRockBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    @Nullable
    private BlockPos controller;
    private List<BlockPos> members = new ArrayList<>();

    private int oilReserves;
    private int fluidAbsorbed = 0;
    private boolean cracked = false;
    private int ticksSinceLastFed = 0;

    public OilRockBlockEntity(BlockPos pos, BlockState state) {
        super(TFMGTweaksBlockEntities.OIL_ROCK.get(), pos, state);
        oilReserves = TFMGTweaksConfig.OIL_ROCK_RESERVES.get();
    }

    /** Only the controller updates; members hold no state of their own. */
    public void tick() {
        if (!isController() || level == null || level.isClientSide) {
            return;
        }
        if (fluidAbsorbed > 0) {
            int graceTicks = TFMGTweaksConfig.OIL_ROCK_FRACKING_DECAY_GRACE_TICKS.get();
            if (ticksSinceLastFed > graceTicks) {
                int decay = TFMGTweaksConfig.OIL_ROCK_FRACKING_DECAY_RATE.get();
                if (decay > 0) {
                    fluidAbsorbed = Math.max(0, fluidAbsorbed - decay);
                    setChanged();
                }
            }
        }
        if (ticksSinceLastFed < Integer.MAX_VALUE) {
            ticksSinceLastFed++;
        }
        updateCrackedState();
    }

    /** Called by OilRockFeature after placing a cluster; allMembers includes this position. */
    public void initializeAsController(List<BlockPos> allMembers) {
        this.controller = null;
        this.members = new ArrayList<>(allMembers);
        this.oilReserves = TFMGTweaksConfig.OIL_ROCK_RESERVES.get();
        setChanged();
    }

    public void initializeAsMember(BlockPos controllerPos) {
        this.controller = controllerPos;
        this.members = new ArrayList<>();
        setChanged();
    }

    public boolean isController() {
        return controller == null;
    }

    /** Returns the deposit's controller, promoting this block if the stored controller is gone. */
    public OilRockBlockEntity getControllerBE() {
        if (isController()) {
            return this;
        }
        if (level != null && level.getBlockEntity(controller) instanceof OilRockBlockEntity be) {
            return be;
        }
        initializeAsController(List.of(getBlockPos()));
        return this;
    }

    public boolean isCracked() {
        return getControllerBE().cracked;
    }

    public int getOilReserves() {
        return getControllerBE().oilReserves;
    }

    /** Adds to the deposit's cracking progress, capped at fluidToFullyCrack. */
    public int addFrackingProgress(int amount) {
        OilRockBlockEntity controllerBE = getControllerBE();
        if (amount <= 0) {
            return 0;
        }
        controllerBE.ticksSinceLastFed = 0;
        int cap = TFMGTweaksConfig.OIL_ROCK_FLUID_TO_FULLY_CRACK.get();
        int room = cap - controllerBE.fluidAbsorbed;
        int accepted = Math.min(amount, room);
        if (accepted <= 0) {
            return 0;
        }
        controllerBE.fluidAbsorbed += accepted;
        controllerBE.updateCrackedState();
        controllerBE.setChanged();
        return accepted;
    }

    /** Removes up to amount from the deposit's reserves, turning the whole cluster to stone once empty. */
    public boolean depleteReserves(int amount) {
        OilRockBlockEntity controllerBE = getControllerBE();
        controllerBE.oilReserves -= amount;
        controllerBE.setChanged();
        if (controllerBE.oilReserves <= 0) {
            controllerBE.convertAllMembersToStone();
            return true;
        }
        return false;
    }

    private void convertAllMembersToStone() {
        if (level == null) {
            return;
        }
        for (BlockPos memberPos : members) {
            level.setBlock(memberPos, Blocks.STONE.defaultBlockState(), 3);
        }
    }

    private void updateCrackedState() {
        boolean shouldBeCracked = fluidAbsorbed >= TFMGTweaksConfig.OIL_ROCK_FLUID_TO_FULLY_CRACK.get();
        if (shouldBeCracked == cracked) {
            return;
        }
        cracked = shouldBeCracked;
        if (level == null) {
            return;
        }
        for (BlockPos memberPos : members) {
            BlockState state = level.getBlockState(memberPos);
            if (state.getBlock() instanceof OilRockBlock) {
                level.setBlock(memberPos, state.setValue(OilRockBlock.CRACKED, cracked), 3);
            }
        }
        setChanged();
        
        if (cracked && level instanceof ServerLevel serverLevel) {
            AABB range = new AABB(getBlockPos()).inflate(32);
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, range)) {
                TFMGTweaksTriggers.FRACKED_OIL.trigger(player);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) {
            tag.putLong("Controller", controller.asLong());
        }
        ListTag memberList = new ListTag();
        for (BlockPos memberPos : members) {
            memberList.add(LongTag.valueOf(memberPos.asLong()));
        }
        tag.put("Members", memberList);
        tag.putInt("OilReserves", oilReserves);
        tag.putInt("FluidAbsorbed", fluidAbsorbed);
        tag.putBoolean("Cracked", cracked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
        members = new ArrayList<>();
        for (var element : tag.getList("Members", 4)) {
            members.add(BlockPos.of(((LongTag) element).getAsLong()));
        }
        oilReserves = tag.getInt("OilReserves");
        fluidAbsorbed = tag.getInt("FluidAbsorbed");
        cracked = tag.getBoolean("Cracked");
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
        TFMGTexts.header("oil_rock").style(ChatFormatting.GRAY).forGoggles(tooltip);
        OilRockBlockEntity controllerBE = getControllerBE();
        if (TFMGTweaksConfig.OIL_ROCK_FINITE_RESERVES.get()) {
            tooltip.add(Component.literal("Remaining Reserves: " + controllerBE.oilReserves).withStyle(ChatFormatting.GRAY));
        }
        boolean isCracked = controllerBE.cracked;
        tooltip.add(Component.literal(isCracked ? "Cracked (extraction boosted)" : "Not yet cracked")
                .withStyle(isCracked ? ChatFormatting.GREEN : ChatFormatting.RED));
        int needed = TFMGTweaksConfig.OIL_ROCK_FLUID_TO_FULLY_CRACK.get();
        int percent = Math.min(100, (int) (100L * controllerBE.fluidAbsorbed / needed));
        tooltip.add(Component.literal("Fracking Progress: " + percent + "%").withStyle(ChatFormatting.GRAY));
        if (!isCracked) {
            tooltip.add(Component.literal("Pump steam down a running pump jack's pipe to frack")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Keep pumping steam to maintain the bonus -- it decays over time")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }
}
