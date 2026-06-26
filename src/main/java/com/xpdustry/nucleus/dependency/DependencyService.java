// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.dependency;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.stream.Collectors;

public final class DependencyService {

    private final Map<Key<?>, Factory<?>> factories = new LinkedHashMap<>(); // for deterministic traversal
    private final SequencedMap<Key<?>, Object> instances = new LinkedHashMap<>();

    public DependencyService(final Module... modules) {
        final var binder = new Binder();
        for (final var module : modules) {
            module.configure(binder);
        }
        binder.bindInstance(DependencyService.class, this);
    }

    public <T> T resolve(final Class<T> type, final String name) {
        return this.resolve(new Key<>(type, name), new LinkedHashSet<>());
    }

    public <T> T resolve(final Class<T> type) {
        return this.resolve(type, "");
    }

    private <T> T resolve(final Key<T> key, final SequencedSet<Key<?>> visited) {
        if (DependencyService.this.instances.containsKey(key)) {
            return key.type.cast(DependencyService.this.instances.get(key));
        }
        final var factory = DependencyService.this.factories.get(key);
        if (factory == null) {
            throw new IllegalStateException("No bindings found for " + key);
        }
        try {
            if (!visited.add(key)) {
                throw new IllegalStateException("Circular bindings detected: "
                        + visited.stream().map(Key::toString).collect(Collectors.joining(" -> ")));
            }
            final var value = factory.create(visited);
            DependencyService.this.instances.put(key, value);
            return key.type.cast(value);
        } finally {
            visited.remove(key);
        }
    }

    public List<Object> resolveAll() {
        final var visited = new LinkedHashSet<Key<?>>();
        for (final var key : this.factories.keySet()) {
            final var _ = this.resolve(key, visited);
        }
        return new ArrayList<>(this.instances.sequencedValues());
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

    private static List<Key<?>> getInjectionKeys(final Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameters())
                .<Key<?>>map(parameter -> {
                    try {
                        final var type = getInjectionKeyType(parameter.getParameterizedType());
                        final var named = parameter.getAnnotation(Named.class);
                        return new Key<>(type, named == null ? "" : named.value());
                    } catch (final Exception e) {
                        throw new IllegalArgumentException(
                                "Failed to create injection key for parameter " + parameter.getName() + " in "
                                        + constructor,
                                e);
                    }
                })
                .toList();
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

    private record Key<T>(Class<T> type, String name) {
        @Override
        public String toString() {
            return this.type.getSimpleName() + (this.name.isEmpty() ? "" : ":" + this.name);
        }
    }

    private sealed interface Factory<T> {

        T create(final SequencedSet<Key<?>> visited);
    }

    private final class ConstructorFactory<T> implements Factory<T> {

        private final Constructor<T> constructor;

        private ConstructorFactory(final Constructor<T> constructor) {
            this.constructor = constructor;
        }

        @Override
        public T create(final SequencedSet<Key<?>> visited) {
            final var arguments = getInjectionKeys(this.constructor).stream()
                    .map(key -> DependencyService.this.resolve(key, visited))
                    .toArray();
            try {
                this.constructor.setAccessible(true);
                return this.constructor.newInstance(arguments);
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke " + this.constructor, e);
            }
        }
    }

    private record StaticInstanceFactory<T>(T instance) implements Factory<T> {

        @Override
        public T create(final SequencedSet<Key<?>> visited) {
            return this.instance;
        }
    }

    public final class Binder {

        public <T> void bindInstance(final Class<T> type, final String name, final T instance) {
            final var _ = getInjectionKeyType(type);
            DependencyService.this.factories.put(new Key<>(type, name), new StaticInstanceFactory<>(instance));
        }

        public <T> void bindInstance(final Class<T> type, final T instance) {
            this.bindInstance(type, "", instance);
        }

        public <T> void bindConstructor(final Class<T> type, final String name, final Class<? extends T> impl) {
            final var _ = getInjectionKeyType(type);
            final var _ = getInjectionKeyType(impl);
            DependencyService.this.factories.put(
                    new Key<>(type, name), new ConstructorFactory<>(getInjectableConstructor(impl)));
        }

        public <T> void bindConstructor(final Class<T> type) {
            this.bindConstructor(type, "", type);
        }

        public <T> void bindConstructor(final Class<T> type, final Class<? extends T> impl) {
            this.bindConstructor(type, "", impl);
        }
    }
}
