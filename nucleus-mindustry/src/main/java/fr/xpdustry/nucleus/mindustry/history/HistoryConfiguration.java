/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.mindustry.history;

import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Composite;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Content;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Enable;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Link;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Logic;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Message;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Unknown;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

public sealed interface HistoryConfiguration permits Content, Enable, Link, Logic, Message, Composite, Unknown {

    @Value.Immutable
    sealed interface Enable extends HistoryConfiguration permits ImmutableEnable {

        static Enable of(final boolean enabled) {
            return ImmutableEnable.builder().value(enabled).build();
        }

        boolean getValue();
    }

    @Value.Immutable
    sealed interface Content extends HistoryConfiguration permits ImmutableContent {

        static Content of(final UnlockableContent content) {
            return ImmutableContent.builder().value(Optional.of(content)).build();
        }

        static Content empty() {
            return ImmutableContent.builder().value(Optional.empty()).build();
        }

        Optional<UnlockableContent> getValue();
    }

    @Value.Immutable
    sealed interface Logic extends HistoryConfiguration permits ImmutableLogic {

        static Logic of(final byte[] bytes, final Logic.Type type) {
            return ImmutableLogic.builder()
                    .value(ByteBuffer.wrap(bytes.clone()))
                    .type(type)
                    .build();
        }

        ByteBuffer getValue();

        Logic.Type getType();

        enum Type {
            INSTRUCTIONS,
            IMAGE
        }
    }

    @Value.Immutable
    sealed interface Message extends HistoryConfiguration permits ImmutableMessage {

        static Message of(final String message) {
            return ImmutableMessage.builder().value(message).build();
        }

        String getValue();
    }

    @Value.Immutable
    sealed interface Link extends HistoryConfiguration permits ImmutableLink {

        static Link of(final List<Integer> links, final Link.Type type) {
            return ImmutableLink.builder().addAllValue(links).type(type).build();
        }

        List<Integer> getValue();

        Link.Type getType();

        enum Type {
            CONNECT,
            DISCONNECT
        }
    }

    @Value.Immutable
    sealed interface Composite extends HistoryConfiguration permits ImmutableComposite {

        static Composite of(final List<HistoryConfiguration> configurations) {
            for (final var configuration : configurations) {
                if (configuration instanceof Composite) {
                    throw new IllegalArgumentException("A Composite configuration cannot contain another.");
                }
            }
            return ImmutableComposite.builder()
                    .addAllConfigurations(configurations)
                    .build();
        }

        List<HistoryConfiguration> getConfigurations();
    }

    @Value.Immutable
    sealed interface Unknown extends HistoryConfiguration permits ImmutableUnknown {

        static Unknown of(final Object value) {
            return ImmutableUnknown.builder().value(Optional.of(value)).build();
        }

        static Unknown empty() {
            return ImmutableUnknown.builder().value(Optional.empty()).build();
        }

        Optional<Object> getValue();
    }

    @FunctionalInterface
    interface Factory<B extends Building> {

        HistoryConfiguration create(final B building, final @Nullable Object config);
    }
}
