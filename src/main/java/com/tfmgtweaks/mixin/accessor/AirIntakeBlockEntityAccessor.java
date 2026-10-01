package com.tfmgtweaks.mixin.accessor;

import com.drmangotea.tfmg.content.machinery.misc.air_intake.AirIntakeBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads and writes these fields on other Air Intake instances, which @Shadow can't reach. */
@Mixin(AirIntakeBlockEntity.class)
public interface AirIntakeBlockEntityAccessor {
    @Accessor("diameter")
    int tfmgtweaks$getDiameter();

    @Accessor("diameter")
    void tfmgtweaks$setDiameter(int value);

    @Accessor("isController")
    boolean tfmgtweaks$isController();

    @Accessor("isController")
    void tfmgtweaks$setIsController(boolean value);

    @Accessor("isUsedByController")
    boolean tfmgtweaks$isUsedByController();

    @Accessor("isUsedByController")
    void tfmgtweaks$setIsUsedByController(boolean value);
}
