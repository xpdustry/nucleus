// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public record ConfigKey<T>(Class<T> type, String name, String description, T def, Set<Flag> flags) {
    public ConfigKey(Class<T> type, String name, String description, T def, Flag... flags) {
        final var set = EnumSet.noneOf(Flag.class);
        set.addAll(Arrays.asList(flags));
        this(type, name, description, def, set);
    }

    public enum Flag {
        SENSITIVE,
        DYNAMIC
    }
}
