package com.tfmgtweaks.compat;

import net.neoforged.fml.loading.LoadingModList;

public final class FlowingFluidsCompat {

    private static final boolean LOADED = LoadingModList.get().getModFileById("flowing_fluids") != null;

    private FlowingFluidsCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }
}
