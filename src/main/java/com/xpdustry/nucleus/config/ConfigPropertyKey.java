// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record ConfigPropertyKey<T>(String name, Class<T> type, T def) {

    private static final Map<String, ConfigPropertyKey<?>> ALL = new HashMap<>();

    // --- Server wide configs ------------------------------------------------

    public static final ConfigPropertyKey<String> SERVER_NAME =
            registering("nucleus.server.name", String.class, "unknown");

    // --- Database -----------------------------------------------------------

    public static final ConfigPropertyKey<String> DATABASE_HOST =
            registering("nucleus.database.host", String.class, "localhost");

    public static final ConfigPropertyKey<Integer> DATABASE_PORT =
            registering("nucleus.database.port", Integer.class, 5432);

    public static final ConfigPropertyKey<String> DATABASE_NAME =
            registering("nucleus.database.name", String.class, "postgres");

    public static final ConfigPropertyKey<String> DATABASE_USERNAME =
            registering("nucleus.database.username", String.class, "postgres");

    public static final ConfigPropertyKey<String> DATABASE_PASSWORD =
            registering("nucleus.database.password", String.class, "postgres");

    public static final ConfigPropertyKey<Boolean> DATABASE_EMBEDDED =
            registering("nucleus.database.embedded", Boolean.class, true);

    // --- E.N.D --------------------------------------------------------------

    public static Map<String, ConfigPropertyKey<?>> all() {
        return Collections.unmodifiableMap(ALL);
    }

    private static <T> ConfigPropertyKey<T> registering(final String name, final Class<T> type, final T def) {
        final var key = new ConfigPropertyKey<>(name, type, def);
        if (ALL.containsKey(key.name())) {
            throw new IllegalStateException("Duplicate key " + key.name());
        } else {
            ALL.put(key.name(), key);
            return key;
        }
    }
}
