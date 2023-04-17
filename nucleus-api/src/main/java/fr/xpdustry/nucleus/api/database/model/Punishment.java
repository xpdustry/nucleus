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
package fr.xpdustry.nucleus.api.database.model;

import fr.xpdustry.nucleus.api.annotation.NucleusStyle;
import fr.xpdustry.nucleus.api.database.Entity;
import fr.xpdustry.nucleus.api.database.ObjectIdentifier;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.immutables.value.Value;

// TODO Add defaults
@Value.Immutable
@NucleusStyle
public sealed interface Punishment extends Entity<ObjectIdentifier> permits ImmutablePunishment {

    static Punishment.Builder builder() {
        return ImmutablePunishment.builder();
    }

    static Punishment.Builder from(final Punishment punishment) {
        return ImmutablePunishment.builder().from(punishment);
    }

    @Override
    ObjectIdentifier getIdentifier();

    List<InetAddress> getTargets();

    Type getType();

    String getReason();

    Duration getDuration();

    boolean isPardoned();

    default boolean isExpired() {
        return isPardoned() || getTimestamp().plus(getDuration()).isBefore(Instant.now());
    }

    default Instant getTimestamp() {
        return getIdentifier().getTimestamp();
    }

    enum Type {
        MUTE,
        KICK,
        BAN
    }

    sealed interface Builder extends Entity.Builder<ObjectIdentifier, Punishment, Punishment.Builder>
            permits ImmutablePunishment.Builder {

        Builder setTargets(final Iterable<? extends InetAddress> targetIps);

        Builder setType(final Type type);

        Builder setReason(final String reason);

        Builder setDuration(final Duration duration);

        Builder setPardoned(final boolean pardoned);
    }
}
