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
import java.net.InetAddress;
import java.time.Duration;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@NucleusStyle
public sealed interface User extends Entity<String> permits ImmutableUser {

    static User.Builder builder() {
        return ImmutableUser.builder();
    }

    @Override
    String getIdentifier();

    String getLastName();

    Set<String> getNames();

    InetAddress getLastAddress();

    Set<InetAddress> getAddresses();

    int getTimesJoined();

    int getTimesKicked();

    int getGamesPlayed();

    Duration getPlayTime();

    default User.Builder toBuilder() {
        return ImmutableUser.builder().from(this);
    }

    sealed interface Builder extends Entity.Builder<String, User, Builder> permits ImmutableUser.Builder {

        Builder setLastName(final String lastName);

        Builder setNames(final Iterable<String> names);

        Builder addName(final String name);

        Builder addAllNames(final Iterable<String> names);

        Builder setLastAddress(final InetAddress lastIp);

        Builder setAddresses(final Iterable<? extends InetAddress> ips);

        Builder addAddress(final InetAddress ip);

        Builder addAllAddresses(final Iterable<? extends InetAddress> ips);

        Builder setTimesJoined(final int timesJoined);

        Builder setTimesKicked(final int timesKicked);

        Builder setGamesPlayed(final int gamesPlayed);

        Builder setPlayTime(final Duration playTime);
    }
}
