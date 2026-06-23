// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.dependency;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class DependencyService {

    private final Map<Key<?>, ProviderMethodInfo> providers = new HashMap<>();
    private final LinkedHashMap<Key<?>, Object> instances = new LinkedHashMap<>();

    public DependencyService(final List<Module> modules) {
        for (final var module : modules) {
            for (final var method : module.getClass().getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Provider.class)) {
                    continue;
                }

                final var _ = getInjectionKeys(method);
                final Key<?> key;
                try {
                    final var type = getInjectionKeyType(method.getGenericReturnType());
                    final var name = getInjectionKeyName(method);
                    key = new Key<>(type, name);
                } catch (final Exception e) {
                    throw new IllegalArgumentException(
                            "Failed to create injection key for return type of " + method, e);
                }

                this.providers.put(key, new ProviderMethodInfo(module, method));
            }
        }
    }

    public <T> T create(final Class<T> type) {
        return this.resolve(null, getInjectableConstructor(type), type, new LinkedHashSet<>());
    }

    public <T> T get(final Class<T> type) {
        return this.resolve(new Key<>(type, ""), new LinkedHashSet<>());
    }

    public <T> T get(final Class<T> type, final String name) {
        return this.resolve(new Key<>(type, name), new LinkedHashSet<>());
    }

    public List<Object> getAll() {
        final var visited = new LinkedHashSet<Key<?>>();
        for (final var key : this.providers.keySet()) {
            final var _ = this.resolve(key, visited);
        }
        return new ArrayList<>(this.instances.sequencedValues());
    }

    private <T> T resolve(
            final @Nullable Object object,
            final Executable executable,
            final Class<T> type,
            final SequencedSet<Key<?>> visited) {
        final var arguments = getInjectionKeys(executable).stream()
                .map(key -> this.resolve(key, visited))
                .toArray();
        try {
            final var value =
                    switch (executable) {
                        case Method method -> method.invoke(Objects.requireNonNull(object), arguments);
                        case Constructor<?> constructor -> constructor.newInstance(arguments);
                    };
            return Objects.requireNonNull(type.cast(value), executable + " returned a null value");
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke " + executable, e);
        }
    }

    private <T> T resolve(final Key<T> key, final SequencedSet<Key<?>> visited) {
        if (DependencyService.this.instances.containsKey(key)) {
            return key.type.cast(DependencyService.this.instances.get(key));
        }
        final var info = DependencyService.this.providers.get(key);
        if (info == null) {
            throw new IllegalStateException("No bindings found for " + key);
        }
        try {
            if (!visited.add(key)) {
                throw new IllegalStateException("Circular bindings detected: "
                        + visited.stream().map(Key::toString).collect(Collectors.joining(" -> ")));
            }
            final var value = resolve(info.object, info.method, key.type, visited);
            DependencyService.this.instances.put(key, value);
            return value;
        } finally {
            visited.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> getInjectableConstructor(final Class<T> type) {
        final var constructors = Arrays.stream(type.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();
        return switch (constructors.size()) {
            case 0 -> throw new IllegalArgumentException(type.getName() + " has no injectable constructor");
            case 1 -> (Constructor<T>) constructors.getFirst();
            default -> throw new IllegalArgumentException(type.getName() + " has more than one injectable annotation");
        };
    }

    private static List<Key<?>> getInjectionKeys(final Executable method) {
        return Arrays.stream(method.getParameters())
                .<Key<?>>map(parameter -> {
                    try {
                        final var type = getInjectionKeyType(parameter.getParameterizedType());
                        final var name = getInjectionKeyName(parameter);
                        return new Key<>(type, name);
                    } catch (final Exception e) {
                        throw new IllegalArgumentException(
                                "Failed to create injection key for parameter " + parameter.getName() + " in " + method,
                                e);
                    }
                })
                .toList();
    }

    private static String getInjectionKeyName(final AnnotatedElement element) {
        final var annotation = element.getAnnotation(Named.class);
        return annotation == null ? "" : annotation.value();
    }

    private static Class<?> getInjectionKeyType(final Type type) {
        if (!(type instanceof Class<?> clazz)) {
            throw new IllegalArgumentException(type.getTypeName() + " is a generic type");
        }
        final var generics = clazz.getTypeParameters();
        if (generics.length != 0) {
            throw new IllegalArgumentException(type.getTypeName() + " has generic type parameters");
        }
        return clazz;
    }

    private record ProviderMethodInfo(Object object, Method method) {}

    private record Key<T>(Class<T> type, String name) {
        @Override
        public String toString() {
            return this.type.getSimpleName() + (this.name.isEmpty() ? "" : ":" + this.name);
        }
    }
}
