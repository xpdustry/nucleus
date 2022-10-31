package fr.xpdustry.nucleus.common;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class NucleusApplicationProvider {

    private static @Nullable NucleusApplication instance = null;

    private NucleusApplicationProvider() {}

    public static NucleusApplication get() {
        if (NucleusApplicationProvider.instance == null) {
            throw new IllegalStateException("Nucleus hasn't been initialized yet.");
        }
        return NucleusApplicationProvider.instance;
    }

    public static void set(final NucleusApplication distributor) {
        if (NucleusApplicationProvider.instance != null) {
            throw new IllegalStateException("Nucleus has already been initialized.");
        }
        NucleusApplicationProvider.instance = distributor;
    }
}
