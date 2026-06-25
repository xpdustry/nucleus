// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.dependency;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// AI-SLOP
final class DependencyServiceTest {

    @Test
    void resolves_instances_and_constructor_bindings() {
        final var service = new DependencyService(
                binder -> {
                    binder.bindInstance(String.class, "hello");
                    binder.bindInstance(Integer.class, 42);
                },
                binder -> binder.bindConstructor(TestClass.class, TestClass.class));

        assertThat(service.resolve(String.class)).isEqualTo("hello");
        assertThat(service.resolve(Integer.class)).isEqualTo(42);
        assertThat(service.resolve(TestClass.class)).isEqualTo(new TestClass("hello", 42));
        assertThat(service.resolveAll()).containsExactly("hello", 42, new TestClass("hello", 42), service);
    }

    @Test
    void resolves_named_bindings() {
        final var service = new DependencyService(binder -> {
            binder.bindInstance(String.class, "hello", "hello");
            binder.bindInstance(String.class, "world", "world");
        });

        assertThat(service.resolve(String.class, "hello")).isEqualTo("hello");
        assertThat(service.resolve(String.class, "world")).isEqualTo("world");
        assertThatThrownBy(() -> service.resolve(String.class)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void does_not_recreate_instances() {
        final var service = new DependencyService(binder -> {
            binder.bindInstance(String.class, "hello");
            binder.bindInstance(Integer.class, 42);
            binder.bindConstructor(TestClass.class, TestClass.class);
        });
        final var instance = service.resolve(TestClass.class);

        assertThat(service.resolve(TestClass.class)).isSameAs(instance);
    }

    @Test
    void later_bindings_override_earlier_bindings() {
        final var service = new DependencyService(
                binder -> {
                    binder.bindInstance(String.class, "hello");
                    binder.bindInstance(Integer.class, 42);
                    binder.bindConstructor(TestClass.class, TestClass.class);
                },
                binder -> binder.bindInstance(String.class, "override"));

        assertThat(service.resolve(String.class)).isEqualTo("override");
        assertThat(service.resolve(Integer.class)).isEqualTo(42);
        assertThat(service.resolve(TestClass.class)).isEqualTo(new TestClass("override", 42));
    }

    @Test
    void binds_an_interface_to_an_implementation_constructor() {
        final var service = new DependencyService(binder -> {
            binder.bindInstance(String.class, "hello");
            binder.bindConstructor(Greeting.class, GreetingImpl.class);
        });

        assertThat(service.resolve(Greeting.class).text()).isEqualTo("hello");
    }

    @Test
    void creates_without_binding() {
        final var service = new DependencyService(binder -> {
            binder.bindInstance(String.class, "hello");
            binder.bindInstance(Integer.class, 42);
        });

        assertThat(service.instantiate(TestClass.class)).isEqualTo(new TestClass("hello", 42));
        assertThat(service.resolveAll()).containsExactlyInAnyOrder("hello", 42, service);
    }

    @Test
    void creates_with_named_parameters() {
        final var service = new DependencyService(binder -> {
            binder.bindInstance(String.class, "named", "named");
            binder.bindInstance(Integer.class, 42);
        });

        assertThat(service.instantiate(NamedTestClass.class)).isEqualTo(new NamedTestClass("named", 42));
    }

    @Test
    void fails_to_instantiate_without_an_injectable_constructor() {
        final var service = new DependencyService();

        assertThatThrownBy(() -> service.instantiate(NoInjectClass.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no injectable constructor");
    }

    @Test
    void uses_an_annotated_secondary_constructor() {
        final var service = new DependencyService(binder -> binder.bindInstance(String.class, "hello"));

        assertThat(service.instantiate(SecondaryInjectClass.class)).isEqualTo(new SecondaryInjectClass("hello", -8));
    }

    @Test
    void detects_circular_dependencies() {
        final var service = new DependencyService(binder -> {
            binder.bindConstructor(CircularA.class, CircularA.class);
            binder.bindConstructor(CircularB.class, CircularB.class);
        });

        assertThatThrownBy(() -> service.resolve(CircularA.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Circular bindings detected");
    }

    @Test
    void rejects_constructor_parameters_with_generic_types() {
        final var service = new DependencyService(
                binder -> binder.bindConstructor(GenericParameterClass.class, GenericParameterClass.class));

        assertThatThrownBy(() -> service.resolve(GenericParameterClass.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to create injection key for parameter")
                .hasRootCauseMessage("java.util.function.Supplier<java.lang.String> is a generic type");
    }

    @Test
    void rejects_generic_binding_keys() {
        assertThatThrownBy(() -> new DependencyService(
                        binder -> binder.bindConstructor(GenericService.class, GenericServiceImpl.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has generic type parameters");
    }

    record TestClass(String text, Integer number) {
        @Inject
        TestClass {}
    }

    record NamedTestClass(String text, Integer number) {
        @Inject
        NamedTestClass(@Named("named") final String text, final Integer number) {
            this.text = text;
            this.number = number;
        }
    }

    interface Greeting {
        String text();
    }

    record GreetingImpl(String text) implements Greeting {
        @Inject
        GreetingImpl {}
    }

    record NoInjectClass(String text) {}

    record SecondaryInjectClass(String text, Integer number) {
        @Inject
        SecondaryInjectClass(final String text) {
            this(text, -8);
        }
    }

    record CircularA(CircularB dependency) {
        @Inject
        CircularA {}
    }

    record CircularB(CircularA dependency) {
        @Inject
        CircularB {}
    }

    static final class GenericParameterClass {

        @Inject
        GenericParameterClass(final Supplier<String> text) {}
    }

    interface GenericService<T> {}

    static final class GenericServiceImpl implements GenericService<String> {}
}
