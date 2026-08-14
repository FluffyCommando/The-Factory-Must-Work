package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.TFMGFluidTankBlockEntity;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlock;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the earlier reactive fix (detect a mixed-type merge after it
 * forms, then split it back apart) with a preventative one: reject a
 * mismatched-type candidate before it's ever absorbed into a forming
 * structure at all. The reactive approach, even after several rounds of
 * fixing reentrancy issues (deferring the split to the next tick,
 * suppressing updateConnectivity across the whole former group), was
 * still fundamentally racing against Create's own formation code, which
 * has no concept of vatType or tank block class and will always try to
 * re-absorb a physically-adjacent mismatched neighbor the moment
 * anything re-triggers formation. Preventing the absorption in the
 * first place removes that race entirely -- there's nothing to split
 * apart because the mismatched merge never happens.
 *
 * This does NOT use separate BlockEntityType registrations per variant
 * (steel/cast_iron/firebrick_lined vat, or aluminum/cast_iron tank),
 * which was considered and rejected: BlockEntityType is resolved from
 * saved NBT data at chunk load, not re-derived from the block, so
 * existing worlds would keep their vats/tanks on the old, shared type
 * indefinitely while only newly-placed ones got a new type -- breaking
 * ordinary same-type merging between an old block and a new one until
 * the old one is broken and replaced. That would need a DataFixer-style
 * migration to resolve cleanly, which risks corrupting existing saves
 * if done wrong. This mixin needs no persisted state or migration at
 * all -- it's pure runtime logic.
 *
 * Mechanism: ConnectivityHandler.formMulti(BlockEntity) is the single
 * public entry point every multiblock formation search starts from
 * (confirmed directly against the compiled bytecode -- it's overloaded,
 * this is specifically the one-argument version). Its own body is just
 * a thin wrapper that immediately calls into the actual search logic,
 * so injecting at its head, before any candidates have been examined
 * yet, is a safe place to record which vat type or tank block class (if
 * either) originated this particular search. That's tracked in a plain
 * static field, not a ThreadLocal -- world/connectivity logic only ever
 * runs on the single main server thread, and the field is unconditionally
 * reset at the start of every formMulti() call regardless of what the
 * previous call left behind, so it can't leak stale state across calls
 * even if some earlier call exited abnormally.
 *
 * ConnectivityHandler.partAt(BlockEntityType, BlockGetter, BlockPos) is
 * the method every candidate position is actually resolved through
 * during the search (via SearchCache.getOrCache, which delegates to it)
 * -- not overloaded, so no descriptor ambiguity there. Its own check is
 * only `blockEntity.getType() == type`; this adds a second condition
 * after that already-successful check: if the object matches vatType a
 * result was found for a Vat, or a tank block class a result was found
 * for a tank block, and the result's own vatType/class doesn't match the
 * value recorded for the current search, override the return value to
 * null instead. Since a matching type is required by definition to have
 * originated the search in the first place, this can only ever narrow
 * results for Vat/tank searches specifically -- it has no effect on any
 * other multiblock type in the game, since neither of these tracked
 * fields is ever set for them.
 */
@Mixin(ConnectivityHandler.class)
public abstract class ConnectivityHandlerVatTankTypeMixin {

    private static String tfmgtweaks$currentVatType = null;
    private static Class<?> tfmgtweaks$currentTankClass = null;

    @Inject(method = "formMulti(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("HEAD"))
    private static void tfmgtweaks$trackFormationOrigin(BlockEntity be, CallbackInfo ci) {
        tfmgtweaks$currentVatType = null;
        tfmgtweaks$currentTankClass = null;
        if (be instanceof VatBlockEntity && be.getBlockState().getBlock() instanceof VatBlock originBlock) {
            tfmgtweaks$currentVatType = originBlock.vatType;
        } else if (be instanceof TFMGFluidTankBlockEntity) {
            tfmgtweaks$currentTankClass = be.getBlockState().getBlock().getClass();
        }
    }

    @Inject(method = "partAt", at = @At("RETURN"), cancellable = true)
    private static void tfmgtweaks$rejectMismatchedVatTankType(BlockEntityType<?> type, BlockGetter level, BlockPos pos,
                                                                 CallbackInfoReturnable<BlockEntity> cir) {
        BlockEntity result = cir.getReturnValue();
        if (result == null) {
            return;
        }
        if (tfmgtweaks$currentVatType != null && result instanceof VatBlockEntity) {
            VatBlock resultBlock = result.getBlockState().getBlock() instanceof VatBlock vb ? vb : null;
            if (resultBlock == null || !resultBlock.vatType.equals(tfmgtweaks$currentVatType)) {
                cir.setReturnValue(null);
            }
        } else if (tfmgtweaks$currentTankClass != null && result instanceof TFMGFluidTankBlockEntity) {
            if (result.getBlockState().getBlock().getClass() != tfmgtweaks$currentTankClass) {
                cir.setReturnValue(null);
            }
        }
    }
}
