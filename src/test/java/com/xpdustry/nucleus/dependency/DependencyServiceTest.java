// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.dependency;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class DependencyServiceTest {

    @Test
    void resolves_simple_providers() {
        final var service = new DependencyService(List.of(new SimpleModule(), new TestClassModule()));

        assertThat(service.get(String.class)).isEqualTo("hello");
        assertThat(service.get(Integer.class)).isEqualTo(42);
        assertThat(service.get(TestClass.class)).isEqualTo(new TestClass("hello", 42));
        assertThat(service.getAll()).containsExactlyInAnyOrder("hello", 42, new TestClass("hello", 42));
    }

    @Test
    void resolves_named_providers() {
        final var service = new DependencyService(List.of(new NamedModule()));

        assertThat(service.get(String.class, "hello")).isEqualTo("hello");
        assertThat(service.get(String.class, "world")).isEqualTo("world");
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.get(String.class));
    }

    @Test
    void does_not_recreate_instances() {
        final var service = new DependencyService(List.of(new SimpleModule(), new TestClassModule()));
        final var instance = service.get(TestClass.class);

        assertThat(service.get(TestClass.class)).isSameAs(instance);
    }

    @Test
    void does_override_providers() {
        final var service =
                new DependencyService(List.of(new SimpleModule(), new TestClassModule(), new OverrideModule()));

        assertThat(service.get(String.class)).isEqualTo("override");
        assertThat(service.get(Integer.class)).isEqualTo(42);
        assertThat(service.get(TestClass.class)).isEqualTo(new TestClass("override", 42));
    }

    @Test
    void creates_without_binding() {
        final var service = new DependencyService(List.of(new SimpleModule()));

        assertThat(service.create(TestClass.class)).isEqualTo(new TestClass("hello", 42));
        assertThat(service.getAll()).containsExactlyInAnyOrder("hello", 42);
    }

    @Test
    void creates_with_named_parameters() {
        final var service = new DependencyService(List.of(new NamedConstructorModule()));

        assertThat(service.create(NamedTestClass.class)).isEqualTo(new NamedTestClass("named", 42));
    }

    @Test
    void fails_to_create_without_an_injectable_constructor() {
        final var service = new DependencyService(List.of());

        assertThatThrownBy(() -> service.create(NoInjectClass.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no injectable constructor");
    }

    @Test
    void uses_an_annotated_secondary_constructor() {
        final var service = new DependencyService(List.of(new SimpleModule()));

        assertThat(service.create(SecondaryInjectClass.class)).isEqualTo(new SecondaryInjectClass("hello", -8));
    }

    @Test
    void detects_circular_dependencies() {
        final var service = new DependencyService(List.of(new CircularModule()));

        assertThatThrownBy(() -> service.get(CircularA.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Circular bindings detected");
    }

    @Test
    void rejects_provider_methods_with_generic_parameters() {
        assertThatThrownBy(() -> new DependencyService(List.of(new GenericParameterModule())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to create injection key for parameter")
                .hasRootCauseMessage("java.util.function.Supplier<java.lang.String> is a generic type");
    }

    @Test
    void rejects_generic_binding_keys() {
        assertThatThrownBy(() -> new DependencyService(List.of(new GenericReturnModule())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to create injection key for return type")
                .hasRootCauseMessage("java.util.function.Supplier<java.lang.String> is a generic type");
    }

    @Test
    void ignores_non_provider_methods_while_scanning_modules() {
        final var service = new DependencyService(List.of(new SimpleModule(), new LarperModule()));

        assertThat(service.get(String.class)).isEqualTo("hello");
    }

    static final class SimpleModule implements Module {

        @Provider
        String text() {
            return "hello";
        }

        @Provider
        Integer number() {
            return 42;
        }
    }

    static final class TestClassModule implements Module {
        @Provider
        TestClass testClass(final String text, final Integer number) {
            return new TestClass(text, number);
        }
    }

    record TestClass(String text, Integer number) {
        @Inject
        TestClass {}
    }

    static final class NamedModule implements Module {

        @Provider
        @Named("hello")
        String hello() {
            return "hello";
        }

        @Provider
        @Named("world")
        String world() {
            return "world";
        }
    }

    static final class OverrideModule implements Module {

        @Provider
        String text() {
            return "override";
        }
    }

    static final class NamedConstructorModule implements Module {

        @Provider
        @Named("named")
        String text() {
            return "named";
        }

        @Provider
        Integer number() {
            return 42;
        }
    }

    record NamedTestClass(String text, Integer number) {
        @Inject
        NamedTestClass(@Named("named") final String text, final Integer number) {
            this.text = text;
            this.number = number;
        }
    }

    static final class WorldModule implements Module {

        @Provider
        String text() {
            return "world";
        }
    }

    static final class NumberModule implements Module {

        @Provider
        Integer number() {
            return 42;
        }
    }

    static final class CircularModule implements Module {

        @Provider
        CircularA circularA(final CircularB dependency) {
            return new CircularA(dependency);
        }

        @Provider
        CircularB circularB(final CircularA dependency) {
            return new CircularB(dependency);
        }
    }

    static final class GenericParameterModule implements Module {

        @Provider
        Integer number(final Supplier<String> text) {
            return text.get().length();
        }
    }

    static final class GenericReturnModule implements Module {

        @Provider
        Supplier<String> text() {
            return () -> "hello";
        }
    }

    static final class LarperModule implements Module {

        String text() {
            return "larper";
        }
    }

    record NoInjectClass(String text) {}

    record SecondaryInjectClass(String text, Integer number) {
        @Inject
        SecondaryInjectClass(final String text) {
            this(text, -8);
        }
    }

    record CircularA(CircularB dependency) {}

    record CircularB(CircularA dependency) {}
}
