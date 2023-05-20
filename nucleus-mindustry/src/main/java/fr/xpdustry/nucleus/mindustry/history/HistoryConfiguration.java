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

import fr.xpdustry.nucleus.common.annotation.ImmutableNucleusStyle;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Canvas;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Color;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Composite;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Content;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Enable;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Link;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Simple;
import fr.xpdustry.nucleus.mindustry.history.HistoryConfiguration.Text;
import fr.xpdustry.nucleus.mindustry.util.ImmutablePoint;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value.Immutable;

// TODO Rename the configurations to dedicated names
public sealed interface HistoryConfiguration permits Content, Enable, Link, Text, Composite, Simple, Color, Canvas {

    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Enable extends HistoryConfiguration permits ImmutableEnable {

        static Enable of(final boolean enabled) {
            return ImmutableEnable.of(enabled);
        }

        boolean getValue();
    }

    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Content extends HistoryConfiguration permits ImmutableContent {

        static Content of(final UnlockableContent content) {
            return ImmutableContent.of(Optional.of(content));
        }

        static Content empty() {
            return ImmutableContent.of(Optional.empty());
        }

        Optional<UnlockableContent> getValue();
    }

    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Link extends HistoryConfiguration permits ImmutableLink {

        static Link of(final Iterable<ImmutablePoint> positions, final boolean connected) {
            return ImmutableLink.of(positions, connected ? Type.CONNECT : Type.DISCONNECT);
        }

        static Link reset() {
            return ImmutableLink.of(List.of(), Type.RESET);
        }

        List<ImmutablePoint> getPositions();

        Link.Type getType();

        enum Type {
            CONNECT,
            DISCONNECT,
            RESET
        }
    }

    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Composite extends HistoryConfiguration permits ImmutableComposite {

        static Composite of(final Iterable<HistoryConfiguration> configurations) {
            for (final var configuration : configurations) {
                if (configuration instanceof Composite) {
                    throw new IllegalArgumentException("A Composite configuration cannot contain another.");
                }
            }
            return ImmutableComposite.of(configurations);
        }

        static Composite of(final HistoryConfiguration... configurations) {
            return of(Arrays.asList(configurations));
        }

        List<HistoryConfiguration> getConfigurations();
    }

    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Simple extends HistoryConfiguration permits ImmutableSimple {

        static Simple of(final Object value) {
            return ImmutableSimple.of(Optional.of(value));
        }

        static Simple empty() {
            return ImmutableSimple.of(Optional.empty());
        }

        Optional<Object> getValue();
    }

    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Text extends HistoryConfiguration permits ImmutableText {

        static Text of(final String text, final Text.Type type) {
            return ImmutableText.of(text, type);
        }

        String getText();

        Text.Type getType();

        enum Type {
            MESSAGE,
            CODE
        }
    }

    // TODO Change the name of the class ?
    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Color extends HistoryConfiguration permits ImmutableColor {

        static Color of(final java.awt.Color color) {
            return ImmutableColor.of(color);
        }

        java.awt.Color getColor();
    }

    @Immutable(copy = false, builder = false)
    @ImmutableNucleusStyle
    sealed interface Canvas extends HistoryConfiguration permits ImmutableCanvas {

        static Canvas of(final byte[] bytes) {
            return ImmutableCanvas.of(ByteBuffer.wrap(bytes));
        }

        ByteBuffer getBytes();
    }

    @FunctionalInterface
    interface Factory<B extends Building> {

        Optional<HistoryConfiguration> create(
                final B building, final HistoryEntry.Type type, final @Nullable Object config);
    }
}
