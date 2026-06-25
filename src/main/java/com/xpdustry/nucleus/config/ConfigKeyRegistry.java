// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.dependency.DependencyService;
import com.xpdustry.nucleus.dependency.Inject;
import com.xpdustry.nucleus.dependency.Named;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.AccessFlag;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigKeyRegistry implements PluginListener {

    private final Map<String, ConfigKey<?>> keys = new ConcurrentHashMap<>();
    private volatile Map<ConfigKey<?>, Object> entries = new HashMap<>();

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .setStrictness(Strictness.STRICT)
            .create();

    private final DependencyService dependencies;
    private final Path file;

    @Inject
    public ConfigKeyRegistry(final DependencyService dependencies, final @Named("home") Path directory) {
        this.dependencies = dependencies;
        this.file = directory.resolve("config.json");
    }

    @Override
    public void onInit() {
        for (final var object : this.dependencies.resolveAll()) {
            for (final var field : object.getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(ConfigAutoRegister.class)) {
                    continue;
                }
                if (!field.getType().equals(ConfigKey.class)) {
                    throw new IllegalArgumentException(field + " is annotated with @ConfigAutoRegister");
                }
                if (!field.accessFlags().contains(AccessFlag.STATIC)) {
                    throw new IllegalArgumentException(field + " annotated with @ConfigAutoRegister is not static");
                }
                final ConfigKey<?> key;
                try {
                    field.setAccessible(true);
                    key = (ConfigKey<?>) field.get(null);
                } catch (final ReflectiveOperationException e) {
                    throw new IllegalArgumentException("Failed to obtain the config key of " + field);
                }
                this.register(key);
            }
        }

        if (Files.exists(this.file)) {
            try (final var stream = Files.newInputStream(this.file);
                    final var reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                this.push(reader);
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to load the local config file", e);
            }
        }
    }

    public void register(final ConfigKey<?> key) {
        if (this.keys.containsKey(key.name())) {
            throw new IllegalStateException(key + " name is already registered by " + this.keys.get(key.name()));
        }
        this.keys.put(key.name(), key);
    }

    public <T> T get(final ConfigKey<T> key) {
        final var value = this.entries.get(key);
        return value == null ? key.def() : key.type().cast(value);
    }

    public void push(final JsonReader reader) throws IOException {
        final var entries = new HashMap<>(this.entries);
        reader.beginObject();
        while (reader.hasNext()) {
            final var name = reader.nextName();
            final var key = this.keys.get(name);
            if (key == null) {
                reader.skipValue();
            } else {
                final var adapter = this.gson.getAdapter(key.type());
                final var value = adapter.read(reader);
                if (value == null) {
                    entries.remove(key);
                } else {
                    entries.put(key, key.type().cast(value));
                }
            }
        }
        reader.endObject();
        this.entries = entries;
    }
}
