// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import com.xpdustry.foundation.plugin.PluginListener;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

public final class ConfigManager implements PluginListener {

    private Map<String, Object> entries = new HashMap<>();
    private final @Nullable Path file;

    public ConfigManager(final Path file) {
        this.file = file;
    }

    public ConfigManager() {
        this.file = null;
    }

    public <T> T get(final ConfigPropertyKey<T> key) {
        final var value = this.entries.get(key.name());
        return value == null ? key.def() : key.type().cast(value);
    }

    public <T> ConfigManager set(final ConfigPropertyKey<T> key, final T value) {
        this.entries.put(key.name(), value);
        return this;
    }

    @Override
    public void onInit() {
        if (this.file == null || Files.notExists(this.file)) {
            return;
        }

        final var properties = new Properties();
        try (final var reader = Files.newBufferedReader(this.file)) {
            properties.load(reader);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read the config file", e);
        }

        for (final var name : properties.stringPropertyNames()) {
            final var key = ConfigPropertyKey.all().get(name);
            if (key == null) {
                throw new IllegalStateException("The key " + name + " is unused");
            }
            try {
                this.entries.put(key.name(), this.parse(key, properties.getProperty(name)));
            } catch (final Exception e) {
                throw new IllegalArgumentException("Failed to parse " + name, e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T parse(final ConfigPropertyKey<T> key, final String string) {
        final Object value;
        if (key.type().equals(String.class)) {
            value = string;
        } else if (key.type().equals(Boolean.class)) {
            value = switch (string.toLowerCase(Locale.ROOT)) {
                case "false" -> false;
                case "true" -> true;
                default -> throw new IllegalArgumentException(string + " is not a valid boolean");
            };
        } else if (key.type().equals(Integer.class)) {
            value = Integer.parseInt(string);
        } else if (key.type().equals(URI.class)) {
            value = URI.create(string);
        } else if (Enum.class.isAssignableFrom(key.type())) {
            // TODO raw types... ew...
            value = Enum.valueOf((Class) key.type(), string);
        } else {
            throw new IllegalArgumentException(key.type().getSimpleName() + " is not a supported config type");
        }
        return key.type().cast(value);
    }

    @VisibleForTesting
    public ConfigManager fork() {
        final var that = new ConfigManager();
        that.entries = new HashMap<>(this.entries);
        return that;
    }
}
