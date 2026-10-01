package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_stove.BlastStoveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Caps the Blast Stove at 2x2, since a 3x3 renders with only its corners visible. */
@Mixin(BlastStoveBlockEntity.class)
public abstract class BlastStoveBlockEntityMixin {
    public int getMaxWidth() {
        return 2;
    }
}
