// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.config;

import arc.util.Strings;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.MappableContent;
import org.github.gestalt.config.decoder.DecoderContext;
import org.github.gestalt.config.decoder.LeafDecoder;
import org.github.gestalt.config.decoder.Priority;
import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.entity.ValidationLevel;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.utils.GResultOf;

/// Decodes Mindustry content names such as `copper`, `duo`, or `dagger`.
public final class MindustryContentDecoder<T extends MappableContent> extends LeafDecoder<T> {

    private final ContentType contentType;
    private final Class<T> contentClass;

    private MindustryContentDecoder(final ContentType contentType, final Class<T> contentClass) {
        if (contentType.contentClass == null) {
            throw new IllegalArgumentException(contentType + " cannot be used as it has no bound ctype class");
        }
        if (!contentType.contentClass.equals(contentClass)) {
            throw new IllegalArgumentException(contentType + " paired with " + contentClass
                    + " is not compatible, expected " + contentType.contentClass);
        }
        this.contentType = contentType;
        this.contentClass = contentClass;
    }

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public String name() {
        return "Mindustry" + Strings.capitalize(this.contentType.name());
    }

    @Override
    public boolean canDecode(final String path, final Tags tags, final ConfigNode node, final TypeCapture<?> type) {
        return this.contentClass.isAssignableFrom(type.getRawType());
    }

    @Override
    protected GResultOf<T> leafDecode(
            final String path, final ConfigNode node, final TypeCapture<?> type, final DecoderContext context) {
        return this.decodeContent(path, node, type.getRawType());
    }

    @Override
    protected GResultOf<T> leafDecode(final String path, final ConfigNode node, final DecoderContext context) {
        return this.decodeContent(path, node, this.contentClass);
    }

    private GResultOf<T> decodeContent(final String path, final ConfigNode node, final Class<?> expectedType) {
        final var name = node.getValue().orElse("");
        final var content = Vars.content.getByName(this.contentType, name);
        if (content == null) {
            return GResultOf.errors(new UnknownMindustryContentError(path, this.contentType, name));
        }
        if (!expectedType.isInstance(content)) {
            return GResultOf.errors(new InvalidMindustryContentTypeError(path, name, this.contentType, expectedType));
        }
        return GResultOf.result(this.contentClass.cast(content));
    }

    /// Error for missing Mindustry content.
    public static final class UnknownMindustryContentError extends ValidationError {

        private final String path;
        private final ContentType contentType;
        private final String value;

        private UnknownMindustryContentError(final String path, final ContentType contentType, final String value) {
            super(ValidationLevel.ERROR);
            this.path = path;
            this.contentType = contentType;
            this.value = value;
        }

        @Override
        public String description() {
            return "Unable to find Mindustry content named " + this.value + " of type " + this.contentType.name()
                    + " on path: " + this.path;
        }
    }

    /// Error for content that exists but is not assignable to the requested type.
    public static final class InvalidMindustryContentTypeError extends ValidationError {

        private final String path;
        private final String value;
        private final ContentType contentType;
        private final Class<?> expectedType;

        private InvalidMindustryContentTypeError(
                final String path, final String value, final ContentType contentType, final Class<?> expectedType) {
            super(ValidationLevel.ERROR);
            this.path = path;
            this.value = value;
            this.contentType = contentType;
            this.expectedType = expectedType;
        }

        @Override
        public String description() {
            return "Mindustry content named " + this.value + " of type " + this.contentType.name()
                    + " is not assignable to " + this.expectedType.getName() + " on path: " + this.path;
        }
    }
}
