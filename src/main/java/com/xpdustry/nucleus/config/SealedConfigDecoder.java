// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.github.gestalt.config.decoder.Decoder;
import org.github.gestalt.config.decoder.DecoderContext;
import org.github.gestalt.config.decoder.Priority;
import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.entity.ValidationLevel;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.node.MapNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.utils.GResultOf;

/// Decodes sealed configuration hierarchies using an explicit `type` discriminator.
public final class SealedConfigDecoder implements Decoder<Object> {

    @Override
    public Priority priority() {
        return Priority.HIGH;
    }

    @Override
    public String name() {
        return "SealedConfig";
    }

    @Override
    public boolean canDecode(final String path, final Tags tags, final ConfigNode node, final TypeCapture<?> type) {
        final var clazz = type.getRawType();
        final var annotation = clazz.getAnnotation(SealedConfig.class);
        if (!(clazz.isSealed() && node instanceof MapNode && annotation != null)) {
            return false;
        }

        final var subclasses = this.getSealedConfigSubclasses(clazz);
        if (annotation.def().isBlank()) {
            throw new IllegalStateException("Missing default subclass of @SealedConfig for " + clazz.getName());
        }
        if (!subclasses.containsKey(annotation.def())) {
            throw new IllegalStateException(
                    "Unknown subclass of @SealedConfig for " + clazz.getName() + ": " + annotation.def());
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GResultOf<Object> decode(
            final String path,
            final Tags tags,
            final ConfigNode node,
            final TypeCapture<?> type,
            final DecoderContext context) {
        final var clazz = type.getRawType();
        final var subclasses = this.getSealedConfigSubclasses(clazz);
        final var property = node.getKey("type");

        final Class<?> subclass;
        if (property.isEmpty()) {
            subclass = Objects.requireNonNull(
                    subclasses.get(Objects.requireNonNull(clazz.getAnnotation(SealedConfig.class))
                            .def()));
        } else {
            final var value = property.get().getValue();
            if (value.isEmpty()) {
                return GResultOf.errors(new InvalidSealedConfigTypeError(path, clazz, ""));
            }
            subclass = subclasses.get(value.get());
            if (subclass == null) {
                return GResultOf.errors(new InvalidSealedConfigTypeError(path, clazz, value.get()));
            }
        }

        return (GResultOf<Object>)
                context.getDecoderService().decodeNode(path, tags, node, TypeCapture.of(subclass), context);
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, Class<? extends T>> getSealedConfigSubclasses(final Class<T> clazz) {
        final Map<String, Class<? extends T>> subclasses = new HashMap<>();
        for (final var permitted : clazz.getPermittedSubclasses()) {
            final var annotation = permitted.getAnnotation(SealedConfig.class);
            if (annotation == null) {
                throw new IllegalStateException("Missing @SealedConfig annotation for class " + permitted.getName());
            }
            if (annotation.name().isBlank()) {
                throw new IllegalStateException("Subtype name required for @SealedConfig for " + permitted.getName());
            }
            if (subclasses.put(annotation.name(), (Class<? extends T>) permitted) != null) {
                throw new IllegalStateException("Duplicate name for @SealedConfig for " + permitted.getName());
            }
            try {
                final var _ = permitted.getDeclaredField("type");
                throw new IllegalStateException("Conflicting 'type' field in @SealedConfig " + permitted.getName());
            } catch (final NoSuchFieldException _) {
            }
        }
        return subclasses;
    }

    /// Error for an invalid sealed configuration discriminator.
    public static final class InvalidSealedConfigTypeError extends ValidationError {

        private final String path;
        private final Class<?> clazz;
        private final String value;

        private InvalidSealedConfigTypeError(final String path, final Class<?> clazz, final String value) {
            super(ValidationLevel.ERROR);
            this.path = path;
            this.clazz = clazz;
            this.value = value;
        }

        @Override
        public String description() {
            return "Invalid @SealedConfig type for class " + this.clazz.getName() + " (value=" + this.value
                    + ") on path: " + this.path;
        }
    }
}
