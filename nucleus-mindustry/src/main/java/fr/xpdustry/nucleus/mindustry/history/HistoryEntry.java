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
import java.time.Instant;
import java.util.Optional;
import mindustry.world.Block;
import org.immutables.value.Value;

// TODO It might be necessary to store the items of a block too
@NucleusStyle
@Value.Immutable
public interface HistoryEntry {

    static Builder builder() {
        return ImmutableHistoryEntry.builder();
    }

    int getX();

    int getBuildX();

    int getY();

    int getBuildY();

    HistoryAuthor getAuthor();

    Block getBlock();

    Optional<HistoryConfiguration> getConfiguration();

    Type getType();

    @Value.Default
    default boolean isVirtual() {
        return false;
    }

    @Value.Default
    default Instant getTimestamp() {
        return Instant.now();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    interface Builder {

        Builder setX(final int x);

        Builder setBuildX(final int x);

        Builder setY(final int y);

        Builder setBuildY(final int y);

        Builder setAuthor(final HistoryAuthor author);

        Builder setBlock(final Block block);

        Builder setVirtual(final boolean virtual);

        Builder setTimestamp(final Instant timestamp);

        Builder setConfiguration(final Optional<? extends HistoryConfiguration> configuration);

        Builder setType(final Type type);

        HistoryEntry build();
    }

    enum Type {
        PLACING,
        PLACE,
        BREAKING,
        BREAK,
        CONFIGURE,
    }
}
