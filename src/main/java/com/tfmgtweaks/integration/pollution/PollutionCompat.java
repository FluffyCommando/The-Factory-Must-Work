package com.tfmgtweaks.integration.pollution;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.neoforged.fml.loading.LoadingModList;

/**
 * Only class that checks whether Pollution of the Realms is loaded; calls into it are passed as suppliers
 * so its classes are never loaded when it's absent.
 */
public final class PollutionCompat {
    private static final boolean LOADED = LoadingModList.get().getModFileById("adpother") != null;

    private PollutionCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /** Runs toExecute only if Pollution of the Realms is loaded. */
    public static void executeIfInstalled(Supplier<Runnable> toExecute) {
        if (LOADED) {
            toExecute.get().run();
        }
    }

    /** Returns the consumed amount, or 0 if Pollution of the Realms isn't loaded. */
    public static int consumePollutionIfInstalled(Supplier<IntSupplier> toExecute) {
        if (LOADED) {
            return toExecute.get().getAsInt();
        }
        return 0;
    }

    /** Returns the settle height, or fallback if Pollution of the Realms isn't loaded. */
    public static int getGasSettleHeightIfInstalled(Supplier<IntSupplier> toExecute, int fallbackIfNotInstalled) {
        if (LOADED) {
            return toExecute.get().getAsInt();
        }
        return fallbackIfNotInstalled;
    }
}

