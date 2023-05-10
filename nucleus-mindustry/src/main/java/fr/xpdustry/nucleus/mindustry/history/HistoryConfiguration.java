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

import fr.xpdustry.nucleus.common.annotation.NucleusStyle;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Composite;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Content;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Enable;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Link;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Logic;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Message;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Simple;
import fr.xpdustry.nucleus.mindustry.util.ImmutablePoint;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

public sealed interface HistoryConfiguration permits Content, Enable, Link, Logic, Message, Composite, Simple {

    @NucleusStyle
    @Value.Immutable
    sealed interface Enable extends HistoryConfiguration permits ImmutableEnable {

        static Enable of(final boolean enabled) {
            return ImmutableEnable.builder().value(enabled).build();
        }

        boolean getValue();
    }

    @NucleusStyle
    @Value.Immutable
    sealed interface Content extends HistoryConfiguration permits ImmutableContent {

        static Content of(final UnlockableContent content) {
            return ImmutableContent.builder().value(content).build();
        }

        static Content empty() {
            return ImmutableContent.builder().value(Optional.empty()).build();
        }

        Optional<UnlockableContent> getValue();
    }

    @NucleusStyle
    @Value.Immutable
    sealed interface Logic extends HistoryConfiguration permits ImmutableLogic {

        static Logic of(final byte[] bytes, final Logic.Type type) {
            return ImmutableLogic.builder()
                    .buffer(ByteBuffer.wrap(bytes.clone()))
                    .type(type)
                    .build();
        }

        ByteBuffer getBuffer();

        Logic.Type getType();

        enum Type {
            INSTRUCTIONS,
            IMAGE
        }
    }

    @NucleusStyle
    @Value.Immutable
    sealed interface Message extends HistoryConfiguration permits ImmutableMessage {

        static Message of(final String message) {
            return ImmutableMessage.builder().value(message).build();
        }

        String getValue();
    }

    @NucleusStyle
    @Value.Immutable
    sealed interface Link extends HistoryConfiguration permits ImmutableLink {

        static Link of(final List<ImmutablePoint> positions, final Link.Type type) {
            return ImmutableLink.builder().addAllPositions(positions).type(type).build();
        }

        List<ImmutablePoint> getPositions();

        Link.Type getType();

        enum Type {
            CONNECT,
            DISCONNECT
        }
    }

    @NucleusStyle
    @Value.Immutable
    sealed interface Composite extends HistoryConfiguration permits ImmutableComposite {

        static Composite of(final HistoryConfiguration... configurations) {
            for (final var configuration : configurations) {
                if (configuration instanceof Composite) {
                    throw new IllegalArgumentException("A Composite configuration cannot contain another.");
                }
            }
            return ImmutableComposite.builder()
                    .addAllConfigurations(Arrays.asList(configurations))
                    .build();
        }

        List<HistoryConfiguration> getConfigurations();
    }

    @NucleusStyle
    @Value.Immutable
    sealed interface Simple extends HistoryConfiguration permits ImmutableSimple {

        static Simple of(final Object value) {
            return ImmutableSimple.builder().value(value).build();
        }

        static Simple empty() {
            return ImmutableSimple.builder().value(Optional.empty()).build();
        }

        Optional<Object> getValue();
    }

    @FunctionalInterface
    interface Factory<B extends Building> {

        Optional<HistoryConfiguration> create(
                final B building, final @Nullable Object config, final @Nullable HistoryConfiguration previous);
    }
}
